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

package argonms.loading.shop;

import java.util.HashMap;
import java.util.Map;

import argonms.loading.DataFileType;

/**
 *
 * @author GoldenKevin
 */
public abstract class NpcShopDataLoader {
	private static NpcShopDataLoader instance;

	protected Map<Integer, NpcShopStock> loadedShops;

	protected NpcShopDataLoader() {
		loadedShops = new HashMap<Integer, NpcShopStock>();
	}

	protected abstract void load(int npcid);

	public abstract boolean loadAll();

	public abstract boolean canLoad(int npcid);

	public NpcShopStock getShopByNpc(int id) {
		Integer oId = Integer.valueOf(id);
		NpcShopStock stats;
		if (!loadedShops.containsKey(oId))
			load(id);
		stats = loadedShops.get(oId);
		return stats;
	}

	public static void setInstance(DataFileType wzType, String wzPath) {
		if (instance == null) {
			switch (wzType) {
				case MCDB:
					instance = new McdbNpcShopDataLoader();
					break;
				default:
					instance = new DbNpcShopDataLoader();
					break;
			}
		}
	}

	public static NpcShopDataLoader getInstance() {
		return instance;
	}
}
