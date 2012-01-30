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

package argonms.common.net.internal;

/**
 * Opcodes for packets sent from the center server and received on a remote
 * server.
 * @author GoldenKevin
 */
public class CenterRemoteOps {
	public static final byte
		AUTH_RESPONSE = 0x00,
		PING = 0x01,
		PONG = 0x02,
		GAME_CONNECTED = 0x03,
		SHOP_CONNECTED = 0x04,
		GAME_DISCONNECTED = 0x05,
		SHOP_DISCONNECTED = 0x06,
		CHANGE_POPULATION = 0x07,
		CHANNEL_PORT_CHANGE = 0x08,
		REMOTE_CHANNEL_PORT = 0x09,
		INTER_CHANNEL_RELAY = 0x0A
	;

	private CenterRemoteOps() {
		//uninstantiable...
	}
}
