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
import argonms.game.character.PlayerContinuation;
import argonms.game.net.WorldChannel;
import argonms.game.net.external.GamePackets;
import java.util.ArrayList;
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

	private final LockableMap<Byte, CrossChannelCommunication> otherChannelsInWorld;
	private final LockableMap<Byte, CrossProcessCrossChannelCommunication> remoteChannelsInWorld;
	private final ReadWriteLock locks;
	private final WorldChannel self;

	public CrossServerCommunication(WorldChannel channel) {
		//some send methods have an early out only if target channel is on the same process,
		//so current channel and same process channels should be first to be iterated.
		//since initializeLocalChannels is called before addRemoteChannels, this should be
		//true as long as otherChannelsInWorld is in insertion order, so use a LinkedHashMap
		otherChannelsInWorld = new LockableMap<Byte, CrossChannelCommunication>(new LinkedHashMap<Byte, CrossChannelCommunication>());
		remoteChannelsInWorld = new LockableMap<Byte, CrossProcessCrossChannelCommunication>(new HashMap<Byte, CrossProcessCrossChannelCommunication>());
		locks = new ReentrantReadWriteLock();
		self = channel;
	}

	//instantiate all CrossServerCommunication for game server and assign them to Map<Byte, CrossServerCommunication> allChannels;
	//Map<Byte, CrossServerCommunication> initialized = empty hashmap;
	//in each iteration over allChannels: this.initializeLocalChannels(initialized) then initialized.put(channel, this)
	public void initializeLocalChannels(Map<Byte, CrossServerCommunication> initialized) {
		locks.writeLock().lock();
		try {
			SameProcessCrossChannelCommunication source = new SameProcessCrossChannelCommunication(this, self.getChannelId(), self.getChannelId());
			SameProcessCrossChannelCommunication sink = new SameProcessCrossChannelCommunication(this, self.getChannelId(), self.getChannelId());
			sink.connect(source);
			this.otherChannelsInWorld.put(Byte.valueOf(self.getChannelId()), sink);

			for (Map.Entry<Byte, CrossServerCommunication> other : initialized.entrySet()) {
				source = new SameProcessCrossChannelCommunication(other.getValue(), other.getKey().byteValue(), self.getChannelId());
				sink = new SameProcessCrossChannelCommunication(other.getValue(), self.getChannelId(), other.getKey().byteValue());
				sink.connect(source);
				other.getValue().otherChannelsInWorld.put(Byte.valueOf(self.getChannelId()), source);
				this.otherChannelsInWorld.put(other.getKey(), sink);
			}
		} finally {
			locks.writeLock().unlock();
		}
	}

	public Set<Byte> localChannels() {
		locks.readLock().lock();
		try {
			Set<Byte> local = new HashSet<Byte>(otherChannelsInWorld.keySet());
			local.removeAll(remoteChannelsInWorld.keySet());
			return local;
		} finally {
			locks.readLock().unlock();
		}
	}

	public Set<Byte> remoteChannels() {
		locks.readLock().lock();
		try {
			return new HashSet<Byte>(remoteChannelsInWorld.keySet());
		} finally {
			locks.readLock().unlock();
		}
	}

	public void addRemoteChannels(byte[] host, Map<Byte, Integer> ports) {
		locks.writeLock().lock();
		try {
			for (Map.Entry<Byte, Integer> port : ports.entrySet()) {
				CrossProcessCrossChannelCommunication cpc = new CrossProcessCrossChannelCommunication(this, self.getChannelId(), port.getKey().byteValue(), host, port.getValue().intValue());
				remoteChannelsInWorld.put(port.getKey(), cpc);
				otherChannelsInWorld.put(port.getKey(), cpc);
			}
		} finally {
			locks.writeLock().unlock();
		}
	}

	public void removeRemoteChannels(Set<Byte> channels) {
		locks.writeLock().lock();
		try {
			for (Byte ch : channels) {
				remoteChannelsInWorld.remove(ch);
				otherChannelsInWorld.remove(ch);
			}
		} finally {
			locks.writeLock().unlock();
		}
	}

	public void changeRemoteChannelPort(byte channel, int port) {
		locks.writeLock().lock();
		try {
			remoteChannelsInWorld.get(Byte.valueOf(channel)).setPort(port);
		} finally {
			locks.writeLock().unlock();
		}
	}

	public void receivedCrossProcessCrossChannelCommunicationPacket(LittleEndianReader packet) {
		byte srcCh = packet.readByte();
		remoteChannelsInWorld.getWhenSafe(Byte.valueOf(srcCh)).receivedCrossProcessCrossChannelCommunicationPacket(packet);
	}

	public void sendChannelChangeRequest(byte destCh, GameCharacter p) {
		otherChannelsInWorld.getWhenSafe(Byte.valueOf(destCh)).sendPlayerContext(p.getId(), new PlayerContinuation(p));
	}

	/* package-private */ void receivedChannelChangeRequest(byte srcCh, int playerId, PlayerContinuation context) {
		self.storePlayerBuffs(playerId, context);
		sendChannelChangeAcceptance(srcCh, playerId);
	}

	public void sendChannelChangeAcceptance(byte destCh, int playerId) {
		otherChannelsInWorld.getWhenSafe(Byte.valueOf(destCh)).sendChannelChangeAcceptance(playerId);
	}

	/* package-private */ void receivedChannelChangeAcceptance(byte srcCh, int playerId) {
		self.performChannelChange(playerId);
	}

	public byte scanChannelOfPlayer(String name) {
		BlockingQueue<Pair<Byte, Object>> queue = new LinkedBlockingQueue<Pair<Byte, Object>>();
		locks.readLock().lock();
		try {
			int remaining = 0;
			for (CrossChannelCommunication ccc : otherChannelsInWorld.values()) {
				ccc.callPlayerExistsCheck(queue, name);
				Pair<Byte, Object> result = queue.poll();
				if (result != null && ((Boolean) result.right).booleanValue())
					//immediately responded
					return result.left.byteValue();
				remaining++;
			}

			long limit = System.currentTimeMillis() + 2000;
			while (remaining > 0) {
				Pair<Byte, Object> result = queue.poll(limit - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
				if (result == null) {
					LOG.log(Level.FINE, "Cross process player search timeout");
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
			locks.readLock().unlock();
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
				otherChannelsInWorld.getWhenSafe(Byte.valueOf(ch)).sendPrivateChat(type, recipients, name, message);
			} else {
				//channel scan for these players
				locks.readLock().lock();
				try {
					for (CrossChannelCommunication ccc : otherChannelsInWorld.values())
						ccc.sendPrivateChat(type, recipients, name, message);
				} finally {
					locks.readLock().unlock();
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
		locks.readLock().lock();
		try {
			int remaining = 0;
			for (CrossChannelCommunication ccc : otherChannelsInWorld.values()) {
				ccc.sendWhisper(queue, recipient, sender, message);
				Pair<Byte, Object> result = queue.poll();
				if (result != null && ((Boolean) result.right).booleanValue())
					//immediately responded
					return true;
				remaining++;
			}

			long limit = System.currentTimeMillis() + 2000;
			while (remaining > 0) {
				Pair<Byte, Object> result = queue.poll(limit - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
				if (result == null) {
					LOG.log(Level.FINE, "Cross process player search timeout");
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
			locks.readLock().unlock();
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

		locks.readLock().lock();
		try {
			for (CrossChannelCommunication ccc : otherChannelsInWorld.values())
				if (ccc.sendSpouseChat(recipient, name, message))
					break;
		} finally {
			locks.readLock().unlock();
		}
	}

	/* package-private */ boolean receivedSpouseChat(int recipient, String sender, String message) {
		GameCharacter p = self.getPlayerById(recipient);
		if (p == null)
			return false;

		p.getClient().getSession().send(GamePackets.writeSpouseChatMessage(sender, message));
		return true;
	}
}
