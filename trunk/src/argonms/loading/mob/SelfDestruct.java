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

package argonms.loading.mob;

/**
 *
 * @author GoldenKevin
 */
public class SelfDestruct {
	private int action;
	private int hp;
	private int removeAfter;

	protected SelfDestruct() {
		
	}

	protected void setAction(int action) {
		this.action = action;
	}

	protected void setHp(int points) {
		this.hp = points;
	}

	protected void setRemoveAfter(int time) {
		this.removeAfter = time;
	}

	public int getAction() {
		return action;
	}

	public int getHp() {
		return hp;
	}

	public int getRemoveAfter() {
		return removeAfter;
	}

	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("Action=").append(action);
		builder.append(", Hp=").append(hp);
		builder.append(", RemoveAfter=").append(removeAfter);
		return builder.toString();
	}
}
