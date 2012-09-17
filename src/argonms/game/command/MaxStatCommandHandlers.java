/*
 * ArgonMS MapleStory server emulator written in Java
 * Copyright (C) 2011-2012  GoldenKevin
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
import argonms.common.character.Skills;
import argonms.common.character.inventory.Equip;
import argonms.common.character.inventory.Inventory;
import argonms.common.character.inventory.Inventory.InventoryType;
import argonms.common.character.inventory.InventorySlot;
import argonms.game.character.GameCharacter;
import argonms.game.character.inventory.StorageInventory;
import argonms.game.loading.skill.SkillDataLoader;
import argonms.game.net.external.GamePackets;
import java.util.Map;
import java.util.Map.Entry;

/**
 *
 * @author GoldenKevin
 */
public final class MaxStatCommandHandlers {
	private static void maxEquips(GameCharacter p) {
		Map<Short, InventorySlot> iv = p.getInventory(InventoryType.EQUIPPED).getAll();
		synchronized(iv) {
			for (Entry<Short, InventorySlot> item : iv.entrySet()) {
				Equip e = (Equip) item.getValue();
				p.equipChanged(e, false, false);
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
				e.setHands(Short.MAX_VALUE);
				e.setSpeed((short) 40);
				e.setJump((short) 23);
				p.equipChanged(e, true, true);
				p.getClient().getSession().send(GamePackets.writeInventoryUpdateEquipStats(item.getKey().shortValue(), e));
			}
		}
	}

	private static void maxSkills(GameCharacter p) {
		for (int skillid : Skills.ALL) {
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

		p.setMesos(Integer.MAX_VALUE);
		for (InventoryType type : new InventoryType[] { InventoryType.EQUIP, InventoryType.USE, InventoryType.SETUP, InventoryType.ETC, InventoryType.CASH }) {
			Inventory inv = p.getInventory(type);
			inv.increaseCapacity((short) 0xFF);
			p.getClient().getSession().send(GamePackets.writeInventoryUpdateCapacity(type, (short) 0xFF));
		}
		StorageInventory inv = p.getStorageInventory();
		inv.increaseCapacity((short) 0xFF);

		BuddyList bList = p.getBuddyList();
		bList.increaseCapacity((short) 0xFF);
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
			return "Maxes out your base stats, your equipment bonus stats, and your skill levels.";
		}

		@Override
		public String getUsage() {
			return "Usage: !maxall";
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

	private MaxStatCommandHandlers() {
		//uninstantiable...
	}
}
