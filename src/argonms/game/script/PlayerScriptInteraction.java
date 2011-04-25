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

package argonms.game.script;

import argonms.character.inventory.Inventory;
import argonms.character.inventory.InventorySlot;
import argonms.character.inventory.InventoryTools;
import argonms.character.inventory.Inventory.InventoryType;
import argonms.character.inventory.InventoryTools.UpdatedSlots;
import argonms.game.GameClient;
import argonms.game.GameServer;
import argonms.net.external.ClientSession;
import argonms.net.external.CommonPackets;

/**
 *
 * @author GoldenKevin
 */
public abstract class PlayerScriptInteraction {
	private final GameClient client;

	public PlayerScriptInteraction(GameClient c) {
		this.client = c;
	}

	public GameClient getClient() {
		return client;
	}

	public void giveExp(int gain) {
		client.getPlayer().gainExp(gain * GameServer.getVariables().getExpRate(), false, true);
	}

	public void giveMesos(int gain) {
		client.getPlayer().gainMesos(gain * GameServer.getVariables().getMesoRate(), true);
	}

	public boolean playerHasItem(int itemid, short quantity) {
		return InventoryTools.hasItem(client.getPlayer(), itemid, quantity);
	}

	public boolean giveItem(int itemid, short quantity) {
		InventoryType type = InventoryTools.getCategory(itemid);
		Inventory inv = getClient().getPlayer().getInventory(type);
		if (InventoryTools.canFitEntirely(inv, itemid, quantity)) {
			ClientSession ses = getClient().getSession();
			UpdatedSlots changedSlots = InventoryTools.addToInventory(inv, itemid, quantity);
			short pos;
			InventorySlot slot;
			for (Short s : changedSlots.modifiedSlots) {
				pos = s.shortValue();
				slot = inv.get(pos);
				//sending false for fromDrop crashes the game for some reason...
				ses.send(CommonPackets.writeInventorySlotUpdate(type, pos, slot, true, false));
			}
			for (Short s : changedSlots.addedOrRemovedSlots) {
				pos = s.shortValue();
				slot = inv.get(pos);
				//sending false for fromDrop crashes the game for some reason...
				ses.send(CommonPackets.writeInventorySlotUpdate(type, pos, slot, true, true));
			}
			ses.send(CommonPackets.writeShowItemGainFromQuest(itemid, quantity));
			return true;
		}
		return false;
	}
}
