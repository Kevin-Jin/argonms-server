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

import argonms.loading.StatEffectsData;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author GoldenKevin
 */
public class ItemEffectsData extends StatEffectsData {
	private static final byte
		POISON = (1 << 0),
		SEAL = (1 << 1),
		DARKNESS = (1 << 2),
		WEAKNESS = (1 << 3),
		CURSE = (1 << 4),
		CONSUME_ON_PICKUP = (1 << 5)
	;

	//use a bitfield on boolean fields to save memory...
	private byte flags;
	private short hpR, mpR;
	private int moveTo;
	private List<Integer> petConsumableBy;

	protected ItemEffectsData(int itemid) {
		super(itemid);
		petConsumableBy = new ArrayList<Integer>();
	}

	public EffectSource getSourceType() {
		return EffectSource.ITEM;
	}

	protected void setMoveTo(int map) {
		this.moveTo = map;
	}

	protected void setPoison() {
		flags |= POISON;
	}

	protected void setSeal() {
		flags |= SEAL;
	}

	protected void setDarkness() {
		flags |= DARKNESS;
	}

	protected void setWeakness() {
		flags |= WEAKNESS;
	}

	protected void setCurse() {
		flags |= CURSE;
	}

	protected void setConsumeOnPickup() {
		flags |= CONSUME_ON_PICKUP;
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

	public boolean curesPoison() {
		return (flags & POISON) != 0;
	}

	public boolean curesSeal() {
		return (flags & SEAL) != 0;
	}

	public boolean curesDarkness() {
		return (flags & DARKNESS) != 0;
	}

	public boolean curesWeakness() {
		return (flags & WEAKNESS) != 0;
	}

	public boolean curesCurse() {
		return (flags & CURSE) != 0;
	}

	public boolean consumeOnPickup() {
		return (flags & CONSUME_ON_PICKUP) != 0;
	}

	public List<Integer> getPetsConsumable() {
		return petConsumableBy;
	}

	public short getHpR() {
		return hpR;
	}

	public short getMpR() {
		return mpR;
	}

	public int hashCode() {
		return getDataId();
	}

	public byte getLevel() {
		return -1;
	}
}
