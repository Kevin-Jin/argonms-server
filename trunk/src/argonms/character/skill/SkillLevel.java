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

package argonms.character.skill;

/**
 *
 * @author GoldenKevin
 */
public class SkillLevel {
	private byte level, master;

	public SkillLevel(byte currentLevel, byte masterLevel) {
		this.level = currentLevel;
		this.master = masterLevel;
	}

	public byte getLevel() {
		return level;
	}

	public void incrementCurrentLevel() {
		++level;
	}

	public byte getMasterLevel() {
		return master;
	}

	public void changeMasterLevel(byte newLevel) {
		this.master = newLevel;
	}
}
