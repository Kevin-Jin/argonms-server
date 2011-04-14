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
 *
 * @author GoldenKevin
 */
public final class ClientRecvOps {
	public static final short
		LOGIN_PASSWORD = 0x01,
		GUEST_LOGIN = 0x02,
		SERVERLIST_REREQUEST = 0x04,
		CHARLIST_REQ = 0x05,
		REQ_SERVERLOAD = 0x06,
		SET_GENDER = 0x08,
		PIN_OPERATION = 0x09,
		REGISTER_PIN = 0x0A,
		SERVERLIST_REQUEST = 0x0B,
		EXIT_CHARLIST = 0x0C,
		VIEW_ALL_CHARS = 0x0D,
		PICK_ALL_CHAR = 0x0E,
		ENTER_EXIT_VIEW_ALL = 0x0F,
		CHAR_NAME_CHANGE = 0x10,
		CHAR_TRANSFER = 0x12,
		CHAR_SELECT = 0x13,
		PLAYER_CONNECTED = 0x14,
		CHECK_CHAR_NAME = 0x15,
		CREATE_CHAR = 0x16,
		DELETE_CHAR = 0x17,
		PONG = 0x18,
		CLIENT_ERROR = 0x19,
		AES_IV_UPDATE_REQUEST = 0x1A,
		RELOG = 0x1C,
		CHANGE_MAP = 0x23,
		CHANGE_CHANNEL = 0x24,
		ENTER_CASH_SHOP = 0x25,
		MOVE_PLAYER = 0x26,
		CANCEL_CHAIR = 0x27,
		USE_CHAIR = 0x28,
		MELEE_ATTACK = 0x29,
		RANGED_ATTACK = 0x2A,
		MAGIC_ATTACK = 0x2B,
		PASSIVE_ENERGY = 0x2C,
		TAKE_DAMAGE = 0x2D,
		MAP_CHAT = 0x2E,
		CLOSE_CHALKBOARD = 0x2F,
		FACIAL_EXPRESSION = 0x30,
		USE_ITEMEFFECT = 0x31,
		NPC_TALK = 0x36,
		NPC_TALK_MORE = 0x38,
		NPC_SHOP = 0x39,
		STORAGE = 0x3A,
		USE_HIRED_MERCHANT = 0x3B,
		DUEY_ACTION = 0x3D,
		ITEM_SORT = 0x40,
		ITEM_MOVE = 0x42,
		USE_ITEM = 0x43,
		CANCEL_ITEM = 0x44,
		USE_SUMMON_BAG = 0x46,
		PET_FOOD = 0x47,
		USE_MOUNT_FOOD = 0x48,
		USE_CASH_ITEM = 0x49,
		USE_CATCH_ITEM = 0x4A,
		USE_SKILL_BOOK = 0x4B,
		USE_RETURN_SCROLL = 0x4E,
		USE_UPGRADE_SCROLL = 0x4F,
		DISTRIBUTE_AP = 0x50,
		HEAL_OVER_TIME = 0x51,
		DISTRIBUTE_SP = 0x52,
		USE_SKILL = 0x53,
		CANCEL_SKILL = 0x54,
		SKILL_EFFECT = 0x55,
		MESO_DROP = 0x56,
		GIVE_FAME = 0x57,
		CHAR_INFO_REQUEST = 0x59,
		SPAWN_PET = 0x5A,
		CANCEL_DEBUFF = 0x5B,
		CHANGE_MAP_SPECIAL = 0x5C,
		USE_INNER_PORTAL = 0x5D,
		TROCK_ADD_MAP = 0x5E,
		QUEST_ACTION = 0x62,
		//Special skills? 0x63 (Concentrate, Maple Warrior, summons) when casted and canceled, or when player gets diseased
		SKILL_MACRO = 0x65,
		REPORT = 0x68,
		USE_TREASURE_BOX = 0x69,
		PARTYCHAT = 0x6B,
		WHISPER = 0x6C,
		SPOUSECHAT = 0x6D,
		MESSENGER = 0x6E,
		PLAYER_SHOP = 0x6F,
		PLAYER_INTERACTION = 0x6F,
		PARTY_OPERATION = 0x70,
		DENY_PARTY_REQUEST = 0x71,
		GUILD_OPERATION = 0x72,
		DENY_GUILD_REQUEST = 0x73,
		BUDDYLIST_MODIFY = 0x76,
		NOTE_ACTION = 0x77,
		USE_DOOR = 0x79,
		CHANGE_BINDING = 0x7B,
		ADMIN_LOG = 0x7F,
		BBS_OPERATION = 0x86,
		ENTER_MTS = 0x87,
		PET_TALK = 0x8B,
		MOVE_PET = 0x8C,
		PET_CHAT = 0x8D,
		PET_COMMAND = 0x8E,
		PET_LOOT = 0x8F,
		PET_AUTO_POT = 0x90,
		MOVE_SUMMON = 0x92,
		DAMAGE_SUMMON = 0x94,
		SUMMON_ATTACK = 0x95,
		MOVE_MOB = 0x9D,
		AUTO_AGGRO = 0x9E,
		MOB_DAMAGE_MOB = 0xA1,
		MONSTER_BOMB = 0xA2,
		HYPNOTIZE = 0xA3,
		MOVE_NPC = 0xA6,
		ITEM_PICKUP = 0xAB,
		DAMAGE_REACTOR = 0xAE,
		TOUCH_REACTOR = 0xAF,
		SNOWBALL = 0xB3,
		MONSTER_CARNIVAL = 0xB9,
		ENTERED_SHIP_MAP = 0xBB,
		PARTY_SEARCH_REGISTER = 0xBD,
		PARTY_SEARCH_START = 0xBF,
		PLAYER_UPDATE = 0xC0,
		TOUCHING_CS = 0xC5,
		BUY_CS_ITEM = 0xC6,
		COUPON_CODE = 0xC7,
		MAPLETV = 0xD4,
		MTS_OP = 0xD9
	;

	private ClientRecvOps() {
		//uninstantiable...
	}
}
