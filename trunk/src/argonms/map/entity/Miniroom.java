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

package argonms.map.entity;

import argonms.character.Player;
import argonms.map.GameMap;
import argonms.map.MapEntity;
import argonms.net.external.ClientSendOps;
import argonms.net.external.CommonPackets;
import argonms.tools.output.LittleEndianByteArrayWriter;
import argonms.tools.output.LittleEndianWriter;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author GoldenKevin
 */
public abstract class Miniroom extends MapEntity {
	public static final byte
		ACT_CREATE = 0x00, //0
		ACT_INVITE = 0x02, //2
		ACT_DECLINE = 0x03, //3
		ACT_VISIT = 0x04, //4
		ACT_JOIN = 0x05, //5
		ACT_CHAT = 0x06, //6
		ACT_EXIT = 0x0A, //10
		ACT_OPEN = 0x0B, //11
		ACT_SET_ITEMS = 0x0E, //14
		ACT_SET_MESO = 0x0F, //15
		ACT_CONFIRM = 0x10, //16
		ACT_ADD_ITEM = 0x14, //20
		ACT_BUY = 0x15, //21
		ACT_CANNOT_BUY = 0x16, //22
		ACT_SHOP_ITEM_UPDATE = 0x17, //23
		ACT_REMOVE_ITEM = 0x19, //25
		ACT_BAN_PLAYER = 0x1A, //26
		ACT_SPAWN_SHOP = 0x1C, //28
		ACT_PUT_ITEM = 0x1F, //31
		ACT_MERCHANT_BUY = 0x20, //32
		ACT_TAKE_ITEM_BACK = 0x24, //36
		ACT_MAINTENANCE_OFF = 0x25, //37
		ACT_CLOSE_MERCHANT = 0x27, //39
		ACT_REQUEST_TIE = 0x2C, //44
		ACT_ANSWER_TIE = 0x2D, //45
		ACT_GIVE_UP = 0x2E, //46
		ACT_REQUEST_REDO = 0x30, //48
		ACT_ANSWER_REDO = 0x31, //49
		ACT_EXIT_AFTER_GAME = 0x32, //50
		ACT_CANCEL_EXIT = 0x33, //51
		ACT_READY = 0x34, //52
		ACT_UN_READY = 0x35, //53
		ACT_EXPEL = 0x36, //54
		ACT_START = 0x37, //55
		ACT_FINISH_GAME = 0x38, //56
		ACT_SKIP = 0x39, //57
		ACT_MOVE_OMOK = 0x3A, //58
		ACT_CANNOT_MOVE = 0x3B, //59
		ACT_SELECT_CARD = 0x3E //62
	;

	public static final byte
		JOIN_ERROR_ALREADY_CLOSED = 0x01,
		JOIN_ERROR_FULL = 0x02,
		JOIN_ERROR_OWNER_BUSY = 0x03,
		JOIN_ERROR_DEAD = 0x04,
		JOIN_ERROR_IN_EVENT = 0x05,
		JOIN_ERROR_UNABLE_TO_DO_IT = 0x06,
		JOIN_ERROR_CANNOT_TRADE_ITEMS = 0x07,
		JOIN_ERROR_DIFFERENT_MAPS = 0x09,
		JOIN_ERROR_TOO_CLOSE_TO_PORTAL = 0x0A,
		JOIN_ERROR_CANNOT_ESTABLISH_HERE = 0x0B,
		JOIN_ERROR_CANNOT_START_GAME_HERE = 0x0C,
		JOIN_ERROR_NOT_IN_FM = 0x0D,
		JOIN_ERROR_ROOM_7_TO_22 = 0x0E,
		JOIN_ERROR_BANNED = 0x0F,
		JOIN_ERROR_STORE_MAINTENANCE = 0x10,
		JOIN_ERROR_CANNOT_ENTER_TOURNEY = 0x11,
		JOIN_ERROR_CANNOT_TRADE_ITEMS_2 = 0x12,
		JOIN_ERROR_INSUFFICIENT_FUNDS = 0x13,
		JOIN_ERROR_INCORRECT_PASSWORD = 0x14
	;

	public static final byte
		INVITE_ERROR_NOT_FOUND = 0x01,
		INVITE_ERROR_BUSY = 0x02,
		INVITE_ERROR_DENIED = 0x03,
		INVITE_ERROR_BLOCKED_INVITATIONS = 0x40
	;

	public static final byte
		EXIT_SELF_SELECTED = 0x00,
		EXIT_NO_MESSAGE = 0x01,
		EXIT_TRADE_CANCELED = 0x02,
		EXIT_ROOM_CLOSED = 0x03,
		EXIT_BANNED = 0x05,
		EXIT_TRADE_SUCCESS = 0x06,
		EXIT_TRADE_FAIL = 0x07,
		EXIT_TRADE_MORE_THAN_ONE = 0x08,
		EXIT_ON_DIFFERENT_MAP = 0x09,
		EXIT_HIRED_MERCHANT_MAINTENCE = 0x0D,
		EXIT_CLOSE_HIRED_MERCHANT = 0x10
	;

	public enum MiniroomType {
		NONE (0),
		OMOK (1),
		MATCH_CARDS (2),
		TRADE (3),
		PLAYER_SHOP (4),
		HIRED_MERCHANT (5);

		private static final Map<Byte, MiniroomType> lookup;

		//initialize reverse lookup
		static {
			lookup = new HashMap<Byte, MiniroomType>(values().length);
			for(MiniroomType type : values())
				lookup.put(Byte.valueOf(type.byteValue()), type);
		}

		private final byte id;

		private MiniroomType(int clientVal) {
			id = (byte) clientVal;
		}

