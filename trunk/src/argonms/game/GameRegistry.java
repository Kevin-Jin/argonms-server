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

package argonms.game;

/**
 *
 * @author GoldenKevin
 */
public class GameRegistry {
	public static final byte
		RATE_EXP = 1,
		RATE_DROP = 2,
		RATE_MESO = 3
	;

	private boolean itemExpires;
	private boolean buffCooldowns;
	private short expRate, dropRate, mesoRate;
	private boolean consecutiveLevelUps;
	private String ticker;

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

	public void setNewsTickerMessage(String message) {
		this.ticker = message;
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

	public String getNewsTickerMessage() {
		return ticker;
	}
}
