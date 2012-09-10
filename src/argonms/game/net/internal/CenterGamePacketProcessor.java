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

package argonms.game.net.internal;

import argonms.common.net.internal.CenterRemoteOps;
import argonms.common.net.internal.CenterRemotePacketProcessor;
import argonms.common.net.internal.RemoteCenterInterface;
import argonms.common.util.input.LittleEndianByteArrayReader;
import argonms.common.util.input.LittleEndianReader;
import argonms.game.GameServer;
import argonms.game.net.WorldChannel;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Processes packet sent from the center server and received at the game server.
 * @author GoldenKevin
 */
public class CenterGamePacketProcessor extends CenterRemotePacketProcessor {
	private static final Logger LOG = Logger.getLogger(CenterGamePacketProcessor.class.getName());

	private final GameServer local;

	public CenterGamePacketProcessor(GameServer gs) {
		local = gs;
	}

	@Override
	public void process(LittleEndianReader packet, RemoteCenterInterface r) {
		switch (packet.readByte()) {
			case CenterRemoteOps.AUTH_RESPONSE:
				processAuthResponse(packet, r.getLocalServer());
				break;
			case CenterRemoteOps.PING:
				r.getSession().send(pongMessage());
				break;
			case CenterRemoteOps.PONG:
				r.getSession().receivedPong();
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
			case CenterRemoteOps.CHANNEL_PORT_CHANGE:
				processChannelPortChange(packet);
				break;
			case CenterRemoteOps.INTER_CHANNEL_RELAY:
				processInterChannelMessage(packet);
				break;
			case CenterRemoteOps.PARTY_SYNCHRONIZATION:
				processPartySynchronization(packet);
				break;
			case CenterRemoteOps.CHATROOM_CREATED:
				processChatroomCreated(packet);
				break;
			case CenterRemoteOps.CHATROOM_ROOM_CHANGED:
				processAssignChatroom(packet);
				break;
			case CenterRemoteOps.CHATROOM_SLOT_CHANGED:
				processChatroomAvatar(packet);
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
		local.registerGame(serverId, host, ports);
	}

	private void processGameDisconnected(LittleEndianReader packet) {
		byte serverId = packet.readByte();
		packet.readByte(); //world - we don't need it
		local.unregisterGame(serverId);
	}

	private void processShopConnected(LittleEndianReader packet) {
		String host = packet.readLengthPrefixedString();
		int port = packet.readInt();
		local.registerShop(host, port);
	}

	private void processShopDisconnected(LittleEndianReader packet) {
		local.unregisterShop();
	}

	private void processChannelPortChange(LittleEndianReader packet) {
		/*byte world = */packet.readByte();
		byte channel = packet.readByte();
		int newPort = packet.readInt();
		local.updateRemoteChannelPort(channel, newPort);
	}

	private void processInterChannelMessage(LittleEndianReader packet) {
		byte channel = packet.readByte();
		if (channel == (byte) -1) { //all channels
			byte[] array = packet.readBytes(packet.available());
			for (WorldChannel ch : local.getChannels().values())
				ch.getInterChannelInterface().receivedPacket(new LittleEndianByteArrayReader(array));
		} else {
			GameServer.getChannel(channel).getInterChannelInterface().receivedPacket(packet);
		}
	}

	private void processPartySynchronization(LittleEndianReader packet) {
		byte channel = packet.readByte();
		GameServer.getChannel(channel).getInterChannelInterface().receivedPartyPacket(packet);
	}

	private void processChatroomCreated(LittleEndianReader packet) {
		byte channel = packet.readByte();
		GameServer.getChannel(channel).getInterChannelInterface().receivedChatroomCreated(packet);
	}

	private void processAssignChatroom(LittleEndianReader packet) {
		byte channel = packet.readByte();
		GameServer.getChannel(channel).getInterChannelInterface().receivedChatroomAssignment(packet);
	}

	private void processChatroomAvatar(LittleEndianReader packet) {
		byte channel = packet.readByte();
		GameServer.getChannel(channel).getInterChannelInterface().receivedChatroomAvatarChanged(packet);
	}
}
