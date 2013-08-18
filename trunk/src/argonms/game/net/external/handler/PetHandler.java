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
import argonms.common.character.inventory.InventoryTools;
import argonms.common.character.inventory.Pet;
import argonms.common.loading.item.ItemDataLoader;
import argonms.common.net.external.CheatTracker;
import argonms.common.net.external.CommonPackets;
import argonms.common.util.input.LittleEndianReader;
import argonms.game.character.GameCharacter;
import argonms.game.character.inventory.ItemTools;
import argonms.game.net.external.GameClient;
import argonms.game.net.external.GamePackets;

/**
 *
 * @author GoldenKevin
 */
public class PetHandler {
	private static final byte
		ITEM_IGNORE_MESOS = 1,
		ITEM_IGNORE_ITEM = 2
	;

	public static void handleUsePet(LittleEndianReader packet, GameClient gc) {
		/*int tickCount = */packet.readInt();
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

	public static void handlePetAutoPotion(LittleEndianReader packet, GameClient gc) {
		long uniqueId = packet.readLong();
		packet.readByte();
		/*int tickCount = */packet.readInt();
		short slot = packet.readShort();
		int itemId = packet.readInt();

		GameCharacter p = gc.getPlayer();
		if (p.indexOfPet(uniqueId) == -1) {
			CheatTracker.get(gc).suspicious(CheatTracker.Infraction.POSSIBLE_PACKET_EDITING, "Tried to use pet auto potion with nonexistent pet");
			return;
		}

		Inventory inv = p.getInventory(Inventory.InventoryType.USE);
		InventorySlot changed = inv.get(slot);
		if (changed == null || changed.getDataId() != itemId || changed.getQuantity() < 1) {
			CheatTracker.get(gc).suspicious(CheatTracker.Infraction.POSSIBLE_PACKET_EDITING, "Tried to use nonexistent potion for pet auto potion");
			return;
		}

		Inventory equippedInv = p.getInventory(Inventory.InventoryType.EQUIPPED);
		if (p.getAutoHpPot() != changed.getDataId() && p.getAutoMpPot() != changed.getDataId()) {
			CheatTracker.get(gc).suspicious(CheatTracker.Infraction.POSSIBLE_PACKET_EDITING, "Tried to use wrong potion for pet auto potion");
			return;
		}

		//check for	potion item pouches
		if (p.getAutoHpPot() == changed.getDataId() && !equippedInv.hasItem(1812002, 1) || p.getAutoMpPot() == changed.getDataId() && !equippedInv.hasItem(1812003, 1)) {
			CheatTracker.get(gc).suspicious(CheatTracker.Infraction.POSSIBLE_PACKET_EDITING, "Tried to use pet auto potion without equip");
			return;
		}

		changed = InventoryTools.takeFromInventory(inv, slot, (short) 1);
		if (changed != null)
			gc.getSession().send(CommonPackets.writeInventoryUpdateSlotQuantity(Inventory.InventoryType.USE, slot, changed));
		else
			gc.getSession().send(CommonPackets.writeInventoryClearSlot(Inventory.InventoryType.USE, slot));
		p.itemCountChanged(itemId);
		ItemTools.useItem(p, itemId);
	}

	public static void handlePetItemIgnore(LittleEndianReader packet, GameClient gc) {
		long uniqueId = packet.readLong();

		GameCharacter p = gc.getPlayer();
		byte slot = p.indexOfPet(uniqueId);
		if (slot == -1) {
			CheatTracker.get(gc).suspicious(CheatTracker.Infraction.POSSIBLE_PACKET_EDITING, "Tried to use pet item ignore with nonexistent pet");
			return;
		}

		byte count = packet.readByte();
		int[] itemIds = new int[count];
		for (int i = 0; i < count; i++)
			itemIds[i] = packet.readInt(); //== Integer.MAX_VALUE for mesos
		p.setPetItemIgnores(uniqueId, itemIds);
	}
}
