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

package argonms.common.loading.item;

import argonms.common.loading.StatusEffectsData.BuffsData;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author GoldenKevin
 */
public class ItemEffectsData extends BuffsData {
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
	private short hp, mp, hpR, mpR;
	private int moveTo;
	private List<Integer> petConsumableBy;

	protected ItemEffectsData(int itemid) {
		super(itemid);
		petConsumableBy = new ArrayList<Integer>();
	}

	@Override
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

	protected void setHpRecover(short recover) {
		this.hp = recover;
	}

	protected void setMpRecover(short recover) {
		this.mp = recover;
	}

	protected void setHpRecoverPercent(short percent) {
		this.hpR = percent;
	}

	protected void setMpRecoverPercent(short percent) {
		this.mpR = percent;
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

	public short getHpRecover() {
		return hp;
	}

	public short getMpRecover() {
		return mp;
	}

	public short getHpRecoverPercent() {
		return hpR;
	}

	public short getMpRecoverPercent() {
		return mpR;
	}

	@Override
	public int hashCode() {
		return getDataId();
	}

	@Override
	public byte getLevel() {
		return -1;
	}
}
