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

package argonms.game.net.internal;

import argonms.common.LocalServer;
import argonms.common.net.internal.RemoteCenterInterface;
import argonms.common.net.internal.RemoteCenterOps;
import argonms.common.util.output.LittleEndianByteArrayWriter;
import argonms.game.GameServer;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 *
 * @author GoldenKevin
 */
public class GameCenterInterface extends RemoteCenterInterface {
	private final GameServer local;
	private final byte serverId;
	private final byte world;

	public GameCenterInterface(byte serverId, byte world, GameServer gs) {
		super(new CenterGamePacketProcessor(gs));
		this.local = gs;
		this.serverId = serverId;
		this.world = world;
	}

	@Override
	public byte getServerId() {
		return serverId;
	}

	public byte getWorld() {
		return world;
	}

	@Override
	protected byte[] auth(String pwd) {
		Set<Byte> channels = local.getChannels().keySet();
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

	@Override
	public void serverReady() {
		getSession().send(serverReady(local.getExternalIp(), world, local.getClientPorts()));
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

	@Override
	public LocalServer getLocalServer() {
		return local;
	}
}
