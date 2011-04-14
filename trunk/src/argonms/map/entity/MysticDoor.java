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

import argonms.map.MapEntity;
import argonms.net.external.CommonPackets;

/**
 *
 * @author GoldenKevin
 */
public class MysticDoor extends MapEntity {
	private boolean townDoor;

	public MysticDoor(boolean town) {
		this.townDoor = town;
	}

	public MapEntityType getEntityType() {
		return MapEntityType.DOOR;
	}

	public boolean isInTown() {
		return townDoor;
	}

	public boolean isAlive() {
		return false;
	}

	public boolean isVisible() {
		return false;
	}

	public byte[] getCreationMessage() {
		return CommonPackets.writeShowMysticDoor(this);
	}

	public byte[] getShowEntityMessage() {
		return null; //TODO: return something... it is ranged after all
	}

	public byte[] getOutOfViewMessage() {
		return null;
	}

	public byte[] getDestructionMessage() {
		return CommonPackets.writeRemoveMysticDoor(this);
	}

	public boolean isNonRangedType() {
		return false;
	}
}
