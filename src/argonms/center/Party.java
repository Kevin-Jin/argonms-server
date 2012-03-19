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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 *
 * @author GoldenKevin
 */
public final class Party {
	public static class Member {
		private final int playerId;
		private final String name;
		private final short job;
		private final short level;
		private final byte channel;

		public Member(int playerId, String name, short job, short level, byte channel) {
			this.playerId = playerId;
			this.name = name;
			this.job = job;
			this.level = level;
			this.channel = channel;
		}

		public int getPlayerId() {
			return playerId;
		}

		public String getName() {
			return name;
		}

		public short getJob() {
			return job;
		}

		public short getLevel() {
			return level;
		}

		public byte getChannel() {
			return channel;
		}
	}

	//we want to keep playerChannels and channelPlayers sync'd, so use a single
	//lock for the entire Party instance rather than two ConcurrentHashMaps
	private final Map<Integer, Byte> playerMembers;
	private final Map<Byte, Map<Integer, Member>> channelMembers;
	private final Lock readLock, writeLock;
	private volatile int leader;

	public Party(Member leader) {
		playerMembers = new HashMap<Integer, Byte>();
		channelMembers = new HashMap<Byte, Map<Integer, Member>>();
		ReadWriteLock locks = new ReentrantReadWriteLock();
		readLock = locks.readLock();
		writeLock = locks.writeLock();

		this.leader = leader.getPlayerId();
		lockWrite();
		try {
			addPlayer(leader);
		} finally {
			unlockWrite();
		}
	}

	/**
	 * This Party must be at least read locked while the returned Collection is in scope.
	 * @param channel
	 * @return 
	 */
	public Collection<Member> getMembersOfChannel(byte channel) {
		return channelMembers.get(Byte.valueOf(channel)).values();
	}

	/**
	 * This Party must be at least read locked while the returned Set is in scope.
	 * @return 
	 */
	public Set<Map.Entry<Byte, Map<Integer, Member>>> getAllMembers() {
		return channelMembers.entrySet();
	}

	/**
	 * This Party must be at least read locked while the returned Set is in scope.
	 * @return 
	 */
	public Set<Byte> allChannels() {
		return channelMembers.keySet();
	}

	/**
	 * This Party must be at least read locked when this method is called.
	 * @return 
	 */
	public boolean isFull() {
		return playerMembers.size() >= 6;
	}

	/**
	 * This Party must be write locked when this method is called.
	 * @param member 
	 */
	public void addPlayer(Member member) {
		Byte oCh = Byte.valueOf(member.getChannel());
		Integer oId = Integer.valueOf(member.getPlayerId());

		Map<Integer, Member> othersOnChannel = channelMembers.get(oCh);
		if (othersOnChannel == null) {
			othersOnChannel = new HashMap<Integer, Member>();
			channelMembers.put(oCh, othersOnChannel);
		}
		othersOnChannel.put(Integer.valueOf(member.getPlayerId()), member);

		playerMembers.put(oId, oCh);
	}

	/**
	 * This Party must be write locked when this method is called.
	 * @param playerId 
	 * @return true if the player was removed, false otherwise
	 */
	public boolean removePlayer(int playerId) {
		boolean success;
		Byte ch = playerMembers.remove(Integer.valueOf(playerId));
		if (ch != null) {
			success = true;
			Map<Integer, Member> others = channelMembers.get(ch);
			others.remove(Integer.valueOf(playerId));
			if (others.isEmpty())
				channelMembers.remove(ch);
		} else {
			success = false;
		}
		return success;
	}

	/**
	 * This Party does NOT need to be locked when this method is called.
	 * @param leader 
	 */
	public void setLeader(int leader) {
		this.leader = leader;
	}

	/**
	 * This Party does NOT need to be locked when this method is called.
	 */
	public int getLeader() {
		return leader;
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
