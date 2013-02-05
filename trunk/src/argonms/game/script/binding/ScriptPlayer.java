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

package argonms.game.script.binding;

import argonms.common.UserPrivileges;
import argonms.common.character.BuddyList;
import argonms.common.character.Skills;
import argonms.common.character.inventory.Inventory;
import argonms.common.character.inventory.InventoryTools;
import argonms.common.character.inventory.Pet;
import argonms.common.net.external.ClientSession;
import argonms.game.GameServer;
import argonms.game.character.GameCharacter;
import argonms.game.character.GuildList;
import argonms.game.loading.skill.SkillDataLoader;
import argonms.game.net.external.GamePackets;
import argonms.game.net.external.handler.GuildListHandler;
import java.lang.ref.WeakReference;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.NativeArray;

/**
 *
 * @author GoldenKevin
 */
public class ScriptPlayer {
	private WeakReference<GameCharacter> player;

	public ScriptPlayer(GameCharacter player) {
		this.player = new WeakReference<GameCharacter>(player);
	}

	protected GameCharacter getPlayer() {
		return player.get();
	}

	public int getId() {
		return getPlayer().getId();
	}

	public void gainExp(int gain) {
		getPlayer().gainExp((int) Math.min((long) gain * GameServer.getVariables().getExpRate(), Integer.MAX_VALUE), false, true);
	}

	public boolean canGainItem(int itemId, int quantity) {
		return InventoryTools.canFitEntirely(getPlayer().getInventory(InventoryTools.getCategory(itemId)), itemId, quantity, true);
	}

	public boolean hasItem(int itemId, int quantity) {
		return InventoryTools.hasItem(getPlayer(), itemId, quantity);
	}

	public boolean gainItem(int itemId, int quantity) {
		Inventory.InventoryType type = InventoryTools.getCategory(itemId);
		Inventory inv = getPlayer().getInventory(type);
		if (InventoryTools.canFitEntirely(inv, itemId, quantity, true)) {
			InventoryTools.UpdatedSlots changedSlots = InventoryTools.addToInventory(inv, itemId, quantity);
			ClientSession<?> ses = getPlayer().getClient().getSession();
			short pos;
			for (Short s : changedSlots.modifiedSlots) {
				pos = s.shortValue();
				ses.send(GamePackets.writeInventoryUpdateSlotQuantity(type, pos, inv.get(pos)));
			}
			for (Short s : changedSlots.addedOrRemovedSlots) {
				pos = s.shortValue();
				ses.send(GamePackets.writeInventoryAddSlot(type, pos, inv.get(pos)));
			}
			getPlayer().itemCountChanged(itemId);
			ses.send(GamePackets.writeShowItemGainFromQuest(itemId, quantity));
			return true;
		}
		return false;
	}

	public boolean loseItem(int itemId, int quantity) {
		if (InventoryTools.hasItem(getPlayer(), itemId, quantity)) {
			Inventory.InventoryType type = InventoryTools.getCategory(itemId);
			Inventory inv = getPlayer().getInventory(type);
			InventoryTools.UpdatedSlots changedSlots = InventoryTools.removeFromInventory(getPlayer(), itemId, quantity);
			ClientSession<?> ses = getPlayer().getClient().getSession();
			short pos;
			for (Short s : changedSlots.modifiedSlots) {
				pos = s.shortValue();
				ses.send(GamePackets.writeInventoryUpdateSlotQuantity(type, pos, inv.get(pos)));
			}
			for (Short s : changedSlots.addedOrRemovedSlots) {
				pos = s.shortValue();
				ses.send(GamePackets.writeInventoryClearSlot(type, pos));
			}
			getPlayer().itemCountChanged(itemId);
			ses.send(GamePackets.writeShowItemGainFromQuest(itemId, -quantity));
			return true;
		}
		return false;
	}

	public void loseItem(int itemId) {
		Inventory.InventoryType type = InventoryTools.getCategory(itemId);
		int quantity = InventoryTools.getAmountOfItem(getPlayer().getInventory(type), itemId);
		if (type == Inventory.InventoryType.EQUIP)
			quantity += InventoryTools.getAmountOfItem(getPlayer().getInventory(Inventory.InventoryType.EQUIPPED), itemId);
		if (quantity > 0)
			loseItem(itemId, quantity);
	}

	public boolean hasMesos(int min) {
		return getPlayer().getMesos() >= min;
	}

