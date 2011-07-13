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
import argonms.game.character.GameCharacter;
import argonms.game.field.GameMap;
import argonms.game.field.entity.Npc;
import argonms.game.loading.mob.MobDataLoader;
import argonms.game.loading.mob.MobStats;
import java.awt.Point;

/**
 *
 * @author GoldenKevin
 */
public class SpawnCommandHandler extends AbstractCommandDefinition {
	@Override
	public String getHelpMessage() {
		return "Spawn a temporary NPC or monster at your current location";
	}

	@Override
	public byte minPrivilegeLevel() {
		return UserPrivileges.GM;
	}

	private String getUsage() {
		return "Syntax: !spawn [mob/npc] [mob/npc WZ id] <mobtime>";
	}

	@Override
	public void execute(GameCharacter p, String[] args, ClientNoticeStream resp) {
		if (args.length < 3) {
			resp.printErr("Invalid usage. " + getUsage());
			return;
		}
		boolean mob;
		if (args[1].equalsIgnoreCase("mob")) {
			mob = true;
		} else if (args[1].equalsIgnoreCase("npc")) {
			mob = false;
		} else {
			resp.printErr(args[1] + " is not a valid spawn type. " + getUsage());
			return;
		}
		int dataId;
		try {
			dataId = Integer.parseInt(args[2]);
		} catch (NumberFormatException e) {
			resp.printErr(args[1] + " is not a valid " + args[1] + " id. " + getUsage());
			return;
		}
		GameMap map = p.getMap();
		if (mob) {
			MobStats stats = MobDataLoader.getInstance().getMobStats(dataId);
			if (stats == null) {
				resp.printErr(args[1] + " is not a valid " + args[1] + " id. " + getUsage());
				return;
			}
			Point pos = p.getPosition();
			int mobtime = -1;
			if (args.length > 3) {
				try {
					mobtime = Integer.parseInt(args[3]);
				} catch (NumberFormatException e) {
					resp.printErr(args[3] + " is not a valid mobtime. " + getUsage());
					return;
				}
			}
			map.addMonsterSpawn(stats, map.calcPointBelow(pos), map.getStaticData().getFootholds().findBelow(pos).getId(), mobtime);
		} else {
			//TODO: check if npcid is valid.
			Point pos = p.getPosition();
			Npc n = new Npc(dataId);
			n.setFoothold(map.getStaticData().getFootholds().findBelow(pos).getId());
			n.setPosition(map.calcPointBelow(pos));
			n.setCy((short) pos.y);
			n.setRx((short) pos.x, (short) pos.x);
			n.setStance((byte) (p.getStance() & 0x01)); //only uses lsb, which is what determines direction
			map.spawnEntity(n);
		}
	}
}
