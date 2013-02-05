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
public abstract class IntraworldGroupList<M extends IntraworldGroupList.Member,
		R extends IntraworldGroupList.RemoteMember,
		L extends IntraworldGroupList.LocalMember> {
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

	private final int id;
	//members on current channel
	protected final Map<Integer, L> localMembers;
	//members on other channels
	protected final Map<Byte, Map<Integer, R>> remoteMembers;
	private final Lock readLock, writeLock;

	public IntraworldGroupList(int groupId) {
		this.id = groupId;
		this.localMembers = new HashMap<Integer, L>();
		this.remoteMembers = new HashMap<Byte, Map<Integer, R>>();

		ReadWriteLock locks = new ReentrantReadWriteLock();
		readLock = locks.readLock();
		writeLock = locks.writeLock();
	}

	protected abstract L createLocalMember(GameCharacter p);

	protected abstract R createRemoteMember(Member member, byte channel);

	public int getId() {
		return id;
	}

	/**
	 * This IntraworldGroupList must be at least read locked when this method is called.
	 * @return 
	 */
	public byte getMembersCount() {
		byte total = (byte) localMembers.size();
		for (Map<Integer, R> channel : remoteMembers.values())
			total += channel.size();
		return total;
	}

	/**
	 * This IntraworldGroupList must be at least read locked while the returned Collection is in scope.
	 * @return 
	 */
	public Collection<L> getMembersInLocalChannel() {
		return Collections.unmodifiableCollection(localMembers.values());
	}

	/**
	 * This IntraworldGroupList must be at least read locked while the returned Member[] is in scope.
	 * @return 
	 */
	public abstract Member[] getAllMembers();

	/**
	 * This IntraworldGroupList must be at least read locked while the returned List is in scope.
	 * @param mapId
	 * @return 
	 */
	public List<GameCharacter> getLocalMembersInMap(int mapId) {
		List<GameCharacter> filtered = new ArrayList<GameCharacter>();
		for (L m : localMembers.values())
			if (m.getMapId() == mapId)
				filtered.add(m.getPlayer());
		return filtered;
	}

	/**
	 * This IntraworldGroupList must be at least read locked when this method is called.
	 * @return 
	 */
	public abstract boolean allOffline();

	protected abstract void removeFromOffline(Member member);

	/**
	 * Moves the Member from the offline list to the online list for a remote
	 * channel.
	 * This IntraworldGroupList must be write locked when this method is called.
	 * @param member 
	 */
	public void memberConnected(R member) {
		removeFromOffline(member);
		addPlayer(member, true);
	}

	/**
	 * Moves the Member from the offline list to the online list for the local
	 * channel.
	 * This IntraworldGroupList must be write locked when this method is called.
	 * @param p 
	 */
	public L memberConnected(GameCharacter p) {
		L member = createLocalMember(p);
		removeFromOffline(member);
		addPlayer(member, true);
		return member;
	}

	protected abstract R addToOffline(Member member);

	/**
	 * Moves the Member from the online list for a remote channel to the
	 * offline list.
	 * This IntraworldGroupList must be write locked when this method is called.
	 * @param ch
	 * @param pId 
	 */
	public R memberDisconnected(byte ch, int pId) {
		return addToOffline(removePlayer(ch, pId, true));
	}

	/**
	 * Moves the Member from the online list for the local channel to the
	 * offline list.
	 * This IntraworldGroupList must be write locked when this method is called.
	 * @param p 
	 */
	public R memberDisconnected(GameCharacter p) {
		return addToOffline(removePlayer(p, true));
	}

	protected void addPlayer(R member, boolean transition) {
		Map<Integer, R> others = remoteMembers.get(Byte.valueOf(member.getChannel()));
		if (others == null) {
			others = new HashMap<Integer, R>();
			remoteMembers.put(Byte.valueOf(member.getChannel()), others);
		}
		others.put(Integer.valueOf(member.getPlayerId()), member);
	}

	/**
	 * Adds a Member from a remote channel.
	 * This IntraworldGroupList must be write locked when this method is called.
	 * @param member 
	 */
	public void addPlayer(R member) {
		addPlayer(member, false);
	}

	protected void addPlayer(L member, boolean transition) {
		localMembers.put(Integer.valueOf(member.getPlayerId()), member);
	}

	/**
	 * Adds a Member from the local channel.
	 * This IntraworldGroupList must be write locked when this method is called.
	 * @param member 
	 */
	public void addPlayer(L member) {
		addPlayer(member, false);
	}

	protected R removePlayer(byte ch, int playerId, boolean transition) {
		Map<Integer, R> others = remoteMembers.get(Byte.valueOf(ch));
		R member = others.remove(Integer.valueOf(playerId));
		if (others.isEmpty())
			remoteMembers.remove(Byte.valueOf(ch));
		return member;
	}

	/**
	 * Removes a player from a remote channel.
	 * This IntraworldGroupList must be write locked when this method is called.
	 * @param ch
	 * @param playerId
	 * @return 
	 */
	public R removePlayer(byte ch, int playerId) {
		return removePlayer(ch, playerId, false);
	}

	protected L removePlayer(GameCharacter p, boolean transition) {
		return localMembers.remove(Integer.valueOf(p.getId()));
	}

	/**
	 * Removes a player from the local channel.
	 * This IntraworldGroupList must be write locked when this method is called.
	 * @param p
	 * @return 
	 */
	public L removePlayer(GameCharacter p) {
		return removePlayer(p, false);
	}

	/**
	 * This IntraworldGroupList must be at least read locked when this method is called.
	 * @param playerId
	 * @return 
	 */
	public abstract R getOfflineMember(int playerId);

	/**
	 * This IntraworldGroupList must be at least read locked when this method is called.
	 * @param playerId
	 * @return 
	 */
	public abstract M getMember(int playerId);

	/**
	 * Gets the RemoteMember in the specified channel.
	 * This IntraworldGroupList must be at least read locked when this method is called.
	 * @param playerId
	 * @return 
	 */
	public R getMember(byte channel, int playerId) {
		Map<Integer, R> channelMembers = remoteMembers.get(Byte.valueOf(channel));
		if (channelMembers != null)
			return channelMembers.get(Integer.valueOf(playerId));
		return null;
	}

	/**
	 * This IntraworldGroupList must be at least read locked when this method is called.
	 * @return 
	 */
	public boolean isFull() {
		int count = localMembers.size();
		for (Map<Integer, R> channel : remoteMembers.values())
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
