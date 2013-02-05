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

package argonms.common.net;

import java.net.SocketAddress;

/**
 * Contains the low level networking state of connections of a certain protocol.
 * May also contain 
 * @author GoldenKevin
 */
public interface Session {
	public enum MessageType { HEADER, BODY }

	public SocketAddress getAddress();

	public void send(byte[] b);

	/**
	 * 
	 * @param reason
	 * @return true if successful. False return indicates that the Session may
	 * have already been closed before.
	 */
	public boolean close(String reason);
}
