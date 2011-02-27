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

package argonms.net.client;

import argonms.character.Player;
import argonms.character.skill.SkillLevel;
import argonms.character.skill.SkillTools;
import argonms.character.inventory.Equip;
import argonms.character.inventory.Inventory;
import argonms.character.inventory.Inventory.InventoryType;
import argonms.character.inventory.InventorySlot;
import argonms.character.inventory.InventorySlot.ItemType;
import argonms.character.inventory.InventoryTools;
import argonms.character.inventory.Pet;
import argonms.character.inventory.Ring;
import argonms.character.inventory.TamingMob;
import argonms.character.skill.Cooldown;
import argonms.map.movement.LifeMovementFragment;
import argonms.map.entity.ItemDrop;
import argonms.map.entity.Mob;
import argonms.map.entity.Npc;
import argonms.tools.HexTool;
import argonms.tools.TimeUtil;
import argonms.tools.output.LittleEndianByteArrayWriter;
import argonms.tools.output.LittleEndianWriter;
import java.awt.Point;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.TreeMap;

/**
 *
 * @author GoldenKevin
 */
public class CommonPackets {
	public static final Random RNG = new Random();

	private static final int[] ROCK_MAPS = {
		999999999, 999999999, 999999999, 999999999, 999999999
	};

	private static final int[] VIP_MAPS = {
		999999999, 999999999, 999999999, 999999999, 999999999,
		999999999, 999999999, 999999999, 999999999, 999999999,
		999999999, 999999999, 999999999, 999999999, 999999999
	};

	/**
	 * Append item expiration time info to an existing LittleEndianWriter.
	 *
	 * @param lew The LittleEndianWriter to write to.
	 * @param time The expiration time.
	 * @param show Show the expiration time.
	 */
	private static void addItemExpire(LittleEndianWriter lew, long time, boolean show) {
		if (!show || time <= 0) //Midnight January 1, 2079 = no expiration
			time = 3439756800000L; //some arbitrary date Wizet made up...
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

	public static void writeAvatar(LittleEndianWriter lew, Player p, boolean messenger) {
		lew.writeByte(p.getGender());
		lew.writeByte(p.getSkinColor());
		lew.writeInt(p.getEyes());
		lew.writeBool(!messenger);
		lew.writeInt(p.getHair()); // hair
		Inventory equip = p.getInventory(InventoryType.EQUIPPED);
		Map<Byte, Integer> myEquip = new LinkedHashMap<Byte, Integer>();
		Map<Byte, Integer> maskedEquip = new LinkedHashMap<Byte, Integer>();
		synchronized (equip) {
			for (Entry<Short, InventorySlot> ent : equip.getAll().entrySet()) {
				byte pos = (byte) (ent.getKey().shortValue() * -1);
				if (pos < 100 && myEquip.get(pos) == null) {
					myEquip.put(pos, ent.getValue().getItemId());
				} else if (pos > 100 && pos != 111) {
					pos -= 100;
					if (myEquip.get(pos) != null)
						maskedEquip.put(pos, myEquip.get(pos));
					myEquip.put(pos, ent.getValue().getItemId());
				} else if (myEquip.get(pos) != null) {
					maskedEquip.put(pos, ent.getValue().getItemId());
				}
			}

			for (Entry<Byte, Integer> entry : myEquip.entrySet()) {
				lew.writeByte(entry.getKey().byteValue());
				lew.writeInt(entry.getValue().intValue());
			}
			lew.writeByte((byte) 0xFF);

			for (Entry<Byte, Integer> entry : maskedEquip.entrySet()) {
				lew.writeByte(entry.getKey().byteValue());
				lew.writeInt(entry.getValue().intValue());
			}
			lew.writeByte((byte) 0xFF);

			InventorySlot cWeapon = equip.get((short) 111);
			lew.writeInt(cWeapon != null ? cWeapon.getItemId() : 0);
		}
		Pet[] pets = p.getPets();
		for (int i = 0; i < 3; i++)
			lew.writeInt(pets[i] == null ? 0 : pets[i].getItemId());
	}

	private static void addItemInfo(LittleEndianWriter lew, short pos,
			InventorySlot item) {
		addItemInfo(lew, pos, item, true, false, false);
	}

	//it seems as though Vana's PlayerPacketHelper::addItemInfo is much simpler
	private static void addItemInfo(LittleEndianWriter lew, short pos,
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
		lew.writeInt(item.getItemId());
		lew.writeBool(cashItem);
		if (cashItem)
			lew.writeLong(item.getUniqueId());
		addItemExpire(lew, item.getExpiration(), showExpire);

		if (item.getType() == ItemType.PET) {
			Pet pet = (Pet) item;
			lew.writePaddedAsciiString(pet.getName(), 13);
			lew.writeByte(pet.getLevel());
			lew.writeShort(pet.getCloseness());
			lew.writeByte(pet.getFullness());
			//00 B8 D5 60 00 CE C8 01
			addItemExpire(lew, item.getExpiration(), showExpire); //again?
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
					lew.writeBytes(HexTool.getByteArrayFromHexString("40 E0 FD 3B 37 4F 01"));
					lew.writeInt(-1);
				}
			}
		} else {
			lew.writeShort(item.getQuantity());
			lew.writeLengthPrefixedString(item.getOwner());
			lew.writeShort(item.getFlag());
			if (InventoryTools.isThrowingStar(item.getItemId())
					|| InventoryTools.isBullet(item.getItemId())) {
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
				addItemInfo(lew, entry.getKey().shortValue(), item);
				if (item.getType() == ItemType.RING)
					rings.put(entry.getKey(), (Ring) item);
			}
		}
		lew.writeByte((byte) 0); //end of visible equipped
		lew.writeByte((byte) 0); //end of masked equipped

