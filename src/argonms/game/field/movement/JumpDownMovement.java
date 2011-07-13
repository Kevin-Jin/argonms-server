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

package argonms.game.field.movement;

import argonms.common.tools.output.LittleEndianWriter;
import java.awt.Point;

/**
 *
 * @author GoldenKevin
 */
public class JumpDownMovement extends AbstractLifeMovement {
	private Point pixelsPerSecond;
	private short unk;
	private short fh;

	public JumpDownMovement(byte type, Point position, short duration, byte newstate) {
		super(type, position, duration, newstate);
	}

	public Point getPixelsPerSecond() {
		return pixelsPerSecond;
	}

	public void setPixelsPerSecond(Point wobble) {
		this.pixelsPerSecond = wobble;
	}

	public short getUnk() {
		return unk;
	}

	public void setUnk(short unk) {
		this.unk = unk;
	}

	public int getFH() {
		return fh;
	}
	
	public void setFH(short fh) {
		this.fh = fh;
	}
	
	@Override
	public void serialize(LittleEndianWriter lew) {
		lew.writeByte(getType());
		lew.writePos(getPosition());
		lew.writePos(pixelsPerSecond);
		lew.writeShort(unk);
		lew.writeShort(fh);
		lew.writeByte(getNewstate());
		lew.writeShort(getDuration());
	}
}
