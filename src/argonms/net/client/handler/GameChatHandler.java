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

package argonms.net.client.handler;

import argonms.UserPrivileges;
import argonms.character.Player;
import argonms.game.GameClient;
import argonms.game.GameServer;
import argonms.net.client.ClientSendOps;
import argonms.net.client.RemoteClient;
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
		p.getMap().sendToAll(writeMapChat(p, message, show));
	}

	public static void handlePrivateChat(LittleEndianReader packet, RemoteClient rc) {
		byte type = packet.readByte(); // 0 for buddys, 1 for partys, 2 for guilds
		byte numRecipients = packet.readByte();
		int[] recipients = new int[numRecipients];
		for (byte i = 0; i < numRecipients; i++)
			recipients[i] = packet.readInt();
		String message = packet.readLengthPrefixedString();
		Player p = ((GameClient) rc).getPlayer();
		GameServer.getChannel(rc.getChannel()).getInterChannelInterface().sendPrivateChat(type, recipients, p, message);
	}

	public static void handleSpouseChat(LittleEndianReader packet, RemoteClient rc) {
		String recipient = packet.readLengthPrefixedString();
		String msg = packet.readLengthPrefixedString();
		Player p = ((GameClient) rc).getPlayer();
		GameServer.getChannel(rc.getChannel()).getInterChannelInterface().sendSpouseChat(recipient, p, msg);
	}

	private static byte[] writeMapChat(Player p, String message, byte show) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter();

		lew.writeShort(ClientSendOps.MAP_CHAT);
		lew.writeInt(p.getId());
		lew.writeBool(p.getPrivilegeLevel() >= UserPrivileges.JUNIOR_GM);
		lew.writeLengthPrefixedString(message);
		lew.writeByte(show);

		return lew.getBytes();
	}
}
