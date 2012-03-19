/*
 * ArgonMS MapleStory server emulator written in Java
 * Copyright (C) 2011-2012  GoldenKevin
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package argonms.game.net.internal;

import argonms.common.character.BuddyList;
import argonms.common.character.BuddyListEntry;
import argonms.common.character.InterServerPartyOps;
import argonms.common.net.internal.RemoteCenterOps;
import argonms.common.util.collections.Pair;
import argonms.common.util.input.LittleEndianReader;
import argonms.common.util.output.LittleEndianByteArrayWriter;
import argonms.common.util.output.LittleEndianWriter;
import argonms.game.GameServer;
import argonms.game.character.BuffState.ItemState;
import argonms.game.character.BuffState.MobSkillState;
import argonms.game.character.BuffState.SkillState;
import argonms.game.character.GameCharacter;
import argonms.game.character.PartyList;
import argonms.game.character.PlayerContinuation;
import argonms.game.field.entity.PlayerSkillSummon;
import argonms.game.net.WorldChannel;
import argonms.game.net.external.GamePackets;
import argonms.game.net.external.handler.BuddyListHandler;
import argonms.game.net.external.handler.PartyListHandler;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author GoldenKevin
 */
public class InterChannelCommunication {
	private static final Logger LOG = Logger.getLogger(InterChannelCommunication.class.getName());

	private static final byte //network opcodes (for remote server communications)
		INBOUND_PLAYER = 1,
		INBOUND_PLAYER_ACCEPTED = 2,
		PLAYER_SEARCH = 3,
		PLAYER_SEARCH_RESPONSE = 4,
		MULTI_CHAT = 5,
		WHISPER_CHAT = 6,
		WHISPER_RESPONSE = 7,
		SPOUSE_CHAT = 8,
		BUDDY_INVITE = 9,
		BUDDY_INVITE_RESPONSE = 10,
		BUDDY_ONLINE = 11,
		BUDDY_ACCEPTED = 12,
		BUDDY_ONLINE_RESPONSE = 13,
		BUDDY_OFFLINE = 14,
		BUDDY_DELETE = 15
	;

	private static final byte //private chat types
		CHAT_TYPE_BUDDY = 0,
		CHAT_TYPE_PARTY = 1,
		CHAT_TYPE_GUILD = 2
	;

	private static class ResponseFuture implements Future<Object[]> {
		private final CountDownLatch waitHandle;
		private final Object[] responses;
		private final AtomicInteger responsesReceived;
		private final AtomicBoolean canceled;

		public ResponseFuture(int expectedResponses) {
			waitHandle = new CountDownLatch(expectedResponses);
			responses = new Object[expectedResponses];
			responsesReceived = new AtomicInteger(0);
			canceled = new AtomicBoolean(false);
		}

		@Override
		public boolean cancel(boolean mayInterruptIfRunning) {
			if (canceled.compareAndSet(false, true)) {
				for (long i = waitHandle.getCount() - 1; i >= 0; i--)
					waitHandle.countDown();
				return true;
			}
			return false;
		}

		@Override
		public boolean isCancelled() {
			return canceled.get();
		}

		@Override
		public boolean isDone() {
			return isCancelled() || waitHandle.getCount() == 0;
		}

		@Override
		public Object[] get() throws InterruptedException, ExecutionException {
			waitHandle.await();
			if (canceled.get())
				throw new CancellationException();
			return responses;
		}

		@Override
		public Object[] get(long timeout, TimeUnit unit) throws InterruptedException, TimeoutException {
			if (!waitHandle.await(timeout, unit))
				throw new TimeoutException("Response for inter channel intercourse timed out after " + timeout + " " + unit.toString().toLowerCase() + ". Needed " + waitHandle.getCount() + " more responses.");
			if (canceled.get())
				throw new CancellationException();
			return responses;
		}

		public void set(Object response) {
			responses[responsesReceived.getAndIncrement()] = response;
			waitHandle.countDown();
		}
	}

	private final byte[] localChannels;
	private final WorldChannel self;
	private final Map<Byte, Integer> remoteChannelPorts;
	private final Map<Byte, byte[]> remoteChannelAddresses;
	private final Lock readLock, writeLock;

	private final AtomicInteger nextResponseId;
	private final Map<Integer, ResponseFuture> responseListeners;

	public InterChannelCommunication(byte[] local, WorldChannel channel) {
		this.localChannels = local;
		this.self = channel;
		this.remoteChannelPorts = new HashMap<Byte, Integer>();
		this.remoteChannelAddresses = new HashMap<Byte, byte[]>();
		ReadWriteLock rwLock = new ReentrantReadWriteLock();
		readLock = rwLock.readLock();
		writeLock = rwLock.writeLock();

		nextResponseId = new AtomicInteger(0);
		responseListeners = new ConcurrentHashMap<Integer, ResponseFuture>();
	}

	public void addRemoteChannels(byte[] host, Map<Byte, Integer> ports) {
		writeLock.lock();
		try {
			remoteChannelPorts.putAll(ports);
			for (Byte ch : ports.keySet())
				remoteChannelAddresses.put(ch, host);
		} finally {
			writeLock.unlock();
		}
	}

	public void removeRemoteChannels(Set<Byte> channels) {
		writeLock.lock();
		try {
			for (Byte ch : channels) {
				remoteChannelPorts.remove(ch);
				remoteChannelAddresses.remove(ch);
			}
		} finally {
			writeLock.unlock();
		}
	}

	private GameCenterInterface getCenterComm() {
		return GameServer.getInstance().getCenterInterface();
	}

	public Pair<byte[], Integer> getChannelHost(byte ch) throws UnknownHostException {
		for (int i = 0; i < localChannels.length; i++) {
			if (ch == localChannels[i]) {
				byte[] address = InetAddress.getByName(GameServer.getInstance().getExternalIp()).getAddress();
				return new Pair<byte[], Integer>(address, Integer.valueOf(GameServer.getChannel(ch).getPort()));
			}
		}
		readLock.lock();
		try {
			Integer port = remoteChannelPorts.get(Byte.valueOf(ch));
			return new Pair<byte[], Integer>(remoteChannelAddresses.get(Byte.valueOf(ch)), port != null ? port : Integer.valueOf(-1));
		} finally {
			readLock.unlock();
		}
	}

