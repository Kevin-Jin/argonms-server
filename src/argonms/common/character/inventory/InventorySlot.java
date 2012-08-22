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

package argonms.common.character.inventory;

/**
 *
 * @author GoldenKevin
 */
public abstract class InventorySlot implements Comparable<InventorySlot>, Cloneable {
	//TODO: store database entry id so we can save some keys, because right now,
	//we can do a maximum of 1403584 logouts in worst case.
	//with maximum of 255 items per inventory and 5 inventories plus maximum of
	//255 items in storage, we can have up to 1530 items per character (not
	//taking into account a potential 65565 cash items per account). An unsigned
	//32-bit integer key allows (2^31 - 1) items, so (2^31 - 1) / 1530 saves.
	//if we upgrade to an unsigned 64 bit integer, 6 quadrillion saves, but
	//in addition to index overhead, we would take 12 more bytes per ring or
	//mount, 8 more per other equips or pets, and 4 more for other items in
	//database storage space.
	public enum ItemType { EQUIP, ITEM, PET, RING, MOUNT }

	public static final byte
		EQUIP = 1,
		ITEM = 2,
		PET = 3
	;

	public static final byte
		FLAG_LOCK = 0x01,
		FLAG_SPIKES = 0x02,
		FLAG_COLD_PROTECTION = 0x04,
		FLAG_TRADE_UNAVAILABLE = 0x08,
		FLAG_KARMA_SCISSORS = 0x10
	;

	private int id;
	private long expire;
	private long uid;
	private String owner;
	private short flag;

	protected InventorySlot(int itemid) {
		this.id = itemid;
	}

	public abstract ItemType getType();
	public abstract byte getTypeByte();
	public abstract short getQuantity();
	public abstract void setQuantity(short value);

	public int getDataId() {
		return id;
	}

	public long getExpiration() {
		return expire;
	}

	public void setExpiration(long expire) {
		this.expire = expire;
	}

	public long getUniqueId() {
		return uid;
	}

	public void setUniqueId(long id) {
		this.uid = id;
	}

	public String getOwner() {
		return owner;
	}

	public void setOwner(String owner) {
		this.owner = owner;
	}

	public short getFlag() {
		return flag;
	}

	public void setFlag(short flag) {
		this.flag = flag;
	}

	public void setFlagBit(byte bit, boolean value) {
		if (value)
			this.flag |= bit;
		else
			this.flag &= ~bit;
	}

	@Override
	public int compareTo(InventorySlot item) {
		return this.getDataId() - item.getDataId();
	}

	@Override
	public abstract InventorySlot clone();
}
