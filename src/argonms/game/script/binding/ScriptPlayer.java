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

package argonms.game.script.binding;

import argonms.common.character.BuddyList;
import argonms.common.character.inventory.Inventory;
import argonms.common.character.inventory.InventorySlot;
import argonms.common.character.inventory.InventoryTools;
import argonms.common.character.inventory.Pet;
import argonms.common.net.external.ClientSession;
import argonms.game.GameServer;
import argonms.game.character.GameCharacter;
import argonms.game.net.external.GamePackets;

/**
 *
 * @author GoldenKevin
 */
public class ScriptPlayer {
	private GameCharacter player;

	public ScriptPlayer(GameCharacter player) {
		this.player = player;
	}

	public int getId() {
		return player.getId();
	}

	public void gainExp(int gain) {
		player.gainExp((int) Math.min((long) gain * GameServer.getVariables().getExpRate(), Integer.MAX_VALUE), false, true);
	}

	public boolean canGainItem(int itemId, int quantity) {
		return InventoryTools.canFitEntirely(player.getInventory(InventoryTools.getCategory(itemId)), itemId, quantity, true);
	}

	public boolean hasItem(int itemId, int quantity) {
		return InventoryTools.hasItem(player, itemId, quantity);
	}

	public boolean gainItem(int itemId, int quantity) {
		Inventory.InventoryType type = InventoryTools.getCategory(itemId);
		Inventory inv = player.getInventory(type);
		if (InventoryTools.canFitEntirely(inv, itemId, quantity, true)) {
			ClientSession<?> ses = player.getClient().getSession();
			InventoryTools.UpdatedSlots changedSlots = InventoryTools.addToInventory(inv, itemId, quantity);
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
			ses.send(GamePackets.writeShowItemGainFromQuest(itemId, quantity));
			player.itemCountChanged(itemId);
			return true;
		}
		return false;
	}

	public boolean loseItem(int itemId, int quantity) {
		if (InventoryTools.hasItem(player, itemId, quantity)) {
			Inventory.InventoryType type = InventoryTools.getCategory(itemId);
			Inventory inv = player.getInventory(type);
			ClientSession<?> ses = player.getClient().getSession();
			InventoryTools.UpdatedSlots changedSlots = InventoryTools.removeFromInventory(player, itemId, quantity);
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
			ses.send(GamePackets.writeShowItemGainFromQuest(itemId, -quantity));
			player.itemCountChanged(itemId);
			return true;
		}
		return false;
	}

	public boolean hasMesos(int min) {
		return player.getMesos() >= min;
	}

	public void gainMesos(int gain) {
		player.gainMesos((int) Math.min((long) gain * GameServer.getVariables().getMesoRate(), Integer.MAX_VALUE), true);
	}

	public void loseMesos(int lose) {
		player.gainMesos(-lose, true);
	}

	public short getLevel() {
		return player.getLevel();
	}

	public short getJob() {
		return player.getJob();
	}

	public void setJob(short newJob) {
		player.setJob(newJob);
	}

	public byte getGender() {
		return player.getGender();
	}

	public short getHair() {
		return player.getHair();
	}

	public void setHair(short newHair) {
		player.setHair(newHair);
	}

	public void setSkin(byte newSkin) {
		player.setSkin(newSkin);
	}

	public short getFace() {
		return player.getEyes();
	}

	public void setFace(short newEyes) {
		player.setEyes(newEyes);
	}

	public short getStr() {
		return player.getStr();
	}

	public short getDex() {
		return player.getDex();
	}

	public short getInt() {
		return player.getInt();
	}

	public short getLuk() {
		return player.getLuk();
	}

	public void increaseMaxHp(short delta) {
		player.setMaxHp((short) (player.getMaxHp() + delta));
		player.gainHp(delta);
	}

	public void increaseMaxMp(short delta) {
		player.setMaxMp((short) (player.getMaxMp() + delta));
		player.gainMp(delta);
	}

	public void gainEquipInventorySlots(short delta) {
		short newCap = player.getInventory(Inventory.InventoryType.EQUIP).increaseCapacity(delta);
		player.getClient().getSession().send(GamePackets.writeInventoryUpdateCapacity(Inventory.InventoryType.EQUIP, newCap));
	}

	public void gainUseInventorySlots(short delta) {
		short newCap = player.getInventory(Inventory.InventoryType.USE).increaseCapacity(delta);
		player.getClient().getSession().send(GamePackets.writeInventoryUpdateCapacity(Inventory.InventoryType.USE, newCap));
	}

	public void gainSetupInventorySlots(short delta) {
		short newCap = player.getInventory(Inventory.InventoryType.SETUP).increaseCapacity(delta);
		player.getClient().getSession().send(GamePackets.writeInventoryUpdateCapacity(Inventory.InventoryType.SETUP, newCap));
	}

	public void gainEtcInventorySlots(short delta) {
		short newCap = player.getInventory(Inventory.InventoryType.ETC).increaseCapacity(delta);
		player.getClient().getSession().send(GamePackets.writeInventoryUpdateCapacity(Inventory.InventoryType.ETC, newCap));
	}

	public void gainSp(short gain) {
		player.setSp((short) (player.getSp() + gain));
	}

	public short getSp() {
		return player.getSp();
	}

	public String getName() {
		return player.getName();
	}

	public short getBuddyCapacity() {
		return player.getBuddyList().getCapacity();
	}

	public void gainBuddySlots(short gain) {
		BuddyList bList = player.getBuddyList();
		bList.increaseCapacity(gain);
		player.getClient().getSession().send(GamePackets.writeBuddyCapacityUpdate(bList.getCapacity()));
	}

	public boolean isQuestCompleted(short questId) {
		return player.isQuestCompleted(questId);
	}

	public boolean isQuestActive(short questId) {
		return player.isQuestActive(questId);
	}

	public boolean isQuestStarted(short questId) {
		return player.isQuestStarted(questId);
	}

	public byte getPetCount() {
		byte count = 0;
		for (Pet p : player.getPets())
			if (p != null)
				count++;
		return count;
	}

	public void gainCloseness(short gain) {
		//TODO: implement pets and pet exp rate?
	}
}
