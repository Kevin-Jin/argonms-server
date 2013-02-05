/*
 * ArgonMS MapleStory server emulator written in Java
 * Copyright (C) 2011-2013  GoldenKevin
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

package argonms.game.field;

import java.awt.Point;

/**
 *
 * @author GoldenKevin
 */
public abstract class AbstractEntity implements MapEntity {
	private int entityid;
	private Point pos;
	/**
	 * 1-byte bit field, with the flags (from most significant to least significant bits):
	 * (?)(?)(?)(?)(has owner)(can fly)(?)(facing left)
	 */
	private byte stance;
	private short foothold;

	@Override
	public int getId() {
		return entityid;
	}

	@Override
	public void setId(int newEid) {
		entityid = newEid;
	}

	@Override
	public Point getPosition() {
		return pos;
	}

	@Override
	public void setPosition(Point newPos) {
		pos = newPos;
	}

	@Override
	public byte getStance() {
		return stance;
	}

	@Override
	public void setStance(byte newStance) {
		stance = newStance;
	}

	@Override
	public short getFoothold() {
		return foothold;
	}

	@Override
	public void setFoothold(short newFh) {
		foothold = newFh;
	}
}
