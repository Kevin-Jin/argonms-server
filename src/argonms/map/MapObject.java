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
public abstract class MapObject {
	public enum MapObjectType {
		NPC, MONSTER, ITEM, PLAYER, DOOR, SUMMON, MIST, REACTOR, HIRED_MERCHANT,
		PLAYER_NPC, MINI_GAME, PLAYER_SHOP, TRADE
	}

	private int objectid;
	private Point pos;
	private byte stance;
	private short foothold;

	public abstract MapObjectType getObjectType();

	public abstract boolean isVisible();

	public abstract byte[] getCreationMessage();
	public abstract byte[] getShowObjectMessage(); //for nonranged types, make this call getCreationMessage().
	public abstract byte[] getOutOfViewMessage(); //nonranged types can return null
	public abstract byte[] getDestructionMessage();

	public int getId() {
		return objectid;
	}

	public void setId(int newOid) {
		objectid = newOid;
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