		iv = p.getInventory(InventoryType.EQUIP);
		synchronized (iv) {
			for (Entry<Short, InventorySlot> entry : iv.getAll().entrySet())
				addItemInfo(lew, entry.getKey().shortValue(), entry.getValue());
		}
		lew.writeByte((byte) 0); //end of equip inventory

		iv = p.getInventory(InventoryType.USE);
		synchronized (iv) {
			for (Entry<Short, InventorySlot> entry : iv.getAll().entrySet())
				addItemInfo(lew, entry.getKey().shortValue(), entry.getValue());
		}
		lew.writeByte((byte) 0); //end of consume inventory

		iv = p.getInventory(InventoryType.SETUP);
		synchronized (iv) {
			for (Entry<Short, InventorySlot> entry : iv.getAll().entrySet())
				addItemInfo(lew, entry.getKey().shortValue(), entry.getValue());
		}
		lew.writeByte((byte) 0); //end of install inventory

		iv = p.getInventory(InventoryType.ETC);
		synchronized (iv) {
			for (Entry<Short, InventorySlot> entry : iv.getAll().entrySet())
				addItemInfo(lew, entry.getKey().shortValue(), entry.getValue());
		}
		lew.writeByte((byte) 0); //end of etc inventory

		iv = p.getInventory(InventoryType.CASH);
		synchronized (iv) {
			for (Entry<Short, InventorySlot> entry : iv.getAll().entrySet())
				addItemInfo(lew, entry.getKey().shortValue(), entry.getValue());
		}
		lew.writeByte((byte) 0); //end of cash inventory

		Map<Integer, SkillLevel> skills = p.getSkills();
		lew.writeShort((short) skills.size());
		for (Entry<Integer, SkillLevel> entry : skills.entrySet()) {
			int skillid = entry.getKey().intValue();
			SkillLevel skill = entry.getValue();
			lew.writeInt(skillid);
			lew.writeInt(skill.getLevel());
			if (SkillTools.isFourthJob(skillid))
				lew.writeInt(skill.getMasterLevel());
		}
		Collection<Cooldown> cooldowns = p.getCooldowns();
		lew.writeShort((short) cooldowns.size());
		for (Cooldown cooling : cooldowns) {
			lew.writeInt(cooling.getParentSkill());
			lew.writeShort(cooling.getSecondsRemaining());
		}

		lew.writeShort((short) 0); //ah whatever, implement quests later...
		/*List<MapleQuestStatus> started = p.getActiveQuests();
		lew.writeShort((short) started.size());
		for (MapleQuestStatus q : started) {
			lew.writeShort(q.getQuest().getId());
			lew.writeLengthPrefixedString(q);
		}
		List<MapleQuestStatus> completed = p.getCompletedQuests();
		lew.writeShort((short) completed.size());
		for (MapleQuestStatus q : completed) {
			lew.writeShort(q.getQuest().getId());
			lew.writeInt(KoreanDateUtil.getQuestTimestamp(q.getCompletionTime()));
			lew.writeInt(KoreanDateUtil.getQuestTimestamp(q.getCompletionTime()));
		}*/

