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

package argonms.game.field.entity;

import argonms.common.character.inventory.InventorySlot;
import argonms.game.field.AbstractEntity;
import argonms.game.net.external.GamePackets;
import java.awt.Point;

/**
 *
 * @author GoldenKevin
 */
public class ItemDrop extends AbstractEntity {
	public static final byte
		ITEM = 0,
		MESOS = 1
	;

	public static final byte
		PICKUP_ALLOW_OWNER = 0, //give charid for owner
		PICKUP_ALLOW_PARTY = 1, //give partyid for owner
		PICKUP_ALLOW_ALL = 2, //no owner
		PICKUP_EXPLOSION = 3 //give charid for owner i guess
	;

	public static final byte
		SPAWN_ANIMATION_DROP = 1,
		SPAWN_ANIMATION_NONE = 2,
		SPAWN_ANIMATION_FADE = 3
	;

	public static final byte
		DESTROY_ANIMATION_FADE = 0,
		DESTROY_ANIMATION_NONE = 1,
		DESTROY_ANIMATION_LOOTED = 2,
		DESTROY_ANIMATION_EXPLODE = 4
	;

	private byte mod;
	private byte dropType;
	private int id;
	private int mob;
	private int owner;
	private Point dropFrom;
	private InventorySlot item;
	private boolean gone;
	private byte petLooter;

	public ItemDrop(InventorySlot item) {
		this.dropType = ITEM;
		this.id = item.getDataId();
		this.item = item;
	}

	public ItemDrop(int amt) {
		this.dropType = MESOS;
		this.id = amt;
	}

	public void init(int mob, int owner, Point dropTo, Point dropFrom, byte allow) {
		this.mob = mob;
		this.owner = owner;
		this.setPosition(dropTo);
		this.dropFrom = dropFrom;
		this.mod = allow;
	}

	public byte getPetSlot() {
		return petLooter;
	}

	public byte getDropType() {
		return dropType;
	}

	public int getDataId() {
		return id;
	}

	public int getMob() {
		return mob;
	}

	public int getOwner() {
		return owner;
	}

	public Point getSourcePos() {
		return dropFrom;
	}

	public long getItemExpire() {
		return item.getExpiration();
	}

	@Override
	public EntityType getEntityType() {
		return EntityType.DROP;
	}

	public InventorySlot getItem() {
		return item;
	}

	public void pickUp(int looter) {
		this.owner = looter;
		this.gone = true;
		this.mod = DESTROY_ANIMATION_LOOTED;
	}

	public void pickUp(int looter, byte pet) {
		this.petLooter = pet;
		pickUp(looter);
	}

	public void explode() {
		this.gone = true;
		this.mod = DESTROY_ANIMATION_EXPLODE;
	}

	public void expire() {
		this.gone = true;
		this.mod = DESTROY_ANIMATION_FADE;
	}

	@Override
	public boolean isAlive() {
		return !gone;
	}

	@Override
	public boolean isVisible() {
		return true;
	}

	@Override
	public byte[] getShowNewSpawnMessage() {
		return GamePackets.writeShowItemDrop(this, SPAWN_ANIMATION_DROP, mod);
	}

	@Override
	public byte[] getShowExistingSpawnMessage() {
		return GamePackets.writeShowItemDrop(this, SPAWN_ANIMATION_NONE, mod);
	}

	public byte[] getDisappearMessage() {
		return GamePackets.writeShowItemDrop(this, SPAWN_ANIMATION_FADE, mod);
	}

	@Override
	public byte[] getDestructionMessage() {
		return GamePackets.writeRemoveItemDrop(this, mod);
	}
}
