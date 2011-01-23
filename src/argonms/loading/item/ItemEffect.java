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

package argonms.loading.item;

import argonms.loading.StatEffects;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author GoldenKevin
 */
public class ItemEffect extends StatEffects {
	private boolean poison, seal, darkness, weakness, curse;
	private boolean consumeOnPickup;
	private short hpR, mpR;
	private int moveTo;
	private List<Integer> petConsumableBy;

	protected ItemEffect() {
		petConsumableBy = new ArrayList<Integer>();
	}

	protected void setMoveTo(int map) {
		this.moveTo = map;
	}

	protected void setPoison() {
		this.poison = true;
	}

	protected void setSeal() {
		this.seal = true;
	}

	protected void setDarkness() {
		this.darkness = true;
	}

	protected void setWeakness() {
		this.weakness = true;
	}

	protected void setCurse() {
		this.curse = true;
	}

	protected void setConsumeOnPickup() {
		this.consumeOnPickup = true;
	}

	protected void addPetConsumableBy(int petid) {
		this.petConsumableBy.add(Integer.valueOf(petid));
	}

	protected void setHpR(short recover) {
		this.hpR = recover;
	}

	protected void setMpR(short recover) {
		this.mpR = recover;
	}

	public int getMoveTo() {
		return moveTo;
	}

	public boolean isPoison() {
		return poison;
	}

	public boolean isSeal() {
		return seal;
	}

	public boolean isDarkness() {
		return darkness;
	}

	public boolean isWeakness() {
		return weakness;
	}

	public boolean isCurse() {
		return curse;
	}

	public boolean isConsumeOnPickup() {
		return consumeOnPickup;
	}

	public boolean petCanConsume(int petId) {
		return petConsumableBy.contains(Integer.valueOf(petId));
	}

	public short getHpR() {
		return hpR;
	}

	public short getMpR() {
		return mpR;
	}
}
