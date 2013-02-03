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
import argonms.game.GameRegistry;
import argonms.game.GameServer;
import argonms.game.character.Chatroom;
import argonms.game.character.GameCharacter;
import argonms.game.character.GuildList;
import argonms.game.character.PartyList;
import argonms.game.character.PlayerContinuation;
import argonms.game.command.CommandTarget;
import argonms.game.command.LocalChannelCommandTarget;
import argonms.game.net.WorldChannel;
import argonms.game.net.external.GamePackets;
import argonms.game.net.external.handler.BuddyListHandler;
import argonms.game.net.external.handler.ChatHandler;
import java.net.UnknownHostException;
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
public class CrossServerSynchronization {
	private static final Logger LOG = Logger.getLogger(CrossServerSynchronization.class.getName());

	private static final byte
		PRIVATE_CHAT_TYPE_BUDDY = 0,
		PRIVATE_CHAT_TYPE_PARTY = 1,
		PRIVATE_CHAT_TYPE_GUILD = 2
	;

	private static final byte
		SCAN_PLAYER_CHANNEL_NO_MATCH = 0,
		SCAN_PLAYER_CHANNEL_HIDDEN = 1,
		SCAN_PLAYER_CHANNEL_FOUND = 2
	;

	/* package-private */ static final int BLOCKING_CALL_TIMEOUT = 2000; //in milliseconds

	private final LockableMap<Byte, CrossChannelSynchronization> allChannelsInWorld;
	private final LockableMap<Byte, CrossProcessCrossChannelSynchronization> remoteChannelsInWorld;
	private CenterServerSynchronization intraworldGroups;
	private final ReadWriteLock locks;
	private final WorldChannel self;

