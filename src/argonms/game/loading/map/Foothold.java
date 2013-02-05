/*
 * ArgonMS MapleStory server emulator written in Java
 * Copyright (C) 2011-2013  GoldenKevin
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

package argonms.game.loading.map;

/**
 *
 * @author GoldenKevin
 */
public class Foothold implements Comparable<Foothold> {
	private final short id;
	private short x1;
	private short y1;
	private short x2;
	private short y2;
	private short prev;
	private short next;

	protected Foothold(short id) {
		this.id = id;
	}

	public short getId() {
		return id;
	}

	protected void setX1(short x1) {
		this.x1 = x1;
	}

	protected void setY1(short y1) {
		this.y1 = y1;
	}

	protected void setX2(short x2) {
		this.x2 = x2;
	}

	protected void setY2(short y2) {
		this.y2 = y2;
	}

	protected void setPrev(short prev) {
		this.prev = prev;
	}

	protected void setNext(short next) {
		this.next = next;
	}

	public short getX1() {
		return x1;
	}

	public short getY1() {
		return y1;
	}

	public short getX2() {
		return x2;
	}

	public short getY2() {
		return y2;
	}

	public short getPrev() {
		return prev;
	}

	public short getNext() {
		return next;
	}

	public boolean isWall() {
		return x1 == x2;
	}

	@Override
	public int compareTo(Foothold o) {
		if (y2 < o.getY1())
			return -1;
		else if (y1 > o.getY2())
			return 1;
		else
			return 0;
	}

	@Override
	public String toString() {
		StringBuilder ret = new StringBuilder();
		ret.append('(').append(x1).append(", ").append(y1).append("), (").append(x2).append(", ").append(y2).append("). Prev=").append(prev).append(", Next=").append(next);
		return ret.toString();
	}
}