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

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import argonms.LocalServer;
import argonms.net.server.RemoteCenterInterface;
import argonms.net.server.RemoteCenterOps;
import argonms.tools.output.LittleEndianByteArrayWriter;

/**
 *
 * @author GoldenKevin
 */
public class GameCenterInterface extends RemoteCenterInterface {
	private GameServer local;
	private byte serverId;
	private byte world;

	public GameCenterInterface(byte serverId, byte world, String password, GameServer gs) {
		super(password, new CenterGamePacketProcessor(gs));
		this.local = gs;
		this.serverId = serverId;
		this.world = world;
	}

	public byte getServerId() {
		return serverId;
	}

	public byte getWorld() {
		return world;
	}

	protected void init() {
		send(auth(serverId, getInterserverPwd(), world, local.getChannels().keySet()));
	}

	public void serverReady() {
		send(serverReady(local.getExternalIp(), world, local.getClientPorts()));
	}

	private static byte[] auth(byte serverId, String pwd, byte world, Set<Byte> channels) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(6
				+ pwd.length() + channels.size());

		lew.writeByte(RemoteCenterOps.AUTH);
		lew.writeByte(serverId);
		lew.writeLengthPrefixedString(pwd);
		lew.writeByte(world);
		lew.writeByte((byte) channels.size());
		for (Byte ch : channels)
			lew.writeByte(ch.byteValue());

		return lew.getBytes();
	}

	private static byte[] serverReady(String ip, byte world, Map<Byte, Integer> ports) {
		byte length = (byte) ports.size();

		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(5 + ip.length() + 5 * length);

		lew.writeByte(RemoteCenterOps.ONLINE);
		lew.writeLengthPrefixedString(ip);
		lew.writeByte(world);
		lew.writeByte(length);
		for (Entry<Byte, Integer> entry : ports.entrySet()) {
			lew.writeByte(entry.getKey().byteValue());
			lew.writeInt(entry.getValue().intValue());
		}

		return lew.getBytes();
	}

	public LocalServer getLocalServer() {
		return local;
	}
}
