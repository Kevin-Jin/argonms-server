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

package argonms.net.external;

import argonms.GlobalConstants;
import argonms.character.ClientUpdateKey;
import argonms.character.Player;
import argonms.character.QuestEntry;
import argonms.character.skill.SkillEntry;
import argonms.character.skill.SkillTools;
import argonms.character.skill.Cooldown;
import argonms.character.skill.PlayerStatusEffectValues.PlayerStatusEffect;
import argonms.character.inventory.Equip;
import argonms.character.inventory.Inventory;
import argonms.character.inventory.Inventory.InventoryType;
import argonms.character.inventory.InventorySlot;
import argonms.character.inventory.InventorySlot.ItemType;
import argonms.character.inventory.InventoryTools;
import argonms.character.inventory.Pet;
import argonms.character.inventory.Ring;
import argonms.character.StatusEffectTools;
import argonms.character.skill.PlayerStatusEffectValues;
import argonms.map.MobSkills;
import argonms.map.MonsterStatusEffectValues.MonsterStatusEffect;
import argonms.map.movement.LifeMovementFragment;
import argonms.map.entity.ItemDrop;
import argonms.map.entity.Mist;
import argonms.map.entity.Mob;
import argonms.map.entity.MysticDoor;
import argonms.map.entity.Npc;
import argonms.map.entity.PlayerNpc;
import argonms.map.entity.Reactor;
import argonms.tools.Rng;
import argonms.tools.TimeUtil;
import argonms.tools.output.LittleEndianByteArrayWriter;
import argonms.tools.output.LittleEndianWriter;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

/**
 *
 * @author GoldenKevin
 */
public class CommonPackets {
	private static final int[] ROCK_MAPS = { //there has to be exactly 5!
		GlobalConstants.NULL_MAP, GlobalConstants.NULL_MAP,
		GlobalConstants.NULL_MAP, GlobalConstants.NULL_MAP,
		GlobalConstants.NULL_MAP
	};

	private static final int[] VIP_MAPS = { //there has to be exactly 15!
		GlobalConstants.NULL_MAP, GlobalConstants.NULL_MAP,
		GlobalConstants.NULL_MAP, GlobalConstants.NULL_MAP,
		GlobalConstants.NULL_MAP, GlobalConstants.NULL_MAP,
		GlobalConstants.NULL_MAP, GlobalConstants.NULL_MAP,
		GlobalConstants.NULL_MAP, GlobalConstants.NULL_MAP,
		GlobalConstants.NULL_MAP, GlobalConstants.NULL_MAP,
		GlobalConstants.NULL_MAP, GlobalConstants.NULL_MAP,
		GlobalConstants.NULL_MAP
	};

	private static final Map<ClientUpdateKey, Number> EMPTY_STATUPDATE = Collections.emptyMap();

	/**
	 * Append item expiration time info to an existing LittleEndianWriter.
	 *
	 * @param lew The LittleEndianWriter to write to.
	 * @param time The expiration time.
	 * @param show Show the expiration time.
	 */
	private static void writeItemExpire(LittleEndianWriter lew, long time, boolean show) {
		if (!show || time <= 0)
			time = TimeUtil.NO_EXPIRATION;
		lew.writeLong(TimeUtil.unixToWindowsTime(time));
	}

	public static void writeCharStats(LittleEndianWriter lew, Player p) {
		lew.writeInt(p.getId()); // character id
		lew.writePaddedAsciiString(p.getName(), 13);
		lew.writeByte(p.getGender()); // gender (0 = male, 1 = female)
		lew.writeByte(p.getSkinColor()); // skin color
		lew.writeInt(p.getEyes()); // face
		lew.writeInt(p.getHair()); // hair
		Pet[] pets = p.getPets();
		for (int i = 0; i < 3; i++)
			lew.writeLong(pets[i] == null ? 0 : pets[i].getUniqueId());
		lew.writeByte((byte) p.getLevel()); // level
		lew.writeShort(p.getJob()); // job
		lew.writeShort(p.getStr()); // str
		lew.writeShort(p.getDex()); // dex
		lew.writeShort(p.getInt()); // int
		lew.writeShort(p.getLuk()); // luk
		lew.writeShort(p.getHp()); // hp (?)
		lew.writeShort(p.getMaxHp()); // maxhp
		lew.writeShort(p.getMp()); // mp (?)
		lew.writeShort(p.getMaxMp()); // maxmp
		lew.writeShort(p.getAp()); // remaining ap
		lew.writeShort(p.getSp()); // remaining sp
		lew.writeInt(p.getExp()); // current exp
		lew.writeShort(p.getFame()); // fame
		lew.writeInt(p.getSpouseId());
		lew.writeInt(p.getMapId()); // current map id
		lew.writeByte(p.getSpawnPoint()); // spawnpoint
		lew.writeInt(0);
	}

	private static void writeAvatar(LittleEndianWriter lew, byte gender, byte skin,
			int eyes, boolean messenger, int hair, Map<Short, Integer> equips, Pet[] pets) {
		lew.writeByte(gender);
		lew.writeByte(skin);
		lew.writeInt(eyes);
		lew.writeBool(!messenger);
		lew.writeInt(hair); // hair

		Map<Byte, Integer> visEquip = new LinkedHashMap<Byte, Integer>();
		Map<Byte, Integer> maskedEquip = new LinkedHashMap<Byte, Integer>();
		for (Entry<Short, Integer> ent : equips.entrySet()) {
			byte pos = (byte) (ent.getKey().shortValue() * -1);
			Byte oPos = Byte.valueOf(pos);
			if (pos < 100 && visEquip.get(oPos) == null) {
				visEquip.put(oPos, ent.getValue());
			} else if (pos > 100 && pos != 111) {
				pos -= 100;
				if (visEquip.get(oPos) != null)
					maskedEquip.put(pos, visEquip.get(oPos));
				visEquip.put(oPos, ent.getValue());
			} else if (visEquip.get(oPos) != null) {
				maskedEquip.put(oPos, ent.getValue());
			}
		}

		for (Entry<Byte, Integer> entry : visEquip.entrySet()) {
			lew.writeByte(entry.getKey().byteValue());
			lew.writeInt(entry.getValue().intValue());
		}
		lew.writeByte((byte) 0xFF);

		for (Entry<Byte, Integer> entry : maskedEquip.entrySet()) {
			lew.writeByte(entry.getKey().byteValue());
			lew.writeInt(entry.getValue().intValue());
		}
		lew.writeByte((byte) 0xFF);

		Integer cWeapon = equips.get(Short.valueOf((short) 111));
		lew.writeInt(cWeapon != null ? cWeapon.intValue() : 0);

		for (int i = 0; i < 3; i++)
			lew.writeInt(pets[i] == null ? 0 : pets[i].getDataId());
	}

	public static void writeAvatar(LittleEndianWriter lew, Player p, boolean messenger) {
		writeAvatar(lew, p.getGender(), p.getSkinColor(), p.getEyes(), messenger,
				p.getHair(), p.getInventory(InventoryType.EQUIPPED).getItemIds(), p.getPets());
	}

	private static void writeItemInfo(LittleEndianWriter lew, short pos,
			InventorySlot item) {
		writeItemInfo(lew, pos, item, true, false, false);
	}

	//it seems as though Vana's PlayerPacketHelper::addItemInfo is much simpler
	private static void writeItemInfo(LittleEndianWriter lew, short pos,
			InventorySlot item, boolean showExpire, boolean leaveOut,
			boolean shopTransfer) {

		boolean cashItem = item.getUniqueId() > 0;
		boolean masking = false;
		if (pos < (byte) 0) {
			pos *= -1;
			if (cashItem && pos > 100) {
				lew.writeBool(false);
				lew.writeByte((byte) (pos - 100));
				masking = true;
			} else {
				lew.writeByte((byte) pos);
			}
		} else if (!leaveOut) {
			lew.writeByte((byte) pos);
		}
		lew.writeByte(item.getTypeByte());
		lew.writeInt(item.getDataId());
		lew.writeBool(cashItem);
		if (cashItem)
			lew.writeLong(item.getUniqueId());
		writeItemExpire(lew, item.getExpiration(), showExpire);

		if (item.getType() == ItemType.PET) {
			Pet pet = (Pet) item;
			lew.writePaddedAsciiString(pet.getName(), 13);
			lew.writeByte(pet.getLevel());
			lew.writeShort(pet.getCloseness());
			lew.writeByte(pet.getFullness());
			//00 B8 D5 60 00 CE C8 01
			writeItemExpire(lew, item.getExpiration(), showExpire); //again?
			lew.writeLengthPrefixedString(item.getOwner());
			lew.writeShort(item.getFlag());
		} else if (item.getType() == ItemType.EQUIP || item.getType() == ItemType.RING) {
			Equip equip = (Equip) item;
			lew.writeByte(equip.getUpgradeSlots());
			lew.writeByte(equip.getLevel());
			lew.writeShort(equip.getStr());
			lew.writeShort(equip.getDex());
			lew.writeShort(equip.getInt());
			lew.writeShort(equip.getLuk());
			lew.writeShort(equip.getHp());
			lew.writeShort(equip.getMp());
			lew.writeShort(equip.getWatk());
			lew.writeShort(equip.getMatk());
			lew.writeShort(equip.getWdef());
			lew.writeShort(equip.getMdef());
			lew.writeShort(equip.getAcc());
			lew.writeShort(equip.getAvoid());
			lew.writeShort(equip.getHands());
			lew.writeShort(equip.getSpeed());
			lew.writeShort(equip.getJump());
			lew.writeLengthPrefixedString(item.getOwner());

			if (item.getType() == ItemType.RING) {
				lew.writeByte(item.getFlag());
			} else {
				if (!shopTransfer) {
					lew.writeShort(equip.getFlag());
					if (!masking)
						lew.writeLong(0);
				} else {
					lew.writeBytes(new byte[] { 0x40, (byte) 0xE0, (byte) 0xFD, 0x3B, 0x37, 0x4F, 0x01 });
					lew.writeInt(-1);
				}
			}
		} else {
			lew.writeShort(item.getQuantity());
			lew.writeLengthPrefixedString(item.getOwner());
			lew.writeShort(item.getFlag());
			if (InventoryTools.isThrowingStar(item.getDataId())
					|| InventoryTools.isBullet(item.getDataId())) {
				//Might be rechargeable ID for internal tracking/duping tracking
				lew.writeLong(0);
			}
		}
	}

