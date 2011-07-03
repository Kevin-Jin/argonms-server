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

import argonms.common.loading.quest.QuestRewards.SkillReward;
import argonms.common.tools.input.LittleEndianByteArrayReader;
import argonms.common.tools.input.LittleEndianReader;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author GoldenKevin
 */
public class KvjQuestDataLoader extends QuestDataLoader {
	private static final byte //main
		QUEST_ACTION = 1,
		QUEST_CHECK = 2,
		QUEST_INFO = 3
	;

	private static final byte //quest info
		END_QUEST_INFO = 0,
		AUTO_START = 1,
		AUTO_PRE_COMPLETE = 2
	;

	private static final byte //actions, checks
		END_BEHAVIOR = 0,
		MIN_LEVEL = 1,
		MAX_LEVEL = 2,
		FAME = 3,
		QUEST_END_DATE = 4,
		START_SCRIPT = 5,
		END_SCRIPT = 6,
		REPEAT_INTERVAL = 7,
		REQ_PET_TAMENESS = 8,
		REQ_MOUNT_TAMENESS = 9,
		REQ_MESOS = 10,
		MIN_POPULATION = 11,
		MAX_POPULATION = 12,
	
		ITEM_PROP = 13,
		ITEM_GENDER = 14,
		ITEM_JOB = 15,
		ITEM_DATE_EXPIRE = 16,
		ITEM_PERIOD = 17,
		END_ITEM = 18,
	
		REWARD_EXP_GAIN = 19,
		REWARD_NEXT_QUEST = 20,
		REWARD_MESOS = 21,
		REWARD_BUFF = 22,
		REWARD_PET_TAMENESS = 23,
		REWARD_PET_SKILL = 24
	;

	private static final Logger LOG = Logger.getLogger(KvjQuestDataLoader.class.getName());

	private String dataPath;

	protected KvjQuestDataLoader(String wzPath) {
		this.dataPath = wzPath;
	}

	public boolean loadAll() {
		try {
			File root = new File(dataPath + "Quest.wz");
			for (String kvj : root.list())
				doWork(new LittleEndianByteArrayReader(new File(root.getAbsolutePath() + File.separatorChar + kvj)));
			return true;
		} catch (IOException ex) {
			LOG.log(Level.WARNING, "Could not load all quest data from KVJ files.", ex);
			return false;
		}
	}

	protected boolean loadInfo() {
		try {
			File f = new File(dataPath + "Quest.wz" + "QuestInfo.img.kvj");
			if (f.exists())
				doWork(new LittleEndianByteArrayReader(f));
			return true;
		} catch (IOException e) {
			LOG.log(Level.WARNING, "Could not read KVJ data file for quest info", e);
			return false;
		}
	}

	protected boolean loadReq() {
		try {
			File f = new File(dataPath + "Quest.wz" + "Check.img.kvj");
			if (f.exists())
				doWork(new LittleEndianByteArrayReader(f));
			return true;
		} catch (IOException e) {
			LOG.log(Level.WARNING, "Could not read KVJ data file for quest checks", e);
			return false;
		}
	}

	protected boolean loadAct() {
		try {
			File f = new File(dataPath + "Quest.wz" + "Act.img.kvj");
			if (f.exists())
				doWork(new LittleEndianByteArrayReader(f));
			return true;
		} catch (IOException e) {
			LOG.log(Level.WARNING, "Could not read KVJ data file for quest actions", e);
			return false;
		}
	}

	private void doWork(LittleEndianReader reader) {
		for (byte now = reader.readByte(); now != -1; now = reader.readByte()) {
			switch (now) {
				case QUEST_ACTION:
					processQuestActions(reader);
					break;
				case QUEST_CHECK:
					processQuestChecks(reader);
					break;
				case QUEST_INFO:
					processQuestInfo(reader);
					break;
			}
		}
	}

	private QuestItemStats processQuestItem(LittleEndianReader reader) {
		int itemId = reader.readInt();
		short quantity = reader.readShort();
		QuestItemStats qis = new QuestItemStats(itemId, quantity);
		for (byte now = reader.readByte(); now != END_ITEM; now = reader.readByte()) {
			switch (now) {
				case ITEM_PROP:
					qis.setProb(reader.readInt());
					break;
				case ITEM_GENDER:
					qis.setGender(reader.readByte());
					break;
				case ITEM_JOB:
					qis.setJob(reader.readShort());
					break;
				case ITEM_DATE_EXPIRE:
					qis.setDateExpire(reader.readInt());
					break;
				case ITEM_PERIOD:
					qis.setPeriod(reader.readInt());
					break;
			}
		}
		return qis;
	}

	private SkillReward processQuestSkillAction(LittleEndianReader reader) {
		int skillId = reader.readInt();
		byte skillLevel = reader.readByte();
		byte masterLevel = reader.readByte();
		boolean onlyMasterLevel = reader.readBool();
		SkillReward r = new SkillReward(skillId, skillLevel, masterLevel, onlyMasterLevel);
		byte amount = reader.readByte();
		for (int i = 0; i < amount; i++)
			r.addApplicableJob(reader.readShort());
		return r;
	}

