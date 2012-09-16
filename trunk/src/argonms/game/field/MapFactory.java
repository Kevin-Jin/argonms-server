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

package argonms.game.field;

import argonms.game.loading.map.MapDataLoader;
import argonms.game.loading.map.MapStats;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 *
 * @author GoldenKevin
 */
public class MapFactory {
	private ConcurrentMap<Integer, GameMap> maps;
	private Set<GameMap> instanceMaps;

	public MapFactory() {
		maps = new ConcurrentHashMap<Integer, GameMap>();
		instanceMaps = Collections.newSetFromMap(new ConcurrentHashMap<GameMap, Boolean>());
	}

	private GameMap newMap(int mapId) {
		MapStats stats = MapDataLoader.getInstance().getMapStats(mapId);
		if (stats == null)
			return null;
		else
			return new GameMap(stats);
	}

	public GameMap getMap(int mapid) {
		Integer oId = Integer.valueOf(mapid);
		GameMap map = maps.get(oId);
		if (map == null) {
			map = newMap(mapid);
			GameMap existing = maps.putIfAbsent(oId, map);
			if (existing != null)
				//some other thread was loading the same map and beat us in
				//instantiating it. no big deal, just use their instance instead
				map = existing;
		}
		return map;
	}

	public GameMap makeInstanceMap(int mapId) {
		GameMap map = newMap(mapId);
		instanceMaps.add(map);
		return map;
	}

	public void destroyInstanceMap(GameMap map) {
		instanceMaps.remove(map);
	}

	public void clear() {
		maps.clear();
	}

	public Collection<GameMap> getMaps() {
		return Collections.unmodifiableCollection(maps.values());
	}

	public Set<GameMap> getInstanceMaps() {
		return Collections.unmodifiableSet(instanceMaps);
	}
}
