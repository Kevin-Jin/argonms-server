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
	public String getUsage() {
		return "Usage: !spawn (mob <wz id> [-c <count>])|(npc <wz id>) [-m <mobtime>]";
	}

	@Override
	public byte minPrivilegeLevel() {
		return UserPrivileges.GM;
	}

	private int getMobTime(String[] array, ClientNoticeStream resp) {
		String s;
		try {
			s = ParseHelper.getOptValue(array, "-m");
		} catch (Exception e) {
			resp.printErr("No mobtime specified after -m flag.");
			resp.printErr(getUsage());
			throw new IllegalArgumentException();
		}
		if (s == null)
			return -1;
		try {
			return Integer.parseInt(s);
		} catch (NumberFormatException e) {
			resp.printErr(s + " is not a valid mobtime.");
			resp.printErr(getUsage());
			throw new IllegalArgumentException();
		}
	}

	private int getCount(String[] array, ClientNoticeStream resp) {
		String s;
		try {
			s = ParseHelper.getOptValue(array, "-c");
		} catch (Exception e) {
			resp.printErr("No count specified after -c flag.");
			resp.printErr(getUsage());
			throw new IllegalArgumentException();
		}
		if (s == null)
			return 1;
		try {
			return Integer.parseInt(s);
		} catch (NumberFormatException e) {
			resp.printErr(s + " is not a valid count.");
			resp.printErr(getUsage());
			throw new IllegalArgumentException();
		}
	}

	@Override
	public void execute(GameCharacter p, String[] args, ClientNoticeStream resp) {
		if (args.length < 3) {
			resp.printErr(getUsage());
			return;
		}
		boolean mob;
		if (args[1].equalsIgnoreCase("mob")) {
			mob = true;
		} else if (args[1].equalsIgnoreCase("npc")) {
			mob = false;
		} else {
			resp.printErr(args[1] + " is not a valid spawn type.");
			resp.printErr(getUsage());
			return;
		}
		int dataId;
		try {
			dataId = Integer.parseInt(args[2]);
		} catch (NumberFormatException e) {
			resp.printErr(args[2] + " is not a valid " + args[1] + " id.");
			resp.printErr(getUsage());
			return;
		}
		GameMap map = p.getMap();
		if (mob) {
			MobStats stats = MobDataLoader.getInstance().getMobStats(dataId);
			if (stats == null) {
				resp.printErr(args[2] + " is not a valid " + args[1] + " id.");
				resp.printErr(getUsage());
				return;
			}
			Point pos = p.getPosition();
			int mobtime = getMobTime(args, resp);
			int count = getCount(args, resp);
			Point spawnLoc = map.calcPointBelow(pos);
			short foothold = map.getStaticData().getFootholds().findBelow(pos).getId();
			for (int i = 0; i < count; i++)
				map.addMonsterSpawn(stats, new Point(spawnLoc), foothold, mobtime);
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
