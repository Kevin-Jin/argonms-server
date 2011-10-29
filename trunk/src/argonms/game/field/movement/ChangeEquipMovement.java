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

import argonms.common.util.output.LittleEndianWriter;
import argonms.game.net.external.handler.GameMovementHandler;
import java.util.EnumSet;
import java.util.Set;

/**
 *
 * @author GoldenKevin
 */
public class ChangeEquipMovement implements LifeMovementFragment {
	/**
	 * 1 indicates something was equipped, 2 indicates something was unequipped
	 * and then something else was equipped.
	 */
	private byte count;

	public ChangeEquipMovement(byte numChanges) {
		this.count = numChanges;
	}

	@Override
	public void serialize(LittleEndianWriter lew) {
		lew.writeByte(GameMovementHandler.EQUIP);
		lew.writeByte(count);
	}

	@Override
	public Set<UpdatedEntityInfo> updatedStats() {
		return EnumSet.noneOf(UpdatedEntityInfo.class);
	}
}
