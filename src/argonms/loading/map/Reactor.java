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
public class Reactor {
	private int id;
	private int x;
	private int y;
	private int reactorTime;
	private String name;
	
	public void setDataId(int id) {
		this.id = id;
	}
	
	public void setX(int x) {
		this.x = x;
	}
	
	public void setY(int y) {
		this.y = y;
	}
	
	public void setReactorTime(int time) {
		this.reactorTime = time;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("id=").append(id);
		if (!name.isEmpty()) builder.append(" (").append(name).append(')');
		builder.append(", loc=(").append(x).append(", ").append(y).append(')');
		builder.append(", time=").append(reactorTime);
		return builder.toString();
	}
}
