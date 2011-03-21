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

package argonms.loading.skill;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author GoldenKevin
 */
public class MobSkillStats {
	private Map<Byte, MobSkillEffectsData> levels;
	private String elemAttr;
	private int animationTime;

	protected MobSkillStats() {
		levels = new HashMap<Byte, MobSkillEffectsData>();
	}

	protected void addLevel(byte level, MobSkillEffectsData effect) {
		levels.put(Byte.valueOf(level), effect);
	}

	protected void setElemAttr(String attr) {
		elemAttr = attr;
	}

	protected void setDelay(int delay) {
		animationTime = delay;
	}

	public MobSkillEffectsData getLevel(byte level) {
		return levels.get(Byte.valueOf(level));
	}

	public String getElemAttr() {
		return elemAttr;
	}

	public int getDelay() {
		return animationTime;
	}
}
