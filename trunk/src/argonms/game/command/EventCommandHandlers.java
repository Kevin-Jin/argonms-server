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
import argonms.game.character.GameCharacter;
import argonms.game.field.MapEntity;
import argonms.game.net.external.GamePackets;
import argonms.game.net.external.handler.ChatHandler;
import java.util.Collections;
import java.util.List;

/**
 *
 * @author GoldenKevin
 */
public class EventCommandHandlers {
	public static class EventUtilCommandHandler extends AbstractCommandDefinition<CommandCaller> {
		@Override
		public String getHelpMessage() {
			return "Roll an \"n\" sided dice using \"!eventutil roll n\", stun a player using \"!eventutil stun playername\", or unstun a player using \"!eventutil unstun playername\". Omitting the player's name for applicable commands will apply effects to everyone that is in the map you are in.";
		}

		@Override
		public String getUsage() {
			return "Usage: !eventutil (roll <possibilities>)|(stun [<target>])|(unstun [<target>])";
		}

		@Override
		public byte minPrivilegeLevel() {
			return UserPrivileges.GM;
		}

		@Override
		public void execute(CommandCaller caller, CommandArguments args, CommandOutput resp) {
			if (!args.hasNext()) {
				resp.printErr("Specify an option.");
				resp.printErr(getUsage());
				return;
			}

			String option = args.next();
			if (option.equalsIgnoreCase("ROLL")) {
				if (!args.hasNext()) {
					resp.printErr("Specify the number of possibilities.");
					resp.printErr(getUsage());
					return;
				}

				String possibilities = args.next();
				try {
					String message = "The dice has been rolled. The result is " + (Rng.getGenerator().nextInt(Integer.parseInt(possibilities)) + 1) + ".";
					caller.getMap().sendToAll(GamePackets.writeServerMessage(ChatHandler.TextStyle.LIGHT_BLUE_TEXT_CLEAR_BG.byteValue(), message, (byte) -1, true));
				} catch (NumberFormatException e) {
					resp.printErr(possibilities + " is not a valid number of possibilities.");
					resp.printErr(getUsage());
				}
			} else if (option.equalsIgnoreCase("STUN")) {
				List<CommandTarget.CharacterManipulation> change = Collections.singletonList(new CommandTarget.CharacterManipulation(CommandTarget.CharacterManipulationKey.STUN, Boolean.valueOf(true)));
				String targetName = args.extractTarget(null, null);
				if (targetName != null) {
					CommandTarget target = args.getTargetByName(targetName, caller);
					if (target == null) {
						resp.printErr("The character " + targetName + " does not exist.");
						resp.printErr(getUsage());
						return;
					}

					target.mutate(change);
				} else {
					for (MapEntity ent : caller.getMap().getAllEntities(MapEntity.EntityType.PLAYER))
						new LocalChannelCommandTarget((GameCharacter) ent).mutate(change);
				}
			} else if (option.equalsIgnoreCase("UNSTUN")) {
				List<CommandTarget.CharacterManipulation> change = Collections.singletonList(new CommandTarget.CharacterManipulation(CommandTarget.CharacterManipulationKey.STUN, Boolean.valueOf(false)));
				String targetName = args.extractTarget(null, null);
				if (targetName != null) {
					CommandTarget target = args.getTargetByName(targetName, caller);
					if (target == null) {
						resp.printErr("The character " + targetName + " does not exist.");
						resp.printErr(getUsage());
						return;
					}

					target.mutate(change);
				} else {
					for (MapEntity ent : caller.getMap().getAllEntities(MapEntity.EntityType.PLAYER))
						new LocalChannelCommandTarget((GameCharacter) ent).mutate(change);
				}
			}
		}
	}
}
