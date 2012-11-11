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

package argonms.game.net.external.handler;

import argonms.common.UserPrivileges;
import argonms.common.net.external.ClientSendOps;
import argonms.common.util.input.LittleEndianReader;
import argonms.common.util.output.LittleEndianByteArrayWriter;
import argonms.game.GameServer;
import argonms.game.character.GameCharacter;
import argonms.game.command.CommandProcessor;
import argonms.game.net.external.GameClient;
import argonms.game.net.external.GamePackets;

/**
 *
 * @author GoldenKevin
 */
public final class ChatHandler {
	private static final byte
		COMMAND_FIND = 5,
		COMMAND_WHISPER = 6
	;

	private static final byte
		FIND_RESPONSE_MAP = 1,
		FIND_RESPONSE_CASH_SHOP = 2,
		FIND_RESPONSE_CHANNEL = 3
	;

	public static void handleMapChat(LittleEndianReader packet, GameClient gc) {
		String message = packet.readLengthPrefixedString();
		byte show = packet.readByte();
		GameCharacter p = gc.getPlayer();
		if (!commandProcessed(p, message) && p.isVisible())
			p.getMap().sendToAll(writeMapChat(p, message, show, p.getPrivilegeLevel() > UserPrivileges.USER));
	}

	public static void handlePrivateChat(LittleEndianReader packet, GameClient gc) {
		byte type = packet.readByte(); // 0 for buddys, 1 for partys, 2 for guilds
		byte numRecipients = packet.readByte();
		int[] recipients = new int[numRecipients];
		for (byte i = 0; i < numRecipients; i++)
			recipients[i] = packet.readInt();
		String message = packet.readLengthPrefixedString();
		GameCharacter p = gc.getPlayer();
		if (!commandProcessed(p, message))
			GameServer.getChannel(gc.getChannel()).getCrossServerInterface().sendPrivateChat(type, recipients, p, message);
	}

	public static void handleClientCommand(LittleEndianReader reader, GameClient gc) {
		switch (reader.readByte()) {
			case COMMAND_FIND: {
				String toFind = reader.readLengthPrefixedString();
				byte channel = GameServer.getChannel(gc.getChannel()).getCrossServerInterface().scanChannelOfPlayer(toFind);
				if (channel == gc.getChannel()) {
					gc.getSession().send(writeFindResultSameChannel(toFind, GameServer.getChannel(channel).getPlayerByName(toFind).getMapId()));
				} else if (channel != 0) {
					gc.getSession().send(writeFindResultDiffChannel(toFind, channel));
				} else {
					//TODO: try shop server
					gc.getSession().send(writeWhisperOutcome(toFind, false));
				}
				break;
			}
			case COMMAND_WHISPER: {
				String recipient = reader.readLengthPrefixedString();
				String message = reader.readLengthPrefixedString();
				GameCharacter p = gc.getPlayer();

				if (!commandProcessed(p, message)) {
					boolean result = GameServer.getChannel(gc.getChannel()).getCrossServerInterface().sendWhisper(recipient, p, message);
					gc.getSession().send(writeWhisperOutcome(recipient, result));
				}
				break;
			}
		}
	}

	public static void handleSpouseChat(LittleEndianReader packet, GameClient gc) {
		String recipient = packet.readLengthPrefixedString();
		String message = packet.readLengthPrefixedString();
		GameCharacter p = gc.getPlayer();
		if (!commandProcessed(p, message))
			GameServer.getChannel(gc.getChannel()).getCrossServerInterface().sendSpouseChat(recipient, p, message);
	}

	private static byte[] writeMapChat(GameCharacter p, String message, byte show, boolean gm) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(10
				+ message.length());

		lew.writeShort(ClientSendOps.MAP_CHAT);
		lew.writeInt(p.getId());
		lew.writeBool(gm);
		lew.writeLengthPrefixedString(message);
		lew.writeByte(show);

