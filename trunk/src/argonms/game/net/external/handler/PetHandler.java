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

package argonms.game.net.external.handler;

import argonms.common.character.Skills;
import argonms.common.character.inventory.Inventory;
import argonms.common.character.inventory.InventorySlot;
import argonms.common.character.inventory.Pet;
import argonms.common.loading.item.ItemDataLoader;
import argonms.common.net.external.CheatTracker;
import argonms.common.util.input.LittleEndianReader;
import argonms.game.character.GameCharacter;
import argonms.game.net.external.GameClient;
import argonms.game.net.external.GamePackets;

/**
 *
 * @author GoldenKevin
 */
public class PetHandler {
	public static void handleUsePet(LittleEndianReader packet, GameClient gc) {
		packet.readInt();
		short slot = packet.readShort();
		boolean boss = packet.readBool();

		GameCharacter p = gc.getPlayer();
		InventorySlot item = p.getInventory(Inventory.InventoryType.CASH).get(slot);
		if (item == null || item.getType() != InventorySlot.ItemType.PET) {
			CheatTracker.get(gc).suspicious(CheatTracker.Infraction.POSSIBLE_PACKET_EDITING, "Tried to use nonexistent pet");
			return;
		}

		Pet pet = (Pet) item;
		ItemDataLoader idl = ItemDataLoader.getInstance();
		if (idl.isPetEvolvable(pet.getDataId())) {
			//TODO: evolvable pets not handled at this time
			gc.getSession().send(GamePackets.writeEnableActions());
			return;
		}

		byte petSlot = p.indexOfPet(pet.getUniqueId());
		if (petSlot != -1) {
			p.removePet(petSlot, (byte) 0);
			return;
		}

		if (pet.isExpired()) {
			CheatTracker.get(gc).suspicious(CheatTracker.Infraction.POSSIBLE_PACKET_EDITING, "Tried to use expired pet");
			return;
		}

		if (p.getSkillLevel(Skills.FOLLOW_THE_LEAD) == 0 && p.getPets()[0] != null)
			p.removePet((byte) 0, (byte) 0);

		if (boss)
			p.addFirstPet(pet);
		else
			p.addLastPet(pet);
	}
}
