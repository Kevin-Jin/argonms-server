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

import argonms.game.field.MapEntity;
import argonms.game.net.external.GamePackets;

/**
 *
 * @author GoldenKevin
 */
public class Npc extends MapEntity {
	private int npcid;
	private short rx0, rx1;
	private short cy;

	public Npc(int npcid) {
		this.npcid = npcid;
	}

	public int getDataId() {
		return npcid;
	}

	public void setRx(short rx0, short rx1) {
		this.rx0 = rx0;
		this.rx1 = rx1;
	}

	public void setCy(short cy) {
		this.cy = cy;
	}

	public short getRx0() {
		return rx0;
	}

	public short getRx1() {
		return rx1;
	}

	public short getCy() {
		return cy;
	}

	public boolean isPlayerNpc() {
		return false;
	}

	@Override
	public EntityType getEntityType() {
		return EntityType.NPC;
	}

	@Override
	public boolean isAlive() {
		return true;
	}

	@Override
	public boolean isVisible() {
		return true;
	}

	@Override
	public byte[] getShowNewSpawnMessage() {
		return GamePackets.writeShowAndControlNpc(this);
	}

	@Override
	public byte[] getShowExistingSpawnMessage() {
		return getShowNewSpawnMessage();
	}

	@Override
	public byte[] getDestructionMessage() {
		return GamePackets.writeStopControlAndRemoveNpc(this);
	}
}
