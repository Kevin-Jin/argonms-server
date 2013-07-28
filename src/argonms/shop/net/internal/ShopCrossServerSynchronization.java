/*
 * ArgonMS MapleStory server emulator written in Java
 * Copyright (C) 2011-2013  GoldenKevin
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

package argonms.shop.net.internal;

import argonms.common.character.BuddyListEntry;
import argonms.common.util.collections.Pair;
import argonms.common.util.input.LittleEndianReader;
import argonms.shop.character.ShopCharacter;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 *
 * @author GoldenKevin
 */
public class ShopCrossServerSynchronization {
	private final Map<Byte, Map<Byte, ShopChannelSynchronization>> allChannels;
	private final Map<Byte, ShopCenterServerSynchronization> intraworldGroups;
	private final ReadWriteLock locks;

	public ShopCrossServerSynchronization() {
		allChannels = new HashMap<Byte, Map<Byte, ShopChannelSynchronization>>();
		intraworldGroups = new HashMap<Byte, ShopCenterServerSynchronization>();
		locks = new ReentrantReadWriteLock();
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

	public Pair<byte[], Integer> getChannelHost(byte world, byte ch) {
		lockRead();
		try {
			ShopChannelSynchronization css = allChannels.get(Byte.valueOf(world)).get(Byte.valueOf(ch));
			return new Pair<byte[], Integer>(css.getIpAddress(), Integer.valueOf(css.getPort()));
		} finally {
			unlockRead();
		}
	}

	public void addChannels(byte world, byte serverId, byte[] host, Map<Byte, Integer> ports) {
		lockWrite();
		try {
			Map<Byte, ShopChannelSynchronization> worldChannels = allChannels.get(Byte.valueOf(world));
			if (worldChannels == null) {
				worldChannels = new HashMap<Byte, ShopChannelSynchronization>();
				allChannels.put(Byte.valueOf(world), worldChannels);
				intraworldGroups.put(Byte.valueOf(world), new ShopCenterServerSynchronization(world));
			}
			for (Map.Entry<Byte, Integer> port : ports.entrySet()) {
				ShopChannelSynchronization cpccs = new ShopChannelSynchronization(world, port.getKey().byteValue(), host, port.getValue().intValue());
				worldChannels.put(port.getKey(), cpccs);
			}
		} finally {
			unlockWrite();
		}
	}

	public void removeChannels(byte world, Set<Byte> channels) {
		lockWrite();
		try {
			Map<Byte, ShopChannelSynchronization> worldChannels = allChannels.get(Byte.valueOf(world));
			if (worldChannels != null) {
				worldChannels.keySet().removeAll(channels);
				if (worldChannels.isEmpty())
					intraworldGroups.remove(Byte.valueOf(world));
			}
		} finally {
			unlockWrite();
		}
	}

	public void changeChannelPort(byte world, byte channel, int port) {
		lockWrite();
		try {
			Map<Byte, ShopChannelSynchronization> worldChannels = allChannels.get(Byte.valueOf(world));
			if (worldChannels != null) {
				ShopChannelSynchronization cpccs = worldChannels.get(Byte.valueOf(channel));
				if (cpccs != null)
					cpccs.setPort(port);
			}
		} finally {
			unlockWrite();
		}
	}

	public void receivedChannelShopSynchronizationPacket(LittleEndianReader packet) {
		byte srcWorld = packet.readByte();
		byte srcCh = packet.readByte();
		lockRead();
		try {
			allChannels.get(Byte.valueOf(srcWorld)).get(Byte.valueOf(srcCh)).receivedChannelShopSynchronizationPacket(packet);
		} finally {
			unlockRead();
		}
	}

	public void receivedCenterServerSynchronizationPacket(LittleEndianReader packet) {
		byte srcWorld = packet.readByte();
		lockRead();
		try {
			intraworldGroups.get(Byte.valueOf(srcWorld)).receivedCenterServerSynchronizationPacket(packet);
		} finally {
			unlockRead();
		}
	}

	public void sendChannelChangeRequest(byte destCh, ShopCharacter p) {
		lockRead();
		try {
			allChannels.get(Byte.valueOf(p.getClient().getWorld())).get(Byte.valueOf(destCh)).sendPlayerContext(p.getId(), p.getReturnContext());
		} finally {
			unlockRead();
		}
	}

	public void sendBuddyLogInNotifications(ShopCharacter p) {
		Collection<BuddyListEntry> buddies = p.getBuddyList().getBuddies();
		if (buddies.isEmpty())
			return;
		int[] recipients = new int[buddies.size()];
		int i = 0;
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
			for (ShopChannelSynchronization scs : allChannels.get(Byte.valueOf(p.getClient().getWorld())).values())
				scs.sendBuddyLogInNotifications(p.getId(), recipients);
		} finally {
			unlockRead();
		}
	}

	public void sendPartyMemberLogInNotifications(ShopCharacter p) {
		lockRead();
		try {
			intraworldGroups.get(Byte.valueOf(p.getClient().getWorld())).sendPartyMemberOnline(p);
		} finally {
			unlockRead();
		}
	}

	public void sendPartyMemberLogOffNotifications(ShopCharacter p, boolean loggingOff) {
		lockRead();
		try {
			intraworldGroups.get(Byte.valueOf(p.getClient().getWorld())).sendPartyMemberOffline(p, loggingOff);
		} finally {
			unlockRead();
		}
	}

	public void sendGuildMemberLogInNotifications(ShopCharacter p) {
		lockRead();
		try {
			intraworldGroups.get(Byte.valueOf(p.getClient().getWorld())).sendGuildMemberOnline(p);
		} finally {
			unlockRead();
		}
	}

	public void sendGuildMemberLogOffNotifications(ShopCharacter p, boolean loggingOff) {
		lockRead();
		try {
			intraworldGroups.get(Byte.valueOf(p.getClient().getWorld())).sendGuildMemberOffline(p, loggingOff);
		} finally {
			unlockRead();
		}
	}

	public void sendLeaveChatroom(int chatroomId, ShopCharacter leaver) {
		lockRead();
		try {
			intraworldGroups.get(Byte.valueOf(leaver.getClient().getWorld())).sendLeaveChatroom(chatroomId, leaver.getId());
		} finally {
			unlockRead();
		}
	}
}
