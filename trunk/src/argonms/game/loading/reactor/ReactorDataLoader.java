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

package argonms.game.loading.reactor;

import argonms.common.loading.DataFileType;
import java.util.HashMap;
import java.util.Map;

//FIXME: Thread safety for concurrent read/writes (if we're not preloading)
/**
 *
 * @author GoldenKevin
 */
public abstract class ReactorDataLoader {
	private static ReactorDataLoader instance;

	protected Map<Integer, ReactorStats> reactorStats;

	protected ReactorDataLoader() {
		reactorStats = new HashMap<Integer, ReactorStats>();
	}

	protected abstract void load(int reactorid);

	public abstract boolean loadAll();

	public abstract boolean canLoad(int reactorid);

	public ReactorStats getReactorStats(int id) {
		Integer oId;
		ReactorStats stats;
		do {
			oId = Integer.valueOf(id);
			if (!reactorStats.containsKey(oId))
				load(id);
			stats = reactorStats.get(oId);
			id = stats != null ? stats.getLink() : 0;
		} while (id != 0);
		return stats;
	}

	public static void setInstance(DataFileType wzType, String wzPath) {
		if (instance == null) {
			switch (wzType) {
				case KVJ:
					instance = new KvjReactorDataLoader(wzPath);
					break;
				case MCDB:
					instance = new McdbReactorDataLoader();
					break;
			}
		}
	}

	public static ReactorDataLoader getInstance() {
		return instance;
	}
}
