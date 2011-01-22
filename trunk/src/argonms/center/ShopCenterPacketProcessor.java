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

package argonms.center;

import argonms.net.server.RemoteCenterOps;
import argonms.net.server.RemoteCenterPacketProcessor;
import argonms.net.server.CenterRemoteInterface;
import argonms.tools.input.LittleEndianReader;

/**
 * Processes packet sent from the shop server and received at the center
 * server.
 * @author GoldenKevin
 */
public class ShopCenterPacketProcessor extends RemoteCenterPacketProcessor {
	public ShopCenterPacketProcessor(CenterRemoteInterface r, byte world) {
		super(r, world);
	}

	public void process(LittleEndianReader packet) {
		switch (packet.readByte()) {
			case RemoteCenterOps.ONLINE:
				serverOnline(packet);
				break;
		}
	}
}
