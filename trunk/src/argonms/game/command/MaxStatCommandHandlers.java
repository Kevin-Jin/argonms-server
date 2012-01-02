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

package argonms.game.command;

import argonms.common.UserPrivileges;
import argonms.common.character.BuddyList;
import argonms.common.character.PlayerJob;
import argonms.common.character.inventory.Equip;
import argonms.common.character.inventory.Inventory;
import argonms.common.character.inventory.Inventory.InventoryType;
import argonms.common.character.inventory.InventorySlot;
import argonms.game.character.GameCharacter;
import argonms.game.character.inventory.StorageInventory;
import argonms.game.loading.skill.SkillDataLoader;
import argonms.game.net.external.GamePackets;
import java.util.Map.Entry;

/**
 *
 * @author GoldenKevin
 */
public class MaxStatCommandHandlers {
	/**
	 * All skills as of v0.62 (taken from Skill.wz) - if new skills are added in
	 * future versions and you wish to upgrade the server's version, you may
	 * want to edit this so that !maxskills and !maxall maxes those new skills.
	 * Or you can remove some skill IDs if you do not wish to max that skill
	 * with the !maxall or !maxskills commands.
	 */
	private static final int[] SKILLS_TO_MAX = {
		8, 1000, 1001, 1002, 1003, 1004, 1005, 1006, 1000000, 1000001, 1000002,
		1001003, 1001004, 1001005, 1100000, 1100001, 1100002, 1100003, 1101004,
		1101005, 1101006, 1101007, 1110000, 1110001, 1111002, 1111003, 1111004,
		1111005, 1111006, 1111007, 1111008, 1120003, 1120004, 1120005, 1121000,
		1121001, 1121002, 1121006, 1121008, 1121010, 1121011, 1200000, 1200001,
		1200002, 1200003, 1201004, 1201005, 1201006, 1201007, 1210000, 1210001,
		1211002, 1211003, 1211004, 1211005, 1211006, 1211007, 1211008, 1211009,
		1220005, 1220006, 1220010, 1221000, 1221001, 1221002, 1221003, 1221004,
		1221007, 1221009, 1221011, 1221012, 1300000, 1300001, 1300002, 1300003,
		1301004, 1301005, 1301006, 1301007, 1310000, 1311001, 1311002, 1311003,
		1311004, 1311005, 1311006, 1311007, 1311008, 1320005, 1320006, 1320008,
		1320009, 1321000, 1321001, 1321002, 1321003, 1321007, 1321010, 2000000,
		2000001, 2001002, 2001003, 2001004, 2001005, 2100000, 2101001, 2101002,
		2101003, 2101004, 2101005, 2110000, 2110001, 2111002, 2111003, 2111004,
		2111005, 2111006, 2121000, 2121001, 2121002, 2121003, 2121004, 2121005,
		2121006, 2121007, 2121008, 2200000, 2201001, 2201002, 2201003, 2201004,
		2201005, 2210000, 2210001, 2211002, 2211003, 2211004, 2211005, 2211006,
		2221000, 2221001, 2221002, 2221003, 2221004, 2221005, 2221006, 2221007,
		2221008, 2300000, 2301001, 2301002, 2301003, 2301004, 2301005, 2310000,
		2311001, 2311002, 2311003, 2311004, 2311005, 2311006, 2321000, 2321001,
		2321002, 2321003, 2321004, 2321005, 2321006, 2321007, 2321008, 2321009,
		3000000, 3000001, 3000002, 3001003, 3001004, 3001005, 3100000, 3100001,
		3101002, 3101003, 3101004, 3101005, 3110000, 3110001, 3111002, 3111003,
		3111004, 3111005, 3111006, 3120005, 3121000, 3121002, 3121003, 3121004,
		3121006, 3121007, 3121008, 3121009, 3200000, 3200001, 3201002, 3201003,
		3201004, 3201005, 3210000, 3210001, 3211002, 3211003, 3211004, 3211005,
		3211006, 3220004, 3221000, 3221001, 3221002, 3221003, 3221005, 3221006,
		3221007, 3221008, 4000000, 4000001, 4001002, 4001003, 4001334, 4001344,
		4100000, 4100001, 4100002, 4101003, 4101004, 4101005, 4110000, 4111001,
		4111002, 4111003, 4111004, 4111005, 4111006, 4120002, 4120005, 4121000,
		4121003, 4121004, 4121006, 4121007, 4121008, 4121009, 4200000, 4200001,
		4201002, 4201003, 4201004, 4201005, 4210000, 4211001, 4211002, 4211003,
		4211004, 4211005, 4211006, 4220002, 4220005, 4221000, 4221001, 4221003,
		4221004, 4221006, 4221007, 4221008, 5000000, 5001001, 5001002, 5001003,
		5001005, 5100000, 5100001, 5101002, 5101003, 5101004, 5101005, 5101006,
		5101007, 5110000, 5110001, 5111002, 5111004, 5111005, 5111006, 5121000,
		5121001, 5121002, 5121003, 5121004, 5121005, 5121007, 5121008, 5121009,
		5121010, 5200000, 5201001, 5201002, 5201003, 5201004, 5201005, 5201006,
		5210000, 5211001, 5211002, 5211004, 5211005, 5211006, 5220001, 5220002,
		5220011, 5221000, 5221003, 5221004, 5221006, 5221007, 5221008, 5221009,
		5221010, 9001000, 9001001, 9001002, 9101000, 9101001, 9101002, 9101003,
		9101004, 9101005, 9101006, 9101007, 9101008
	};

