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

package argonms.game.loading.mob;

import argonms.common.loading.DataFileType;
import java.util.HashMap;
import java.util.Map;

//FIXME: Thread safety for concurrent read/writes (if we're not preloading)
/**
 *
 * @author GoldenKevin
 */
public abstract class MobDataLoader {
	private static MobDataLoader instance;

	protected final Map<Integer, MobStats> mobStats;

	protected MobDataLoader() {
		mobStats = new HashMap<Integer, MobStats>();
	}

	protected abstract void load(int mobid);

	public abstract boolean loadAll();

	public abstract boolean canLoad(int mobid);

	public MobStats getMobStats(int id) {
		Integer oId;
		MobStats stats;
		//do {
			oId = Integer.valueOf(id);
			if (!mobStats.containsKey(oId))
				load(id);
			stats = mobStats.get(oId);
			//id = stats != null ? stats.getLink() : 0;
		//} while (id != 0);
		return stats;
	}

	public static void setInstance(DataFileType wzType, String wzPath) {
		if (instance == null) {
			switch (wzType) {
				case KVJ:
					instance = new KvjMobDataLoader(wzPath);
					break;
				case MCDB:
					instance = new McdbMobDataLoader();
					break;
			}
		}
	}

	public static MobDataLoader getInstance() {
		return instance;
	}
}
