package argonms.loading.map;

import argonms.loading.DataFileType;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author GoldenKevin
 */
public abstract class MapDataLoader {
	private static MapDataLoader instance;

	protected Map<Integer, MapStats> mapStats;

	protected MapDataLoader() {
		mapStats = new HashMap<Integer, MapStats>();
	}

	protected abstract void load(int mapid);

	public abstract boolean loadAll();

	public MapStats getMapStats(int id) {
		Integer oId;
		MapStats stats;
		//do {
			oId = Integer.valueOf(id);
			if (!mapStats.containsKey(oId))
				load(id);
			stats = mapStats.get(oId);
			//id = stats != null ? stats.getLink() : 0;
		//} while (id != 0);
		return stats;
	}

	public static MapDataLoader setInstance(DataFileType wzType, String wzPath) {
		switch (wzType) {
			case KVJ:
				instance = new KvjMapDataLoader(wzPath);
				break;
		}
		return instance;
	}

	public static MapDataLoader getInstance() {
		return instance;
	}
}
