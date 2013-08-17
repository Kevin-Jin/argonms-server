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

import argonms.common.character.inventory.InventorySlot;
import argonms.common.character.inventory.Pet;
import argonms.common.util.Scheduler;
import java.util.HashSet;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 *
 * @author GoldenKevin
 */
public abstract class LoggedInPlayer extends Player {
	protected static abstract class ItemExpireTask implements Runnable {
		private final NavigableMap<Long, Set<Long>> itemExpires;
		private ScheduledFuture<?> nextItemExpire;
		private long scheduledRun;
		private Set<Long> uniqueIds;

		public ItemExpireTask() {
			itemExpires = new TreeMap<Long, Set<Long>>();
		}

		public synchronized void addExpire(long expiration, long uniqueId) {
			Set<Long> sameTimeExpires = itemExpires.get(Long.valueOf(expiration));
			if (sameTimeExpires == null) {
				sameTimeExpires = new HashSet<Long>();
				itemExpires.put(Long.valueOf(expiration), sameTimeExpires);
				if (itemExpires.firstKey().longValue() == expiration) {
					//just added item is at front of queue
					if (nextItemExpire == null) {
						//schedule it if nothing else is scheduled
						scheduleNext();
					} else if (expiration < scheduledRun) {
						//move it ahead of scheduled - note that run() is synchronized with this method
						nextItemExpire.cancel(false);
						itemExpires.put(Long.valueOf(scheduledRun), uniqueIds);
						scheduleNext();
					}
					//if expiration > scheduledRun, it'll be picked up later on by scheduleNext(), which is synchronized with this method
					//if expiration == scheduledRun, it'll be picked up by run(), which is synchronized with this method
				}
			}
			sameTimeExpires.add(Long.valueOf(uniqueId));
		}

		protected abstract void onExpire(long uniqueId);

		public synchronized void scheduleNext() {
			Map.Entry<Long, Set<Long>> entry = itemExpires.pollFirstEntry();
			if (entry != null) {
				scheduledRun = entry.getKey().longValue();
				uniqueIds = entry.getValue();
				nextItemExpire = Scheduler.getInstance().runAfterDelay(this, scheduledRun - System.currentTimeMillis());
			} else {
				nextItemExpire = null;
			}
		}

		@Override
		public synchronized void run() {
			for (Long oUniqueId : uniqueIds)
				onExpire(oUniqueId.longValue());
			scheduleNext();
		}

		public synchronized void cancel() {
			if (nextItemExpire != null) {
				nextItemExpire.cancel(false);
				nextItemExpire = null;
			}
		}
	}

	private final ReadWriteLock statLocks;
	private final ReadWriteLock questLocks;
	protected ItemExpireTask itemExpireTask;

	protected LoggedInPlayer() {
		statLocks = new ReentrantReadWriteLock();
		questLocks = new ReentrantReadWriteLock();
	}

	public abstract ReadableBuddyList getBuddyList();

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

	public void checkForExpiredItems() {
		itemExpireTask.scheduleNext();
	}

	public void onExpirableItemAdded(InventorySlot item) {
		if (item.getExpiration() != 0)
			itemExpireTask.addExpire(item.getExpiration(), item.getUniqueId());
	}

	public byte indexOfPet(long uniqueId) {
		Pet[] pets = getPets();
		for (byte i = 0; i < 3; i++)
			if (pets[i] != null && pets[i].getUniqueId() == uniqueId)
				return i;
		return -1;
	}

	public void removePet(byte slot) {
		Pet[] pets = getPets();
		for (int i = slot; i < 2; i++)
			pets[i] = pets[i + 1];
		pets[2] = null;
	}
}