	public static void writeCharData(LittleEndianWriter lew, Player p) {
		lew.writeLong(-1);
		writeCharStats(lew, p);
		lew.writeByte((byte) p.getBuddyList().getCapacity());

		lew.writeInt(p.getMesos()); // mesos
		lew.writeByte((byte) p.getInventory(InventoryType.EQUIP).getMaxSlots()); // equip slots
		lew.writeByte((byte) p.getInventory(InventoryType.USE).getMaxSlots()); // use slots
		lew.writeByte((byte) p.getInventory(InventoryType.SETUP).getMaxSlots()); // set-up slots
		lew.writeByte((byte) p.getInventory(InventoryType.ETC).getMaxSlots()); // etc slots
		lew.writeByte((byte) p.getInventory(InventoryType.CASH).getMaxSlots()); // cash slots

		Inventory iv = p.getInventory(InventoryType.EQUIPPED);
		Map<Short, Ring> rings = new TreeMap<Short, Ring>();
		synchronized (iv) {
			for (Entry<Short, InventorySlot> entry : iv.getAll().entrySet()) {
				InventorySlot item = entry.getValue();
				writeItemInfo(lew, entry.getKey().shortValue(), item);
				if (item.getType() == ItemType.RING)
					rings.put(entry.getKey(), (Ring) item);
			}
		}
		lew.writeByte((byte) 0); //end of visible equipped
		lew.writeByte((byte) 0); //end of masked equipped

		iv = p.getInventory(InventoryType.EQUIP);
		synchronized (iv) {
			for (Entry<Short, InventorySlot> entry : iv.getAll().entrySet())
				writeItemInfo(lew, entry.getKey().shortValue(), entry.getValue());
		}
		lew.writeByte((byte) 0); //end of equip inventory

		iv = p.getInventory(InventoryType.USE);
		synchronized (iv) {
			for (Entry<Short, InventorySlot> entry : iv.getAll().entrySet())
				writeItemInfo(lew, entry.getKey().shortValue(), entry.getValue());
		}
		lew.writeByte((byte) 0); //end of consume inventory

		iv = p.getInventory(InventoryType.SETUP);
		synchronized (iv) {
			for (Entry<Short, InventorySlot> entry : iv.getAll().entrySet())
				writeItemInfo(lew, entry.getKey().shortValue(), entry.getValue());
		}
		lew.writeByte((byte) 0); //end of install inventory

		iv = p.getInventory(InventoryType.ETC);
		synchronized (iv) {
			for (Entry<Short, InventorySlot> entry : iv.getAll().entrySet())
				writeItemInfo(lew, entry.getKey().shortValue(), entry.getValue());
		}
		lew.writeByte((byte) 0); //end of etc inventory

		iv = p.getInventory(InventoryType.CASH);
		synchronized (iv) {
			for (Entry<Short, InventorySlot> entry : iv.getAll().entrySet())
				writeItemInfo(lew, entry.getKey().shortValue(), entry.getValue());
		}
		lew.writeByte((byte) 0); //end of cash inventory

		Map<Integer, SkillEntry> skills = p.getSkillEntries();
		lew.writeShort((short) skills.size());
		for (Entry<Integer, SkillEntry> entry : skills.entrySet()) {
			int skillid = entry.getKey().intValue();
			SkillEntry skill = entry.getValue();
			lew.writeInt(skillid);
			lew.writeInt(skill.getLevel());
			if (SkillTools.isFourthJob(skillid))
				lew.writeInt(skill.getMasterLevel());
		}
		Map<Integer, Cooldown> cooldowns = p.getCooldowns();
		lew.writeShort((short) cooldowns.size());
		for (Entry<Integer, Cooldown> cooling : cooldowns.entrySet()) {
			lew.writeInt(cooling.getKey().intValue());
			lew.writeShort(cooling.getValue().getSecondsRemaining());
		}

		Map<Short, QuestEntry> quests = p.getAllQuests();
		Map<Short, QuestEntry> started = new HashMap<Short, QuestEntry>();
		Map<Short, QuestEntry> completed = new HashMap<Short, QuestEntry>();
		for (Entry<Short, QuestEntry> entry : quests.entrySet()) {
			QuestEntry status = entry.getValue();
			switch (status.getState()) {
				case QuestEntry.STATE_NOT_STARTED:
					break;
				case QuestEntry.STATE_STARTED:
					started.put(entry.getKey(), status);
					break;
				case QuestEntry.STATE_COMPLETED:
					completed.put(entry.getKey(), status);
					break;
			}
		}
		lew.writeShort((short) started.size());
		for (Entry<Short, QuestEntry> startedQuest : started.entrySet()) {
			lew.writeShort(startedQuest.getKey().shortValue());
			lew.writeLengthPrefixedString(startedQuest.getValue().getData());
		}
		lew.writeShort((short) completed.size());
		for (Entry<Short, QuestEntry> completedQuest : completed.entrySet()) {
			lew.writeShort(completedQuest.getKey().shortValue());
			lew.writeLong(TimeUtil.unixToWindowsTime(completedQuest.getValue().getCompletionTime()));
		}

		//dude, what the fuck was that guy who wrote this smoking?
		boolean FR_last = false;
		if (!rings.isEmpty())
			lew.writeShort((short) 0);
		for (Ring ring : rings.values()) {
			lew.writeShort((short) 0);
			lew.writeShort((short) 1);
			lew.writeInt(ring.getPartnerCharId());
			lew.writePaddedAsciiString(Player.getNameFromId(ring.getPartnerCharId()), 13);
			lew.writeLong(ring.getUniqueId());
			lew.writeInt((int) ring.getPartnerRingId()); //this is definitely wrong, considering UIDs are 64-bit long
			if (ring.getDataId() >= 1112800 && ring.getDataId() <= 1112803 || ring.getDataId() <= 1112806 || ring.getDataId() <= 1112807 || ring.getDataId() <= 1112809) {
				FR_last = true;
				lew.writeInt(0);
				lew.writeInt(ring.getDataId());
				lew.writeShort((short) 0);
			} else {
				if (rings.size() > 1)
					lew.writeShort((short) 0);
				FR_last = false;
			}
		}
		//if (!FR_last)
			lew.writeLong(0);

		for (int i = 0; i < 5; i++)
			lew.writeInt(ROCK_MAPS[i]);
		for (int i = 0; i < 10; i++)
			lew.writeInt(VIP_MAPS[i]);
		lew.writeInt(0);
	}

