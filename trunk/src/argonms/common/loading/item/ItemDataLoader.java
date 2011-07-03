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

package argonms.common.loading.item;

import argonms.common.StatEffect;
import argonms.game.character.inventory.InventoryTools;
import argonms.common.loading.DataFileType;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

//FIXME: Thread safety for concurrent read/writes (if we're not preloading)
/**
 *
 * @author GoldenKevin
 */
public abstract class ItemDataLoader {
	private static ItemDataLoader instance;

	protected List<Integer> loaded;
	protected Map<Integer, Integer> wholePrice;
	protected Map<Integer, Short> slotMax;
	protected List<Integer> tradeBlocked;
	protected List<Integer> onlyOne;
	protected List<Integer> questItem;
	protected Map<Integer, short[]> bonusStats;
	protected Map<Integer, List<int[]>> summons;
	protected Map<Integer, Integer> success;
	protected Map<Integer, Integer> cursed;
	protected List<Integer> cash; //I don't think this is really needed...
	protected Map<Integer, List<byte[]>> operatingHours;
	protected List<Integer> useOnPickup;
	protected Map<Integer, List<Integer>> skills;
	protected Map<Integer, Double> unitPrice;
	protected Map<Integer, short[]> reqStats;
	protected Map<Integer, List<Integer>> scrollReqs;
	protected Map<Integer, ItemEffectsData> statEffects;
	protected Map<Integer, Integer> triggerItem;
	protected Map<Integer, Byte> tuc;
	protected Map<Integer, Integer> mesoValue;

	protected Map<Integer, Map<Byte, int[]>> petCommands;
	protected Map<Integer, Integer> petHunger;
	protected Map<Integer, List<int[]>> evolveChoices;

	protected Map<Integer, Byte> tamingMobIds;

	protected ItemDataLoader() {
		loaded = new ArrayList<Integer>();
		wholePrice = new HashMap<Integer, Integer>();
		slotMax = new HashMap<Integer, Short>();
		tradeBlocked = new ArrayList<Integer>();
		onlyOne = new ArrayList<Integer>();
		questItem = new ArrayList<Integer>();
		bonusStats = new HashMap<Integer, short[]>();
		summons = new HashMap<Integer, List<int[]>>();
		success = new HashMap<Integer, Integer>();
		cursed = new HashMap<Integer, Integer>();
		cash = new ArrayList<Integer>();
		operatingHours = new HashMap<Integer, List<byte[]>>();
		useOnPickup = new ArrayList<Integer>();
		skills = new HashMap<Integer, List<Integer>>();
		unitPrice = new HashMap<Integer, Double>();
		reqStats = new HashMap<Integer, short[]>();
		scrollReqs = new HashMap<Integer, List<Integer>>();
		statEffects = new HashMap<Integer, ItemEffectsData>();
		triggerItem = new HashMap<Integer, Integer>();
		tuc = new HashMap<Integer, Byte>();
		mesoValue = new HashMap<Integer, Integer>();

		petCommands = new HashMap<Integer, Map<Byte, int[]>>();
		petHunger = new HashMap<Integer, Integer>();
		evolveChoices = new HashMap<Integer, List<int[]>>();

		tamingMobIds = new HashMap<Integer, Byte>();
	}

	protected abstract void load(int itemid);

	public abstract boolean loadAll();

	public abstract boolean canLoad(int itemid);

	public int loadedItems() {
		return loaded.size();
	}

	public int getWholePrice(int itemId) {
		Integer oId = Integer.valueOf(itemId);
		if (!loaded.contains(oId))
			load(itemId);
		Integer ret = wholePrice.get(oId);
		return ret != null ? ret.intValue() : 0;
	}

	public double getUnitPrice(int itemId) {
		Integer oId = Integer.valueOf(itemId);
		if (!loaded.contains(oId))
			load(itemId);
		Double ret = unitPrice.get(oId);
		return ret != null ? ret.doubleValue() : 0;
	}

	public short getSlotMax(int itemId) {
		Integer oId = Integer.valueOf(itemId);
		if (!loaded.contains(oId))
			load(itemId);
		Short ret = slotMax.get(oId);
		return ret != null ? ret.shortValue() : (short)
				(InventoryTools.isEquip(itemId) ||
				InventoryTools.isPet(itemId) ? 1 : 100);
	}

	public boolean isTradeBlocked(int itemId) {
		Integer oId = Integer.valueOf(itemId);
		if (!loaded.contains(oId))
			load(itemId);
		return tradeBlocked.contains(oId);
	}

	public boolean isOnlyOne(int itemId) {
		Integer oId = Integer.valueOf(itemId);
		if (!loaded.contains(oId))
			load(itemId);
		return onlyOne.contains(oId);
	}

	public boolean isQuestItem(int itemId) {
		Integer oId = Integer.valueOf(itemId);
		if (!loaded.contains(oId))
			load(itemId);
		return questItem.contains(oId);
	}

	//TODO: is this the correct data we're using?
	public boolean canDrop(int itemId) {
		return !isTradeBlocked(itemId) && !isOnlyOne(itemId) && !isQuestItem(itemId);
	}

	public short getReqLevel(int itemId) {
		Integer oId = Integer.valueOf(itemId);
		if (!loaded.contains(oId))
			load(itemId);
		short[] ret = reqStats.get(oId);
		return ret != null ? ret[StatEffect.Level] : 0;
	}

	public short[] getBonusStats(int itemId) {
		Integer oId = Integer.valueOf(itemId);
		if (!loaded.contains(oId))
			load(itemId);
		short[] ret = bonusStats.get(oId);
		//don't trust the caller not to alter the array for the rest of us...
		return ret != null ? ret.clone() : null;
	}

	public boolean isCashEquip(int itemId) {
		Integer oId = Integer.valueOf(itemId);
		if (!loaded.contains(oId))
			load(itemId);
		return cash.contains(oId);
	}

	private boolean isHoliday(Calendar now) {
		return false;
	}

	public boolean isRateCardOperating(int itemId) {
		Integer oId = Integer.valueOf(itemId);
		if (!loaded.contains(oId))
			load(itemId);
		Calendar nowInLa = Calendar.getInstance(TimeZone.getTimeZone("America/Los_Angeles"));
		int today = nowInLa.get(Calendar.DAY_OF_WEEK);
		int thisHour = nowInLa.get(Calendar.HOUR_OF_DAY);
		for (byte[] t : operatingHours.get(oId))
			if ((t[0] == today || t[0] == 8 && isHoliday(nowInLa))
					&& thisHour >= t[1] && thisHour <= t[2])
				return true;
		return false;
	}

	public boolean isConsumeOnPickup(int itemId) {
		Integer oId = Integer.valueOf(itemId);
		if (!loaded.contains(oId))
			load(itemId);
		return useOnPickup.contains(oId);
	}

	public ItemEffectsData getEffect(int itemId) {
		Integer oId = Integer.valueOf(itemId);
		if (!loaded.contains(oId))
			load(itemId);
		return statEffects.get(oId);
	}

	public byte getUpgradeSlots(int itemId) {
		Integer oId = Integer.valueOf(itemId);
		if (!loaded.contains(oId))
			load(itemId);
		Byte ret = tuc.get(oId);
		return ret != null ? ret.byteValue() : 7;
	}

	public static void setInstance(DataFileType wzType, String wzPath) {
		if (instance == null) {
			switch (wzType) {
				case KVJ:
					instance = new KvjItemDataLoader(wzPath);
					break;
				case MCDB:
					instance = new McdbItemDataLoader();
					break;
			}
		}
	}

	public static ItemDataLoader getInstance() {
		return instance;
	}
}
