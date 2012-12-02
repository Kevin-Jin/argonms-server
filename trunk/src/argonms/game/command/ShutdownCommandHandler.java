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
import argonms.game.character.GameCharacter;
import argonms.game.net.WorldChannel;
import argonms.game.net.external.GamePackets;
import argonms.game.net.external.handler.ChatHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author GoldenKevin
 */
public class ShutdownCommandHandler extends AbstractCommandDefinition<CommandCaller> {
	private static final Logger LOG = Logger.getLogger(ShutdownCommandHandler.class.getName());

	@Override
	public String getHelpMessage() {
		return "Starts a timer to restart or shutdown the server. Gives all players a warning beforehand, with an optional message.";
	}

	@Override
	public String getUsage() {
		return "Usage: !shutdown [-r|-h|-c] <seconds countdown>|now [<message>]";
	}

	@Override
	public byte minPrivilegeLevel() {
		return UserPrivileges.ADMIN;
	}

	private void serverWideNotice(String message) {
		byte[] packet = GamePackets.writeServerMessage(ChatHandler.TextStyle.OK_BOX.byteValue(), message, (byte) -1, true);
		for (WorldChannel chn : GameServer.getInstance().getChannels().values())
			for (GameCharacter p : chn.getConnectedPlayers())
				p.getClient().getSession().send(packet);
	}

	private String reason(boolean halt, boolean restart) {
		if (restart)
			return "restart";
		if (halt)
			return "halt";
		return "maintenance";
	}

	private String formatTime(int seconds) {
		return ((seconds == 0) ? "NOW" : ("in " + seconds + " seconds"));
	}

	@Override
	public void execute(CommandCaller caller, CommandArguments args, CommandOutput resp) {
		boolean halt = false, restart = false, cancel = false;;
		boolean option;
		String param;
		do {
			if (!args.hasNext()) {
				resp.printErr(getUsage());
				return;
			}
			param = args.next();
			if (param.equalsIgnoreCase("-H")) {
				restart = false;
				cancel = false;
				halt = true;
				option = true;
			} else if (param.equalsIgnoreCase("-R")) {
				halt = false;
				cancel = false;
				restart = true;
				option = true;
			} else if (param.equalsIgnoreCase("-C")) {
				halt = false;
				restart = false;
				cancel = true;
				option = false;
			} else {
				option = false;
			}
		} while (option);

		//TODO: support hh:mm for time
		int seconds;
		if (param.equalsIgnoreCase("NOW")) {
			seconds = 0;
		} else {
			try {
				seconds = Integer.parseInt(param);
			} catch (NumberFormatException e) {
				resp.printErr(getUsage());
				return;
			}
		}

		String message = null;
		if (args.hasNext())
			message = args.restOfString();

		if (!cancel) {
			String notice = "The server is going down for " + reason(halt, restart) + " " + formatTime(seconds) + "!\r\n" + message;
			serverWideNotice(notice);
			LOG.log(Level.WARNING, notice);
			GameServer.getInstance().shutdown(halt, seconds * 1000);
			if (restart) {
				//TODO: implement restart
			}
		} else {
			//TODO: implement cancel
		}
	}
}