	/**
	 * 
	 * @param stats THIS MAP MUST BE SORTED! Preferably pass an EnumMap.
	 * @param itemReaction
	 * @return
	 */
	public static byte[] writeUpdatePlayerStats(Map<ClientUpdateKey, ? extends Number> stats, boolean is) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter();
		lew.writeShort(ClientSendOps.PLAYER_STAT_UPDATE);
		lew.writeBool(is);
		int updateMask = 0;
		for (ClientUpdateKey key : stats.keySet())
			updateMask |= key.intValue();
		lew.writeInt(updateMask);
		for (Entry<ClientUpdateKey, ? extends Number> statupdate : stats.entrySet()) {
			switch (statupdate.getKey()) {
				case LEVEL: //unsigned
					lew.writeByte((byte) statupdate.getValue().shortValue());
					break;
				case JOB:
				case STR:
				case DEX:
				case INT:
				case LUK:
				case HP:
				case MAXHP:
				case MP:
				case MAXMP:
				case AVAILABLEAP:
				case AVAILABLESP:
					lew.writeShort(statupdate.getValue().shortValue());
					break;
				case FACE:
				case HAIR:
				case EXP:
				case FAME:
				case MESO:
				case PET:
					lew.writeInt(statupdate.getValue().intValue());
					break;
			}
		}
		return lew.getBytes();
	}

	public static byte[] writeEnableActions() {
		return writeUpdatePlayerStats(EMPTY_STATUPDATE, true);
	}

	public static byte[] writeUseSkill(Player p, Map<PlayerStatusEffect, Short> stats, int skillId, int duration) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter();

		lew.writeShort(ClientSendOps.FIRST_PERSON_APPLY_STATUS_EFFECT);
		long updateMask = 0;
		for (PlayerStatusEffect key : stats.keySet())
			updateMask |= key.longValue();
		lew.writeLong(0);
		lew.writeLong(updateMask);
		for (Short statupdate : stats.values()) {
			lew.writeShort(statupdate.shortValue());
			lew.writeInt(skillId);
			lew.writeInt(duration);
		}
		lew.writeShort((short) 0);
		lew.writeShort((short) 0); //additional info
		lew.writeByte((byte) 0); //# of times skill was cast

		return lew.getBytes();
	}

	public static byte[] writeGiveDebuff(Player p, Map<PlayerStatusEffect, Short> stats, short skillId, short skillLevel, int duration, short delay) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter();

		lew.writeShort(ClientSendOps.FIRST_PERSON_APPLY_STATUS_EFFECT);
		long updateMask = 0;
		for (PlayerStatusEffect key : stats.keySet())
			updateMask |= key.longValue();
		lew.writeLong(0);
		lew.writeLong(updateMask);
		for (Short statupdate : stats.values()) {
			lew.writeShort(statupdate.shortValue());
			lew.writeShort(skillId);
			lew.writeShort(skillLevel);
			lew.writeInt(duration);
		}
		lew.writeShort((short) 0);
		lew.writeShort(delay);
		lew.writeByte((byte) 1); //# of times skill was cast

		return lew.getBytes();
	}

	public static byte[] writeUseItem(Player p, Map<PlayerStatusEffect, Short> stats, int itemId, int duration) {
		return writeUseSkill(p, stats, -itemId, duration);
	}

	public static byte[] writeCancelStatusEffect(Set<PlayerStatusEffect> stats) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(19);

		lew.writeShort(ClientSendOps.FIRST_PERSON_CANCEL_STATUS_EFFECT);
		long updateMask = 0;
		for (PlayerStatusEffect key : stats)
			updateMask |= key.longValue();
		lew.writeLong(0);
		lew.writeLong(updateMask);
		lew.writeByte((byte) 0);

		return lew.getBytes();
	}

	public static byte[] writeUpdateSkillLevel(int skillid, byte level, byte masterlevel) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(18);

		lew.writeShort(ClientSendOps.SKILL_ENTRY_UPDATE);
		lew.writeBool(true);
		lew.writeShort((short) 1);
		lew.writeInt(skillid);
		lew.writeInt(level);
		lew.writeInt(masterlevel);
		lew.writeBool(true);

		return lew.getBytes();
	}

	public static byte[] writeUpdateAvatar(Player p) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter();
		lew.writeShort(ClientSendOps.UPDATE_AVATAR);
		lew.writeInt(p.getId());
		lew.writeBool(true);
		writeAvatar(lew, p, false);
		Inventory inv = p.getInventory(InventoryType.EQUIPPED);
		Collection<InventorySlot> equippedC = inv.getAll().values();
		List<Ring> rings = new ArrayList<Ring>();
		for (InventorySlot item : equippedC)
			if (item.getType() == ItemType.RING)
				rings.add((Ring) item);
		Collections.sort(rings);
		lew.writeByte((byte) 0);
		if (rings.size() > 0) {
			for (Ring ring : rings) {
				lew.writeBool(true);
				lew.writeLong(ring.getUniqueId());
				lew.writeLong(ring.getPartnerRingId());
				lew.writeInt(ring.getDataId());
			}
		} else {
			lew.writeBool(false);
		}
		lew.writeShort((short) 0);
		lew.writeShort((short) 0);

		return lew.getBytes();
	}

	public static byte[] writeBuffMapVisualEffect(Player p, byte effectType, int skillId, byte skillLevel, byte direction) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(direction != 3 ? 13 : 12);

		lew.writeShort(ClientSendOps.THIRD_PERSON_VISUAL_EFFECT);
		lew.writeInt(p.getId());
		lew.writeByte(effectType);
		lew.writeInt(skillId);
		lew.writeByte(skillLevel);
		if (direction != 3)
			lew.writeByte(direction);

		return lew.getBytes();
	}

	private static byte[] writeShowThirdPersonEffect(Player p, byte effectId) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(7);
		lew.writeShort(ClientSendOps.THIRD_PERSON_VISUAL_EFFECT);
		lew.writeInt(p.getId());
		lew.writeByte(effectId);
		return lew.getBytes();
	}

	public static byte[] writeShowLevelUp(Player p) {
		return writeShowThirdPersonEffect(p, StatusEffectTools.LEVEL_UP);
	}

	public static byte[] writeShowJobChange(Player p) {
		return writeShowThirdPersonEffect(p, StatusEffectTools.JOB_ADVANCEMENT);
	}

	public static byte[] writeShowQuestEffect(Player p) {
		return writeShowThirdPersonEffect(p, StatusEffectTools.QUEST);
	}

	public static byte[] writeBuffMapEffect(Player p, Map<PlayerStatusEffect, Short> stats) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter();

		lew.writeShort(ClientSendOps.THIRD_PERSON_APPLY_STATUS_EFFECT);
		lew.writeInt(p.getId());
		long updateMask = 0;
		for (PlayerStatusEffect key : stats.keySet())
			updateMask |= key.longValue();
		lew.writeLong(0);
		lew.writeLong(updateMask);
		for (Entry<PlayerStatusEffect, Short> statupdate : stats.entrySet()) {
			if (statupdate.getKey() == PlayerStatusEffect.SHADOW_STARS) {
				lew.writeShort((short) 0);
				lew.writeByte((byte) 0);
			}
			lew.writeShort(statupdate.getValue().shortValue());
		}
		lew.writeShort((short) 0);
		lew.writeByte((byte) 0);

		return lew.getBytes();
	}

	public static byte[] writeDebuffMapEffect(Player p, Map<PlayerStatusEffect, Short> stats, short skillId, short skillLevel, short delay) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter();

		lew.writeShort(ClientSendOps.THIRD_PERSON_APPLY_STATUS_EFFECT);
		lew.writeInt(p.getId());
		long updateMask = 0;
		for (PlayerStatusEffect key : stats.keySet())
			updateMask |= key.longValue();
		lew.writeLong(0);
		lew.writeLong(updateMask);
		for (Short statupdate : stats.values()) {
			if (skillId == MobSkills.MIST)
				lew.writeShort(statupdate.shortValue());
			lew.writeShort(skillId);
			lew.writeShort(skillLevel);
		}
		lew.writeShort((short) 0);
		lew.writeShort(delay);

		return lew.getBytes();
	}

	public static byte[] writeCancelStatusEffectMapEffect(Player p, Set<PlayerStatusEffect> stats) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(22);

		lew.writeShort(ClientSendOps.THIRD_PERSON_CANCEL_STATUS_EFFECT);
		lew.writeInt(p.getId());
		long updateMask = 0;
		for (PlayerStatusEffect key : stats)
			updateMask |= key.longValue();
		lew.writeLong(0);
		lew.writeLong(updateMask);

		return lew.getBytes();
	}

	public static byte[] writeShowItemGain(int itemid, int quantity) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(20);

		lew.writeShort(ClientSendOps.SHOW_STATUS_INFO);
		lew.writeByte(PacketSubHeaders.STATUS_INFO_INVENTORY);
		lew.writeByte((byte) 0);
		lew.writeInt(itemid);
		lew.writeInt(quantity);
		lew.writeInt(0);
		lew.writeInt(0);

		return lew.getBytes();
	}

	public static byte[] writeShowMesoGain(int gain) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(10);

		lew.writeShort(ClientSendOps.SHOW_STATUS_INFO);
		lew.writeByte(PacketSubHeaders.STATUS_INFO_INVENTORY);
		lew.writeByte((byte) 1);
		lew.writeInt(gain);
		lew.writeShort((short) 0);

		return lew.getBytes();
	}

	private static byte[] writeShowInventoryStatus(byte mode) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(12);

		lew.writeShort(ClientSendOps.SHOW_STATUS_INFO);
		lew.writeByte(PacketSubHeaders.STATUS_INFO_INVENTORY);
		lew.writeByte(mode);
		lew.writeInt(0);
		lew.writeInt(0);

		return lew.getBytes();
	}

	public static byte[] writeShowInventoryFull() {
		return writeShowInventoryStatus((byte) 0xFF);
	}

	public static byte[] writeShowInventoryUnavailable() {
		return writeShowInventoryStatus((byte) 0xFE);
	}

	public static byte[] writeQuestForfeit(short questId) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(16);

		lew.writeShort(ClientSendOps.SHOW_STATUS_INFO);
		lew.writeByte(PacketSubHeaders.STATUS_INFO_QUEST);
		lew.writeShort(questId);
		lew.writeByte(QuestEntry.STATE_NOT_STARTED);
		lew.writeShort((short) 0);
		lew.writeLong(0);

		return lew.getBytes();
	}

	public static byte[] writeQuestProgress(short questId, String data) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(8 + data.length());

		lew.writeShort(ClientSendOps.SHOW_STATUS_INFO);
		lew.writeByte(PacketSubHeaders.STATUS_INFO_QUEST);
		lew.writeShort(questId);
		lew.writeByte(QuestEntry.STATE_STARTED);
		lew.writeLengthPrefixedString(data);

		return lew.getBytes();
	}

	public static byte[] writeQuestCompleted(short questId, QuestEntry status) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter();

		lew.writeShort(ClientSendOps.SHOW_STATUS_INFO);
		lew.writeByte(PacketSubHeaders.STATUS_INFO_QUEST);
		lew.writeShort(questId);
		lew.writeByte(QuestEntry.STATE_COMPLETED);
		lew.writeLong(TimeUtil.unixToWindowsTime(status.getCompletionTime()));

		return lew.getBytes();
	}

	public static byte[] writeShowExpGain(int gain, boolean white, boolean fromQuest) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(!fromQuest ? 20 : 21);

		lew.writeShort(ClientSendOps.SHOW_STATUS_INFO);
		lew.writeByte(PacketSubHeaders.STATUS_INFO_EXP);
		lew.writeBool(white);
		lew.writeInt(gain);
		lew.writeBool(fromQuest);
		lew.writeByte((byte) 0);
		lew.writeShort((short) 0);
		lew.writeLong(0);
		if (fromQuest)
			lew.writeByte((byte) 0);

		return lew.getBytes();
	}

	/**
	 * 
	 * @param gain
	 * @param pointType must be one of the following values from
	 * PacketSubHeaders: STATUS_INFO_FAME, STATUS_INFO_MESOS,
	 * STATUS_INFO_GUILD_POINTS
	 * @return
	 */
	public static byte[] writeShowPointsGainFromQuest(int gain, byte pointType) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(7);

		lew.writeShort(ClientSendOps.SHOW_STATUS_INFO);
		lew.writeByte(pointType);
		lew.writeInt(gain);

		return lew.getBytes();
	}

	public static byte[] writeShowQuestReqsFulfilled(short questId) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(4);

		lew.writeShort(ClientSendOps.SHOW_QUEST_COMPLETION);
		lew.writeShort(questId);

		return lew.getBytes();
	}

	public static byte[] writeShowSelfBuff(int skillId, byte skillLevel) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(8);

		lew.writeShort(ClientSendOps.FIRST_PERSON_VISUAL_EFFECT);
		lew.writeByte(StatusEffectTools.ACTIVE_BUFF);
		lew.writeInt(skillId);
		lew.writeByte(skillLevel);

		return lew.getBytes();
	}

	public static byte[] writeShowItemGainFromQuest(int itemid, int quantity) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(12);

		lew.writeShort(ClientSendOps.FIRST_PERSON_VISUAL_EFFECT);
		lew.writeByte(StatusEffectTools.ITEM_GAIN);
		lew.writeByte((byte) 1); //Number of different items (itemid and amount gets repeated)
		lew.writeInt(itemid);
		lew.writeInt(quantity);

		return lew.getBytes();
	}

	/**
	 *
	 * @param p
	 * @param effectType a const from StatusEffectTools.
	 * @param skillId
	 * @return
	 */
	public static byte[] writeShowSelfEffect(Player p, byte effectType, int skillId) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(7);

		lew.writeShort(ClientSendOps.FIRST_PERSON_VISUAL_EFFECT);
		lew.writeByte(effectType);
		lew.writeInt(skillId);

		return lew.getBytes();
	}

	public static byte[] writeShowSelfQuestEffect() {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(3);

		lew.writeShort(ClientSendOps.FIRST_PERSON_VISUAL_EFFECT);
		lew.writeByte(StatusEffectTools.QUEST);

		return lew.getBytes();
	}

	public static byte[] writeQuestStartSuccess(short questId, int npcId) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(13);

		lew.writeShort(ClientSendOps.QUEST_START);
		lew.writeByte(QuestEntry.QUEST_START_SUCCESS);
		lew.writeShort(questId);
		lew.writeInt(npcId);
		lew.writeShort((short) 0);

		return lew.getBytes();
	}

	public static byte[] writeQuestStartNext(short questId, int npcId, short next) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(11);

		lew.writeShort(ClientSendOps.QUEST_START);
		lew.writeByte(QuestEntry.QUEST_START_SUCCESS);
		lew.writeShort(questId);
		lew.writeInt(npcId);
		lew.writeShort(next);

		return lew.getBytes();
	}

	public static byte[] writeQuestStartError(short questId, byte errorType) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(9);

		lew.writeShort(ClientSendOps.QUEST_START);
		lew.writeByte(errorType);
		lew.writeShort(questId);
		lew.writeInt(0);

		return lew.getBytes();
	}

	public static byte[] writeInventoryFull() {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(4);

		lew.writeShort(ClientSendOps.MODIFY_INVENTORY_SLOT);
		lew.writeBool(true);
		lew.writeByte(PacketSubHeaders.INVENTORY_FULL);

		return lew.getBytes();
	}

	public static byte[] writeInventoryAddSlot(InventoryType type, short pos, InventorySlot item) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter();

		lew.writeShort(ClientSendOps.MODIFY_INVENTORY_SLOT);
		lew.writeBool(true);
		lew.writeByte(PacketSubHeaders.INVENTORY_CHANGE_SLOT);
		lew.writeByte((byte) 0);
		lew.writeByte(type.byteValue());
		lew.writeByte((byte) pos);
		writeItemInfo(lew, (short) 0, item);

		return lew.getBytes();
	}

	public static byte[] writeInventorySlotUpdate(InventoryType type, short pos, InventorySlot item) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(10);

		lew.writeShort(ClientSendOps.MODIFY_INVENTORY_SLOT);
		lew.writeBool(true);
		lew.writeByte(PacketSubHeaders.INVENTORY_CHANGE_SLOT);
		lew.writeByte((byte) 1);
		lew.writeByte(type.byteValue());
		lew.writeByte((byte) pos);
		lew.writeBool(false);
		lew.writeShort(item.getQuantity());

		return lew.getBytes();
	}

	public static byte[] writeInventoryDropItem(InventoryType type, short src, short quantity) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(10);

		lew.writeShort(ClientSendOps.MODIFY_INVENTORY_SLOT);
		lew.writeBool(true);
		lew.writeByte(PacketSubHeaders.INVENTORY_CHANGE_SLOT);
		lew.writeByte((byte) 1);
		lew.writeByte(type.byteValue());
		lew.writeShort(src);
		lew.writeShort(quantity);

		return lew.getBytes();
	}

	public static byte[] writeInventoryMoveItem(InventoryType type, short src, short dst, byte equipIndicator) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(equipIndicator == -1 ? 10 : 11);

		lew.writeShort(ClientSendOps.MODIFY_INVENTORY_SLOT);
		lew.writeBool(true);
		lew.writeByte(PacketSubHeaders.INVENTORY_CHANGE_SLOT);
		lew.writeByte((byte) 2);
		lew.writeByte(type.byteValue());
		lew.writeShort(src);
		lew.writeShort(dst);
		if (equipIndicator != -1)
			lew.writeByte(equipIndicator);

		return lew.getBytes();
	}

	public static byte[] writeInventoryClearSlot(InventoryType type, short slot) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(slot >= 0 ? 8 : 9);

		lew.writeShort(ClientSendOps.MODIFY_INVENTORY_SLOT);
		lew.writeBool(true);
		lew.writeByte(PacketSubHeaders.INVENTORY_CHANGE_SLOT);
		lew.writeByte((byte) 3);
		lew.writeByte(type.byteValue());
		lew.writeShort(slot);
		if (slot < 0)
			lew.writeBool(true);

		return lew.getBytes();
	}

	public static byte[] writeInventoryMoveItemShiftQuantities(InventoryType type, short src, short srcQty, short dst, short dstQty) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(10);

		lew.writeShort(ClientSendOps.MODIFY_INVENTORY_SLOT);
		lew.writeBool(true);
		lew.writeByte(PacketSubHeaders.INVENTORY_SHIFT_QTY);
		lew.writeByte((byte) 1);
		lew.writeByte(type.byteValue());
		lew.writeShort(src);
		lew.writeShort(srcQty);
		lew.writeBool(true);
		lew.writeByte(type.byteValue());
		lew.writeShort(dst);
		lew.writeShort(dstQty);

		return lew.getBytes();
	}

	public static byte[] writeInventoryMoveItemCombineQuantities(InventoryType type, short src, short dst, short total) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(10);

		lew.writeShort(ClientSendOps.MODIFY_INVENTORY_SLOT);
		lew.writeBool(true);
		lew.writeByte(PacketSubHeaders.INVENTORY_SHIFT_QTY);
		lew.writeByte((byte) 3);
		lew.writeByte(type.byteValue());
		lew.writeShort(src);
		lew.writeBool(true);
		lew.writeByte(type.byteValue());
		lew.writeShort(dst);
		lew.writeShort(total);

		return lew.getBytes();
	}

	public static byte[] writeCooldown(int skill, short seconds) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(8);
		lew.writeShort(ClientSendOps.COOLDOWN);
		lew.writeInt(skill);
		lew.writeShort(seconds);
		return lew.getBytes();
	}

	public static byte[] writeChangeMap(int mapid, byte spawnPoint, Player p) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(26);

		lew.writeShort(ClientSendOps.CHANGE_MAP);
		lew.writeInt(p.getClient().getChannel() - 1);
		lew.writeShort((short) 2);
		lew.writeShort((short) 0);
		lew.writeInt(mapid);
		lew.writeByte(spawnPoint);
		lew.writeShort(p.getHp()); // hp (???)
		lew.writeByte((byte) 0);
		//long questMask = 0x1FFFFFFFFFFFFFFL;
		long questMask = TimeUtil.unixToWindowsTime(System.currentTimeMillis());
		lew.writeLong(questMask);

		return lew.getBytes();
	}

	private static void writeMapEntryStatusEffectValue(LittleEndianWriter lew, PlayerStatusEffect key, PlayerStatusEffectValues v) {
		//perhaps it would be more concise if we didn't use a switch-case, and just use some conditionals if there are patterns.
		switch (key) {
			default: //give no value at all
				break;
			case COMBO: //TODO: save (combo + 1) in v.mod!!
			case JUMP:
				lew.writeByte((byte) v.getModifier());
				break;
			case HOMING_BEACON: //all non-debuff 5th byte keys
			case MORPH:
			case RECOVERY:
			case MAPLE_WARRIOR:
			case STANCE:
			case SHARP_EYES:
			case MANA_REFLECTION:
			case DRAGON_ROAR:
				lew.writeShort(v.getModifier());
				break;
			case SEDUCE: //all debuffs besides slow (glitch in global, SLOW doesn't display properly and if you try, it error 38s)
			case STUN:
			case POISON:
			case SEAL:
			case DARKNESS:
			case WEAKEN:
			case CURSE:
				lew.writeShort((short) v.getSource());
				lew.writeShort(v.getModifier());
				break;
			case CHARGE:
				lew.writeInt(0 /* p.getCharge() */);
				break;
			case SHADOW_STARS:
				lew.writeInt(v.getModifier());
				break;
		}
	}

	public static byte[] writeShowPlayer(Player p) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter();

		lew.writeShort(ClientSendOps.SHOW_PLAYER);
		lew.writeInt(p.getId());
		lew.writeLengthPrefixedString(p.getName());
		/*if (p.getGuildId() > 0) {
			MapleGuildSummary gs = p.getClient().getChannelServer().getGuildSummary(p.getGuildId());

			if (gs != null) {
				lew.writeLengthPrefixedString(gs.getName());
				lew.writeShort(gs.getLogoBG());
				lew.writeByte(gs.getLogoBGColor());
				lew.writeShort(gs.getLogo());
				lew.writeByte(gs.getLogoColor());
			} else {
				lew.writeLengthPrefixedString("");
				lew.writeShort((short) 0);
				lew.writeByte((byte) 0);
				lew.writeShort((short) 0);
				lew.writeByte((byte) 0);
			}
		} else {*/
			lew.writeLengthPrefixedString("");
			lew.writeShort((short) 0);
			lew.writeByte((byte) 0);
			lew.writeShort((short) 0);
			lew.writeByte((byte) 0);
		//}
		lew.writeInt(0);
		lew.writeInt(1);

		long updateMask = 0;
		Map<PlayerStatusEffect, PlayerStatusEffectValues> statusEffects = new TreeMap<PlayerStatusEffect, PlayerStatusEffectValues>(new Comparator<PlayerStatusEffect>() {
			//sort by value order (i.e. 5,6,7,8,1,2,3,4), then by mask (i.e. enum order)
			public int compare(PlayerStatusEffect k1, PlayerStatusEffect k2) {
				int diff = k1.getValueOrder() - k2.getValueOrder();
				if (diff == 0) //if k1 and k2 share the same value order
					//sort by enum order (which should be smallest to biggest)
					diff = k1.compareTo(k2); //also equivalent to ((int) (k1.longValue() - k2.longValue()))
				return diff;
			}
		});
		statusEffects.putAll(p.getAllEffects());
		for (PlayerStatusEffect key : statusEffects.keySet())
			updateMask |= key.longValue();
		//no idea why we have to do it, but make the 4th byte (in a 64-bit little endian integer) = 0xF8
		lew.writeLong(updateMask & 0xFFFFFFFF00FFFFFFL | 0x00000000F8000000L);
		for (Entry<PlayerStatusEffect, PlayerStatusEffectValues> effect : statusEffects.entrySet())
			writeMapEntryStatusEffectValue(lew, effect.getKey(), effect.getValue());
		lew.writeInt(0);
		//we write the 4th byte here. yeah. what the fuck Nexon.
		lew.writeByte((byte) ((updateMask & 0x00000000FF000000L) >> 24));
		lew.writeByte((byte) 0);
		lew.writeInt(0);

		int CHAR_MAGIC_SPAWN = Rng.getGenerator().nextInt();
		lew.writeInt(CHAR_MAGIC_SPAWN);
		lew.writeShort((short) 0);
		lew.writeLong(0);
		lew.writeInt(CHAR_MAGIC_SPAWN);
		lew.writeShort((short) 0);
		lew.writeLong(0);
		lew.writeInt(CHAR_MAGIC_SPAWN);
		lew.writeShort((short) 0);
		//TODO: should we only check if a player has an equipped
		//mount, and not if they have monster riding on?
		if (p.isEffectActive(PlayerStatusEffect.MONSTER_RIDING)) {
			/*TamingMob mount = p.getEquippedMount();
			if (mount != null) {
				lew.writeInt(mount.getDataId());
				lew.writeInt(mount.getSkillId());
				lew.writeInt(CHAR_MAGIC_SPAWN);
			} else {*/
				lew.writeInt(1932000);
				lew.writeInt(5221006);
				lew.writeInt(CHAR_MAGIC_SPAWN);
			//}
		} else {
			lew.writeInt(0);
			lew.writeInt(0);
			lew.writeInt(CHAR_MAGIC_SPAWN);
		}

		lew.writeLong(0);
		lew.writeInt(CHAR_MAGIC_SPAWN);
		lew.writeLong(0);
		lew.writeInt(0);
		lew.writeShort((short) 0);
		lew.writeInt(CHAR_MAGIC_SPAWN);
		lew.writeInt(0);
		lew.writeShort(p.getJob()); // 40 01?
		writeAvatar(lew, p, false);
		lew.writeInt(0);
		lew.writeInt(p.getItemEffect());
		lew.writeInt(p.getChair());
		lew.writePos(p.getPosition());
		lew.writeByte((byte) p.getStance());
		lew.writeShort(p.getFoothold());
		lew.writeByte((byte) 0);
		for (Pet pet : p.getPets()) {
			if (pet != null) {
				lew.writeByte((byte) 1);
				lew.writeInt(pet.getDataId());
				lew.writeLengthPrefixedString(pet.getName());
				lew.writeLong(pet.getUniqueId());
				lew.writePos(pet.getPosition());
				lew.writeByte(pet.getStance());
				lew.writeInt(pet.getFoothold());
			}
		}
		/*PlayerInteractionRoom room = p.getInteractionRoom();
		if (room != null && room.isOwner(p))
			addAnnounceBox(lew, room);
		else
			lew.writeByte((byte) 0);

		lew.writeShort((short) 0);
		Map<Short, InventorySlot> equippedC = p.getInventory(InventoryType.EQUIPPED).getAll();
		List<Ring> rings = new ArrayList<Ring>();
		for (Entry<Short, InventorySlot> slot : equippedC.entrySet()) {
			if (slot.getValue().getType() == ItemType.RING) {
				rings.add((Ring) slot.getValue());
			}
		}
		if (rings.size() > 0) {
			lew.writeByte((byte) 0);
			for (Ring ring : rings) {
				lew.writeByte((byte) 1);
				lew.writeLong(ring.getUniqueId());
				lew.writeLong(ring.getPartnerRingId());
				lew.writeInt(ring.getDataId());
			}
			lew.writeShort((short) 0);
		} else {
			lew.writeInt(0);
		}*/
		lew.writeByte((byte) 0);
		lew.writeShort((short) 1);
		lew.writeInt(0);
		lew.writeInt(0);
		lew.writeInt(0);
		lew.writeInt(0);
		lew.writeInt(0);
		return lew.getBytes();
	}

	public static byte[] writeRemovePlayer(Player p) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(6);

		lew.writeShort(ClientSendOps.REMOVE_PLAYER);
		lew.writeInt(p.getId());

		return lew.getBytes();
	}

	public static byte[] writeShowPet(Player p, byte slot, Pet pet,
			boolean equip, boolean hunger) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(equip ? 32 + pet.getName().length() : 9);

		lew.writeShort(ClientSendOps.SHOW_PET);
		lew.writeInt(p.getId());
		lew.writeByte(slot);
		lew.writeBool(equip);
		lew.writeBool(hunger);
		if (equip) {
			lew.writeInt(pet.getDataId());
			lew.writeLengthPrefixedString(pet.getName());
			lew.writeLong(pet.getUniqueId());
			lew.writePos(pet.getPosition());
			lew.writeByte(pet.getStance());
			lew.writeShort(pet.getFoothold());
			lew.writeBool(false); //has name tag
			lew.writeBool(false); //has quote item
		}

		return lew.getBytes();
	}

	public static byte[] writePlayerNpcLook(PlayerNpc pnpc) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(22);
		lew.writeShort(ClientSendOps.PLAYER_NPC);
		lew.writeBool(true);
		lew.writeInt(pnpc.getDataId());
		lew.writeLengthPrefixedString(pnpc.getPlayerName());
		writeAvatar(lew, pnpc.getGender(), pnpc.getSkinColor(), pnpc.getEyes(),
				true, pnpc.getHair(), pnpc.getEquips(), new Pet[3]);
		return lew.getBytes();
	}

	private static void writeMonsterData(LittleEndianWriter lew, Mob monster, boolean newSpawn, byte effect) {
		lew.writeInt(monster.getId());
		lew.writeByte((byte) 5);
		lew.writeInt(monster.getDataId());

		//mob status
		lew.writeByte((byte) 0);
		lew.writeShort((short) 0);
		lew.writeByte((byte) 8);
		lew.writeInt(0);

		lew.writePos(monster.getPosition());
		lew.writeByte(monster.getStance());
		lew.writeShort((short) 0);
		lew.writeShort(monster.getFoothold());
		if (effect > 0) {
			lew.writeByte(effect);
			lew.writeByte((byte) 0);
			lew.writeShort((short) 0);
		}
		lew.writeShort((short) (newSpawn ? -2 : -1));
		lew.writeInt(0);
	}

	/**
	 * Spawn an monster without making the client animate the monster for itself.
	 * @param monster
	 * @param newSpawn
	 * @param effect
	 * @return
	 */
	public static byte[] writeShowMonster(Mob monster, boolean newSpawn, byte effect) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter();
		lew.writeShort(ClientSendOps.SHOW_MONSTER);
		writeMonsterData(lew, monster, newSpawn, effect);
		return lew.getBytes();
	}

	/**
	 * This will only make the monster disappear if it was spawned by sending
	 * writeShowMonster.
	 * @param monster
	 * @param animation
	 * @return
	 */
	public static byte[] writeRemoveMonster(Mob m, byte animation) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(7);
		lew.writeShort(ClientSendOps.REMOVE_MONSTER);
		lew.writeInt(m.getId());
		lew.writeByte(animation);
		return lew.getBytes();
	}

	/**
	 * Spawn an monster and make the client animate it. The client will not be
	 * physically affected by it (i.e. it can't hit or be hurt by the monster)
	 * unless writeShowMonster is also sent.
	 * @param monster
	 * @param aggro
	 * @return
	 */
	public static byte[] writeShowAndControlMonster(Mob monster, boolean aggro) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter();
		lew.writeShort(ClientSendOps.CONTROL_MONSTER);
		lew.writeByte((byte) (monster.isVisible() ? aggro ? 2 : 1 : 0));
		if (monster.isVisible())
			writeMonsterData(lew, monster, false, (byte) 0);
		else
			lew.writeInt(monster.getId());
		return lew.getBytes();
	}

	public static byte[] writeStopControlMonster(Mob monster) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(7);
		lew.writeShort(ClientSendOps.CONTROL_MONSTER);
		lew.writeByte((byte) 0);
		lew.writeInt(monster.getId());
		return lew.getBytes();
	}

	public static byte[] writeMonsterBuff(Mob monster, Map<MonsterStatusEffect, Short> stats, short skillId, short skillLevel, short delay) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(7);

		lew.writeShort(ClientSendOps.APPLY_MONSTER_STATUS_EFFECT);
		lew.writeInt(monster.getId());
		int updateMask = 0;
		for (MonsterStatusEffect key : stats.keySet())
			updateMask |= key.intValue();

		lew.writeInt(updateMask);
		for (Short statupdate : stats.values()) {
			lew.writeShort(statupdate.shortValue());
			lew.writeShort(skillId);
			lew.writeShort(skillLevel);
			lew.writeShort((short) 0);
		}
		lew.writeShort(delay); //delay in ms
		lew.writeBool(true);

		return lew.getBytes();
	}

	public static byte[] writeMonsterDebuff(Mob monster, Map<MonsterStatusEffect, Short> stats, int skillId, short delay) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(7);

		lew.writeShort(ClientSendOps.APPLY_MONSTER_STATUS_EFFECT);
		lew.writeInt(monster.getId());
		int updateMask = 0;
		for (MonsterStatusEffect key : stats.keySet())
			updateMask |= key.intValue();

		lew.writeInt(updateMask);
		for (Short statupdate : stats.values()) {
			lew.writeShort(statupdate.shortValue());
			lew.writeInt(skillId);
			lew.writeShort((short) 0);
		}
		lew.writeShort(delay); //delay in ms
		lew.writeBool(true);

		return lew.getBytes();
	}

	public static byte[] writeMonsterCancelStatusEffect(Mob monster, Set<MonsterStatusEffect> stats) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(7);

		lew.writeShort(ClientSendOps.CANCEL_MONSTER_STATUS_EFFECT);
		lew.writeInt(monster.getId());
		int updateMask = 0;
		for (MonsterStatusEffect key : stats)
			updateMask |= key.intValue();

		lew.writeInt(updateMask);
		lew.writeBool(true);

		return lew.getBytes();
	}

	private static void writeNpcData(LittleEndianWriter lew, Npc npc) {
		lew.writeInt(npc.getId());
		lew.writeInt(npc.getDataId());
		lew.writeShort((short) npc.getPosition().x);
		lew.writeShort(npc.getCy());
		lew.writeByte(npc.getStance());
		lew.writeShort(npc.getFoothold());
		lew.writeShort(npc.getRx0());
		lew.writeShort(npc.getRx1());
		lew.writeBool(true);
	}

	/**
	 * Spawn an NPC without making the client animate the NPC for itself.
	 * @param npc
	 * @return
	 */
	public static byte[] writeShowNpc(Npc npc) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(22);
		lew.writeShort(ClientSendOps.SHOW_NPC);
		writeNpcData(lew, npc);
		return lew.getBytes();
	}

	/**
	 * This will only make the NPC disappear if it was spawned by sending
	 * writeShowNpc.
	 * @param npc
	 * @return
	 */
	public static byte[] writeRemoveNpc(Npc npc) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(6);
		lew.writeShort(ClientSendOps.REMOVE_NPC);
		lew.writeInt(npc.getId());
		return lew.getBytes();
	}

	/**
	 * Spawn an NPC and make the client animate it.
	 * @param npc
	 * @return
	 */
	public static byte[] writeShowAndControlNpc(Npc npc) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(22);
		lew.writeShort(ClientSendOps.CONTROL_NPC);
		lew.writeByte((byte) 1);
		writeNpcData(lew, npc);
		return lew.getBytes();
	}

	/**
	 * This will only make the NPC disappear if it was spawned by sending
	 * writeShowAndControlNpc.
	 * @param npc
	 * @return
	 */
	public static byte[] writeStopControlAndRemoveNpc(Npc npc) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(22);
		lew.writeShort(ClientSendOps.CONTROL_NPC);
		lew.writeByte((byte) 0);
		lew.writeInt(npc.getId());
		return lew.getBytes();
	}

	public static byte[] writeShowItemDrop(ItemDrop drop, byte animation, byte pickupAllow) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter();

		lew.writeShort(ClientSendOps.SHOW_ITEM_DROP);
		lew.writeByte(animation);
		lew.writeInt(drop.getId());
		lew.writeByte(drop.getDropType());
		lew.writeInt(drop.getDataId());
		lew.writeInt(drop.getOwner());
		lew.writeByte(pickupAllow);
		lew.writePos(drop.getPosition());
		lew.writeInt(0); //source mob entity id
		if (animation != ItemDrop.SPAWN_ANIMATION_NONE) {
			lew.writePos(drop.getSourcePos());
			lew.writeShort((short) 0);
		}

		if (drop.getDropType() == ItemDrop.ITEM)
			writeItemExpire(lew, drop.getItemExpire(), true);
		lew.writeBool(true); //allow pet item pickup?

		return lew.getBytes();
	}

	public static byte[] writeRemoveItemDrop(ItemDrop d, byte animation) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter();

		lew.writeShort(ClientSendOps.REMOVE_ITEM_DROP);
		lew.writeByte(animation);
		lew.writeInt(d.getId());
		if (animation > ItemDrop.DESTROY_ANIMATION_NONE) {
			lew.writeInt(d.getOwner());
			if (d.getPetSlot() >= 0)
				lew.writeByte(d.getPetSlot());
		}

		return lew.getBytes();
	}

	public static byte[] writeShowMist(Mist mist) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(41);

		lew.writeShort(ClientSendOps.SHOW_MIST);
		lew.writeInt(mist.getId());
		lew.writeInt(mist.getMistType());
		lew.writeInt(mist.getOwner());
		lew.writeInt(mist.getSkillId());
		lew.writeByte(mist.getSkillLevel());
		lew.writeShort(mist.getSkillDelay());
		Rectangle box = mist.getBox();
		lew.writeInt(box.x);
		lew.writeInt(box.y);
		lew.writeInt(box.x + box.width);
		lew.writeInt(box.y + box.height);
		lew.writeInt(0);

		return lew.getBytes();
	}

	public static byte[] writeRemoveMist(Mist mist) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(6);

		lew.writeShort(ClientSendOps.REMOVE_MIST);
		lew.writeInt(mist.getId());

		return lew.getBytes();
	}

	public static byte[] writeShowMysticDoor(MysticDoor d) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(11);

		lew.writeShort(ClientSendOps.SHOW_DOOR);
		lew.writeBool(d.isInTown());
		lew.writeInt(d.getId());
		lew.writePos(d.getPosition());

		return lew.getBytes();
	}

	public static byte[] writeRemoveMysticDoor(MysticDoor d) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(d.isInTown() ? 10 : 7);

		//what.
		if (d.isInTown()) {
			lew.writeShort(ClientSendOps.SPAWN_PORTAL);
			lew.writeInt(GlobalConstants.NULL_MAP);
			lew.writeInt(GlobalConstants.NULL_MAP);
		} else {
			lew.writeShort(ClientSendOps.REMOVE_DOOR);
			lew.writeBool(false);
			lew.writeInt(d.getId());
		}

		return lew.getBytes();
	}

	public static byte[] writeTriggerReactor(Reactor reactor) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(15);

		lew.writeShort(ClientSendOps.HIT_REACTOR);
		lew.writeInt(reactor.getId());
		lew.writeByte(reactor.getStateId());
		lew.writePos(reactor.getPosition());
		lew.writeShort(reactor.getStance());
		lew.writeBool(false);
		lew.writeByte((byte) 5); // frame delay, set to 5 since there doesn't appear to be a fixed formula for it

		return lew.getBytes();
	}

	public static byte[] writeShowReactor(Reactor reactor) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(16);

		lew.writeShort(ClientSendOps.SHOW_REACTOR);
		lew.writeInt(reactor.getId());
		lew.writeInt(reactor.getDataId());
		lew.writeByte(reactor.getStateId());
		lew.writePos(reactor.getPosition());
		lew.writeBool(false);

		return lew.getBytes();
	}

	public static byte[] writeRemoveReactor(Reactor reactor) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(11);

		lew.writeShort(ClientSendOps.REMOVE_REACTOR);
		lew.writeInt(reactor.getId());
		lew.writeByte(reactor.getStateId());
		lew.writePos(reactor.getPosition());

		return lew.getBytes();
	}

	public static void writeSerializedMovements(LittleEndianByteArrayWriter lew, List<LifeMovementFragment> moves) {
		lew.writeByte((byte) moves.size());
		for (LifeMovementFragment move : moves)
			move.serialize(lew);
	}

	public static byte[] writePrivateChatMessage(byte type, String name, String message) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(7
				+ name.length() + message.length());

		lew.writeShort(ClientSendOps.PRIVATE_CHAT);
		lew.writeByte(type);
		lew.writeLengthPrefixedString(name);
		lew.writeLengthPrefixedString(message);

		return lew.getBytes();
	}

	public static byte[] writeSpouseChatMessage(String name, String message) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(6
				+ name.length() + message.length());

		lew.writeShort(ClientSendOps.SPOUSE_CHAT);
		lew.writeLengthPrefixedString(name);
		lew.writeLengthPrefixedString(message);

		return lew.getBytes();
	}

	public static byte[] writeServerMessage(byte type, String message, byte channel, boolean megaEar) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter((type == 3 ? 7 : type == 4 ? 6 : 5) + message.length());
		lew.writeShort(ClientSendOps.SERVER_MESSAGE);
		lew.writeByte(type);
		if (type == 4) //scrolling message ticker
			lew.writeBool(true);
		lew.writeLengthPrefixedString(message);
		if (type == 3) { //smega
			lew.writeByte((byte) (channel - 1));
			lew.writeBool(megaEar);
		}
		return lew.getBytes();
	}

	public static byte[] writeWhisperMessge(String name, String message, byte channel) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(9 + message.length());
		lew.writeShort(ClientSendOps.WHISPER);
		lew.writeByte((byte) 0x12); //???
		lew.writeLengthPrefixedString(name);
		lew.writeShort((short) (channel - 1));
		lew.writeLengthPrefixedString(message);
		return lew.getBytes();
	}

	public static byte[] writeTipMessage(String message) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(5 + message.length());
		lew.writeShort(ClientSendOps.TIP_MESSAGE);
		lew.writeByte((byte) 0xFF);
		lew.writeLengthPrefixedString(message);
		return lew.getBytes();
	}

	/*public static byte[] writeEnterCs(Player p) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter();

		lew.writeShort(ClientSendOps.CS_OPEN);
		writeCharData(lew, p);
		lew.writeByte((byte) 1);
		lew.writeLengthPrefixedString(p.getClient().getAccountName());
		lew.writeInt(0);
		lew.writeShort((short) 322); //amount of items in this packet.
		for (int sn = 10101048; sn <= 10101197; sn++) { //150 consecutive items with modifier 512
			lew.writeInt(sn);
			lew.writeShort(0x200);
			lew.write(1);
		}
		//170 more items that I can't see a clear pattern through.
		lew.writeBytes(getAdditionalCashItems());
		//No idea what this is, definitely not an sn.
		lew.writeBytes(HexTool.getByteArrayFromHexString("00 08 00 00 00 37 00 31 00 38 00 31 00 00 00 00 00 18 00 0E 00 0F 00 0C 06 38 02 14 00 08 80 B6 03 67 00 69 00 6E 00 49 00 70 00 00 00 00 00 00 00 06 00 04 00 13 00 0E 06 A8 01 14 00 D8 9F CD 03 33 00 2E 00 33 00 31 00 2E 00 32 00 33 00 35 00 2E 00 32 00 32 00 34 00 00 00 00 00 00 00 00 00 04 00 0A 00 15 01 0C 06 0E 00 00 00 62 00 65 00 67 00 69 00 6E 00 49 00 01 00 00 00 00 00 00 00 C5 FD FD 02 01 00 00 00 00 00 00 00 05 FE FD 02 01 00 00 00 00 00 00 00 13 FE FD 02 01 00 00 00 00 00 00 00 22 4A CB 01 01 00 00 00 00 00 00 00 C2 FD FD 02 01 00 00 00 01 00 00 00 C5 FD FD 02 01 00 00 00 01 00 00 00 05 FE FD 02 01 00 00 00 01 00 00 00 13 FE FD 02 01 00 00 00 01 00 00 00 22 4A CB 01 01 00 00 00 01 00 00 00 C2 FD FD 02 02 00 00 00 00 00 00 00 C5 FD FD 02 02 00 00 00 00 00 00 00 05 FE FD 02 02 00 00 00 00 00 00 00 13 FE FD 02 02 00 00 00 00 00 00 00 22 4A CB 01 02 00 00 00 00 00 00 00 C2 FD FD 02 02 00 00 00 01 00 00 00 C5 FD FD 02 02 00 00 00 01 00 00 00 05 FE FD 02 02 00 00 00 01 00 00 00 13 FE FD 02 02 00 00 00 01 00 00 00 22 4A CB 01 02 00 00 00 01 00 00 00 C2 FD FD 02 03 00 00 00 00 00 00 00 C5 FD FD 02 03 00 00 00 00 00 00 00 05 FE FD 02 03 00 00 00 00 00 00 00 13 FE FD 02 03 00 00 00 00 00 00 00 22 4A CB 01 03 00 00 00 00 00 00 00 C2 FD FD 02 03 00 00 00 01 00 00 00 C5 FD FD 02 03 00 00 00 01 00 00 00 05 FE FD 02 03 00 00 00 01 00 00 00 13 FE FD 02 03 00 00 00 01 00 00 00 22 4A CB 01 03 00 00 00 01 00 00 00 C2 FD FD 02 04 00 00 00 00 00 00 00 C5 FD FD 02 04 00 00 00 00 00 00 00 05 FE FD 02 04 00 00 00 00 00 00 00 13 FE FD 02 04 00 00 00 00 00 00 00 22 4A CB 01 04 00 00 00 00 00 00 00 C2 FD FD 02 04 00 00 00 01 00 00 00 C5 FD FD 02 04 00 00 00 01 00 00 00 05 FE FD 02 04 00 00 00 01 00 00 00 13 FE FD 02 04 00 00 00 01 00 00 00 22 4A CB 01 04 00 00 00 01 00 00 00 C2 FD FD 02 05 00 00 00 00 00 00 00 C5 FD FD 02 05 00 00 00 00 00 00 00 05 FE FD 02 05 00 00 00 00 00 00 00 13 FE FD 02 05 00 00 00 00 00 00 00 22 4A CB 01 05 00 00 00 00 00 00 00 C2 FD FD 02 05 00 00 00 01 00 00 00 C5 FD FD 02 05 00 00 00 01 00 00 00 05 FE FD 02 05 00 00 00 01 00 00 00 13 FE FD 02 05 00 00 00 01 00 00 00 22 4A CB 01 05 00 00 00 01 00 00 00 C2 FD FD 02 06 00 00 00 00 00 00 00 C5 FD FD 02 06 00 00 00 00 00 00 00 05 FE FD 02 06 00 00 00 00 00 00 00 13 FE FD 02 06 00 00 00 00 00 00 00 22 4A CB 01 06 00 00 00 00 00 00 00 C2 FD FD 02 06 00 00 00 01 00 00 00 C5 FD FD 02 06 00 00 00 01 00 00 00 05 FE FD 02 06 00 00 00 01 00 00 00 13 FE FD 02 06 00 00 00 01 00 00 00 22 4A CB 01 06 00 00 00 01 00 00 00 C2 FD FD 02 07 00 00 00 00 00 00 00 C5 FD FD 02 07 00 00 00 00 00 00 00 05 FE FD 02 07 00 00 00 00 00 00 00 13 FE FD 02 07 00 00 00 00 00 00 00 22 4A CB 01 07 00 00 00 00 00 00 00 C2 FD FD 02 07 00 00 00 01 00 00 00 C5 FD FD 02 07 00 00 00 01 00 00 00 05 FE FD 02 07 00 00 00 01 00 00 00 13 FE FD 02 07 00 00 00 01 00 00 00 22 4A CB 01 07 00 00 00 01 00 00 00 C2 FD FD 02 08 00 00 00 00 00 00 00 C5 FD FD 02 08 00 00 00 00 00 00 00 05 FE FD 02 08 00 00 00 00 00 00 00 13 FE FD 02 08 00 00 00 00 00 00 00 22 4A CB 01 08 00 00 00 00 00 00 00 C2 FD FD 02 08 00 00 00 01 00 00 00 C5 FD FD 02 08 00 00 00 01 00 00 00 05 FE FD 02 08 00 00 00 01 00 00 00 13 FE FD 02 08 00 00 00 01 00 00 00 22 4A CB 01 08 00 00 00 01 00 00 00 C2 FD FD 02 00 00 A3 00 26 71 0F 00 F3 20 9A 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 23 00 00 00 FF FF FF FF 0F 00 00 00 BD 68 32 01 BD 68 32 01 10 00 00 00 11 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 00 00 00 00 54 71 0F 00 F4 20 9A 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 28 00 00 00 FF FF FF FF 0F 00 00 00 BD 68 32 01 BD 68 32 01 10 00 00 00 11 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 00 00 00 00 58 71 0F 00 F5 20 9A 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 32 00 00 00 FF FF FF FF 0F 00 00 00 BD 68 32 01 BD 68 32 01 10 00 00 00 11 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 00 00 00 00 B5 E6 0F 00 F8 20 9A 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 1E 00 00 00 FF FF FF FF 0F 00 00 00 BD 68 32 01 BD 68 32 01 10 00 00 00 11 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 00 00 00 00 FD 4A 0F 00 FC 20 9A 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 14 00 00 00 FF FF FF FF 0F 00 00 00 BE 68 32 01 BE 68 32 01 0B 00 00 00 0C 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 29 71 0F 00 FD 20 9A 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 23 00 00 00 FF FF FF FF 0F 00 00 00 BE 68 32 01 BE 68 32 01 0B 00 00 00 0C 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 E5 DE 0F 00 FF 20 9A 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 2D 00 00 00 FF FF FF FF 0F 00 00 00 BE 68 32 01 BE 68 32 01 0B 00 00 00 0C 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 0B D1 10 00 00 21 9A 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 23 00 00 00 FF FF FF FF 0F 00 00 00 BE 68 32 01 BE 68 32 01 0B 00 00 00 0C 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 1C F9 19 00 01 21 9A 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 28 00 00 00 FF FF FF FF 0F 00 00 00 BE 68 32 01 BE 68 32 01 0B 00 00 00 0C 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 49 4B 4C 00 02 21 9A 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 1E 00 00 00 FF FF FF FF 0F 00 00 00 BE 68 32 01 BE 68 32 01 0B 00 00 00 0C 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 63 72 4C 00 03 21 9A 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 28 00 00 00 FF FF FF FF 0F 00 00 00 BE 68 32 01 BE 68 32 01 0B 00 00 00 0C 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 76 72 4C 00 04 21 9A 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 32 00 00 00 FF FF FF FF 0F 00 00 00 BE 68 32 01 BE 68 32 01 0B 00 00 00 0C 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 77 72 4C 00 05 21 9A 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 1E 00 00 00 FF FF FF FF 0F 00 00 00 BE 68 32 01 BE 68 32 01 0B 00 00 00 0C 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 FC 4A 0F 00 06 21 9A 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 28 00 00 00 FF FF FF FF 0F 00 00 00 BF 68 32 01 BF 68 32 01 0B 00 00 00 0C 00 00 00 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 F6 4B 0F 00 07 21 9A 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 1E 00 00 00 FF FF FF FF 0F 00 00 00 BF 68 32 01 BF 68 32 01 0B 00 00 00 0C 00 00 00 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 07 20 4E 00 3A 21 9A 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 28 00 00 00 00 00 00 00 0F 00 00 00 3D 6A 32 01 3D 6A 32 01 09 00 00 00 0A 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 79 72 4C 00 3B 21 9A 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 37 00 00 00 FF FF FF FF 0F 00 00 00 3D 6A 32 01 3D 6A 32 01 09 00 00 00 0A 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 42 BC 4E 00 3C 21 9A 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 50 00 00 00 FF FF FF FF 0F 00 00 00 3D 6A 32 01 3D 6A 32 01 09 00 00 00 0A 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 0A 31 10 00 3D 21 9A 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 28 00 00 00 FF FF FF FF 0F 00 00 00 3D 6A 32 01 3D 6A 32 01 09 00 00 00 0A 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 54 72 4C 00 3E 21 9A 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 28 00 00 00 FF FF FF FF 0F 00 00 00 3D 6A 32 01 3D 6A 32 01 09 00 00 00 0A 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 50 69 0F 00 3F 21 9A 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 28 00 00 00 FF FF FF FF 0F 00 00 00 3D 6A 32 01 3D 6A 32 01 09 00 00 00 0A 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 78 4B 0F 00 40 21 9A 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 19 00 00 00 FF FF FF FF 0F 00 00 00 3D 6A 32 01 3D 6A 32 01 09 00 00 00 0A 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 A8 F8 19 00 41 21 9A 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 28 00 00 00 FF FF FF FF 0F 00 00 00 3D 6A 32 01 3D 6A 32 01 09 00 00 00 0A 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 21 A6 1B 00 42 21 9A 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 B4 00 00 00 FF FF FF FF 0F 00 00 00 3D 6A 32 01 3D 6A 32 01 0B 00 00 00 0D 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 84 5C 10 00 43 21 9A 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 37 00 00 00 FF FF FF FF 0F 00 00 00 3D 6A 32 01 3D 6A 32 01 0B 00 00 00 0D 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 98 34 10 00 44 21 9A 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 46 00 00 00 FF FF FF FF 0F 00 00 00 3D 6A 32 01 3D 6A 32 01 0B 00 00 00 0D 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 55 98 0F 00 45 21 9A 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 37 00 00 00 FF FF FF FF 0F 00 00 00 3D 6A 32 01 3D 6A 32 01 0B 00 00 00 0D 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 75 83 10 00 46 21 9A 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 37 00 00 00 FF FF FF FF 0F 00 00 00 3D 6A 32 01 3D 6A 32 01 0B 00 00 00 0D 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 18 0A 10 00 47 21 9A 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 2D 00 00 00 FF FF FF FF 0F 00 00 00 3D 6A 32 01 3D 6A 32 01 0B 00 00 00 0D 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 0B 31 10 00 48 21 9A 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 46 00 00 00 FF FF FF FF 0F 00 00 00 3D 6A 32 01 3D 6A 32 01 0B 00 00 00 0D 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 61 72 4C 00 49 21 9A 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 37 00 00 00 FF FF FF FF 0F 00 00 00 3D 6A 32 01 3D 6A 32 01 0B 00 00 00 0D 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 4B BC 4E 00 4A 21 9A 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 50 00 00 00 FF FF FF FF 0F 00 00 00 3D 6A 32 01 3D 6A 32 01 0B 00 00 00 0D 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 39 70 4D 00 4B 21 9A 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 37 00 00 00 FF FF FF FF 0F 00 00 00 3D 6A 32 01 3D 6A 32 01 0B 00 00 00 0D 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 10 47 4E 00 4C 21 9A 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 C8 00 00 00 FF FF FF FF 0F 00 00 00 3D 6A 32 01 3D 6A 32 01 0D 00 00 00 0F 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 B3 E6 0F 00 4D 21 9A 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 37 00 00 00 FF FF FF FF 0F 00 00 00 3D 6A 32 01 3D 6A 32 01 0D 00 00 00 0F 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 73 34 10 00 4E 21 9A 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 46 00 00 00 FF FF FF FF 0F 00 00 00 3D 6A 32 01 3D 6A 32 01 0D 00 00 00 0F 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 5C 98 0F 00 4F 21 9A 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 2D 00 00 00 FF FF FF FF 0F 00 00 00 3D 6A 32 01 3D 6A 32 01 0D 00 00 00 0F 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 DF 82 10 00 50 21 9A 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 55 00 00 00 FF FF FF FF 0F 00 00 00 3D 6A 32 01 3D 6A 32 01 0D 00 00 00 0F 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 57 4B 4C 00 51 21 9A 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 19 00 00 00 FF FF FF FF 0F 00 00 00 3D 6A 32 01 3D 6A 32 01 0D 00 00 00 0F 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 B8 34 10 00 52 21 9A 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 28 00 00 00 FF FF FF FF 0F 00 00 00 3D 6A 32 01 3D 6A 32 01 0D 00 00 00 0F 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 48 94 0F 00 53 21 9A 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 2D 00 00 00 FF FF FF FF 0F 00 00 00 3D 6A 32 01 3D 6A 32 01 0D 00 00 00 0F 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 A9 7E 10 00 54 21 9A 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 2D 00 00 00 FF FF FF FF 0F 00 00 00 3D 6A 32 01 3D 6A 32 01 0D 00 00 00 0F 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 96 0D 10 00 55 21 9A 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 37 00 00 00 FF FF FF FF 0F 00 00 00 3D 6A 32 01 3D 6A 32 01 0D 00 00 00 0F 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 3C 95 4E 00 56 21 9A 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 55 00 00 00 FF FF FF FF 0F 00 00 00 3D 6A 32 01 3D 6A 32 01 10 00 00 00 12 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 E9 F8 19 00 57 21 9A 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 50 00 00 00 FF FF FF FF 0F 00 00 00 3D 6A 32 01 3D 6A 32 01 10 00 00 00 12 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 1C D1 10 00 58 21 9A 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 46 00 00 00 FF FF FF FF 0F 00 00 00 3D 6A 32 01 3D 6A 32 01 10 00 00 00 12 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 3F 71 0F 00 59 21 9A 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 55 00 00 00 FF FF FF FF 0F 00 00 00 3D 6A 32 01 3D 6A 32 01 10 00 00 00 12 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 F5 4B 0F 00 5A 21 9A 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 55 00 00 00 FF FF FF FF 0F 00 00 00 3D 6A 32 01 3D 6A 32 01 10 00 00 00 12 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 B9 82 10 00 5B 21 9A 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 28 00 00 00 FF FF FF FF 0F 00 00 00 3D 6A 32 01 3D 6A 32 01 10 00 00 00 12 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 B7 D0 10 00 5C 21 9A 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 2D 00 00 00 FF FF FF FF 0F 00 00 00 3D 6A 32 01 3D 6A 32 01 10 00 00 00 12 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 37 98 0F 00 5D 21 9A 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 37 00 00 00 FF FF FF FF 0F 00 00 00 3D 6A 32 01 3D 6A 32 01 10 00 00 00 12 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 51 4B 4C 00 5E 21 9A 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 0F 00 00 00 FF FF FF FF 0F 00 00 00 3D 6A 32 01 3D 6A 32 01 10 00 00 00 12 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 C0 DE 0F 00 5F 21 9A 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 28 00 00 00 FF FF FF FF 0F 00 00 00 3D 6A 32 01 3D 6A 32 01 10 00 00 00 12 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 50 E3 4E 00 60 21 9A 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 B4 00 00 00 FF FF FF FF 0F 00 00 00 3D 6A 32 01 3D 6A 32 01 12 00 00 00 14 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 D0 27 4E 00 61 21 9A 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 37 00 00 00 FF FF FF FF 0F 00 00 00 3D 6A 32 01 3D 6A 32 01 12 00 00 00 14 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 E8 94 50 00 62 21 9A 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 55 00 00 00 FF FF FF FF 0F 00 00 00 3D 6A 32 01 3D 6A 32 01 12 00 00 00 14 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 26 71 0F 00 63 21 9A 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 37 00 00 00 FF FF FF FF 0F 00 00 00 3D 6A 32 01 3D 6A 32 01 12 00 00 00 14 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 B1 3E 52 00 64 21 9A 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 55 00 00 00 FF FF FF FF 0F 00 00 00 3D 6A 32 01 3D 6A 32 01 12 00 00 00 14 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 D6 D0 10 00 65 21 9A 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 37 00 00 00 FF FF FF FF 0F 00 00 00 3D 6A 32 01 3D 6A 32 01 12 00 00 00 14 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 6B 71 0F 00 66 21 9A 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 28 00 00 00 FF FF FF FF 0F 00 00 00 3D 6A 32 01 3D 6A 32 01 12 00 00 00 14 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 71 4B 0F 00 67 21 9A 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 28 00 00 00 FF FF FF FF 0F 00 00 00 3D 6A 32 01 3D 6A 32 01 12 00 00 00 14 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 8B F8 10 00 68 21 9A 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 37 00 00 00 FF FF FF FF 0F 00 00 00 3D 6A 32 01 3D 6A 32 01 12 00 00 00 14 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 A9 F8 19 00 69 21 9A 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 19 00 00 00 FF FF FF FF 0F 00 00 00 3D 6A 32 01 3D 6A 32 01 12 00 00 00 14 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 89 83 4F 00 6A 21 9A 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 BE 00 00 00 FF FF FF FF 0F 00 00 00 E9 69 32 01 E9 69 32 01 08 00 00 00 0A 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 C5 F7 10 00 6B 21 9A 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 5F 00 00 00 FF FF FF FF 0F 00 00 00 E9 69 32 01 E9 69 32 01 08 00 00 00 0A 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 02 20 4E 00 6C 21 9A 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 37 00 00 00 FF FF FF FF 0F 00 00 00 E9 69 32 01 E9 69 32 01 08 00 00 00 0A 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 F0 F8 4D 00 6D 21 9A 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 28 00 00 00 FF FF FF FF 0F 00 00 00 E9 69 32 01 E9 69 32 01 08 00 00 00 0A 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 43 BC 4E 00 6E 21 9A 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 2D 00 00 00 FF FF FF FF 0F 00 00 00 E9 69 32 01 E9 69 32 01 08 00 00 00 0A 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 10 31 10 00 6F 21 9A 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 19 00 00 00 FF FF FF FF 0F 00 00 00 E9 69 32 01 E9 69 32 01 08 00 00 00 0A 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 71 72 4C 00 70 21 9A 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 19 00 00 00 FF FF FF FF 0F 00 00 00 E9 69 32 01 E9 69 32 01 08 00 00 00 0A 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 28 71 0F 00 71 21 9A 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 19 00 00 00 FF FF FF FF 0F 00 00 00 E9 69 32 01 E9 69 32 01 08 00 00 00 0A 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 A9 57 10 00 72 21 9A 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 28 00 00 00 FF FF FF FF 0F 00 00 00 E9 69 32 01 E9 69 32 01 08 00 00 00 0A 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 08 20 4E 00 73 21 9A 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 28 00 00 00 FF FF FF FF 0F 00 00 00 E9 69 32 01 E9 69 32 01 08 00 00 00 0A 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 46 4B 4C 00 74 21 9A 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 28 00 00 00 FF FF FF FF 0F 00 00 00 E9 69 32 01 E9 69 32 01 0B 00 00 00 0D 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 6A 5C 10 00 75 21 9A 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 55 00 00 00 FF FF FF FF 0F 00 00 00 E9 69 32 01 E9 69 32 01 0B 00 00 00 0D 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 AB 34 10 00 76 21 9A 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 37 00 00 00 FF FF FF FF 0F 00 00 00 E9 69 32 01 E9 69 32 01 0B 00 00 00 0D 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 E9 94 50 00 77 21 9A 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 55 00 00 00 FF FF FF FF 0F 00 00 00 E9 69 32 01 E9 69 32 01 0B 00 00 00 0D 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 47 BC 4E 00 78 21 9A 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 50 00 00 00 FF FF FF FF 0F 00 00 00 E9 69 32 01 E9 69 32 01 0B 00 00 00 0D 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 CA 2C 10 00 79 21 9A 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 28 00 00 00 FF FF FF FF 0F 00 00 00 E9 69 32 01 E9 69 32 01 0B 00 00 00 0D 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 60 72 4C 00 7A 21 9A 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 28 00 00 00 FF FF FF FF 0F 00 00 00 E9 69 32 01 E9 69 32 01 0B 00 00 00 0D 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 4A BC 4E 00 7B 21 9A 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 55 00 00 00 FF FF FF FF 0F 00 00 00 E9 69 32 01 E9 69 32 01 0B 00 00 00 0D 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 38 70 4D 00 7C 21 9A 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 28 00 00 00 FF FF FF FF 0F 00 00 00 E9 69 32 01 E9 69 32 01 0B 00 00 00 0D 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 7A C0 4C 00 7D 21 9A 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 19 00 00 00 FF FF FF FF 0F 00 00 00 E9 69 32 01 E9 69 32 01 0B 00 00 00 0D 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 B0 CD 4F 00 7E 21 9A 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 AA 00 00 00 FF FF FF FF 0F 00 00 00 E9 69 32 01 E9 69 32 01 0D 00 00 00 0F 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 D1 E6 0F 00 7F 21 9A 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 55 00 00 00 FF FF FF FF 0F 00 00 00 E9 69 32 01 E9 69 32 01 0D 00 00 00 0F 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 CB 34 10 00 80 21 9A 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 37 00 00 00 FF FF FF FF 0F 00 00 00 E9 69 32 01 E9 69 32 01 0D 00 00 00 0F 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 40 98 0F 00 81 21 9A 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 28 00 00 00 FF FF FF FF 0F 00 00 00 E9 69 32 01 E9 69 32 01 0D 00 00 00 0F 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 32 83 10 00 82 21 9A 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 37 00 00 00 FF FF FF FF 0F 00 00 00 E9 69 32 01 E9 69 32 01 0D 00 00 00 0F 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 90 0D 10 00 83 21 9A 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 46 00 00 00 FF FF FF FF 0F 00 00 00 E9 69 32 01 E9 69 32 01 0D 00 00 00 0F 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 C3 2C 10 00 84 21 9A 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 19 00 00 00 FF FF FF FF 0F 00 00 00 E9 69 32 01 E9 69 32 01 0D 00 00 00 0F 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 42 98 0F 00 85 21 9A 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 28 00 00 00 FF FF FF FF 0F 00 00 00 E9 69 32 01 E9 69 32 01 0D 00 00 00 0F 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 FD 09 10 00 86 21 9A 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 19 00 00 00 FF FF FF FF 0F 00 00 00 E9 69 32 01 E9 69 32 01 0D 00 00 00 0F 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 B5 DE 0F 00 87 21 9A 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 19 00 00 00 FF FF FF FF 0F 00 00 00 E9 69 32 01 E9 69 32 01 0D 00 00 00 0F 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 24 A6 1B 00 88 21 9A 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 B4 00 00 00 FF FF FF FF 0F 00 00 00 E9 69 32 01 E9 69 32 01 0F 00 00 00 11 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 62 E6 0F 00 89 21 9A 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 46 00 00 00 FF FF FF FF 0F 00 00 00 E9 69 32 01 E9 69 32 01 0F 00 00 00 11 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 EF D0 10 00 8A 21 9A 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 37 00 00 00 FF FF FF FF 0F 00 00 00 E9 69 32 01 E9 69 32 01 0F 00 00 00 11 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 5B 98 0F 00 8B 21 9A 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 55 00 00 00 FF FF FF FF 0F 00 00 00 E9 69 32 01 E9 69 32 01 0F 00 00 00 11 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 CA 4A 0F 00 8C 21 9A 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 55 00 00 00 FF FF FF FF 0F 00 00 00 E9 69 32 01 E9 69 32 01 0F 00 00 00 11 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 DC D0 10 00 8D 21 9A 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 28 00 00 00 FF FF FF FF 0F 00 00 00 E9 69 32 01 E9 69 32 01 0F 00 00 00 11 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 4E 98 0F 00 8E 21 9A 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 19 00 00 00 FF FF FF FF 0F 00 00 00 E9 69 32 01 E9 69 32 01 0F 00 00 00 11 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 3C 46 0F 00 8F 21 9A 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 19 00 00 00 FF FF FF FF 0F 00 00 00 E9 69 32 01 E9 69 32 01 0F 00 00 00 11 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 4A 4B 4C 00 90 21 9A 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 19 00 00 00 FF FF FF FF 0F 00 00 00 E9 69 32 01 E9 69 32 01 0F 00 00 00 11 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 9D E6 0F 00 91 21 9A 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 28 00 00 00 FF FF FF FF 0F 00 00 00 E9 69 32 01 E9 69 32 01 0F 00 00 00 11 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 4B 4B 4C 00 92 21 9A 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 19 00 00 00 FF FF FF FF 0F 00 00 00 E9 69 32 01 E9 69 32 01 11 00 00 00 12 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 D3 F8 19 00 93 21 9A 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 50 00 00 00 FF FF FF FF 0F 00 00 00 E9 69 32 01 E9 69 32 01 11 00 00 00 12 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 F2 D0 10 00 94 21 9A 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 37 00 00 00 FF FF FF FF 0F 00 00 00 E9 69 32 01 E9 69 32 01 11 00 00 00 12 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 3C 71 0F 00 95 21 9A 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 37 00 00 00 FF FF FF FF 0F 00 00 00 E9 69 32 01 E9 69 32 01 11 00 00 00 12 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 67 4B 0F 00 96 21 9A 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 37 00 00 00 FF FF FF FF 0F 00 00 00 E9 69 32 01 E9 69 32 01 11 00 00 00 12 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 EE D0 10 00 97 21 9A 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 28 00 00 00 FF FF FF FF 0F 00 00 00 E9 69 32 01 E9 69 32 01 11 00 00 00 12 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 58 71 0F 00 98 21 9A 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 28 00 00 00 FF FF FF FF 0F 00 00 00 E9 69 32 01 E9 69 32 01 11 00 00 00 12 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 45 4C 0F 00 99 21 9A 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 19 00 00 00 FF FF FF FF 0F 00 00 00 E9 69 32 01 E9 69 32 01 11 00 00 00 12 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 8E F8 10 00 9A 21 9A 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 2D 00 00 00 FF FF FF FF 0F 00 00 00 E9 69 32 01 E9 69 32 01 11 00 00 00 12 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 95 F8 19 00 9B 21 9A 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 19 00 00 00 FF FF FF FF 0F 00 00 00 E9 69 32 01 E9 69 32 01 11 00 00 00 12 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 3A 95 4E 00 9C 21 9A 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 37 00 00 00 FF FF FF FF 0F 00 00 00 EA 69 32 01 EA 69 32 01 08 00 00 00 0A 00 00 00 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 09 20 4E 00 9D 21 9A 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 55 00 00 00 FF FF FF FF 0F 00 00 00 EA 69 32 01 EA 69 32 01 08 00 00 00 0A 00 00 00 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 46 BC 4E 00 9E 21 9A 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 50 00 00 00 FF FF FF FF 0F 00 00 00 EA 69 32 01 EA 69 32 01 08 00 00 00 0A 00 00 00 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 1F 0A 10 00 9F 21 9A 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 28 00 00 00 FF FF FF FF 0F 00 00 00 EA 69 32 01 EA 69 32 01 08 00 00 00 0A 00 00 00 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 0D 31 10 00 A0 21 9A 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 19 00 00 00 FF FF FF FF 0F 00 00 00 EA 69 32 01 EA 69 32 01 08 00 00 00 0A 00 00 00 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 7A 72 4C 00 A1 21 9A 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 28 00 00 00 FF FF FF FF 0F 00 00 00 EA 69 32 01 EA 69 32 01 08 00 00 00 0A 00 00 00 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 7A 71 0F 00 A2 21 9A 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 28 00 00 00 FF FF FF FF 0F 00 00 00 EA 69 32 01 EA 69 32 01 08 00 00 00 0A 00 00 00 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 3D 70 4D 00 A3 21 9A 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 37 00 00 00 FF FF FF FF 0F 00 00 00 EA 69 32 01 EA 69 32 01 08 00 00 00 0A 00 00 00 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 77 5C 10 00 A4 21 9A 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 19 00 00 00 FF FF FF FF 0F 00 00 00 EA 69 32 01 EA 69 32 01 08 00 00 00 0A 00 00 00 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 F9 23 4E 00 A5 21 9A 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 37 00 00 00 FF FF FF FF 0F 00 00 00 EA 69 32 01 EA 69 32 01 08 00 00 00 0A 00 00 00 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 10 47 4E 00 A6 21 9A 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 BE 00 00 00 FF FF FF FF 0F 00 00 00 EA 69 32 01 EA 69 32 01 0B 00 00 00 0D 00 00 00 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 19 5C 10 00 A7 21 9A 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 55 00 00 00 FF FF FF FF 0F 00 00 00 EA 69 32 01 EA 69 32 01 0B 00 00 00 0D 00 00 00 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 7F 34 10 00 A8 21 9A 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 37 00 00 00 FF FF FF FF 0F 00 00 00 EA 69 32 01 EA 69 32 01 0B 00 00 00 0D 00 00 00 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 34 98 0F 00 A9 21 9A 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 37 00 00 00 FF FF FF FF 0F 00 00 00 EA 69 32 01 EA 69 32 01 0B 00 00 00 0D 00 00 00 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 11 31 10 00 AA 21 9A 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 19 00 00 00 FF FF FF FF 0F 00 00 00 EA 69 32 01 EA 69 32 01 0B 00 00 00 0D 00 00 00 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 57 98 0F 00 AB 21 9A 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 28 00 00 00 FF FF FF FF 0F 00 00 00 EA 69 32 01 EA 69 32 01 0B 00 00 00 0D 00 00 00 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 45 BC 4E 00 AC 21 9A 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 55 00 00 00 FF FF FF FF 0F 00 00 00 EA 69 32 01 EA 69 32 01 0B 00 00 00 0D 00 00 00 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 FF 09 10 00 AD 21 9A 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 28 00 00 00 FF FF FF FF 0F 00 00 00 EA 69 32 01 EA 69 32 01 0B 00 00 00 0D 00 00 00 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 AD DE 0F 00 AE 21 9A 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 19 00 00 00 FF FF FF FF 0F 00 00 00 EA 69 32 01 EA 69 32 01 0B 00 00 00 0D 00 00 00 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 70 4B 0F 00 AF 21 9A 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 19 00 00 00 FF FF FF FF 0F 00 00 00 EA 69 32 01 EA 69 32 01 0B 00 00 00 0D 00 00 00 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 80 64 4D 00 B0 21 9A 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 F0 00 00 00 FF FF FF FF 0F 00 00 00 EA 69 32 01 EA 69 32 01 0D 00 00 00 0F 00 00 00 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 AF E6 0F 00 B1 21 9A 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 37 00 00 00 FF FF FF FF 0F 00 00 00 EA 69 32 01 EA 69 32 01 0D 00 00 00 0F 00 00 00 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 98 34 10 00 B2 21 9A 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 55 00 00 00 FF FF FF FF 0F 00 00 00 EA 69 32 01 EA 69 32 01 0D 00 00 00 0F 00 00 00 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 31 98 0F 00 B3 21 9A 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 37 00 00 00 FF FF FF FF 0F 00 00 00 EA 69 32 01 EA 69 32 01 0D 00 00 00 0F 00 00 00 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 77 83 10 00 B4 21 9A 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 55 00 00 00 FF FF FF FF 0F 00 00 00 EA 69 32 01 EA 69 32 01 0D 00 00 00 0F 00 00 00 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 1B D1 10 00 B5 21 9A 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 28 00 00 00 FF FF FF FF 0F 00 00 00 EA 69 32 01 EA 69 32 01 0D 00 00 00 0F 00 00 00 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 58 98 0F 00 B6 21 9A 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 19 00 00 00 FF FF FF FF 0F 00 00 00 EA 69 32 01 EA 69 32 01 0D 00 00 00 0F 00 00 00 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 DE 82 10 00 B7 21 9A 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 19 00 00 00 FF FF FF FF 0F 00 00 00 EA 69 32 01 EA 69 32 01 0D 00 00 00 0F 00 00 00 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 FE 09 10 00 B8 21 9A 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 19 00 00 00 FF FF FF FF 0F 00 00 00 EA 69 32 01 EA 69 32 01 0D 00 00 00 0F 00 00 00 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 8A E6 0F 00 B9 21 9A 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 28 00 00 00 FF FF FF FF 0F 00 00 00 EA 69 32 01 EA 69 32 01 0D 00 00 00 0F 00 00 00 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 20 A6 1B 00 BA 21 9A 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 BE 00 00 00 FF FF FF FF 0F 00 00 00 EA 69 32 01 EA 69 32 01 0F 00 00 00 11 00 00 00 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 24 F9 19 00 BB 21 9A 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 28 00 00 00 FF FF FF FF 0F 00 00 00 EA 69 32 01 EA 69 32 01 0F 00 00 00 11 00 00 00 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 D7 D0 10 00 BC 21 9A 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 37 00 00 00 FF FF FF FF 0F 00 00 00 EA 69 32 01 EA 69 32 01 0F 00 00 00 11 00 00 00 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 57 71 0F 00 BD 21 9A 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 37 00 00 00 FF FF FF FF 0F 00 00 00 EA 69 32 01 EA 69 32 01 0F 00 00 00 11 00 00 00 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 EC 4A 0F 00 BE 21 9A 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 28 00 00 00 FF FF FF FF 0F 00 00 00 EA 69 32 01 EA 69 32 01 0F 00 00 00 11 00 00 00 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 E3 D0 10 00 BF 21 9A 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 28 00 00 00 FF FF FF FF 0F 00 00 00 EA 69 32 01 EA 69 32 01 0F 00 00 00 11 00 00 00 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 64 98 0F 00 C0 21 9A 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 28 00 00 00 FF FF FF FF 0F 00 00 00 EA 69 32 01 EA 69 32 01 0F 00 00 00 11 00 00 00 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 F3 4A 0F 00 C1 21 9A 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 19 00 00 00 FF FF FF FF 0F 00 00 00 EA 69 32 01 EA 69 32 01 0F 00 00 00 11 00 00 00 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 A9 F8 10 00 C2 21 9A 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 37 00 00 00 FF FF FF FF 0F 00 00 00 EA 69 32 01 EA 69 32 01 0F 00 00 00 11 00 00 00 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 8D E6 0F 00 C3 21 9A 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 28 00 00 00 FF FF FF FF 0F 00 00 00 EA 69 32 01 EA 69 32 01 0F 00 00 00 11 00 00 00 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 89 83 4F 00 C4 21 9A 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 8C 00 00 00 FF FF FF FF 0F 00 00 00 EA 69 32 01 EA 69 32 01 11 00 00 00 13 00 00 00 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 28 F9 19 00 C5 21 9A 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 37 00 00 00 FF FF FF FF 0F 00 00 00 EA 69 32 01 EA 69 32 01 11 00 00 00 13 00 00 00 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 20 D1 10 00 C6 21 9A 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 55 00 00 00 FF FF FF FF 0F 00 00 00 EA 69 32 01 EA 69 32 01 11 00 00 00 13 00 00 00 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 58 BF 0F 00 C7 21 9A 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 55 00 00 00 FF FF FF FF 0F 00 00 00 EA 69 32 01 EA 69 32 01 11 00 00 00 13 00 00 00 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 B0 3E 52 00 C8 21 9A 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 46 00 00 00 FF FF FF FF 0F 00 00 00 EA 69 32 01 EA 69 32 01 11 00 00 00 13 00 00 00 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 E2 D0 10 00 C9 21 9A 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 28 00 00 00 FF FF FF FF 0F 00 00 00 EA 69 32 01 EA 69 32 01 11 00 00 00 13 00 00 00 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 4F 71 0F 00 CA 21 9A 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 28 00 00 00 FF FF FF FF 0F 00 00 00 EA 69 32 01 EA 69 32 01 11 00 00 00 13 00 00 00 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 77 4B 0F 00 CB 21 9A 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 19 00 00 00 FF FF FF FF 0F 00 00 00 EA 69 32 01 EA 69 32 01 11 00 00 00 13 00 00 00 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 2A F8 10 00 CC 21 9A 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 37 00 00 00 FF FF FF FF 0F 00 00 00 EA 69 32 01 EA 69 32 01 11 00 00 00 13 00 00 00 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 13 F9 19 00 CD 21 9A 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 19 00 00 00 FF FF FF FF 0F 00 00 00 EA 69 32 01 EA 69 32 01 11 00 00 00 13 00 00 00 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 27 00 00 00"));

		return lew.getBytes();
	}

	public static byte[] writeEnterMts(Player p) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter();

		lew.writeShort(ClientSendOps.MTS_OPEN);
		writeCharData(lew, p);
		lew.writeLengthPrefixedString(p.getClient().getAccountName());
		lew.writeInt(5000);
		lew.write(HexTool.getByteArrayFromHexString("0A 00 00 00 64 00 00 00 18 00 00 00 A8 00 00 00 B0 ED 4E 3C FD 68 C9 01"));

		return lew.getBytes();
	}*/

	public static byte[] writeShowHide() {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(4);

		lew.writeShort(ClientSendOps.GM);
		lew.writeByte(PacketSubHeaders.GM_HIDE);
		lew.writeBool(true);

		return lew.getBytes();
	}

	public static byte[] writeStopHide() {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(4);

		lew.writeShort(ClientSendOps.GM);
		lew.writeByte(PacketSubHeaders.GM_HIDE);
		lew.writeBool(false);

		return lew.getBytes();
	}
}
