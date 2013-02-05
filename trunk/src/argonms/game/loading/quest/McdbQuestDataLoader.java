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

package argonms.game.loading.quest;

import argonms.common.util.DatabaseManager;
import argonms.common.util.DatabaseManager.DatabaseType;
import argonms.game.loading.quest.QuestRewards.SkillReward;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author GoldenKevin
 */
public class McdbQuestDataLoader extends QuestDataLoader {
	private static final Logger LOG = Logger.getLogger(McdbQuestDataLoader.class.getName());

	@Override
	public boolean loadAll() {
		if (!loadInfo())
			return false;
		if (!loadReq())
			return false;
		if (!loadAct())
			return false;
		return true;
	}

	@Override
	protected boolean loadInfo() {
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			con = DatabaseManager.getConnection(DatabaseType.WZ);
			ps = con.prepareStatement("SELECT `objectid`,`name` FROM `stringdata` WHERE `type` = 6");
			rs = ps.executeQuery();
			while (rs.next())
				questNames.put(Short.valueOf(rs.getShort(1)), rs.getString(2));
			return true;
		} catch (SQLException e) {
			LOG.log(Level.WARNING, "Error loading quest info data from the MCDB.", e);
			return false;
		} finally {
			DatabaseManager.cleanup(DatabaseType.WZ, rs, ps, con);
		}
	}

	@Override
	protected boolean loadAct() {
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			con = DatabaseManager.getConnection(DatabaseType.WZ);
			ps = con.prepareStatement("SELECT `questid`,`nextquest` FROM `questdata`");
			rs = ps.executeQuery();
			while (rs.next()) {
				short nextQuest = rs.getShort(2);
				if (nextQuest != 0) {
					QuestRewards qr = new QuestRewards();
					qr.setRewardQuest(nextQuest);
					completeRewards.put(Short.valueOf(rs.getShort(1)), qr);
				}
			}
			rs.close();
			ps.close();

			ps = con.prepareStatement("SELECT * FROM `questrewarddata`");
			rs = ps.executeQuery();
			while (rs.next()) {
				Short questId = Short.valueOf(rs.getShort(2));
				QuestRewards qr;
				if (rs.getBoolean(3)) { //start
					qr = startRewards.get(questId);
					if (qr == null) {
						qr = new QuestRewards();
						startRewards.put(questId, qr);
					}
				} else {
					qr = completeRewards.get(questId);
					if (qr == null) {
						qr = new QuestRewards();
						completeRewards.put(questId, qr);
					}
				}
				if (rs.getBoolean(4)) { //item
					QuestItemStats qis = new QuestItemStats(rs.getInt(11), rs.getShort(12));
					qis.setProb(rs.getInt(16));
					byte gender = rs.getByte(14);
					if (gender != -1)
						qis.setGender(gender);
					short job = rs.getShort(15);
					if (job != -1)
						qis.setJob(job);
					qr.addRewardItem(qis);
				} else if (rs.getBoolean(5)) { //exp
					qr.setRewardExp(rs.getInt(11));
				} else if (rs.getBoolean(6)) { //mesos
					qr.setRewardMoney(rs.getInt(11));
				} else if (rs.getBoolean(7)) { //fame
					qr.setRewardFame(rs.getShort(11));
				} else if (rs.getBoolean(8)) { //skill
					int skillId = rs.getInt(11);
					SkillReward sr = qr.getSkillReward(skillId);
					if (sr == null) {
						sr = new SkillReward(skillId, rs.getByte(12), rs.getByte(13), rs.getBoolean(9));
						qr.addRewardSkill(sr);
					}
					sr.addApplicableJob(rs.getShort(15));
				}
			}
			return true;
		} catch (SQLException e) {
			LOG.log(Level.WARNING, "Error loading quest action data from the MCDB.", e);
			return false;
		} finally {
			DatabaseManager.cleanup(DatabaseType.WZ, rs, ps, con);
		}
	}

	@Override
	protected boolean loadReq() {
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			con = DatabaseManager.getConnection(DatabaseType.WZ);
			ps = con.prepareStatement("SELECT * FROM `questrequestdata`");
			rs = ps.executeQuery();
			while (rs.next()) {
				short questId = rs.getShort(2);
				QuestChecks qc = completeReqs.get(Short.valueOf(questId));
				if (qc == null) {
					qc = new QuestChecks(questId);
					completeReqs.put(Short.valueOf(questId), qc);
				}
				if (rs.getBoolean(3)) {
					qc.addReqMobKills(rs.getInt(6), rs.getShort(7));
				} else if (rs.getBoolean(4)) {
					qc.addReqItem(new QuestItemStats(rs.getInt(6), rs.getShort(7)));
				} else if (rs.getBoolean(5)) {
					qc.addReqQuest(rs.getShort(6), rs.getByte(7));
				}
			}
			return true;
		} catch (SQLException e) {
			LOG.log(Level.WARNING, "Error loading quest check data from the MCDB.", e);
			return false;
		} finally {
			DatabaseManager.cleanup(DatabaseType.WZ, rs, ps, con);
		}
	}
}
