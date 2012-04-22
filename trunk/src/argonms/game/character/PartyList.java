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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 *
 * @author GoldenKevin
 */
public class PartyList {
	public static final byte CASH_SHOP_CH = 0;
	public static final byte OFFLINE_CH = -1;

	public interface Member {
		public int getPlayerId();
		public String getName();
		public short getJob();
		public short getLevel();
		public byte getChannel();
		public int getMapId();
	}

	public static class RemoteMember implements Member {
		private final int playerId;
		private final String name;
		private short job;
		private short level;
		private final byte channel;

		public RemoteMember(int playerId, String name, short job, short level, byte channel) {
			this.playerId = playerId;
			this.name = name;
			this.job = job;
			this.level = level;
			this.channel = channel;
		}

		public RemoteMember(Member copy, byte channel) {
			this.playerId = copy.getPlayerId();
			this.name = copy.getName();
			this.job = copy.getJob();
			this.level = copy.getLevel();
			this.channel = channel;
		}

		@Override
		public int getPlayerId() {
			return playerId;
		}

		@Override
		public String getName() {
			return name;
		}

		@Override
		public short getJob() {
			return job;
		}

		@Override
		public short getLevel() {
			return level;
		}

		@Override
		public byte getChannel() {
			return channel;
		}

		@Override
		public int getMapId() {
			return 0;
		}

		public void setLevel(short newValue) {
			level = newValue;
		}

		public void setJob(short newValue) {
			job = newValue;
		}
	}

	public static class LocalMember implements Member {
		private final GameCharacter player;

		public LocalMember(GameCharacter player) {
			this.player = player;
		}

		public GameCharacter getPlayer() {
			return player;
		}

		@Override
		public int getPlayerId() {
			return player.getId();
		}

		@Override
		public String getName() {
			return player.getName();
		}

		@Override
		public short getJob() {
			return player.getJob();
		}

		@Override
		public short getLevel() {
			return player.getLevel();
		}

		@Override
		public byte getChannel() {
			return player.getClient().getChannel();
		}

		@Override
		public int getMapId() {
			return player.getMapId();
		}
	}

	public static class EmptyMember implements Member {
		private static EmptyMember instance = new EmptyMember();

		private EmptyMember() {
			
		}

		@Override
		public int getPlayerId() {
			return 0;
		}

		@Override
		public String getName() {
			return "";
		}

		@Override
		public short getJob() {
			return 0;
		}

		@Override
		public short getLevel() {
			return 0;
		}

		@Override
		public byte getChannel() {
			return OFFLINE_CH;
		}

		@Override
		public int getMapId() {
			return 0;
		}

		public static EmptyMember getInstance() {
			return instance;
		}
	}

	private final int id;
	//members on current channel
	private final Map<Integer, LocalMember> localMembers;
	//members on other channels
	private final Map<Byte, Map<Integer, RemoteMember>> remoteMembers;
	private volatile int leader;
	private final Lock readLock, writeLock;

	public PartyList(int partyId, GameCharacter init, boolean loggedOn, boolean leader) {
		this.id = partyId;
		this.localMembers = new HashMap<Integer, LocalMember>();
		this.remoteMembers = new HashMap<Byte, Map<Integer, RemoteMember>>();

		ReadWriteLock locks = new ReentrantReadWriteLock();
		readLock = locks.readLock();
		writeLock = locks.writeLock();

		if (leader)
			this.leader = init.getId();

		if (loggedOn)
			addPlayer(new LocalMember(init));
		else
			addToOffline(new RemoteMember(new LocalMember(init), OFFLINE_CH));
	}

	public int getId() {
		return id;
	}

	/**
	 * This PartyList must be at least read locked while the returned Collection is in scope.
	 * @return 
	 */
	public Collection<LocalMember> getMembersInLocalChannel() {
		return Collections.unmodifiableCollection(localMembers.values());
	}

	/**
	 * This PartyList must be at least read locked while the returned List is in scope.
	 * @return 
	 */
	public List<Member> getAllMembers() {
		List<Member> all = new ArrayList<Member>();
		all.addAll(localMembers.values());
		for (Map<Integer, RemoteMember> otherChannel : remoteMembers.values())
			all.addAll(otherChannel.values());
		for (int i = all.size(); i < 6; i++)
			all.add(EmptyMember.getInstance());
		return all;
	}

	/**
	 * This PartyList must be at least read locked while the returned List is in scope.
	 * @param mapId
	 * @return 
	 */
	public List<GameCharacter> getLocalMembersInMap(int mapId) {
		List<GameCharacter> filtered = new ArrayList<GameCharacter>();
		for (LocalMember m : localMembers.values())
			if (m.getMapId() == mapId)
				filtered.add(m.getPlayer());
		return filtered;
	}

	private void removeFromOffline(Member member) {
		Map<Integer, RemoteMember> others = remoteMembers.get(Byte.valueOf(OFFLINE_CH));
		others.remove(Integer.valueOf(member.getPlayerId()));
		if (others.isEmpty())
			remoteMembers.remove(Byte.valueOf(OFFLINE_CH));
	}

