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

import argonms.loading.KvjEffects;
import argonms.loading.item.ItemDataLoader;
import argonms.loading.string.StringDataLoader;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author GoldenKevin
 */
public class InventoryTools {
	private static Map<Integer, Equip> equipCache;

	static {
		equipCache = new HashMap<Integer, Equip>();
	}

	/**
	 * Distributes the given amount of a particular kind of item in the given
	 * inventory. Each slot that is used will conform to the correct amount of
	 * maximum items per slot of the particular item and the minimum amount of
	 * slots will be used. Either slots that have the same item id will have
	 * their quantity increased or free slots will be populated when all other
	 * slots with the same item id are full.
	 * @param inv
	 * @param itemid
	 * @param quantity
	 * @return a list of slots that have been modified (either the items have
	 * been placed in or the existing item's quantity has changed). If the
	 * inventory cannot accommodate the given quantity of the given item, then
	 * as many items will be placed in the inventory until it is full.
	 */
	public static List<Short> addToInventory(Inventory inv, int itemid, short quantity) {
		List<Short> modifiedSlots = new ArrayList<Short>();

		if (quantity > 0) {
			String cat = getCategory(itemid);
			boolean equip = cat.equals("Equip");
			boolean pet = cat.equals("Pet");

			short slotMax = ItemDataLoader.getInstance().getSlotMax(itemid);
			if (!equip && !pet) {
				for (Short s : inv.getItemSlots(itemid)) {
					InventorySlot slot = inv.get(s.shortValue());
					if (slot.getQuantity() < slotMax) {
						int delta = Math.min(slotMax - slot.getQuantity(), quantity);
						quantity -= delta;
						slot.setQuantity((short) (slot.getQuantity() + delta));
						modifiedSlots.add(s);
					}
				}
			}

			if (quantity > 0) {
				int slotsNeeded = ((quantity - 1) / slotMax) + 1;
				List<Short> freeSlots = inv.getFreeSlots(slotsNeeded);
				InventorySlot item;
				if (equip) {
					item = getCleanEquip(itemid);
				} else if (pet) {
					item = new Pet(itemid);
					((Pet) item).setName(StringDataLoader.getInstance().getItemNameFromId(itemid));
				} else {
					item = new Item(itemid);
				}
				for (Short s : freeSlots) {
					short slotAmt = (short) Math.min(slotMax, quantity);
					quantity -= slotAmt;
					if (!equip && !pet)
						item.setQuantity(slotAmt);
					inv.put(s.shortValue(), item);
					modifiedSlots.add(s.shortValue());
					item = item.clone();
				}
			}
		}
		return modifiedSlots;
	}

	/**
	 * Equip a piece of a equipment from a player's equipment inventory.
	 * @param equips the equipment inventory
	 * @param equipped the equipped equipment inventory
	 * @param src the slot of the piece of equipment in the equipment inventory
	 * @param dest the slot of where to equip the piece of equipment
	 */
	public static void equip(Inventory equips, Inventory equipped, short src, short dest) {
		if (equipped.get(dest) != null) {
			InventorySlot temp = equipped.remove(dest);
			equipped.put(dest, equips.remove(src));
			equips.put(src, temp);
		} else {
			equipped.put(dest, equips.remove(src));
		}
	}

	/**
	 * Remove a piece of equipment from a player's equipped inventory and move
	 * it to their equipment inventory.
	 * 
	 * @param equipped the inventory to remove the piece of equipment from
	 * @param equips the inventory to move the piece of equipment to
	 * @param src the slot of the piece of equipment in the equipped inventory
	 * @param dest the slot of where to unequip the piece of equipment
	 * @return true if successful, false if there was not enough room.
	 */
	public static boolean unequip(Inventory equipped, Inventory equips, short src, short dest) {
		List<Short> freeSlots = equips.getFreeSlots(1);
		if (freeSlots.isEmpty())
			return false;
		equips.put(dest, equipped.remove(src));
		return true;
	}