	public CrossServerSynchronization(WorldChannel channel) {
		//some send methods have an early out only if target channel is on the same process,
		//so current channel and same process channels should be first to be iterated.
		//since initializeLocalChannels is called before addRemoteChannels, this should be
		//true as long as otherChannelsInWorld is in insertion order, so use a LinkedHashMap
		allChannelsInWorld = new LockableMap<Byte, CrossChannelSynchronization>(new LinkedHashMap<Byte, CrossChannelSynchronization>());
		remoteChannelsInWorld = new LockableMap<Byte, CrossProcessCrossChannelSynchronization>(new HashMap<Byte, CrossProcessCrossChannelSynchronization>());
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

	public void initializeLocalChannels(Map<Byte, CrossServerSynchronization> initialized) {
		lockWrite();
		try {
			SameProcessCrossChannelSynchronization source = new SameProcessCrossChannelSynchronization(this, self.getChannelId(), self.getChannelId());
			SameProcessCrossChannelSynchronization sink = new SameProcessCrossChannelSynchronization(this, self.getChannelId(), self.getChannelId());
			sink.connect(source);
			this.allChannelsInWorld.put(Byte.valueOf(self.getChannelId()), sink);

			for (Map.Entry<Byte, CrossServerSynchronization> other : initialized.entrySet()) {
				source = new SameProcessCrossChannelSynchronization(other.getValue(), other.getKey().byteValue(), self.getChannelId());
				sink = new SameProcessCrossChannelSynchronization(this, self.getChannelId(), other.getKey().byteValue());
				sink.connect(source);
				other.getValue().allChannelsInWorld.put(Byte.valueOf(self.getChannelId()), source);
				this.allChannelsInWorld.put(other.getKey(), sink);
			}

			//this could be called in the ctor, but that would leak "this"
			intraworldGroups = new CenterServerSynchronization(this, self);
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

	public Pair<byte[], Integer> getChannelHost(byte ch) throws UnknownHostException {
		CrossChannelSynchronization css = allChannelsInWorld.getWhenSafe(Byte.valueOf(ch));
		return new Pair<byte[], Integer>(css.getIpAddress(), Integer.valueOf(css.getPort()));
	}

	public void addRemoteChannels(byte serverId, byte[] host, Map<Byte, Integer> ports) {
		lockWrite();
		try {
			for (Map.Entry<Byte, Integer> port : ports.entrySet()) {
				CrossProcessCrossChannelSynchronization cpccs = new CrossProcessCrossChannelSynchronization(this, self.getChannelId(), port.getKey().byteValue(), serverId, host, port.getValue().intValue());
				remoteChannelsInWorld.put(port.getKey(), cpccs);
				allChannelsInWorld.put(port.getKey(), cpccs);
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
			CrossProcessCrossChannelSynchronization cpccs = remoteChannelsInWorld.get(Byte.valueOf(channel));
			if (cpccs != null)
				cpccs.setPort(port);
		} finally {
			unlockWrite();
		}
	}

	public void receivedCrossProcessCrossChannelSynchronizationPacket(LittleEndianReader packet) {
		byte srcCh = packet.readByte();
		remoteChannelsInWorld.getWhenSafe(Byte.valueOf(srcCh)).receivedCrossProcessCrossChannelSynchronizationPacket(packet);
	}

	public void receivedCenterServerSynchronizationPacket(LittleEndianReader packet) {
		intraworldGroups.receivedCenterServerSynchronizationPacket(packet);
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

	public byte scanChannelOfPlayer(String name, boolean ignoreHidden) {
		BlockingQueue<Pair<Byte, Object>> queue = new LinkedBlockingQueue<Pair<Byte, Object>>();
		lockRead();
		try {
			int remaining = 0;
			byte findResult;
			for (CrossChannelSynchronization ccs : allChannelsInWorld.values()) {
				ccs.callPlayerExistsCheck(queue, name);
				Pair<Byte, Object> result;
				//address any results that have since responded, for a possibility of early out
				while ((result = queue.poll()) != null)
					if ((findResult = ((Byte) result.right).byteValue()) == SCAN_PLAYER_CHANNEL_NO_MATCH)
						remaining--;
					else if (!ignoreHidden || findResult == SCAN_PLAYER_CHANNEL_FOUND)
						return result.left.byteValue();
					else //if (ignoreHidden && value == SCAN_PLAYER_HIDDEN)
						return 0;
				remaining++;
			}

			long limit = System.currentTimeMillis() + 2000;
			while (remaining > 0) {
				Pair<Byte, Object> result = queue.poll(limit - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
				if (result == null) {
					LOG.log(Level.FINE, "Cross process player search timeout after " + BLOCKING_CALL_TIMEOUT + " milliseconds");
					return 0;
				}
				if ((findResult = ((Byte) result.right).byteValue()) == SCAN_PLAYER_CHANNEL_NO_MATCH)
					remaining--;
				else if (!ignoreHidden || findResult == SCAN_PLAYER_CHANNEL_FOUND)
					return result.left.byteValue();
				else //if (ignoreHidden && value == SCAN_PLAYER_HIDDEN)
					return 0;
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

	/* package-private */ byte makePlayerExistsResult(String name) {
		GameCharacter p = self.getPlayerByName(name);
		if (p == null)
			return SCAN_PLAYER_CHANNEL_NO_MATCH;
		if (!p.isVisible())
			return SCAN_PLAYER_CHANNEL_HIDDEN;
		return SCAN_PLAYER_CHANNEL_FOUND;
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
					Byte ch = Byte.valueOf(entry != null ? entry.getChannel() : BuddyListEntry.OFFLINE_CHANNEL);
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
				if (p.getGuild() == null)
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
					for (CrossChannelSynchronization ccs : allChannelsInWorld.values())
						ccs.sendPrivateChat(type, recipients, name, message);
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

	public boolean sendWhisper(String recipient, GameCharacter sender, String message) {
		BlockingQueue<Pair<Byte, Object>> queue = new LinkedBlockingQueue<Pair<Byte, Object>>();
		lockRead();
		try {
			int remaining = 0;
			for (CrossChannelSynchronization ccs : allChannelsInWorld.values()) {
				ccs.callSendWhisper(queue, recipient, sender.getName(), message);
				Pair<Byte, Object> result;
				//address any results that have since responded, for a possibility of early out
				while ((result = queue.poll()) != null)
					if (((Boolean) result.right).booleanValue())
						return true;
					else
						remaining--;
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
				else
					remaining--;
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
		return p.isVisible();
	}

	public void sendSpouseChat(String spouse, GameCharacter p, String message) {
		//perhaps we should compare spouse's name with p.getSpouseId()?
		String name = p.getName();
		int recipient = p.getSpouseId();
		if (recipient == 0)
			return;

		lockRead();
		try {
			for (CrossChannelSynchronization ccs : allChannelsInWorld.values())
				if (ccs.sendSpouseChat(recipient, name, message))
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

	public Pair<Byte, Byte> sendBuddyInvite(GameCharacter sender, int recipientId) {
		BlockingQueue<Pair<Byte, Object>> queue = new LinkedBlockingQueue<Pair<Byte, Object>>();
		lockRead();
		try {
			int remaining = 0;
			Byte inviteResult;
			for (CrossChannelSynchronization ccs : allChannelsInWorld.values()) {
				ccs.callSendBuddyInvite(queue, recipientId, sender.getId(), sender.getName());
				Pair<Byte, Object> result;
				//address any results that have since responded, for a possibility of early out
				while ((result = queue.poll()) != null)
					if (((inviteResult = (Byte) result.right).byteValue()) != -1)
						return new Pair<Byte, Byte>(result.left, inviteResult);
					else
						remaining--;
				remaining++;
			}

			long limit = System.currentTimeMillis() + BLOCKING_CALL_TIMEOUT;
			while (remaining > 0) {
				Pair<Byte, Object> result = queue.poll(limit - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
				if (result == null) {
					LOG.log(Level.FINE, "Cross process buddy invite timeout after " + BLOCKING_CALL_TIMEOUT + " milliseconds");
					return new Pair<Byte, Byte>(Byte.valueOf((byte) -1), Byte.valueOf((byte) -1));
				}
				if (((inviteResult = (Byte) result.right).byteValue()) != -1)
					return new Pair<Byte, Byte>(result.left, inviteResult);
				else
					remaining--;
			}
			return new Pair<Byte, Byte>(Byte.valueOf((byte) -1), Byte.valueOf((byte) -1));
		} catch (InterruptedException e) {
			//propagate the interrupted status further up to our worker
			//executor service and see if they care - we don't care about it
			Thread.currentThread().interrupt();
			return new Pair<Byte, Byte>(Byte.valueOf((byte) -1), Byte.valueOf((byte) -1));
		} finally {
			unlockRead();
		}
	}

	/* package-private */ byte makeBuddyInviteResult(int recipientId, byte srcCh, int senderId, String senderName) {
		GameCharacter p = self.getPlayerById(recipientId);
		if (p == null)
			return -1;

		BuddyList bList = p.getBuddyList();
		if (bList.isFull())
			return BuddyListHandler.THEIR_LIST_FULL;
		BuddyListEntry existing = bList.getBuddy(senderId);
		if (existing != null) {
			assert (existing.getStatus() == BuddyListEntry.STATUS_HALF_OPEN);
			existing.setStatus(BuddyListEntry.STATUS_MUTUAL);
			existing.setChannel(srcCh);
			p.getClient().getSession().send(GamePackets.writeBuddyLoggedIn(existing));
			p.getClient().getSession().send(GamePackets.writeBuddyList(BuddyListHandler.ADD, bList));
			return Byte.MIN_VALUE;
		}
		bList.addInvite(senderId, senderName);
		p.getClient().getSession().send(GamePackets.writeBuddyInvite(senderId, senderName));
		return Byte.MAX_VALUE;
	}

	public boolean sendBuddyInviteRetracted(GameCharacter p, int deletedId) {
		lockRead();
		try {
			for (CrossChannelSynchronization ccs : allChannelsInWorld.values())
				if (ccs.sendBuddyInviteRetracted(p.getId(), deletedId))
					return true;
		} finally {
			unlockRead();
		}
		return false;
	}

	/* package-private */ boolean receivedBuddyInviteRetracted(int recipient, int sender) {
		GameCharacter p = self.getPlayerById(recipient);
		if (p == null)
			return false;

		p.getBuddyList().removeInvite(sender);
		return true;
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
			if (buddy.getStatus() == BuddyListEntry.STATUS_MUTUAL)
				recipients[i++] = buddy.getId();
		if (recipients.length != i) {
			//just trim recipients of extra 0s
			int[] temp = new int[i];
			System.arraycopy(recipients, 0, temp, 0, i);
			recipients = temp;
		}

		lockRead();
		try {
			for (CrossChannelSynchronization ccs : allChannelsInWorld.values())
				if ((remaining -= ccs.exchangeBuddyLogInNotifications(p.getId(), recipients)) <= 0)
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

	public boolean sendBuddyInviteAccepted(GameCharacter p, int recipient) {
		lockRead();
		try {
			for (CrossChannelSynchronization ccs : allChannelsInWorld.values())
				if (ccs.sendBuddyInviteAccepted(p.getId(), recipient))
					return true;
		} finally {
			unlockRead();
		}
		return false;
	}

	/* package-private */ boolean receivedBuddyInviteAccepted(int sender, int recipient, byte srcCh) {
		GameCharacter p = self.getPlayerById(recipient);
		if (p == null)
			return false;

		BuddyList bList = p.getBuddyList();
		BuddyListEntry entry = bList.getBuddy(sender);
		//in case we deleted the entry in the meantime...
		if (entry == null)
			return true;

		entry.setStatus(BuddyListEntry.STATUS_MUTUAL);
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
			if (buddy.getChannel() == BuddyListEntry.OFFLINE_CHANNEL)
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

			entry.setChannel(BuddyListEntry.OFFLINE_CHANNEL);
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

		entry.setStatus(BuddyListEntry.STATUS_HALF_OPEN);
		entry.setChannel(BuddyListEntry.OFFLINE_CHANNEL);
		p.getClient().getSession().send(GamePackets.writeBuddyList(BuddyListHandler.REMOVE, bList));
	}

	public void sendMakeParty(GameCharacter p) {
		intraworldGroups.sendMakeParty(p);
	}

	public void sendDisbandParty(int partyId) {
		intraworldGroups.sendDisbandParty(partyId);
	}

	public void sendLeaveParty(GameCharacter p, int partyId) {
		intraworldGroups.sendLeaveParty(p, partyId);
	}

	public void sendJoinParty(GameCharacter p, int partyId) {
		intraworldGroups.sendJoinParty(p, partyId);
	}

	public void sendExpelPartyMember(PartyList.Member member, int partyId) {
		intraworldGroups.sendExpelPartyMember(member, partyId);
	}

	public void sendChangePartyLeader(int partyId, int newLeader) {
		intraworldGroups.sendChangePartyLeader(partyId, newLeader);
	}

	/* package-private */ void fillPartyList(PartyList party) {
		BlockingQueue<Pair<Byte, Object>> queue = new LinkedBlockingQueue<Pair<Byte, Object>>();
		intraworldGroups.sendFillPartyList(queue, party);

		long limit = System.currentTimeMillis() + BLOCKING_CALL_TIMEOUT;
		try {
			Pair<Byte, Object> result = queue.poll(limit - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
			if (result == null) {
				LOG.log(Level.FINE, "Cross process fill party list timeout after " + BLOCKING_CALL_TIMEOUT + " milliseconds");
				return;
			}
			party.setLeader(((Integer) result.right).intValue());

			result = queue.poll(limit - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
			if (result == null) {
				LOG.log(Level.FINE, "Cross process fill party list timeout after " + BLOCKING_CALL_TIMEOUT + " milliseconds");
				return;
			}
			for (PartyList.Member mem : (PartyList.Member[]) result.right)
				if (mem instanceof PartyList.LocalMember)
					party.addPlayer((PartyList.LocalMember) mem);
				else if (mem instanceof PartyList.RemoteMember)
					party.addPlayer((PartyList.RemoteMember) mem);
		} catch (InterruptedException e) {
			//propagate the interrupted status further up to our worker
			//executor service and see if they care - we don't care about it
			Thread.currentThread().interrupt();
		}
	}

	public PartyList sendFetchPartyList(int partyId) {
		return intraworldGroups.sendFetchPartyList(partyId);
	}

	public void sendPartyMemberLogInNotifications(GameCharacter p) {
		intraworldGroups.sendPartyMemberOnline(p);
	}

	public void sendPartyMemberLogOffNotifications(GameCharacter p, boolean loggingOff) {
		intraworldGroups.sendPartyMemberOffline(p, loggingOff);
	}

	public void sendPartyLevelOrJobUpdate(GameCharacter p, boolean level) {
		intraworldGroups.sendPartyLevelOrJobUpdate(p, level);
	}

	public void sendMakeGuild(String name, PartyList party) {
		intraworldGroups.sendMakeGuild(name, party);
	}

	/* package-private */ void fillGuildList(GuildList guild) {
		BlockingQueue<Pair<Byte, Object>> queue = new LinkedBlockingQueue<Pair<Byte, Object>>();
		intraworldGroups.sendFillGuildList(queue, guild);

		long limit = System.currentTimeMillis() + BLOCKING_CALL_TIMEOUT;
		try {
			Pair<Byte, Object> result = queue.poll(limit - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
			if (result == null) {
				LOG.log(Level.FINE, "Cross process fill guild list timeout after " + BLOCKING_CALL_TIMEOUT + " milliseconds");
				return;
			}
			Object[] properties = (Object[]) result.right;
			guild.setName((String) properties[0]);
			guild.setEmblem(((Short) properties[1]).shortValue(), ((Byte) properties[2]).byteValue(), ((Short) properties[3]).shortValue(), ((Byte) properties[4]).byteValue());
			guild.setTitles((String[]) properties[5]);
			guild.setCapacity(((Byte) properties[6]).byteValue());
			guild.setNotice((String) properties[7]);
			guild.setGp(((Integer) properties[8]).intValue());
			guild.setAlliance(((Integer) properties[9]).intValue());

			result = queue.poll(limit - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
			if (result == null) {
				LOG.log(Level.FINE, "Cross process fill guild list timeout after " + BLOCKING_CALL_TIMEOUT + " milliseconds");
				return;
			}
			for (GuildList.Member mem : (GuildList.Member[]) result.right)
				if (mem instanceof GuildList.LocalMember)
					guild.addPlayer((GuildList.LocalMember) mem);
				else if (mem instanceof GuildList.RemoteMember)
					guild.addPlayer((GuildList.RemoteMember) mem);
		} catch (InterruptedException e) {
			//propagate the interrupted status further up to our worker
			//executor service and see if they care - we don't care about it
			Thread.currentThread().interrupt();
		}
	}

	public GuildList sendFetchGuildList(int guildList) {
		return intraworldGroups.sendFetchGuildList(guildList);
	}

	public void sendGuildMemberLogInNotifications(GameCharacter p) {
		intraworldGroups.sendGuildMemberOnline(p);
	}

	public void sendGuildMemberLogOffNotifications(GameCharacter p, boolean loggingOff) {
		intraworldGroups.sendGuildMemberOffline(p, loggingOff);
	}

	public void sendJoinGuild(GameCharacter p, int guildId) {
		intraworldGroups.sendJoinGuild(p, guildId);
	}

	public void sendMakeChatroom(GameCharacter p) {
		intraworldGroups.sendMakeChatroom(p);
	}

	public void sendJoinChatroom(GameCharacter joiner, int roomId) {
		intraworldGroups.sendJoinChatroom(joiner, roomId);
	}

	public void sendLeaveChatroom(GameCharacter leaver) {
		intraworldGroups.sendLeaveChatroom(leaver.getChatRoom().getRoomId(), leaver.getId());
	}

	public boolean sendChatroomInvite(String inviter, int roomId, String invitee) {
		BlockingQueue<Pair<Byte, Object>> queue = new LinkedBlockingQueue<Pair<Byte, Object>>();
		lockRead();
		try {
			int remaining = 0;
			for (CrossChannelSynchronization ccs : allChannelsInWorld.values()) {
				ccs.callSendChatroomInvite(queue, invitee, roomId, inviter);
				Pair<Byte, Object> result;
				//address any results that have since responded, for a possibility of early out
				while ((result = queue.poll()) != null)
					if (((Boolean) result.right).booleanValue())
						return true;
					else
						remaining--;
				remaining++;
			}

			long limit = System.currentTimeMillis() + BLOCKING_CALL_TIMEOUT;
			while (remaining > 0) {
				Pair<Byte, Object> result = queue.poll(limit - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
				if (result == null) {
					LOG.log(Level.FINE, "Cross process buddy invite timeout after " + BLOCKING_CALL_TIMEOUT + " milliseconds");
					return false;
				}
				if (((Boolean) result.right).booleanValue())
					return true;
				else
					remaining--;
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

	/* package-private */ boolean makeChatroomInviteResult(String invitee, int roomId, String inviter) {
		GameCharacter p = self.getPlayerByName(invitee);
		if (p == null)
			return false;
		p.getClient().getSession().send(GamePackets.writeChatroomInvite(inviter, roomId));
		return true;
	}

	public void sendChatroomDecline(String invitee, String inviter) {
		lockRead();
		try {
			for (CrossChannelSynchronization ccs : allChannelsInWorld.values())
				if (ccs.sendChatroomDecline(invitee, inviter))
					break;
		} finally {
			unlockRead();
		}
	}

	/* package-private */ boolean receivedChatroomDecline(String invitee, String inviter) {
		GameCharacter p = self.getPlayerByName(inviter);
		if (p == null)
			return false;
		p.getClient().getSession().send(GamePackets.writeChatroomInviteResponse(Chatroom.ACT_DECLINE, invitee, false));
		return true;
	}

	public void sendChatroomText(String text, Chatroom room, int sender) {
		Set<Byte> channels;
		room.lockRead();
		try {
			channels = room.allChannels();
		} finally {
			room.unlockRead();
		}

		for (Byte ch : channels)
			allChannelsInWorld.getWhenSafe(ch.byteValue()).sendChatroomText(text, room.getRoomId(), sender);
	}

	/* package-private */ void receivedChatroomText(String text, int roomId, int sender) {
		Chatroom room = intraworldGroups.getChatRoom(roomId);
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

	public void chatroomPlayerChangingChannels(int playerId, Chatroom room) {
		intraworldGroups.chatroomPlayerChangingChannels(playerId, room);
	}

	public void sendChatroomPlayerChangedChannels(GameCharacter p, int roomId) {
		intraworldGroups.sendChatroomPlayerChangedChannels(p.getId(), roomId);
	}

	public void sendChatroomPlayerLookUpdate(GameCharacter p, int roomId) {
		intraworldGroups.sendChatroomPlayerLookUpdate(p, roomId);
	}

	public void sendCrossChannelCommandCharacterManipulation(byte destCh, String recipient, List<CommandTarget.CharacterManipulation> updates) {
		allChannelsInWorld.getWhenSafe(Byte.valueOf(destCh)).sendCrossChannelCommandCharacterManipulation(recipient, updates);
	}

	/* package-private*/ void receivedCrossChannelCommandCharacterManipulation(String recipient, List<CommandTarget.CharacterManipulation> updates) {
		GameCharacter p = self.getPlayerByName(recipient);
		if (p != null)
			new LocalChannelCommandTarget(p).mutate(updates);
	}

	public Object sendCrossChannelCommandCharacterAccess(byte destCh, String target, CommandTarget.CharacterProperty key) {
		BlockingQueue<Pair<Byte, Object>> queue = new LinkedBlockingQueue<Pair<Byte, Object>>();
		allChannelsInWorld.getWhenSafe(Byte.valueOf(destCh)).callCrossChannelCommandCharacterAccess(queue, target, key);
		long limit = System.currentTimeMillis() + BLOCKING_CALL_TIMEOUT;
		try {
			Pair<Byte, Object> result = queue.poll(limit - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
			if (result == null) {
				LOG.log(Level.FINE, "Cross process cross channel command target character access timeout after " + BLOCKING_CALL_TIMEOUT + " milliseconds");
				return null;
			}
			return result.right;
		} catch (InterruptedException e) {
			//propagate the interrupted status further up to our worker
			//executor service and see if they care - we don't care about it
			Thread.currentThread().interrupt();
			return null;
		}
	}

	/* package-private*/ Object makeCrossChannelCommandCharacterAccessResult(String target, CommandTarget.CharacterProperty key) {
		GameCharacter p = self.getPlayerByName(target);
		if (p != null)
			return new LocalChannelCommandTarget(p).access(key);
		return null;
	}

	public void sendWorldWideNotice(byte style, String message) {
		allChannelsInWorld.getWhenSafe(Byte.valueOf(self.getChannelId())).sendWorldWideNotice(style, message);
		Set<Byte> triedGameServers = new HashSet<Byte>();
		lockRead();
		try {
			for (CrossProcessCrossChannelSynchronization ccs : remoteChannelsInWorld.values()) {
				if (!triedGameServers.contains(Byte.valueOf(ccs.getServerId()))) {
					ccs.sendWorldWideNotice(style, message);
					triedGameServers.add(Byte.valueOf(ccs.getServerId()));
				}
			}
		} finally {
			unlockRead();
		}
	}

	/* package-private*/ void receivedWorldWideNotice(byte style, String message) {
		if (style == ChatHandler.TextStyle.TICKER.byteValue())
			GameServer.getVariables().setNewsTickerMessage(message);

		GameServer.getInstance().serverWideMessage(style, message);
	}

	public void sendServerShutdown(boolean halt, boolean restart, boolean cancel, int seconds, String message) {
		allChannelsInWorld.getWhenSafe(Byte.valueOf(self.getChannelId())).sendServerShutdown(halt, restart, cancel, seconds, message);
		Set<Byte> triedGameServers = new HashSet<Byte>();
		lockRead();
		try {
			for (CrossProcessCrossChannelSynchronization ccs : remoteChannelsInWorld.values()) {
				if (!triedGameServers.contains(Byte.valueOf(ccs.getServerId()))) {
					ccs.sendServerShutdown(halt, restart, cancel, seconds, message);
					triedGameServers.add(Byte.valueOf(ccs.getServerId()));
				}
			}
		} finally {
			unlockRead();
		}
	}

	/* package-private*/ void receivedServerShutdown(boolean halt, boolean restart, boolean cancel, int seconds, String message) {
		String reason;
		if (restart)
			reason = "restart";
		else if (halt)
			reason = "halt";
		else
			reason = "maintenance";
		String formattedTime = ((seconds == 0) ? "NOW" : ("in " + seconds + " seconds"));

		if (!cancel) {
			String notice = "The server is going down for " + reason + " " + formattedTime + "!\r\n" + message;
			GameServer.getInstance().serverWideMessage(ChatHandler.TextStyle.OK_BOX.byteValue(), notice);
			LOG.log(Level.WARNING, notice);
			GameServer.getInstance().shutdown(halt, seconds * 1000);
			if (restart) {
				//TODO: implement restart
			}
		} else {
			//TODO: implement cancel
		}
	}

	public void sendServerRateChange(byte type, short newRate) {
		allChannelsInWorld.getWhenSafe(Byte.valueOf(self.getChannelId())).sendServerRateChange(type, newRate);
		Set<Byte> triedGameServers = new HashSet<Byte>();
		lockRead();
		try {
			for (CrossProcessCrossChannelSynchronization ccs : remoteChannelsInWorld.values()) {
				if (!triedGameServers.contains(Byte.valueOf(ccs.getServerId()))) {
					ccs.sendServerRateChange(type, newRate);
					triedGameServers.add(Byte.valueOf(ccs.getServerId()));
				}
			}
		} finally {
			unlockRead();
		}
	}

	/* package-private*/ void receivedServerRateChange(byte type, short newRate) {
		String str = null;
		switch (type) {
			case GameRegistry.RATE_EXP:
				GameServer.getVariables().setExpRate(newRate);
				str = "EXP";
				break;
			case GameRegistry.RATE_DROP:
				GameServer.getVariables().setDropRate(newRate);
				str = "drop";
				break;
			case GameRegistry.RATE_MESO:
				GameServer.getVariables().setMesoRate(newRate);
				str = "meso";
				break;
		}
		GameServer.getInstance().serverWideMessage(ChatHandler.TextStyle.LIGHT_BLUE_TEXT_CLEAR_BG.byteValue(), "This world's " + str + " rate has been set to " + newRate + ".");
	}

	public String retrieveConnectedPlayersList(byte privilegeLevelLimit) {
		BlockingQueue<Pair<Byte, Object>> queue = new LinkedBlockingQueue<Pair<Byte, Object>>();
		StringBuilder sb = new StringBuilder();
		lockRead();
		try {
			int remaining = 0;
			for (CrossChannelSynchronization ccs : allChannelsInWorld.values()) {
				ccs.callRetrieveConnectedPlayersList(queue, privilegeLevelLimit);
				remaining++;
			}

			long limit = System.currentTimeMillis() + BLOCKING_CALL_TIMEOUT;
			while (remaining > 0) {
				Pair<Byte, Object> result = queue.poll(limit - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
				if (result == null) {
					LOG.log(Level.FINE, "Cross process connected players list retrieval timeout after " + BLOCKING_CALL_TIMEOUT + " milliseconds");
					return null;
				}
				sb.append(result.right);
				remaining--;
			}

			return sb.length() > 0 ? sb.substring(0, sb.length() - 1) : "";
		} catch (InterruptedException e) {
			//propagate the interrupted status further up to our worker
			//executor service and see if they care - we don't care about it
			Thread.currentThread().interrupt();
			return null;
		} finally {
			unlockRead();
		}
	}

	/* package-private*/ String makeRetrieveConnectedPlayersListResult(byte privilegeLevelLimit) {
		StringBuilder sb = new StringBuilder();
		for (GameCharacter c : GameServer.getChannel(self.getChannelId()).getConnectedPlayers())
			if (c.getPrivilegeLevel() >= privilegeLevelLimit)
				sb.append(c.getName()).append(',');
		return sb.toString();
	}
}
