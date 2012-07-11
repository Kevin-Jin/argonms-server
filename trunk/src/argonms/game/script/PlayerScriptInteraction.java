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
import argonms.common.character.inventory.Inventory;
import argonms.common.character.inventory.Inventory.InventoryType;
import argonms.common.character.inventory.InventorySlot;
import argonms.common.character.inventory.InventoryTools;
import argonms.common.character.inventory.InventoryTools.UpdatedSlots;
import argonms.common.character.inventory.Pet;
import argonms.common.net.external.ClientSession;
import argonms.game.GameServer;
import argonms.game.character.MapMemoryVariable;
import argonms.game.character.PartyList;
import argonms.game.net.external.GameClient;
import argonms.game.net.external.GamePackets;
import argonms.game.net.external.handler.GameChatHandler;

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
			UpdatedSlots changedSlots = InventoryTools.removeFromInventory(client.getPlayer(), itemid, quantity);
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

	public void takeItem(int itemid) {
		InventoryType type = InventoryTools.getCategory(itemid);
		short quantity = InventoryTools.getAmountOfItem(client.getPlayer().getInventory(type), itemid);
		if (type == InventoryType.EQUIP)
			quantity += InventoryTools.getAmountOfItem(client.getPlayer().getInventory(InventoryType.EQUIPPED), itemid);
		if (quantity > 0)
			takeItem(itemid, quantity);
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

	public short getPlayerJob() {
		return client.getPlayer().getJob();
	}

	public void setPlayerJob(short newJob) {
		client.getPlayer().setJob(newJob);
	}

	public byte getPlayerGender() {
		return client.getPlayer().getGender();
	}

	public short getPlayerHair() {
		return client.getPlayer().getHair();
	}

	public void setPlayerHair(short newHair) {
		client.getPlayer().setHair(newHair);
	}

	public void setPlayerSkin(byte newSkin) {
		client.getPlayer().setSkin(newSkin);
	}

	public short getPlayerFace() {
		return client.getPlayer().getEyes();
	}

	public void setPlayerFace(short newEyes) {
		client.getPlayer().setEyes(newEyes);
	}

	public short getPlayerStr() {
		return client.getPlayer().getStr();
	}

	public short getPlayerDex() {
		return client.getPlayer().getDex();
	}

	public short getPlayerInt() {
		return client.getPlayer().getInt();
	}

	public short getPlayerLuk() {
		return client.getPlayer().getLuk();
	}

	public void increasePlayerMaxHp(short delta) {
		client.getPlayer().setMaxHp((short) (client.getPlayer().getMaxHp() + delta));
		client.getPlayer().gainHp(delta);
	}

	public void increasePlayerMaxMp(short delta) {
		client.getPlayer().setMaxMp((short) (client.getPlayer().getMaxMp() + delta));
		client.getPlayer().gainMp(delta);
	}

	public void giveEquipInventorySlots(short delta) {
		client.getPlayer().getInventory(InventoryType.EQUIP).increaseCapacity(delta);
	}

	public void giveUseInventorySlots(short delta) {
		client.getPlayer().getInventory(InventoryType.USE).increaseCapacity(delta);
	}

	public void giveEtcInventorySlots(short delta) {
		client.getPlayer().getInventory(InventoryType.ETC).increaseCapacity(delta);
	}

	public void giveSp(short gain) {
		client.getPlayer().setSp((short) (client.getPlayer().getSp() + gain));
	}

	public short getPlayerSp() {
		return client.getPlayer().getSp();
	}

	public byte getPlayerPartyMembersInMapCount() {
		PartyList p = client.getPlayer().getParty();
		if (p == null)
			return 0;
		p.lockRead();
		try {
			return (byte) p.getLocalMembersInMap(client.getPlayer().getMapId()).size();
		} finally {
			p.unlockRead();
		}
	}

	public boolean playerIsPartyLeader() {
		PartyList p = client.getPlayer().getParty();
		return p != null && p.getLeader() == client.getPlayer().getId();
	}

	public byte getPlayerPartyMembersInLevelCount(short min, short max) {
		PartyList p = client.getPlayer().getParty();
		if (p == null)
			return 0;
		byte count = 0;
		p.lockRead();
		try {
			for (PartyList.LocalMember m : p.getMembersInLocalChannel())
				if (m.getLevel() >= min && m.getLevel() <= max)
					count++;
		} finally {
			p.unlockRead();
		}
		return count;
	}

	public void sayInChat(String message) {
		client.getSession().send(GamePackets.writeServerMessage(GameChatHandler.TextStyle.LIGHT_BLUE_TEXT_CLEAR_BG.byteValue(), message, (byte) -1, true));
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

	public boolean isQuestCompleted(short questId) {
		return client.getPlayer().isQuestCompleted(questId);
	}

	public boolean isQuestActive(short questId) {
		return client.getPlayer().isQuestActive(questId);
	}

	public boolean isQuestStarted(short questId) {
		return client.getPlayer().isQuestStarted(questId);
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
