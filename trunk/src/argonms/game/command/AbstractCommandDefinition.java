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

package argonms.game.command;

import argonms.game.character.GameCharacter;

/**
 *
 * @author GoldenKevin
 */
public abstract class AbstractCommandDefinition {
	public abstract String getHelpMessage();
	public abstract void execute(GameCharacter p, String[] args, ClientNoticeStream resp);
	public abstract byte minPrivilegeLevel();

	public static boolean isNumber(String s) {
		char[] array = s.toCharArray();
		for (int i = array.length - 1; i > 0; --i)
			if (array[i] > '9' || array[i] < '0')
				return false;
		return true;
	}
}
