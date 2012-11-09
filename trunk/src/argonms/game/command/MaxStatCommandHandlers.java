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
import argonms.common.character.PlayerJob;
import argonms.common.character.Skills;
import argonms.game.loading.skill.SkillDataLoader;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author GoldenKevin
 */
public final class MaxStatCommandHandlers {
	private static void maxEquips(List<CommandTarget.CharacterManipulation> changes) {
		changes.add(new CommandTarget.CharacterManipulation(CommandTarget.CharacterManipulationKey.MAX_ALL_EQUIP_STATS, null));
	}

	private static void maxSkills(List<CommandTarget.CharacterManipulation> changes) {
		for (int skillid : Skills.ALL) {
			byte masterLevel = SkillDataLoader.getInstance().getSkill(skillid).maxLevel();
			changes.add(new CommandTarget.CharacterManipulation(CommandTarget.CharacterManipulationKey.SET_SKILL_LEVEL, new CommandTarget.SkillValue(skillid, masterLevel, masterLevel)));
		}
	}

	private static void maxStats(List<CommandTarget.CharacterManipulation> changes) {
		changes.add(new CommandTarget.CharacterManipulation(CommandTarget.CharacterManipulationKey.SET_STR, Short.valueOf(Short.MAX_VALUE)));
		changes.add(new CommandTarget.CharacterManipulation(CommandTarget.CharacterManipulationKey.SET_DEX, Short.valueOf(Short.MAX_VALUE)));
		changes.add(new CommandTarget.CharacterManipulation(CommandTarget.CharacterManipulationKey.SET_INT, Short.valueOf(Short.MAX_VALUE)));
		changes.add(new CommandTarget.CharacterManipulation(CommandTarget.CharacterManipulationKey.SET_LUK, Short.valueOf(Short.MAX_VALUE)));
		changes.add(new CommandTarget.CharacterManipulation(CommandTarget.CharacterManipulationKey.SET_MAX_HP, Short.valueOf((short) 30000)));
		changes.add(new CommandTarget.CharacterManipulation(CommandTarget.CharacterManipulationKey.SET_HP, Short.valueOf((short) 30000)));
		changes.add(new CommandTarget.CharacterManipulation(CommandTarget.CharacterManipulationKey.SET_MAX_MP, Short.valueOf((short) 30000)));
		changes.add(new CommandTarget.CharacterManipulation(CommandTarget.CharacterManipulationKey.SET_MP, Short.valueOf((short) 30000)));
		changes.add(new CommandTarget.CharacterManipulation(CommandTarget.CharacterManipulationKey.SET_LEVEL, Short.valueOf((short) 200)));
		changes.add(new CommandTarget.CharacterManipulation(CommandTarget.CharacterManipulationKey.SET_EXP, Integer.valueOf(0)));
		changes.add(new CommandTarget.CharacterManipulation(CommandTarget.CharacterManipulationKey.SET_JOB, Short.valueOf(PlayerJob.JOB_SUPER_GM)));
		changes.add(new CommandTarget.CharacterManipulation(CommandTarget.CharacterManipulationKey.SET_FAME, Short.valueOf(Short.MAX_VALUE)));

		changes.add(new CommandTarget.CharacterManipulation(CommandTarget.CharacterManipulationKey.SET_MESO, Integer.valueOf(Integer.MAX_VALUE)));
		changes.add(new CommandTarget.CharacterManipulation(CommandTarget.CharacterManipulationKey.MAX_INVENTORY_SLOTS, null));

		changes.add(new CommandTarget.CharacterManipulation(CommandTarget.CharacterManipulationKey.MAX_BUDDY_LIST_SLOTS, null));

		//TODO: increase character limit to 6
	}

	public static class MaxEquipStatsHandler extends AbstractCommandDefinition<CommandCaller> {
		@Override
		public String getHelpMessage() {
			return "Sets the bonus stats of all of player's equipped equipment to their max values.";
		}

		@Override
		public String getUsage() {
			return "Usage: !maxequips [<target>]";
		}

		@Override
		public byte minPrivilegeLevel() {
			return UserPrivileges.GM;
		}

		@Override
		public void execute(CommandCaller caller, CommandArguments args, CommandOutput resp) {
			String targetName = args.extractTarget(null, caller.getName());
			if (targetName == null) {
				resp.printErr(getUsage());
				return;
			}
			CommandTarget target = args.getTargetByName(targetName, caller);
			if (target == null) {
				resp.printErr("The character " + targetName + " does not exist.");
				resp.printErr(getUsage());
				return;
			}

			List<CommandTarget.CharacterManipulation> changes = new ArrayList<CommandTarget.CharacterManipulation>();
			maxEquips(changes);
			target.mutate(changes);
		}
	}

	public static class MaxSkillsHandler extends AbstractCommandDefinition<CommandCaller> {
		@Override
		public String getHelpMessage() {
			return "Sets all skills across all skillbooks to their master levels.";
		}

		@Override
		public String getUsage() {
			return "Usage: !maxskills [<target>]";
		}

		@Override
		public byte minPrivilegeLevel() {
			return UserPrivileges.GM;
		}

		@Override
		public void execute(CommandCaller caller, CommandArguments args, CommandOutput resp) {
			String targetName = args.extractTarget(null, caller.getName());
			if (targetName == null) {
				resp.printErr(getUsage());
				return;
			}
			CommandTarget target = args.getTargetByName(targetName, caller);
			if (target == null) {
				resp.printErr("The character " + targetName + " does not exist.");
				resp.printErr(getUsage());
				return;
			}

			List<CommandTarget.CharacterManipulation> changes = new ArrayList<CommandTarget.CharacterManipulation>();
			maxSkills(changes);
			target.mutate(changes);
		}
	}

	public static class MaxAllHandler extends AbstractCommandDefinition<CommandCaller> {
		@Override
		public String getHelpMessage() {
			return "Maxes out a player's base stats, equipment bonus stats, and skill levels.";
		}

		@Override
		public String getUsage() {
			return "Usage: !maxall [<target>]";
		}

		@Override
		public byte minPrivilegeLevel() {
			return UserPrivileges.GM;
		}

		@Override
		public void execute(CommandCaller caller, CommandArguments args, CommandOutput resp) {
			String targetName = args.extractTarget(null, caller.getName());
			if (targetName == null) {
				resp.printErr(getUsage());
				return;
			}
			CommandTarget target = args.getTargetByName(targetName, caller);
			if (target == null) {
				resp.printErr("The character " + targetName + " does not exist.");
				resp.printErr(getUsage());
				return;
			}

			List<CommandTarget.CharacterManipulation> changes = new ArrayList<CommandTarget.CharacterManipulation>();
			maxSkills(changes);
			maxEquips(changes);
			maxStats(changes);
			target.mutate(changes);
		}
	}

	private MaxStatCommandHandlers() {
		//uninstantiable...
	}
}
