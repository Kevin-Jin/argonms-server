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

package argonms.common.character;

/**
 *
 * @author GoldenKevin
 */
public class InterServerPartyOps {
	public static final byte
		CREATE = 0,
		DISBAND = 1,
		ADD_PLAYER = 2,
		REMOVE_PLAYER = 3,
		CHANGE_LEADER = 4,
		JOIN = 5,
		LEAVE = 6,
		FETCH_LIST = 7,
		MEMBER_CHANGED_CHANNEL = 8,
		MEMBER_LOGGED_OFF = 9,
		MEMBER_STAT_UPDATED = 10
	;
}
