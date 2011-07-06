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

package argonms.common.character.inventory;

import argonms.common.StatEffect;
import argonms.common.UniqueIdGenerator;
import argonms.common.character.Player;
import argonms.common.character.inventory.Equip.WeaponType;
import argonms.common.character.inventory.Inventory.InventoryType;
import argonms.common.loading.item.ItemDataLoader;
import argonms.common.loading.string.StringDataLoader;
import argonms.common.tools.Rng;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author GoldenKevin
 */
public class InventoryTools {
	public enum WeaponClass {
		ONE_HANDED_MELEE(1),
		SPEAR_POLEARM	(2),
		BOW				(3),
		CROSSBOW		(4),
		TWO_HANDED_MELEE(5),
		WAND_STAFF		(6),
		CLAW			(7),
		KNUCKLE			(8),
		GUN				(9);

		private static final Map<Byte, WeaponClass> lookup;

		//initialize reverse lookup
		static {
			lookup = new HashMap<Byte, WeaponClass>(values().length);
			for (WeaponClass type : values())
				lookup.put(Byte.valueOf(type.byteValue()), type);
		}

		private final byte value;

		private WeaponClass(int value) {
			this.value = (byte) value;
		}

		public byte byteValue() {
			return value;
		}

		public static WeaponClass valueOf(byte value) {
			return lookup.get(Byte.valueOf(value));
		}

		public static WeaponClass getForPlayer(Player p) {
			WeaponClass wClass;
			InventorySlot weapon = p.getInventory(InventoryType.EQUIPPED).get((short) -11);
			if (weapon == null)
				return null;
			int itemId = weapon.getDataId();
			if (itemId >= 1300000 && itemId < 1340000)
				wClass = WeaponClass.ONE_HANDED_MELEE;
			else if (itemId >= 1370000 && itemId < 1390000)
				wClass = WeaponClass.WAND_STAFF;
			else if (itemId >= 1400000 && itemId < 1430000)
				wClass = WeaponClass.TWO_HANDED_MELEE;
			else if (itemId >= 1430000 && itemId < 1450000)
				wClass = WeaponClass.SPEAR_POLEARM;
			else if (itemId >= 1450000 && itemId < 1460000)
				wClass = WeaponClass.BOW;
			else if (itemId >= 1460000 && itemId < 1470000)
				wClass = WeaponClass.CROSSBOW;
			else if (itemId >= 1470000 && itemId < 1480000)
				wClass = WeaponClass.CLAW;
			else if (itemId >= 1480000 && itemId < 1490000)
				wClass = WeaponClass.KNUCKLE;
			else if (itemId >= 1490000 && itemId < 1500000)
				wClass = WeaponClass.GUN;
			else
				wClass = null;
			return wClass;
		}
	}

	private static final Logger LOG = Logger.getLogger(InventoryTools.class.getName());

	private static Map<Integer, Equip> equipCache;

	static {
		equipCache = new HashMap<Integer, Equip>();
	}

	public static boolean canFitEntirely(Inventory inv, int itemid, short remQty, boolean breakRechargeableStack) {
		if (remQty > 0) {
			if (!isRechargeable(itemid) || breakRechargeableStack) {
				//TODO: getPersonalSlotMax, but this is in argonms.common. X.X
				short slotMax = ItemDataLoader.getInstance().getSlotMax(itemid);
				for (Short s : inv.getItemSlots(itemid)) {
					InventorySlot slot = inv.get(s.shortValue());
					if (slot.getQuantity() < slotMax)
						remQty -= (slotMax - slot.getQuantity());
				}
				if (remQty > 0) {
					int slotsNeeded = ((remQty - 1) / slotMax) + 1; //ceiling of (remQty / slotMax)
					if (inv.freeSlots() < slotsNeeded)
						return false;
				}
			} else {
				//rechargeables can go beyond even a personal slot max (e.g. if
				//the personal slot max of the giving player is higher than the
				//receiving player because of higher claw/gun mastery for).
				//the item will be stacked to the given quantity in one slot.
				return inv.freeSlots() > 0;
			}
		}
		return true;
	}

	public static InventorySlot makeItemWithId(int itemid) {
		InventorySlot item;
		if (isEquip(itemid)) {
			item = getCleanEquip(itemid);
		} else if (isPet(itemid)) {
			item = new Pet(itemid);
			((Pet) item).setName(StringDataLoader.getInstance().getItemNameFromId(itemid));
		} else {
			item = new Item(itemid);
		}
		if (isCashItem(itemid)) {
			try {
				item.setUniqueId(UniqueIdGenerator.incrementAndGet());
			} catch (Exception e) {
				LOG.log(Level.WARNING, "Failed to set new uid for cash item.", e);
			}
		}
		return item;
	}

