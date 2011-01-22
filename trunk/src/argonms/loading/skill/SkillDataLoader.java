package argonms.loading.skill;

import argonms.loading.DataFileType;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author GoldenKevin
 */
public abstract class SkillDataLoader {
	private static SkillDataLoader instance;

	protected Map<Integer, SkillStats> skillStats;

	public SkillDataLoader() {
		skillStats = new HashMap<Integer, SkillStats>();
	}

	protected abstract void load(int skillid);

	public abstract boolean loadAll();

	public static SkillDataLoader setInstance(DataFileType wzType, String wzPath) {
		switch (wzType) {
			case KVJ:
				instance = new KvjSkillDataLoader(wzPath);
				break;
		}
		return instance;
	}

	public static SkillDataLoader getInstance() {
		return instance;
	}
}
