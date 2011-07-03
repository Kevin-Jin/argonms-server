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

package argonms.common.loading.quest;

import argonms.game.character.GameCharacter;
import argonms.common.loading.DataFileType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

//TODO: quests take a full 6MB of memory on unconditional preloading. be more
//efficient with memory usage in several of these structures?
/**
 *
 * @author GoldenKevin
 */
public abstract class QuestDataLoader {
	private static QuestDataLoader instance;

	protected final Map<Short, String> questNames;
	protected final List<Short> autoStart;
	protected final List<Short> autoPreComplete;
	protected final Map<Short, QuestRewards> startRewards;
	protected final Map<Short, QuestRewards> completeRewards;
	protected final Map<Short, QuestChecks> startReqs;
	protected final Map<Short, QuestChecks> completeReqs;

	protected QuestDataLoader() {
		questNames = new HashMap<Short, String>();
		autoStart = new ArrayList<Short>();
		autoPreComplete = new ArrayList<Short>();
		startReqs = new HashMap<Short, QuestChecks>();
		completeReqs = new HashMap<Short, QuestChecks>();
		startRewards = new HashMap<Short, QuestRewards>();
		completeRewards = new HashMap<Short, QuestRewards>();
	}

	public abstract boolean loadAll();

	protected abstract boolean loadInfo();

	protected abstract boolean loadReq();

	protected abstract boolean loadAct();

	public String getQuestNameFromId(short questid) {
		return questNames.get(Short.valueOf(questid));
	}

	public List<String> getSimilarNamedQuests(String reference) {
		List<String> retSkills = new ArrayList<String>();
		for (Entry<Short, String> name : questNames.entrySet())
			if (name.getValue().toLowerCase().contains(reference.toLowerCase()))
				retSkills.add(name.getKey() + " - " + name.getValue());
		return retSkills;
	}

	public boolean canStartQuest(GameCharacter p, short questId) {
		QuestChecks qc = startReqs.get(Short.valueOf(questId));
		if (qc != null)
			return qc.passesRequirements(p);
		//MCDB doesn't have a quest entry if there are no mob, item, or quest requirements
		//hope that questId is a real quest because we can't check if it is.
		return true;
	}

	public void startedQuest(GameCharacter p, short questId) {
		QuestRewards qr = startRewards.get(Short.valueOf(questId));
		if (qr != null)
			qr.giveRewards(p, -1);
	}

	public boolean canCompleteQuest(GameCharacter p, short questId) {
		QuestChecks qc = completeReqs.get(Short.valueOf(questId));
		if (qc != null)
			return qc.passesRequirements(p);
		//MCDB doesn't have a quest entry if there are no mob, item, or quest requirements
		//hope that questId is a real quest because we can't check if it is.
		return true;
	}

	public short finishedQuest(GameCharacter p, short questId, int selection) {
		QuestRewards qr = completeRewards.get(Short.valueOf(questId));
		if (qr != null)
			return qr.giveRewards(p, selection);
		return 0;
	}

	public QuestChecks getCompleteReqs(short questId) {
		return completeReqs.get(Short.valueOf(questId));
	}

	public String getStartScriptName(short questId) {
		QuestChecks qc = startReqs.get(Short.valueOf(questId));
		if (qc != null)
			return qc.getStartScriptName();
		return null;
	}

	public String getEndScriptName(short questId) {
		QuestChecks qc = completeReqs.get(Short.valueOf(questId));
		if (qc != null)
			return qc.getEndScriptName();
		return null;
	}

	public static void setInstance(DataFileType wzType, String wzPath) {
		if (instance == null) {
			switch (wzType) {
				case KVJ:
					instance = new KvjQuestDataLoader(wzPath);
					break;
				case MCDB:
					instance = new McdbQuestDataLoader();
					break;
			}
		}
	}

	public static QuestDataLoader getInstance() {
		return instance;
	}
}
