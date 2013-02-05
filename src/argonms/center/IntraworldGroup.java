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

package argonms.center;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 *
 * @author GoldenKevin
 */
public abstract class IntraworldGroup<T extends IntraworldGroup.Member> {
	public static class Member {
		private final int playerId;
		private final String name;
		private short job;
		private short level;
		private byte channel;

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

		public void setJob(short newValue) {
			job = newValue;
		}

		public void setLevel(short newValue) {
			level = newValue;
		}

		public void setChannel(byte newCh) {
			channel = newCh;
		}
	}

	//we want to keep playerChannels and channelPlayers sync'd, so use a single
	//lock for the entire IntraworldGroup instance rather than two ConcurrentHashMaps
	private final Map<Integer, T> allMembers;
	private final Map<Byte, Map<Integer, T>> channelMembers;
	private final Lock readLock, writeLock;

	public IntraworldGroup() {
		allMembers = new LinkedHashMap<Integer, T>(6);
		channelMembers = new HashMap<Byte, Map<Integer, T>>();
		ReadWriteLock locks = new ReentrantReadWriteLock();
		readLock = locks.readLock();
		writeLock = locks.writeLock();
	}

	/**
	 * This IntraworldGroup must be at least read locked while the returned Collection is in scope.
	 * @param channel
	 * @return 
	 */
	public Collection<T> getMembersOfChannel(byte channel) {
		Map<Integer, T> members = channelMembers.get(Byte.valueOf(channel));
		if (members == null)
			return Collections.emptyList();
		return members.values();
	}

	/**
	 * This IntraworldGroup must be at least read locked while the returned Collection is in scope.
	 * @return 
	 */
	public Collection<T> getAllMembers() {
		return allMembers.values();
	}

	/**
	 * This IntraworldGroup must be at least read locked while the returned Set is in scope.
	 * @return 
	 */
	public Set<Byte> allChannels() {
		return channelMembers.keySet();
	}

	/**
	 * This IntraworldGroup must be at least read locked when this method is called.
	 * @return 
	 */
	public boolean isFull() {
		return allMembers.size() >= 6;
	}

	private void addPlayer(T member, boolean transition) {
		Byte oCh = Byte.valueOf(member.getChannel());
		Integer oId = Integer.valueOf(member.getPlayerId());

		Map<Integer, T> othersOnChannel = channelMembers.get(oCh);
		if (othersOnChannel == null) {
			othersOnChannel = new HashMap<Integer, T>();
			channelMembers.put(oCh, othersOnChannel);
		}
		othersOnChannel.put(Integer.valueOf(member.getPlayerId()), member);

		if (!transition)
			allMembers.put(oId, member);
	}

	/**
	 * This IntraworldGroup must be write locked when this method is called.
	 * @param member 
	 */
	public void addPlayer(T member) {
		addPlayer(member, false);
	}

	public boolean removePlayer(int playerId, boolean transition) {
		boolean success;
		Member removed = !transition ? allMembers.remove(Integer.valueOf(playerId)) : allMembers.get(Integer.valueOf(playerId));
		if (removed != null) {
			success = true;
			Map<Integer, T> others = channelMembers.get(Byte.valueOf(removed.getChannel()));
			others.remove(Integer.valueOf(playerId));
			if (others.isEmpty())
				channelMembers.remove(Byte.valueOf(removed.getChannel()));
		} else {
			success = false;
		}
		return success;
	}

	/**
	 * This IntraworldGroup must be write locked when this method is called.
	 * @param playerId 
	 * @return true if the player was removed, false otherwise
	 */
	public boolean removePlayer(int playerId) {
		return removePlayer(playerId, false);
	}

	/**
	 * This IntraworldGroup must be write locked when this method is called.
	 * @param playerId 
	 * @param newCh
	 * @return true if the player was moved, false otherwise
	 */
	public boolean setMemberChannel(int playerId, byte newCh) {
		boolean success = false;
		T mem = allMembers.get(Integer.valueOf(playerId));
		if (mem != null) {
			removePlayer(playerId, true);
			mem.setChannel(newCh);
			addPlayer(mem, true);
			success = true;
		}
		return success;
	}

	/**
	 * This IntraworldGroup must be at least read locked when this method is called.
	 * @return 
	 */
	public T getMember(int playerId) {
		return allMembers.get(Integer.valueOf(playerId));
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
