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
import argonms.common.character.CenterServerSynchronizationOps;
import argonms.common.character.inventory.Inventory;
import argonms.common.net.internal.RemoteCenterOps;
import argonms.common.util.collections.Pair;
import argonms.common.util.input.LittleEndianReader;
import argonms.common.util.output.LittleEndianByteArrayWriter;
import argonms.common.util.output.LittleEndianWriter;
import argonms.game.GameServer;
import argonms.game.character.BuffState.ItemState;
import argonms.game.character.BuffState.MobSkillState;
import argonms.game.character.BuffState.SkillState;
import argonms.game.character.Chatroom;
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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
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

	private static final byte //interchannel opcodes
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
		BUDDY_DELETE = 15,
		CHATROOM_INVITE = 16,
		CHATROOM_INVITE_RESPONSE = 17,
		CHATROOM_DECLINE = 18,
		CHATROOM_TEXT = 19
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

	private final ConcurrentMap<Integer, PartyList> activeLocalParties;
	private final ConcurrentMap<Integer, Chatroom> localChatRooms;

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
		activeLocalParties = new ConcurrentHashMap<Integer, PartyList>();
		localChatRooms = new ConcurrentHashMap<Integer, Chatroom>();
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

	public void changeRemoteChannelPort(byte channel, int port) {
		Byte ch = Byte.valueOf(channel);
		writeLock.lock();
		try {
			remoteChannelPorts.put(ch, Integer.valueOf(port));
		} finally {
			writeLock.unlock();
		}
	}

	private GameCenterInterface getCenterComm() {
		return GameServer.getInstance().getCenterInterface();
	}

	public Pair<byte[], Integer> getChannelHost(byte ch) throws UnknownHostException {
		for (int i = 0; i < localChannels.length; i++) {
			if (ch != localChannels[i])
				continue;

			byte[] address = InetAddress.getByName(GameServer.getInstance().getExternalIp()).getAddress();
			return new Pair<byte[], Integer>(address, Integer.valueOf(GameServer.getChannel(ch).getPort()));
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
			if (destCh != localChannels[i])
				continue;

			GameServer.getChannel(destCh).getInterChannelInterface().receivedInboundPlayer(self.getChannelId(), p.getId(), new PlayerContinuation(p));
			return;
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
			if (networkPeers == 0)
				return 0;

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
				return 0;
			} catch (InterruptedException ex) {
				//propagate the interrupted status further up to our worker
				//executor service and see if they care - we don't care about it
				Thread.currentThread().interrupt();
				return 0;
			} catch (TimeoutException ex) {
				LOG.log(Level.FINE, "Player search timeout", ex);
				return 0;
			} catch (CancellationException ex) {
				return 0;
			} finally {
				responseListeners.remove(Integer.valueOf(responseId));
			}
		} finally {
			if (!readUnlocked)
				readLock.unlock();
		}
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
			}
			case CHAT_TYPE_PARTY: {
				if (p.getParty() == null)
					return;

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
				break;
			}
			case CHAT_TYPE_GUILD: {
				if (p.getGuildId() == 0)
					return;

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
				break;
			}
		}

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
					if (knownCh != localChannels[i])
						continue;

					local = true;
					GameServer.getChannel(knownCh).getInterChannelInterface().receivedPrivateChat(type, recipients, name, message);
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
			if (networkPeers == 0)
				return false;

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
				return false;
			} catch (InterruptedException ex) {
				//propagate the interrupted status further up to our worker
				//executor service and see if they care - we don't care about it
				Thread.currentThread().interrupt();
				return false;
			} catch (TimeoutException ex) {
				LOG.log(Level.FINE, "Whisper timeout", ex);
				return false;
			} catch (CancellationException ex) {
				return false;
			} finally {
				responseListeners.remove(Integer.valueOf(responseId));
			}
		} finally {
			if (!readUnlocked)
				readLock.unlock();
		}
	}

	public void sendSpouseChat(String spouse, GameCharacter p, String message) {
		//perhaps we should compare spouse's name with p.getSpouseId()?
		String name = p.getName();
		int recipient = p.getSpouseId();
		if (recipient == 0)
			return;

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
			if (networkPeers == 0)
				return result;

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
			if (buddy.getChannel() == 0)
				continue;

			Byte ch = Byte.valueOf(buddy.getChannel());
			List<Integer> buddiesOnChannel = buddyChannels.get(ch);
			if (buddiesOnChannel == null) {
				buddiesOnChannel = new ArrayList<Integer>();
				buddyChannels.put(ch, buddiesOnChannel);
			}
			buddiesOnChannel.add(Integer.valueOf(buddy.getId()));
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
				if (ch != localChannels[i])
					continue;

				local = true;
				GameServer.getChannel(ch).getInterChannelInterface().receivedBuddyOffline(p.getId(), recipients);
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
			if (channel != localChannels[i])
				continue;

			GameServer.getChannel(channel).getInterChannelInterface().receivedBuddyDelete(p.getId(), recipient);
			return;
		}
		getCenterComm().getSession().send(writeBuddyDelete(channel, p.getId(), recipient));
	}

	public void sendMakeParty(GameCharacter p) {
		getCenterComm().getSession().send(writeMakeParty(self.getChannelId(), p));
	}

	public void sendDisbandParty(int partyId) {
		getCenterComm().getSession().send(writeDisbandParty(partyId));
	}

	public void sendLeaveParty(GameCharacter p, int partyId) {
		getCenterComm().getSession().send(writeLeaveParty(p, partyId));
	}

	public void sendJoinParty(GameCharacter p, int partyId) {
		getCenterComm().getSession().send(writeJoinParty(p, partyId));
	}

	public void sendExpelPartyMember(PartyList.Member member, int partyId) {
		getCenterComm().getSession().send(writeExpelPartyMember(member.getPlayerId(), member.getName(), partyId, member.getChannel()));
	}

	public void sendChangePartyLeader(int partyId, int newLeader) {
		getCenterComm().getSession().send(writeNewPartyLeader(partyId, newLeader));
	}

	private void fillPartyList(GameCharacter excludeMember, PartyList party) {
		int responseId = nextResponseId.getAndIncrement();
		ResponseFuture response = new ResponseFuture(2);
		responseListeners.put(Integer.valueOf(responseId), response);
		getCenterComm().getSession().send(writeFetchPartyList(self.getChannelId(), excludeMember.getId(), party.getId(), responseId));
		try {
			Object[] array = response.get(2, TimeUnit.SECONDS);
			party.setLeader(((Integer) array[0]).intValue());
			for (Object mem : (PartyList.Member[]) array[1]) {
				if (mem instanceof PartyList.LocalMember)
					party.addPlayer((PartyList.LocalMember) mem);
				else if (mem instanceof PartyList.RemoteMember)
					party.addPlayer((PartyList.RemoteMember) mem);
			}
		} catch (InterruptedException ex) {
			//propagate the interrupted status further up to our worker
			//executor service and see if they care - we don't care about it
			Thread.currentThread().interrupt();
		} catch (TimeoutException ex) {
			LOG.log(Level.FINE, "Party fetch timeout", ex);
		} catch (CancellationException ex) {

		} finally {
			responseListeners.remove(Integer.valueOf(responseId));
		}
	}

	public PartyList sendFetchPartyList(GameCharacter p, int partyId) {
		PartyList newParty = new PartyList(partyId, p, false, false);
		PartyList party;
		//prevent any concurrent accessors from using an uninitialized PartyList
		newParty.lockWrite();
		try {
			party = activeLocalParties.putIfAbsent(Integer.valueOf(partyId), newParty);
			if (party == null) {
				party = newParty;
				fillPartyList(p, party);
			}
		} finally {
			newParty.unlockWrite();
		}
		return party;
	}

	public void sendPartyMemberOnline(GameCharacter p, PartyList party) {
		getCenterComm().getSession().send(writePartyMemberEnteredChannel(self.getChannelId(), p.getId(), party.getId()));
	}

	public void sendPartyMemberOffline(GameCharacter p, boolean loggingOff) {
		getCenterComm().getSession().send(writePartyMemberExitedChannel(self.getChannelId(), p.getId(), p.getParty().getId(), loggingOff));
	}

	public void sendPartyLevelOrJobUpdate(GameCharacter p, boolean level) {
		if (level)
			getCenterComm().getSession().send(writePartyMemberUpdatedStat(self.getChannelId(), p.getId(), p.getParty().getId(), true, p.getLevel()));
		else
			getCenterComm().getSession().send(writePartyMemberUpdatedStat(self.getChannelId(), p.getId(), p.getParty().getId(), false, p.getJob()));
	}

	public void sendMakeChatroom(GameCharacter p) {
		getCenterComm().getSession().send(writeMakeChatroom(p));
	}

	public void sendJoinChatroom(GameCharacter p, int roomId) {
		getCenterComm().getSession().send(writeJoinChatroom(p, roomId));
	}

	public void sendLeaveChatroom(GameCharacter p, Chatroom room) {
		getCenterComm().getSession().send(writeLeaveChatroom(p.getId(), room.getRoomId()));
	}

	public boolean sendChatroomInvite(String inviter, int roomId, String invitee) {
		byte channel = GameServer.getInstance().channelOfPlayer(invitee);
		if (channel != -1) //invitee could be on local process
			return GameServer.getChannel(channel).getInterChannelInterface().receivedChatroomInvite(invitee, roomId, inviter);
		boolean readUnlocked = false;
		readLock.lock();
		try {
			int networkPeers = remoteChannelPorts.size();
			if (networkPeers == 0)
				return false;

			int responseId = nextResponseId.getAndIncrement();
			ResponseFuture response = new ResponseFuture(networkPeers);
			responseListeners.put(Integer.valueOf(responseId), response);
			for (Byte ch : remoteChannelPorts.keySet()) //invitee could be on another process
				getCenterComm().getSession().send(writeChatroomInvite(ch.byteValue(), self.getChannelId(), responseId, invitee, roomId, inviter));
			readLock.unlock();
			readUnlocked = true;
			try {
				Object[] array = response.get(2, TimeUnit.SECONDS);
				for (Object o : array)
					if (((Boolean) o).booleanValue())
						return true;
				return false;
			} catch (InterruptedException ex) {
				//propagate the interrupted status further up to our worker
				//executor service and see if they care - we don't care about it
				Thread.currentThread().interrupt();
				return false;
			} catch (TimeoutException ex) {
				LOG.log(Level.FINE, "Chatroom invite timeout", ex);
				return false;
			} catch (CancellationException ex) {
				return false;
			} finally {
				responseListeners.remove(Integer.valueOf(responseId));
			}
		} finally {
			if (!readUnlocked)
				readLock.unlock();
		}
	}

	public void sendChatroomDecline(String invitee, String inviter) {
		byte channel = GameServer.getInstance().channelOfPlayer(inviter);
		if (channel != -1) { //local process
			GameServer.getChannel(channel).getInterChannelInterface().receivedChatroomDecline(inviter, invitee);
			return;
		}
		for (Byte ch : remoteChannelPorts.keySet()) //another process
			getCenterComm().getSession().send(writeChatroomDecline(ch.byteValue(), inviter, invitee));
	}

	public void sendChatroomText(String text, Chatroom room, int senderPlayerId) {
		Set<Byte> channels;
		room.lockRead();
		try {
			channels = room.allChannels();
		} finally {
			room.unlockRead();
		}
		if (channels.contains(Byte.valueOf(self.getChannelId())))
			receivedChatroomText(text, room.getRoomId(), senderPlayerId); //players on current channel
		for (int i = 0; i < localChannels.length; i++) //players on local process
			if (channels.contains(Byte.valueOf(localChannels[i])))
				GameServer.getChannel(localChannels[i]).getInterChannelInterface().receivedChatroomText(text, room.getRoomId(), senderPlayerId);
		for (Byte ch : remoteChannelPorts.keySet()) //players on other processes
			if (channels.contains(ch))
				getCenterComm().getSession().send(writeChatroomText(ch.byteValue(), text, room.getRoomId(), senderPlayerId));
	}

	public void chatroomPlayerChangingChannels(int playerId, Chatroom room) {
		Set<Byte> others;
		room.lockRead();
		try {
			others = room.localChannelSlots();
			others.remove(Byte.valueOf(room.positionOf(playerId)));
		} finally {
			room.unlockRead();
		}
		//if there are no other players in the chatroom on the
		//channel, delete the chatroom from our cache
		if (others.isEmpty())
			localChatRooms.remove(Integer.valueOf(room.getRoomId()));
	}

	public void sendChatroomPlayerChangedChannels(GameCharacter p, int roomId) {
		getCenterComm().getSession().send(writeChatroomPlayerChangedChannels(p.getId(), roomId, self.getChannelId()));
	}

	public void sendChatroomPlayerLookUpdate(GameCharacter p, int roomId) {
		getCenterComm().getSession().send(writeChatroomPlayerLookUpdate(p, roomId));
	}

	public void receivedInboundPlayer(byte fromCh, int playerId, PlayerContinuation context) {
		self.storePlayerBuffs(playerId, context);

		for (int i = 0; i < localChannels.length; i++) {
			if (fromCh != localChannels[i])
				continue;

			GameServer.getChannel(fromCh).getInterChannelInterface().acceptedInboundPlayer(playerId);
			return;
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
		if (p == null)
			return false;

		p.getClient().getSession().send(GamePackets.writeWhisperMessage(name, message, fromCh));
		return true;
	}

	private boolean receivedSpouseChat(int recipient, String name, String message) {
		GameCharacter p = self.getPlayerById(recipient);
		if (p == null)
			return false;

		p.getClient().getSession().send(GamePackets.writeSpouseChatMessage(name, message));
		return true;
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
		if (p == null)
			return -1;

		BuddyList bList = p.getBuddyList();
		if (bList.isFull())
			return BuddyListHandler.THEIR_LIST_FULL;
		if (bList.getBuddy(inviter) != null)
			return BuddyListHandler.ALREADY_ON_LIST;
		bList.addInvite(inviter, inviterName);
		p.getClient().getSession().send(GamePackets.writeBuddyInvite(inviter, inviterName));
		return Byte.MAX_VALUE;
	}

	private int receivedBuddyOnline(byte fromCh, int sender, int[] receivers) {
		int received = 0;
		for (int receiver : receivers) {
			GameCharacter p = self.getPlayerById(receiver);
			if (p == null)
				continue;

			received++;
			BuddyList bList = p.getBuddyList();
			BuddyListEntry entry = bList.getBuddy(sender);
			//in case we deleted the entry in the meantime...
			if (entry == null)
				continue;

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
		return received;
	}

	private boolean receivedBuddyAccepted(byte fromCh, int sender, int receiver) {
		GameCharacter p = self.getPlayerById(receiver);
		if (p == null)
			return false;

		BuddyList bList = p.getBuddyList();
		BuddyListEntry entry = bList.getBuddy(sender);
		//in case we deleted the entry in the meantime...
		if (entry == null)
			return true;

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
			if (fromCh != localChannels[i])
				continue;

			local = true;
			GameServer.getChannel(fromCh).getInterChannelInterface().receivedBuddyOnlineResponse(self.getChannelId(), sender, receiver, true);
		}
		if (!local)
			getCenterComm().getSession().send(writeBuddyOnlineResponse(fromCh, self.getChannelId(), sender, receiver, true));
		return true;
	}

	private void receivedBuddyOnlineResponse(byte srcCh, int receiver, int sender, boolean notify) {
		GameCharacter p = self.getPlayerById(receiver);
		//in case we logged off or something like that?
		if (p == null)
			return;

		BuddyList bList = p.getBuddyList();
		BuddyListEntry entry = bList.getBuddy(sender);
		//in case we deleted the entry in the meantime...
		if (entry == null)
			return;

		entry.setChannel(srcCh);
		if (notify)
			p.getClient().getSession().send(GamePackets.writeBuddyLoggedIn(entry));
		p.getClient().getSession().send(GamePackets.writeBuddyList(BuddyListHandler.ADD, bList));
	}

	private void receivedBuddyOffline(int sender, int[] receivers) {
		for (int receiver : receivers) {
			GameCharacter p = self.getPlayerById(receiver);
			//in case we logged off or something like that?
			if (p == null)
				continue;

			BuddyList bList = p.getBuddyList();
			BuddyListEntry entry = bList.getBuddy(sender);
			//in case we deleted the entry in the meantime...
			if (entry == null)
				continue;

			entry.setChannel((byte) 0);
			p.getClient().getSession().send(GamePackets.writeBuddyList(BuddyListHandler.REMOVE, bList));
		}
	}

	private void receivedBuddyDelete(int sender, int receiver) {
		GameCharacter p = self.getPlayerById(receiver);
		//in case we logged off or something like that?
		if (p == null)
			return;

		BuddyList bList = p.getBuddyList();
		BuddyListEntry entry = bList.getBuddy(sender);
		//in case we deleted the entry in the meantime...
		if (entry == null)
			return;

		entry.setStatus(BuddyListHandler.STATUS_HALF_OPEN);
		p.getClient().getSession().send(GamePackets.writeBuddyList(BuddyListHandler.REMOVE, bList));
	}

	private boolean receivedChatroomInvite(String invitee, int roomId, String inviter) {
		GameCharacter p = self.getPlayerByName(invitee);
		if (p == null)
			return false;
		p.getClient().getSession().send(GamePackets.writeChatroomInvite(inviter, roomId));
		return true;
	}

	private void receivedChatroomDecline(String inviter, String invitee) {
		GameCharacter p = self.getPlayerByName(inviter);
		if (p != null)
			p.getClient().getSession().send(GamePackets.writeChatroomInviteResponse(Chatroom.ACT_DECLINE, invitee, false));
	}

	private void receivedChatroomText(String text, int roomId, int sender) {
		Chatroom room = localChatRooms.get(Integer.valueOf(roomId));
		if (room == null)
			return;

		room.lockRead();
		try {
			for (Byte pos : room.localChannelSlots()) {
				int playerId = room.getAvatar(pos.byteValue()).getPlayerId();
				GameCharacter p = self.getPlayerById(playerId);
				if (playerId != sender && p != null)
					p.getClient().getSession().send(GamePackets.writeChatroomText(text));
			}
		} finally {
			room.unlockRead();
		}
	}

	private static void writeHeader(LittleEndianWriter lew, byte channel) {
		lew.writeByte(RemoteCenterOps.CROSS_CHANNEL_SYNCHRONIZATION);
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
		lew.writeInt(context.getChatroomId());
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

	private static byte[] writeMakeParty(byte creatorCh, GameCharacter creator) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(13 + creator.getName().length());
		lew.writeByte(RemoteCenterOps.CENTER_SERVER_SYNCHRONIZATION);
		lew.writeByte(CenterServerSynchronizationOps.PARTY_CREATE);
		lew.writeInt(creator.getId());
		lew.writeByte(creatorCh);
		lew.writeLengthPrefixedString(creator.getName());
		lew.writeShort(creator.getJob());
		lew.writeShort(creator.getLevel());
		return lew.getBytes();
	}

	private static byte[] writeDisbandParty(int partyId) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(6);
		lew.writeByte(RemoteCenterOps.CENTER_SERVER_SYNCHRONIZATION);
		lew.writeByte(CenterServerSynchronizationOps.PARTY_DISBAND);
		lew.writeInt(partyId);
		return lew.getBytes();
	}

	private static byte[] writeLeaveParty(GameCharacter p, int partyId) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(14 + p.getName().length());
		lew.writeByte(RemoteCenterOps.CENTER_SERVER_SYNCHRONIZATION);
		lew.writeByte(CenterServerSynchronizationOps.PARTY_REMOVE_PLAYER);
		lew.writeInt(partyId);
		lew.writeInt(p.getId());
		lew.writeByte(p.getClient().getChannel());
		lew.writeLengthPrefixedString(p.getName());
		lew.writeBool(false);
		return lew.getBytes();
	}

	private static byte[] writeExpelPartyMember(int playerId, String playerName, int partyId, byte playerChannel) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(14 + playerName.length());
		lew.writeByte(RemoteCenterOps.CENTER_SERVER_SYNCHRONIZATION);
		lew.writeByte(CenterServerSynchronizationOps.PARTY_REMOVE_PLAYER);
		lew.writeInt(partyId);
		lew.writeInt(playerId);
		lew.writeByte(playerChannel);
		lew.writeLengthPrefixedString(playerName);
		lew.writeBool(true);
		return lew.getBytes();
	}

	private static byte[] writeJoinParty(GameCharacter p, int partyId) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(17 + p.getName().length());
		lew.writeByte(RemoteCenterOps.CENTER_SERVER_SYNCHRONIZATION);
		lew.writeByte(CenterServerSynchronizationOps.PARTY_ADD_PLAYER);
		lew.writeInt(partyId);
		lew.writeInt(p.getId());
		lew.writeByte(p.getClient().getChannel());
		lew.writeLengthPrefixedString(p.getName());
		lew.writeShort(p.getJob());
		lew.writeShort(p.getLevel());
		return lew.getBytes();
	}

	private static byte[] writeNewPartyLeader(int partyId, int newLeader) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(10);
		lew.writeByte(RemoteCenterOps.CENTER_SERVER_SYNCHRONIZATION);
		lew.writeByte(CenterServerSynchronizationOps.PARTY_CHANGE_LEADER);
		lew.writeInt(partyId);
		lew.writeInt(newLeader);
		return lew.getBytes();
	}

	private static byte[] writeFetchPartyList(byte responseCh, int playerId, int partyId, int responseId) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(15);
		lew.writeByte(RemoteCenterOps.CENTER_SERVER_SYNCHRONIZATION);
		lew.writeByte(CenterServerSynchronizationOps.PARTY_FETCH_LIST);
		lew.writeInt(partyId);
		lew.writeInt(playerId);
		lew.writeByte(responseCh);
		lew.writeInt(responseId);
		return lew.getBytes();
	}

	private static byte[] writePartyMemberEnteredChannel(byte targetCh, int entererId, int partyId) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(11);
		lew.writeByte(RemoteCenterOps.CENTER_SERVER_SYNCHRONIZATION);
		lew.writeByte(CenterServerSynchronizationOps.PARTY_MEMBER_CONNECTED);
		lew.writeInt(partyId);
		lew.writeInt(entererId);
		lew.writeByte(targetCh);
		return lew.getBytes();
	}

	private static byte[] writePartyMemberExitedChannel(byte lastCh, int exiterId, int partyId, boolean loggingOff) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(12);
		lew.writeByte(RemoteCenterOps.CENTER_SERVER_SYNCHRONIZATION);
		lew.writeByte(CenterServerSynchronizationOps.PARTY_MEMBER_DISCONNECTED);
		lew.writeInt(partyId);
		lew.writeInt(exiterId);
		lew.writeByte(lastCh);
		lew.writeBool(loggingOff);
		return lew.getBytes();
	}

	private static byte[] writePartyMemberUpdatedStat(byte updatedStatPlayerCh, int updatedStatPlayerId, int partyId, boolean updateLevel, short newValue) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(14);
		lew.writeByte(RemoteCenterOps.CENTER_SERVER_SYNCHRONIZATION);
		lew.writeByte(CenterServerSynchronizationOps.PARTY_MEMBER_STAT_UPDATED);
		lew.writeInt(partyId);
		lew.writeInt(updatedStatPlayerId);
		lew.writeByte(updatedStatPlayerCh);
		lew.writeBool(updateLevel);
		lew.writeShort(newValue);
		return lew.getBytes();
	}

	private static void writeChatroomAvatar(LittleEndianWriter lew, GameCharacter p, Map<Short, Integer> equips) {
		lew.writeInt(p.getId());
		lew.writeByte((byte) equips.size());
		for (Map.Entry<Short, Integer> entry : equips.entrySet()) {
			lew.writeShort(entry.getKey().shortValue());
			lew.writeInt(entry.getValue().intValue());
		}
		lew.writeByte(p.getGender());
		lew.writeByte(p.getSkinColor());
		lew.writeInt(p.getEyes());
		lew.writeInt(p.getHair());
		lew.writeLengthPrefixedString(p.getName());
		lew.writeByte(p.getClient().getChannel());
	}

	private static byte[] writeMakeChatroom(GameCharacter creator) {
		Map<Short, Integer> equips = creator.getInventory(Inventory.InventoryType.EQUIPPED).getItemIds();
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(19 + creator.getName().length() + equips.size() * 6);
		lew.writeByte(RemoteCenterOps.CREATE_CHATROOM);
		writeChatroomAvatar(lew, creator, equips);
		
		return lew.getBytes();
	}

	private static byte[] writeJoinChatroom(GameCharacter p, int roomId) {
		Map<Short, Integer> equips = p.getInventory(Inventory.InventoryType.EQUIPPED).getItemIds();
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(23 + p.getName().length() + equips.size() * 6);
		lew.writeByte(RemoteCenterOps.JOIN_CHATROOM);
		lew.writeInt(roomId);
		writeChatroomAvatar(lew, p, equips);

		return lew.getBytes();
	}

	private static byte[] writeLeaveChatroom(int playerId, int roomId) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(9);
		lew.writeByte(RemoteCenterOps.LEAVE_CHATROOM);
		lew.writeInt(roomId);
		lew.writeInt(playerId);
		return lew.getBytes();
	}

	private static byte[] writeChatroomInvite(byte dest, byte src, int responseId, String invitee, int roomId, String inviter) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(11 + invitee.length() + inviter.length());
		writeHeader(lew, dest);
		lew.writeByte(CHATROOM_INVITE);
		lew.writeLengthPrefixedString(invitee);
		lew.writeInt(roomId);
		lew.writeLengthPrefixedString(inviter);
		lew.writeInt(responseId);
		lew.writeByte(src);
		return lew.getBytes();
	}

	private byte[] writeChatroomInviteResponse(byte channel, int responseId, boolean result) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(8);
		writeHeader(lew, channel);
		lew.writeByte(CHATROOM_INVITE_RESPONSE);
		lew.writeInt(responseId);
		lew.writeBool(result);
		return lew.getBytes();
	}

	private byte[] writeChatroomDecline(byte channel, String inviter, String invitee) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(7 + inviter.length() + invitee.length());
		writeHeader(lew, channel);
		lew.writeByte(CHATROOM_DECLINE);
		lew.writeLengthPrefixedString(inviter);
		lew.writeLengthPrefixedString(invitee);
		return lew.getBytes();
	}

	private byte[] writeChatroomText(byte channel, String text, int roomId, int sender) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(13 + text.length());
		writeHeader(lew, channel);
		lew.writeByte(CHATROOM_TEXT);
		lew.writeLengthPrefixedString(text);
		lew.writeInt(roomId);
		lew.writeInt(sender);
		return lew.getBytes();
	}

	private byte[] writeChatroomPlayerChangedChannels(int playerId, int roomId, byte dest) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(9);
		lew.writeByte(RemoteCenterOps.UPDATE_CHATROOM_AVATAR_CHANNEL);
		lew.writeInt(playerId);
		lew.writeInt(roomId);
		lew.writeByte(dest);
		return lew.getBytes();
	}

	private byte[] writeChatroomPlayerLookUpdate(GameCharacter p, int roomId) {
		Map<Short, Integer> equips = p.getInventory(Inventory.InventoryType.EQUIPPED).getItemIds();
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(19 + 6 * equips.size());
		lew.writeByte(RemoteCenterOps.UPDATE_CHATROOM_AVATAR_LOOK);
		lew.writeInt(p.getId());
		lew.writeInt(roomId);
		lew.writeByte((byte) equips.size());
		for (Map.Entry<Short, Integer> entry : equips.entrySet()) {
			lew.writeShort(entry.getKey().shortValue());
			lew.writeInt(entry.getValue().intValue());
		}
		lew.writeByte(p.getSkinColor());
		lew.writeInt(p.getEyes());
		lew.writeInt(p.getHair());
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
				context.setChatroomId(packet.readInt());
				receivedInboundPlayer(channel, playerId, context);
				break;
			}
			case INBOUND_PLAYER_ACCEPTED: {
				int playerId = packet.readInt();
				acceptedInboundPlayer(playerId);
				break;
			}
			case PLAYER_SEARCH: {
				int responseId = packet.readInt();
				byte fromCh = packet.readByte();
				String query = packet.readLengthPrefixedString();
				getCenterComm().getSession().send(writePlayerSearchResponse(responseId, fromCh, playerOnLocal(query) ? self.getChannelId() : 0));
				break;
			}
			case PLAYER_SEARCH_RESPONSE: {
				int responseId = packet.readInt();
				byte value = packet.readByte();
				ResponseFuture response = responseListeners.get(Integer.valueOf(responseId));
				if (response != null)
					response.set(Byte.valueOf(value));
				else
					LOG.log(Level.FINE, "Received inter channel intercourse response {0} on channel {1} after Future timed out.", new Object[] { responseId, self.getChannelId()});
				break;
			}
			case MULTI_CHAT: {
				byte type = packet.readByte();
				byte amount = packet.readByte();
				int[] recipients = new int[amount];
				for (byte i = 0; i < amount; i++)
					recipients[i] = packet.readInt();
				String name = packet.readLengthPrefixedString();
				String message = packet.readLengthPrefixedString();
				receivedPrivateChat(type, recipients, name, message);
				break;
			}
			case WHISPER_CHAT: {
				int responseId = packet.readInt();
				String recipient = packet.readLengthPrefixedString();
				String sender = packet.readLengthPrefixedString();
				byte fromCh = packet.readByte();
				String message = packet.readLengthPrefixedString();
				boolean result = receivedWhisper(recipient, sender, message, fromCh);
				getCenterComm().getSession().send(writeWhisperResponse(fromCh, responseId, result));
				break;
			}
			case WHISPER_RESPONSE: {
				int responseId = packet.readInt();
				boolean result = packet.readBool();
				ResponseFuture response = responseListeners.get(Integer.valueOf(responseId));
				if (response != null)
					response.set(Boolean.valueOf(result));
				else
					LOG.log(Level.FINE, "Received inter channel intercourse response {0} on channel {1} after Future timed out.", new Object[] { responseId, self.getChannelId()});
				break;
			}
			case SPOUSE_CHAT: {
				int recipient = packet.readInt();
				String name = packet.readLengthPrefixedString();
				String message = packet.readLengthPrefixedString();
				receivedSpouseChat(recipient, name, message);
				break;
			}
			case BUDDY_INVITE: {
				int responseId = packet.readInt();
				byte returnCh = packet.readByte();
				int recipient = packet.readInt();
				int sender = packet.readInt();
				String senderName = packet.readLengthPrefixedString();
				byte result = receivedBuddyInvite(recipient, sender, senderName);
				getCenterComm().getSession().send(writeBuddyInviteResponse(returnCh, responseId, result));
				break;
			}
			case BUDDY_INVITE_RESPONSE: {
				int responseId = packet.readInt();
				byte result = packet.readByte();
				ResponseFuture response = responseListeners.get(Integer.valueOf(responseId));
				if (response != null)
					response.set(Byte.valueOf(result));
				else
					LOG.log(Level.FINE, "Received inter channel intercourse response {0} on channel {1} after Future timed out.", new Object[] { responseId, self.getChannelId()});
				break;
			}
			case BUDDY_ONLINE: {
				byte channel = packet.readByte();
				int sender = packet.readInt();
				byte receiversCount = packet.readByte();
				int[] receivers = new int[receiversCount];
				for (int i = 0; i < receiversCount; i++)
					receivers[i] = packet.readInt();
				receivedBuddyOnline(channel, sender, receivers);
				break;
			}
			case BUDDY_ACCEPTED: {
				byte channel = packet.readByte();
				int sender = packet.readInt();
				int receiver = packet.readInt();
				receivedBuddyAccepted(channel, sender, receiver);
				break;
			}
			case BUDDY_ONLINE_RESPONSE: {
				byte channel = packet.readByte();
				int receiver = packet.readInt();
				int sender = packet.readInt();
				boolean notify = packet.readBool();
				receivedBuddyOnlineResponse(channel, receiver, sender, notify);
				break;
			}
			case BUDDY_OFFLINE: {
				int sender = packet.readInt();
				byte receiversCount = packet.readByte();
				int[] receivers = new int[receiversCount];
				for (int i = 0; i < receiversCount; i++)
					receivers[i] = packet.readInt();
				receivedBuddyOffline(sender, receivers);
				break;
			}
			case BUDDY_DELETE: {
				int sender = packet.readInt();
				int receiver = packet.readInt();
				receivedBuddyDelete(sender, receiver);
				break;
			}
			case CHATROOM_INVITE: {
				String invitee = packet.readLengthPrefixedString();
				int roomId = packet.readInt();
				String inviter = packet.readLengthPrefixedString();
				int responseId = packet.readInt();
				byte returnCh = packet.readByte();
				boolean result = receivedChatroomInvite(invitee, roomId, inviter);
				getCenterComm().getSession().send(writeChatroomInviteResponse(returnCh, responseId, result));
				break;
			}
			case CHATROOM_INVITE_RESPONSE: {
				int responseId = packet.readInt();
				boolean result = packet.readBool();
				ResponseFuture response = responseListeners.get(Integer.valueOf(responseId));
				if (response != null)
					response.set(Boolean.valueOf(result));
				else
					LOG.log(Level.FINE, "Received inter channel intercourse response {0} on channel {1} after Future timed out.", new Object[] { responseId, self.getChannelId()});
				break;
			}
			case CHATROOM_DECLINE: {
				String inviter = packet.readLengthPrefixedString();
				String invitee = packet.readLengthPrefixedString();
				receivedChatroomDecline(inviter, invitee);
				break;
			}
			case CHATROOM_TEXT: {
				String text = packet.readLengthPrefixedString();
				int roomId = packet.readInt();
				int senderId = packet.readInt();
				receivedChatroomText(text, roomId, senderId);
				break;
			}
		}
	}

	public void receivedPartyPacket(LittleEndianReader packet) {
		switch (packet.readByte()) {
			case CenterServerSynchronizationOps.PARTY_CREATE: {
				int partyId = packet.readInt();
				int leaderId = packet.readInt();
				GameCharacter leaderPlayer = self.getPlayerById(leaderId);
				if (leaderPlayer == null)
					return;

				PartyList party = new PartyList(partyId, leaderPlayer, true, true);
				activeLocalParties.put(Integer.valueOf(partyId), party);
				leaderPlayer.setParty(party);
				leaderPlayer.getClient().getSession().send(GamePackets.writePartyCreated(partyId));
				break;
			}
			case CenterServerSynchronizationOps.PARTY_DISBAND: {
				int partyId = packet.readInt();
				PartyList party = activeLocalParties.remove(Integer.valueOf(partyId));
				if (party == null)
					return;

				party.lockRead();
				try {
					for (PartyList.LocalMember mem : party.getMembersInLocalChannel()) {
						mem.getPlayer().setParty(null);
						mem.getPlayer().getClient().getSession().send(GamePackets.writePartyDisbanded(party.getId(), party.getLeader()));
					}
				} finally {
					party.unlockRead();
				}
				break;
			}
			case CenterServerSynchronizationOps.PARTY_REMOVE_PLAYER: {
				int partyId = packet.readInt();
				int leaverId = packet.readInt();
				byte leaverCh = packet.readByte();
				boolean leaverExpelled = packet.readBool();
				//TODO: not safe for activeLocalParties when members
				//concurrently join or leave party, or enter or exit channel
				PartyList party = activeLocalParties.get(Integer.valueOf(partyId));
				if (party == null)
					return;

				boolean removeParty = false;
				String leaverName;
				party.lockWrite();
				try {
					if (self.getChannelId() == leaverCh) {
						GameCharacter leavingPlayer = self.getPlayerById(leaverId);
						leaverName = leavingPlayer.getName();
						party.removePlayer(leavingPlayer);
						removeParty = party.getMembersInLocalChannel().isEmpty();

						leavingPlayer.setParty(null);
						leavingPlayer.getClient().getSession().send(GamePackets.writePartyMemberLeft(party, leaverId, leaverName, leaverExpelled));

						//GameCenterPacketProcessor.processPartyMemberLeft
						//has an explanation for this
						if (removeParty) {
							boolean save;
							party.lockRead();
							try {
								save = party.allOffline();
							} finally {
								party.unlockRead();
							}
							if (save)
								leavingPlayer.saveCharacter();
						}
					} else {
						leaverName = packet.readLengthPrefixedString();
						party.removePlayer(leaverCh, leaverId);
					}
				} finally {
					party.unlockWrite();
				}
				if (removeParty) {
					activeLocalParties.remove(Integer.valueOf(partyId));
				} else {
					party.lockRead();
					try {
						for (PartyList.LocalMember mem : party.getMembersInLocalChannel())
							mem.getPlayer().getClient().getSession().send(GamePackets.writePartyMemberLeft(party, leaverId, leaverName, leaverExpelled));
					} finally {
						party.unlockRead();
					}
				}
				break;
			}
			case CenterServerSynchronizationOps.PARTY_ADD_PLAYER: {
				int partyId = packet.readInt();
				int joinerId = packet.readInt();
				byte joinerCh = packet.readByte();
				if (joinerCh == self.getChannelId()) {
					GameCharacter joiningPlayer = self.getPlayerById(joinerId);
					PartyList newParty = new PartyList(partyId, joiningPlayer, true, false);
					PartyList party = activeLocalParties.putIfAbsent(Integer.valueOf(partyId), newParty);
					if (party == null) {
						//only can happen if the leader changed channels or
						//logged off. is that even a valid case?
						party = newParty;
						party.lockWrite();
						try {
							fillPartyList(joiningPlayer, party);
						} finally {
							party.unlockWrite();
						}
					} else {
						party.lockWrite();
						try {
							party.addPlayer(new PartyList.LocalMember(joiningPlayer));
						} finally {
							party.unlockWrite();
						}
						party.lockRead();
						try {
							for (PartyList.LocalMember mem : party.getMembersInLocalChannel()) {
								mem.getPlayer().getClient().getSession().send(GamePackets.writePartyMemberJoined(party, joiningPlayer.getName()));
								mem.getPlayer().pullPartyHp();
								mem.getPlayer().pushHpToParty();
							}
						} finally {
							party.unlockRead();
						}
					}
					joiningPlayer.setParty(party);
				} else {
					String joinerName = packet.readLengthPrefixedString();
					short joinerJob = packet.readShort();
					short joinerLevel = packet.readShort();
					PartyList party = activeLocalParties.get(Integer.valueOf(partyId));
					if (party != null) {
						party.lockWrite();
						try {
							party.addPlayer(new PartyList.RemoteMember(joinerId, joinerName, joinerJob, joinerLevel, joinerCh));
						} finally {
							party.unlockWrite();
						}
						party.lockRead();
						try {
							for (PartyList.LocalMember mem : party.getMembersInLocalChannel())
								mem.getPlayer().getClient().getSession().send(GamePackets.writePartyMemberJoined(party, joinerName));
						} finally {
							party.unlockRead();
						}
					}
				}
				break;
			}
			case CenterServerSynchronizationOps.PARTY_JOIN_ERROR: {
				int joinFailedPlayerId = packet.readInt();
				GameCharacter joinFailedPlayer = self.getPlayerById(joinFailedPlayerId);
				if (joinFailedPlayer != null)
					joinFailedPlayer.getClient().getSession().send(GamePackets.writeSimplePartyListMessage(PartyListHandler.PARTY_FULL));
				break;
			}
			case CenterServerSynchronizationOps.PARTY_CHANGE_LEADER: {
				int partyId = packet.readInt();
				int newLeader = packet.readInt();
				PartyList party = activeLocalParties.get(Integer.valueOf(partyId));
				if (party == null)
					return;

				party.setLeader(newLeader);
				party.lockRead();
				try {
					for (PartyList.LocalMember mem : party.getMembersInLocalChannel())
						mem.getPlayer().getClient().getSession().send(GamePackets.writePartyChangeLeader(newLeader, false));
				} finally {
					party.unlockRead();
				}
				break;
			}
			case CenterServerSynchronizationOps.PARTY_FETCH_LIST: {
				int responseId = packet.readInt();
				ResponseFuture response = responseListeners.get(Integer.valueOf(responseId));
				if (response != null) {
					int leader = packet.readInt();
					response.set(Integer.valueOf(leader));
					byte count = packet.readByte();
					PartyList.Member[] members = new PartyList.Member[count];
					for (int i = 0; i < count; i++) {
						int memberId = packet.readInt();
						byte memberCh = packet.readByte();
						if (memberCh == self.getChannelId()) {
							members[i] = new PartyList.LocalMember(self.getPlayerById(memberId));
						} else {
							String memberName = packet.readLengthPrefixedString();
							short memberJob = packet.readShort();
							short memberLevel = packet.readShort();
							members[i] = new PartyList.RemoteMember(memberId, memberName, memberJob, memberLevel, memberCh);
						}
					}
					response.set(members);
				} else {
					LOG.log(Level.FINE, "Received inter channel intercourse response {0} on channel {1} after Future timed out.", new Object[] { responseId, self.getChannelId()});
				}
				break;
			}
			case CenterServerSynchronizationOps.PARTY_MEMBER_CONNECTED: {
				int partyId = packet.readInt();
				int entererId = packet.readInt();
				byte targetCh = packet.readByte();

				if (targetCh == self.getChannelId()) {
					GameCharacter enteringPlayer = self.getPlayerById(entererId);
					PartyList newParty = new PartyList(partyId, enteringPlayer, true, false);
					PartyList party = activeLocalParties.putIfAbsent(Integer.valueOf(partyId), newParty);
					if (party == null) {
						//this should never happen, since we always call
						//sendFetchPartyList when we load the character, which
						//happens before we call sendPartyMemberEnteredChannel
						party = newParty;
						party.lockWrite();
						try {
							fillPartyList(enteringPlayer, party);
						} finally {
							party.unlockWrite();
						}
					} else {
						party.lockWrite();
						try {
							party.memberConnected(self.getPlayerById(entererId));
						} finally {
							party.unlockWrite();
						}
						party.lockRead();
						try {
							for (PartyList.LocalMember mem : party.getMembersInLocalChannel()) {
								mem.getPlayer().getClient().getSession().send(GamePackets.writePartyList(party));
								mem.getPlayer().pullPartyHp();
								mem.getPlayer().pushHpToParty();
							}
						} finally {
							party.unlockRead();
						}
					}
				} else {
					PartyList party = activeLocalParties.get(Integer.valueOf(partyId));
					if (party != null) {
						party.lockWrite();
						try {
							party.memberConnected(new PartyList.RemoteMember(party.getOfflineMember(entererId), targetCh));
						} finally {
							party.unlockWrite();
						}
						party.lockRead();
						try {
							for (PartyList.LocalMember mem : party.getMembersInLocalChannel())
								mem.getPlayer().getClient().getSession().send(GamePackets.writePartyList(party));
						} finally {
							party.unlockRead();
						}
					}
				}
				break;
			}
			case CenterServerSynchronizationOps.PARTY_MEMBER_DISCONNECTED: {
				int partyId = packet.readInt();
				int exiterId = packet.readInt();
				byte lastCh = packet.readByte();
				boolean loggingOff = packet.readBool();
				//TODO: not safe for activeLocalParties when members
				//concurrently join or leave party, or enter or exit channel
				PartyList party = activeLocalParties.get(Integer.valueOf(partyId));
				if (party == null)
					return;

				boolean removeParty = false;
				party.lockWrite();
				try {
					if (lastCh == self.getChannelId()) {
						party.memberDisconnected(self.getPlayerById(exiterId));
						removeParty = party.getMembersInLocalChannel().isEmpty();
					} else {
						party.memberDisconnected(lastCh, exiterId);
					}
				} finally {
					party.unlockWrite();
				}
				if (removeParty) {
					activeLocalParties.remove(Integer.valueOf(partyId));
				} else if (loggingOff) {
					party.lockRead();
					try {
						for (PartyList.LocalMember mem : party.getMembersInLocalChannel())
							mem.getPlayer().getClient().getSession().send(GamePackets.writePartyList(party));
					} finally {
						party.unlockRead();
					}
				}
				break;
			}
			case CenterServerSynchronizationOps.PARTY_MEMBER_STAT_UPDATED: {
				int partyId = packet.readInt();
				int updatedPlayerId = packet.readInt();
				byte updatedPlayerCh = packet.readByte();
				PartyList party = activeLocalParties.get(Integer.valueOf(partyId));
				if (party == null)
					return;

				party.lockRead();
				try {
					if (updatedPlayerCh != self.getChannelId()) {
						boolean levelUpdated = packet.readBool();
						short newValue = packet.readShort();
						PartyList.RemoteMember member = party.getMember(updatedPlayerCh, updatedPlayerId);
						if (levelUpdated)
							member.setLevel(newValue);
						else
							member.setJob(newValue);
					}
					for (PartyList.LocalMember mem : party.getMembersInLocalChannel())
						mem.getPlayer().getClient().getSession().send(GamePackets.writePartyList(party));
				} finally {
					party.unlockRead();
				}
				break;
			}
		}
	}

	public void receivedChatroomCreated(LittleEndianReader packet) {
		int roomId = packet.readInt();
		int creatorId = packet.readInt();
		GameCharacter p = self.getPlayerById(creatorId);
		if (p == null)
			return;

		Chatroom room = new Chatroom(roomId, p);
		localChatRooms.put(Integer.valueOf(roomId), room);
		p.setChatRoom(room);
	}

	public void receivedChatroomAssignment(LittleEndianReader packet) {
		int roomId = packet.readInt();
		int playerId = packet.readInt();
		byte position = packet.readByte();
		GameCharacter p = self.getPlayerById(playerId);
		if (roomId != 0) {
			//this will be false if joiner was in the room before and just changed channels.
			boolean firstTime = packet.readBool();
			if (position == -1)
				return; //room is full

			Set<Byte> existingLocalSlots;
			Chatroom room = localChatRooms.get(Integer.valueOf(roomId));
			if (room == null) { //first on this channel to join the chat room
				room = new Chatroom(roomId);
				for (byte pos = 0; pos < 3; pos++) {
					byte channel = packet.readByte();
					if (channel == 0)
						continue;

					int avatarPlayerId = packet.readInt();
					Map<Short, Integer> equips = new HashMap<Short, Integer>();
					for (byte i = packet.readByte(); i > 0; i--) {
						short slot = packet.readShort();
						int itemId = packet.readInt();
						equips.put(Short.valueOf(slot), Integer.valueOf(itemId));
					}
					byte gender = packet.readByte();
					byte skin = packet.readByte();
					int eyes = packet.readInt();
					int hair = packet.readInt();
					String name = packet.readLengthPrefixedString();
					room.setAvatar(pos, new Chatroom.Avatar(avatarPlayerId, gender, skin, eyes, hair, equips, name, channel), false);
				}
				//defer adding Chatroom to cache until after initialization to
				//avoid write locking for room.setAvatar above
				localChatRooms.put(Integer.valueOf(roomId), room);
				existingLocalSlots = Collections.emptySet();
			} else {
				room.lockRead();
				try {
					existingLocalSlots = room.localChannelSlots();
				} finally {
					room.unlockRead();
				}
			}
			Chatroom.Avatar a = new Chatroom.Avatar(p);
			room.lockWrite();
			try {
				room.setAvatar(position, a, true);
			} finally {
				room.unlockWrite();
			}
			p.setChatRoom(room);

			//Center server will not send us a CHATROOM_SLOT_CHANGED if the
			//joiner is on the same channel as us, to save bandwidth.
			//so send joiner's avatar to any other player in the chatroom on
			//the channel
			byte[] message = GamePackets.writeChatroomAvatar(firstTime ? Chatroom.ACT_OPEN : Chatroom.ACT_REFRESH_AVATAR, position, a, firstTime);
			room.lockRead();
			try {
				for (Byte pos : existingLocalSlots) {
					GameCharacter other = self.getPlayerById(room.getAvatar(pos.byteValue()).getPlayerId());
					if (other != null)
						other.getClient().getSession().send(message);
				}
			} finally {
				room.unlockRead();
			}

			if (firstTime) {
				//send other avatars to joiner
				p.getClient().getSession().send(GamePackets.writeChatroomJoin(position));
				room.lockRead();
				try {
					for (byte pos = 0; pos < 3; pos++) {
						a = room.getAvatar(pos);
						if (position != pos && a != null)
							p.getClient().getSession().send(GamePackets.writeChatroomAvatar(Chatroom.ACT_OPEN, pos, a, false));
					}
				} finally {
					room.unlockRead();
				}
			} else {
				//client doesn't update avatar tooltip for its own player
				//after it changes channels...
				p.getClient().getSession().send(GamePackets.writeChatroomAvatar(Chatroom.ACT_REFRESH_AVATAR, position, a, false));
			}
		} else { //left room
			if (p == null)
				return;

			Chatroom room = p.getChatRoom();
			p.setChatRoom(null);
			if (room == null)
				return;

			room.lockWrite();
			try {
				room.setAvatar(position, null, true);
			} finally {
				room.unlockWrite();
			}

			//Center server will not send us a CHATROOM_SLOT_CHANGED if the
			//leaver is on the same channel as us, to save bandwidth.
			//so notify any other player in the chatroom on the channel
			//that the slot is now unoccupied.
			byte[] message = GamePackets.writeChatroomClear(position);
			room.lockRead();
			try {
				for (Byte pos : room.localChannelSlots()) {
					GameCharacter other = self.getPlayerById(room.getAvatar(pos.byteValue()).getPlayerId());
					if (other != null)
						other.getClient().getSession().send(message);
				}
			} finally {
				room.unlockRead();
			}
			//if there are no other players in the chatroom on the
			//channel, delete the chatroom from our cache
			room.lockRead();
			try {
				if (room.localChannelSlots().isEmpty())
					localChatRooms.remove(Integer.valueOf(room.getRoomId()));
			} finally {
				room.unlockRead();
			}
		}
	}

	public void receivedChatroomAvatarChanged(LittleEndianReader packet) {
		int roomId = packet.readInt();
		byte position = packet.readByte();
		int playerId = packet.readInt();
		Chatroom room = localChatRooms.get(Integer.valueOf(roomId));
		if (room == null)
			return;
		if (playerId != 0) {
			byte channel = packet.readByte();
			Map<Short, Integer> equips = new HashMap<Short, Integer>();
			for (byte i = packet.readByte(); i > 0; i--) {
				short slot = packet.readShort();
				int itemId = packet.readInt();
				equips.put(Short.valueOf(slot), Integer.valueOf(itemId));
			}
			byte gender = packet.readByte();
			byte skin = packet.readByte();
			int eyes = packet.readInt();
			int hair = packet.readInt();
			String name = packet.readLengthPrefixedString();
			byte[] message;
			Set<Byte> slotsToNotify;

			room.lockWrite();
			try {
				Chatroom.Avatar a = room.getAvatar(position);
				boolean firstTime = (a == null);
				assert firstTime || a.getPlayerId() == playerId;
				a = new Chatroom.Avatar(playerId, gender, skin, eyes, hair, equips, name, channel);
				message = GamePackets.writeChatroomAvatar(firstTime ? Chatroom.ACT_OPEN : Chatroom.ACT_REFRESH_AVATAR, position, a, firstTime);
				//Chatroom.Avatar is immutable...
				room.setAvatar(position, a, channel == self.getChannelId());
				slotsToNotify = room.localChannelSlots();
			} finally {
				room.unlockWrite();
			}

			slotsToNotify.remove(Byte.valueOf(position));
			room.lockRead();
			try {
				for (Byte pos : slotsToNotify) {
					GameCharacter other = self.getPlayerById(room.getAvatar(pos.byteValue()).getPlayerId());
					if (other != null)
						other.getClient().getSession().send(message);
				}
			} finally {
				room.unlockRead();
			}
		} else { //remove slot
			room.lockWrite();
			try {
				room.setAvatar(position, null, false);
			} finally {
				room.unlockWrite();
			}

			byte[] message = GamePackets.writeChatroomClear(position);
			room.lockRead();
			try {
				for (Byte pos : room.localChannelSlots()) {
					GameCharacter other = self.getPlayerById(room.getAvatar(pos.byteValue()).getPlayerId());
					if (other != null)
						other.getClient().getSession().send(message);
				}
				assert !room.localChannelSlots().isEmpty();
			} finally {
				room.unlockRead();
			}
		}
	}
}
