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

package argonms.map.object;

import argonms.loading.reactor.ReactorStats;
import argonms.loading.reactor.State;
import argonms.map.MapObject;

/**
 *
 * @author GoldenKevin
 */
public class Reactor extends MapObject {
	private ReactorStats stats;
	private String name;
	private int delay;
	private byte state;

	public Reactor(ReactorStats reactorStats) {
		this.stats = reactorStats;
		this.state = 0;
		stats.getStates();
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setDelay(int delay) {
		this.delay = delay;
	}

	public void setState(byte state) {
		this.state = state;
	}

	public String getName() {
		return name;
	}

	public int getDelay() {
		return delay;
	}

	public State getState() {
		return stats.getStates().get(Integer.valueOf(state));
	}

	public MapObjectType getObjectType() {
		return MapObjectType.REACTOR;
	}

	public boolean isVisible() {
		return false;
	}

	public byte[] getCreationMessage() {
		return null;
	}

	public byte[] getShowObjectMessage() {
		return null;
	}

	public byte[] getOutOfViewMessage() {
		return null;
	}

	public byte[] getDestructionMessage() {
		return null;
	}

	public boolean isNonRangedType() {
		return false;
	}
}
