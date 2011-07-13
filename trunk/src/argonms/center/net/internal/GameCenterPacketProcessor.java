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

package argonms.center.net.internal;

import argonms.center.CenterServer;
import argonms.common.ServerType;
import argonms.common.net.internal.CenterRemoteOps;
import argonms.common.net.internal.RemoteCenterOps;
import argonms.common.tools.input.LittleEndianReader;
import argonms.common.tools.output.LittleEndianByteArrayWriter;
import java.util.HashMap;
import java.util.Map;

/**
 * Processes packet sent from the game server and received at the center
 * server.
 * @author GoldenKevin
 */
public class GameCenterPacketProcessor extends RemoteCenterPacketProcessor {
	private CenterGameInterface r;

	public GameCenterPacketProcessor(CenterGameInterface r) {
		this.r = r;
	}

	@Override
	public void process(LittleEndianReader packet) {
		switch (packet.readByte()) {
			case RemoteCenterOps.ONLINE:
				serverOnline(packet);
				break;
			case RemoteCenterOps.POPULATION_CHANGED:
				processPopulationChange(packet);
				break;
			case RemoteCenterOps.MODIFY_CHANNEL_PORT:
				processChannelPortChange(packet);
				break;
			case RemoteCenterOps.INTER_CHANNEL:
				processInterChannel(packet);
				break;
			case RemoteCenterOps.INTER_CHANNEL_ALL:
				processInterChannelAll(packet);
				break;
		}
	}

	private void serverOnline(LittleEndianReader packet)  {
		r.setHost(packet.readLengthPrefixedString());
		byte world = packet.readByte();
		byte size = packet.readByte();
		Map<Byte, Integer> clientPorts = new HashMap<Byte, Integer>(size);
		for (int i = 0; i < size; i++)
			clientPorts.put(Byte.valueOf(packet.readByte()), packet.readInt());
		r.setWorld(world);
		r.setClientPorts(clientPorts);
		CenterServer.getInstance().gameConnected(r);
	}

	private void processPopulationChange(LittleEndianReader packet) {
		byte channel = packet.readByte();
		short now = packet.readShort();
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(4);
		lew.writeByte(CenterRemoteOps.CHANGE_POPULATION);
		lew.writeByte(r.getWorld());
		lew.writeByte(channel);
		lew.writeShort(now);
		CenterServer.getInstance().sendToLogin(lew.getBytes());
	}

	private void processChannelPortChange(LittleEndianReader packet) {
		byte world = packet.readByte();
		byte channel = packet.readByte();
		int newPort = packet.readInt();
		CenterServer.getInstance().channelPortChanged(world, r.getServerId(), channel, newPort);
	}

	private void processInterChannel(LittleEndianReader packet) {
		byte world = packet.readByte();
		byte channel = packet.readByte();
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(packet.available() + 2);
		lew.writeByte(CenterRemoteOps.INTER_CHANNEL_RELAY);
		lew.writeByte(channel);
		lew.writeBytes(packet.readBytes(packet.available()));
		byte[] message = lew.getBytes();
		for (CenterGameInterface cgi : CenterServer.getInstance().getAllServersOfWorld(world, ServerType.UNDEFINED))
			if (cgi.isOnline() && cgi.getChannels().contains(Byte.valueOf(channel)))
				cgi.getSession().send(message);
	}

	private void processInterChannelAll(LittleEndianReader packet) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(packet.available() + 2);
		lew.writeByte(CenterRemoteOps.INTER_CHANNEL_RELAY);
		lew.writeByte((byte) -1);
		lew.writeBytes(packet.readBytes(packet.available()));
		byte[] message = lew.getBytes();
		for (CenterGameInterface cgi : CenterServer.getInstance().getAllServersOfWorld(r.getWorld(), r.getServerId()))
			if (cgi.isOnline())
				cgi.getSession().send(message);
	}
}
