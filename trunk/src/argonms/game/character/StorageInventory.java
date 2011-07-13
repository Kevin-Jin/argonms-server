/*
 * ArgonMS MapleStory server emulator written in Java
 * Copyright (C) 2011  GoldenKevin
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

import argonms.common.character.inventory.IInventory;
import argonms.common.character.inventory.Inventory.InventoryType;
import argonms.common.character.inventory.InventorySlot;
import argonms.common.character.inventory.InventoryTools;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 *
 * @author GoldenKevin
 */
public class StorageInventory implements IInventory {
	private short capacity;
	private InventorySlot[] startItems;
	private final Map<InventoryType, List<InventorySlot>> realItems;
	private final Set<InventoryType> alreadyTouched;
	private int mesos;
	private final Lock readLock;
	private final Lock writeLock;
	private short occupied;

	public StorageInventory(short capacity, int mesos) {
		this.capacity = capacity;
		this.startItems = new InventorySlot[4];
		this.realItems = new EnumMap<InventoryType, List<InventorySlot>>(InventoryType.class);
		this.alreadyTouched = EnumSet.noneOf(InventoryType.class);
		this.mesos = mesos;
		ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
		readLock = lock.readLock();
		writeLock = lock.writeLock();
	}

	private void ensureStartItemsCapacity(int minCapacity) {
		int oldCapacity = startItems.length;
		if (minCapacity > oldCapacity) {
			int newCapacity = Math.max((oldCapacity * 3) / 2 + 1, minCapacity);
			InventorySlot[] oldData = startItems;
			startItems = new InventorySlot[newCapacity];
			System.arraycopy(oldData, 0, startItems, 0, oldCapacity);
		}
	}

	public void put(short position, InventorySlot item) {
		writeLock.lock();
		try {
			ensureStartItemsCapacity(position + 1);
			startItems[position] = item;
			InventoryType type = InventoryTools.getCategory(item.getDataId());
			List<InventorySlot> inventorySpecificItems = realItems.get(type);
			if (inventorySpecificItems == null) {
				inventorySpecificItems = new ArrayList<InventorySlot>();
				realItems.put(type, inventorySpecificItems);
			}
			inventorySpecificItems.add(item);
			occupied++;
		} finally {
			writeLock.unlock();
		}
	}

	public void put(InventorySlot item) {
		InventoryType type = InventoryTools.getCategory(item.getDataId());
		writeLock.lock();
		try {
			List<InventorySlot> inventorySpecificItems = realItems.get(type);
			if (inventorySpecificItems == null) {
				inventorySpecificItems = new ArrayList<InventorySlot>();
				realItems.put(type, inventorySpecificItems);
			}
			inventorySpecificItems.add(item);
			occupied++;
		} finally {
			writeLock.unlock();
		}
	}

	public void remove(InventoryType inv, short position) {
		writeLock.lock();
		try {
			if (alreadyTouched.contains(inv)) {
				InventorySlot removed = realItems.get(inv).remove(position);
				int i;
				for (i = 0; i < startItems.length && !removed.equals(startItems[i]); i++);
				if (i < startItems.length)
					startItems[i] = null;
			} else {
				InventorySlot removed = startItems[position];
				startItems[position] = null;
				realItems.get(inv).remove(removed);
			}
			occupied--;
		} finally {
			writeLock.unlock();
		}
	}

	public InventorySlot get(InventoryType inv, short position) {
		readLock.lock();
		try {
			if (alreadyTouched.contains(inv))
				return realItems.get(inv).get(position);
			else
				return startItems[position];
		} finally {
			readLock.unlock();
		}
	}

	public Map<Short, InventorySlot> getAll() {
		InventorySlot item;
		readLock.lock();
		try {
			Map<Short, InventorySlot> mapRep = new LinkedHashMap<Short, InventorySlot>(occupied);
			for (short i = 0; i < startItems.length; i++)
				if ((item = startItems[i]) != null)
					mapRep.put(Short.valueOf(i), item);
			return mapRep;
		} finally {
			readLock.unlock();
		}
	}

	public InventorySlot[] getStartingItems() {
		readLock.lock();
		try{
			InventorySlot[] ret = new InventorySlot[occupied];
			System.arraycopy(startItems, 0, ret, 0, occupied);
			return ret;
		} finally {
			readLock.unlock();
		}
	}

	public List<InventorySlot> getRealItems(InventoryType inv) {
		readLock.lock();
		try {
			return realItems.get(inv);
		} finally {
			readLock.unlock();
		}
	}

	public short getMaxSlots() {
		readLock.lock();
		try {
			return capacity;
		} finally {
			readLock.unlock();
		}
	}

	public short freeSlots() {
		readLock.lock();
		try {
			return (short) (capacity - occupied);
		} finally {
			readLock.unlock();
		}
	}

	public int getMesos() {
		return mesos;
	}

	public void changeMesos(int delta) {
		this.mesos -= delta;
	}

	public int getBitfield(boolean init, Set<InventoryType> inventories, boolean updateMesos) {
		int field = 0;
		for (InventoryType inv : inventories)
			field |= 2 << inv.byteValue();
		if (updateMesos)
			field |= 2 << 0;
		writeLock.lock();
		try {
			if (!init && !updateMesos)
				alreadyTouched.addAll(inventories);
		} finally {
			writeLock.unlock();
		}
		return field;
	}

	public void collapse() {
		writeLock.lock();
		try {
			alreadyTouched.clear();
			List<InventorySlot> allItems = new ArrayList<InventorySlot>(occupied);
			for (Iterator<List<InventorySlot>> iter = realItems.values().iterator(); iter.hasNext();) {
				List<InventorySlot> inventorySpecificItems = iter.next();
				if (!inventorySpecificItems.isEmpty())
					allItems.addAll(inventorySpecificItems);
				else
					iter.remove();
			}
			startItems = allItems.toArray(new InventorySlot[occupied]);
		} finally {
			writeLock.unlock();
		}
	}
}
