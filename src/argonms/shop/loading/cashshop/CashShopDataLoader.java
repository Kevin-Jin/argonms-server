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

package argonms.shop.loading.cashshop;

import argonms.common.loading.DataFileType;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author GoldenKevin
 */
public abstract class CashShopDataLoader {
	private static CashShopDataLoader instance;

	protected final Map<Integer, Commodity> commodities;
	protected final Map<Integer, int[]> packages;

	protected CashShopDataLoader() {
		commodities = new HashMap<Integer, Commodity>();
		packages = new HashMap<Integer, int[]>();
	}

	public abstract boolean loadAll();

	public Commodity getCommodity(int serialNumber) {
		return commodities.get(Integer.valueOf(serialNumber));
	}

	public void setCommodity(int serialNumber, Commodity c) {
		commodities.put(Integer.valueOf(serialNumber), c);
	}

	public int[] getSnsForPackage(int packageNumber) {
		return packages.get(Integer.valueOf(packageNumber));
	}

	public static void setInstance(DataFileType wzType, String wzPath) {
		if (instance == null) {
			switch (wzType) {
				case KVJ:
					instance = new KvjCashShopDataLoader(wzPath);
					break;
				default:
					instance = new DefaultCashShopDataLoader();
					break;
			}
		}
	}

	public static CashShopDataLoader getInstance() {
		return instance;
	}
}
