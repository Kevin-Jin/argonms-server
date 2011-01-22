package argonms.loading.mob;

import argonms.loading.DataFileType;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author GoldenKevin
 */
public abstract class MobDataLoader {
	private static MobDataLoader instance;

	protected Map<Integer, MobStats> mobStats;

	protected MobDataLoader() {
		mobStats = new HashMap<Integer, MobStats>();
	}

	protected abstract void load(int mobid);

	public abstract boolean loadAll();

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

	public static MobDataLoader setInstance(DataFileType wzType, String wzPath) {
		switch (wzType) {
			case KVJ:
				instance = new KvjMobDataLoader(wzPath);
				break;
		}
		return instance;
	}

	public static MobDataLoader getInstance() {
		return instance;
	}
}
