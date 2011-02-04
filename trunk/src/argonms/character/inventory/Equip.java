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

package argonms.character.inventory;

/**
 *
 * @author GoldenKevin
 */
public class Equip extends InventorySlot implements Cloneable {
	private short
		str,
		dex,
		_int,
		luk,
		hp,
		mp,
		watk,
		matk,
		wdef,
		mdef,
		acc,
		avoid,
		speed,
		jump
	;

	private byte tuc;

	public Equip(int itemid) {
		super(itemid);
	}

	public ItemType getType() {
		return ItemType.EQUIP;
	}

	public short getQuantity() {
		return 1;
	}

	public void setQuantity(short newValue) {
		throw new UnsupportedOperationException("Cannot change quantity of an equip.");
	}

	public short getStr() {
		return str;
	}

	public short getDex() {
		return dex;
	}

	public short getInt() {
		return _int;
	}

	public short getLuk() {
		return luk;
	}

	public short getHp() {
		return hp;
	}

	public short getMp() {
		return mp;
	}

	public short getWatk() {
		return watk;
	}

	public short getMatk() {
		return matk;
	}

	public short getWdef() {
		return wdef;
	}

	public short getMdef() {
		return mdef;
	}

	public short getAcc() {
		return acc;
	}

	public short getAvoid() {
		return avoid;
	}

	public short getSpeed() {
		return speed;
	}

	public short getJump() {
		return jump;
	}

	public byte getUpgradeSlots() {
		return tuc;
	}

	public void setStr(short value) {
		this.str = value;
	}

	public void setDex(short value) {
		this.dex = value;
	}

	public void setInt(short value) {
		this._int = value;
	}

	public void setLuk(short value) {
		this.luk = value;
	}

	public void setHp(short value) {
		this.hp = value;
	}

	public void setMp(short value) {
		this.mp = value;
	}

	public void setWatk(short value) {
		this.watk = value;
	}

	public void setMatk(short value) {
		this.matk = value;
	}

	public void setWdef(short value) {
		this.wdef = value;
	}

	public void setMdef(short value) {
		this.mdef = value;
	}

	public void setAcc(short value) {
		this.acc = value;
	}

	public void setAvoid(short value) {
		this.avoid = value;
	}

	public void setSpeed(short value) {
		this.speed = value;
	}

	public void setJump(short value) {
		this.jump = value;
	}

	public void setUpgradeSlots(byte value) {
		this.tuc = value;
	}

	public Equip clone() {
		Equip copy = new Equip(getItemId());
		copy.setExpiration(getExpiration());
		copy.setUniqueId(getUniqueId());

		copy.setStr(getStr());
		copy.setDex(getDex());
		copy.setInt(getInt());
		copy.setLuk(getLuk());
		copy.setHp(getHp());
		copy.setMp(getMp());
		copy.setWatk(getWatk());
		copy.setMatk(getMatk());
		copy.setWdef(getWdef());
		copy.setMdef(getMdef());
		copy.setAcc(getAcc());
		copy.setAvoid(getAvoid());
		copy.setSpeed(getSpeed());
		copy.setJump(getJump());
		copy.setUpgradeSlots(getUpgradeSlots());
		return copy;
	}
}
