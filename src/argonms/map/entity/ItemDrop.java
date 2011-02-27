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

package argonms.map.entity;

import java.awt.Point;

import argonms.character.inventory.InventorySlot;
import argonms.map.MapEntity;
import argonms.net.client.CommonPackets;

/**
 *
 * @author GoldenKevin
 */
public class ItemDrop extends MapEntity {
	public static final byte
		ITEM = 0,
		MESOS = 1
	;

	private byte mod;
	private byte dropType;
	private int id;
	private int owner;
	private Point dropFrom;
	private int dropper;
	private InventorySlot item;
	private boolean gone;
	private byte petLooter;

	public ItemDrop(InventorySlot item) {
		this.dropType = ITEM;
		this.id = item.getItemId();
		this.item = item;
	}

	public ItemDrop(int amt) {
		this.dropType = MESOS;
		this.id = amt;
	}

	public void init(int owner, Point dropTo, Point dropFrom, int dropperEid) {
		this.owner = owner;
		this.setPosition(dropTo);
		this.dropFrom = dropFrom;
		this.dropper = dropperEid;
	}

	public byte getPetSlot() {
		return petLooter;
	}

	public byte getDropType() {
		return dropType;
	}

	public int getItemId() {
		return id;
	}

	public int getMesoValue() {
		return id;
	}

	public int getOwner() {
		return owner;
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

	public MapEntityType getEntityType() {
		return MapEntityType.ITEM;
	}

	public InventorySlot getItem() {
		return item;
	}

	public void pickUp(int looter) {
		this.owner = looter;
		this.gone = true;
		this.mod = 2;
	}

	public void pickUp(int looter, byte pet) {
		this.petLooter = pet;
		pickUp(looter);
	}

	public void expire() {
		this.gone = true;
		this.mod = 0;
	}

	public boolean isAlive() {
		return !gone;
	}

	public boolean isVisible() {
		return !gone;
	}

	public byte[] getCreationMessage() {
		return CommonPackets.writeShowItemDrop(this, (byte) 1);
	}

	public byte[] getShowObjectMessage() {
		return CommonPackets.writeShowItemDrop(this, (byte) 2);
	}

	public byte[] getDisappearMessage() {
		return CommonPackets.writeShowItemDrop(this, (byte) 3);
	}

	public byte[] getOutOfViewMessage() {
		return CommonPackets.writeRemoveItemDrop(this, (byte) 1);
	}

	public byte[] getDestructionMessage() {
		return CommonPackets.writeRemoveItemDrop(this, mod);
	}

	public boolean isNonRangedType() {
		return false;
	}
}
