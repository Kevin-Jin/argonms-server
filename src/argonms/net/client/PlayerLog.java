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
import java.util.HashMap;
import java.util.Map;

//Is thread-safety really a problem in this class?
/**
 *
 * @author GoldenKevin
 */
public class PlayerLog {
	private final Map<String, Player> nameToPlayerMapping;
	private final Map<Integer, Player> idToPlayerMapping;

	public PlayerLog() {
		this.nameToPlayerMapping = new HashMap<String, Player>();
		this.idToPlayerMapping = new HashMap<Integer, Player>();
	}

	public void addPlayer(Player p) {
		synchronized(nameToPlayerMapping) {
			nameToPlayerMapping.put(p.getName(), p);
		}
		synchronized(idToPlayerMapping) {
			idToPlayerMapping.put(Integer.valueOf(p.getId()), p);
		}
	}

	public void deletePlayer(Player p) {
		synchronized(nameToPlayerMapping) {
			nameToPlayerMapping.remove(p.getName());
		}
		synchronized(idToPlayerMapping) {
			idToPlayerMapping.remove(Integer.valueOf(p.getId()));
		}
	}

	public Player getPlayer(int id) {
		synchronized(idToPlayerMapping) {
			return idToPlayerMapping.get(Integer.valueOf(id));
		}
	}

	public Player getPlayer(String name) {
		synchronized(nameToPlayerMapping) {
			return nameToPlayerMapping.get(name);
		}
	}
}
