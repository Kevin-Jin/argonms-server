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

package argonms.game;

import argonms.net.server.CenterRemoteOps;
import argonms.net.server.CenterRemotePacketProcessor;
import argonms.net.server.RemoteCenterInterface;
import argonms.tools.input.LittleEndianReader;

/**
 * Processes packet sent from the center server and received at the game server.
 * @author GoldenKevin
 */
public class CenterGamePacketProcessor extends CenterRemotePacketProcessor {
	private GameServer local;

	public CenterGamePacketProcessor(GameServer gs) {
		local = gs;
	}

	public void process(LittleEndianReader packet, RemoteCenterInterface r) {
		switch (packet.readByte()) {
			case CenterRemoteOps.AUTH_RESPONSE:
				processAuthResponse(packet, r.getLocalServer());
				break;
			case CenterRemoteOps.SHOP_CONNECTED:
				processShopConnected(packet);
				break;
			case CenterRemoteOps.SHOP_DISCONNECTED:
				processShopDisconnected(packet);
				break;
		}
	}

	private void processShopConnected(LittleEndianReader packet) {
		String host = packet.readLengthPrefixedString();
		int port = packet.readInt();
		local.shopConnected(host, port);
	}

	private void processShopDisconnected(LittleEndianReader packet) {
		local.shopDisconnected();
	}
}