	/**
	 * Distributes the given amount of the specified item to the given
	 * inventory. Each slot that is used will conform to the correct amount of
	 * maximum items per slot of the particular item and the minimum amount of
	 * slots will be used. Either slots that have the same item id will have
	 * their quantity increased or free slots will be populated when all other
	 * slots with the same item id are full. If the inventory cannot
	 * accommodate the given quantity of the given item, then as many items will
	 * be placed in the inventory until it is full. The quantity of the given
	 * InventorySlot will be ignored, and the object itself will be placed in
	 * the first slot added once all other slots with the same item id are full.
	 * 
	 * If breakRechargeableStack is false and the given item is a rechargeable,
	 * then the entire quantity will be placed into one slot. Useful for
	 * preserving a stack of stars/bullets when items are transferred from a
	 * player who has a higher personal slot max of a particular rechargeable
	 * than the receiving player (i.e. higher claw/gun mastery).
	 * @param inv
	 * @param item
	 * @param quantity
	 * @param breakRechargeableStack should be false only if the passed item
	 * came from another player, e.g. by means of trading or picking up a drop
	 * made by a player.
	 * @return
	 */
	public static UpdatedSlots addToInventory(Inventory inv, InventorySlot item, int quantity, boolean breakRechargeableStack) {
		List<Short> modifiedSlots = new ArrayList<Short>();
		List<Short> insertedSlots = new ArrayList<Short>();

		int itemid = item.getDataId();
		boolean rechargeable = isRechargeable(itemid);
		if (rechargeable && quantity < Short.MAX_VALUE && (!breakRechargeableStack || quantity == 0)) {
			//a fully exhausted rechargeable ammo item will have quantity 0, but
			//we can still add it to our inventory.
			item.setQuantity((short) quantity);
			List<Short> freeSlots = inv.getFreeSlots(1);
			for (Short s : freeSlots) {
				inv.put(s.shortValue(), item);
				insertedSlots.add(s);
			}
		} else if (quantity > 0) {
			boolean equip = isEquip(itemid);
			boolean pet = isPet(itemid);

			short slotMax = ItemDataLoader.getInstance().getSlotMax(itemid);
			if (!equip && !pet && !rechargeable) {
				for (Short s : inv.getItemSlots(itemid)) {
					InventorySlot slot = inv.get(s.shortValue());
					if (slot.getQuantity() < slotMax) {
						int delta = Math.min(slotMax - slot.getQuantity(), quantity);
						quantity -= delta;
						slot.setQuantity((short) (slot.getQuantity() + delta));
						modifiedSlots.add(s);
						if (quantity == 0)
							break;
					}
				}
			}

			if (quantity > 0) {
				boolean updateUid = isCashItem(itemid);
				int slotsNeeded = ((quantity - 1) / slotMax) + 1; //ceiling
				List<Short> freeSlots = inv.getFreeSlots(slotsNeeded);
				Iterator<Short> i = freeSlots.iterator();
				while (i.hasNext()) {
					short s = i.next().shortValue();
					short slotAmt = (short) Math.min(slotMax, quantity);
					quantity -= slotAmt;
					if (!equip && !pet)
						item.setQuantity(slotAmt);
					inv.put(s, item);
					insertedSlots.add(s);
					if (i.hasNext()) {
						item = item.clone();
						if (updateUid) {
							try {
								item.setUniqueId(UniqueIdGenerator.incrementAndGet());
							} catch (Exception e) {
								LOG.log(Level.WARNING, "Failed to set new uid for cash item.", e);
							}
						}
					}
				}
			}
		}
		return new UpdatedSlots(modifiedSlots, insertedSlots);
	}

	/**
	 * Distributes the given amount of a particular kind of item in the given
	 * inventory. Each slot that is used will conform to the correct amount of
	 * maximum items per slot of the particular item and the minimum amount of
	 * slots will be used. Either slots that have the same item id will have
	 * their quantity increased or free slots will be populated when all other
	 * slots with the same item id are full. If the inventory cannot
	 * accommodate the given quantity of the given item, then as many items will
	 * be placed in the inventory until it is full.
	 * @param inv
	 * @param itemid
	 * @param quantity
	 * @return a Pair, with the left being a list of pre-existing slots that
	 * had their quantities changed, and the right being a list of slots that
	 * were added.
	 */
	public static UpdatedSlots addToInventory(Inventory inv, int itemid, int quantity) {
		return addToInventory(inv, makeItemWithId(itemid), quantity, true);
	}

