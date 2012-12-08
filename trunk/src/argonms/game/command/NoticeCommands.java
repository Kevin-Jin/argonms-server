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
import argonms.game.GameServer;
import argonms.game.net.external.handler.ChatHandler;

/**
 *
 * @author GoldenKevin
 */
public class NoticeCommands {
	public static class NoticeCommandHandler extends AbstractCommandDefinition<CommandCaller> {
		@Override
		public String getHelpMessage() {
			return "Send a message to all players on the server. Use the popup option to display an OK box, or the chat option to show the message (in blue) in the player's chat log.";
		}

		@Override
		public String getUsage() {
			return "Usage: !notice popup|chat <message>";
		}

		@Override
		public byte minPrivilegeLevel() {
			return UserPrivileges.GM;
		}

		private ChatHandler.TextStyle getStyle(String option) {
			if (option.equalsIgnoreCase("POPUP"))
				return ChatHandler.TextStyle.OK_BOX;
			if (option.equalsIgnoreCase("CHAT"))
				return ChatHandler.TextStyle.LIGHT_BLUE_TEXT_CLEAR_BG;
			return null;
		}

		@Override
		public void execute(CommandCaller caller, CommandArguments args, CommandOutput resp) {
			ChatHandler.TextStyle style;
			if (!args.hasNext() || (style = getStyle(args.next())) == null) {
				resp.printErr("Specify whether to display the message in a popup box or in the chat log.");
				resp.printErr(getUsage());
				return;
			}

			if (!args.hasNext()) {
				resp.printErr("No message specified.");
				resp.printErr(getUsage());
				return;
			}
			String message = args.restOfString();

			GameServer.getChannel(caller.getChannel()).getCrossServerInterface().sendWorldWideNotice(style.byteValue(), message);
		}
	}

	public static class TickerCommandHandler extends AbstractCommandDefinition<CommandCaller> {
		@Override
		public String getHelpMessage() {
			return "Update the ticker message for all players on this world. Leaving out the message parameter removes the ticker.";
		}

		@Override
		public String getUsage() {
			return "Usage: !ticker <message>";
		}

		@Override
		public byte minPrivilegeLevel() {
			return UserPrivileges.GM;
		}

		@Override
		public void execute(CommandCaller caller, CommandArguments args, CommandOutput resp) {
			String message;
			if (args.hasNext())
				message = args.restOfString();
			else
				message = "";

			GameServer.getChannel(caller.getChannel()).getCrossServerInterface().sendWorldWideNotice(ChatHandler.TextStyle.TICKER.byteValue(), message);
		}
	}
}
