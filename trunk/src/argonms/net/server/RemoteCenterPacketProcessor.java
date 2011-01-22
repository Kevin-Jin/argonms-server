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

package argonms.net.server;

import argonms.ServerType;
import argonms.center.CenterServer;
import argonms.center.GameCenterPacketProcessor;
import argonms.center.LoginCenterPacketProcessor;
import argonms.center.ShopCenterPacketProcessor;
import argonms.tools.input.LittleEndianReader;

/**
 *
 * @author GoldenKevin
 */
public abstract class RemoteCenterPacketProcessor {
	protected CenterRemoteInterface r;
	protected byte world;

	public RemoteCenterPacketProcessor(CenterRemoteInterface r, byte world) {
		this.r = r;
		this.world = world;
	}

	public static RemoteCenterPacketProcessor getProcessor(CenterRemoteInterface r, byte world) {
		switch (world) {
			case ServerType.LOGIN:
				return new LoginCenterPacketProcessor(r, world);
			case ServerType.SHOP:
				return new ShopCenterPacketProcessor(r, world);
			default:
				if (world >= 0)
					return new GameCenterPacketProcessor(r, world);
		}
		return null;
	}

	public abstract void process(LittleEndianReader packet);

	protected void serverOnline(LittleEndianReader packet) {
		r.setHost(packet.readLengthPrefixedString());
		int[] clientPorts = new int[packet.readByte()];
		for (int i = 0; i < clientPorts.length; i++)
			clientPorts[i] = packet.readInt();
		r.setClientPorts(clientPorts);
		CenterServer.getInstance().serverConnected(world, r);
	}
}
