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

package argonms.common.util;

import java.util.Calendar;
import java.util.TimeZone;

/**
 *
 * @author GoldenKevin
 */
public final class TimeTool {
	/**
	 * Number of 100 nanosecond units from 1/1/1601 to 1/1/1970
	 */
	private static final long EPOCH_BIAS = 116444736000000000L;
	private static TimeTool instance;

	private final int timeZoneOffset;
	/**
	 * Some arbitrary date that Wizet made up that is supposed to tell the
	 * client that an item has no expiration date on it.
	 * The date itself happens to be midnight January 1, 2079 UTC...
	 */
	private final long noExpiration;
	private final TimeZone tz;

	private TimeTool(TimeZone tz) {
		this.tz = tz;
		timeZoneOffset = tz.getRawOffset() + tz.getDSTSavings();
		noExpiration = 3439756800000L - timeZoneOffset;
	}

	public static void setInstance(TimeZone tz) {
		instance = new TimeTool(tz);
	}

	public static long getNoExpirationTimestamp() {
		return instance.noExpiration;
	}

	/**
	 * Converts a POSIX/Unix timestamp AKA time_t (milliseconds since the Unix
	 * epoch -  January 1, 1970 00:00) to a Windows/ANSI timestamp AKA FILETIME
	 * (ticks, or hundreds of nanoseconds since January 1, 1601 00:00).
	 * @param unixTime time_t
	 * @return FILETIME
	 */
	public static long unixToWindowsTime(long unixTime) {
		return ((unixTime + instance.timeZoneOffset) * 10000 + EPOCH_BIAS);
	}

	public static Calendar intDateToCalendar(int idate) {
		String timeStr = Integer.toString(idate);
		Calendar cal = Calendar.getInstance(instance.tz);
		switch (timeStr.length()) {
			case 8: //YYYYMMDD
				cal.set(Integer.parseInt(timeStr.substring(0, 4)), Integer.parseInt(timeStr.substring(4, 6)), Integer.parseInt(timeStr.substring(6, 8)));
				break;
			case 10: //YYYYMMDDHH
				cal.set(Integer.parseInt(timeStr.substring(0, 4)), Integer.parseInt(timeStr.substring(4, 6)), Integer.parseInt(timeStr.substring(6, 8)), Integer.parseInt(timeStr.substring(8, 10)), 0);
				break;
			default:
				cal = null;
				break;
		}
		return cal;
	}

	public static TimeZone getTimeZone() {
		return instance.tz;
	}

	public static Calendar currentDateTime() {
		return Calendar.getInstance(instance.tz);
	}
}
