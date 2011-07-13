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

/**
 *
 * @author GoldenKevin
 */
public class CenterLoginInterface extends CenterRemoteInterface {
	private String host;
	private int port;
	private LoginCenterPacketProcessor pp;

	public CenterLoginInterface(CenterRemoteSession session) {
		super(session);
	}

	@Override
	public void makePacketProcessor() {
		this.pp = new LoginCenterPacketProcessor(this);
	}

	@Override
	public RemoteCenterPacketProcessor getPacketProcessor() {
		return pp;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public void setClientPort(int port) {
		this.port = port;
	}

	public String getHost() {
		return host;
	}

	public int getClientPort() {
		return port;
	}

	@Override
	public void disconnected() {
		if (online) {
			online = false;
			disconnecting = true;
			CenterServer.getInstance().loginDisconnected();
		}
	}
}
