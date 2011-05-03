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

package argonms.game.handler;

import argonms.character.Player;
import argonms.character.inventory.Inventory.InventoryType;
import argonms.character.inventory.InventorySlot;
import argonms.character.inventory.InventoryTools;
import argonms.character.inventory.ItemTools;
import argonms.character.skill.SkillTools;
import argonms.character.skill.Skills;
import argonms.game.GameClient;
import argonms.net.external.CommonPackets;
import argonms.net.external.RemoteClient;
import argonms.tools.input.LittleEndianReader;
import java.awt.Point;

/**
 *
 * @author GoldenKevin
 */
public class GameBuffHandler {
	public static void handleUseSkill(LittleEndianReader packet, RemoteClient rc) {
		Player p = ((GameClient) rc).getPlayer();
		/*int tickCount = */packet.readInt();
		int skillId = packet.readInt();
		byte skillLevel = packet.readByte();
		switch (skillId) {
			case Skills.HERO_MONSTER_MAGNET:
			case Skills.PALADIN_MONSTER_MAGNET:
			case Skills.DARK_KNIGHT_MONSTER_MAGNET: {
				//TODO: monster magnet
				for (int i = packet.readInt(); i > 0; --i) {
					int mobId = packet.readInt();
					byte success = packet.readByte();
				}
				byte direction = packet.readByte();
				break;
			}
		}
		if (packet.available() == 5) { //summon skill
			Point summonPos = packet.readPos();
			//TODO: summon skills
		}
		SkillTools.useBuffSkill(p, skillId, skillLevel);
	}

	public static void handleUseItem(LittleEndianReader packet, RemoteClient rc) {
		Player p = ((GameClient) rc).getPlayer();
		/*int tickCount = */packet.readInt();
		short slot = packet.readShort();
		int itemId = packet.readInt();
		//TODO: hacking if item's id at slot does not match itemId
		InventorySlot changed = InventoryTools.takeFromInventory(p.getInventory(InventoryType.USE), slot, (short) 1);
		if (changed != null)
			rc.getSession().send(CommonPackets.writeInventorySlotUpdate(InventoryType.USE, slot, changed));
		else
			rc.getSession().send(CommonPackets.writeInventoryClearSlot(InventoryType.USE, slot));
		ItemTools.useItem(p, itemId);
	}

	public static void handleCancelSkill(LittleEndianReader packet, RemoteClient rc) {
		Player p = ((GameClient) rc).getPlayer();
		int skillId = packet.readInt();
		switch (skillId) {
			case Skills.HURRICANE:
			case Skills.PIERCING_ARROW:
			case Skills.RAPID_FIRE:
				//TODO: continuous fire skills
				break;
			default:
				SkillTools.cancelBuffSkill(p, skillId);
				break;
		}
	}

	public static void handleCancelItem(LittleEndianReader packet, RemoteClient rc) {
		Player p = ((GameClient) rc).getPlayer();
		int itemId = -packet.readInt();
		ItemTools.cancelBuffItem(p, itemId);
	}
}