	public void sendChannelChangeRequest(byte destCh, GameCharacter p) {
		for (int i = 0; i < localChannels.length; i++) {
			if (destCh == localChannels[i]) {
				GameServer.getChannel(destCh).getInterChannelInterface().receivedInboundPlayer(self.getChannelId(), p.getId(), new PlayerContinuation(p));
				return;
			}
		}
		getCenterComm().getSession().send(writePlayerContext(destCh, self.getChannelId(), p.getId(), new PlayerContinuation(p)));
	}

	public byte getChannelOfPlayer(String name) {
		if (playerOnLocal(name))
			return self.getChannelId();
		for (int i = 0; i < localChannels.length; i++)
			if (GameServer.getChannel(localChannels[i]).getInterChannelInterface().playerOnLocal(name))
				return localChannels[i];
		boolean readUnlocked = false;
		readLock.lock();
		try {
			int networkPeers = remoteChannelPorts.size();
			if (networkPeers != 0) {
				int responseId = nextResponseId.getAndIncrement();
				ResponseFuture response = new ResponseFuture(networkPeers);
				responseListeners.put(Integer.valueOf(responseId), response);
				for (Byte ch : remoteChannelPorts.keySet()) //recipient could be on another process
					getCenterComm().getSession().send(writePlayerSearch(ch.byteValue(), responseId, self.getChannelId(), name));
				readLock.unlock();
				readUnlocked = true;
				try {
					byte result;
					Object[] array = response.get(2, TimeUnit.SECONDS);
					for (Object o : array)
						if ((result = ((Byte) o).byteValue()) != 0)
							return result;
				} catch (InterruptedException ex) {
					//propagate the interrupted status further up to our worker
					//executor service and see if they care - we don't care about it
					Thread.currentThread().interrupt();
				} catch (TimeoutException ex) {
					LOG.log(Level.FINE, "Player search timeout", ex);
				} catch (CancellationException ex) {

				} finally {
					responseListeners.remove(Integer.valueOf(responseId));
				}
			}
		} finally {
			if (!readUnlocked)
				readLock.unlock();
		}
		return 0;
	}

	public void sendPrivateChat(byte type, int[] recipients, GameCharacter p, String message) {
		String name = p.getName();
		Map<Byte, List<Integer>> peerChannels = null;

		switch (type) {
			case CHAT_TYPE_BUDDY: {
				peerChannels = new HashMap<Byte, List<Integer>>();
				BuddyList bList = p.getBuddyList();
				for (int buddy : recipients) {
					BuddyListEntry entry = bList.getBuddy(buddy);
					Byte ch = Byte.valueOf(entry != null ? entry.getChannel() : (byte) 0);
					List<Integer> peersOnChannel = peerChannels.get(ch);
					if (peersOnChannel == null) {
						peersOnChannel = new ArrayList<Integer>();
						peerChannels.put(ch, peersOnChannel);
					}
					peersOnChannel.add(Integer.valueOf(buddy));
				}
				break;
			} case CHAT_TYPE_PARTY: {
				if (p.getParty() != null) {
					peerChannels = new HashMap<Byte, List<Integer>>();
					for (int member : recipients) {
						Byte ch = Byte.valueOf((byte) 0);
						List<Integer> peersOnChannel = peerChannels.get(ch);
						if (peersOnChannel == null) {
							peersOnChannel = new ArrayList<Integer>();
							peerChannels.put(ch, peersOnChannel);
						}
						peersOnChannel.add(Integer.valueOf(member));
					}
				}
				break;
			} case CHAT_TYPE_GUILD: {
				if (p.getGuildId() != 0) {
					peerChannels = new HashMap<Byte, List<Integer>>();
					for (int member : recipients) {
						Byte ch = Byte.valueOf((byte) 0);
						List<Integer> peersOnChannel = peerChannels.get(ch);
						if (peersOnChannel == null) {
							peersOnChannel = new ArrayList<Integer>();
							peerChannels.put(ch, peersOnChannel);
						}
						peersOnChannel.add(Integer.valueOf(member));
					}
				}
				break;
			}
		}
		if (peerChannels != null) {
			for (Entry<Byte, List<Integer>> entry : peerChannels.entrySet()) {
				byte knownCh = entry.getKey().byteValue();
				int i = 0;
				recipients = new int[entry.getValue().size()];
				for (Integer recipient : entry.getValue())
					recipients[i++] = recipient.intValue();
				if (knownCh != 0) {
					boolean local = false;
					if (knownCh == self.getChannelId()) {
						local = true;
						receivedPrivateChat(type, recipients, name, message);
					}
					for (i = 0; i < localChannels.length && !local; i++) {
						if (knownCh == localChannels[i]) {
							local = true;
							GameServer.getChannel(knownCh).getInterChannelInterface().receivedPrivateChat(type, recipients, name, message);
						}
					}
					if (!local)
						getCenterComm().getSession().send(writePrivateChatMessage(knownCh, type, recipients, name, message));
				} else { //channel unknown
					receivedPrivateChat(type, recipients, name, message); //recipients on same channel
					for (byte ch : localChannels) //recipients on same process
						GameServer.getChannel(ch).getInterChannelInterface().receivedPrivateChat(type, recipients, name, message);
					readLock.lock();
					try {
						for (Byte ch : remoteChannelPorts.keySet()) //recipients on another process
							getCenterComm().getSession().send(writePrivateChatMessage(ch.byteValue(), type, recipients, name, message));
					} finally {
						readLock.unlock();
					}
				}
			}
		}
	}

