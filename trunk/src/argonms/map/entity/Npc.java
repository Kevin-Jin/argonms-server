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
import argonms.net.client.CommonPackets;

/**
 *
 * @author GoldenKevin
 */
public class Npc extends MapEntity {
	private int npcid;
	private short rx0, rx1;
	private short cy;
	private boolean f;

	public Npc(int npcid) {
		this.npcid = npcid;
	}

	public int getNpcId() {
		return npcid;
	}

	public void setRx(short rx0, short rx1) {
		this.rx0 = rx0;
		this.rx1 = rx1;
	}

	public void setCy(short cy) {
		this.cy = cy;
	}

	public void setF(boolean f) {
		this.f = f;
	}

	public short getRx0() {
		return rx0;
	}

	public short getRx1() {
		return rx1;
	}

	public boolean isF() {
		return f;
	}

	public short getCy() {
		return cy;
	}

	public byte[] getShopPacket() {
		return null;
	}

	public MapEntityType getEntityType() {
		return MapEntityType.NPC;
	}

	public boolean isAlive() {
		return true;
	}

	public boolean isVisible() {
		return true;
	}

	public byte[][] getCreationMessages() {
		return new byte[][] { CommonPackets.writeShowNpc(this),
			//let every client animate their own NPCs.
			//controlling mobs is complicated enough as it is, we
			//don't need to keep track of more than necessary things
				CommonPackets.writeControlNpc(this) };
	}

	public byte[][] getShowEntityMessages() {
		return getCreationMessages();
	}

	public byte[] getOutOfViewMessage() {
		return null;
	}

	public byte[] getDestructionMessage() {
		return CommonPackets.writeRemoveNpc(this);
	}

	public boolean isNonRangedType() {
		return true;
	}
}