		return lew.getBytes();
	}

	private static boolean commandProcessed(GameCharacter p, String chat) {
		char first = chat.charAt(0);
		if (first != '!' && first != '@')
			return false;
		CommandProcessor.getInstance().process(p, chat);
		return true;
	}

	public enum TextStyle {
		OK_BOX(1),
		LIGHT_BLUE_TEXT_WHITE_BG(2),
		TICKER(4),
		RED_TEXT_CLEAR_BG(5),
		LIGHT_BLUE_TEXT_CLEAR_BG(6),
		ORANGE_TEXT_CLEAR_BG(0),
		PURPLE_TEXT_CLEAR_BG(1),
		PINK_TEXT_CLEAR_BG(2),
		DULL_GREEN_TEXT_CLEAR_BG(3),
		WHITE_TEXT_CLEAR_BG(0),
		BLACK_TEXT_WHITE_BG(1),
		BRIGHT_GREEN_TEXT_CLEAR_BG(-1),
		YELLOW_TEXT_CLEAR_BG(-1)
		;

		private final byte subType;

		private TextStyle(int subType) {
			this.subType = (byte) subType;
		}

		public byte byteValue() {
			return subType;
		}
	}

	public static byte[] writeStyledChat(TextStyle ts, GameCharacter p, String message, boolean megaEar) {
		switch (ts) {
			case OK_BOX:
			case LIGHT_BLUE_TEXT_WHITE_BG:
			case RED_TEXT_CLEAR_BG:
			case LIGHT_BLUE_TEXT_CLEAR_BG:
				return GamePackets.writeServerMessage(ts.byteValue(), p.getName() + " : " + message, p.getClient().getChannel(), megaEar);
			case ORANGE_TEXT_CLEAR_BG:
			case PURPLE_TEXT_CLEAR_BG:
			case PINK_TEXT_CLEAR_BG:
				return GamePackets.writePrivateChatMessage(ts.byteValue(), p.getName(), message);
			case DULL_GREEN_TEXT_CLEAR_BG:
				return GamePackets.writeSpouseChatMessage(p.getName(), message);
			case WHITE_TEXT_CLEAR_BG:
			case BLACK_TEXT_WHITE_BG:
				return writeMapChat(p, message, (byte) 0, ts.byteValue() != 0);
			case BRIGHT_GREEN_TEXT_CLEAR_BG:
				return GamePackets.writeWhisperMessge(p.getName(), message, p.getClient().getChannel());
			case YELLOW_TEXT_CLEAR_BG:
				return GamePackets.writeTipMessage(p.getName() + " : " + message);
			default:
				return null;
		}
	}

	private static byte[] writeFindResultSameChannel(String name, int map) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(18
				+ name.length());

		lew.writeShort(ClientSendOps.WHISPER);
		lew.writeByte((byte) 9);
		lew.writeLengthPrefixedString(name);
		lew.writeByte(FIND_RESPONSE_MAP);
		lew.writeInt(map);
		lew.writeLong(0);

		return lew.getBytes();
	}

	private static byte[] writeFindResultCashShop(String name) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(10
				+ name.length());

		lew.writeShort(ClientSendOps.WHISPER);
		lew.writeByte((byte) 9);
		lew.writeLengthPrefixedString(name);
		lew.writeByte(FIND_RESPONSE_CASH_SHOP);
		lew.writeInt(-1);

		return lew.getBytes();
	}

	private static byte[] writeFindResultDiffChannel(String name, byte channel) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(10
				+ name.length());

		lew.writeShort(ClientSendOps.WHISPER);
		lew.writeByte((byte) 9);
		lew.writeLengthPrefixedString(name);
		lew.writeByte(FIND_RESPONSE_CHANNEL);
		lew.writeInt(channel - 1);

		return lew.getBytes();
	}

	private static byte[] writeWhisperOutcome(String to, boolean success) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(6
				+ to.length());

		lew.writeShort(ClientSendOps.WHISPER);
		lew.writeByte((byte) 0x0A);
		lew.writeLengthPrefixedString(to);
		lew.writeBool(success);

		return lew.getBytes();
	}

	private ChatHandler() {
		//uninstantiable...
	}
}
