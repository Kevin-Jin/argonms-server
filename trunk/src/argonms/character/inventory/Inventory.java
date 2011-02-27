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

package argonms.character.inventory;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 *
 * @author GoldenKevin
 */
public class Inventory {
	public enum InventoryType {
		EQUIPPED(-1),
		EQUIP(1),
		USE(2),
		SETUP(3),
		ETC(4),
		CASH(5),
		STORAGE(6),
		CASH_SHOP(7);

		byte value;

		InventoryType(int value) {
			this.value = (byte) value;
		}

		public byte value() {
			return value;
		}

		public static InventoryType get(byte type) {
			for (InventoryType it : values())
				if (it.value() == type)
					return it;
			return null;
		}
	}

	private short maxSlots;
	private Map<Short, InventorySlot> slots;

	public Inventory(short maxSlots) {
		this.maxSlots = maxSlots;
		this.slots = new TreeMap<Short, InventorySlot>();
	}

	public void put(short slot, InventorySlot item) {
		slots.put(Short.valueOf(slot), item);
	}

	public void put(Map<Short, InventorySlot> slots) {
		slots.putAll(slots);
	}

	public InventorySlot remove(short s) {
		return slots.remove(Short.valueOf(s));
	}

	public Map<Short, InventorySlot> getAll() {
		return Collections.unmodifiableMap(slots);
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
		for (Entry<Short, InventorySlot> entry : slots.entrySet())
			if (entry.getValue().getItemId() == itemid)
				positions.add(entry.getKey());
		return positions;
	}

	/**
	 * The returned List is guaranteed to be iterated in ascending order. If
	 * using an iterator with the returned List, the top slots of a player's
	 * inventory will be filled first.
	 * @param needed
	 * @return
	 */
	public List<Short> getFreeSlots(int needed) {
		//keep it in insertion order! (which should keep it sorted ascending)
		List<Short> empty = new LinkedList<Short>();
		for (short i = 1; i <= maxSlots && empty.size() < needed; i++) {
			Short slot = Short.valueOf(i);
			if (!slots.containsKey(slot))
				empty.add(slot);
		}
		return empty;
	}

	public short getMaxSlots() {
		return maxSlots;
	}

	public short freeSlots() {
		return (short) (maxSlots - slots.size());
	}
}
