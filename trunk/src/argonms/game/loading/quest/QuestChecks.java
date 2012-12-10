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

import argonms.common.GlobalConstants;
import argonms.common.character.PlayerJob;
import argonms.common.character.QuestEntry;
import argonms.common.character.inventory.InventoryTools;
import argonms.common.character.inventory.Pet;
import argonms.common.util.TimeTool;
import argonms.game.character.GameCharacter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 *
 * @author GoldenKevin
 */
public class QuestChecks {
	public enum QuestRequirementType {
		MOB, ITEM, MESOS, PET, PET_TAMENESS, QUEST
	}

	private final short questId;
	private final List<QuestItemStats> reqItems;
	private final Map<Short, Byte> reqQuests;
	private final List<Short> reqJobs;
	private final Map<Integer, Integer> reqSkills;
	private final Map<Integer, Short> reqMobs;
	private final List<Integer> reqPets;
	private short minLevel, maxLevel;
	private short minFame;
	private long endDate;
	private String startScript, endScript;
	private int repeatInterval;
	private short reqPetTameness;
	private short reqMountTameness;
	private short minMesos;
	private short minPop, maxPop;

	protected QuestChecks(short questId) {
		this.questId = questId;
		reqItems = new ArrayList<QuestItemStats>();
		reqQuests = new HashMap<Short, Byte>();
		reqSkills = new HashMap<Integer, Integer>();
		reqMobs = new LinkedHashMap<Integer, Short>();
		reqPets = new ArrayList<Integer>();
		reqJobs = new ArrayList<Short>();
		maxLevel = GlobalConstants.MAX_LEVEL;
		endDate = Long.MAX_VALUE;
		repeatInterval = -1;
	}

	protected void addReqItem(QuestItemStats item) {
		reqItems.add(item);
	}

	protected void addReqQuest(short questId, byte state) {
		reqQuests.put(Short.valueOf(questId), Byte.valueOf(state));
	}

	protected void addReqJob(short jobId) {
		reqJobs.add(Short.valueOf(jobId));
	}

	protected void addReqSkill(int skillId, int acquire) {
		reqSkills.put(Integer.valueOf(skillId), Integer.valueOf(acquire));
	}

	protected void addReqMobKills(int mobId, short minCount) {
		reqMobs.put(Integer.valueOf(mobId), Short.valueOf(minCount));
	}

	protected void addReqPet(int itemId) {
		reqPets.add(Integer.valueOf(itemId));
	}

	protected void setMinLevel(short level) {
		minLevel = level;
	}

	protected void setMaxLevel(short level) {
		maxLevel = level;
	}

	public void setReqFame(short pop) {
		minFame = pop;
	}

	protected void setEndDate(int idate) {
		endDate = TimeTool.intDateToCalendar(idate).getTimeInMillis();
	}

	protected void setStartScript(String scriptName) {
		startScript = scriptName;
	}

	protected void setEndScript(String scriptName) {
		endScript = scriptName;
	}

	protected void setRepeatInterval(int interval) {
		repeatInterval = interval;
	}

	protected void setReqPetTameness(short minTameness) {
		reqPetTameness = minTameness;
	}

	protected void setReqMesos(short mesos) {
		minMesos = mesos;
	}

	protected void setReqMountTameness(short tameness) {
		reqMountTameness = tameness;
	}

	protected void setMinPopulation(short pop) {
		minPop = pop;
	}

	protected void setMaxPopulation(short pop) {
		maxPop = pop;
	}

	private boolean hasPet(GameCharacter p) {
		if (reqPets.isEmpty())
			return true;
		for (Pet pet : p.getPets())
			if (pet != null && reqPets.contains(Integer.valueOf(pet.getDataId())))
				return true;
		return false;
	}

	public boolean passesRequirements(GameCharacter p) {
		if (System.currentTimeMillis() >= endDate
				|| p.getFame() < minFame || !hasPet(p)
				|| p.getLevel() < minLevel || p.getLevel() > maxLevel
				|| p.getMesos() < minMesos)
			return false;
		if (!reqJobs.isEmpty() && !reqJobs.contains(Short.valueOf(p.getJob())) && !PlayerJob.isGameMaster(p.getJob()))
			return false;
		for (QuestItemStats item : reqItems) {
			int itemId = item.getItemId();
			if (!p.getInventory(InventoryTools.getCategory(itemId)).hasItem(itemId, item.getCount()))
				return false;
		}
		Map<Short, QuestEntry> statuses = p.getAllQuests();
		QuestEntry status;
		p.readLockQuests();
		try {
			for (Entry<Short, Byte> entry : reqQuests.entrySet()) {
				status = statuses.get(entry.getKey());
				if (status != null) {
					if (entry.getValue().byteValue() != status.getState())
						return false;
				} else {
					if (entry.getValue().byteValue() != QuestEntry.STATE_NOT_STARTED)
						return false;
				}
			}
		} finally {
			p.readUnlockQuests();
		}
		for (Entry<Integer, Integer> entry : reqSkills.entrySet()) {
			//TODO: what the hell? what is the value supposed to mean?
		}
		status = statuses.get(Short.valueOf(questId));
		if (!reqMobs.isEmpty()) {
			if (status == null)
				return false;
			for (Entry<Integer, Short> entry : reqMobs.entrySet()) {
				if (status.getMobCount(entry.getKey().intValue())
						< entry.getValue().shortValue())
					return false;
			}
		}
		if (repeatInterval != -1) {
			if (status != null && status.getState() == QuestEntry.STATE_COMPLETED)
				//repeatInterval is in minutes, so convert it to milliseconds
				if (System.currentTimeMillis() < status.getCompletionTime() + (long) repeatInterval * 60000)
					return false;
		}
		if (reqPetTameness != 0) {
			//TODO: WHICH PET DO WE CHECK?
		}
		if (reqMountTameness != 0) {
			//TODO: check mount tameness
		}
		if (minPop != 0) {
			//TODO: check world population
		}
		if (maxPop != 0) {
			//TODO: check world population
		}
		return true;
	}

	public String getStartScriptName() {
		return startScript;
	}

	public String getEndScriptName() {
		return endScript;
	}

	public Map<Integer, Short> getReqMobCounts() {
		return Collections.unmodifiableMap(reqMobs);
	}

	public Map<Integer, Short> getReqItems() {
		Map<Integer, Short> reqItemCounts = new HashMap<Integer, Short>();
		for (QuestItemStats qis : reqItems)
			reqItemCounts.put(Integer.valueOf(qis.getItemId()), Short.valueOf(qis.getCount()));
		return reqItemCounts;
	}

	public List<Integer> getReqPets() {
		return reqPets;
	}

	public Map<Short, Byte> getReqQuests() {
		return reqQuests;
	}

	public boolean requiresMesos() {
		return minMesos != 0;
	}
}
