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
import argonms.tools.Pair;
import argonms.tools.input.LittleEndianReader;
import argonms.tools.output.LittleEndianByteArrayWriter;
import java.net.UnknownHostException;

/**
 *
 * @author GoldenKevin
 */
public class GameGoToHandler {
	public static void handleChangeChannel(LittleEndianReader packet, RemoteClient rc) {
		byte destCh = (byte) (packet.readByte() + 1);
		byte curCh = rc.getChannel();
		byte[] host;
		int port;
		try {
			Pair<byte[], Integer> hostAndPort = GameServer.getChannel(curCh).getInterChannelInterface().getChannelHost(destCh);
			host = hostAndPort.getLeft();
			port = hostAndPort.getRight().intValue();
		} catch (UnknownHostException e) {
			host = null;
			port = -1;
		}
		if (host != null && port != -1) {
			((GameClient) rc).getPlayer().prepareChannelChange();
			rc.getSession().send(writeNewHost(host, port));
			rc.getSession().close();
		} else {
			//send them an error!
		}
	}

	public static void handleMapChange(LittleEndianReader packet, RemoteClient rc) {
		/*byte reason = */packet.readByte(); // 1 = died, 2 = entered portal
		int dest = packet.readInt();
		String portalName = packet.readLengthPrefixedString();
		Player p = ((GameClient) rc).getPlayer();
		if (dest != -1) { //map client command - only let GMs use them.
			if (p.getPrivilegeLevel() > UserPrivileges.USER)
				p.changeMap(dest);
		} else {
			p.getMap().enterPortal(p, portalName);
		}
	}

	public static void handleEnteredSpecialPortal(LittleEndianReader packet, RemoteClient rc) {
		packet.readByte();
		String portalName = packet.readLengthPrefixedString();
		packet.readByte();
		packet.readByte(); //sourcefm?

		Player p = ((GameClient) rc).getPlayer();
		p.getMap().enterPortal(p, portalName);
	}

	public static void handleWarpCs(LittleEndianReader packet, RemoteClient rc) {
		
	}

	private static byte[] writeNewHost(byte[] host, int port) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(9);
		lew.writeShort(ClientSendOps.GAME_HOST_ADDRESS);
		lew.writeBool(true);
		lew.writeBytes(host);
		lew.writeShort((short) port);
		return lew.getBytes();
	}
}
