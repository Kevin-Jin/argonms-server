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

package argonms.common.character;

import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 *
 * @author GoldenKevin
 */
public abstract class LoggedInPlayer extends Player {
	private final ReadWriteLock statLocks;
	private final ReadWriteLock questLocks;

	protected LoggedInPlayer() {
		statLocks = new ReentrantReadWriteLock();
		questLocks = new ReentrantReadWriteLock();
	}

	public abstract byte getBuddyListCapacity();

	public abstract int getMesos();

	public abstract Map<Integer, SkillEntry> getSkillEntries();

	public abstract Map<Integer, Cooldown> getCooldowns();

	/**
	 * Quests must be at least read locked while the returned Map is in scope.
	 * @return 
	 */
	public abstract Map<Short, QuestEntry> getAllQuests();

	public void readLockStats() {
		statLocks.readLock().lock();
	}

	public void readUnlockStats() {
		statLocks.readLock().unlock();
	}

	public void writeLockStats() {
		statLocks.writeLock().lock();
	}

	public void writeUnlockStats() {
		statLocks.writeLock().unlock();
	}

	public void readLockQuests() {
		questLocks.readLock().lock();
	}

	public void readUnlockQuests() {
		questLocks.readLock().unlock();
	}

	public void writeLockQuests() {
		questLocks.readLock().lock();
	}

	public void writeUnlockQuests() {
		questLocks.readLock().unlock();
	}
}
