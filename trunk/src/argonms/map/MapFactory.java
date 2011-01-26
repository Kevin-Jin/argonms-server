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

import argonms.loading.map.MapDataLoader;
import argonms.loading.map.MapStats;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author GoldenKevin
 */
public class MapFactory {
	private Map<Integer, MapleMap> maps;

	public MapFactory() {
		this.maps = new HashMap<Integer, MapleMap>();
	}

	public MapleMap getMap(int mapid) {
		Integer oId = Integer.valueOf(mapid);
		if (!maps.containsKey(oId)) {
			MapStats stats = MapDataLoader.getInstance().getMapStats(mapid);
			if (stats == null)
				maps.put(oId, null);
			else
				maps.put(oId, new MapleMap(stats));
		}
		return maps.get(oId);
	}
}
