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
import argonms.common.net.external.ClientSendOps;
import argonms.common.net.external.CommonPackets;
import argonms.common.util.Rng;
import argonms.common.util.input.LittleEndianReader;
import argonms.common.util.output.LittleEndianByteArrayWriter;
import argonms.game.character.GameCharacter;
import argonms.game.character.inventory.ItemTools;
import argonms.game.character.inventory.PetTools;
import argonms.game.net.external.GameClient;

/**
 *
 * @author GoldenKevin
 */
public class PetHandler {
	public static void handlePetFood(LittleEndianReader packet, GameClient gc) {
		/*int tickCount = */packet.readInt();
		short foodSlot = packet.readShort();
		int foodItemId = packet.readInt();

		GameCharacter p = gc.getPlayer();
		Inventory inv = p.getInventory(Inventory.InventoryType.USE);
		InventorySlot food = inv.get(foodSlot);
		if (food == null || food.getDataId() != foodItemId || food.getQuantity() < 1) {
			CheatTracker.get(gc).suspicious(CheatTracker.Infraction.POSSIBLE_PACKET_EDITING, "Tried to use nonexistent pet food");
			return;
		}

		//List<Integer> foodConsumableBy = ItemDataLoader.getInstance().getEffect(foodItemId).getPetsConsumable();
		Pet[] pets = p.getPets();
		byte petSlot = 0;
		for (byte i = 0; i < 3 && pets[i] != null; i++)
			if (/*foodConsumableBy.contains(Integer.valueOf(pets[i].getDataId())) && */pets[i].getFullness() < pets[petSlot].getFullness())
				petSlot = i;

		food = InventoryTools.takeFromInventory(inv, foodSlot, (short) 1);
		if (food != null)
			gc.getSession().send(CommonPackets.writeInventoryUpdateSlotQuantity(Inventory.InventoryType.USE, foodSlot, food));
		else
			gc.getSession().send(CommonPackets.writeInventoryClearSlot(Inventory.InventoryType.USE, foodSlot));
		p.itemCountChanged(foodItemId);
		Pet pet = pets[petSlot];
		if (pet == null) //no pets active
			return;

		if (pet.getFullness() < 100) {
			PetTools.gainFullness(pet, ItemDataLoader.getInstance().getPetFullnessRecover(foodItemId));
			if (Rng.getGenerator().nextBoolean())
				PetTools.gainCloseness(p, petSlot, pet, 1);
			PetTools.updatePet(p, pet);
			p.getMap().sendToAll(writePetFoodResponse(p, petSlot, true));
		} else {
			if (Rng.getGenerator().nextBoolean() && PetTools.gainCloseness(p, petSlot, pet, -1))
				PetTools.updatePet(p, pet);
			p.getMap().sendToAll(writePetFoodResponse(p, petSlot, false));
		}
	}

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
		byte petSlot = p.indexOfPet(pet.getUniqueId());
		if (petSlot != -1) {
			p.removePet(petSlot, (byte) 0);
			return;
		}

		if (!ItemDataLoader.getInstance().isEquippablePet(pet.getDataId())) {
			PetTools.evolvePet(p, pet, (byte) -1);
			PetTools.updatePet(p, pet);
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

	public static void handlePetChat(LittleEndianReader packet, GameClient gc) {
		long uniqueId = packet.readLong();
		packet.readByte();
		byte act = packet.readByte();
		String message = packet.readLengthPrefixedString();

		GameCharacter p = gc.getPlayer();
		byte petSlot = p.indexOfPet(uniqueId);
		if (petSlot == -1) {
			CheatTracker.get(gc).suspicious(CheatTracker.Infraction.POSSIBLE_PACKET_EDITING, "Tried to pet chat with nonexistent pet");
			return;
		}

		p.getMap().sendToAll(writePetChat(p, petSlot, act, message));
	}

	public static void handlePetCommand(LittleEndianReader packet, GameClient gc) {
		long uniqueId = packet.readLong();
		packet.readByte();
		byte act = packet.readByte();

		GameCharacter p = gc.getPlayer();
		byte petSlot = p.indexOfPet(uniqueId);
		if (petSlot == -1) {
			CheatTracker.get(gc).suspicious(CheatTracker.Infraction.POSSIBLE_PACKET_EDITING, "Tried to use pet command with nonexistent pet");
			return;
		}

		Pet pet = p.getPets()[petSlot];
		int[] command = ItemDataLoader.getInstance().getPetCommand(pet.getDataId(), act);
		if (command == null) {
			CheatTracker.get(gc).suspicious(CheatTracker.Infraction.POSSIBLE_PACKET_EDITING, "Tried to use nonexistent pet command");
			return;
		}

		if (Rng.getGenerator().nextInt(100) < command[0]) {
			PetTools.gainCloseness(p, petSlot, pet, command[1]);
			PetTools.updatePet(p, pet);
			p.getMap().sendToAll(writePetCommandResponse(p, petSlot, act, true));
		} else {
			p.getMap().sendToAll(writePetCommandResponse(p, petSlot, act, false));
		}
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

	private static byte[] writePetFoodResponse(GameCharacter p, byte slot, boolean positive) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(10);
		lew.writeShort(ClientSendOps.PET_RESPONSE);
		lew.writeInt(p.getId());
		lew.writeByte(slot);
		lew.writeBool(true);
		lew.writeBool(positive);
		lew.writeBool(false); //chat item
		return lew.getBytes();
	}

	private static byte[] writePetCommandResponse(GameCharacter p, byte slot, byte command, boolean positive) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(11);
		lew.writeShort(ClientSendOps.PET_RESPONSE);
		lew.writeInt(p.getId());
		lew.writeByte(slot);
		lew.writeBool(false);
		lew.writeByte(command);
		lew.writeBool(positive);
		lew.writeBool(false); //chat item
		return lew.getBytes();
	}

	private static byte[] writePetChat(GameCharacter p, byte slot, byte act, String message) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(12 + message.length());
		lew.writeShort(ClientSendOps.PET_CHAT);
		lew.writeInt(p.getId());
		lew.writeByte(slot);
		lew.writeByte((byte) 0);
		lew.writeByte(act);
		lew.writeLengthPrefixedString(message);
		lew.writeBool(false); //chat item
		return lew.getBytes();
	}
}
