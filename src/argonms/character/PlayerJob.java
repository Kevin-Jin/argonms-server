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

package argonms.character;

/**
 * A helper class with static methods that accept job id parameters and check
 * various conditions.
 * @author GoldenKevin
 */
public class PlayerJob {
	public static boolean isWarrior(short jobid) {
		return ((jobid / 100) == 1);
	}

	public static boolean isMage(short jobid) {
		return ((jobid / 100) == 2);
	}

	public static boolean isArcher(short jobid) {
		return ((jobid / 100) == 3);
	}

	public static boolean isThief(short jobid) {
		return ((jobid / 100) == 4);
	}

	public static boolean isPirate(short jobid) {
		return ((jobid / 100) == 5);
	}

	public static boolean isModerator(short jobid) {
		return ((jobid / 100) == 9);
	}
}
