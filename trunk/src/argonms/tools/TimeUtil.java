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

package argonms.tools;

/**
 *
 * @author GoldenKevin
 */
public final class TimeUtil {
	/**
	 * Number of 100 nanosecond units from 1/1/1601 to 1/1/1970
	 */
	private static final long EPOCH_BIAS = 116444736000000000L;

	/**
	 * Converts a POSIX/Unix timestamp AKA time_t (milliseconds since the Unix
	 * epoch -  January 1, 1970 00:00) to a Windows/ANSI timestamp AKA FILETIME
	 * (ticks, or hundreds of nanoseconds since January 1, 1601 00:00).
	 * @param unixTime time_t
	 * @return FILETIME
	 */
	public static long unixToWindowsTime(long unixTime) {
		return (unixTime * 10000 + EPOCH_BIAS);
	}
}
