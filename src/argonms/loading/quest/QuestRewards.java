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

package argonms.loading.quest;

import argonms.character.Player;
import argonms.character.QuestEntry;
import argonms.character.inventory.Inventory;
import argonms.character.inventory.Inventory.InventoryType;
import argonms.character.inventory.InventorySlot;
import argonms.character.inventory.InventoryTools;
import argonms.character.inventory.InventoryTools.UpdatedSlots;
import argonms.character.inventory.ItemTools;
import argonms.game.GameServer;
import argonms.net.external.ClientSession;
import argonms.net.external.CommonPackets;
import argonms.tools.TimeUtil;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 *
 * @author GoldenKevin
 */
public class QuestRewards {
	private final List<QuestItemStats> items;
	private final Map<Short, Byte> questChanges;
	private final List<Short> jobs;
	private final List<SkillReward> skillChanges;
	private int sumItemProbs;
	private short minLevel;
	private int giveExp;
	private short nextQuest;
	private int giveMesos;
	private int giveBuff;
	private short givePetTameness;
	private short givePetSkill;
	private short giveFame;
	private long endDate;

	protected QuestRewards() {
		items = new ArrayList<QuestItemStats>();
		questChanges = new HashMap<Short, Byte>();
		jobs = new ArrayList<Short>();
		skillChanges = new ArrayList<SkillReward>();
	}

	protected void addRewardItem(QuestItemStats item) {
		sumItemProbs += item.getProb();
		items.add(item);
	}

	protected void addQuestToChange(short questId, byte state) {
		questChanges.put(Short.valueOf(questId), Byte.valueOf(state));
	}

	protected void addJob(short jobId) {
		jobs.add(Short.valueOf(jobId));
	}

	protected void addRewardSkill(SkillReward reward) {
		skillChanges.add(reward);
	}

	protected void setMinLevel(short level) {
		minLevel = level;
	}

	protected void setRewardExp(int exp) {
		giveExp = exp;
	}

	protected void setRewardQuest(short questId) {
		nextQuest = questId;
	}

	protected void setRewardMoney(int mesos) {
		giveMesos = mesos;
	}

	protected void setRewardBuff(int itemId) {
		giveBuff = itemId;
	}

	protected void setRewardPetTameness(short tameness) {
		givePetTameness = tameness;
	}

	protected void setRewardPetSkill(short petSkillId) {
		givePetSkill = petSkillId;
	}

	protected void setRewardFame(short pop) {
		giveFame = pop;
	}

	protected void setEndDate(int idate) {
		endDate = TimeUtil.intDateToCalendar(idate).getTimeInMillis();
	}

	protected void setRepeatInterval(int interval) {
		//you know what? I don't care about this value since
		//it takes too much effort to check in giveRewards. T.T
	}

	protected SkillReward getSkillReward(int skillId) {
		for (SkillReward sr : skillChanges)
			if (sr.skillId == skillId)
				return sr;
		return null;
	}

	private boolean canGiveItem(Player p, QuestItemStats item) {
		return item.jobMatch(p.getJob()) && item.genderMatch(p.getGender())
				&& item.notExpired() && item.roll(sumItemProbs);
	}

	private void giveItem(Player p, int itemId, short quantity, int period) {
		//TODO: set InventorySlot's expiration using item.getPeriod();
		ClientSession ses = p.getClient().getSession();
		InventoryType type = InventoryTools.getCategory(itemId);
		Inventory inv = p.getInventory(InventoryTools.getCategory(itemId));
		UpdatedSlots changedSlots = InventoryTools.addToInventory(inv, itemId, quantity);
		short pos;
		InventorySlot slot;
		for (Short s : changedSlots.modifiedSlots) {
			pos = s.shortValue();
			slot = inv.get(pos);
			ses.send(CommonPackets.writeInventorySlotUpdate(type, pos, slot));
		}
		for (Short s : changedSlots.addedOrRemovedSlots) {
			pos = s.shortValue();
			slot = inv.get(pos);
			ses.send(CommonPackets.writeInventoryAddSlot(type, pos, slot));
		}
		ses.send(CommonPackets.writeShowItemGainFromQuest(itemId, quantity));
		p.itemCountChanged(itemId);
	}

