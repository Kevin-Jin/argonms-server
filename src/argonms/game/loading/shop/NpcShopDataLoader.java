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

package argonms.game.loading.shop;

import argonms.common.loading.DataFileType;
import java.util.HashMap;
import java.util.Map;

//FIXME: Thread safety for concurrent read/writes (if we're not preloading)
/**
 *
 * @author GoldenKevin
 */
public abstract class NpcShopDataLoader {
	private static NpcShopDataLoader instance;

	protected Map<Integer, NpcShop> loadedShops;
	protected Map<Integer, Integer> npcToShop;

	protected NpcShopDataLoader() {
		loadedShops = new HashMap<Integer, NpcShop>();
		npcToShop = new HashMap<Integer, Integer>();
	}

	protected abstract int load(int npcid);

	public abstract boolean loadAll();

	public abstract boolean canLoad(int npcid);

	public NpcShop getShopByNpc(int id) {
		Integer shopId = npcToShop.get(Integer.valueOf(id));
		if (shopId == null || shopId.intValue() != 0 && !loadedShops.containsKey(shopId))
			shopId = Integer.valueOf(load(id));
		if (shopId.intValue() == 0) {
			if (!npcToShop.containsKey(shopId))
				npcToShop.put(shopId, Integer.valueOf(0));
			return null; //no shop for this NPC
		}
		return loadedShops.get(shopId);
	}

	public static void setInstance(DataFileType wzType, String wzPath) {
		if (instance == null) {
			switch (wzType) {
				case MCDB:
					instance = new McdbNpcShopDataLoader();
					break;
				default:
					instance = new DefaultNpcShopDataLoader();
					break;
			}
		}
	}

	public static NpcShopDataLoader getInstance() {
		return instance;
	}
}
