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

package argonms.center.net.internal;

import argonms.common.ServerType;
import argonms.common.net.SessionDataModel;

/**
 *
 * @author GoldenKevin
 */
public abstract class CenterRemoteInterface implements SessionDataModel {
	private final CenterRemoteSession session;
	protected boolean online;
	protected boolean disconnecting;

	public CenterRemoteInterface(CenterRemoteSession session) {
		this.session = session;
	}

	@Override
	public CenterRemoteSession getSession() {
		return session;
	}

	public boolean isStartingUp() {
		return !online && !disconnecting;
	}

	public boolean isOnline() {
		return online;
	}

	public boolean isShuttingDown() {
		return disconnecting;
	}

	public abstract void makePacketProcessor();

	public abstract RemoteCenterPacketProcessor getPacketProcessor();

	public abstract String getServerName();

	public void serverOnline() {
		online = true;
	}

	/**
	 * DO NOT USE THIS METHOD TO FORCE THE CLIENT TO CLOSE ITSELF. USE
	 * getSession().close() INSTEAD.
	 */
	@Override
	public abstract void disconnected();

	public static CenterRemoteInterface makeByServerId(byte serverId, CenterRemoteSession session) {
		if (ServerType.isGame(serverId))
			return new CenterGameInterface(session, serverId);
		if (ServerType.isLogin(serverId))
			return new CenterLoginInterface(session);
		if (ServerType.isShop(serverId))
			return new CenterShopInterface(session);
		return null;
	}
}
