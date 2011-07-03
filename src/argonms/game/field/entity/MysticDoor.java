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

package argonms.game.field.entity;

import argonms.common.net.external.CommonPackets;
import argonms.game.field.MapEntity;
import argonms.game.field.MapEntity.EntityType;

/**
 *
 * @author GoldenKevin
 */
public class MysticDoor extends MapEntity {
	private boolean townDoor;

	public MysticDoor(boolean town) {
		this.townDoor = town;
	}

	public EntityType getEntityType() {
		return EntityType.DOOR;
	}

	public boolean isInTown() {
		return townDoor;
	}

	public boolean isAlive() {
		return false;
	}

	public boolean isVisible() {
		return true;
	}

	public byte[] getShowNewSpawnMessage() {
		return CommonPackets.writeShowMysticDoor(this);
	}

	public byte[] getShowExistingSpawnMessage() {
		return getShowNewSpawnMessage();
	}

	public byte[] getDestructionMessage() {
		return CommonPackets.writeRemoveMysticDoor(this);
	}
}
