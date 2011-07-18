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

package argonms.login.net.internal;

import argonms.common.LocalServer;
import argonms.common.ServerType;
import argonms.common.net.internal.RemoteCenterInterface;
import argonms.common.net.internal.RemoteCenterOps;
import argonms.common.util.output.LittleEndianByteArrayWriter;
import argonms.login.LoginServer;

/**
 * Provides an interface for the login server between it and the center server.
 * 
 * @author GoldenKevin
 */
public class LoginCenterInterface extends RemoteCenterInterface {
	private LoginServer local;

	public LoginCenterInterface(String password, LoginServer ls) {
		super(password, new CenterLoginPacketProcessor(ls));
		this.local = ls;
	}

	@Override
	protected byte getServerId() {
		return ServerType.LOGIN;
	}

	@Override
	public void serverReady() {
		send(serverReady(local.getExternalIp(), local.getClientPort()));
	}

	private static byte[] serverReady(String ip, int port) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(7 + ip.length());

		lew.writeByte(RemoteCenterOps.ONLINE);
		lew.writeLengthPrefixedString(ip);
		lew.writeInt(port);

		return lew.getBytes();
	}

	@Override
	public LocalServer getLocalServer() {
		return local;
	}
}
