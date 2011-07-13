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

package argonms.common.character.inventory;

/**
 *
 * @author GoldenKevin
 */
public class Equip extends InventorySlot implements Cloneable {
	public enum WeaponType {
		NOT_A_WEAPON(0),
		BOW(3.4),
		CLAW(3.6),
		DAGGER(4),
		CROSSBOW(3.6),
		AXE1H(4.4),
		SWORD1H(4.0),
		BLUNT1H(4.4),
		AXE2H(4.8),
		SWORD2H(4.6),
		BLUNT2H(4.8),
		POLE_ARM(5.0),
		SPEAR(5.0),
		STAFF(3.6),
		WAND(3.6),
		KNUCKLE(4.8),
		GUN(3.715);

		private final double damageMultiplier;

		private WeaponType(double maxDamageMultiplier) {
			this.damageMultiplier = maxDamageMultiplier;
		}

		public double getMaxDamageMultiplier() {
			return damageMultiplier;
		}
	}

	private byte tuc;
	private byte level;

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
		hands,
		speed,
		jump
	;

	public Equip(int itemid) {
		super(itemid);
	}

	@Override
	public ItemType getType() {
		return ItemType.EQUIP;
	}

	@Override
	public byte getTypeByte() {
		return InventorySlot.EQUIP;
	}

	@Override
	public short getQuantity() {
		return 1;
	}

	@Override
	public void setQuantity(short newValue) {
		throw new UnsupportedOperationException("Cannot change quantity of an equip.");
	}

	public byte getUpgradeSlots() {
		return tuc;
	}

	public byte getLevel() {
		return level;
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

	public short getHands() {
		return hands;
	}

	public short getSpeed() {
		return speed;
	}

	public short getJump() {
		return jump;
	}

	public void setUpgradeSlots(byte value) {
		this.tuc = value;
	}

	public void setLevel(byte value) {
		this.level = value;
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

	@Override
	public Equip clone() {
		Equip copy = new Equip(getDataId());
		copy.setExpiration(getExpiration());
		copy.setUniqueId(getUniqueId());
		copy.setOwner(getOwner());
		copy.setFlag(getFlag());

		copy.setUpgradeSlots(getUpgradeSlots());
		copy.setLevel(getLevel());
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
		return copy;
	}
}