	public boolean sendWhisper(String recipient, GameCharacter p, String message) {
		if (receivedWhisper(recipient, p.getName(), message, self.getChannelId())) //recipient could be on same channel
			return true;
		for (byte ch : localChannels) //recipient could be on same process
			if (GameServer.getChannel(ch).getInterChannelInterface().receivedWhisper(recipient, p.getName(), message, self.getChannelId()))
				return true;
		boolean readUnlocked = false;
		readLock.lock();
		try {
			int networkPeers = remoteChannelPorts.size();
			if (networkPeers != 0) {
				int responseId = nextResponseId.getAndIncrement();
				ResponseFuture response = new ResponseFuture(networkPeers);
				responseListeners.put(Integer.valueOf(responseId), response);
				for (Byte ch : remoteChannelPorts.keySet()) //recipient could be on another process
					getCenterComm().getSession().send(writeWhisperMessage(ch.byteValue(), responseId, recipient, p.getName(), message, self.getChannelId()));
				readLock.unlock();
				readUnlocked = true;
				try {
					Object[] array = response.get(2, TimeUnit.SECONDS);
					for (Object o : array)
						if (((Boolean) o).booleanValue())
							return true;
				} catch (InterruptedException ex) {
					//propagate the interrupted status further up to our worker
					//executor service and see if they care - we don't care about it
					Thread.currentThread().interrupt();
				} catch (TimeoutException ex) {
					LOG.log(Level.FINE, "Whisper timeout", ex);
				} catch (CancellationException ex) {
					
				} finally {
					responseListeners.remove(Integer.valueOf(responseId));
				}
			}
		} finally {
			if (!readUnlocked)
				readLock.unlock();
		}
		return false;
	}

	public void sendSpouseChat(String spouse, GameCharacter p, String message) {
		//perhaps we should compare spouse's name with p.getSpouseId()?
		String name = p.getName();
		int recipient = p.getSpouseId();
		if (recipient != 0) {
			if (receivedSpouseChat(recipient, name, message)) //recipient could be on same channel
				return;
			for (byte ch : localChannels) //recipient could be on same process
				if (GameServer.getChannel(ch).getInterChannelInterface().receivedSpouseChat(recipient, name, message))
					return;
			readLock.lock();
			try {
				for (Byte ch : remoteChannelPorts.keySet()) //recipient could be on another process
					getCenterComm().getSession().send(writeSpouseChatMessage(ch.byteValue(), recipient, name, message));
			} finally {
				readLock.unlock();
			}
		}
	}

	public byte sendBuddyInvite(GameCharacter inviter, int inviteeId) {
		byte result;
		if ((result = receivedBuddyInvite(inviteeId, inviter.getId(), inviter.getName())) != -1) //invitee could be on same channel
			return result;
		for (byte ch : localChannels) //invitee could be on same channel
				if ((result = GameServer.getChannel(ch).getInterChannelInterface().receivedBuddyInvite(inviteeId, inviter.getId(), inviter.getName())) != -1)
					return result;
		boolean readUnlocked = false;
		readLock.lock();
		try {
			int networkPeers = remoteChannelPorts.size();
			if (networkPeers != 0) {
				int responseId = nextResponseId.getAndIncrement();
				ResponseFuture response = new ResponseFuture(networkPeers);
				responseListeners.put(Integer.valueOf(responseId), response);
				for (Byte ch : remoteChannelPorts.keySet()) //invitee could be on another process
					getCenterComm().getSession().send(writeBuddyInvite(ch.byteValue(), self.getChannelId(), responseId, inviteeId, inviter.getId(), inviter.getName()));
				readLock.unlock();
				readUnlocked = true;
				try {
					Object[] array = response.get(2, TimeUnit.SECONDS);
					for (Object o : array)
						if ((result = ((Byte) o).byteValue()) != -1)
							return result;
				} catch (InterruptedException ex) {
					result = -1;
					//propagate the interrupted status further up to our worker
					//executor service and see if they care - we don't care about it
					Thread.currentThread().interrupt();
				} catch (TimeoutException ex) {
					LOG.log(Level.FINE, "Buddy invite timeout", ex);
					result = -1;
				} catch (CancellationException ex) {
					result = -1;
				} finally {
					responseListeners.remove(Integer.valueOf(responseId));
				}
			}
		} finally {
			if (!readUnlocked)
				readLock.unlock();
		}
		return result;
	}

	public void sendBuddyOnline(GameCharacter p) {
		Collection<BuddyListEntry> buddies = p.getBuddyList().getBuddies();
		if (buddies.isEmpty())
			return;
		int[] recipients = new int[buddies.size()];
		int i = 0, remaining = buddies.size();
		for (BuddyListEntry buddy : buddies)
			if (buddy.getStatus() == BuddyListHandler.STATUS_MUTUAL)
				recipients[i++] = buddy.getId();
		if (recipients.length != i) {
			//just trim recipients of extra 0s
			int[] temp = new int[i];
			System.arraycopy(recipients, 0, temp, 0, i);
			recipients = temp;
		}
		remaining -= receivedBuddyOnline(self.getChannelId(), p.getId(), recipients);
		for (i = 0; i < localChannels.length && remaining > 0; i++)
			remaining -= GameServer.getChannel(localChannels[i]).getInterChannelInterface().receivedBuddyOnline(self.getChannelId(), p.getId(), recipients);
		if (remaining > 0)
			getCenterComm().getSession().send(writeBuddyOnlineNotification(self.getChannelId(), p.getId(), recipients));
	}

	public void sendBuddyAccepted(GameCharacter p, int recipient) {
		if (receivedBuddyAccepted(self.getChannelId(), p.getId(), recipient))
			return;
		for (int i = 0; i < localChannels.length; i++)
			if (GameServer.getChannel(localChannels[i]).getInterChannelInterface().receivedBuddyAccepted(self.getChannelId(), p.getId(), recipient))
				return;
		getCenterComm().getSession().send(writeBuddyAccepted(self.getChannelId(), p.getId(), recipient));
	}

