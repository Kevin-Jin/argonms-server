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
public class Party {
	public static final byte OFFLINE_CH = -1;

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
	//lock for the entire Party instance rather than two ConcurrentHashMaps
	private final Map<Integer, Member> allMembers;
	private final Map<Byte, Map<Integer, Member>> channelMembers;
	private final Lock readLock, writeLock;
	private volatile int leader;

	public Party() {
		allMembers = new LinkedHashMap<Integer, Member>(6);
		channelMembers = new HashMap<Byte, Map<Integer, Member>>();
		ReadWriteLock locks = new ReentrantReadWriteLock();
		readLock = locks.readLock();
		writeLock = locks.writeLock();
	}

	public Party(Member creator) {
		this();

		this.leader = creator.getPlayerId();
		//no locking necessary as long as we don't leak this reference
		addPlayer(creator, false);
	}

	/**
	 * This Party must be at least read locked while the returned Collection is in scope.
	 * @param channel
	 * @return 
	 */
	public Collection<Member> getMembersOfChannel(byte channel) {
		Map<Integer, Member> members = channelMembers.get(Byte.valueOf(channel));
		if (members == null)
			return Collections.emptyList();
		return members.values();
	}

	/**
	 * This Party must be at least read locked while the returned Collection is in scope.
	 * @return 
	 */
	public Collection<Member> getAllMembers() {
		return allMembers.values();
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
		return allMembers.size() >= 6;
	}

	private void addPlayer(Member member, boolean transition) {
		Byte oCh = Byte.valueOf(member.getChannel());
		Integer oId = Integer.valueOf(member.getPlayerId());

		Map<Integer, Member> othersOnChannel = channelMembers.get(oCh);
		if (othersOnChannel == null) {
			othersOnChannel = new HashMap<Integer, Member>();
			channelMembers.put(oCh, othersOnChannel);
		}
		othersOnChannel.put(Integer.valueOf(member.getPlayerId()), member);

		if (!transition)
			allMembers.put(oId, member);
	}

	/**
	 * This Party must be write locked when this method is called.
	 * @param member 
	 */
	public void addPlayer(Member member) {
		addPlayer(member, false);
	}

	public boolean removePlayer(int playerId, boolean transition) {
		boolean success;
		Member removed = !transition ? allMembers.remove(Integer.valueOf(playerId)) : allMembers.get(Integer.valueOf(playerId));
		if (removed != null) {
			success = true;
			Map<Integer, Member> others = channelMembers.get(Byte.valueOf(removed.getChannel()));
			others.remove(Integer.valueOf(playerId));
			if (others.isEmpty())
				channelMembers.remove(Byte.valueOf(removed.getChannel()));
		} else {
			success = false;
		}
		return success;
	}

	/**
	 * This Party must be write locked when this method is called.
	 * @param playerId 
	 * @return true if the player was removed, false otherwise
	 */
	public boolean removePlayer(int playerId) {
		return removePlayer(playerId, false);
	}

	/**
	 * This Party must be write locked when this method is called.
	 * @param playerId 
	 * @param newCh
	 * @return true if the player was moved, false otherwise
	 */
	public boolean setMemberChannel(int playerId, byte newCh) {
		boolean success = false;
		Member mem = allMembers.get(Integer.valueOf(playerId));
		if (mem != null) {
			removePlayer(playerId, true);
			mem.setChannel(newCh);
			addPlayer(mem, true);
			success = true;
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

	/**
	 * This Party must be at least read locked when this method is called.
	 * @return 
	 */
	public Member getMember(int playerId) {
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
