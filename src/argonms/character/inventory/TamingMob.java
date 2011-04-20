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
public class TamingMob extends Equip {
	private byte level;
	private short exp;
	private byte tiredness;

	public TamingMob(int itemid) {
		super(itemid);

		//set all the default stats
		this.level = 1;
		this.exp = 0;
		this.tiredness = 100;
	}

	public TamingMob(int id, byte level, short exp, byte tiredness) {
		super(id);

		this.level = level;
		this.exp = exp;
		this.tiredness = tiredness;
	}

	public ItemType getType() {
		return ItemType.MOUNT;
	}

	public byte getMountLevel() {
		return level;
	}

	public short getExp() {
		return exp;
	}

	public byte getTiredness() {
		return tiredness;
	}

	public void setMountLevel(byte level) {
		this.level = level;
	}

	public void setExp(short exp) {
		this.exp = exp;
	}

	public void setTiredness(byte tiredness) {
		this.tiredness = tiredness;
	}

	public TamingMob clone() {
		TamingMob copy = new TamingMob(getDataId());
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

		copy.setLevel(getMountLevel());
		copy.setExp(getExp());
		copy.setTiredness(getTiredness());
		return copy;
	}
}
