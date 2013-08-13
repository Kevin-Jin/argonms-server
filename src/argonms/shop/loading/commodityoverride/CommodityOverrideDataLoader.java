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

package argonms.shop.loading.commodityoverride;

import argonms.common.loading.DataFileType;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author GoldenKevin
 */
public abstract class CommodityOverrideDataLoader {
	private static CommodityOverrideDataLoader instance;

	protected final Map<Integer, Map<CommodityMod, Object>> mods;

	protected CommodityOverrideDataLoader() {
		mods = new HashMap<Integer, Map<CommodityMod, Object>>();
	}

	public abstract boolean loadAll();

	public Map<CommodityMod, Object> getAllModifications(int serialNumber) {
		return mods.get(Integer.valueOf(serialNumber));
	}

	public Map<Integer, Map<CommodityMod, Object>> getAllModifications() {
		return mods;
	}

	public static void setInstance(DataFileType wzType, String wzPath) {
		if (instance == null) {
			switch (wzType) {
				default:
					instance = new JsonCommodityOverrideDataLoader();
					break;
			}
		}
	}

	public static CommodityOverrideDataLoader getInstance() {
		return instance;
	}
}