	public static Equip getCleanEquip(int itemId) {
		Integer oId = Integer.valueOf(itemId);
		if (!equipCache.containsKey(oId)) {
			Equip e;
			if (isRing(itemId))
				e = new Ring(itemId);
			else
				e = new Equip(itemId);
			short[] statUps = ItemDataLoader.getInstance().getBonusStats(itemId);
			e.setStr(statUps[KvjEffects.STR]);
			e.setDex(statUps[KvjEffects.DEX]);
			e.setInt(statUps[KvjEffects.INT]);
			e.setLuk(statUps[KvjEffects.LUK]);
			e.setHp(statUps[KvjEffects.MHP]);
			e.setMp(statUps[KvjEffects.MMP]);
			e.setWatk(statUps[KvjEffects.PAD]);
			e.setMatk(statUps[KvjEffects.MAD]);
			e.setWdef(statUps[KvjEffects.PDD]);
			e.setMdef(statUps[KvjEffects.MDD]);
			e.setAcc(statUps[KvjEffects.ACC]);
			e.setAvoid(statUps[KvjEffects.EVA]);
			e.setSpeed(statUps[KvjEffects.Speed]);
			e.setJump(statUps[KvjEffects.Jump]);
			e.setUpgradeSlots(ItemDataLoader.getInstance().getUpgradeSlots(itemId));
			equipCache.put(oId, e);
		}
		return equipCache.get(oId).clone();
	}

	public static boolean isPet(int itemId) {
		return (itemId >= 5000000 && itemId <= 5000100);
	}

	public static boolean isRing(int itemId) {
		return (itemId >= 1112000 && itemId < 1112100 ||
				itemId >= 1112800 && itemId < 1112803);
	}

	public static String getCategory(int itemid) {
		switch (itemid / 1000000) {
			case 1:
				return "Equip";
			case 2:
				return "Consume";
			case 3:
				return "Install";
			case 4:
				return "Etc";
			case 5:
				if (itemid >= 5000000 && itemid <= 5000100)
					return "Pet";
				else
					return "Cash";
			default:
				return null;
		}
	}

	public static String getCharCat(int id) {
		switch (id / 10000) {
			case 2:
				return "Face";
			case 3:
				return "Hair";
			case 100:
				return "Cap";
			case 101:
			case 102:
			case 103:
			case 112:
				return "Accessory";
			case 104:
				return "Coat";
			case 105:
				return "Longcoat";
			case 106:
				return "Pants";
			case 107:
				return "Shoes";
			case 108:
				return "Glove";
			case 109:
				return "Shield";
			case 110:
				return "Cape";
			case 111:
				return "Ring";
			case 130:
			case 131:
			case 132:
			case 133:
			case 137:
			case 138:
			case 139:
			case 140:
			case 141:
			case 142:
			case 143:
			case 144:
			case 145:
			case 146:
			case 147:
			case 148:
			case 149:
			case 160:
			case 170:
				return "Weapon";
			case 180:
			case 181:
			case 182:
			case 183:
				return "PetEquip";
			case 190:
			case 191:
			case 193:
				return "TamingMob";
			default:
				return null;
		}
	}

	public static byte getDayByteFromString(String str) {
		if (str.equals("SUN")) {
			return Calendar.SUNDAY;
		} else if (str.equals("MON")) {
			return Calendar.MONDAY;
		} else if (str.equals("TUE")) {
			return Calendar.TUESDAY;
		} else if (str.equals("WED")) {
			return Calendar.WEDNESDAY;
		} else if (str.equals("THU")) {
			return Calendar.THURSDAY;
		} else if (str.equals("FRI")) {
			return Calendar.FRIDAY;
		} else if (str.equals("SAT")) {
			return Calendar.SATURDAY;
		} else if (str.equals("HOL")) {
			return 8;
		} else {
			return 0;
		}
	}
}
