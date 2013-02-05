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
import argonms.common.net.internal.RemoteCenterOps;
import argonms.common.util.input.LittleEndianReader;

/**
 * Processes packet sent from the shop server and received at the center
 * server.
 * @author GoldenKevin
 */
public class ShopCenterPacketProcessor extends RemoteCenterPacketProcessor {
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
		}
	}

	private void serverOnline(LittleEndianReader packet)  {
		r.setHost(packet.readLengthPrefixedString());
		int port = packet.readInt();
		r.setClientPort(port);
		CenterServer.getInstance().registerShop(r);
	}
}