	private static void maxEquips(GameCharacter p) {
		for (Entry<Short, InventorySlot> item : p.getInventory(InventoryType.EQUIPPED).getAll().entrySet()) {
			Equip e = (Equip) item.getValue();
			e.setStr(Short.MAX_VALUE);
			e.setDex(Short.MAX_VALUE);
			e.setInt(Short.MAX_VALUE);
			e.setLuk(Short.MAX_VALUE);
			e.setHp((short) 30000);
			e.setMp((short) 30000);
			e.setWatk(Short.MAX_VALUE);
			e.setMatk(Short.MAX_VALUE);
			e.setWdef(Short.MAX_VALUE);
			e.setMdef(Short.MAX_VALUE);
			e.setAcc(Short.MAX_VALUE);
			e.setAvoid(Short.MAX_VALUE);
			e.setSpeed((short) 40);
			e.setJump((short) 23);
			p.getClient().getSession().send(GamePackets.writeInventoryUpdateEquipStats(item.getKey().shortValue(), e));
		}
	}

	private static void maxSkills(GameCharacter p) {
		for (int skillid : SKILLS_TO_MAX) {
			byte masterLevel = SkillDataLoader.getInstance().getSkill(skillid).maxLevel();
			p.setSkillLevel(skillid, masterLevel, masterLevel);
		}
	}

	private static void maxStats(GameCharacter p, String[] args) {
		p.setStr(Short.MAX_VALUE);
		p.setDex(Short.MAX_VALUE);
		p.setInt(Short.MAX_VALUE);
		p.setLuk(Short.MAX_VALUE);
		p.setMaxHp((short) 30000);
		p.setHp((short) 30000);
		p.setMaxMp((short) 30000);
		p.setMp((short) 30000);
		p.setLevel((short) 200);
		p.setExp(0);
		p.setJob(PlayerJob.JOB_SUPER_GM);
		p.setFame(Short.MAX_VALUE);

		if (ParseHelper.hasOpt(args, "-i")) {
			p.setMesos(Integer.MAX_VALUE);
			//TODO: find some packets to notify client of new slot limits
			for (InventoryType type : new InventoryType[] { InventoryType.EQUIP, InventoryType.USE, InventoryType.SETUP, InventoryType.ETC, InventoryType.CASH }) {
				Inventory inv = p.getInventory(type);
				inv.increaseCapacity((short) (0xFF - inv.getMaxSlots()));
			}
			StorageInventory inv = p.getStorageInventory();
			inv.increaseCapacity((short) (0xFF - inv.getMaxSlots()));
		}

		BuddyList bList = p.getBuddyList();
		bList.increaseCapacity((short) (0xFF - bList.getCapacity()));
		p.getClient().getSession().send(GamePackets.writeBuddyCapacityUpdate(bList.getCapacity()));

		//TODO: increase character limit to 6
	}

	public static class MaxEquipStatsHandler extends AbstractCommandDefinition {
		@Override
		public String getHelpMessage() {
			return "Sets the bonus stats of all your equipped equipment to their max values.";
		}

		@Override
		public String getUsage() {
			return "Usage: !maxequips";
		}

		@Override
		public byte minPrivilegeLevel() {
			return UserPrivileges.GM;
		}

		@Override
		public void execute(GameCharacter p, String[] args, ClientNoticeStream resp) {
			maxEquips(p);
		}
	}

	public static class MaxSkillsHandler extends AbstractCommandDefinition {
		@Override
		public String getHelpMessage() {
			return "Sets all skills across all skillbooks to their master levels.";
		}

		@Override
		public String getUsage() {
			return "Usage: !maxskills";
		}

		@Override
		public byte minPrivilegeLevel() {
			return UserPrivileges.GM;
		}

		@Override
		public void execute(GameCharacter p, String[] args, ClientNoticeStream resp) {
			maxSkills(p);
		}
	}

	public static class MaxAllHandler extends AbstractCommandDefinition {
		@Override
		public String getHelpMessage() {
			return "Maxes out your base stats (including amount of inventory/storage slots and mesos by specifying -i), your equipment bonus stats, and your skill levels.";
		}

		@Override
		public String getUsage() {
			return "Usage: !maxall [-i]";
		}

		@Override
		public byte minPrivilegeLevel() {
			return UserPrivileges.GM;
		}

		@Override
		public void execute(GameCharacter p, String[] args, ClientNoticeStream resp) {
			maxSkills(p);
			maxEquips(p);
			maxStats(p, args);
		}
	}
}
