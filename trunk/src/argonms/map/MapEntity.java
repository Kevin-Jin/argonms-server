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

package argonms.map;

import java.awt.Point;

/**
 *
 * @author GoldenKevin
 */
public abstract class MapEntity {
	public enum MapEntityType {
		NPC, MONSTER, ITEM, PLAYER, DOOR, SUMMON, MIST, REACTOR, HIRED_MERCHANT,
		PLAYER_NPC, MINI_GAME, PLAYER_SHOP, TRADE
	}

	private int entityid;
	private Point pos;
	/**
	 * 1-byte bit field, with the flags (from most significant to least significant bits):
	 * (?)(?)(?)(?)(has owner)(can fly)(?)(facing left)
	 */
	private byte stance;
	private short foothold;

	public abstract MapEntityType getEntityType();

	public abstract boolean isAlive();
	public abstract boolean isVisible();

	public abstract byte[] getCreationMessage();
	public abstract byte[] getShowEntityMessage(); //for nonranged types, make this call getCreationMessage().
	public abstract byte[] getOutOfViewMessage(); //nonranged types can return null
	public abstract byte[] getDestructionMessage();

	public int getId() {
		return entityid;
	}

	public void setId(int newEid) {
		entityid = newEid;
	}

	public Point getPosition() {
		return pos;
	}

	public void setPosition(Point newPos) {
		pos = newPos;
	}

	public byte getStance() {
		return stance;
	}

	public void setStance(byte newStance) {
		stance = newStance;
	}

	public short getFoothold() {
		return foothold;
	}

	public void setFoothold(short newFh) {
		foothold = newFh;
	}

	public abstract boolean isNonRangedType();
}
