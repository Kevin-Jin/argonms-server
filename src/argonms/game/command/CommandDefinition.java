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

import argonms.game.character.GameCharacter;

/**
 *
 * @author GoldenKevin
 */
public class CommandDefinition extends AbstractCommandDefinition {
	private final CommandAction r;
	private final String help;
	private final byte minGm;

	public CommandDefinition(CommandAction toExec, String helpMessage, byte minPriv) {
		r = toExec;
		help = helpMessage;
		minGm = minPriv;
	}

	@Override
	public String getHelpMessage() {
		return help;
	}

	@Override
	public String getUsage() {
		return r.getUsage();
	}

	@Override
	public byte minPrivilegeLevel() {
		return minGm;
	}

	@Override
	public void execute(GameCharacter p, CommandArguments args, ClientNoticeStream resp) {
		r.doAction(p, args, resp);
	}

	public interface CommandAction {
		public String getUsage();
		public void doAction(GameCharacter p, CommandArguments args, ClientNoticeStream resp);
	}
}
