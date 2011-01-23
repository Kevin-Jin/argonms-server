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

package argonms.loading.item;

import argonms.loading.DataFileType;
import argonms.loading.KvjEffects;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

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
	protected Map<Integer, ItemEffect> statEffects;
	protected Map<Integer, Integer> triggerItem;
	protected Map<Integer, Byte> tuc;
	protected Map<Integer, Integer> mesoValue;

	protected Map<Integer, Map<Byte, int[]>> petCommands;
	protected Map<Integer, Integer> petHunger;
	protected Map<Integer, List<int[]>> evolveChoices;

	protected abstract void load(int itemid);

	public abstract boolean loadAll();

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
		statEffects = new HashMap<Integer, ItemEffect>();
		triggerItem = new HashMap<Integer, Integer>();
		tuc = new HashMap<Integer, Byte>();
		mesoValue = new HashMap<Integer, Integer>();

		petCommands = new HashMap<Integer, Map<Byte, int[]>>();
		petHunger = new HashMap<Integer, Integer>();
		evolveChoices = new HashMap<Integer, List<int[]>>();
	}

	public int getWholePrice(int itemId) {
		Integer oId = Integer.valueOf(itemId);
		if (!loaded.contains(oId))
			load(itemId);
		return wholePrice.containsKey(oId) ? wholePrice.get(oId).intValue() : 0;
	}

	public short getSlotMax(int itemId) {
		Integer oId = Integer.valueOf(itemId);
		if (!loaded.contains(oId))
			load(itemId);
		if (!slotMax.containsKey(oId))
			slotMax.put(oId, Short.valueOf((short) (!getCategory(itemId).equals("Equip") ? 100 : 1)));
		return slotMax.get(oId).shortValue();
	}

	public boolean isTradeBlocked(int itemId) {
		Integer oId = Integer.valueOf(itemId);
		if (!loaded.contains(oId))
			load(itemId);
		return tradeBlocked.contains(oId);
	}

	public short getReqLevel(int itemId) {
		Integer oId = Integer.valueOf(itemId);
		if (!loaded.contains(oId))
			load(itemId);
		return reqStats.containsKey(oId) ? reqStats.get(oId)[KvjEffects.Level] : 0;
	}

	public boolean isRateCardOperating(int itemId) {
		Integer oId = Integer.valueOf(itemId);
		if (!loaded.contains(oId))
			load(itemId);
		Calendar nowInLa = Calendar.getInstance(TimeZone.getTimeZone("America/Los_Angeles"));
		int today = nowInLa.get(Calendar.DAY_OF_WEEK);
		int thisHour = nowInLa.get(Calendar.HOUR_OF_DAY);
		for (byte[] t : operatingHours.get(oId))
			if (t[0] == 8) {
				if (isHoliday(nowInLa) && thisHour >= t[1] && thisHour <= t[2])
					return true;
			} else if (t[0] == today && thisHour >= t[1] && thisHour <= t[2])
				return true;
		return false;
	}

	private boolean isHoliday(Calendar now) {
		return false;
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

	public static ItemDataLoader setInstance(DataFileType wzType, String wzPath) {
		switch (wzType) {
			case KVJ:
				instance = new KvjItemDataLoader(wzPath);
				break;
			case MCDB:
				instance = new McdbItemDataLoader();
				break;
		}
		return instance;
	}

	public static ItemDataLoader getInstance() {
		return instance;
	}
}