		public byte byteValue() {
			return id;
		}

		public static MiniroomType valueOf(byte value) {
			return lookup.get(Byte.valueOf(value));
		}
	}

	private final Player[] occupants;
	private final String dsc, pwd;
	private final byte type;
	protected boolean openToMap;

	public Miniroom(Player creator, int maxOccupants, String desc, String pass, byte kind) {
		occupants = new Player[maxOccupants];
		occupants[0] = creator;
		dsc = desc;
		pwd = pass;
		type = kind;
	}

	public Player getPlayerByPosition(byte pos) {
		return occupants[pos];
	}

	protected void setPlayerToPosition(byte pos, Player p) {
		occupants[pos] = p;
	}

	public String getMessage() {
		return dsc;
	}

	public String getPassword() {
		return pwd;
	}

	public byte getStyle() {
		return type;
	}

	public boolean joinRoom(Player p) {
		for (byte i = 1; i < occupants.length; i++) {
			if (occupants[i] == null) {
				p.setMiniRoom(this);
				sendToAll(getThirdPersonJoinMessage(p, i));
				occupants[i] = p;
				p.getClient().getSession().send(getFirstPersonJoinMessage(p));
				if (openToMap)
					p.getMap().sendToAll(getUpdateMapBoxMessage());
				return true;
			}
		}
		return false;
	}

	public void leaveRoom(Player p) {
		Player v;
		byte pos = positionOf(p);
		if (pos == 0) {
			closeRoom(p.getMap());
			for (byte i = 1; i < occupants.length; i++) {
				if ((v = occupants[i]) != null) {
					v.setMiniRoom(null);
					v.getClient().getSession().send(getFirstPersonLeaveMessage(i, EXIT_ROOM_CLOSED));
				}
			}
		} else {
			occupants[pos] = null;
			sendToAll(getThirdPersonLeaveMessage(pos));
			if (openToMap)
				p.getMap().sendToAll(getUpdateMapBoxMessage());
		}
	}

	protected void banVisitor(byte pos) {
		Player p = occupants[pos];
		p.setMiniRoom(null);
		p.getClient().getSession().send(getFirstPersonLeaveMessage(pos, EXIT_BANNED));
		leaveRoom(p);
	}

	public boolean isPlayerBanned(Player p) {
		return false;
	}

	public void closeRoom(GameMap map) {
		map.destroyEntity(this);
	}

	public byte positionOf(Player p) {
		for (byte i = 0; i < occupants.length; i++)
			if (occupants[i] == p)
				return i;
		return -1;
	}

	public byte getAmountOfPlayers() {
		byte count = 0;
		for (int i = 0; i < occupants.length; i++)
			if (occupants[i] != null)
				count++;
		return count;
	}

	public byte getMaxPlayers() {
		return (byte) occupants.length;
	}

	public boolean gameInProgress() {
		return false;
	}

	public void sendToAll(byte[] message) {
		for (Player p : occupants)
			if (p != null)
				p.getClient().getSession().send(message);
	}

	public void openRoom(GameMap map) {
		openToMap = true;
		//miniroom should already be in map's entities list - so just make it
		//visible to the players
		map.sendToAll(getCreationMessage());
	}

	public void tempCloseRoom(GameMap map) {
		openToMap = false;
		//don't actually remove miniroom from map's entities list - reserve this
		//entId for ourselves for easy reopening
		map.sendToAll(getDestructionMessage());
	}

	public abstract MiniroomType getMiniroomType();

	public abstract byte[] getFirstPersonJoinMessage(Player p);

	public abstract byte[] getThirdPersonJoinMessage(Player p, byte pos);

	public byte[] getFirstPersonLeaveMessage(byte pos, byte message) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(5);
		lew.writeShort(ClientSendOps.MINIROOM_ACT);
		lew.writeByte(ACT_EXIT);
		lew.writeByte(pos);
		lew.writeByte(message);
		return lew.getBytes();
	}

	public byte[] getThirdPersonLeaveMessage(byte pos) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(4);
		lew.writeShort(ClientSendOps.MINIROOM_ACT);
		lew.writeByte(ACT_EXIT);
		lew.writeByte(pos);
		return lew.getBytes();
	}

	public byte[] getUpdateMapBoxMessage() {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter();
		lew.writeShort(ClientSendOps.MINIROOM_BOX);
		lew.writeInt(occupants[0].getId());
		CommonPackets.writeMiniroomMapBox(lew, this);
		return lew.getBytes();
	}

	public EntityType getEntityType() {
		return EntityType.MINI_ROOM;
	}

	public boolean isAlive() {
		return true;
	}

	public boolean isVisible() {
		return openToMap;
	}

	public byte[] getCreationMessage() {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter();
		lew.writeShort(ClientSendOps.MINIROOM_BOX);
		lew.writeInt(occupants[0].getId());
		CommonPackets.writeMiniroomMapBox(lew, this);
		return lew.getBytes();
	}

	public byte[] getShowEntityMessage() {
		return getCreationMessage();
	}

	public byte[] getDestructionMessage() {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(7);
		lew.writeShort(ClientSendOps.MINIROOM_BOX);
		lew.writeInt(occupants[0].getId());
		lew.writeByte((byte) 0);
		return lew.getBytes();
	}

	public byte[] getOutOfViewMessage() {
		return null;
	}

	public boolean isNonRangedType() {
		return true;
	}

	protected static void writeMiniroomAvatar(LittleEndianWriter lew, Player p, byte pos) {
		lew.writeByte(pos);
		CommonPackets.writeAvatar(lew, p, false);
		lew.writeLengthPrefixedString(p.getName());
	}
}
