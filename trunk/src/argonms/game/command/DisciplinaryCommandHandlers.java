/*
 * ArgonMS MapleStory server emulator written in Java
 * Copyright (C) 2011-2013  GoldenKevin
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
import argonms.common.util.TimeTool;
import argonms.game.character.MapMemoryVariable;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author GoldenKevin
 */
public class DisciplinaryCommandHandlers implements CommandCollection<CommandCaller> {
	@Override
	public Map<String, AbstractCommandDefinition<CommandCaller>> getDefinitions() {
		Map<String, AbstractCommandDefinition<CommandCaller>> commands = new HashMap<String, AbstractCommandDefinition<CommandCaller>>();
		commands.put("!jail", new CommandDefinition<CommandCaller>(new CommandDefinition.CommandAction<CommandCaller>() {
			@Override
			public String getUsage() {
				return "Usage: !jail <target>";
			}

			@Override
			public void doAction(CommandCaller caller, CommandArguments args, CommandOutput resp) {
				String targetName = args.extractTarget(null, null);
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

				target.mutate(Collections.singletonList(new CommandTarget.CharacterManipulation(CommandTarget.CharacterManipulationKey.CHANGE_MAP, new CommandTarget.MapValue(CommandTarget.MapValue.JAIL_MAP_ID))));
			}
		}, "Disclipline a player by sending them to a map they cannot leave from", UserPrivileges.GM));
		commands.put("!unjail", new CommandDefinition<CommandCaller>(new CommandDefinition.CommandAction<CommandCaller>() {
			@Override
			public String getUsage() {
				return "Usage: !unjail <target>";
			}

			@Override
			public void doAction(CommandCaller caller, CommandArguments args, CommandOutput resp) {
				String targetName = args.extractTarget(null, null);
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

				target.mutate(Collections.singletonList(new CommandTarget.CharacterManipulation(CommandTarget.CharacterManipulationKey.RETURN_TO_REMEMBERED_MAP, MapMemoryVariable.JAIL)));
			}
		}, "Allow a jailed player to resume playing where they were before being jailed", UserPrivileges.GM));
		commands.put("!ban", new CommandDefinition<CommandCaller>(new CommandDefinition.CommandAction<CommandCaller>() {
			@Override
			public String getUsage() {
				return "Usage: !ban <target> [<expire date>|perm] <reason>";
			}

			@Override
			public void doAction(CommandCaller caller, CommandArguments args, CommandOutput resp) {
				String targetName = args.extractTarget(null, null);
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

				long expireTimestamp;
				if (!args.hasNext()) {
					resp.printErr(getUsage());
					return;
				}
				String param = args.next();
				try {
					int iDate = Integer.parseInt(param);
					Calendar expireCal = TimeTool.intDateToCalendar(iDate);
					if (expireCal == null) {
						resp.printErr("Expire date must be in the form of YYYYMMDD.");
						resp.printErr(getUsage());
						return;
					}

					expireCal.set(Calendar.HOUR_OF_DAY, 0);
					expireCal.set(Calendar.MINUTE, 0);
					expireCal.set(Calendar.SECOND, 0);
					expireCal.set(Calendar.MILLISECOND, 0);

					if (expireCal.before(Calendar.getInstance())) {
						resp.printErr("Expire date must not be in the past.");
						resp.printErr(getUsage());
						return;
					}
					expireTimestamp = expireCal.getTimeInMillis();
				} catch (NumberFormatException e) {
					if (!param.equalsIgnoreCase("perm")) {
						resp.printErr("Expire date must be in the form of YYYYMMDD.");
						resp.printErr(getUsage());
						return;
					}
					expireTimestamp = TimeTool.NO_EXPIRATION;
				}

				if (!args.hasNext()) {
					resp.printErr(getUsage());
					return;
				}
				String reason = args.restOfString();

				target.mutate(Collections.singletonList(new CommandTarget.CharacterManipulation(CommandTarget.CharacterManipulationKey.BAN, new CommandTarget.BanValue(caller.getName(), reason, expireTimestamp))));
			}
		}, "Raise a player's infraction level past the tolerance to ban them", UserPrivileges.GM));
		commands.put("!kick", new CommandDefinition<CommandCaller>(new CommandDefinition.CommandAction<CommandCaller>() {
			@Override
			public String getUsage() {
				return "Usage: !kick <target>";
			}

			@Override
			public void doAction(CommandCaller caller, CommandArguments args, CommandOutput resp) {
				String targetName = args.extractTarget(null, null);
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

				target.mutate(Collections.singletonList(new CommandTarget.CharacterManipulation(CommandTarget.CharacterManipulationKey.KICK, null)));
			}
		}, "Force disconnect a player", UserPrivileges.GM));
		return Collections.unmodifiableMap(commands);
	}
}
