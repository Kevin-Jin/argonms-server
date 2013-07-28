/*
 * ArgonMS MapleStory server emulator written in Java
 * Copyright (C) 2011-2013  GoldenKevin
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

package argonms.common.net.internal;

/**
 *
 * @author GoldenKevin
 */
public class ChannelSynchronizationOps {
	public static final byte
		INBOUND_PLAYER = 1,
		INBOUND_PLAYER_ACCEPTED = 2,
		PLAYER_SEARCH = 3,
		PLAYER_SEARCH_RESPONSE = 4,
		MULTI_CHAT = 5,
		WHISPER_CHAT = 6,
		WHISPER_RESPONSE = 7,
		SPOUSE_CHAT = 8,
		BUDDY_INVITE = 9,
		BUDDY_INVITE_RESPONSE = 10,
		BUDDY_INVITE_RETRACTION = 11,
		BUDDY_ONLINE = 12,
		BUDDY_ACCEPTED = 13,
		BUDDY_ONLINE_RESPONSE = 14,
		BUDDY_OFFLINE = 15,
		BUDDY_DELETED = 16,
		CHATROOM_INVITE = 17,
		CHATROOM_INVITE_RESPONSE = 18,
		CHATROOM_DECLINE = 19,
		CHATROOM_TEXT = 20,
		CROSS_CHANNEL_COMMAND_CHARACTER_MANIPULATION = 21,
		CROSS_CHANNEL_COMMAND_CHARACTER_ACCESS = 22,
		CROSS_CHANNEL_COMMAND_CHARACTER_ACCESS_RESPONSE = 23,
		SYNCHRONIZED_NOTICE = 24,
		SYNCHRONIZED_SHUTDOWN = 25,
		SYNCHRONIZED_RATE_CHANGE = 26,
		WHO_COMMAND = 27,
		WHO_COMMAND_RESPONSE = 28
	;

	public static final byte
		SCAN_PLAYER_CHANNEL_NO_MATCH = 0,
		SCAN_PLAYER_CHANNEL_HIDDEN = 1,
		SCAN_PLAYER_CHANNEL_FOUND = 2
	;

	public static final byte
		CHANNEL_OFFLINE = -1,
		CHANNEL_CASH_SHOP = 0
	;
}
