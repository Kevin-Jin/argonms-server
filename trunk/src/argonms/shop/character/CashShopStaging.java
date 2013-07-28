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

package argonms.shop.character;

import argonms.common.character.inventory.Inventory;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 *
 * @author GoldenKevin
 */
public class CashShopStaging extends Inventory {
	public static class CashItemGift {
		private final long uniqueId;
		private final int itemId;
		private final String sender;
		private final String message;

		public CashItemGift(long uniqueId, int dataId, String sender, String message) {
			this.uniqueId = uniqueId;
			this.itemId = dataId;
			this.sender = sender;
			this.message = message;
		}

		public long getUniqueId() {
			return uniqueId;
		}

		public int getItemId() {
			return itemId;
		}

		public String getSender() {
			return sender;
		}

		public String getMessage() {
			return message;
		}
	}

	private final ReadWriteLock locks;
	private final Map<Long, Integer> accounts;
	private final Map<Long, Integer> serialNumbers;
	private final List<CashItemGift> gifts;

	public CashShopStaging() {
		super(Short.MAX_VALUE);
		//forgo to the overhead of ConcurrentHashMap. with no scripts and
		//commands, we are guaranteed to not do much concurrency in cash shop
		locks = new ReentrantReadWriteLock();
		accounts = new HashMap<Long, Integer>();
		serialNumbers = new HashMap<Long, Integer>();
		gifts = new ArrayList<CashItemGift>();
	}

	public void lockRead() {
		locks.readLock().lock();
	}

	public void unlockRead() {
		locks.readLock().unlock();
	}

	public void lockWrite() {
		locks.writeLock().lock();
	}

	public void unlockWrite() {
		locks.writeLock().unlock();
	}

	public int getAccount(long uniqueId, int def) {
		Integer account = accounts.get(Long.valueOf(uniqueId));
		if (account == null)
			return def;
		return account.intValue();
	}

	public int getSn(long uniqueId) {
		Integer sn = serialNumbers.get(Long.valueOf(uniqueId));
		if (sn == null)
			return 0;
		return sn.intValue();
	}

	public Collection<CashItemGift> getGiftedItems() {
		return gifts;
	}
}
