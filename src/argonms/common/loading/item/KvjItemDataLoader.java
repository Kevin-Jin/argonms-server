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

import argonms.common.character.inventory.InventoryTools;
import argonms.common.loading.KvjEffects;
import argonms.common.util.input.LittleEndianByteArrayReader;
import argonms.common.util.input.LittleEndianReader;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author GoldenKevin
 */
public class KvjItemDataLoader extends ItemDataLoader {
	private static final Logger LOG = Logger.getLogger(KvjItemDataLoader.class.getName());

	private static final byte
		WHOLE_PRICE = 1,
		SLOT_MAX = 2,
		IS_TRADE_BLOCKED = 3,
		IS_ONE_ONLY = 4,
		IS_QUEST_ITEM = 5,
		BONUS_STAT = 6,
		SUMMON = 7,
		SUCCESS = 8,
		CURSED = 9,
		CASH = 10,
		OPERATING_HOURS = 11,
		SKILL = 12,
		UNIT_PRICE = 13,
		REQ_STAT = 14,
		UPGRADE_SLOTS = 15,
		SCROLL_REQUIREMENTS = 16,
		ITEM_EFFECT = 17,
		TRIGGER_ITEM = 18,
		MESO_VALUE = 19,

		PET_COMMAND = 20,
		PET_HUNGER = 21,
		PET_EVOLVE = 22,

		TAMING_MOB_ID = 23
	;

	private String dataPath;

	protected KvjItemDataLoader(String wzPath) {
		this.dataPath = wzPath;
	}

	@Override
	protected void load(int itemid) {
		File f = getFile(itemid);
		try {
			if (f.exists())
				doWork(itemid, new LittleEndianByteArrayReader(f));
		} catch (IOException e) {
			LOG.log(Level.WARNING, "Could not read KVJ data file for item " + itemid, e);
		}
		loaded.add(Integer.valueOf(itemid));
	}

	@Override
	public boolean loadAll() {
		try {
			File root = new File(dataPath + "Item.wz");
			for (String cat : root.list()) {
				File catFolder = new File(root.getAbsolutePath() + File.separatorChar + cat);
				if (cat.equals("Pet")) {
					for (String kvj : catFolder.list()) {
						int itemid = Integer.parseInt(kvj.substring(0, kvj.lastIndexOf(".img.kvj")));
						doWork(itemid, new LittleEndianByteArrayReader(new File(catFolder.getAbsolutePath() + File.separatorChar + kvj)));
						loaded.add(Integer.valueOf(itemid));
					}
				} else {
					for (String pref : catFolder.list()) {
						File prefFolder = new File(catFolder.getAbsolutePath() + File.separatorChar + pref);
						for (String kvj : prefFolder.list()) {
							int itemid = Integer.parseInt(kvj.substring(0, kvj.lastIndexOf(".kvj")));
							doWork(itemid, new LittleEndianByteArrayReader(new File(prefFolder.getAbsolutePath() + File.separatorChar + kvj)));
							loaded.add(Integer.valueOf(itemid));
						}
					}
				}
			}
			root = new File(dataPath + "Character.wz");
			for (String cat : root.list()) {
				File catFolder = new File(root.getAbsolutePath() + File.separatorChar + cat);
				if (!cat.equals("Afterimage") && !cat.equals("Face") && !cat.equals("Hair")) {
					for (String kvj : catFolder.list()) {
						int itemid = Integer.parseInt(kvj.substring(0, kvj.lastIndexOf(".img.kvj")));
						doWork(itemid, new LittleEndianByteArrayReader(new File(catFolder.getAbsolutePath() + File.separatorChar + kvj)));
						loaded.add(Integer.valueOf(itemid));
					}
				}
			}
			return true;
		} catch (IOException ex) {
			LOG.log(Level.WARNING, "Could not load all item data from KVJ files.", ex);
			return false;
		}
	}

	@Override
	public boolean canLoad(int itemid) {
		return loaded.contains(Integer.valueOf(itemid)) || getFile(itemid).exists();
	}

