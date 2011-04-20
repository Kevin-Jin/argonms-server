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

package argonms.map.entity;

import argonms.loading.reactor.ReactorStats;
import argonms.loading.reactor.State;
import argonms.map.MapEntity;

/**
 *
 * @author GoldenKevin
 */
public class Reactor extends MapEntity {
	private ReactorStats stats;
	private String name;
	private int delay;
	private byte state;
	private boolean alive;

	public Reactor(ReactorStats reactorStats) {
		this.stats = reactorStats;
		this.state = 0;
		this.alive = true;
	}

	public int getDataId() {
		return stats.getReactorId();
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
		return stats.getStates().get(Byte.valueOf(state));
	}

	public EntityType getEntityType() {
		return EntityType.REACTOR;
	}

	public void setAlive(boolean alive) {
		this.alive = alive;
	}

	public boolean isAlive() {
		return alive;
	}

	public boolean isVisible() {
		return false;
	}

	public byte[] getCreationMessage() {
		return null;
	}

	public byte[] getShowEntityMessage() {
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
