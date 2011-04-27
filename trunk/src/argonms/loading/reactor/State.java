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

package argonms.loading.reactor;

import java.awt.Point;

/**
 *
 * @author GoldenKevin
 */
public class State {
	private byte type;
	private byte nextState;

	//item event only
	private int itemid;
	private short quantity;
	private Point lt;
	private Point rb;

	protected State() {
		
	}

	protected void setType(byte type) {
		this.type = type;
	}

	protected void setNextState(byte state) {
		this.nextState = state;
	}

	protected void setItem(int id, short quantity) {
		this.itemid = id;
		this.quantity = quantity;
	}

	protected void setLt(short x, short y) {
		this.lt = new Point(x, y);
	}

	protected void setRb(short x, short y) {
		this.rb = new Point(x, y);
	}

	public int getType() {
		return type;
	}

	public byte getNextState() {
		return nextState;
	}

	public int getItemId() {
		return itemid;
	}

	public short getItemQuantity() {
		return quantity;
	}

	public Point getLt() {
		return lt;
	}

	public Point getRb() {
		return rb;
	}

	public String toString() {
		StringBuilder builder = new StringBuilder();
		boolean itemEvent = (type == 100);
		builder.append("type=").append(type).append(" (itemEvent=").append(itemEvent).append(')');
		builder.append(", nextState=").append(nextState);
		if (itemEvent) {
			builder.append(", itemid=").append(itemid).append(" (Qty=").append(quantity).append(')');
			builder.append(", lt=(").append(lt.x).append(", ").append(lt.y).append(')');
			builder.append(", rb=(").append(rb.x).append(", ").append(rb.y).append(')');
		}
		return builder.toString();
	}
}
