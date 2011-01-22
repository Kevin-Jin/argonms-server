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

package argonms.login;

import argonms.ServerType;
import argonms.net.server.RemoteCenterInterface;

/**
 * Provides an interface for the login server between it and the center server.
 * 
 * @author GoldenKevin
 */
public class LoginCenterInterface extends RemoteCenterInterface {
	public LoginCenterInterface(String password, LoginServer ls) {
		super(ls, password, new CenterLoginPacketProcessor(ls));
	}

	protected byte getWorld() {
		return ServerType.LOGIN;
	}
}
