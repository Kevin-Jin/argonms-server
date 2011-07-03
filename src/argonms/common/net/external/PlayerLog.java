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

package argonms.common.net.external;

import argonms.common.character.Player;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 * @author GoldenKevin
 */
public class PlayerLog<T extends Player> {
	private final Map<String, T> nameToPlayerMapping;
	private final Map<Integer, T> idToPlayerMapping;

	public PlayerLog() {
		this.nameToPlayerMapping = new ConcurrentHashMap<String, T>();
		this.idToPlayerMapping = new ConcurrentHashMap<Integer, T>();
	}

	public void addPlayer(T p) {
		nameToPlayerMapping.put(p.getName().toLowerCase(), p);
		idToPlayerMapping.put(Integer.valueOf(p.getDataId()), p);
	}

	public void deletePlayer(T p) {
		nameToPlayerMapping.remove(p.getName());
		idToPlayerMapping.remove(Integer.valueOf(p.getDataId()));
	}

	public T getPlayer(int id) {
		return idToPlayerMapping.get(Integer.valueOf(id));
	}

	public T getPlayer(String name) {
		return nameToPlayerMapping.get(name.toLowerCase());
	}

	public short getConnectedCount() {
		return (short) nameToPlayerMapping.size();
	}
}
