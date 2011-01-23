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

package argonms.loading.map;

/**
 *
 * @author GoldenKevin
 */
public class Area {
	private int x1;
	private int y1;
	private int x2;
	private int y2;

	protected Area() {
		
	}

	protected void setX1(int x1) {
		this.x1 = x1;
	}

	protected void setY1(int y1) {
		this.y1 = y1;
	}

	protected void setX2(int x2) {
		this.x2 = x2;
	}

	protected void setY2(int y2) {
		this.y2 = y2;
	}

	public int getX1() {
		return x1;
	}

	public int getY1() {
		return y1;
	}

	public int getX2() {
		return x2;
	}

	public int getY2() {
		return y2;
	}

	public String toString() {
		StringBuilder ret = new StringBuilder();
		ret.append('(').append(x1).append(", ").append(y1).append("), (").append(x2).append(", ").append(y2).append(")");
		return ret.toString();
	}
}