	public void gainMesos(int gain) {
		getPlayer().gainMesos((int) Math.min((long) gain * GameServer.getVariables().getMesoRate(), Integer.MAX_VALUE), true);
	}

	public void loseMesos(int lose) {
		getPlayer().gainMesos(-lose, true);
	}

	public void changeMap(int mapId) {
		getPlayer().changeMap(mapId);
	}

	public void changeMap(int mapId, byte portal) {
		getPlayer().changeMap(mapId, portal);
	}

	public void changeMap(int mapId, String portal) {
		changeMap(mapId, GameServer.getChannel(getPlayer().getClient().getChannel()).getMapFactory().getMap(mapId).getPortalIdByName(portal));
	}

	public void changeMap(ScriptField map) {
		getPlayer().changeMap(map.getMap(), (byte) 0);
	}

	public short getLevel() {
		return getPlayer().getLevel();
	}

	public short getJob() {
		return getPlayer().getJob();
	}

	public void setJob(short newJob) {
		getPlayer().setJob(newJob);
	}

	public byte getGender() {
		return getPlayer().getGender();
	}

	public short getHair() {
		return getPlayer().getHair();
	}

	public void setHair(short newHair) {
		getPlayer().setHair(newHair);
	}

	public void setSkin(byte newSkin) {
		getPlayer().setSkin(newSkin);
	}

	public short getFace() {
		return getPlayer().getEyes();
	}

	public void setFace(short newEyes) {
		getPlayer().setEyes(newEyes);
	}

	public short getStr() {
		return getPlayer().getStr();
	}

	public short getDex() {
		return getPlayer().getDex();
	}

	public short getInt() {
		return getPlayer().getInt();
	}

	public short getLuk() {
		return getPlayer().getLuk();
	}

	public short getHp() {
		return getPlayer().getHp();
	}

	public void increaseMaxHp(short delta) {
		getPlayer().setMaxHp((short) (getPlayer().getMaxHp() + delta));
		getPlayer().gainHp(delta);
	}

	public void increaseMaxMp(short delta) {
		getPlayer().setMaxMp((short) (getPlayer().getMaxMp() + delta));
		getPlayer().gainMp(delta);
	}

	public void setHp(short newHp) {
		getPlayer().setHp(newHp);
	}

	public boolean isGm() {
		return getPlayer().getPrivilegeLevel() >= UserPrivileges.GM;
	}

	public void maxSkills() {
		for (int skillid : Skills.ALL) {
			byte masterLevel = SkillDataLoader.getInstance().getSkill(skillid).maxLevel();
			getPlayer().setSkillLevel(skillid, masterLevel, masterLevel, false);
		}
	}

	public void gainEquipInventorySlots(short delta) {
		short newCap = getPlayer().getInventory(Inventory.InventoryType.EQUIP).increaseCapacity(delta);
		getPlayer().getClient().getSession().send(GamePackets.writeInventoryUpdateCapacity(Inventory.InventoryType.EQUIP, newCap));
	}

	public void gainUseInventorySlots(short delta) {
		short newCap = getPlayer().getInventory(Inventory.InventoryType.USE).increaseCapacity(delta);
		getPlayer().getClient().getSession().send(GamePackets.writeInventoryUpdateCapacity(Inventory.InventoryType.USE, newCap));
	}

	public void gainSetupInventorySlots(short delta) {
		short newCap = getPlayer().getInventory(Inventory.InventoryType.SETUP).increaseCapacity(delta);
		getPlayer().getClient().getSession().send(GamePackets.writeInventoryUpdateCapacity(Inventory.InventoryType.SETUP, newCap));
	}

	public void gainEtcInventorySlots(short delta) {
		short newCap = getPlayer().getInventory(Inventory.InventoryType.ETC).increaseCapacity(delta);
		getPlayer().getClient().getSession().send(GamePackets.writeInventoryUpdateCapacity(Inventory.InventoryType.ETC, newCap));
	}

	public void gainCashInventorySlots(short delta) {
		short newCap = getPlayer().getInventory(Inventory.InventoryType.CASH).increaseCapacity(delta);
		getPlayer().getClient().getSession().send(GamePackets.writeInventoryUpdateCapacity(Inventory.InventoryType.CASH, newCap));
	}

	public void gainSp(short gain) {
		getPlayer().setSp((short) (getPlayer().getSp() + gain));
	}

	public short getSp() {
		return getPlayer().getSp();
	}

	public String getName() {
		return getPlayer().getName();
	}

