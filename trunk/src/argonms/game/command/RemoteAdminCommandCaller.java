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

package argonms.game.command;

import argonms.game.GameServer;
import argonms.game.field.GameMap;

/**
 *
 * @author GoldenKevin
 */
public class RemoteAdminCommandCaller implements CommandCaller {
	//telnet clients will be able to choose channel to apply commands on ("cc", analogous to "cd" on a file system)
	//on telnet login, map and privilegeLevel are loaded from database and channel defaults to 1
	//center server will resend these values to the game server that contains the channel every time a command is made
	//fetch map id from database before each packet sent from the center server to here, in case !map command was used
	private final String name;
	private final byte world;
	private final byte channel;
	private final int map;
	private final byte privilegeLevel;

	public RemoteAdminCommandCaller(String name, byte world, byte channel, int map, byte privilegeLevel) {
		this.name = name;
		this.world = world;
		this.channel = channel;
		this.map = map;
		this.privilegeLevel = privilegeLevel;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public byte getWorld() {
		return world;
	}

	@Override
	public byte getChannel() {
		return channel;
	}

	@Override
	public GameMap getMap() {
		return GameServer.getChannel(channel).getMapFactory().getMap(map);
	}

	@Override
	public boolean isInGame() {
		return false;
	}

	@Override
	public byte getPrivilegeLevel() {
		return privilegeLevel;
	}

	@Override
	public boolean isDisconnected() {
		return false; //TODO:
	}

	public void changeChannel(byte dest) {
		//TODO: send packet back to center server
	}
}
