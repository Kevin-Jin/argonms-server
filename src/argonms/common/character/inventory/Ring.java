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
public class Ring extends Equip implements Cloneable {
	private int partnerCharId;
	private long partnerRingUid;

	public Ring(int itemid) {
		super(itemid);
	}

	@Override
	public ItemType getType() {
		return ItemType.RING;
	}

	public int getPartnerCharId() {
		return partnerCharId;
	}

	public long getPartnerRingId() {
		return partnerRingUid;
	}

	public void setPartnerCharId(int cid) {
		this.partnerCharId = cid;
	}

	public void setPartnerRingId(long uid) {
		this.partnerRingUid = uid;
	}

	@Override
	public Ring clone() {
		Ring copy = new Ring(getDataId());
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

		copy.setPartnerCharId(getPartnerCharId());
		copy.setPartnerRingId(getPartnerRingId());
		return copy;
	}
}
