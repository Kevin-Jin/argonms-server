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

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import argonms.net.internal.CenterRemoteOps;
import argonms.net.internal.CenterRemotePacketProcessor;
import argonms.net.internal.RemoteCenterInterface;
import argonms.tools.input.LittleEndianReader;

/**
 * Processes packet sent from the center server and received at the game server.
 * @author GoldenKevin
 */
public class CenterGamePacketProcessor extends CenterRemotePacketProcessor {
	private static final Logger LOG = Logger.getLogger(CenterGamePacketProcessor.class.getName());

	private GameServer local;

	public CenterGamePacketProcessor(GameServer gs) {
		local = gs;
	}

	public void process(LittleEndianReader packet, RemoteCenterInterface r) {
		switch (packet.readByte()) {
			case CenterRemoteOps.AUTH_RESPONSE:
				processAuthResponse(packet, r.getLocalServer());
				break;
			case CenterRemoteOps.GAME_CONNECTED:
				processGameConnected(packet);
				break;
			case CenterRemoteOps.GAME_DISCONNECTED:
				processGameDisconnected(packet);
				break;
			case CenterRemoteOps.SHOP_CONNECTED:
				processShopConnected(packet);
				break;
			case CenterRemoteOps.SHOP_DISCONNECTED:
				processShopDisconnected(packet);
				break;
			case CenterRemoteOps.INTER_CHANNEL_RELAY:
				processInterChannelMessage(packet);
				break;
			default:
				LOG.log(Level.FINE, "Received unhandled interserver packet {0} bytes long:\n{1}", new Object[] { packet.available() + 2, packet });
				break;
		}
	}

	private void processGameConnected(LittleEndianReader packet) {
		byte serverId = packet.readByte();
		packet.readByte(); //world - we don't need it
		String host = packet.readLengthPrefixedString();
		byte size = packet.readByte();
		Map<Byte, Integer> ports = new HashMap<Byte, Integer>(size);
		for (int i = 0; i < size; i++)
			ports.put(Byte.valueOf(packet.readByte()), Integer.valueOf(packet.readInt()));
		local.gameConnected(serverId, host, ports);
	}

	private void processGameDisconnected(LittleEndianReader packet) {
		byte serverId = packet.readByte();
		packet.readByte(); //world - we don't need it
		local.gameDisconnected(serverId);
	}

	private void processShopConnected(LittleEndianReader packet) {
		String host = packet.readLengthPrefixedString();
		int port = packet.readInt();
		local.shopConnected(host, port);
	}

	private void processShopDisconnected(LittleEndianReader packet) {
		local.shopDisconnected();
	}

	private void processInterChannelMessage(LittleEndianReader packet) {
		byte channel = packet.readByte();
		if (channel == (byte) -1) { //all channels
			for (WorldChannel ch : local.getChannels().values())
				ch.getInterChannelInterface().receivedPacket(packet);
		} else {
			GameServer.getChannel(channel).getInterChannelInterface().receivedPacket(packet);
		}
	}
}
