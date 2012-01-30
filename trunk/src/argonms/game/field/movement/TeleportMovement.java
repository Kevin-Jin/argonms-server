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

package argonms.game.field.movement;

import argonms.common.util.output.LittleEndianWriter;
import java.awt.Point;
import java.util.EnumSet;
import java.util.Set;

/**
 *
 * @author GoldenKevin
 */
public class TeleportMovement implements PositionChangedMovementFragment, StanceChangedMovementFragment {
	private final byte type;
	private final Point position;
	private final Point pixelsPerSecond;
	private final byte stance;

	public TeleportMovement(byte type, Point position, Point wobble, byte stance) {
		this.type = type;
		this.position = position;
		this.pixelsPerSecond = wobble;
		this.stance = stance;
	}

	@Override
	public void serialize(LittleEndianWriter lew) {
		lew.writeByte(type);
		lew.writePos(position);
		lew.writePos(pixelsPerSecond);
		lew.writeByte(stance);
	}

	@Override
	public Set<UpdatedEntityInfo> updatedStats() {
		return EnumSet.of(UpdatedEntityInfo.POSITION, UpdatedEntityInfo.STANCE);
	}

	@Override
	public Point getPosition() {
		return position;
	}

	@Override
	public byte getStance() {
		return stance;
	}
}