		//dude, what the fuck was that guy who wrote this smoking?
		boolean FR_last = false;
		//if (!rings.isEmpty())
			lew.writeShort((short) 0);
		for (Ring ring : rings.values()) {
			lew.writeShort((short) 0);
			lew.writeShort((short) 1);
			lew.writeInt(ring.getPartnerCharId());
			lew.writePaddedAsciiString(Player.getNameFromId(ring.getPartnerCharId()), 13);
			lew.writeLong(ring.getUniqueId());
			lew.writeInt((int) ring.getPartnerRingId()); //this is definitely wrong, considering UIDs are 64-bit long
			if (ring.getItemId() >= 1112800 && ring.getItemId() <= 1112803 || ring.getItemId() <= 1112806 || ring.getItemId() <= 1112807 || ring.getItemId() <= 1112809) {
				FR_last = true;
				lew.writeInt(0);
				lew.writeInt(ring.getItemId());
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

	public static byte[] writeInventoryFull() {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(4);

		lew.writeShort(ClientSendOps.MODIFY_INVENTORY_SLOT);
		lew.writeBool(true);
		lew.writeByte((byte) 0);

		return lew.getBytes();
	}

	public static byte[] writeInventorySlotUpdate(InventoryType type, short pos, InventorySlot item, boolean fromDrop, boolean add) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(10);

		lew.writeShort(ClientSendOps.MODIFY_INVENTORY_SLOT);
		lew.writeBool(fromDrop);
		lew.writeByte((byte) 1);
		lew.writeBool(!add);
		lew.writeByte(type.value());
		lew.writeByte((byte) pos);
		if (!add) {
			lew.writeBool(false);
			lew.writeShort(item.getQuantity());
		} else {
			addItemInfo(lew, (short) 0, item);
		}

		return lew.getBytes();
	}

	public static byte[] writeInventoryDropItem(InventoryType type, short src, short quantity) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(10);

		lew.writeShort(ClientSendOps.MODIFY_INVENTORY_SLOT);
		lew.writeBool(true);
		lew.writeByte((byte) 1);
		lew.writeByte((byte) 1);
		lew.writeByte(type.value());
		lew.writeShort(src);
		lew.writeShort(quantity);

		return lew.getBytes();
	}

	public static byte[] writeInventoryMoveItem(InventoryType type, short src, short dst, byte equipIndicator) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(equipIndicator == -1 ? 10 : 11);

		lew.writeShort(ClientSendOps.MODIFY_INVENTORY_SLOT);
		lew.writeBool(true);
		lew.writeByte((byte) 1);
		lew.writeByte((byte) 2);
		lew.writeByte(type.value());
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
		lew.writeByte((byte) 1);
		lew.writeByte((byte) 3);
		lew.writeByte(type.value());
		lew.writeShort(slot);
		if (slot < 0)
			lew.writeBool(true);

