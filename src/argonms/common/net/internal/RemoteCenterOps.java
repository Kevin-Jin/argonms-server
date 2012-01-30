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
 * Opcodes for packets sent from a remote server and received on the center
 * server.
 * @author GoldenKevin
 */
public class RemoteCenterOps {
	public static final byte
		AUTH = 0x00,
		PING = 0x01,
		PONG = 0x02,
		ONLINE = 0x03,
		POPULATION_CHANGED = 0x04,
		MODIFY_CHANNEL_PORT = 0x05,
		INTER_CHANNEL = 0x06,
		INTER_CHANNEL_ALL = 0x07
	;

	private RemoteCenterOps() {
		//uninstantiable...
	}
}
