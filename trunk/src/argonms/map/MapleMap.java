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

package argonms.map;

import argonms.character.Player;
import argonms.loading.map.MapStats;
import argonms.loading.map.Portal;
import java.awt.Point;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 *
 * @author GoldenKevin
 */
public class MapleMap {
	private final MapStats stats;
	private final Map<Integer, MapObject> objects = new LinkedHashMap<Integer, MapObject>();
	private final Set<Player> players = new LinkedHashSet<Player>();

	protected MapleMap(MapStats stats) {
		this.stats = stats;
		//load reactors, life, etc. from stats
	}

	public int getMapId() {
		return stats.getMapId();
	}

	public int getReturnMap() {
		return stats.getReturnMap();
	}

	public int getForcedReturnMap() {
		return stats.getForcedReturn();
	}

	public byte nearestSpawnPoint(Point from) {
		byte closest = 0;
		double shortestDistance = Double.POSITIVE_INFINITY;
		for (Entry<Byte, Portal> entry : stats.getPortals().entrySet()) {
			Portal portal = entry.getValue();
			double distance = portal.getPosition().distanceSq(from);
			if (portal.getPortalType() >= 0 && portal.getPortalType() <= 2 && distance < shortestDistance) {
				closest = entry.getKey().byteValue();
				shortestDistance = distance;
			}
		}
		return closest;
	}

	public void addPlayer(Player p) {
		synchronized (players) {
			players.add(p);
		}
		synchronized (objects) {
			objects.put(Integer.valueOf(p.getId()), p);
		}
	}
}