		return lew.getBytes();
	}

	public static byte[] writeInventoryMoveItemShiftQuantities(InventoryType type, short src, short srcQty, short dst, short dstQty) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(10);

		lew.writeShort(ClientSendOps.MODIFY_INVENTORY_SLOT);
		lew.writeBool(true);
		lew.writeByte((byte) 2);
		lew.writeByte((byte) 1);
		lew.writeByte(type.value());
		lew.writeShort(src);
		lew.writeShort(srcQty);
		lew.writeBool(true);
		lew.write(type.value());
		lew.writeShort(dst);
		lew.writeShort(dstQty);

		return lew.getBytes();
	}

	public static byte[] writeInventoryMoveItemCombineQuantities(InventoryType type, short src, short dst, short total) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(10);

		lew.writeShort(ClientSendOps.MODIFY_INVENTORY_SLOT);
		lew.writeBool(true);
		lew.writeByte((byte) 2);
		lew.writeByte((byte) 3);
		lew.write(type.value());
		lew.writeShort(src);
		lew.writeBool(true);
		lew.writeByte(type.value());
		lew.writeShort(dst);
		lew.writeShort(total);

		return lew.getBytes();
	}

	private static byte[] writeShowInventoryStatus(byte mode) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(12);
		lew.writeShort(ClientSendOps.SHOW_STATUS_INFO);
		lew.writeBool(false);
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

	public static byte[] writeChangeMap(int mapid, byte spawnPoint, Player p) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter();

		lew.writeShort(ClientSendOps.CHANGE_MAP);
		lew.writeInt(p.getClient().getChannel() - 1);
		lew.writeShort((short) 2);
		lew.writeShort((short) 0);
		lew.writeInt(mapid);
		lew.writeByte(spawnPoint);
		lew.writeShort(p.getHp()); // hp (???)
		lew.writeByte((byte) 0);
		//long questMask = 0x1FFFFFFFFFFFFFFL;
		long questMask = TimeUtil.unixToWindowsTime((long) System.currentTimeMillis());
		lew.writeLong(questMask);

		return lew.getBytes();
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
		lew.writeByte((byte) 0);
		lew.writeByte((byte) 0);
		lew.writeByte((byte) 0);
		lew.writeByte((byte) 0xF8);
		lew.writeByte((byte) 0);
		lew.writeByte((byte) 0);
		lew.writeByte((byte) 0);
		lew.writeByte((byte) 0);
		lew.writeInt(0);
		lew.writeByte((byte) 0);
		lew.writeByte((byte) 0);
		lew.writeInt(0);
		int CHAR_MAGIC_SPAWN = RNG.nextInt();
		lew.writeInt(CHAR_MAGIC_SPAWN);
		lew.writeShort((short) 0);
		lew.writeLong(0);
		lew.writeInt(CHAR_MAGIC_SPAWN);
		lew.writeShort((short) 0);
		lew.writeLong(0);
		lew.writeInt(CHAR_MAGIC_SPAWN);
		lew.writeShort((short) 0);
		/*if (p.getBuffedValue(MapleBuffStat.MONSTER_RIDING) != null) {
			TamingMob mount = p.getEquippedMount();
			if (mount != null) {
				lew.writeInt(mount.getItemId());
				lew.writeInt(mount.getSkillId());
				lew.writeInt(CHAR_MAGIC_SPAWN);
			} else {
				lew.writeInt(1932000);
				lew.writeInt(5221006);
				lew.writeInt(CHAR_MAGIC_SPAWN);
			}
		} else {*/
			lew.writeInt(0);
			lew.writeInt(0);
			lew.writeInt(CHAR_MAGIC_SPAWN);
		//}

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
		Point pos = p.getPosition();
		lew.writeShort((short) pos.x);
		lew.writeShort((short) pos.y);
		lew.writeByte((byte) p.getStance());
		lew.writeShort(p.getFoothold());
		lew.writeByte((byte) 0);
		for (Pet pet : p.getPets()) {
			if (pet != null) {
				lew.writeByte((byte) 1);
				lew.writeInt(pet.getItemId());
				lew.writeLengthPrefixedString(pet.getName());
				lew.writeLong(pet.getUniqueId());
				pos = pet.getPosition();
				lew.writeShort((short) pos.x);
				lew.writeShort((short) pos.y);
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
				lew.writeInt(ring.getItemId());
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
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter();

		lew.writeShort(ClientSendOps.REMOVE_PLAYER);
		lew.writeInt(p.getId());

		return lew.getBytes();
	}

	public static byte[] writeShowPet(Player p, byte slot, Pet pet,
			boolean equip, boolean hunger) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter();

		lew.writeShort(ClientSendOps.SHOW_PET);
		lew.writeInt(p.getId());
		lew.writeByte(slot);
		lew.writeBool(equip);
		lew.writeBool(hunger);
		if (equip) {
			lew.writeInt(pet.getItemId());
			lew.writeLengthPrefixedString(pet.getName());
			lew.writeLong(pet.getUniqueId());
			Point pos = pet.getPosition();
			lew.writeShort((short) pos.x);
			lew.writeShort((short) pos.y);
			lew.writeByte(pet.getStance());
			lew.writeShort(pet.getFoothold());
			lew.writeBool(false); //has name tag
			lew.writeBool(false); //has quote item
		}

		return lew.getBytes();
	}

	private static void writeMonsterData(LittleEndianWriter lew, Mob monster, boolean newSpawn, byte effect) {
		lew.writeInt(monster.getId());
		lew.writeByte((byte) 5);
		lew.writeInt(monster.getMobId());

		//mob status
		lew.writeByte((byte) 0);
		lew.writeShort((short) 0);
		lew.writeByte((byte) 8);
		lew.writeInt(0);

		Point pos = monster.getPosition();
		lew.writeShort((short) pos.x);
		lew.writeShort((short) pos.y);
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

	public static byte[] writeShowMonster(Mob monster, boolean newSpawn, byte effect) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter();
		lew.writeShort(ClientSendOps.SHOW_MONSTER);
		writeMonsterData(lew, monster, newSpawn, effect);
		return lew.getBytes();
	}

	public static byte[] writeRemoveMonster(Mob m, byte animation) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter();
		lew.writeShort(ClientSendOps.REMOVE_MONSTER);
		lew.writeInt(m.getId());
		lew.writeByte(animation);
		return lew.getBytes();
	}

	public static byte[] writeControlMonster(Mob monster, boolean aggro) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter();
		lew.writeShort(ClientSendOps.CONTROL_MONSTER);
		lew.writeByte((byte) (monster.isVisible() ? aggro ? 2 : 1 : 0));
		if (monster.isVisible())
			writeMonsterData(lew, monster, false, (byte) 0);
		else
			lew.writeInt(monster.getId());
		return lew.getBytes();
	}

	public static byte[] writeStopControllingMonster(Mob monster) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(7);
		lew.writeShort(ClientSendOps.CONTROL_MONSTER);
		lew.writeByte((byte) 0);
		lew.writeInt(monster.getId());
		return lew.getBytes();
	}

	public static byte[] writeShowItemDrop(ItemDrop drop, byte animation) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter();

		lew.writeShort(ClientSendOps.SHOW_ITEM_DROP);
		lew.writeByte(animation); // 1 = animation, 2 = none
		lew.writeInt(drop.getId());
		lew.writeByte(drop.getDropType());
		lew.writeInt(drop.getItemId());
		lew.writeInt(drop.getOwner());
		lew.writeByte((byte) 0);
		Point pos = drop.getPosition();
		lew.writeShort((short) pos.x);
		lew.writeShort((short) pos.y);
		if (animation != 2) {
			lew.writeInt(drop.getOwner());
			pos = drop.getSourcePos();
			lew.writeShort((short) pos.x);
			lew.writeShort((short) pos.y);
		} else {
			lew.writeInt(drop.getSourceObjectId());
		}

		lew.writeByte((byte) 0);
		if (animation != 2) {
			lew.writeByte((byte) 0);
			lew.writeByte((byte) 1);
		}

		if (drop.getDropType() != ItemDrop.MESOS) {
			addItemExpire(lew, drop.getItemExpire(), true);
			lew.writeByte((byte) 1);
		}

		return lew.getBytes();
	}

	public static byte[] writeRemoveItemDrop(ItemDrop d, byte animation) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter();

		lew.writeShort(ClientSendOps.REMOVE_ITEM_DROP);
		lew.writeByte(animation);
		lew.writeInt(d.getId());
		if (animation >= 2) {
			lew.writeInt(d.getOwner());
			if (d.getPetSlot() >= 0)
				lew.writeByte(d.getPetSlot());
		}

		return lew.getBytes();
	}

	private static void writeNpcData(LittleEndianWriter lew, Npc npc) {
		lew.writeInt(npc.getId());
		lew.writeInt(npc.getNpcId());
		lew.writeShort((short) npc.getPosition().x);
		lew.writeShort(npc.getCy());
		lew.writeBool(!npc.isF());
		lew.writeShort(npc.getFoothold());
		lew.writeShort(npc.getRx0());
		lew.writeShort(npc.getRx1());
		lew.writeBool(true);
	}

	public static byte[] writeShowNpc(Npc npc) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(22);
		lew.writeShort(ClientSendOps.SHOW_NPC);
		writeNpcData(lew, npc);
		return lew.getBytes();
	}

	public static byte[] writeRemoveNpc(Npc npc) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(6);
		lew.writeShort(ClientSendOps.REMOVE_NPC);
		lew.writeInt(npc.getId());
		return lew.getBytes();
	}

	public static byte[] writeControlNpc(Npc npc) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(22);
		lew.writeShort(ClientSendOps.CONTROL_NPC);
		writeNpcData(lew, npc);
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
}