	/**
	 * Equip a piece of a equipment from a player's equipment inventory.
	 * @param equips the equipment inventory
	 * @param equipped the equipped equipment inventory
	 * @param src the slot of the piece of equipment in the equipment inventory
	 * @param dest the slot of where to equip the piece of equipment
	 * @return any inventory items from the equipped inventory that have been
	 * moved to the equips inventory, or null if there was not enough room in
	 * the equips inventory to unequip the addition inventory items (and so no
	 * changes could be made to the inventories). If no items were moved, the
	 * returned short array with have length 0. Otherwise, it'll have length 2,
	 * with index 0 holding the item's old position in the equipped inventory
	 * and index 1 holding the item's new position in the equips inventory.
	 */
	public static short[] equip(Inventory equips, Inventory equipped, short src, short dest) {
		Equip toEquip = (Equip) equips.get(src);
		InventorySlot existing = equipped.get(dest);
		short[] otherChange = new short[0];
		boolean removeOld = true;
		switch (dest) {
			case -5: { //top/overall slot
				if (isOverall(toEquip.getDataId())) {
					InventorySlot bottom = equipped.get((short) -6);
					if (bottom != null) {
						short slot;
						if (existing == null) { //no top/overall equipped before
							slot = src; //overwrite our overall with the pants
							removeOld = false;
						} else { //we have top and pants equipped already
							if (!canFitEntirely(equips, bottom.getDataId(), bottom.getQuantity(), true))
								return null;
							slot = equips.getFreeSlots(1).get(0);
						}
						unequip(equipped, equips, (short) -6, slot);
						otherChange = new short[] { -6, slot };
					}
				}
				break;
			} case -6: { //pants
				InventorySlot top = equipped.get((short) -5);
				if (top != null && isOverall(top.getDataId())) {
					short slot;
					if (existing == null) { //no pants equipped before
						slot = src; //overwrite our pants with the overall
						removeOld = false;
					} else { //we have overall and pants equipped already (impossible! O.O)
						if (!canFitEntirely(equips, top.getDataId(), top.getQuantity(), true))
							return null;
						slot = equips.getFreeSlots(1).get(0);
					}
					unequip(equipped, equips, (short) -5, slot);
					otherChange = new short[] { -5, slot };
				}
				break;
			} case -10: { //shield
				InventorySlot weapon = equipped.get((short) -11);
				if (weapon != null && isTwoHanded(weapon.getDataId())) {
					short slot;
					if (existing == null) { //no shield equipped before
						slot = src; //overwrite our shield with two handed weapon
						removeOld = false;
					} else { //we have shield and two handed weapon equipped already (impossible! O.O)
						if (!canFitEntirely(equips, weapon.getDataId(), weapon.getQuantity(), true))
							return null;
						slot = equips.getFreeSlots(1).get(0);
					}
					unequip(equipped, equips, (short) -11, slot);
					otherChange = new short[] { -11, slot };
				}
				break;
			} case -11: { //weapon
				if (isTwoHanded(toEquip.getDataId())) {
					InventorySlot shield = equipped.get((short) -10);
					if (shield != null) {
						short slot;
						if (existing == null) { //no weapon equipped before
							slot = src; //overwrite our weapon with the shield
							removeOld = false;
						} else { //we have weapon and shield equipped already
							if (!canFitEntirely(equips, shield.getDataId(), shield.getQuantity(), true))
								return null;
							slot = equips.getFreeSlots(1).get(0);
						}
						unequip(equipped, equips, (short) -10, slot);
						otherChange = new short[] { -10, slot };
					}
				}
				break;
			}
		}
		if (existing != null)
			equips.put(src, existing);
		else if (removeOld)
			equips.remove(src);
		equipped.put(dest, toEquip);
		return otherChange;
	}

	public static InventorySlot takeFromInventory(Inventory inv, short slot, short toRemove) {
		InventorySlot item = inv.get(slot);
		if (item.getQuantity() > toRemove) {
			item.setQuantity((short) (item.getQuantity() - toRemove));
			return item;
		} else {
			inv.remove(slot);
			return null;
		}
	}

