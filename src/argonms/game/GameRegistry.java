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

package argonms.game;

public class GameRegistry {
	private boolean itemExpires;
	private boolean buffCooldowns;
	private short expRate, dropRate, mesoRate;
	private boolean consecutiveLevelUps;

	protected GameRegistry() {
		
	}

	public void setItemsWillExpire(boolean expire) {
		this.itemExpires = expire;
	}

	public void setBuffsWillCooldown(boolean cooldown) {
		this.buffCooldowns = cooldown;
	}

	public void setExpRate(short newRate) {
		this.expRate = newRate;
	}

	public void setDropRate(short newRate) {
		this.dropRate = newRate;
	}

	public void setMesoRate(short newRate) {
		this.mesoRate = newRate;
	}

	public void setMultiLevel(boolean allow) {
		this.consecutiveLevelUps = allow;
	}

	public boolean doItemExpires() {
		return itemExpires;
	}

	public boolean doCooldowns() {
		return buffCooldowns;
	}

	public short getExpRate() {
		return expRate;
	}

	public short getDropRate() {
		return dropRate;
	}

	public short getMesoRate() {
		return mesoRate;
	}

	public boolean doMultiLevel() {
		return consecutiveLevelUps;
	}
}
