package argonms.loading.reactor;

import argonms.loading.DataFileType;
import java.util.HashMap;
import java.util.Map;

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

	public static ReactorDataLoader setInstance(DataFileType wzType, String wzPath) {
		switch (wzType) {
			case KVJ:
				instance = new KvjReactorDataLoader(wzPath);
				break;
		}
		return instance;
	}

	public static ReactorDataLoader getInstance() {
		return instance;
	}
}
