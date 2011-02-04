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

package argonms.shop;

import argonms.ServerType;
import argonms.character.Player;
import argonms.net.client.RemoteClient;

//world and channel are needed to redirect player to game server when they exit
//world is easy (in characters db table), but how do we get channel...?
/**
 *
 * @author GoldenKevin
 */
public class ShopClient extends RemoteClient {
	private Player player;

	public Player getPlayer() {
		return player;
	}

	public byte getServerType() {
		return ServerType.SHOP;
	}

	public void disconnect() {
		updateState(STATUS_NOTLOGGEDIN);
	}
}
