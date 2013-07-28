/*
 * ArgonMS MapleStory server emulator written in Java
 * Copyright (C) 2011-2013  GoldenKevin
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

package argonms.center.net.internal;

import argonms.center.CenterServer;
import argonms.common.ServerType;
import argonms.common.character.CenterServerSynchronizationOps;
import argonms.common.net.internal.CenterRemoteOps;
import argonms.common.net.internal.RemoteCenterOps;
import argonms.common.util.input.LittleEndianReader;
import argonms.common.util.output.LittleEndianByteArrayWriter;

/**
 * Processes packet sent from the shop server and received at the center
 * server.
 * @author GoldenKevin
 */
public class ShopCenterPacketProcessor extends GameOrShopPacketProcessor {
	private final CenterShopInterface r;

	public ShopCenterPacketProcessor(CenterShopInterface r) {
		this.r = r;
	}

	@Override
	public void process(LittleEndianReader packet) {
		switch (packet.readByte()) {
			case RemoteCenterOps.PING:
				r.getSession().send(pongMessage());
				break;
			case RemoteCenterOps.PONG:
				r.getSession().receivedPong();
				break;
			case RemoteCenterOps.ONLINE:
				serverOnline(packet);
				break;
			case RemoteCenterOps.SHOP_CHANNEL_SHOP_SYNCHRONIZATION:
				processShopChannelSynchronization(packet);
				break;
			case RemoteCenterOps.CENTER_SERVER_SYNCHRONIZATION:
				processCenterServerSynchronization(packet);
				break;
		}
	}

	private void serverOnline(LittleEndianReader packet)  {
		r.setHost(packet.readLengthPrefixedString());
		int port = packet.readInt();
		r.setClientPort(port);
		CenterServer.getInstance().registerShop(r);
	}

	private void processShopChannelSynchronization(LittleEndianReader packet) {
		byte world = packet.readByte();
		byte channel = packet.readByte();
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(packet.available() + 2);
		lew.writeByte(CenterRemoteOps.SHOP_CHANNEL_SHOP_SYNCHRONIZATION);
		lew.writeByte(channel);
		lew.writeBytes(packet.readBytes(packet.available()));
		byte[] message = lew.getBytes();
		for (CenterGameInterface cgi : CenterServer.getInstance().getAllServersOfWorld(world, ServerType.UNDEFINED))
			if (cgi.isOnline() && cgi.getChannels().contains(Byte.valueOf(channel)))
				cgi.getSession().send(message);
	}

	private void processCenterServerSynchronization(LittleEndianReader packet) {
		byte world = packet.readByte();
		switch (packet.readByte()) {
			case CenterServerSynchronizationOps.PARTY_MEMBER_CONNECTED:
				processPartyMemberEnteredChannel(packet, world);
				break;
			case CenterServerSynchronizationOps.PARTY_MEMBER_DISCONNECTED:
				processPartyMemberExitedChannel(packet, world);
				break;
			case CenterServerSynchronizationOps.GUILD_MEMBER_CONNECTED:
				processGuildMemberEnteredChannel(packet, world);
				break;
			case CenterServerSynchronizationOps.GUILD_MEMBER_DISCONNECTED:
				processGuildMemberExitedChannel(packet, world);
				break;
			case CenterServerSynchronizationOps.CHATROOM_REMOVE_PLAYER:
				processCloseChatroom(packet, world, null);
				break;
		}
	}
}
