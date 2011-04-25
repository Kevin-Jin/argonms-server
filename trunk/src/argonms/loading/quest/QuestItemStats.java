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

import argonms.tools.TimeUtil;

/**
 *
 * @author GoldenKevin
 */
public class QuestItemStats {
	private int itemId;
	private short qty;
	private int prob;
	private byte gender;
	private short job;
	private long expireTime;
	private int period;

	protected QuestItemStats(int id, short count) {
		this.itemId = id;
		this.qty = count;
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

	protected byte getGender() {
		return gender;
	}

	protected void setJob(short job) {
		this.job = job;
	}

	protected short getJob() {
		return job;
	}

	protected void setDateExpire(int dateExpire) {
		expireTime = TimeUtil.intDateToCalendar(dateExpire).getTimeInMillis();
	}

	protected long getDateExpire() {
		return expireTime;
	}

	protected void setPeriod(int period) {
		this.period = period;
	}

	protected int getPeriod() {
		return period;
	}
}
