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

package argonms.map.movement;

import argonms.tools.output.LittleEndianWriter;
import java.awt.Point;

/**
 *
 * @author GoldenKevin
 */
public class ChairMovement extends AbstractLifeMovement {
	private short unk;

	public ChairMovement(byte type, Point position, short duration, byte newstate) {
		super(type, position, duration, newstate);
	}

	public short getUnk() {
		return unk;
	}

	public void setUnk(short unk) {
		this.unk = unk;
	}

	@Override
	public void serialize(LittleEndianWriter lew) {
		lew.writeByte(getType());
		lew.writeShort((short) getPosition().x);
		lew.writeShort((short) getPosition().y);
		lew.writeShort(unk);
		lew.writeByte(getNewstate());
		lew.writeShort(getDuration());
	}
}

