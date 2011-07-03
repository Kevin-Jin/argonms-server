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

package argonms.common;

import argonms.game.character.inventory.Inventory;
import argonms.game.character.inventory.Inventory.InventoryType;
import argonms.game.character.inventory.Pet;
import argonms.common.net.external.RemoteClient;

/**
 *
 * @author GoldenKevin
 */
public interface Player {
	public RemoteClient getClient();
	public String getName();
	public int getDataId();
	public int getId();
	public byte getGender();
	public byte getSkinColor();
	public short getEyes();
	public short getHair();
	public Pet[] getPets();
	public short getLevel();
	public short getJob();
	public short getStr();
	public short getDex();
	public short getInt();
	public short getLuk();
	public short getHp();
	public short getMaxHp();
	public short getMp();
	public short getMaxMp();
	public short getAp();
	public short getSp();
	public int getExp();
	public short getFame();
	public int getSpouseId();
	public int getMapId();
	public byte getSpawnPoint();
	public Inventory getInventory(InventoryType type);
	public byte getPrivilegeLevel();
	public void close();

	public static class CharacterTools {
		//TODO: place common Player tasks here and call them from implementations of Player
	}
}
