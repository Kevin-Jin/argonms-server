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

import argonms.common.character.PlayerJob;
import argonms.common.util.TimeTool;

/**
 *
 * @author GoldenKevin
 */
public class QuestItemStats {
	private final int itemId;
	private final short qty;
	private int prob;
	private byte gender;
	private short jobs;
	private long expireTime;
	private int period;

	protected QuestItemStats(int id, short count) {
		this.itemId = id;
		this.qty = count;
		this.jobs = PlayerJob.ALL_JOBS_BITSTRING;
		this.gender = 2;
	}

	protected int getItemId() {
		return itemId;
	}

	protected short getCount() {
		return qty;
	}

	protected void setProb(int prop) {
		this.prob = prop;
	}

	protected int getProb() {
		return prob;
	}

	protected void setGender(byte gender) {
		this.gender = gender;
	}

	protected boolean genderMatch(byte pGender) {
		return gender == 2 || gender == pGender;
	}

	protected void setJob(short job) {
		this.jobs = job;
	}

	protected boolean jobMatch(short pJob) {
		return PlayerJob.isJobInBitString(pJob, jobs);
	}

	protected void setDateExpire(int dateExpire) {
		expireTime = TimeTool.intDateToCalendar(dateExpire).getTimeInMillis();
	}

	protected boolean notExpired() {
		return expireTime == 0 || expireTime > System.currentTimeMillis();
	}

	protected void setPeriod(int period) {
		this.period = period;
	}

	protected int getPeriod() {
		return period;
	}
}
