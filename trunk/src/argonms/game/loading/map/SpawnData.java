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
public class SpawnData {
	private char type;
	private int id;
	private short x;
	private short y;
	private int mobTime;
	private boolean f;
	private boolean hide;
	private short fh;
	private short cy;
	private short rx0;
	private short rx1;

	protected SpawnData() {
		
	}

	protected void setType(char type) {
		this.type = type;
	}

	protected void setDataId(int id) {
		this.id = id;
	}

	protected void setX(short x) {
		this.x = x;
	}

	protected void setY(short y) {
		this.y = y;
	}

	protected void setMobTime(int time) {
		this.mobTime = time;
	}

	protected void setF(boolean value) {
		this.f = value;
	}

	protected void setHide(boolean value) {
		this.hide = value;
	}

	protected void setFoothold(short fh) {
		this.fh = fh;
	}

	protected void setCy(short cy) {
		this.cy = cy;
	}

	protected void setRx0(short rx0) {
		this.rx0 = rx0;
	}

	protected void setRx1(short rx1) {
		this.rx1 = rx1;
	}

	public char getType() {
		return type;
	}

	public int getDataId() {
		return id;
	}

	public short getX() {
		return x;
	}

	public short getY() {
		return y;
	}

	public int getMobTime() {
		return mobTime;
	}

	public boolean isF() {
		return f;
	}

	public boolean isHidden() {
		return hide;
	}

	public short getFoothold() {
		return fh;
	}

	public short getCy() {
		return cy;
	}

	public short getRx0() {
		return rx0;
	}

	public short getRx1() {
		return rx1;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("type=").append(type);
		builder.append(", id=").append(id);
		builder.append(", loc=(").append(x).append(", ").append(y).append(')');
		builder.append(", mobtime=").append(mobTime);
		builder.append(", f=").append(f);
		builder.append(", hide=").append(hide);
		builder.append(", fh=").append(fh);
		builder.append(", cy=").append(cy);
		builder.append(", rx0=").append(rx0);
		builder.append(", rx1=").append(rx1);
		return builder.toString();
	}
}
