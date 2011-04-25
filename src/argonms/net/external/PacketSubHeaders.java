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

package argonms.net.external;

/**
 * Describes every second-level constant value (i.e., a header directly
 * underneath a ClientSendOps constant) used in CommonPackets that isn't already
 * enumerated in a different class. To prevent collisions, each constant is
 * given a meaningful prefix to describe its name space. The class isn't very
 * populated at the moment, but I will do my best in appending on to the list.
 *
 * Maybe one day, I'll group each packet with the same top-level (ClientSendOps)
 * header in a single class and just list the sub headers in that class. Not
 * just one class per file, but maybe a couple of related top-level operations
 * grouped in public static classes in a file.
 *
 * Created as part of an effort to curb magic numbers and to give more meaning
 * to certain constants that are in written in packets sent to clients
 * @author GoldenKevin
 */
public class PacketSubHeaders {
	public static final byte //byte constants
		STATUS_INFO_INVENTORY = 0,
		STATUS_INFO_QUEST = 1,
		STATUS_INFO_EXP = 3,
		STATUS_INFO_FAME = 4,
		STATUS_INFO_MESOS = 5,
		STATUS_INFO_GUILD_POINTS = 6,
		//got these from Vana, I'm really interested in how they work! ^.^
		GM_BLOCK = 4,
		GM_INVALID_CHAR_NAME = 6,
		GM_SET_GET_VAR_RESULT = 9,
		GM_HIDE = 16,
		GM_HIRED_MERCHANT_PLACE = 19,
		GM_WARNING = 29
	;

	/*public static final int //int constants
	;*/

	/*public static final short //short constants
	;*/
}