	private File getFile(int iid) {
		File f;
		String id = String.format("%08d", iid);
		String cat = InventoryTools.getCategoryName(iid);
		if (cat.equals("Pet"))
			f = new File(new StringBuilder(dataPath).append("Item.wz").append(File.separator).append(cat).append(File.separator).append(String.format("%07d", iid)).append(".img.kvj").toString());
		else if (cat.equals("Equip"))
			f = new File(new StringBuilder(dataPath).append("Character.wz").append(File.separator).append(InventoryTools.getCharCat(iid)).append(File.separator).append(id).append(".img.kvj").toString());
		else
			f = new File(new StringBuilder(dataPath).append("Item.wz").append(File.separator).append(cat).append(File.separator).append(id.substring(0, 4)).append(".img").append(File.separator).append(id).append(".kvj").toString());
		return f;
	}

	private void doWork(int itemid, LittleEndianReader reader) {
		Integer oId = Integer.valueOf(itemid);
		for (byte now = reader.readByte(); now != -1; now = reader.readByte()) {
			switch (now) {
				case WHOLE_PRICE:
					wholePrice.put(oId, Integer.valueOf(reader.readInt()));
					break;
				case SLOT_MAX:
					slotMax.put(oId, Short.valueOf(reader.readShort()));
					break;
				case IS_TRADE_BLOCKED:
					tradeBlocked.add(oId);
					break;
				case IS_ONE_ONLY:
					onlyOne.add(oId);
					break;
				case IS_QUEST_ITEM:
					questItem.add(oId);
					break;
				case BONUS_STAT:
					if (!bonusStats.containsKey(oId))
						bonusStats.put(oId, new short[16]);
					processBonusStat(reader, oId);
					break;
				case SUMMON:
					if (!summons.containsKey(oId))
						summons.put(oId, new ArrayList<int[]>());
					summons.get(oId).add(processSummon(reader));
					break;
				case SUCCESS:
					success.put(oId, Integer.valueOf(reader.readInt()));
					break;
				case CURSED:
					cursed.put(oId, Integer.valueOf(reader.readInt()));
					break;
				case CASH:
					cash.add(oId);
					break;
				case OPERATING_HOURS:
					if (!operatingHours.containsKey(oId))
						operatingHours.put(oId, new ArrayList<byte[]>());
					operatingHours.get(oId).add(processOperatingHours(reader));
					break;
				case SKILL:
					if (!skills.containsKey(oId))
						skills.put(oId, new ArrayList<Integer>());
					skills.get(oId).add(Integer.valueOf(reader.readInt()));
					break;
				case UNIT_PRICE:
					unitPrice.put(oId, Double.valueOf(reader.readDouble()));
					break;
				case REQ_STAT:
					if (!reqStats.containsKey(oId))
						reqStats.put(oId, new short[16]);
					processReqStat(reader, oId);
					break;
				case UPGRADE_SLOTS:
					tuc.put(oId, Byte.valueOf(reader.readByte()));
					break;
				case SCROLL_REQUIREMENTS:
					scrollReqs.put(oId, processScrollReqs(reader));
					break;
				case ITEM_EFFECT:
					statEffects.put(oId, processEffect(itemid, reader));
					break;
				case TRIGGER_ITEM:
					triggerItem.put(oId, Integer.valueOf(reader.readInt()));
					break;
				case MESO_VALUE:
					mesoValue.put(oId, Integer.valueOf(reader.readInt()));
					break;

				case PET_COMMAND:
					if (!petCommands.containsKey(oId))
						petCommands.put(oId, new HashMap<Byte, int[]>());
					processPetCmd(reader, oId);
					break;
				case PET_HUNGER:
					petHunger.put(oId, Integer.valueOf(reader.readInt()));
					break;
				case PET_EVOLVE:
					if (!evolveChoices.containsKey(oId))
						evolveChoices.put(oId, new ArrayList<int[]>());
					evolveChoices.get(oId).add(processPetEvolve(reader));
					break;

				case TAMING_MOB_ID:
					tamingMobIds.put(oId, Byte.valueOf(reader.readByte()));
					break;
			}
		}
	}

