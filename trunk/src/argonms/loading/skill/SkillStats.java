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
public class SkillStats {
	private Map<Byte, PlayerSkillEffectsData> levels;
	private String elemAttr;
	private boolean isBuff, isCharged;
	private int animationTime;

	protected SkillStats() {
		levels = new HashMap<Byte, PlayerSkillEffectsData>();
	}

	protected void addLevel(byte level, PlayerSkillEffectsData effect) {
		levels.put(Byte.valueOf(level), effect);
	}

	protected void setElemAttr(String attr) {
		elemAttr = attr;
	}

	protected void setBuff() {
		isBuff = true;
	}

	protected void setChargedSkill() {
		isCharged = true;
	}

	protected void setDelay(int delay) {
		animationTime = delay;
	}

	public PlayerSkillEffectsData getLevel(byte level) {
		return levels.get(Byte.valueOf(level));
	}

	public String getElemAttr() {
		return elemAttr;
	}

	public boolean isBuff() {
		return isBuff;
	}

	public boolean isChargedSkill() {
		return isCharged;
	}

	public int getDelay() {
		return animationTime;
	}
}
