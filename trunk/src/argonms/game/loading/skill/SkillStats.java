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

package argonms.game.loading.skill;

import argonms.game.field.Element;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 *
 * @author GoldenKevin
 */
public class SkillStats {
	private final SortedMap<Byte, PlayerSkillEffectsData> levels;
	private Element elemAttr;
	private byte summon;
	private boolean prepared, keydown, keydownend;
	private int animationTime;

	protected SkillStats() {
		levels = new TreeMap<Byte, PlayerSkillEffectsData>();
		summon = -1;
	}

	protected void addLevel(byte level, PlayerSkillEffectsData effect) {
		levels.put(Byte.valueOf(level), effect);
	}

	protected void setElementalAttribute(String attr) {
		elemAttr = Element.valueOf(attr.charAt(0));
	}

	protected void setSummonType(byte type) {
		summon = type;
	}

	protected void setPrepared() {
		prepared = true;
	}

	protected void setKeydown() {
		keydown = true;
	}

	protected void setKeydownEnd() {
		keydownend = true;
	}

	protected void setDelay(int delay) {
		animationTime = delay;
	}

	public PlayerSkillEffectsData getLevel(byte level) {
		return levels.get(Byte.valueOf(level));
	}

	public byte maxLevel() {
		return levels.lastKey().byteValue();
	}

	public Element getElement() {
		return elemAttr;
	}

	public boolean isSummon() {
		return summon != -1;
	}

	public byte getSummonType() {
		return summon;
	}

	public boolean isPrepared() {
		return prepared;
	}

	public boolean isKeydown() {
		return keydown;
	}

	public boolean isKeydownEnd() {
		return keydownend;
	}

	public int getDelay() {
		return animationTime;
	}
}
