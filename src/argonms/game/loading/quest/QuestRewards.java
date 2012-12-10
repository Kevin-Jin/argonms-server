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

package argonms.game.loading.quest;

import argonms.common.character.QuestEntry;
import argonms.common.character.inventory.Inventory;
import argonms.common.character.inventory.Inventory.InventoryType;
import argonms.common.character.inventory.InventorySlot;
import argonms.common.character.inventory.InventoryTools;
import argonms.common.character.inventory.InventoryTools.UpdatedSlots;
import argonms.common.net.external.ClientSession;
import argonms.common.util.Rng;
import argonms.common.util.TimeTool;
import argonms.game.GameServer;
import argonms.game.character.GameCharacter;
import argonms.game.character.inventory.ItemTools;
import argonms.game.net.external.GamePackets;
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
		endDate = TimeTool.intDateToCalendar(idate).getTimeInMillis();
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

	private boolean canGiveItem(GameCharacter p, QuestItemStats item) {
		return item.jobMatch(p.getJob()) && item.genderMatch(p.getGender()) && item.notExpired();
	}

	private void giveItem(GameCharacter p, int itemId, short quantity, int period) {
		InventoryType type = InventoryTools.getCategory(itemId);
		Inventory inv = p.getInventory(InventoryTools.getCategory(itemId));
		InventorySlot slot = InventoryTools.makeItemWithId(itemId);
		//period is stored in minutes for quests
		if (period != 0)
			slot.setExpiration(System.currentTimeMillis() + (period * 1000 * 60));
		UpdatedSlots changedSlots = InventoryTools.addToInventory(inv, slot, quantity, true);
		ClientSession<?> ses = p.getClient().getSession();
		short pos;
		for (Short s : changedSlots.modifiedSlots) {
			pos = s.shortValue();
			ses.send(GamePackets.writeInventoryUpdateSlotQuantity(type, pos, inv.get(pos)));
		}
		for (Short s : changedSlots.addedOrRemovedSlots) {
			pos = s.shortValue();
			ses.send(GamePackets.writeInventoryAddSlot(type, pos, inv.get(pos)));
		}
		p.itemCountChanged(itemId);
		ses.send(GamePackets.writeShowItemGainFromQuest(itemId, quantity));
	}

	private void takeItem(GameCharacter p, int itemId, short quantity) {
		InventoryType type = InventoryTools.getCategory(itemId);
		Inventory inv = p.getInventory(InventoryTools.getCategory(itemId));
		if (quantity == 0)
			quantity = (short) -InventoryTools.getAmountOfItem(inv, itemId);
		UpdatedSlots changedSlots = InventoryTools.removeFromInventory(p, itemId, -quantity);
		ClientSession<?> ses = p.getClient().getSession();
		short pos;
		for (Short s : changedSlots.modifiedSlots) {
			pos = s.shortValue();
			ses.send(GamePackets.writeInventoryUpdateSlotQuantity(type, pos, inv.get(pos)));
		}
		for (Short s : changedSlots.addedOrRemovedSlots) {
			pos = s.shortValue();
			ses.send(GamePackets.writeInventoryClearSlot(type, pos));
		}
		p.itemCountChanged(itemId);
		ses.send(GamePackets.writeShowItemGainFromQuest(itemId, quantity));
	}

	//TODO: check if we can fit all items in the player's inventory.
	private void awardItems(GameCharacter p, int selection) {
		boolean findRandomItem = (sumItemProbs > 0);
		int selectableItemIndex = 0;
		int random = findRandomItem ? Rng.getGenerator().nextInt(sumItemProbs) : 0, runningProbs = 0;

		for (QuestItemStats item : items) {
			boolean give = canGiveItem(p, item);
			if (item.getProb() != 0 && give) {
				if (item.getProb() == -1) {
					//items List better keep the order of the item rewards in
					//Quest.wz/Act.img...
					if (selectableItemIndex != selection)
						give = false;
					selectableItemIndex++;
				} else {
					if (findRandomItem && random < (runningProbs += item.getProb()))
						//use this item - leave give = true and don't look for more random items
						findRandomItem = false;
					else
						//don't give this item
						give = false;
				}
			}
			if (give) {
				short quantity = item.getCount();
				if (quantity > 0)
					giveItem(p, item.getItemId(), quantity, item.getPeriod());
				else
					takeItem(p, item.getItemId(), quantity);
			}
		}
	}

	public short giveRewards(GameCharacter p, int selection) {
		//TODO: What do I do with selection?
		if (minLevel != 0 && p.getLevel() < minLevel
				|| endDate != 0 && System.currentTimeMillis() >= endDate
				|| !jobs.isEmpty() && !jobs.contains(Short.valueOf(p.getJob())))
			return -1; //Nexon fails. This should only be in Quest.wz/Check.img. T.T
		awardItems(p, selection);
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
			p.gainExp((int) Math.min((long) giveExp * GameServer.getVariables().getExpRate(), Integer.MAX_VALUE), false, true);
		if (giveMesos != 0)
			p.gainMesos((int) Math.min((long) giveMesos * GameServer.getVariables().getMesoRate(), Integer.MAX_VALUE), true);
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
		private byte currentLevel, masterLevel;
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

		protected void applyTo(GameCharacter p) {
			if (!compatibleJobs.contains(Short.valueOf(p.getJob())))
				return;
			p.setSkillLevel(skillId, currentLevel, masterLevel, onlyMasterLevel);
		}
	}
}
