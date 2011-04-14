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

package argonms.game.handler;

import argonms.UserPrivileges;
import argonms.character.Player;
import argonms.game.clientcommand.CommandProcessor;
import argonms.game.GameClient;
import argonms.game.GameServer;
import argonms.net.external.ClientSendOps;
import argonms.net.external.CommonPackets;
import argonms.net.external.RemoteClient;
import argonms.tools.input.LittleEndianReader;
import argonms.tools.output.LittleEndianByteArrayWriter;

/**
 *
 * @author GoldenKevin
 */
public class GameChatHandler {
	public static void handleMapChat(LittleEndianReader packet, RemoteClient rc) {
		String message = packet.readLengthPrefixedString();
		byte show = packet.readByte();
		Player p = ((GameClient) rc).getPlayer();
		if (!commandProcessed(p, message))
			p.getMap().sendToAll(writeMapChat(p, message, show, p.getPrivilegeLevel() > UserPrivileges.USER));
	}

	public static void handlePrivateChat(LittleEndianReader packet, RemoteClient rc) {
		byte type = packet.readByte(); // 0 for buddys, 1 for partys, 2 for guilds
		byte numRecipients = packet.readByte();
		int[] recipients = new int[numRecipients];
		for (byte i = 0; i < numRecipients; i++)
			recipients[i] = packet.readInt();
		String message = packet.readLengthPrefixedString();
		Player p = ((GameClient) rc).getPlayer();
		if (!commandProcessed(p, message))
			GameServer.getChannel(rc.getChannel()).getInterChannelInterface().sendPrivateChat(type, recipients, p, message);
	}

	public static void handleSpouseChat(LittleEndianReader packet, RemoteClient rc) {
		String recipient = packet.readLengthPrefixedString();
		String message = packet.readLengthPrefixedString();
		Player p = ((GameClient) rc).getPlayer();
		if (!commandProcessed(p, message))
			GameServer.getChannel(rc.getChannel()).getInterChannelInterface().sendSpouseChat(recipient, p, message);
	}

	private static byte[] writeMapChat(Player p, String message, byte show, boolean gm) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(10
				+ message.length());

		lew.writeShort(ClientSendOps.MAP_CHAT);
		lew.writeInt(p.getId());
		lew.writeBool(gm);
		lew.writeLengthPrefixedString(message);
		lew.writeByte(show);

		return lew.getBytes();
	}

	private static boolean commandProcessed(Player p, String chat) {
		char first = chat.charAt(0);
		if (first != '!' && first != '@')
			return false;
		CommandProcessor.getInstance().process(p, chat);
		return true;
	}

	public enum TextStyle {
		OK_BOX(1),
		LIGHT_BLUE_TEXT_WHITE_BG(2),
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

		public byte getMod() {
			return subType;
		}
	}

	public static byte[] writeStyledChat(TextStyle ts, Player p, String message, boolean megaEar) {
		switch (ts) {
			case OK_BOX:
			case LIGHT_BLUE_TEXT_WHITE_BG:
			case RED_TEXT_CLEAR_BG:
			case LIGHT_BLUE_TEXT_CLEAR_BG:
				return CommonPackets.writeServerMessage(ts.getMod(), p.getName() + " : " + message, p.getClient().getChannel(), megaEar);
			case ORANGE_TEXT_CLEAR_BG:
			case PURPLE_TEXT_CLEAR_BG:
			case PINK_TEXT_CLEAR_BG:
				return CommonPackets.writePrivateChatMessage(ts.getMod(), p.getName(), message);
			case DULL_GREEN_TEXT_CLEAR_BG:
				return CommonPackets.writeSpouseChatMessage(p.getName(), message);
			case WHITE_TEXT_CLEAR_BG:
			case BLACK_TEXT_WHITE_BG:
				return writeMapChat(p, message, (byte) 0, ts.getMod() != 0);
			case BRIGHT_GREEN_TEXT_CLEAR_BG:
				return CommonPackets.writeWhisperMessge(p.getName(), message, p.getClient().getChannel());
			case YELLOW_TEXT_CLEAR_BG:
				return CommonPackets.writeTipMessage(p.getName() + " : " + message);
			default:
				return null;
		}
	}
}
