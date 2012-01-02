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

package argonms.common.util;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 *
 * @author GoldenKevin
 */
public class Scheduler {
	private static Scheduler instance;
	private static Scheduler hashedWheel;

	private ScheduledExecutorService timer;

	private Scheduler(ScheduledExecutorService impl) {
		timer = impl;
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

	public static void enable(boolean enableGeneral, boolean enableHashedWheel) {
		if (enableGeneral)
			instance = new Scheduler(Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors(), new ThreadFactory() {
				private final ThreadGroup group;
				private final AtomicInteger threadNumber = new AtomicInteger(1);

				{
					SecurityManager s = System.getSecurityManager();
					group = (s != null)? s.getThreadGroup() :
										 Thread.currentThread().getThreadGroup();
				}

				@Override
				public Thread newThread(Runnable r) {
					Thread t = new Thread(group, r, "scheduler-pool-thread-" + threadNumber.getAndIncrement(), 0);
					if (t.isDaemon())
						t.setDaemon(false);
					if (t.getPriority() != Thread.NORM_PRIORITY)
						t.setPriority(Thread.NORM_PRIORITY);
					return t;
				}
			}));
		if (enableHashedWheel)
			hashedWheel = new Scheduler(new ScheduledHashedWheelExecutor());
	}

	public static Scheduler getInstance() {
		return instance;
	}

	public static Scheduler getWheelTimer() {
		return hashedWheel;
	}
}
