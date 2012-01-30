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

import argonms.common.GlobalConstants;
import argonms.common.UserPrivileges;
import argonms.game.character.ExpTables;
import argonms.game.character.GameCharacter;

/**
 *
 * @author GoldenKevin
 */
public class StatCommandHandler extends AbstractCommandDefinition {
	@Override
	public String getHelpMessage() {
		return "Change the value of one of your stats.";
	}

	@Override
	public String getUsage() {
		return "Usage: !stat set|add str|dex|int|luk|ap|sp|level|exp|job|maxhp|maxmp|hp|mp|fame|meso <amount>";
	}

	@Override
	public byte minPrivilegeLevel() {
		return UserPrivileges.GM;
	}

	@Override
	public void execute(GameCharacter p, String[] args, ClientNoticeStream resp) {
		if (args.length < 4) {
			resp.printErr(getUsage());
			return;
		}
		int val;
		try {
			val = Integer.parseInt(args[3]);
		} catch (NumberFormatException e) {
			resp.printErr(getUsage());
			return;
		}
		String option = args[1];
		String stat = args[2];
		if (option.equalsIgnoreCase("set")) {
			if (stat.equalsIgnoreCase("str")) {
				p.setStr((short) Math.min(val, Short.MAX_VALUE));
			} else if (stat.equalsIgnoreCase("dex")) {
				p.setDex((short) Math.min(val, Short.MAX_VALUE));
			} else if (stat.equalsIgnoreCase("int")) {
				p.setInt((short) Math.min(val, Short.MAX_VALUE));
			} else if (stat.equalsIgnoreCase("luk")) {
				p.setLuk((short) Math.min(val, Short.MAX_VALUE));
			} else if (stat.equalsIgnoreCase("ap")) {
				p.setAp((short) Math.min(val, Short.MAX_VALUE));
			} else if (stat.equalsIgnoreCase("sp")) {
				p.setSp((short) Math.min(val, Short.MAX_VALUE));
			} else if (stat.equalsIgnoreCase("level")) {
				p.setLevel((short) Math.min(val, GlobalConstants.MAX_LEVEL));
				if (p.getLevel() < GlobalConstants.MAX_LEVEL)
					p.setExp(Math.min(p.getExp(), ExpTables.getForLevel(p.getLevel()) - 1));
				else
					p.setExp(0);
			} else if (stat.equalsIgnoreCase("exp")) {
				if (p.getLevel() < GlobalConstants.MAX_LEVEL)
					p.setExp(Math.min(val, ExpTables.getForLevel(p.getLevel()) - 1));
				else if (val == 0)
					p.setExp(0);
			} else if (stat.equalsIgnoreCase("job")) {
				p.setJob((short) val);
			} else if (stat.equalsIgnoreCase("maxhp")) {
				p.setMaxHp((short) Math.min(val, 30000));
			} else if (stat.equalsIgnoreCase("maxmp")) {
				p.setMaxMp((short) Math.min(val, 30000));
			} else if (stat.equalsIgnoreCase("hp")) {
				p.setHp((short) Math.min(val, p.getCurrentMaxHp()));
			} else if (stat.equalsIgnoreCase("mp")) {
				p.setMp((short) Math.min(val, p.getCurrentMaxMp()));
			} else if (stat.equalsIgnoreCase("fame")) {
				p.setFame((short) Math.min(val, Short.MAX_VALUE));
			} else if (stat.equalsIgnoreCase("meso")) {
				p.setMesos(val);
			} else {
				resp.printErr("Invalid stat " + stat + ". Valid choices: str, dex,"
						+ "int, luk, ap, sp, level, exp, job, maxhp, maxmp, hp, mp, fame, meso");
			}
		} else if (option.equalsIgnoreCase("add")) {
			if (stat.equalsIgnoreCase("str")) {
				p.setStr((short) Math.min(p.getStr() + val, Short.MAX_VALUE));
			} else if (stat.equalsIgnoreCase("dex")) {
				p.setDex((short) Math.min(p.getDex() + val, Short.MAX_VALUE));
			} else if (stat.equalsIgnoreCase("int")) {
				p.setInt((short) Math.min(p.getInt() + val, Short.MAX_VALUE));
			} else if (stat.equalsIgnoreCase("luk")) {
				p.setLuk((short) Math.min(p.getLuk() + val, Short.MAX_VALUE));
			} else if (stat.equalsIgnoreCase("ap")) {
				p.setAp((short) Math.min(p.getAp() + val, Short.MAX_VALUE));
			} else if (stat.equalsIgnoreCase("sp")) {
				p.setSp((short) Math.min(p.getSp() + val, Short.MAX_VALUE));
			} else if (stat.equalsIgnoreCase("level")) {
				p.setLevel((short) Math.min(p.getLevel() + val, GlobalConstants.MAX_LEVEL));
				if (p.getLevel() < GlobalConstants.MAX_LEVEL)
					p.setExp(Math.min(p.getExp(), ExpTables.getForLevel(p.getLevel()) - 1));
				else
					p.setExp(0);
			} else if (stat.equalsIgnoreCase("exp")) {
				if (p.getLevel() < GlobalConstants.MAX_LEVEL)
					p.setExp(Math.min((int) Math.min((long) p.getExp() + val, Integer.MAX_VALUE), ExpTables.getForLevel(p.getLevel()) - 1));
				else if (p.getExp() + val == 0)
					p.setExp(0);
			} else if (stat.equalsIgnoreCase("maxhp")) {
				p.setMaxHp((short) Math.min(p.getMaxHp() + val, 30000));
			} else if (stat.equalsIgnoreCase("maxmp")) {
				p.setMaxMp((short) Math.min(p.getMaxMp() + val, 30000));
			} else if (stat.equalsIgnoreCase("hp")) {
				p.setHp((short) Math.min(p.getHp() + val, p.getCurrentMaxHp()));
			} else if (stat.equalsIgnoreCase("mp")) {
				p.setMp((short) Math.min(p.getMp() + val, p.getCurrentMaxMp()));
			} else if (stat.equalsIgnoreCase("fame")) {
				p.setFame((short) Math.min(p.getFame() + val, Short.MAX_VALUE));
			}  else if (stat.equalsIgnoreCase("meso")) {
				p.setMesos((int) Math.min((long) p.getMesos() + val, Integer.MAX_VALUE));
			}else {
				resp.printErr("Invalid stat " + stat + ". Valid choices: str, dex,"
						+ "int, luk, ap, sp, level, exp, maxhp, maxmp, hp, mp, fame, meso");
			}
		} else {
			resp.printErr(getUsage());
			return;
		}
	}
}
