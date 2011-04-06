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

package argonms.net.client;

/**
 *
 * @author GoldenKevin
 */
public final class ClientSendOps {
	public static final short
		LOGIN_RESULT = 0x00,
		SERVERLOAD_MSG = 0x03,
		GENDER_DONE = 0x04,
		PIN_RESPONSE = 0x06,
		PIN_ASSIGNED = 0x07,
		ALL_CHARLIST = 0x08,
		WORLD_ENTRY = 0x0A,
		CHARLIST = 0x0B,
		CHANNEL_ADDRESS = 0x0C,
		CHECK_NAME_RESP = 0x0D,
		CHAR_CREATED = 0x0E,
		DELETE_CHAR_RESPONSE = 0x0F,
		GAME_HOST_ADDRESS = 0x10,
		PING = 0x11,
		RELOG_RESPONSE = 0x16,
		MODIFY_INVENTORY_SLOT = 0x1A,
		PLAYER_STAT_UPDATE = 0x1C,
		SHOW_STATUS_INFO = 0x24,
		SPAWN_PORTAL = 0x40,
		SERVER_MESSAGE = 0x41,
		TIP_MESSAGE = 0x4A,
		SKILL_MACRO = 0x5B,
		CHANGE_MAP = 0x5C,
		MTS_OPEN = 0x5D,
		CS_OPEN = 0x5E,
		PRIVATE_CHAT = 0x64,
		WHISPER = 0x65,
		SPOUSE_CHAT = 0x66,
		COOLDOWN = 0x70,
		SHOW_PLAYER = 0x78,
		REMOVE_PLAYER = 0x79,
		MAP_CHAT = 0x7A,
		SHOW_PET = 0x7F,
		MOVE_PLAYER = 0x8D,
		MELEE_ATTACK = 0x8E,
		RANGED_ATTACK = 0x8F,
		MAGIC_ATTACK = 0x90,
		DAMAGE_PLAYER = 0x94,
		FACIAL_EXPRESSION = 0x95,
		THIRD_PERSON_VISUAL_EFFECT = 0x99,
		FIRST_PERSON_VISUAL_EFFECT = 0xA1,
		PLAYER_HINT = 0xA9,
		SHOW_MONSTER = 0xAF,
		REMOVE_MONSTER = 0xB0,
		CONTROL_MONSTER = 0xB1,
		MOVE_MONSTER = 0xB2,
		MOVE_MONSTER_RESPONSE = 0xB3,
		SHOW_NPC = 0xC2,
		REMOVE_NPC = 0xC3,
		CONTROL_NPC = 0xC4,
		MOVE_NPC = 0xC5,
		SHOW_ITEM_DROP = 0xCD,
		REMOVE_ITEM_DROP = 0xCE,
		SHOW_MIST = 0xD2,
		REMOVE_MIST = 0xD3,
		SHOW_DOOR = 0xD4,
		REMOVE_DOOR = 0xD5,
		NPC_TALK = 0xED,
		NPC_SHOP = 0xEE,
		KEYMAP = 0x107
	;
	
	private ClientSendOps() {
		//uninstantiable...
	}
}