	public void sendBuddyOffline(GameCharacter p) {
		Collection<BuddyListEntry> buddies = p.getBuddyList().getBuddies();
		if (buddies.isEmpty())
			return;
		Map<Byte, List<Integer>> buddyChannels = new HashMap<Byte, List<Integer>>();
		for (BuddyListEntry buddy : buddies) {
			if (buddy.getChannel() != 0) {
				Byte ch = Byte.valueOf(buddy.getChannel());
				List<Integer> buddiesOnChannel = buddyChannels.get(ch);
				if (buddiesOnChannel == null) {
					buddiesOnChannel = new ArrayList<Integer>();
					buddyChannels.put(ch, buddiesOnChannel);
				}
				buddiesOnChannel.add(Integer.valueOf(buddy.getId()));
			}
		}
		for (Entry<Byte, List<Integer>> entry : buddyChannels.entrySet()) {
			byte ch = entry.getKey().byteValue();
			int[] recipients = new int[entry.getValue().size()];
			int i = 0;
			for (Integer recipient : entry.getValue())
				recipients[i++] = recipient.intValue();
			boolean local = false;
			if (ch == self.getChannelId()) {
				local = true;
				receivedBuddyOffline(p.getId(), recipients);
			}
			for (i = 0; i < localChannels.length && !local; i++) {
				if (ch == localChannels[i]) {
					local = true;
					GameServer.getChannel(ch).getInterChannelInterface().receivedBuddyOffline(p.getId(), recipients);
				}
			}
			if (!local)
				getCenterComm().getSession().send(writeBuddyOfflineNotification(ch, p.getId(), recipients));
		}
	}

	public void sendBuddyDeleted(GameCharacter p, int recipient, byte channel) {
		if (channel == self.getChannelId()) {
			receivedBuddyDelete(p.getId(), recipient);
			return;
		}
		for (int i = 0; i < localChannels.length; i++) {
			if (channel == localChannels[i]) {
				GameServer.getChannel(channel).getInterChannelInterface().receivedBuddyDelete(p.getId(), recipient);
				return;
			}
		}
		getCenterComm().getSession().send(writeBuddyDelete(channel, p.getId(), recipient));
	}

	public void makeParty(GameCharacter p) {
		getCenterComm().getSession().send(writeMakeParty(self.getChannelId(), p));
	}

	public void disbandParty(int partyId) {
		getCenterComm().getSession().send(writeDisbandParty(partyId));
	}

	public void leaveParty(GameCharacter p, int partyId) {
		getCenterComm().getSession().send(writeLeaveParty(p, partyId));
	}

	public void joinParty(GameCharacter p, int partyId) {
		getCenterComm().getSession().send(writeJoinParty(p, partyId));
	}

	public void expelPartyMember(PartyList.Member member, int partyId) {
		getCenterComm().getSession().send(writeExpelPartyMember(member.getPlayerId(), member.getName(), partyId, member.getChannel()));
	}

	public void changePartyLeader(int partyId, int newLeader) {
		getCenterComm().getSession().send(writeNewPartyLeader(partyId, newLeader));
	}

	public void receivedInboundPlayer(byte fromCh, int playerId, PlayerContinuation context) {
		self.storePlayerBuffs(playerId, context);

		for (int i = 0; i < localChannels.length; i++) {
			if (fromCh == localChannels[i]) {
				GameServer.getChannel(fromCh).getInterChannelInterface().acceptedInboundPlayer(playerId);
				return;
			}
		}
		getCenterComm().getSession().send(writePlayerContextAccepted(fromCh, playerId));
	}

	private void acceptedInboundPlayer(int playerId) {
		self.performChannelChange(playerId);
	}

	private boolean playerOnLocal(String name) {
		return self.getPlayerByName(name) != null;
	}

	private void receivedPrivateChat(byte type, int[] recipients, String name, String message) {
		GameCharacter p;
		for (int i = 0; i < recipients.length; i++) {
			p = self.getPlayerById(recipients[i]);
			if (p != null)
				p.getClient().getSession().send(GamePackets.writePrivateChatMessage(type, name, message));
		}
	}

	private boolean receivedWhisper(String recipient, String name, String message, byte fromCh) {
		GameCharacter p = self.getPlayerByName(recipient);
		if (p != null) {
			p.getClient().getSession().send(GamePackets.writeWhisperMessage(name, message, fromCh));
			return true;
		}
		return false;
	}

	private boolean receivedSpouseChat(int recipient, String name, String message) {
		GameCharacter p = self.getPlayerById(recipient);
		if (p != null) {
			p.getClient().getSession().send(GamePackets.writeSpouseChatMessage(name, message));
			return true;
		}
		return false;
	}

	/**
	 *
	 * @param invitee
	 * @param inviter
	 * @return -1 if invitee is not on this channel, Byte.MAX_VALUE if the
	 * invite was successfully sent, or two other response codes
	 */
	private byte receivedBuddyInvite(int invitee, int inviter, String inviterName) {
		GameCharacter p = self.getPlayerById(invitee);
		if (p != null) {
			BuddyList bList = p.getBuddyList();
			if (bList.isFull())
				return BuddyListHandler.THEIR_LIST_FULL;
			if (bList.getBuddy(inviter) != null)
				return BuddyListHandler.ALREADY_ON_LIST;
			bList.addInvite(inviter, inviterName);
			p.getClient().getSession().send(GamePackets.writeBuddyInvite(inviter, inviterName));
			return Byte.MAX_VALUE;
		}
		return -1;
	}

	private int receivedBuddyOnline(byte fromCh, int sender, int[] receivers) {
		int received = 0;
		for (int receiver : receivers) {
			GameCharacter p = self.getPlayerById(receiver);
			if (p != null) {
				BuddyList bList = p.getBuddyList();
				BuddyListEntry entry = bList.getBuddy(sender);
				//in case we deleted the entry in the meantime...
				if (entry != null) {
					entry.setChannel(fromCh);
					p.getClient().getSession().send(GamePackets.writeBuddyLoggedIn(entry));
					p.getClient().getSession().send(GamePackets.writeBuddyList(BuddyListHandler.ADD, bList));
					boolean local = false;
					if (fromCh == self.getChannelId()) {
						local = true;
						receivedBuddyOnlineResponse(self.getChannelId(), sender, receiver, false);
					}
					for (int i = 0; i < localChannels.length && !local; i++) {
						if (fromCh == localChannels[i]) {
							local = true;
							GameServer.getChannel(fromCh).getInterChannelInterface().receivedBuddyOnlineResponse(self.getChannelId(), sender, receiver, false);
						}
					}
					if (!local)
						getCenterComm().getSession().send(writeBuddyOnlineResponse(fromCh, self.getChannelId(), sender, receiver, false));
				}
				received++;
			}
		}
		return received;
	}