	public static UpdatedSlots removeFromInventory(Inventory inv, int itemId, int quantity) {
		boolean rechargeable = isRechargeable(itemId);
		List<Short> changed = new ArrayList<Short>();
		List<Short> removed = new ArrayList<Short>();
		int delta;
		for (Short slot : inv.getItemSlots(itemId)) {
			InventorySlot item = inv.get(slot.shortValue());
			delta = Math.min(quantity, item.getQuantity());
			quantity -= delta;
			short newQty = (short) (item.getQuantity() - delta);
			if (newQty == 0 && !rechargeable) {
				inv.remove(slot);
				removed.add(slot);
			} else {
				item.setQuantity(newQty);
				changed.add(slot);
			}
		}
		return new UpdatedSlots(changed, removed);
	}

	public static UpdatedSlots removeFromInventory(Player p, int itemId, int quantity) {
		return removeFromInventory(p.getInventory(getCategory(itemId)), itemId, quantity);
	}

	/**
	 * Remove a piece of equipment from a player's equipped inventory and move
	 * it to their equipment inventory.
	 * 
	 * @param equipped the inventory to remove the piece of equipment from
	 * @param equips the inventory to move the piece of equipment to
	 * @param src the slot of the piece of equipment in the equipped inventory
	 * @param dest the slot of where to unequip the piece of equipment
	 */
	public static void unequip(Inventory equipped, Inventory equips, short src, short dest) {
		equips.put(dest, equipped.remove(src));
	}

	public static boolean unequip(Inventory equipped, Inventory equips, short src) {
		List<Short> freeSlots = equips.getFreeSlots(1);
		if (freeSlots.isEmpty())
			return false;
		unequip(equipped, equips, src, freeSlots.get(0));
		return true;
	}

	public static short getAmountOfItem(Inventory inv, int itemId) {
		short total = 0;
		for (Short slot : inv.getItemSlots(itemId))
			total += inv.get(slot.shortValue()).getQuantity();
		return total;
	}

	public static boolean hasItem(Player p, int itemId, int quantity) {
		InventoryType type = getCategory(itemId);
		if (quantity > 0) {
			return (p.getInventory(type).hasItem(itemId, quantity)
					|| (type == InventoryType.EQUIP
					&& p.getInventory(InventoryType.EQUIPPED).hasItem(itemId, quantity)));
		} else if (quantity == 0) {
			return (p.getInventory(type).hasItem(itemId, quantity)
					&& (type != InventoryType.EQUIP
					|| p.getInventory(InventoryType.EQUIPPED).hasItem(itemId, quantity)));
		} else {
			throw new IllegalArgumentException("Domain error. Quantity must be >= 0");
		}
	}

	public static Equip getCleanEquip(int itemId) {
		Integer oId = Integer.valueOf(itemId);
		if (!equipCache.containsKey(oId)) {
			Equip e;
			if (isRing(itemId))
				e = new Ring(itemId);
			else if (isMount(itemId))
				e = new TamingMob(itemId);
			else
				e = new Equip(itemId);
			short[] statUps = ItemDataLoader.getInstance().getBonusStats(itemId);
			if (statUps != null) {
				e.setStr(statUps[StatEffect.STR]);
				e.setDex(statUps[StatEffect.DEX]);
				e.setInt(statUps[StatEffect.INT]);
				e.setLuk(statUps[StatEffect.LUK]);
				e.setHp(statUps[StatEffect.MHP]);
				e.setMp(statUps[StatEffect.MMP]);
				e.setWatk(statUps[StatEffect.PAD]);
				e.setMatk(statUps[StatEffect.MAD]);
				e.setWdef(statUps[StatEffect.PDD]);
				e.setMdef(statUps[StatEffect.MDD]);
				e.setAcc(statUps[StatEffect.ACC]);
				e.setAvoid(statUps[StatEffect.EVA]);
				e.setSpeed(statUps[StatEffect.Speed]);
				e.setJump(statUps[StatEffect.Jump]);
			}
			e.setUpgradeSlots(ItemDataLoader.getInstance().getUpgradeSlots(itemId));
			equipCache.put(oId, e);
		}
		return equipCache.get(oId).clone();
	}

	private static short makeRandStat(short original, int maxDiff) {
		if (original == 0)
			return 0;

		//max difference allowed between clean stat and rand stat
		maxDiff = Math.min((int) Math.ceil(original * 0.1), maxDiff);
		//select a random number between (original - maxDiff) and (original + maxDiff) inclusive
		return (short) (Rng.getGenerator().nextInt((maxDiff * 2) + 1) + (original - maxDiff));
	}

