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

package argonms.map.object;

import java.awt.Point;

import argonms.character.inventory.InventorySlot;
import argonms.map.MapObject;
import argonms.net.client.CommonPackets;

/**
 *
 * @author GoldenKevin
 */
public class ItemDrop extends MapObject {
	public static final byte
		ITEM = 0,
		MESOS = 1
	;

	private byte mod;
	private byte dropType;
	private int id;
	private int charid;
	private Point dropFrom;
	private int dropper;
	private InventorySlot item;

	public ItemDrop(InventorySlot item) {
		this.dropType = ITEM;
		this.id = item.getItemId();
		this.item = item;
	}

	public ItemDrop(int amt) {
		this.dropType = MESOS;
		this.id = amt;
	}

	public void init(byte mod, int killer, Point dropTo, Point dropFrom, int dropperOid) {
		this.mod = mod;
		this.charid = killer;
		this.setPosition(dropTo);
		this.dropFrom = dropFrom;
		this.dropper = dropperOid;
	}

	public byte getMod() {
		return mod;
	}

	public byte getDropType() {
		return dropType;
	}

	public int getItemId() {
		return id;
	}

	public int getOwner() {
		return charid;
	}

	public Point getSourcePos() {
		return dropFrom;
	}

	public int getSourceObjectId() {
		return dropper;
	}

	public long getItemExpire() {
		return item.getExpiration();
	}

	public MapObjectType getObjectType() {
		return MapObjectType.ITEM;
	}

	public boolean isVisible() {
		return true;
	}

	public byte[] getCreationMessage() {
		return CommonPackets.writeDropItemFromMapObject(this);
	}

	public byte[] getShowObjectMessage() {
		return null;
	}

	public byte[] getOutOfViewMessage() {
		return null;
	}

	public byte[] getDestructionMessage() {
		return null;
	}

	public boolean isNonRangedType() {
		return false;
	}
}
