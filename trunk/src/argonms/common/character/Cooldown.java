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

package argonms.common.character;

import argonms.common.util.Scheduler;
import java.util.concurrent.ScheduledFuture;

/**
 *
 * @author GoldenKevin
 */
public class Cooldown {
	private long endTime;
	private ScheduledFuture<?> expiration;

	public Cooldown(int remaining, Runnable expireTask) {
		this.endTime = System.currentTimeMillis() + remaining;
		this.expiration = Scheduler.getInstance().runAfterDelay(expireTask, remaining);
	}

	public int getMillisecondsRemaining() {
		return (int) (endTime - System.currentTimeMillis());
	}

	public short getSecondsRemaining() {
		return (short) ((endTime - System.currentTimeMillis()) / 1000);
	}

	public void cancel() {
		expiration.cancel(false);
	}
}
