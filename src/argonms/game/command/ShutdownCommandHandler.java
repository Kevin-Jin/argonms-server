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
import argonms.game.net.external.handler.GameChatHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author GoldenKevin
 */
public class ShutdownCommandHandler extends AbstractCommandDefinition {
	private static final Logger LOG = Logger.getLogger(ShutdownCommandHandler.class.getName());

	@Override
	public String getHelpMessage() {
		return "Look up the WZ data ID of an object based on its name.";
	}

	@Override
	public String getUsage() {
		return "Usage: !shutdown [-r|-h|-c] <seconds countdown>|now";
	}

	@Override
	public byte minPrivilegeLevel() {
		return UserPrivileges.ADMIN;
	}

	private void serverWideNotice(String message) {
		byte[] packet = GamePackets.writeServerMessage(GameChatHandler.TextStyle.OK_BOX.byteValue(), message, (byte) -1, true);
		for (WorldChannel chn : GameServer.getInstance().getChannels().values())
			for (GameCharacter p : chn.getConnectedPlayers())
				p.getClient().getSession().send(packet);
	}

	private String reason(boolean halt, boolean restart) {
		if (restart)
			return "restart";
		else if (halt)
			return "halt";
		else
			return "maintenance";
	}

	private String formatTime(int seconds) {
		return ((seconds == 0) ? "NOW" : ("in " + seconds + " seconds"));
	}

	@Override
	public void execute(GameCharacter p, String[] args, ClientNoticeStream resp) {
		if (ParseHelper.hasOpt(args, "-c")) {
			//TODO: cancel shutdown
		} else {
			boolean halt = false, restart = false;
			int currentIndex = 1;
			if (ParseHelper.hasOpt(args, "-h")) {
				halt = true;
				currentIndex++;
			}
			if (ParseHelper.hasOpt(args, "-r")) {
				restart = true;
				currentIndex++;
			}
			int seconds;
			if (args[currentIndex].equalsIgnoreCase("NOW"))
				seconds = 0;
			else
				seconds = Integer.parseInt(args[currentIndex]);
			//TODO: implement restart
			String message = "The server is going down for " + reason(halt, restart) + " " + formatTime(seconds) + "!";
			serverWideNotice(message);
			LOG.log(Level.WARNING, message);
			GameServer.getInstance().shutdown(halt, seconds * 1000);
		}
	}
}
