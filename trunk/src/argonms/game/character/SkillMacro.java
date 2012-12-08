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

package argonms.game.character;

/**
 *
 * @author GoldenKevin
 */
public class SkillMacro {
	private final String name;
	private final boolean silent;
	private final int skill1, skill2, skill3;

	public SkillMacro(String name, boolean silent, int skill1, int skill2, int skill3) {
		this.name = name;
		this.silent = silent;
		this.skill1 = skill1;
		this.skill2 = skill2;
		this.skill3 = skill3;
	}

	public String getName() {
		return name;
	}

	public boolean isSilent() {
		return silent;
	}

	public int getFirstSkill() {
		return skill1;
	}

	public int getSecondSkill() {
		return skill2;
	}

	public int getThirdSkill() {
		return skill3;
	}
}