	private void processBonusStat(LittleEndianReader reader, Integer oId) {
		byte stat = reader.readByte();
		short value = reader.readShort();
		bonusStats.get(oId)[stat] = value;
	}

	private void processReqStat(LittleEndianReader reader, Integer oId) {
		byte stat = reader.readByte();
		short value = reader.readShort();
		reqStats.get(oId)[stat] = value;
	}

	private List<Integer> processScrollReqs(LittleEndianReader reader) {
		List<Integer> reqs = new ArrayList<Integer>();
		for (int i = reader.readInt(); i > 0; i--)
			reqs.add(Integer.valueOf(reader.readInt()));
		return reqs;
	}

	private int[] processSummon(LittleEndianReader reader) {
		int mobId = reader.readInt();
		int prob = reader.readInt();
		return new int[] { mobId, prob };
	}

	private byte[] processOperatingHours(LittleEndianReader reader) {
		byte day = reader.readByte();
		byte startHour = reader.readByte();
		byte endHour = reader.readByte();
		return new byte[] { day, startHour, endHour };
	}

	private void processPetCmd(LittleEndianReader reader, Integer oId) {
		byte commandId = reader.readByte();
		int prob = reader.readInt();
		int expInc = reader.readInt();
		petCommands.get(oId).put(Byte.valueOf(commandId), new int[] { prob, expInc });
	}

	private int[] processPetEvolve(LittleEndianReader reader) {
		int itemId = reader.readInt();
		int prob = reader.readInt();
		return new int[] { itemId, prob };
	}

	private ItemEffectsData processEffect(int itemid, LittleEndianReader reader) {
		ItemEffectsData effect = new ItemEffectsData(itemid);
		loop:
		for (byte now = reader.readByte(); now != -1; now = reader.readByte()) {
			switch (now) {
				case KvjEffects.DURATION:
					effect.setDuration(reader.readInt());
					break;
				case KvjEffects.WATK:
					effect.setWatk(reader.readShort());
					break;
				case KvjEffects.WDEF:
					effect.setWdef(reader.readShort());
					break;
				case KvjEffects.MATK:
					effect.setMatk(reader.readShort());
					break;
				case KvjEffects.MDEF:
					effect.setMdef(reader.readShort());
					break;
				case KvjEffects.ACC:
					effect.setAcc(reader.readShort());
					break;
				case KvjEffects.AVOID:
					effect.setAvoid(reader.readShort());
					break;
				case KvjEffects.HP:
					effect.setHpRecover(reader.readShort());
					break;
				case KvjEffects.MP:
					effect.setMpRecover(reader.readShort());
					break;
				case KvjEffects.SPEED:
					effect.setSpeed(reader.readShort());
					break;
				case KvjEffects.JUMP:
					effect.setJump(reader.readShort());
					break;
				case KvjEffects.MORPH:
					effect.setMorph(reader.readInt());
					break;
				case KvjEffects.HP_RECOVER_PERCENT:
					effect.setHpRecoverPercent(reader.readShort());
					break;
				case KvjEffects.MP_RECOVER_PERCENT:
					effect.setMpRecoverPercent(reader.readShort());
					break;
				case KvjEffects.MOVE_TO:
					effect.setMoveTo(reader.readInt());
					break;
				case KvjEffects.POISON:
					effect.setPoison();
					break;
				case KvjEffects.SEAL:
					effect.setSeal();
					break;
				case KvjEffects.DARKNESS:
					effect.setDarkness();
					break;
				case KvjEffects.WEAKNESS:
					effect.setWeakness();
					break;
				case KvjEffects.CURSE:
					effect.setCurse();
					break;
				case KvjEffects.CONSUME_ON_PICKUP:
					effect.setConsumeOnPickup();
					break;
				case KvjEffects.PET_CONSUMABLE_BY:
					effect.addPetConsumableBy(reader.readInt());
					break;
				case KvjEffects.END_EFFECT:
					break loop;
			}
		}
		return effect;
	}
}