	private void processQuestAction(short questId, boolean completionActs, LittleEndianReader reader) {
		QuestRewards qr = new QuestRewards();
		byte amount = reader.readByte();
		for (int i = 0; i < amount; i++)
			qr.addRewardItem(processQuestItem(reader));
		amount = reader.readByte();
		for (int i = 0; i < amount; i++)
			qr.addQuestToChange(reader.readShort(), reader.readByte());
		amount = reader.readByte();
		for (int i = 0; i < amount; i++)
			qr.addJob(reader.readShort());
		amount = reader.readByte();
		for (int i = 0; i < amount; i++)
			qr.addRewardSkill(processQuestSkillAction(reader));
		for (byte now = reader.readByte(); now != END_BEHAVIOR; now = reader.readByte()) {
			switch (now) {
				case MIN_LEVEL:
					qr.setMinLevel(reader.readShort());
					break;
				case FAME:
					qr.setRewardFame(reader.readShort());
					break;
				case QUEST_END_DATE:
					qr.setEndDate(reader.readInt());
					break;
				case REPEAT_INTERVAL:
					qr.setRepeatInterval(reader.readInt());
					break;
				case REWARD_EXP_GAIN:
					qr.setRewardExp(reader.readInt());
					break;
				case REWARD_NEXT_QUEST:
					qr.setRewardQuest(reader.readShort());
					break;
				case REWARD_MESOS:
					qr.setRewardMoney(reader.readInt());
					break;
				case REWARD_BUFF:
					qr.setRewardBuff(reader.readInt());
					break;
				case REWARD_PET_TAMENESS:
					qr.setRewardPetTameness(reader.readShort());
					break;
				case REWARD_PET_SKILL:
					qr.setRewardPetSkill(reader.readShort());
					break;
			}
		}
		if (completionActs)
			completeRewards.put(Short.valueOf(questId), qr);
		else
			startRewards.put(Short.valueOf(questId), qr);
	}

	private void processQuestActions(LittleEndianReader reader) {
		short questId = reader.readShort();
		processQuestAction(questId, false, reader);
		processQuestAction(questId, true, reader);
	}

	private void processQuestCheck(short questId, boolean completionChecks, LittleEndianReader reader) {
		QuestChecks qc = new QuestChecks(questId);
		byte amount = reader.readByte();
		for (int i = 0; i < amount; i++)
			qc.addReqItem(processQuestItem(reader));
		amount = reader.readByte();
		for (int i = 0; i < amount; i++)
			qc.addReqQuest(reader.readShort(), reader.readByte());
		amount = reader.readByte();
		for (int i = 0; i < amount; i++)
			qc.addReqJob(reader.readShort());
		amount = reader.readByte();
		for (int i = 0; i < amount; i++)
			qc.addReqSkill(reader.readInt(), reader.readInt());
		amount = reader.readByte();
		for (int i = 0; i < amount; i++)
			qc.addReqMobKills(reader.readInt(), reader.readShort());
		amount = reader.readByte();
		for (int i = 0; i < amount; i++)
			qc.addReqPet(reader.readInt());
		for (byte now = reader.readByte(); now != END_BEHAVIOR; now = reader.readByte()) {
			switch (now) {
				case MIN_LEVEL:
					qc.setMinLevel(reader.readShort());
					break;
				case MAX_LEVEL:
					qc.setMaxLevel(reader.readShort());
					break;
				case FAME:
					qc.setReqFame(reader.readShort());
					break;
				case QUEST_END_DATE:
					qc.setEndDate(reader.readInt());
					break;
				case REPEAT_INTERVAL:
					qc.setRepeatInterval(reader.readInt());
					break;
				case REQ_PET_TAMENESS:
					qc.setReqPetTameness(reader.readShort());
					break;
				case REQ_MOUNT_TAMENESS:
					qc.setReqMountTameness(reader.readShort());
					break;
				case REQ_MESOS:
					qc.setReqMesos(reader.readShort());
					break;
				case MIN_POPULATION:
					qc.setMinPopulation(reader.readShort());
					break;
				case MAX_POPULATION:
					qc.setMaxPopulation(reader.readShort());
					break;
				case START_SCRIPT:
					qc.setStartScript(reader.readNullTerminatedString());
					break;
				case END_SCRIPT:
					qc.setEndScript(reader.readNullTerminatedString());
					break;
			}
		}
		if (completionChecks)
			completeReqs.put(Short.valueOf(questId), qc);
		else
			startReqs.put(Short.valueOf(questId), qc);
	}

	private void processQuestChecks(LittleEndianReader reader) {
		short questId = reader.readShort();
		processQuestCheck(questId, false, reader);
		processQuestCheck(questId, true, reader);
	}

	private void processQuestInfo(LittleEndianReader reader) {
		short questId = reader.readShort();
		questNames.put(Short.valueOf(questId), reader.readNullTerminatedString());
		for (byte now = reader.readByte(); now != END_QUEST_INFO; now = reader.readByte()) {
			switch (now) {
				case AUTO_START:
					autoStart.add(Short.valueOf(questId));
					break;
				case AUTO_PRE_COMPLETE:
					autoPreComplete.add(Short.valueOf(questId));
					break;
			}
		}
	}
}
