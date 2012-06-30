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

package argonms.game.script;

import argonms.common.character.BuddyList;
import argonms.common.character.PlayerJob;
import argonms.common.character.inventory.Inventory;
import argonms.common.character.inventory.Inventory.InventoryType;
import argonms.common.character.inventory.InventorySlot;
import argonms.common.character.inventory.InventoryTools;
import argonms.common.character.inventory.InventoryTools.UpdatedSlots;
import argonms.common.character.inventory.Pet;
import argonms.common.net.external.ClientSession;
import argonms.game.GameServer;
import argonms.game.character.MapMemoryVariable;
import argonms.game.net.external.GameClient;
import argonms.game.net.external.GamePackets;

/**
 *
 * @author GoldenKevin
 */
public abstract class PlayerScriptInteraction {
	private GameClient client;

	public PlayerScriptInteraction(GameClient c) {
		this.client = c;
	}

	protected void dissociateClient() {
		client = null;
	}

	public GameClient getClient() {
		return client;
	}

	public void giveExp(int gain) {
		client.getPlayer().gainExp((int) Math.min((long) gain * GameServer.getVariables().getExpRate(), Integer.MAX_VALUE), false, true);
	}

	public boolean playerHasMesos(int min) {
		return client.getPlayer().getMesos() >= min;
	}

	public void giveMesos(int gain) {
		client.getPlayer().gainMesos((int) Math.min((long) gain * GameServer.getVariables().getMesoRate(), Integer.MAX_VALUE), true);
	}

	public void takeMesos(int lose) {
		client.getPlayer().gainMesos(-lose, true);
	}

	public boolean playerHasItem(int itemid, int quantity) {
		return InventoryTools.hasItem(client.getPlayer(), itemid, quantity);
	}

	public boolean playerCanHoldItem(int itemid, short quantity) {
		return InventoryTools.canFitEntirely(client.getPlayer().getInventory(InventoryTools.getCategory(itemid)), itemid, quantity, true);
	}

	public boolean giveItem(int itemid, short quantity) {
		InventoryType type = InventoryTools.getCategory(itemid);
		Inventory inv = client.getPlayer().getInventory(type);
		if (InventoryTools.canFitEntirely(inv, itemid, quantity, true)) {
			ClientSession<?> ses = client.getSession();
			UpdatedSlots changedSlots = InventoryTools.addToInventory(inv, itemid, quantity);
			short pos;
			InventorySlot slot;
			for (Short s : changedSlots.modifiedSlots) {
				pos = s.shortValue();
				slot = inv.get(pos);
				ses.send(GamePackets.writeInventoryUpdateSlotQuantity(type, pos, slot));
			}
			for (Short s : changedSlots.addedOrRemovedSlots) {
				pos = s.shortValue();
				slot = inv.get(pos);
				ses.send(GamePackets.writeInventoryAddSlot(type, pos, slot));
			}
			ses.send(GamePackets.writeShowItemGainFromQuest(itemid, quantity));
			client.getPlayer().itemCountChanged(itemid);
			return true;
		}
		return false;
	}

	public boolean takeItem(int itemid, int quantity) {
		if (InventoryTools.hasItem(client.getPlayer(), itemid, quantity)) {
			InventoryType type = InventoryTools.getCategory(itemid);
			Inventory inv = client.getPlayer().getInventory(type);
			ClientSession<?> ses = client.getSession();
			UpdatedSlots changedSlots = InventoryTools.removeFromInventory(inv, itemid, quantity);
			short pos;
			InventorySlot slot;
			for (Short s : changedSlots.modifiedSlots) {
				pos = s.shortValue();
				slot = inv.get(pos);
				ses.send(GamePackets.writeInventoryUpdateSlotQuantity(type, pos, slot));
			}
			for (Short s : changedSlots.addedOrRemovedSlots) {
				pos = s.shortValue();
				ses.send(GamePackets.writeInventoryClearSlot(type, pos));
			}
			ses.send(GamePackets.writeShowItemGainFromQuest(itemid, -quantity));
			client.getPlayer().itemCountChanged(itemid);
			return true;
		}
		return false;
	}

	public int getMap() {
		return client.getPlayer().getMapId();
	}

	public void warpPlayer(int mapId) {
		client.getPlayer().changeMap(mapId);
	}

	public short getPlayerLevel() {
		return client.getPlayer().getLevel();
	}

	public boolean playerIsBeginner() {
		return client.getPlayer().getJob() == PlayerJob.JOB_BEGINNER;
	}

	public void rememberMap(String variable) {
		client.getPlayer().rememberMap(MapMemoryVariable.valueOf(variable));
	}

	public int getRememberedMap(String variable) {
		return client.getPlayer().getRememberedMap(MapMemoryVariable.valueOf(variable));
	}

	public int resetRememberedMap(String variable) {
		return client.getPlayer().resetRememberedMap(MapMemoryVariable.valueOf(variable));
	}

	public short getPlayerBuddyCapacity() {
		return client.getPlayer().getBuddyList().getCapacity();
	}

	public void giveBuddySlots(short delta) {
		BuddyList bList = client.getPlayer().getBuddyList();
		bList.increaseCapacity(delta);
		client.getSession().send(GamePackets.writeBuddyCapacityUpdate(bList.getCapacity()));
	}

	public boolean isQuestFinished(short questId) {
		return client.getPlayer().isQuestCompleted(questId);
	}

	public int getPlayerPetCount() {
		int count = 0;
		Pet[] pets = client.getPlayer().getPets();
		for (int i = 0; i < pets.length; i++)
			if (pets[i] != null)
				count++;
		return count;
	}

	public void giveCloseness(short gain) {
		//TODO: pet exp rate?
		//TODO: implement
	}
}
