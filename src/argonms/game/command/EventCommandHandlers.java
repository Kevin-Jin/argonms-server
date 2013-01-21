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
import argonms.common.util.Rng;
import argonms.game.GameServer;
import argonms.game.character.GameCharacter;
import argonms.game.field.GameMap;
import argonms.game.field.MapEntity;
import argonms.game.net.external.GamePackets;
import argonms.game.net.external.handler.ChatHandler;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author GoldenKevin
 */
public class EventCommandHandlers implements CommandCollection<CommandCaller> {
	private static class EventUtilCommandHandler extends AbstractCommandDefinition<CommandCaller> {
		@Override
		public String getHelpMessage() {
			return "Commands that help with the execution of GM-led events. Pass the --usage parameter after any one of the subcommands to see how it is used.";
		}

		@Override
		public String getUsage() {
			return "Usage: !eventutil (roll <roll arguments>|--usage)|(stun <stun arguments>|--usage)|(unstun <unstun arguments>|--usage)|(admit <admit arguments>|--usage)";
		}

		@Override
		public byte minPrivilegeLevel() {
			return UserPrivileges.GM;
		}

		@Override
		public void execute(CommandCaller caller, CommandArguments args, CommandOutput resp) {
			if (!args.hasNext()) {
				resp.printErr(getUsage());
				return;
			}

			String option = args.next();
			if (option.equalsIgnoreCase("ROLL")) {
				final String usage = "Usage: !eventutil roll <possibilities>|--usage";

				if (!args.hasNext()) {
					resp.printErr("Specify the number of possibilities.");
					resp.printErr(usage);
					return;
				}

				String possibilities = args.next();
				if (possibilities.equalsIgnoreCase("--USAGE")) {
					resp.printOut(usage);
					resp.printOut("Roll an \"n\" sided dice and have the result be sent to everyone in your map.");
					return;
				}
				try {
					String message = "The dice has been rolled. The result is " + (Rng.getGenerator().nextInt(Integer.parseInt(possibilities)) + 1) + ".";
					caller.getMap().sendToAll(GamePackets.writeServerMessage(ChatHandler.TextStyle.LIGHT_BLUE_TEXT_CLEAR_BG.byteValue(), message, (byte) -1, true));
				} catch (NumberFormatException e) {
					resp.printErr(possibilities + " is not a valid number of possibilities.");
					resp.printErr(usage);
				}
			} else if (option.equalsIgnoreCase("STUN")) {
				final String usage = "Usage: !eventutil stun [<target>]|--usage";

				List<CommandTarget.CharacterManipulation> change = Collections.singletonList(new CommandTarget.CharacterManipulation(CommandTarget.CharacterManipulationKey.STUN, Boolean.valueOf(true)));
				String targetName = args.extractTarget(null, null);
				if (targetName != null) {
					if (targetName.equalsIgnoreCase("--USAGE")) {
						resp.printOut(usage);
						resp.printOut("Cast stun on one target player. The entire map will be stunned if target is omitted.");
						return;
					}

					CommandTarget target = args.getTargetByName(targetName, caller);
					if (target == null) {
						resp.printErr("The character " + targetName + " does not exist.");
						resp.printErr(usage);
						return;
					}

					target.mutate(change);
				} else {
					for (MapEntity ent : caller.getMap().getAllEntities(MapEntity.EntityType.PLAYER))
						new LocalChannelCommandTarget((GameCharacter) ent).mutate(change);
				}
			} else if (option.equalsIgnoreCase("UNSTUN")) {
				final String usage = "Usage: !eventutil unstun [<target>]|--usage";

				List<CommandTarget.CharacterManipulation> change = Collections.singletonList(new CommandTarget.CharacterManipulation(CommandTarget.CharacterManipulationKey.STUN, Boolean.valueOf(false)));
				String targetName = args.extractTarget(null, null);
				if (targetName != null) {
					if (targetName.equalsIgnoreCase("--USAGE")) {
						resp.printOut(usage);
						resp.printOut("Cancel the stun effect for one target player. The entire map will be unstunned if target is omitted.");
						return;
					}

					CommandTarget target = args.getTargetByName(targetName, caller);
					if (target == null) {
						resp.printErr("The character " + targetName + " does not exist.");
						resp.printErr(usage);
						return;
					}

					target.mutate(change);
				} else {
					for (MapEntity ent : caller.getMap().getAllEntities(MapEntity.EntityType.PLAYER))
						new LocalChannelCommandTarget((GameCharacter) ent).mutate(change);
				}
			} else if (option.equalsIgnoreCase("ADMIT")) {
				final String usage = "Usage: !eventutil admit (<source map> [<target player>])|--usage";

				if (!args.hasNext()) {
					resp.printErr("Missing map id.");
					resp.printErr(usage);
					return;
				}
				String arg = args.next();
				if (arg.equalsIgnoreCase("--USAGE")) {
					resp.printOut(usage);
					resp.printOut("Move everyone from a map (on your channel) to the map that the target player is in. If target player is omitted, the players will be warped to your location.");
					return;
				}
				int mapId;
				try {
					mapId = Integer.parseInt(arg);
				} catch (NumberFormatException ex) {
					resp.printErr("Invalid map id.");
					resp.printErr(usage);
					return;
				}
				GameMap sourceMap = GameServer.getChannel(caller.getChannel()).getMapFactory().getMap(mapId);
				if (sourceMap == null) {
					resp.printErr("Invalid map id.");
					resp.printErr(usage);
					return;
				}

				String targetName = args.extractTarget(null, caller.getName());
				CommandTarget target = args.getTargetByName(targetName, caller);
				if (target == null) {
					resp.printErr("The character " + targetName + " does not exist.");
					resp.printErr(usage);
					return;
				}

				CommandTarget.MapValue destination = (CommandTarget.MapValue) target.access(CommandTarget.CharacterProperty.MAP);
				if (destination.channel != caller.getChannel())
					resp.printOut("Players from map " + mapId + " will be transferred to channel " + destination.channel + ".");
				List<CommandTarget.CharacterManipulation> change = Collections.singletonList(new CommandTarget.CharacterManipulation(CommandTarget.CharacterManipulationKey.CHANGE_MAP, destination));
				for (MapEntity ent : sourceMap.getAllEntities(MapEntity.EntityType.PLAYER))
					new LocalChannelCommandTarget((GameCharacter) ent).mutate(change);
			}
		}
	}

	@Override
	public Map<String, AbstractCommandDefinition<CommandCaller>> getDefinitions() {
		return Collections.<String, AbstractCommandDefinition<CommandCaller>>unmodifiableMap(Collections.singletonMap("!eventutil", new EventUtilCommandHandler()));
	}
}
