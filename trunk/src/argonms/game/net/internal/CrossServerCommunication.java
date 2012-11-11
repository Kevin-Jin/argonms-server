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
import argonms.common.util.collections.LockableMap;
import argonms.common.util.collections.Pair;
import argonms.common.util.input.LittleEndianReader;
import argonms.game.character.GameCharacter;
import argonms.game.character.PartyList;
import argonms.game.character.PlayerContinuation;
import argonms.game.net.WorldChannel;
import argonms.game.net.external.GamePackets;
import argonms.game.net.external.handler.BuddyListHandler;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author GoldenKevin
 */
public class CrossServerCommunication {
	private static final Logger LOG = Logger.getLogger(CrossServerCommunication.class.getName());

	private static final byte
		PRIVATE_CHAT_TYPE_BUDDY = 0,
		PRIVATE_CHAT_TYPE_PARTY = 1,
		PRIVATE_CHAT_TYPE_GUILD = 2
	;

	/* package-private */ static final int BLOCKING_CALL_TIMEOUT = 2000; //in milliseconds

	private final LockableMap<Byte, CrossChannelCommunication> allChannelsInWorld;
	private final LockableMap<Byte, CrossProcessCrossChannelCommunication> remoteChannelsInWorld;
	private CenterServerSynchronization partiesAndChatRooms;
	private final ReadWriteLock locks;
	private final WorldChannel self;

	public CrossServerCommunication(WorldChannel channel) {
		//some send methods have an early out only if target channel is on the same process,
		//so current channel and same process channels should be first to be iterated.
		//since initializeLocalChannels is called before addRemoteChannels, this should be
		//true as long as otherChannelsInWorld is in insertion order, so use a LinkedHashMap
		allChannelsInWorld = new LockableMap<Byte, CrossChannelCommunication>(new LinkedHashMap<Byte, CrossChannelCommunication>());
		remoteChannelsInWorld = new LockableMap<Byte, CrossProcessCrossChannelCommunication>(new HashMap<Byte, CrossProcessCrossChannelCommunication>());
		locks = new ReentrantReadWriteLock();
		self = channel;
	}

	/* package-private */ void lockWrite() {
		locks.writeLock().lock();
	}

	/* package-private */ void unlockWrite() {
		locks.writeLock().unlock();
	}

	/* package-private */ void lockRead() {
		locks.readLock().lock();
	}

	/* package-private */ void unlockRead() {
		locks.readLock().unlock();
	}

	//instantiate all CrossServerCommunication for game server and assign them to Map<Byte, CrossServerCommunication> allChannels;
	//Map<Byte, CrossServerCommunication> initialized = empty hashmap;
	//in each iteration over allChannels: this.initializeLocalChannels(initialized) then initialized.put(channel, this)
	public void initializeLocalChannels(Map<Byte, CrossServerCommunication> initialized) {
		lockWrite();
		try {
			SameProcessCrossChannelCommunication source = new SameProcessCrossChannelCommunication(this, self.getChannelId(), self.getChannelId());
			SameProcessCrossChannelCommunication sink = new SameProcessCrossChannelCommunication(this, self.getChannelId(), self.getChannelId());
			sink.connect(source);
			this.allChannelsInWorld.put(Byte.valueOf(self.getChannelId()), sink);

			for (Map.Entry<Byte, CrossServerCommunication> other : initialized.entrySet()) {
				source = new SameProcessCrossChannelCommunication(other.getValue(), other.getKey().byteValue(), self.getChannelId());
				sink = new SameProcessCrossChannelCommunication(other.getValue(), self.getChannelId(), other.getKey().byteValue());
				sink.connect(source);
				other.getValue().allChannelsInWorld.put(Byte.valueOf(self.getChannelId()), source);
				this.allChannelsInWorld.put(other.getKey(), sink);
			}

			//this could be called in the ctor, but that would leak "this"
			partiesAndChatRooms = new CenterServerSynchronization(this, self);
		} finally {
			unlockWrite();
		}
	}

	public Set<Byte> localChannels() {
		lockRead();
		try {
			Set<Byte> local = new HashSet<Byte>(allChannelsInWorld.keySet());
			local.removeAll(remoteChannelsInWorld.keySet());
			return local;
		} finally {
			unlockRead();
		}
	}