	private void takeItem(Player p, int itemId, short quantity) {
		ClientSession ses = p.getClient().getSession();
		InventoryType type = InventoryTools.getCategory(itemId);
		Inventory inv = p.getInventory(InventoryTools.getCategory(itemId));
		if (quantity == 0)
			quantity = (short) -InventoryTools.getAmountOfItem(inv, itemId);
		UpdatedSlots changedSlots = InventoryTools.removeFromInventory(p, itemId, (short) -quantity);
		short pos;
		InventorySlot slot;
		for (Short s : changedSlots.modifiedSlots) {
			pos = s.shortValue();
			slot = inv.get(pos);
			ses.send(CommonPackets.writeInventorySlotUpdate(type, pos, slot));
		}
		for (Short s : changedSlots.addedOrRemovedSlots) {
			pos = s.shortValue();
			ses.send(CommonPackets.writeInventoryClearSlot(type, pos));
		}
		ses.send(CommonPackets.writeShowItemGainFromQuest(itemId, quantity));
		p.itemCountChanged(itemId);
	}

	//TODO: check if we can fit all items in the player's inventory.
	private void awardItems(Player p) {
		boolean awardedRandomItem = false;
		for (QuestItemStats item : items) {
			if (canGiveItem(p, item)) {
				short quantity = item.getCount();
				if (quantity > 0) {
					if (item.getProb() == 0 || !awardedRandomItem)
						giveItem(p, item.getItemId(), quantity, item.getPeriod());
				} else {
					takeItem(p, item.getItemId(), quantity);
				}
				if (item.getProb() != 0)
					awardedRandomItem = true;
			}
		}
	}

	public short giveRewards(Player p, int selection) {
		//TODO: What do I do with selection?
		if (minLevel != 0 && p.getLevel() < minLevel
				|| endDate != 0 && System.currentTimeMillis() >= endDate
				|| !jobs.isEmpty() && !jobs.contains(Short.valueOf(p.getJob())))
			return -1; //Nexon fails. This should only be in Quest.wz/Check.img. T.T
		awardItems(p);
		for (Entry<Short, Byte> entry : questChanges.entrySet()) {
			switch (entry.getValue().byteValue()) {
				case QuestEntry.STATE_STARTED:
					p.localStartQuest(entry.getKey().shortValue());
					break;
				case QuestEntry.STATE_COMPLETED:
					p.localCompleteQuest(entry.getKey().shortValue(), -1);
					break;
			}
		}
		for (SkillReward skill : skillChanges)
			skill.applyTo(p);
		if (giveExp != 0)
			p.gainExp(giveExp * GameServer.getVariables().getExpRate(), false, true);
		if (giveMesos != 0)
			p.gainMesos(giveMesos * GameServer.getVariables().getMesoRate(), true);
		if (giveBuff != 0)
			ItemTools.useItem(p, giveBuff);
		if (givePetTameness != 0) {
			//TODO: WHICH PET DO WE APPLY THIS TO?
		}
		if (givePetSkill != 0) {
			//TODO: WHICH PET DO WE APPLY THIS TO?
		}
		if (giveFame != 0)
			p.gainFame(giveFame, true);
		return nextQuest;
	}

	protected static class SkillReward {
		private final List<Short> compatibleJobs;
		private int skillId;
		private int currentLevel, masterLevel;
		private boolean onlyMasterLevel;

		protected SkillReward(int skillId, byte skillLevel, byte masterLevel, boolean onlyMasterLevel) {
			this.compatibleJobs = new ArrayList<Short>();
			this.skillId = skillId;
			this.currentLevel = skillLevel;
			this.masterLevel = masterLevel;
			this.onlyMasterLevel = onlyMasterLevel;
		}

		protected void addApplicableJob(short jobId) {
			compatibleJobs.add(Short.valueOf(jobId));
		}

		protected void applyTo(Player p) {
			if (!compatibleJobs.contains(Short.valueOf(p.getJob())))
				return;
			//TODO: what the hell do all these variables mean???
		}
	}
}