	public short getBuddyCapacity() {
		return getPlayer().getBuddyList().getCapacity();
	}

	public void gainBuddySlots(short gain) {
		BuddyList bList = getPlayer().getBuddyList();
		bList.increaseCapacity(gain);
		getPlayer().getClient().getSession().send(GamePackets.writeBuddyCapacityUpdate(bList.getCapacity()));
	}

	public boolean isQuestCompleted(short questId) {
		return getPlayer().isQuestCompleted(questId);
	}

	public boolean isQuestActive(short questId) {
		return getPlayer().isQuestActive(questId);
	}

	public boolean isQuestStarted(short questId) {
		return getPlayer().isQuestStarted(questId);
	}

	public void startQuest(short questId, int npcId) {
		getPlayer().startQuest(questId, npcId);
	}

	public void completeQuest(short questId, int npcId) {
		getPlayer().completeQuest(questId, npcId, -1);
	}

	public byte getPetCount() {
		byte count = 0;
		for (Pet p : getPlayer().getPets())
			if (p != null)
				count++;
		return count;
	}

	public void gainCloseness(short gain) {
		//TODO: implement pets and pet exp rate?
	}

	public void setEvent(ScriptEvent event) {
		getPlayer().setEvent(event == null ? null : event.getScriptInterface());
	}

	public void showTimer(int seconds) {
		getPlayer().getClient().getSession().send(GamePackets.writeTimer(seconds));
	}

	public String createGuild(String name) {
		if (getPlayer().getLevel() <= 10) {
			getPlayer().getClient().getSession().send(GamePackets.writeSimpleGuildListMessage(GuildListHandler.LEVEL_TOO_LOW));
			//haven't captured continuation yet, so we don't need to call this:
			//getPlayer().getClient().getNpc().guildNameReceived(null);
			return null;
		}
		GameServer.getChannel(getPlayer().getClient().getChannel()).getCrossServerInterface().sendMakeGuild(name, getPlayer().getParty());
		Context cx = Context.enter();
		try {
			throw cx.captureContinuation();
		} finally {
			Context.exit();
		}
	}

	public int getGuildId() {
		return getPlayer().getGuild() == null ? 0 : getPlayer().getGuild().getId();
	}

	public byte getGuildRank() {
		GuildList guild = getPlayer().getGuild();
		if (guild == null)
			return 5;
		return guild.getMember(getPlayer().getId()).getRank();
	}

	public int getGuildCapacity() {
		GuildList guild = getPlayer().getGuild();
		if (guild == null)
			return 0;
		return guild.getCapacity();
	}

	public void increaseGuildCapacity(byte amount) {
		GameServer.getChannel(getPlayer().getClient().getChannel()).getCrossServerInterface().sendExpandGuild(getPlayer().getGuild(), amount);
	}

	public boolean hasGuildEmblem() {
		GuildList guild = getPlayer().getGuild();
		if (guild == null)
			return false;
		return guild.getEmblemBackground() != 0 || guild.getEmblemBackgroundColor() != 0
				&& guild.getEmblemDesign() != 0 || guild.getEmblemDesignColor() != 0;
	}

	public void setGuildEmblem(NativeArray arr) {
		short background = ((Number) arr.get(0, null)).shortValue();
		byte backgroundColor = ((Number) arr.get(1, null)).byteValue();
		short design = ((Number) arr.get(2, null)).shortValue();
		byte designColor = ((Number) arr.get(3, null)).byteValue();
		GameServer.getChannel(getPlayer().getClient().getChannel()).getCrossServerInterface().sendUpdateGuildEmblem(getPlayer().getGuild(), background, backgroundColor, design, designColor);
	}

	public byte getAllianceRank() {
		GuildList guild = getPlayer().getGuild();
		if (guild == null)
			throw new UnsupportedOperationException("TODO: IMPLEMENT");
		return guild.getMember(getPlayer().getId()).getAllianceRank();
	}

	public void disbandGuild() {
		GameServer.getChannel(getPlayer().getClient().getChannel()).getCrossServerInterface().sendDisbandGuild(getPlayer().getGuild().getId());
	}

	public void createAlliance() {
		throw new UnsupportedOperationException("TODO: IMPLEMENT");
	}

	public int getAllianceCapacity() {
		throw new UnsupportedOperationException("TODO: IMPLEMENT");
	}

	public void increaseAllianceCapacity() {
		throw new UnsupportedOperationException("TODO: IMPLEMENT");
	}
}
