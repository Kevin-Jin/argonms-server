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

package argonms.common.net.external;

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
		UPDATE_INVENTORY_CAPACITY = 0x1B,
		PLAYER_STAT_UPDATE = 0x1C,
		FIRST_PERSON_APPLY_STATUS_EFFECT = 0x1D,
		FIRST_PERSON_CANCEL_STATUS_EFFECT = 0x1E,
		SKILL_ENTRY_UPDATE = 0x21,
		FAME_OPERATION = 0x23,
		SHOW_STATUS_INFO = 0x24,
		SHOW_QUEST_COMPLETION = 0x2E,
		SHOW_HIRED_MERCHANT_AGREEMENT = 0x2F,
		PERSONAL_INFO_RESPONSE = 0x3A,
		PARTY_LIST = 0x3B,
		BUDDY_LIST = 0x3C,
		SPAWN_PORTAL = 0x40,
		SERVER_MESSAGE = 0x41,
		TIP_MESSAGE = 0x4A,
		PLAYER_NPC = 0x4E,
		SKILL_MACRO = 0x5B,
		CHANGE_MAP = 0x5C,
		MTS_OPEN = 0x5D,
		CS_OPEN = 0x5E,
		SHOW_EQUIP_EFFECT = 0x63,
		PRIVATE_CHAT = 0x64,
		WHISPER = 0x65,
		SPOUSE_CHAT = 0x66,
		MAP_EFFECT = 0x68,
		GM = 0x6B,
		CLOCK = 0x6E,
		SHOW_PLAYER = 0x78,
		REMOVE_PLAYER = 0x79,
		MAP_CHAT = 0x7A,
		MINIROOM_BALLOON = 0x7C,
		SHOW_PET = 0x7F,
		SHOW_SUMMON = 0x86,
		REMOVE_SUMMON = 0x87,
		MOVE_SUMMON = 0x88,
		SUMMON_ATTACK = 0x89,
		DAMAGE_SUMMON = 0x8A,
		MOVE_PLAYER = 0x8D,
		MELEE_ATTACK = 0x8E,
		RANGED_ATTACK = 0x8F,
		MAGIC_ATTACK = 0x90,
		ENERGY_CHARGE_ATTACK = 0x91,
		PREPARED_SKILL = 0x92,
		END_KEY_DOWN = 0x93,
		DAMAGE_PLAYER = 0x94,
		FACIAL_EXPRESSION = 0x95,
		ITEM_CHAIR = 0x97,
		UPDATE_AVATAR = 0x98,
		THIRD_PERSON_VISUAL_EFFECT = 0x99,
		THIRD_PERSON_APPLY_STATUS_EFFECT = 0x9A,
		THIRD_PERSON_CANCEL_STATUS_EFFECT = 0x9B,
		UPDATE_PARTY_MEMBER_HP = 0x9C,
		CHAIR = 0xA0,
		FIRST_PERSON_VISUAL_EFFECT = 0xA1,
		QUEST_START = 0xA6,
		PLAYER_HINT = 0xA9,
		COOLDOWN = 0xAD,
		SHOW_MONSTER = 0xAF,
		REMOVE_MONSTER = 0xB0,
		CONTROL_MONSTER = 0xB1,
		MOVE_MONSTER = 0xB2,
		MOVE_MONSTER_RESPONSE = 0xB3,
		APPLY_MONSTER_STATUS_EFFECT = 0xB5,
		CANCEL_MONSTER_STATUS_EFFECT = 0xB6,
		SHOW_MONSTER_HP = 0xBD,
		MONSTER_DRAGGED = 0xBE,
		SHOW_NPC = 0xC2,
		REMOVE_NPC = 0xC3,
		CONTROL_NPC = 0xC4,
		MOVE_NPC = 0xC5,
		SHOW_HIRED_MERCHANT = 0xCA,
		REMOVE_HIRED_MERCHANT = 0xCB,
		HIRED_MERCHANT_BALLOON = 0xCC,
		SHOW_ITEM_DROP = 0xCD,
		REMOVE_ITEM_DROP = 0xCE,
		SHOW_MIST = 0xD2,
		REMOVE_MIST = 0xD3,
		SHOW_DOOR = 0xD4,
		REMOVE_DOOR = 0xD5,
		HIT_REACTOR = 0xD6,
		SHOW_REACTOR = 0xD8,
		REMOVE_REACTOR = 0xD9,
		NPC_TALK = 0xED,
		NPC_SHOP = 0xEE,
		CONFIRM_SHOP_TRANSACTION = 0xEF,
		NPC_STORAGE = 0xF0,
		MINIROOM_ACT = 0xF5,
		KEYMAP = 0x107
	;
	
	private ClientSendOps() {
		//uninstantiable...
	}
}