	private boolean receivedBuddyAccepted(byte fromCh, int sender, int receiver) {
		GameCharacter p = self.getPlayerById(receiver);
		if (p != null) {
			BuddyList bList = p.getBuddyList();
			BuddyListEntry entry = bList.getBuddy(sender);
			//in case we deleted the entry in the meantime...
			if (entry != null) {
				entry.setChannel(fromCh);
				entry.setStatus(BuddyListHandler.STATUS_MUTUAL);
				p.getClient().getSession().send(GamePackets.writeBuddyLoggedIn(entry));
				p.getClient().getSession().send(GamePackets.writeBuddyList(BuddyListHandler.ADD, bList));
				boolean local = false;
				if (fromCh == self.getChannelId()) {
					local = true;
					receivedBuddyOnlineResponse(self.getChannelId(), sender, receiver, true);
				}
				for (int i = 0; i < localChannels.length && !local; i++) {
					if (fromCh == localChannels[i]) {
						local = true;
						GameServer.getChannel(fromCh).getInterChannelInterface().receivedBuddyOnlineResponse(self.getChannelId(), sender, receiver, true);
					}
				}
				if (!local)
					getCenterComm().getSession().send(writeBuddyOnlineResponse(fromCh, self.getChannelId(), sender, receiver, true));
			}
			return true;
		}
		return false;
	}

	private void receivedBuddyOnlineResponse(byte srcCh, int receiver, int sender, boolean notify) {
		GameCharacter p = self.getPlayerById(receiver);
		//in case we logged off or something like that?
		if (p != null) {
			BuddyList bList = p.getBuddyList();
			BuddyListEntry entry = bList.getBuddy(sender);
			//in case we deleted the entry in the meantime...
			if (entry != null) {
				entry.setChannel(srcCh);
				if (notify)
					p.getClient().getSession().send(GamePackets.writeBuddyLoggedIn(entry));
				p.getClient().getSession().send(GamePackets.writeBuddyList(BuddyListHandler.ADD, bList));
			}
		}
	}

	private void receivedBuddyOffline(int sender, int[] receivers) {
		for (int receiver : receivers) {
			GameCharacter p = self.getPlayerById(receiver);
			//in case we logged off or something like that?
			if (p != null) {
				BuddyList bList = p.getBuddyList();
				BuddyListEntry entry = bList.getBuddy(sender);
				//in case we deleted the entry in the meantime...
				if (entry != null) {
					entry.setChannel((byte) 0);
					p.getClient().getSession().send(GamePackets.writeBuddyList(BuddyListHandler.REMOVE, bList));
				}
			}
		}
	}

	private void receivedBuddyDelete(int sender, int receiver) {
		GameCharacter p = self.getPlayerById(receiver);
		//in case we logged off or something like that?
		if (p != null) {
			BuddyList bList = p.getBuddyList();
			BuddyListEntry entry = bList.getBuddy(sender);
			//in case we deleted the entry in the meantime...
			if (entry != null) {
				entry.setStatus(BuddyListHandler.STATUS_HALF_OPEN);
				p.getClient().getSession().send(GamePackets.writeBuddyList(BuddyListHandler.REMOVE, bList));
			}
		}
	}

	private static void writeHeader(LittleEndianWriter lew, byte channel) {
		lew.writeByte(RemoteCenterOps.INTER_CHANNEL);
		lew.writeByte(channel);
	}

