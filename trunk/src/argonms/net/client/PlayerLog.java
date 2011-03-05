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

package argonms.net.client;

import argonms.character.Player;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 * @author GoldenKevin
 */
public class PlayerLog {
	private final Map<String, Player> nameToPlayerMapping;
	private final Map<Integer, Player> idToPlayerMapping;
	//private short connected;

	public PlayerLog() {
		this.nameToPlayerMapping = new ConcurrentHashMap<String, Player>();
		this.idToPlayerMapping = new ConcurrentHashMap<Integer, Player>();
		//this.connected = 0;
	}

	public void addPlayer(Player p) {
		nameToPlayerMapping.put(p.getName(), p);
		idToPlayerMapping.put(Integer.valueOf(p.getId()), p);
		//connected++;
	}

	public void deletePlayer(Player p) {
		nameToPlayerMapping.remove(p.getName());
		idToPlayerMapping.remove(Integer.valueOf(p.getId()));
		//connected--;
	}

	public Player getPlayer(int id) {
		return idToPlayerMapping.get(Integer.valueOf(id));
	}

	public Player getPlayer(String name) {
		return nameToPlayerMapping.get(name);
	}

	public short getConnectedCount() {
		//return connected;
		return (short) nameToPlayerMapping.size();
	}
}