	public Set<Byte> remoteChannels() {
		lockRead();
		try {
			return new HashSet<Byte>(remoteChannelsInWorld.keySet());
		} finally {
			unlockRead();
		}
	}

	public void addRemoteChannels(byte[] host, Map<Byte, Integer> ports) {
		lockWrite();
		try {
			for (Map.Entry<Byte, Integer> port : ports.entrySet()) {
				CrossProcessCrossChannelCommunication cpc = new CrossProcessCrossChannelCommunication(this, self.getChannelId(), port.getKey().byteValue(), host, port.getValue().intValue());
				remoteChannelsInWorld.put(port.getKey(), cpc);
				allChannelsInWorld.put(port.getKey(), cpc);
			}
		} finally {
			unlockWrite();
		}
	}

	public void removeRemoteChannels(Set<Byte> channels) {
		lockWrite();
		try {
			for (Byte ch : channels) {
				remoteChannelsInWorld.remove(ch);
				allChannelsInWorld.remove(ch);
			}
		} finally {
			unlockWrite();
		}
	}

	public void changeRemoteChannelPort(byte channel, int port) {
		lockWrite();
		try {
			remoteChannelsInWorld.get(Byte.valueOf(channel)).setPort(port);
		} finally {
			unlockWrite();
		}
	}

	public void receivedCrossProcessCrossChannelCommunicationPacket(LittleEndianReader packet) {
		byte srcCh = packet.readByte();
		remoteChannelsInWorld.getWhenSafe(Byte.valueOf(srcCh)).receivedCrossProcessCrossChannelCommunicationPacket(packet);
	}

	public void sendChannelChangeRequest(byte destCh, GameCharacter p) {
		allChannelsInWorld.getWhenSafe(Byte.valueOf(destCh)).sendPlayerContext(p.getId(), new PlayerContinuation(p));
	}

	/* package-private */ void receivedChannelChangeRequest(byte srcCh, int playerId, PlayerContinuation context) {
		self.storePlayerBuffs(playerId, context);
		sendChannelChangeAcceptance(srcCh, playerId);
	}

	public void sendChannelChangeAcceptance(byte destCh, int playerId) {
		allChannelsInWorld.getWhenSafe(Byte.valueOf(destCh)).sendChannelChangeAcceptance(playerId);
	}

	/* package-private */ void receivedChannelChangeAcceptance(byte srcCh, int playerId) {
		self.performChannelChange(playerId);
	}