	private static byte[] writePlayerContext(byte channel, byte src, int playerId, PlayerContinuation context) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter();
		writeHeader(lew, channel);
		lew.writeByte(INBOUND_PLAYER);
		lew.writeByte(src);
		lew.writeInt(playerId);
		Map<Integer, ItemState> activeItems = context.getActiveItems();
		lew.writeByte((byte) activeItems.size());
		for (Entry<Integer, ItemState> item : activeItems.entrySet()) {
			lew.writeInt(item.getKey().intValue());
			lew.writeLong(item.getValue().endTime);
		}
		Map<Integer, SkillState> activeSkills = context.getActiveSkills();
		lew.writeByte((byte) activeSkills.size());
		for (Entry<Integer, SkillState> skill : activeSkills.entrySet()) {
			SkillState skillState = skill.getValue();
			lew.writeInt(skill.getKey().intValue());
			lew.writeByte(skillState.level);
			lew.writeLong(skillState.endTime);
		}
		Map<Short, MobSkillState> activeDebuffs = context.getActiveDebuffs();
		lew.writeByte((byte) activeDebuffs.size());
		for (Entry<Short, MobSkillState> debuff : activeDebuffs.entrySet()) {
			MobSkillState debuffState = debuff.getValue();
			lew.writeShort(debuff.getKey().shortValue());
			lew.writeByte(debuffState.level);
			lew.writeLong(debuffState.endTime);
		}
		Map<Integer, PlayerSkillSummon> activeSummons = context.getActiveSummons();
		lew.writeByte((byte) activeSummons.size());
		for (Entry<Integer, PlayerSkillSummon> summon : activeSummons.entrySet()) {
			PlayerSkillSummon summonState = summon.getValue();
			lew.writeInt(summon.getKey().intValue());
			lew.writePos(summonState.getPosition());
			lew.writeByte(summonState.getStance());
		}
		lew.writeShort(context.getEnergyCharge());
		return lew.getBytes();
	}

	private static byte[] writePlayerContextAccepted(byte channel, int playerId) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(7);
		writeHeader(lew, channel);
		lew.writeByte(INBOUND_PLAYER_ACCEPTED);
		lew.writeInt(playerId);
		return lew.getBytes();
	}

	private static byte[] writePlayerSearch(byte dest, int responseId, byte src, String name) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(10
				+ name.length());
		writeHeader(lew, dest);
		lew.writeByte(PLAYER_SEARCH);
		lew.writeInt(responseId);
		lew.writeByte(src);
		lew.writeLengthPrefixedString(name);
		return lew.getBytes();
	}

	private static byte[] writePlayerSearchResponse(int responseId, byte channel, byte response) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(8);
		writeHeader(lew, channel);
		lew.writeByte(PLAYER_SEARCH_RESPONSE);
		lew.writeInt(responseId);
		lew.writeByte(response);
		return lew.getBytes();
	}

	private static byte[] writePrivateChatMessage(byte channel, byte type, int[] recipients, String name, String message) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(9
				+ 4 * recipients.length + name.length() + message.length());
		writeHeader(lew, channel);
		lew.writeByte(MULTI_CHAT);
		lew.writeByte(type);
		lew.writeByte((byte) recipients.length);
		for (byte i = 0; i < recipients.length; i++)
			lew.writeInt(recipients[i]);
		lew.writeLengthPrefixedString(name);
		lew.writeLengthPrefixedString(message);
		return lew.getBytes();
	}

	private static byte[] writeWhisperMessage(byte dest, int responseId, String recipient, String name, String message, byte src) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(14
				+ recipient.length() + name.length() + message.length());
		writeHeader(lew, dest);
		lew.writeByte(WHISPER_CHAT);
		lew.writeInt(responseId);
		lew.writeLengthPrefixedString(recipient);
		lew.writeLengthPrefixedString(name);
		lew.writeByte(src);
		lew.writeLengthPrefixedString(message);
		return lew.getBytes();
	}

	private static byte[] writeWhisperResponse(byte channel, int responseId, boolean result) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(8);
		writeHeader(lew, channel);
		lew.writeByte(WHISPER_RESPONSE);
		lew.writeInt(responseId);
		lew.writeBool(result);
		return lew.getBytes();
	}

	private static byte[] writeSpouseChatMessage(byte channel, int recipient, String name, String message) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(11
				+ name.length() + message.length());
		writeHeader(lew, channel);
		lew.writeByte(SPOUSE_CHAT);
		lew.writeInt(recipient);
		lew.writeLengthPrefixedString(name);
		lew.writeLengthPrefixedString(message);
		return lew.getBytes();
	}

	private static byte[] writeBuddyInvite(byte dest, byte src, int responseId, int recipient, int sender, String senderName) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(17 + senderName.length());
		writeHeader(lew, dest);
		lew.writeByte(BUDDY_INVITE);
		lew.writeInt(responseId);
		lew.writeByte(src);
		lew.writeInt(recipient);
		lew.writeInt(sender);
		lew.writeLengthPrefixedString(senderName);
		return lew.getBytes();
	}

	private static byte[] writeBuddyInviteResponse(byte channel, int responseId, byte result) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(8);
		writeHeader(lew, channel);
		lew.writeByte(BUDDY_INVITE_RESPONSE);
		lew.writeInt(responseId);
		lew.writeByte(result);
		return lew.getBytes();
	}

	private static byte[] writeBuddyOnlineNotification(byte src, int sender, int[] receivers) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(9 + 4 * receivers.length);
		lew.writeByte(RemoteCenterOps.INTER_CHANNEL_ALL);
		lew.writeByte(BUDDY_ONLINE);
		lew.writeByte(src);
		lew.writeInt(sender);
		lew.writeByte((byte) receivers.length);
		for (int receiver : receivers)
			lew.writeInt(receiver);
		return lew.getBytes();
	}

	private static byte[] writeBuddyAccepted(byte src, int sender, int receiver) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(11);
		lew.writeByte(RemoteCenterOps.INTER_CHANNEL_ALL);
		lew.writeByte(BUDDY_ACCEPTED);
		lew.writeByte(src);
		lew.writeInt(sender);
		lew.writeInt(receiver);
		return lew.getBytes();
	}

	private static byte[] writeBuddyOnlineResponse(byte destCh, byte srcCh, int receiver, int sender, boolean notify) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(13);
		writeHeader(lew, destCh);
		lew.writeByte(BUDDY_ONLINE_RESPONSE);
		lew.writeByte(srcCh);
		lew.writeInt(receiver);
		lew.writeInt(sender);
		lew.writeBool(notify);
		return lew.getBytes();
	}

	private static byte[] writeBuddyOfflineNotification(byte destCh, int sender, int[] receivers) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(8 + 4 * receivers.length);
		writeHeader(lew, destCh);
		lew.writeByte(BUDDY_OFFLINE);
		lew.writeInt(sender);
		lew.writeByte((byte) receivers.length);
		for (Integer receiver : receivers)
			lew.writeInt(receiver.intValue());
		return lew.getBytes();
	}

	private static byte[] writeBuddyDelete(byte destCh, int sender, int receiver) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(11);
		writeHeader(lew, destCh);
		lew.writeByte(BUDDY_DELETE);
		lew.writeInt(sender);
		lew.writeInt(receiver);
		return lew.getBytes();
	}

	private static byte[] writeMakeParty(byte srcCh, GameCharacter creator) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(13 + creator.getName().length());
		lew.writeByte(RemoteCenterOps.PARTY_SYNCHRONIZATION);
		lew.writeByte(InterServerPartyOps.CREATE);
		lew.writeByte(srcCh);
		lew.writeInt(creator.getId());
		lew.writeLengthPrefixedString(creator.getName());
		lew.writeShort(creator.getJob());
		lew.writeShort(creator.getLevel());
		return lew.getBytes();
	}

	private static byte[] writeDisbandParty(int partyId) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(6);
		lew.writeByte(RemoteCenterOps.PARTY_SYNCHRONIZATION);
		lew.writeByte(InterServerPartyOps.DISBAND);
		lew.writeInt(partyId);
		return lew.getBytes();
	}

	private static byte[] writeLeaveParty(GameCharacter p, int partyId) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(14 + p.getName().length());
		lew.writeByte(RemoteCenterOps.PARTY_SYNCHRONIZATION);
		lew.writeByte(InterServerPartyOps.REMOVE_PLAYER);
		lew.writeInt(partyId);
		lew.writeInt(p.getId());
		lew.writeLengthPrefixedString(p.getName());
		lew.writeByte(p.getClient().getChannel());
		lew.writeBool(false);
		return lew.getBytes();
	}

	private static byte[] writeJoinParty(GameCharacter p, int partyId) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(17 + p.getName().length());
		lew.writeByte(RemoteCenterOps.PARTY_SYNCHRONIZATION);
		lew.writeByte(InterServerPartyOps.ADD_PLAYER);
		lew.writeByte(p.getClient().getChannel());
		lew.writeInt(partyId);
		lew.writeInt(p.getId());
		lew.writeLengthPrefixedString(p.getName());
		lew.writeShort(p.getJob());
		lew.writeShort(p.getLevel());
		return lew.getBytes();
	}

	private static byte[] writeExpelPartyMember(int playerId, String playerName, int partyId, byte playerChannel) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(14 + playerName.length());
		lew.writeByte(RemoteCenterOps.PARTY_SYNCHRONIZATION);
		lew.writeByte(InterServerPartyOps.REMOVE_PLAYER);
		lew.writeInt(partyId);
		lew.writeInt(playerId);
		lew.writeLengthPrefixedString(playerName);
		lew.writeByte(playerChannel);
		lew.writeBool(true);
		return lew.getBytes();
	}

	private static byte[] writeNewPartyLeader(int partyId, int newLeader) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(10);
		lew.writeByte(RemoteCenterOps.PARTY_SYNCHRONIZATION);
		lew.writeByte(InterServerPartyOps.CHANGE_LEADER);
		lew.writeInt(partyId);
		lew.writeInt(newLeader);
		return lew.getBytes();
	}

	public void receivedPacket(LittleEndianReader packet) {
		switch (packet.readByte()) {
			case INBOUND_PLAYER: {
				PlayerContinuation context = new PlayerContinuation();
				byte channel = packet.readByte();
				int playerId = packet.readInt();
				byte count = packet.readByte();
				for (int i = 0; i < count; i++)
					context.addItemBuff(packet.readInt(), packet.readLong());
				count = packet.readByte();
				for (int i = 0; i < count; i++)
					context.addSkillBuff(packet.readInt(), packet.readByte(), packet.readLong());
				count = packet.readByte();
				for (int i = 0; i < count; i++)
					context.addMonsterDebuff(packet.readShort(), packet.readByte(), packet.readLong());
				count = packet.readByte();
				for (int i = 0; i < count; i++)
					context.addActiveSummon(packet.readInt(), playerId, packet.readPos(), packet.readByte());
				context.setEnergyCharge(packet.readShort());
				receivedInboundPlayer(channel, playerId, context);
				break;
			} case INBOUND_PLAYER_ACCEPTED: {
				int playerId = packet.readInt();
				acceptedInboundPlayer(playerId);
				break;
			} case PLAYER_SEARCH: {
				int responseId = packet.readInt();
				byte fromCh = packet.readByte();
				String query = packet.readLengthPrefixedString();
				getCenterComm().getSession().send(writePlayerSearchResponse(responseId, fromCh, playerOnLocal(query) ? self.getChannelId() : 0));
				break;
			} case PLAYER_SEARCH_RESPONSE: {
				int responseId = packet.readInt();
				byte value = packet.readByte();
				ResponseFuture response = responseListeners.get(Integer.valueOf(responseId));
				if (response != null)
					response.set(Byte.valueOf(value));
				else
					LOG.log(Level.FINE, "Received inter channel intercourse response {0} on channel {1} after Future timed out.", new Object[] { responseId, self.getChannelId()});
				break;
			} case MULTI_CHAT: {
				byte type = packet.readByte();
				byte amount = packet.readByte();
				int[] recipients = new int[amount];
				for (byte i = 0; i < amount; i++)
					recipients[i] = packet.readInt();
				String name = packet.readLengthPrefixedString();
				String message = packet.readLengthPrefixedString();
				receivedPrivateChat(type, recipients, name, message);
				break;
			} case WHISPER_CHAT: {
				int responseId = packet.readInt();
				String recipient = packet.readLengthPrefixedString();
				String sender = packet.readLengthPrefixedString();
				byte fromCh = packet.readByte();
				String message = packet.readLengthPrefixedString();
				boolean result = receivedWhisper(recipient, sender, message, fromCh);
				getCenterComm().getSession().send(writeWhisperResponse(fromCh, responseId, result));
				break;
			} case WHISPER_RESPONSE: {
				int responseId = packet.readInt();
				boolean result = packet.readBool();
				ResponseFuture response = responseListeners.get(Integer.valueOf(responseId));
				if (response != null)
					response.set(Boolean.valueOf(result));
				else
					LOG.log(Level.FINE, "Received inter channel intercourse response {0} on channel {1} after Future timed out.", new Object[] { responseId, self.getChannelId()});
				break;
			} case SPOUSE_CHAT: {
				int recipient = packet.readInt();
				String name = packet.readLengthPrefixedString();
				String message = packet.readLengthPrefixedString();
				receivedSpouseChat(recipient, name, message);
				break;
			} case BUDDY_INVITE: {
				int responseId = packet.readInt();
				byte returnCh = packet.readByte();
				int recipient = packet.readInt();
				int sender = packet.readInt();
				String senderName = packet.readLengthPrefixedString();
				byte result = receivedBuddyInvite(recipient, sender, senderName);
				getCenterComm().getSession().send(writeBuddyInviteResponse(returnCh, responseId, result));
				break;
			} case BUDDY_INVITE_RESPONSE: {
				int responseId = packet.readInt();
				byte result = packet.readByte();
				ResponseFuture response = responseListeners.get(Integer.valueOf(responseId));
				if (response != null)
					response.set(Byte.valueOf(result));
				else
					LOG.log(Level.FINE, "Received inter channel intercourse response {0} on channel {1} after Future timed out.", new Object[] { responseId, self.getChannelId()});
				break;
			} case BUDDY_ONLINE: {
				byte channel = packet.readByte();
				int sender = packet.readInt();
				byte receiversCount = packet.readByte();
				int[] receivers = new int[receiversCount];
				for (int i = 0; i < receiversCount; i++)
					receivers[i] = packet.readInt();
				receivedBuddyOnline(channel, sender, receivers);
				break;
			} case BUDDY_ACCEPTED: {
				byte channel = packet.readByte();
				int sender = packet.readInt();
				int receiver = packet.readInt();
				receivedBuddyAccepted(channel, sender, receiver);
				break;
			} case BUDDY_ONLINE_RESPONSE: {
				byte channel = packet.readByte();
				int receiver = packet.readInt();
				int sender = packet.readInt();
				boolean notify = packet.readBool();
				receivedBuddyOnlineResponse(channel, receiver, sender, notify);
				break;
			} case BUDDY_OFFLINE: {
				int sender = packet.readInt();
				byte receiversCount = packet.readByte();
				int[] receivers = new int[receiversCount];
				for (int i = 0; i < receiversCount; i++)
					receivers[i] = packet.readInt();
				receivedBuddyOffline(sender, receivers);
				break;
			} case BUDDY_DELETE: {
				int sender = packet.readInt();
				int receiver = packet.readInt();
				receivedBuddyDelete(sender, receiver);
				break;
			}
		}
	}

	public void receivedPartyPacket(LittleEndianReader packet) {
		switch (packet.readByte()) {
			case InterServerPartyOps.CREATE: {
				GameCharacter p = self.getPlayerById(packet.readInt());
				int partyId = packet.readInt();
				PartyList party = new PartyList(partyId, p.getId(), p);
				if (p != null) { //if there was lag, player may have changed channels or logged off while waiting for party to be created
					p.setParty(party);
					p.getClient().getSession().send(GamePackets.writePartyCreated(partyId));
				}
				break;
			} case InterServerPartyOps.DISBAND: {
				for (int i = packet.readByte() - 1; i >= 0; --i) {
					int playerId = packet.readInt();
					GameCharacter p = self.getPlayerById(playerId);
					if (p != null) { //if there was lag, members may have changed channels or logged off before party was disbanded
						PartyList party = p.getParty();
						p.setParty(null);
						p.getClient().getSession().send(GamePackets.writePartyDisbanded(party.getId(), party.getLeader()));
					}
				}
				break;
			} case InterServerPartyOps.REMOVE_PLAYER: {
				int leaverId = packet.readInt();
				String leaverName = packet.readLengthPrefixedString();
				byte leaverChannel = packet.readByte();
				boolean leaverExpelled = packet.readBool();
				for (int i = packet.readByte() - 1; i >= 0; --i) {
					int playerId = packet.readInt();
					GameCharacter p = self.getPlayerById(playerId);
					if (p != null) {
						PartyList party = p.getParty();
						if (self.getChannelId() == leaverChannel)
							party.removePlayer(self.getPlayerById(leaverId));
						else
							party.removePlayer(leaverChannel, leaverId);
						p.getClient().getSession().send(GamePackets.writePartyMemberLeft(party, leaverId, leaverName, leaverExpelled));
					}
				}
				break;
			} case InterServerPartyOps.ADD_PLAYER: {
				byte joinerCh = packet.readByte();
				int joinerId = packet.readInt();
				if (joinerCh == self.getChannelId()) {
					GameCharacter joiner = self.getPlayerById(joinerId);
					for (int i = packet.readByte() - 1; i >= 0; --i) {
						int playerId = packet.readInt();
						GameCharacter p = self.getPlayerById(playerId);
						if (p != null) {
							PartyList party = p.getParty();
							party.lockWrite();
							try {
								party.addPlayer(new PartyList.LocalMember(joiner));
							} finally {
								party.unlockWrite();
							}
							p.getClient().getSession().send(GamePackets.writePartyMemberJoined(party, joiner.getName()));
							p.pullPartyHp();
							p.pushHpToParty();
						}
					}
				} else {
					String joinerName = packet.readLengthPrefixedString();
					short joinerJob = packet.readShort();
					short joinerLevel = packet.readShort();
					for (int i = packet.readByte() - 1; i >= 0; --i) {
						int playerId = packet.readInt();
						GameCharacter p = self.getPlayerById(playerId);
						if (p != null) {
							PartyList party = p.getParty();
							party.lockWrite();
							try {
								party.addPlayer(new PartyList.RemoteMember(joinerId, joinerName, joinerJob, joinerLevel, joinerCh));
							} finally {
								party.unlockWrite();
							}
							p.getClient().getSession().send(GamePackets.writePartyMemberJoined(party, joinerName));
							p.pullPartyHp();
							p.pushHpToParty();
						}
					}
				}
				break;
			} case InterServerPartyOps.JOIN: {
				GameCharacter p = self.getPlayerById(packet.readInt());
				boolean success = packet.readBool();
				if (success) {
					int partyId = packet.readInt();
					int leader = packet.readInt();
					PartyList party = new PartyList(partyId, leader, p);
					party.lockWrite();
					try {
						for (int i = packet.readByte() - 1; i >= 0; --i) {
							byte ch = packet.readByte();
							int playerId = packet.readInt();
							if (ch == self.getChannelId()) {
								party.addPlayer(new PartyList.LocalMember(self.getPlayerById(playerId)));
							} else {
								String name = packet.readLengthPrefixedString();
								short job = packet.readShort();
								short level = packet.readShort();
								party.addPlayer(new PartyList.RemoteMember(playerId, name, job, level, ch));
							}
						}
					} finally {
						party.unlockWrite();
					}
					if (p != null) {
						p.setParty(party);
						p.getClient().getSession().send(GamePackets.writePartyMemberJoined(party, p.getName()));
						p.pullPartyHp();
						p.pushHpToParty();
					}
				} else {
					p.getClient().getSession().send(GamePackets.writeSimplePartyListMessage(PartyListHandler.PARTY_FULL));
				}
			} case InterServerPartyOps.LEAVE: {
				int leaverId = packet.readInt();
				boolean leaverExpelled = packet.readBool();
				GameCharacter p = self.getPlayerById(leaverId);
				if (p != null) {
					PartyList party = p.getParty();
					party.removePlayer(p);
					p.setParty(null);
					p.getClient().getSession().send(GamePackets.writePartyMemberLeft(party, leaverId, p.getName(), leaverExpelled));
				}
				break;
			} case InterServerPartyOps.CHANGE_LEADER: {
				int newLeader = packet.readInt();
				for (int i = packet.readByte() - 1; i >= 0; --i) {
					int playerId = packet.readInt();
					GameCharacter p = self.getPlayerById(playerId);
					if (p != null) {
						PartyList party = p.getParty();
						party.setLeader(newLeader);
						p.getClient().getSession().send(GamePackets.writePartyChangeLeader(party));
					}
				}
				break;
			}
		}
	}
}
