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

package argonms.common.character.inventory;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;

/**
 *
 * @author GoldenKevin
 */
public class Inventory implements IInventory {
	public enum InventoryType {
		EQUIPPED (-1),
		EQUIP (1),
		USE (2),
		SETUP (3),
		ETC (4),
		CASH (5),
		STORAGE (6),
		CASH_SHOP (7);

		private static final Map<Byte, InventoryType> lookup;

		//initialize reverse lookup
		static {
			lookup = new HashMap<Byte, InventoryType>(values().length);
			for (InventoryType type : values())
				lookup.put(Byte.valueOf(type.byteValue()), type);
		}

		private final byte value;

		private InventoryType(int value) {
			this.value = (byte) value;
		}

		public byte byteValue() {
			return value;
		}

		public static InventoryType valueOf(byte type) {
			return lookup.get(Byte.valueOf(type));
		}
	}

	private final AtomicInteger maxSlots;
	private final Map<Short, InventorySlot> slots;

	public Inventory(short maxSlots) {
		this.maxSlots = new AtomicInteger(maxSlots);
		this.slots = Collections.synchronizedMap(new TreeMap<Short, InventorySlot>());
	}

	@Override
	public void put(short slot, InventorySlot item) {
		slots.put(Short.valueOf(slot), item);
	}

	public void put(Map<Short, InventorySlot> slots) {
		slots.putAll(slots);
	}

	public InventorySlot remove(short s) {
		return slots.remove(Short.valueOf(s));
	}

	/**
	 * Must be synchronized around iterations.
	 * @return 
	 */
	@Override
	public Map<Short, InventorySlot> getAll() {
		return slots;
	}

	public Map<Short, Integer> getItemIds() {
		Map<Short, Integer> ids;
		synchronized(slots) {
			ids = new LinkedHashMap<Short, Integer>(slots.size());
			for (Entry<Short, InventorySlot> entry : slots.entrySet())
				ids.put(entry.getKey(), Integer.valueOf(entry.getValue().getDataId()));
		}
		return ids;
	}

	public InventorySlot get(short slot) {
		return slots.get(Short.valueOf(slot));
	}

	/**
	 * The returned Set is guaranteed to be iterated in ascending order. If
	 * using an iterator with the returned Set, the top slots of a player's
	 * inventory will be fetched first.
	 * @param itemid
	 * @return
	 */
	public Set<Short> getItemSlots(int itemid) {
		//keep them sorted in ascending order!
		Set<Short> positions = new TreeSet<Short>();
		synchronized(slots) {
			for (Entry<Short, InventorySlot> entry : slots.entrySet())
				if (entry.getValue().getDataId() == itemid)
					positions.add(entry.getKey());
		}
		return positions;
	}

	/**
	 * The returned List is guaranteed to be iterated in ascending order. If
	 * using an iterator with the returned List, the top slots of a player's
	 * inventory will be filled first.
	 * It is advised that if items are to be added to this inventory, the call
	 * to getFreeSlots and all the put methods are to be surrounded by
	 * synchronized(getAll())
	 * @param needed
	 * @return
	 */
	public List<Short> getFreeSlots(int needed) {
		//keep it in ascending order! (insertion order should work when
		//iterationg over SortedMaps)
		List<Short> empty = new LinkedList<Short>();
		for (short i = 1; i <= maxSlots.get() && empty.size() < needed; i++) {
			Short slot = Short.valueOf(i);
			if (!slots.containsKey(slot))
				empty.add(slot);
		}
		return empty;
	}

	public boolean hasItem(int itemid, int minQty) {
		if (minQty < 0)
			throw new IllegalArgumentException("Domain error. Quantity must be >= 0");
		int remaining = minQty;
		for (InventorySlot i : slots.values()) {
			if (i.getDataId() == itemid) {
				remaining -= i.getQuantity();
				if (remaining <= 0)
					//true if our purpose was to find it we have enough of the item.
					//false if our purpose was to find if we have none of the item.
					return minQty != 0;
			}
		}
		//false if our purpose was to find it we have enough of the item.
		//true if our purpose was to find if we have none of the item.
		return minQty == 0;
	}

	@Override
	public short getMaxSlots() {
		return (short) maxSlots.get();
	}

	public void increaseCapacity(short delta) {
		maxSlots.addAndGet(delta);
	}

	public short freeSlots() {
		return (short) (maxSlots.get() - slots.size());
	}
}
