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
	private final Member[] allMembers;
	private volatile int leader;
	private final Lock readLock, writeLock;

	public PartyList(int partyId) {
		this.id = partyId;
		this.localMembers = new HashMap<Integer, LocalMember>();
		this.remoteMembers = new HashMap<Byte, Map<Integer, RemoteMember>>();
		this.allMembers = new Member[6];
		for (int i = 0; i < 6; i++)
			allMembers[i] = EmptyMember.getInstance();

		ReadWriteLock locks = new ReentrantReadWriteLock();
		readLock = locks.readLock();
		writeLock = locks.writeLock();
	}

	public PartyList(int partyId, GameCharacter creator) {
		this(partyId);
		this.leader = creator.getId();
		addPlayer(new LocalMember(creator), false);
	}

	public int getId() {
		return id;
	}

	/**
	 * This PartyList must be at least read locked when this method is called.
	 * @return 
	 */
	public byte getMembersCount() {
		byte total = (byte) localMembers.size();
		for (Map<Integer, RemoteMember> channel : remoteMembers.values())
			total += channel.size();
		return total;
	}

	/**
	 * This PartyList must be at least read locked while the returned Collection is in scope.
	 * @return 
	 */
	public Collection<LocalMember> getMembersInLocalChannel() {
		return Collections.unmodifiableCollection(localMembers.values());
	}

	/**
	 * This PartyList must be at least read locked while the returned Member[] is in scope.
	 * @return 
	 */
	public Member[] getAllMembers() {
		return allMembers;
	}

	private void removeFromAllMembersAndCollapse(int playerId) {
		for (int i = 0; i < 6; i++) {
			if (allMembers[i].getPlayerId() == playerId) {
				for (; i < 5 && allMembers[i + 1].getPlayerId() != 0; i++)
					allMembers[i] = allMembers[i + 1];
				allMembers[i] = EmptyMember.getInstance();
				break;
			}
		}
	}

	private void addToAllMembers(Member member) {
		for (int i = 0; i < 6; i++) {
			if (allMembers[i].getPlayerId() == 0) {
				allMembers[i] = member;
				break;
			}
		}
	}

	private void syncWithAllMembers(Member member) {
		for (int i = 0; i < 6; i++) {
			if (allMembers[i].getPlayerId() == member.getPlayerId()) {
				allMembers[i] = member;
				break;
			}
		}
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

	/**
	 * This PartyList must be at least read locked when this method is called.
	 * @return 
	 */
	public boolean allOffline() {
		return localMembers.isEmpty() && remoteMembers.size() == 1 && remoteMembers.containsKey(Byte.valueOf(OFFLINE_CH));
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
		addPlayer(member, true);
		syncWithAllMembers(member);
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
		addPlayer(member, true);
		syncWithAllMembers(member);
	}

	private RemoteMember addToOffline(Member member) {
		Map<Integer, RemoteMember> others = remoteMembers.get(Byte.valueOf(OFFLINE_CH));
		if (others == null) {
			others = new HashMap<Integer, RemoteMember>();
			remoteMembers.put(Byte.valueOf(OFFLINE_CH), others);
		}
		RemoteMember offlineMember = new RemoteMember(member, OFFLINE_CH);
		others.put(Integer.valueOf(member.getPlayerId()), offlineMember);
		return offlineMember;
	}

	/**
	 * Moves the Member from the online list for a remote channel to the
	 * offline list.
	 * This PartyList must be write locked when this method is called.
	 * @param ch
	 * @param pId 
	 */
	public void memberDisconnected(byte ch, int pId) {
		syncWithAllMembers(addToOffline(removePlayer(ch, pId, true)));
	}

	/**
	 * Moves the Member from the online list for the local channel to the
	 * offline list.
	 * This PartyList must be write locked when this method is called.
	 * @param p 
	 */
	public void memberDisconnected(GameCharacter p) {
		syncWithAllMembers(addToOffline(removePlayer(p, true)));
	}

	private void addPlayer(RemoteMember member, boolean transition) {
		Map<Integer, RemoteMember> others = remoteMembers.get(Byte.valueOf(member.getChannel()));
		if (others == null) {
			others = new HashMap<Integer, RemoteMember>();
			remoteMembers.put(Byte.valueOf(member.getChannel()), others);
		}
		others.put(Integer.valueOf(member.getPlayerId()), member);
		if (!transition)
			addToAllMembers(member);
	}

	/**
	 * Adds a Member from a remote channel.
	 * This PartyList must be write locked when this method is called.
	 * @param member 
	 */
	public void addPlayer(RemoteMember member) {
		addPlayer(member, false);
	}

	private void addPlayer(LocalMember member, boolean transition) {
		localMembers.put(Integer.valueOf(member.getPlayerId()), member);
		if (!transition)
			addToAllMembers(member);
	}

	/**
	 * Adds a Member from the local channel.
	 * This PartyList must be write locked when this method is called.
	 * @param member 
	 */
	public void addPlayer(LocalMember member) {
		addPlayer(member, false);
	}

	private RemoteMember removePlayer(byte ch, int playerId, boolean transition) {
		if (!transition)
			removeFromAllMembersAndCollapse(playerId);
		Map<Integer, RemoteMember> others = remoteMembers.get(Byte.valueOf(ch));
		RemoteMember member = others.remove(Integer.valueOf(playerId));
		if (others.isEmpty())
			remoteMembers.remove(Byte.valueOf(ch));
		return member;
	}

	/**
	 * Removes a player from a remote channel.
	 * This PartyList must be write locked when this method is called.
	 * @param ch
	 * @param playerId
	 * @return 
	 */
	public RemoteMember removePlayer(byte ch, int playerId) {
		return removePlayer(ch, playerId, false);
	}

	private LocalMember removePlayer(GameCharacter p, boolean transition) {
		if (!transition)
			removeFromAllMembersAndCollapse(p.getId());
		return localMembers.remove(Integer.valueOf(p.getId()));
	}

	/**
	 * Removes a player from the local channel.
	 * This PartyList must be write locked when this method is called.
	 * @param p
	 * @return 
	 */
	public LocalMember removePlayer(GameCharacter p) {
		return removePlayer(p, false);
	}

	/**
	 * This PartyList must be at least read locked when this method is called.
	 * @param playerId
	 * @return 
	 */
	public RemoteMember getOfflineMember(int playerId) {
		return remoteMembers.get(Byte.valueOf(OFFLINE_CH)).get(Integer.valueOf(playerId));
	}

	/**
	 * This PartyList must be at least read locked when this method is called.
	 * @param playerId
	 * @return 
	 */
	public Member getMember(int playerId) {
		for (int i = 0; i < 6; i++)
			if (allMembers[i].getPlayerId() == playerId)
				return allMembers[i];
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
	 * This PartyList must be at least read locked when this method is called.
	 * @param position
	 * @return 
	 */
	public Member getMember(byte position) {
		return allMembers[position];
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