	/**
	 * Moves the Member from the offline list to the online list for a remote
	 * channel.
	 * This PartyList must be write locked when this method is called.
	 * @param member 
	 */
	public void memberConnected(RemoteMember member) {
		removeFromOffline(member);
		addPlayer(member);
	}

	/**
	 * Moves the Member from the offline list to the online list for the local
	 * channel.
	 * This PartyList must be write locked when this method is called.
	 * @param p 
	 */
	public void memberConnected(GameCharacter p) {
		LocalMember member = new LocalMember(p);
		removeFromOffline(member);
		addPlayer(member);
	}

	private void addToOffline(Member member) {
		Map<Integer, RemoteMember> others = remoteMembers.get(Byte.valueOf(OFFLINE_CH));
		if (others == null) {
			others = new HashMap<Integer, RemoteMember>();
			remoteMembers.put(Byte.valueOf(OFFLINE_CH), others);
		}
		others.put(Integer.valueOf(member.getPlayerId()), new RemoteMember(member, OFFLINE_CH));
	}

	/**
	 * Moves the Member from the online list for a remote channel to the
	 * offline list.
	 * This PartyList must be write locked when this method is called.
	 * @param ch
	 * @param pId 
	 */
	public void memberDisconnected(byte ch, int pId) {
		addToOffline(removePlayer(ch, pId));
	}

	/**
	 * Moves the Member from the online list for the local channel to the
	 * offline list.
	 * This PartyList must be write locked when this method is called.
	 * @param p 
	 */
	public void memberDisconnected(GameCharacter p) {
		addToOffline(removePlayer(p));
	}

	/**
	 * Adds a Member from a remote channel.
	 * This PartyList must be write locked when this method is called.
	 * @param member 
	 */
	public void addPlayer(RemoteMember member) {
		Map<Integer, RemoteMember> others = remoteMembers.get(Byte.valueOf(member.getChannel()));
		if (others == null) {
			others = new HashMap<Integer, RemoteMember>();
			remoteMembers.put(Byte.valueOf(member.getChannel()), others);
		}
		others.put(Integer.valueOf(member.getPlayerId()), member);
	}

	/**
	 * Adds a Member from the local channel.
	 * This PartyList must be write locked when this method is called.
	 * @param member 
	 */
	public void addPlayer(LocalMember member) {
		localMembers.put(Integer.valueOf(member.getPlayerId()), member);
	}

	/**
	 * Removes a player from a remote channel.
	 * This PartyList must be write locked when this method is called.
	 * @param ch
	 * @param playerId
	 * @return 
	 */
	public RemoteMember removePlayer(byte ch, int playerId) {
		Map<Integer, RemoteMember> others = remoteMembers.get(Byte.valueOf(ch));
		RemoteMember member = others.remove(Integer.valueOf(playerId));
		if (others.isEmpty())
			remoteMembers.remove(Byte.valueOf(ch));
		return member;
	}

	/**
	 * Removes a player from the local channel.
	 * This PartyList must be write locked when this method is called.
	 * @param p
	 * @return 
	 */
	public LocalMember removePlayer(GameCharacter p) {
		return localMembers.remove(Integer.valueOf(p.getId()));
	}

	public RemoteMember getOfflineMember(int playerId) {
		return remoteMembers.get(Byte.valueOf(OFFLINE_CH)).get(Integer.valueOf(playerId));
	}

	/**
	 * This PartyList must be at least read locked when this method is called.
	 * @param playerId
	 * @return 
	 */
	public Member getMember(int playerId) {
		Member member = localMembers.get(Integer.valueOf(playerId));
		if (member != null)
			return member;
		for (Map<Integer, RemoteMember> channel : remoteMembers.values()) {
			member = channel.get(Integer.valueOf(playerId));
			if (member != null)
				return member;
		}
		return null;
	}

	/**
	 * Gets the RemoteMember in the specified channel.
	 * This PartyList must be at least read locked when this method is called.
	 * @param playerId
	 * @return 
	 */
	public RemoteMember getMember(byte channel, int playerId) {
		Map<Integer, RemoteMember> channelMembers = remoteMembers.get(Byte.valueOf(channel));
		if (channelMembers != null)
			return channelMembers.get(Integer.valueOf(playerId));
		return null;
	}

	/**
	 * This PartyList does NOT need to be locked when this method is called.
	 */
	public int getLeader() {
		return leader;
	}

	/**
	 * This PartyList does NOT need to be locked when this method is called.
	 */
	public void setLeader(int leader) {
		this.leader = leader;
	}

	/**
	 * This PartyList must be at least read locked when this method is called.
	 * @return 
	 */
	public boolean isFull() {
		int count = localMembers.size();
		for (Map<Integer, RemoteMember> channel : remoteMembers.values())
			count += channel.size();
		return count >= 6;
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