	public static void randomizeStats(Equip e) {
		e.setStr(makeRandStat(e.getStr(), 5));
		e.setDex(makeRandStat(e.getDex(), 5));
		e.setInt(makeRandStat(e.getInt(), 5));
		e.setLuk(makeRandStat(e.getLuk(), 5));
		e.setMatk(makeRandStat(e.getMatk(), 5));
		e.setWatk(makeRandStat(e.getWatk(), 5));
		e.setAcc(makeRandStat(e.getAcc(), 5));
		e.setAvoid(makeRandStat(e.getAvoid(), 5));
		e.setJump(makeRandStat(e.getJump(), 5));
		e.setSpeed(makeRandStat(e.getSpeed(), 5));
		e.setWdef(makeRandStat(e.getWdef(), 10));
		e.setMdef(makeRandStat(e.getMdef(), 10));
		e.setHp(makeRandStat(e.getHp(), 10));
		e.setMp(makeRandStat(e.getMp(), 10));
	}

	public static WeaponType getWeaponType(int itemId) {
		int cat = (itemId / 10000) % 100;
		switch (cat) {
			case 30:
				return WeaponType.SWORD1H;
			case 31:
				return WeaponType.AXE1H;
			case 32:
				return WeaponType.BLUNT1H;
			case 33:
				return WeaponType.DAGGER;
			case 37:
				return WeaponType.WAND;
			case 38:
				return WeaponType.STAFF;
			case 40:
				return WeaponType.SWORD2H;
			case 41:
				return WeaponType.AXE2H;
			case 42:
				return WeaponType.BLUNT2H;
			case 43:
				return WeaponType.SPEAR;
			case 44:
				return WeaponType.POLE_ARM;
			case 45:
				return WeaponType.BOW;
			case 46:
				return WeaponType.CROSSBOW;
			case 47:
				return WeaponType.CLAW;
			case 48:
				return WeaponType.KNUCKLE;
			case 49:
				return WeaponType.GUN;

		}
		return WeaponType.NOT_A_WEAPON;
	}

	public static boolean isCashItem(int itemId) {
		if (isEquip(itemId))
			return ItemDataLoader.getInstance().isCashEquip(itemId);
		return (itemId >= 5000000 && itemId < 6000000);
	}

	public static boolean isEquip(int itemId) {
		return (itemId >= 1000000 && itemId < 2000000);
	}

	public static boolean isTwoHanded(int itemId) {
		switch (getWeaponType(itemId)) {
			case AXE2H:
				return true;
			case BLUNT2H:
				return true;
			case BOW:
				return true;
			case CLAW:
				return true;
			case CROSSBOW:
				return true;
			case POLE_ARM:
				return true;
			case SPEAR:
				return true;
			case SWORD2H:
				return true;
			case KNUCKLE:
				return true;
			case GUN:
				return true;
			default:
				return false;
		}
	}

	public static boolean isThrowingStar(int itemId) {
		return (itemId >= 2070000 && itemId < 2080000);
	}

	public static boolean isBullet(int itemId) {
		return (itemId >= 2330000 && itemId < 2331000);
	}

	public static boolean isRechargeable(int itemId) {
		int cat = itemId / 10000;
		return (cat == 233 || cat == 207);
	}

	public static boolean isOverall(int itemId) {
		return itemId >= 1050000 && itemId < 1060000;
	}

	public static boolean isPet(int itemId) {
		return (itemId >= 5000000 && itemId <= 5000100);
	}

	public static boolean isRing(int itemId) {
		return (itemId >= 1112000 && itemId < 1112100 ||
				itemId >= 1112800 && itemId < 1112803);
	}

	public static boolean isMount(int itemId) {
		return (itemId >= 1900000 && itemId < 1940000);
	}

	public static boolean isArrowForCrossBow(int itemId) {
		return itemId >= 2061000 && itemId < 2062000;
	}

	public static boolean isArrowForBow(int itemId) {
		return itemId >= 2060000 && itemId < 2061000;
	}

	public static InventoryType getCategory(int itemid) {
		switch (itemid / 1000000) {
			case 1:
				return InventoryType.EQUIP;
			case 2:
				return InventoryType.USE;
			case 3:
				return InventoryType.SETUP;
			case 4:
				return InventoryType.ETC;
			case 5:
				return InventoryType.CASH;
		}
		return null;
	}

	public static String getCategoryName(int itemid) {
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

	public static class UpdatedSlots {
		public final List<Short> modifiedSlots;
		public final List<Short> addedOrRemovedSlots;

		public UpdatedSlots(List<Short> modify, List<Short> addOrRemove) {
			modifiedSlots = modify;
			addedOrRemovedSlots = addOrRemove;
		}

		public void union(UpdatedSlots other) {
			modifiedSlots.addAll(other.modifiedSlots);
			addedOrRemovedSlots.addAll(other.addedOrRemovedSlots);
		}
	}
}
