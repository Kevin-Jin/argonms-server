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
	private int moveTo;
	private List<Integer> petConsumableBy;

	public ItemEffect() {
		petConsumableBy = new ArrayList<Integer>();
	}

	public void setMoveTo(int map) {
		this.moveTo = map;
	}

	public void setPoison() {
		this.poison = true;
	}

	public void setSeal() {
		this.seal = true;
	}

	public void setDarkness() {
		this.darkness = true;
	}

	public void setWeakness() {
		this.weakness = true;
	}

	public void setCurse() {
		this.curse = true;
	}

	public void setConsumeOnPickup() {
		this.consumeOnPickup = true;
	}

	public void addPetConsumableBy(int petid) {
		petConsumableBy.add(Integer.valueOf(petid));
	}
}
