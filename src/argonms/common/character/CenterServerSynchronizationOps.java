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
public class CenterServerSynchronizationOps {
	public static final byte
		PARTY_CREATE = 0,
		PARTY_DISBAND = 1,
		PARTY_ADD_PLAYER = 2,
		PARTY_REMOVE_PLAYER = 3,
		PARTY_CHANGE_LEADER = 4,
		PARTY_JOIN_ERROR = 5,
		PARTY_FETCH_LIST = 6,
		PARTY_MEMBER_CONNECTED = 7,
		PARTY_MEMBER_DISCONNECTED = 8,
		PARTY_MEMBER_STAT_UPDATED = 9,
		CHATROOM_CREATE = 10,
		CHATROOM_ADD_PLAYER = 11,
		CHATROOM_REMOVE_PLAYER = 12,
		CHATROOM_UPDATE_AVATAR_CHANNEL = 13,
		CHATROOM_UPDATE_AVATAR_LOOK = 14,
		CHATROOM_CREATED = 15,
		CHATROOM_ROOM_CHANGED = 16,
		CHATROOM_SLOT_CHANGED = 17
	;
}
