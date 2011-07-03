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

package argonms.game.loading.skill;

import argonms.common.loading.DataFileType;
import java.util.HashMap;
import java.util.Map;

//FIXME: Thread safety for concurrent read/writes (if we're not preloading)
/**
 *
 * @author GoldenKevin
 */
public abstract class SkillDataLoader {
	private static SkillDataLoader instance;

	protected Map<Integer, SkillStats> skillStats;
	protected Map<Short, MobSkillStats> mobSkillStats;

	protected SkillDataLoader() {
		skillStats = new HashMap<Integer, SkillStats>();
		mobSkillStats = new HashMap<Short, MobSkillStats>();
	}

	protected abstract void loadPlayerSkill(int skillid);

	protected abstract void loadMobSkill(short skillid);

	public abstract boolean loadAll();

	public abstract boolean canLoadPlayerSkill(int skillid);

	public abstract boolean canLoadMobSkill(short skillid);

	public SkillStats getSkill(int skillid) {
		Integer oId = Integer.valueOf(skillid);
		if (!skillStats.containsKey(oId))
			loadPlayerSkill(skillid);
		return skillStats.get(oId);
	}

	public MobSkillStats getMobSkill(short skillid) {
		Short oId = Short.valueOf(skillid);
		if (!mobSkillStats.containsKey(oId))
			loadMobSkill(skillid);
		return mobSkillStats.get(oId);
	}

	public static void setInstance(DataFileType wzType, String wzPath) {
		if (instance == null) {
			switch (wzType) {
				case KVJ:
					instance = new KvjSkillDataLoader(wzPath);
					break;
				case MCDB:
					instance = new McdbSkillDataLoader();
					break;
			}
		}
	}

	public static SkillDataLoader getInstance() {
		return instance;
	}
}
