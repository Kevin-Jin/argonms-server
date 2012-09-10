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

package argonms.game.character;

import argonms.common.character.inventory.Inventory;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 *
 * @author GoldenKevin
 */
public class Chatroom {
	public static final byte
		ACT_OPEN = 0,
		ACT_JOIN = 1,
		ACT_EXIT = 2,
		ACT_INVITE = 3,
		ACT_INVITE_RESPONSE = 4,
		ACT_DECLINE = 5,
		ACT_CHAT = 6,
		ACT_REFRESH_AVATAR = 7
	;

	public static class Avatar {
		private final int playerId;
		private final byte gender;
		private final byte skin;
		private final int eyes;
		private final int hair;
		private final Map<Short, Integer> equips;
		private final String name;
		private final byte channel;

		public Avatar(int playerId, byte gender, byte skin, int eyes, int hair, Map<Short, Integer> equips, String name, byte channel) {
			this.playerId = playerId;
			this.gender = gender;
			this.skin = skin;
			this.eyes = eyes;
			this.hair = hair;
			this.equips = equips;
			this.name = name;
			this.channel = channel;
		}

		public Avatar(GameCharacter p) {
			playerId = p.getId();
			gender = p.getGender();
			skin = p.getSkinColor();
			eyes = p.getEyes();
			hair = p.getHair();
			equips = p.getInventory(Inventory.InventoryType.EQUIPPED).getItemIds();
			name = p.getName();
			channel = p.getClient().getChannel();
		}

		public int getPlayerId() {
			return playerId;
		}

		public byte getGender() {
			return gender;
		}

		public byte getSkin() {
			return skin;
		}

		public int getEyes() {
			return eyes;
		}

		public int getHair() {
			return hair;
		}

		public Map<Short, Integer> getEquips() {
			return equips;
		}

		public String getName() {
			return name;
		}

		public byte getChannel() {
			return channel;
		}
	}

	private final int roomId;
	private final Avatar[] occupants;
	private final Set<Byte> localAvatarPositions;
	private final Lock readLock, writeLock;

	public Chatroom(int roomId) {
		this.roomId = roomId;
		occupants = new Avatar[3];
		localAvatarPositions = new HashSet<Byte>(3);

		ReadWriteLock locks = new ReentrantReadWriteLock();
		readLock = locks.readLock();
		writeLock = locks.writeLock();
	}

	public Chatroom(int roomId, GameCharacter init) {
		this(roomId);
		setAvatar((byte) 0, new Avatar(init), true);
	}

	public int getRoomId() {
		return roomId;
	}

	/**
	 * This Chatroom must be write locked when this method is called.
	 * @param pos
	 * @param a
	 * @param local 
	 */
	public final void setAvatar(byte pos, Avatar a, boolean local) {
		occupants[pos] = a;
		if (local && a != null)
			localAvatarPositions.add(Byte.valueOf(pos));
		else
			localAvatarPositions.remove(Byte.valueOf(pos));
	}

	/**
	 * This Chatroom must be at least read locked when this method is called.
	 * @param pos
	 * @return 
	 */
	public Avatar getAvatar(byte pos) {
		return occupants[pos];
	}

	/**
	 * This Chatroom must be at least read locked when this method is called.
	 * @param playerId
	 * @return 
	 */
	public byte positionOf(int playerId) {
		for (byte i = 0; i < 3; i++)
			if (occupants[i] != null && occupants[i].getPlayerId() == playerId)
				return i;
		return -1;
	}

	/**
	 * This Chatroom must be at least read locked when this method is called.
	 * @return 
	 */
	public boolean isFull() {
		byte occupied = 0;
		for (byte i = 0; i < 3; i++)
			if (occupants[i] != null)
				occupied++;
		return occupied == 3;
	}

	/**
	 * This Chatroom must be at least read locked when this method is called.
	 * @return 
	 */
	public Set<Byte> localChannelSlots() {
		return new HashSet<Byte>(localAvatarPositions);
	}

	/**
	 * This Chatroom must be at least read locked when this method is called.
	 * @return 
	 */
	public Set<Byte> allChannels() {
		Set<Byte> set = new HashSet<Byte>(3);
		for (byte i = 0; i < 3; i++)
			if (occupants[i] != null)
				set.add(Byte.valueOf(occupants[i].getChannel()));
		return set;
	}

	public void lockRead() {
		readLock.lock();
	}

	public void unlockRead() {
		readLock.unlock();
	}

	public void lockWrite() {
		writeLock.lock();
	}

	public void unlockWrite() {
		writeLock.unlock();
	}
}
