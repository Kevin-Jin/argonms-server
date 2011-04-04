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

package argonms.game.clientcommand;

import argonms.character.Player;

/**
 *
 * @author GoldenKevin
 */
public class CommandDefinition extends AbstractCommandDefinition {
	private CommandAction r;
	private String help;
	private byte minGm;

	public CommandDefinition(CommandAction toExec, String helpMessage, byte minPriv) {
		r = toExec;
		help = helpMessage;
		minGm = minPriv;
	}

	public String getHelpMessage() {
		return help;
	}

	public byte minPrivilegeLevel() {
		return minGm;
	}

	public void execute(Player p, String[] args, ClientNoticeStream resp) {
		r.doAction(p, args, resp);
	}

	public interface CommandAction {
		public void doAction(Player p, String[] args, ClientNoticeStream resp);
	}
}
