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

package argonms.tools;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author GoldenKevin
 */
public class Timer {
	private static Timer instance;
	private ScheduledThreadPoolExecutor timer;

	private Timer() {
		timer = new ScheduledThreadPoolExecutor(4);
	}

	public ScheduledFuture<?> runAfterDelay(Runnable r, long delay) {
		return timer.schedule(r, delay, TimeUnit.MILLISECONDS);
	}

	public ScheduledFuture<?> runRepeatedly(Runnable r, long delay, long period) {
		return timer.scheduleAtFixedRate(r, delay, period, TimeUnit.MILLISECONDS);
	}

	public void shutdown() {
		timer.shutdown();
	}

	public static void enable() {
		instance = new Timer();
	}

	public static Timer getInstance() {
		return instance;
	}
}
