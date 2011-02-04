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
import argonms.character.PlayerJob;
import argonms.character.inventory.Inventory;
import argonms.character.inventory.Inventory.InventoryType;
import argonms.character.inventory.InventorySlot;
import argonms.character.inventory.Pet;
import argonms.tools.output.LittleEndianWriter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 *
 * @author GoldenKevin
 */
public class CommonPackets {
	public static void writeCharEntry(LittleEndianWriter lew, Player p) {
		writeCharStats(lew, p);
		writeAvatar(lew, p, false);
		//if (!PlayerJob.isModerator(p.getJob())) {
			//lew.writeBool(true);
			//lew.writeInt(p.getRank());
			//lew.writeInt(p.getRankMove());
			//lew.writeInt(p.getJobRank());
			//lew.writeInt(p.getJobRankMove());
		//} else {
			lew.writeBool(false);
		//}
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
			lew.writeInt(cWeapon == null ? 0 : cWeapon.getItemId());
		}
		lew.writeInt(0);
		lew.writeLong(0);
	}
}
