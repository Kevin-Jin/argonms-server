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

package argonms.center;

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

		public Avatar(Avatar copy, byte channel) {
			this.playerId = copy.playerId;
			this.gender = copy.gender;
			this.skin = copy.skin;
			this.eyes = copy.eyes;
			this.hair = copy.hair;
			this.equips = copy.equips;
			this.name = copy.name;
			this.channel = channel;
		}

		public Avatar(Avatar copy, Map<Short, Integer> equips, byte skin, int eyes, int hair) {
			this.playerId = copy.playerId;
			this.gender = copy.gender;
			this.skin = skin;
			this.eyes = eyes;
			this.hair = hair;
			this.equips = equips;
			this.name = copy.name;
			this.channel = copy.channel;
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

	private final Avatar[] occupants;
	private final Set<Byte> channels;
	private final Lock readLock, writeLock;
	private int count;

	public Chatroom(Avatar creator) {
		occupants = new Avatar[3];
		channels = new HashSet<Byte>(3);
		ReadWriteLock locks = new ReentrantReadWriteLock();
		readLock = locks.readLock();
		writeLock = locks.writeLock();

		occupants[0] = creator;
		channels.add(Byte.valueOf(creator.getChannel()));
		count = 1;
	}

	/**
	 * This Chatroom must be at least read locked while the returned Set is in scope.
	 * @param channel
	 * @return 
	 */
	public Set<Byte> allChannels() {
		return channels;
	}

	/**
	 * This Chatroom must be write locked when this method is called.
	 * @param chatter 
	 * @return the position the player was added in, or -1 if full
	 */
	public byte addPlayer(Avatar chatter) {
		for (byte i = 0; i < 3; i++) {
			if (occupants[i] == null) {
				occupants[i] = chatter;
				channels.add(Byte.valueOf(chatter.getChannel()));
				count++;
				return i;
			}
		}
		return -1;
	}

	private void rebuildChannelSet() {
		channels.clear();
		for (byte i = 0; i < 3; i++)
			if (occupants[i] != null)
				channels.add(Byte.valueOf(occupants[i].getChannel()));
	}

	/**
	 * This Chatroom must be write locked when this method is called.
	 * @param playerId 
	 * @return the Avatar removed from the position
	 */
	public Avatar removePlayer(byte position) {
		Avatar a = occupants[position];
		occupants[position] = null;
		count--;
		rebuildChannelSet();
		return a;
	}

	/**
	 * This Chatroom must be write locked when this method is called.
	 * @param pos
	 * @param a 
	 */
	public void setPlayer(byte pos, Avatar a) {
		if (occupants[pos] == null)
			count++;
		occupants[pos] = a;
		rebuildChannelSet();
	}

	/**
	 * This Chatroom must be at least read locked when this method is called.
	 * @return 
	 */
	public Avatar getPlayer(byte position) {
		return occupants[position];
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
	 * @return true if there are no more players in this Chatroom, false
	 * otherwise
	 */
	public boolean isEmpty() {
		return count == 0;
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