	public byte scanChannelOfPlayer(String name) {
		BlockingQueue<Pair<Byte, Object>> queue = new LinkedBlockingQueue<Pair<Byte, Object>>();
		lockRead();
		try {
			int remaining = 0;
			for (CrossChannelCommunication ccc : allChannelsInWorld.values()) {
				ccc.callPlayerExistsCheck(queue, name);
				Pair<Byte, Object> result;
				//address any results that have since responded, for a possibility of early out
				while ((result = queue.poll()) != null && ((Boolean) result.right).booleanValue())
					return result.left.byteValue();
				remaining++;
			}

			long limit = System.currentTimeMillis() + 2000;
			while (remaining > 0) {
				Pair<Byte, Object> result = queue.poll(limit - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
				if (result == null) {
					LOG.log(Level.FINE, "Cross process player search timeout after " + BLOCKING_CALL_TIMEOUT + " milliseconds");
					return 0;
				}
				if (((Boolean) result.right).booleanValue())
					return result.left.byteValue();
			}
			return 0;
		} catch (InterruptedException e) {
			//propagate the interrupted status further up to our worker
			//executor service and see if they care - we don't care about it
			Thread.currentThread().interrupt();
			return 0;
		} finally {
			unlockRead();
		}
	}

	/* package-private */ boolean makePlayerExistsResult(String name) {
		return self.getPlayerByName(name) != null;
	}

	public void sendPrivateChat(byte type, int[] recipients, GameCharacter p, String message) {
		String name = p.getName();
		Map<Byte, List<Integer>> peerChannels = null;

		switch (type) {
			case PRIVATE_CHAT_TYPE_BUDDY: {
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
			case PRIVATE_CHAT_TYPE_PARTY: {
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
			case PRIVATE_CHAT_TYPE_GUILD: {
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

		for (Map.Entry<Byte, List<Integer>> entry : peerChannels.entrySet()) {
			byte ch = entry.getKey().byteValue();
			int i = 0;
			recipients = new int[entry.getValue().size()];
			for (Integer recipient : entry.getValue())
				recipients[i++] = recipient.intValue();

			if (ch != 0) {
				//we already know the destination channel, and don't need to channel scan
				allChannelsInWorld.getWhenSafe(Byte.valueOf(ch)).sendPrivateChat(type, recipients, name, message);
			} else {
				//channel scan for these players
				lockRead();
				try {
					for (CrossChannelCommunication ccc : allChannelsInWorld.values())
						ccc.sendPrivateChat(type, recipients, name, message);
				} finally {
					unlockRead();
				}
			}
		}
	}

	/* package-private */ void receivedPrivateChat(byte type, int[] recipients, String name, String message) {
		GameCharacter p;
		for (int i = 0; i < recipients.length; i++) {
			p = self.getPlayerById(recipients[i]);
			if (p != null)
				p.getClient().getSession().send(GamePackets.writePrivateChatMessage(type, name, message));
		}
	}

	public boolean sendWhisper(String recipient, String sender, String message) {
		BlockingQueue<Pair<Byte, Object>> queue = new LinkedBlockingQueue<Pair<Byte, Object>>();
		lockRead();
		try {
			int remaining = 0;
			for (CrossChannelCommunication ccc : allChannelsInWorld.values()) {
				ccc.callSendWhisper(queue, recipient, sender, message);
				Pair<Byte, Object> result;
				//address any results that have since responded, for a possibility of early out
				while ((result = queue.poll()) != null && ((Boolean) result.right).booleanValue())
					return true;
				remaining++;
			}

			long limit = System.currentTimeMillis() + 2000;
			while (remaining > 0) {
				Pair<Byte, Object> result = queue.poll(limit - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
				if (result == null) {
					LOG.log(Level.FINE, "Cross process whisper timeout after " + BLOCKING_CALL_TIMEOUT + " milliseconds");
					return false;
				}
				if (((Boolean) result.right).booleanValue())
					return true;
			}
			return false;
		} catch (InterruptedException e) {
			//propagate the interrupted status further up to our worker
			//executor service and see if they care - we don't care about it
			Thread.currentThread().interrupt();
			return false;
		} finally {
			unlockRead();
		}
	}

	/* package-private */ boolean makeWhisperResult(String recipient, String sender, String message, byte srcCh) {
		GameCharacter p = self.getPlayerByName(recipient);
		if (p == null)
			return false;

		p.getClient().getSession().send(GamePackets.writeWhisperMessage(sender, message, srcCh));
		return true;
	}

	public void sendSpouseChat(String spouse, GameCharacter p, String message) {
		//perhaps we should compare spouse's name with p.getSpouseId()?
		String name = p.getName();
		int recipient = p.getSpouseId();
		if (recipient == 0)
			return;

		lockRead();
		try {
			for (CrossChannelCommunication ccc : allChannelsInWorld.values())
				if (ccc.sendSpouseChat(recipient, name, message))
					break;
		} finally {
			unlockRead();
		}
	}

	/* package-private */ boolean receivedSpouseChat(int recipient, String sender, String message) {
		GameCharacter p = self.getPlayerById(recipient);
		if (p == null)
			return false;

		p.getClient().getSession().send(GamePackets.writeSpouseChatMessage(sender, message));
		return true;
	}

	public byte sendBuddyInvite(GameCharacter sender, int recipientId) {
		BlockingQueue<Pair<Byte, Object>> queue = new LinkedBlockingQueue<Pair<Byte, Object>>();
		lockRead();
		try {
			int remaining = 0;
			for (CrossChannelCommunication ccc : allChannelsInWorld.values()) {
				ccc.callSendBuddyInvite(queue, recipientId, sender.getId(), sender.getName());
				Pair<Byte, Object> result;
				byte inviteResult;
				//address any results that have since responded, for a possibility of early out
				while ((result = queue.poll()) != null && (inviteResult = ((Byte) result.right).byteValue()) != -1)
					return inviteResult;
				remaining++;
			}

			long limit = System.currentTimeMillis() + BLOCKING_CALL_TIMEOUT;
			while (remaining > 0) {
				Pair<Byte, Object> result = queue.poll(limit - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
				if (result == null) {
					LOG.log(Level.FINE, "Cross process buddy invite timeout after " + BLOCKING_CALL_TIMEOUT + " milliseconds");
					return 0;
				}
				byte inviteResult = ((Byte) result.right).byteValue();
				if (inviteResult != -1)
					return inviteResult;
			}
			return 0;
		} catch (InterruptedException e) {
			//propagate the interrupted status further up to our worker
			//executor service and see if they care - we don't care about it
			Thread.currentThread().interrupt();
			return 0;
		} finally {
			unlockRead();
		}
	}

	/* package-private */ byte receivedBuddyInvite(int recipientId, int senderId, String senderName) {
		GameCharacter p = self.getPlayerById(recipientId);
		if (p == null)
			return -1;

		BuddyList bList = p.getBuddyList();
		if (bList.isFull())
			return BuddyListHandler.THEIR_LIST_FULL;
		if (bList.getBuddy(senderId) != null)
			return BuddyListHandler.ALREADY_ON_LIST;
		bList.addInvite(senderId, senderName);
		p.getClient().getSession().send(GamePackets.writeBuddyInvite(senderId, senderName));
		return Byte.MAX_VALUE;
	}

	private void sendReturnBuddyLogInNotifications(byte destCh, int recipient, List<Integer> senders, boolean bubble) {
		allChannelsInWorld.getWhenSafe(Byte.valueOf(destCh)).sendReturnBuddyLogInNotifications(recipient, senders, bubble);
	}

	/* package-private */ void receivedReturnedBuddyLogInNotifications(int recipient, List<Integer> senders, boolean bubble, byte srcCh) {
		GameCharacter p = self.getPlayerById(recipient);
		//in case we logged off or something like that?
		if (p == null)
			return;

		BuddyList bList = p.getBuddyList();
		for (Integer sender : senders) {
			BuddyListEntry entry = bList.getBuddy(sender.intValue());
			//in case we deleted the entry in the meantime...
			if (entry == null)
				continue;

			entry.setChannel(srcCh);
			if (bubble)
				p.getClient().getSession().send(GamePackets.writeBuddyLoggedIn(entry));
		}
		p.getClient().getSession().send(GamePackets.writeBuddyList(BuddyListHandler.ADD, bList));
	}

	public void sendExchangeBuddyLogInNotifications(GameCharacter p) {
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

		lockRead();
		try {
			for (CrossChannelCommunication ccc : allChannelsInWorld.values())
				if ((remaining -= ccc.exchangeBuddyLogInNotifications(p.getId(), recipients)) <= 0)
					break;
		} finally {
			unlockRead();
		}
	}

	/* package-private */ int receivedSentBuddyLogInNotifications(int sender, int[] recipients, byte srcCh) {
		List<Integer> localRecipients = new ArrayList<Integer>();
		for (int recipient : recipients) {
			GameCharacter p = self.getPlayerById(recipient);
			if (p == null)
				continue;

			localRecipients.add(Integer.valueOf(recipient));
			BuddyList bList = p.getBuddyList();
			BuddyListEntry entry = bList.getBuddy(sender);
			//in case we deleted the entry in the meantime...
			if (entry == null)
				continue;

			entry.setChannel(srcCh);
			p.getClient().getSession().send(GamePackets.writeBuddyLoggedIn(entry));
			p.getClient().getSession().send(GamePackets.writeBuddyList(BuddyListHandler.ADD, bList));
		}

		sendReturnBuddyLogInNotifications(srcCh, sender, localRecipients, false);
		return localRecipients.size();
	}

	public void sendBuddyAccepted(GameCharacter p, int recipient) {
		lockRead();
		try {
			for (CrossChannelCommunication ccc : allChannelsInWorld.values())
				if (ccc.sendBuddyAccepted(p.getId(), recipient))
					break;
		} finally {
			unlockRead();
		}
	}

	/* package-private */ boolean receivedBuddyAccepted(int sender, int recipient, byte srcCh) {
		GameCharacter p = self.getPlayerById(recipient);
		if (p == null)
			return false;

		BuddyList bList = p.getBuddyList();
		BuddyListEntry entry = bList.getBuddy(sender);
		//in case we deleted the entry in the meantime...
		if (entry == null)
			return true;

		entry.setStatus(BuddyListHandler.STATUS_MUTUAL);
		entry.setChannel(srcCh);
		p.getClient().getSession().send(GamePackets.writeBuddyLoggedIn(entry));
		p.getClient().getSession().send(GamePackets.writeBuddyList(BuddyListHandler.ADD, bList));

		sendReturnBuddyLogInNotifications(srcCh, sender, Collections.singletonList(Integer.valueOf(recipient)), true);
		return true;
	}

	public void sendBuddyLogOffNotifications(GameCharacter p) {
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
		for (Map.Entry<Byte, List<Integer>> entry : buddyChannels.entrySet()) {
			int[] recipients = new int[entry.getValue().size()];
			int i = 0;
			for (Integer recipient : entry.getValue())
				recipients[i++] = recipient.intValue();
			allChannelsInWorld.getWhenSafe(entry.getKey()).sendBuddyLogOffNotifications(p.getId(), recipients);
		}
	}

	/* package-private */ void receivedBuddyLogOffNotifications(int sender, int[] recipients) {
		for (int recipient : recipients) {
			GameCharacter p = self.getPlayerById(recipient);
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

	public void sendBuddyDeleted(GameCharacter p, int recipient, byte destCh) {
		allChannelsInWorld.getWhenSafe(Byte.valueOf(destCh)).sendBuddyDeleted(p.getId(), recipient);
	}

	/* package-private */ void receivedBuddyDeleted(int recipient, int sender) {
		GameCharacter p = self.getPlayerById(recipient);
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

	public void sendMakeParty(GameCharacter p) {
		partiesAndChatRooms.sendMakeParty(p);
	}

	public void sendDisbandParty(int partyId) {
		partiesAndChatRooms.sendDisbandParty(partyId);
	}

	public void sendLeaveParty(GameCharacter p, int partyId) {
		partiesAndChatRooms.sendLeaveParty(p, partyId);
	}

	public void sendJoinParty(GameCharacter p, int partyId) {
		partiesAndChatRooms.sendJoinParty(p, partyId);
	}

	public void sendExpelPartyMember(PartyList.Member member, int partyId) {
		partiesAndChatRooms.sendExpelPartyMember(member, partyId);
	}

	public void sendChangePartyLeader(int partyId, int newLeader) {
		partiesAndChatRooms.sendChangePartyLeader(partyId, newLeader);
	}

	/* package-private */ void fillPartyList(GameCharacter excludeMember, PartyList party) {
		BlockingQueue<Pair<Byte, Object>> resultConsumer = new LinkedBlockingQueue<Pair<Byte, Object>>();
		long limit = System.currentTimeMillis() + BLOCKING_CALL_TIMEOUT;
		try {
			Pair<Byte, Object> result = resultConsumer.poll(limit - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
			if (result == null) {
				LOG.log(Level.FINE, "Cross process fill party list timeout after " + BLOCKING_CALL_TIMEOUT + " milliseconds");
				return;
			}
			party.setLeader(((Integer) result.right).intValue());

			result = resultConsumer.poll(limit - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
			if (result == null) {
				LOG.log(Level.FINE, "Cross process fill party list timeout after " + BLOCKING_CALL_TIMEOUT + " milliseconds");
				return;
			}
			for (Object mem : (PartyList.Member[]) result.right) {
				if (mem instanceof PartyList.LocalMember)
					party.addPlayer((PartyList.LocalMember) mem);
				else if (mem instanceof PartyList.RemoteMember)
					party.addPlayer((PartyList.RemoteMember) mem);
			}
		} catch (InterruptedException e) {
			//propagate the interrupted status further up to our worker
			//executor service and see if they care - we don't care about it
			Thread.currentThread().interrupt();
		}
	}

	public PartyList sendFetchPartyList(GameCharacter p, int partyId) {
		return partiesAndChatRooms.sendFetchPartyList(p, partyId);
	}

	public void sendPartyMemberLogInNotifications(GameCharacter p, PartyList party) {
		partiesAndChatRooms.sendPartyMemberOnline(p, party);
	}

	public void sendPartyMemberLogOffNotifications(GameCharacter p, boolean loggingOff) {
		partiesAndChatRooms.sendPartyMemberOffline(p, loggingOff);
	}

	public void sendPartyLevelOrJobUpdate(GameCharacter p, boolean level) {
		partiesAndChatRooms.sendPartyLevelOrJobUpdate(p, level);
	}

	/* package-private */ LockableMap<Byte, CrossChannelCommunication> getChannelsInWorld() {
		return allChannelsInWorld;
	}
}
