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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * This implementation employs an efficient single-threaded algorithm based on
 * one described in
 * <a href="http://cseweb.ucsd.edu/users/varghese/PAPERS/twheel.ps.Z">Hashed
 * and Hierarchical Timing Wheels</a> by George Varghese and Tony Lauck.
 
 * Note that the actual start of execution time of a scheduled task is only
 * accurate to the value passed to the constructor's tickDuration argument.
 * Tasks will never execute before their scheduled time, but can execute after
 * it, almost always less than the specified tickDuration, unless there is a
 * computationally heavy task being executed before it which will temporarily
 * delay the start of the wheel's next tick.
 * @author GoldenKevin
 */
public class ScheduledHashedWheelExecutor implements ScheduledExecutorService {
	//pretty much just a type-safe way of having an array of generic classes.
	//could just settle with @SuppressWarnings("unchecked") and ConcurrentLinkedQueue[]
	@SuppressWarnings("serial")
	private static class Bucket extends ConcurrentLinkedQueue<HashedWheelFuture<?>> { }

	private final int millisPerTick, millisPerRev;
	private final Thread workerThread;
	private final Bucket[] buckets;
	private final AtomicInteger queuedTaskCount;
	private final AtomicBoolean shutdown;
	private final AtomicBoolean shutdownImmediately;
	private final AtomicBoolean terminated;
	private final Lock readLock, writeLock;
	private int wheelCursor;

	private abstract class HashedWheelFuture<V> implements ScheduledFuture<V> {
		private final long startTime;
		private final long initDelay;
		private final long periodLength;
		private final boolean fixedRate;
		private int periodNo;
		private long remainingRevolutions;
		private int bucket;
		private long scheduledExecutionTime;
		private volatile boolean done, canceled;

		public HashedWheelFuture(long submitTime, long delay, long period, boolean fixedRate) {
			this.startTime = submitTime;
			this.initDelay = delay;
			this.periodLength = period;
			this.fixedRate = fixedRate;
			this.periodNo = 0;
			this.scheduledExecutionTime = startTime + initDelay;
		}

		@Override
		public long getDelay(TimeUnit unit) {
			return unit.convert(initDelay, TimeUnit.MILLISECONDS) - System.currentTimeMillis();
		}

		@Override
		public int compareTo(Delayed arg0) {
			return (int) (initDelay - arg0.getDelay(TimeUnit.MILLISECONDS));
		}

		@Override
		public boolean cancel(boolean mayInterruptIfRunning) {
			boolean success = !isDone() ? buckets[bucket].remove(this) : false;
			canceled = true;
			if (!done)
				queuedTaskCount.decrementAndGet();
			synchronized (this) {
				notifyAll();
			}
			return success;
		}

		@Override
		public V get() throws InterruptedException, ExecutionException {
			if (canceled)
				throw new CancellationException();
			if (done)
				return getResult();
			waitForResult();
			if (canceled)
				throw new CancellationException();
			//assert done;
			return getResult();
		}

		@Override
		public V get(long arg0, TimeUnit arg1) throws InterruptedException,
				ExecutionException, TimeoutException {
			long submitTime = System.currentTimeMillis();
			if (canceled)
				throw new CancellationException();
			if (done)
				return getResult();
			boolean timedOut = waitForResult(submitTime + arg1.toMillis(arg0));
			if (canceled)
				throw new CancellationException();
			if (timedOut)
				throw new TimeoutException();
			//assert done;
			return getResult();
		}

		@Override
		public boolean isCancelled() {
			return canceled;
		}

		@Override
		public boolean isDone() {
			return done || canceled;
		}

		protected abstract void runExpireTask();

		protected abstract Runnable getRunnableTask();

		protected abstract V getResult() throws ExecutionException;

		private void waitForResult() throws InterruptedException {
			synchronized (this) {
				while (!done && !canceled)
					wait();
			}
		}

		private boolean waitForResult(long deadline) throws InterruptedException {
			boolean timedOut = false;
			synchronized (this) {
				while (!done && !canceled && !(timedOut = (System.currentTimeMillis() >= deadline)))
					wait();
			}
			return timedOut;
		}

		private void executed() {
			done = true;
			if (!canceled) {
				if (periodLength == 0) {
					queuedTaskCount.decrementAndGet();
				} else if (fixedRate) {
					scheduledExecutionTime = startTime + initDelay + periodLength * ++periodNo;
					long delay = scheduledExecutionTime - System.currentTimeMillis();
					delay = delay <= 0 ? delay : Math.max(delay, millisPerTick);
					schedule(this, delay);
				} else {
					periodNo++;
					long delay = periodLength;
					scheduledExecutionTime = System.currentTimeMillis() + delay;
					delay = delay <= 0 ? delay : Math.max(delay, millisPerTick);
					schedule(this, delay);
				}
			}
			synchronized (this) {
				notifyAll();
			}
		}
	}

	private class VoidHashedWheelFuture extends HashedWheelFuture<Void> {
		private Runnable expireTask;

		public VoidHashedWheelFuture(Runnable task, boolean fixedRate, long submitTime, long delay, long period) {
			super(submitTime, delay, period, fixedRate);
			expireTask = task;
		}

		@Override
		protected void runExpireTask() {
			expireTask.run();
		}

		@Override
		protected Runnable getRunnableTask() {
			return expireTask;
		}

		@Override
		protected Void getResult() throws ExecutionException {
			return null;
		}
	}

	private class HashedWheelFutureImpl<V> extends HashedWheelFuture<V> {
		private Callable<V> expireTask;
		private Exception exc;
		private V result;

		public HashedWheelFutureImpl(Callable<V> task, long submitTime, long delay, long period) {
			super(submitTime, delay, period, true);
			expireTask = task;
		}

		@Override
		protected void runExpireTask() {
			try {
				result = expireTask.call();
				this.exc = null;
			} catch (Exception ex) {
				this.exc = ex;
			}
		}

		@Override
		protected Runnable getRunnableTask() {
			return null;
		}

		@Override
		protected V getResult() throws ExecutionException {
			if (exc != null)
				throw new ExecutionException(exc);
			return result;
		}
	}

	private class HashedWheelFutureKnownResult<V> extends HashedWheelFuture<V> {
		private Runnable expireTask;
		private V result;

		public HashedWheelFutureKnownResult(Runnable task, V result, long submitTime, long delay, long period) {
			super(submitTime, delay, period, true);
			expireTask = task;
			this.result = result;
		}

		@Override
		protected void runExpireTask() {
			expireTask.run();
		}

		@Override
		protected Runnable getRunnableTask() {
			return expireTask;
		}

		@Override
		protected V getResult() throws ExecutionException {
			return result;
		}
	}

	private class Worker implements Runnable {
		private long startTime;
		private int tick;

		/**
		 * 
		 * @return the scheduled start time of execution of this tick, or -1
		 * if 
		 */
		private long syncWithTick() {
			long scheduledTime = startTime + ++tick * millisPerTick;
			long sleepTime;
			while ((sleepTime = scheduledTime - System.currentTimeMillis()) > 0) {
				try {
					Thread.sleep(sleepTime);
				} catch (InterruptedException e) {
					if (shutdownImmediately.get())
						return -1;
				}
			}
			return scheduledTime;
		}

		@Override
		public void run() {
			startTime = System.currentTimeMillis();
			long tickScheduledExecute;
			while (!shutdownImmediately.get() && !shutdown.get() && !Thread.interrupted() && (tickScheduledExecute = syncWithTick()) != -1) {
				Iterator<HashedWheelFuture<?>> iter;
				writeLock.lock();
				try {
					wheelCursor = (wheelCursor + 1) % buckets.length;
					iter = buckets[wheelCursor].iterator();
				} finally {
					writeLock.unlock();
				}
				while (iter.hasNext()) {
					HashedWheelFuture<?> future = iter.next();
					if (future.remainingRevolutions > 0) {
						future.remainingRevolutions--;
					} else {
						iter.remove();
						if (future.scheduledExecutionTime <= tickScheduledExecute) {
							future.runExpireTask();
							future.executed();
						} else {
							//NEVER execute a task if the tick's start time is
							//before the task's scheduled time even if the
							//current time is after it
							schedule(future, tickScheduledExecute - future.scheduledExecutionTime);
						}
					}
				}
			}
			terminated.set(true);
		}
	}

	/**
	 * 
	 * @param buckets the amount of buckets that constitute a full rotation.
	 * The lower the number, the higher the amount of tasks that will be queued
	 * in each bucket, which usually results in higher overhead per tick, as
	 * each task in a bucket has to be checked. The higher the number, the more
	 * memory will be used. Generally, higher loads require more buckets.
	 * @param tickDuration the rate at which bucket executions are performed.
	 * Directly proportional with accuracy and inversely proportion with CPU
	 * efficiency.
	 * @param unit the time unit that tickDuration is in.
	 */
	public ScheduledHashedWheelExecutor(int buckets, long tickDuration, TimeUnit unit) {
		millisPerTick = (int) unit.toMillis(tickDuration);
		millisPerRev = millisPerTick * buckets;
		workerThread = new Thread(new Worker());
		this.buckets = new Bucket[buckets];
		for (int i = 0; i < buckets; i++)
			this.buckets[i] = new Bucket();
		queuedTaskCount = new AtomicInteger(0);
		shutdown = new AtomicBoolean(false);
		shutdownImmediately = new AtomicBoolean(false);
		terminated = new AtomicBoolean(false);

		ReadWriteLock rwLock = new ReentrantReadWriteLock();
		readLock = rwLock.readLock();
		writeLock = rwLock.writeLock();

		workerThread.start();
	}

	/**
	 * Allocate a ScheduledHashWheelExecutor with the default values; that is,
	 * 512 buckets and 100 milliseconds per tick.
	 */
	public ScheduledHashedWheelExecutor() {
		this(512, 100, TimeUnit.MILLISECONDS);
	}

	private void schedule(HashedWheelFuture<?> future, long delay) {
		if (delay > 0) {
			long lastRoundDelay = delay % millisPerRev;
			long lastTickDelay = delay % millisPerTick;
			long relativeIndex = lastRoundDelay / millisPerTick + Math.min(lastTickDelay, 1);
			long revolutions = delay / millisPerRev - (delay % millisPerRev == 0 ? 1 : 0);

			readLock.lock();
			try {
				int bucket = ((wheelCursor + (int) relativeIndex) % buckets.length);
				future.bucket = bucket;
				future.remainingRevolutions = revolutions;
				buckets[bucket].add(future);
			} finally {
				readLock.unlock();
			}
		} else {
			readLock.lock();
			try {
				int bucket = (wheelCursor + 1) % buckets.length;
				future.bucket = bucket;
				future.remainingRevolutions = 0;
				buckets[bucket].add(future);
			} finally {
				readLock.unlock();
			}
		}
	}

	@Override
	public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
		long submitTime = System.currentTimeMillis();
		if (shutdown.get())
			throw new RejectedExecutionException("shutdown");

		long millisDelay = delay <= 0 ? delay : Math.max(unit.toMillis(delay), millisPerTick);
		HashedWheelFuture<Void> future = new VoidHashedWheelFuture(command, true, submitTime, millisDelay, -1);
		schedule(future, millisDelay);
		queuedTaskCount.incrementAndGet();
		return future;
	}

	@Override
	public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
		long submitTime = System.currentTimeMillis();
		if (shutdown.get())
			throw new RejectedExecutionException("shutdown");

		long millisDelay = delay <= 0 ? delay : Math.max(unit.toMillis(delay), millisPerTick);
		HashedWheelFuture<V> future = new HashedWheelFutureImpl<V>(callable, submitTime, millisDelay, -1);
		schedule(future, millisDelay);
		queuedTaskCount.incrementAndGet();
		return future;
	}

	@Override
	public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
		long submitTime = System.currentTimeMillis();
		if (shutdown.get())
			throw new RejectedExecutionException("shutdown");

		long millisDelay = initialDelay <= 0 ? initialDelay : Math.max(unit.toMillis(initialDelay), millisPerTick);
		HashedWheelFuture<Void> future = new VoidHashedWheelFuture(command, true, submitTime, millisDelay, -1);
		schedule(future, millisDelay);
		queuedTaskCount.incrementAndGet();
		return future;
	}

	@Override
	public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) {
		long submitTime = System.currentTimeMillis();
		if (shutdown.get())
			throw new RejectedExecutionException("shutdown");

		long millisDelay = initialDelay <= 0 ? initialDelay : Math.max(unit.toMillis(initialDelay), millisPerTick);
		HashedWheelFuture<Void> future = new VoidHashedWheelFuture(command, false, submitTime, millisDelay, -1);
		schedule(future, millisDelay);
		queuedTaskCount.incrementAndGet();
		return future;
	}

	@Override
	public void shutdown() {
		shutdown.set(true);
	}

	@Override
	public List<Runnable> shutdownNow() {
		shutdownImmediately.set(true);
		workerThread.interrupt();
		List<Runnable> notRun = new ArrayList<Runnable>(queuedTaskCount.get());
		writeLock.lock();
		try {
			Runnable r;
			for (int i = 0; i < buckets.length; i++) {
				for (HashedWheelFuture<?> f : buckets[i])
					if ((r = f.getRunnableTask()) != null)
						notRun.add(r);
				buckets[i] = null;
			}
		} finally {
			writeLock.unlock();
		}
		return notRun;
	}

	@Override
	public boolean isShutdown() {
		return shutdownImmediately.get();
	}

	@Override
	public boolean isTerminated() {
		return terminated.get();
	}

	@Override
	public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
		workerThread.join(unit.toMillis(timeout));
		return !workerThread.isAlive();
	}

	@Override
	public <T> Future<T> submit(Callable<T> task) {
		long submitTime = System.currentTimeMillis();
		if (shutdown.get())
			throw new RejectedExecutionException("shutdown");
		HashedWheelFuture<T> future = new HashedWheelFutureImpl<T>(task, submitTime, 0, -1);
		schedule(future, 0);
		queuedTaskCount.incrementAndGet();
		return future;
	}

	@Override
	public <T> Future<T> submit(Runnable task, T result) {
		long submitTime = System.currentTimeMillis();
		if (shutdown.get())
			throw new RejectedExecutionException("shutdown");
		HashedWheelFuture<T> future = new HashedWheelFutureKnownResult<T>(task, result, submitTime, 0, -1);
		schedule(future, 0);
		queuedTaskCount.incrementAndGet();
		return future;
	}

	@Override
	public Future<?> submit(Runnable task) {
		long submitTime = System.currentTimeMillis();
		if (shutdown.get())
			throw new RejectedExecutionException("shutdown");
		HashedWheelFuture<Void> future = new VoidHashedWheelFuture(task, true, submitTime, 0, -1);
		schedule(future, 0);
		queuedTaskCount.incrementAndGet();
		return future;
	}

	@Override
	public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
		long submitTime;
		if (shutdown.get())
			throw new RejectedExecutionException("shutdown");
		List<HashedWheelFuture<T>> futures = new ArrayList<HashedWheelFuture<T>>(tasks.size());
		for (Callable<T> task : tasks) {
			submitTime = System.currentTimeMillis();
			HashedWheelFuture<T> future = new HashedWheelFutureImpl<T>(task, submitTime, 0, -1);
			schedule(future, 0);
			queuedTaskCount.incrementAndGet();
			futures.add(future);
		}
		for (HashedWheelFuture<T> future : futures)
			if (!future.isDone())
				future.waitForResult();
		return new ArrayList<Future<T>>(futures);
	}

	@Override
	public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException {
		long submitTime = System.currentTimeMillis();
		if (shutdown.get())
			throw new RejectedExecutionException("shutdown");
		long timeoutTime = submitTime + unit.toMillis(timeout);
		List<HashedWheelFuture<T>> futures = new ArrayList<HashedWheelFuture<T>>(tasks.size());
		for (Callable<T> task : tasks) {
			submitTime = System.currentTimeMillis();
			HashedWheelFuture<T> future = new HashedWheelFutureImpl<T>(task, submitTime, 0, -1);
			schedule(future, 0);
			queuedTaskCount.incrementAndGet();
			future.waitForResult();
			futures.add(future);
		}
		boolean timedOut = System.currentTimeMillis() >= timeoutTime;
		for (HashedWheelFuture<T> future : futures)
			if (!future.isDone() && (timedOut || (timedOut = System.currentTimeMillis() >= timeoutTime) || (timedOut = future.waitForResult(timeoutTime))))
				future.cancel(false);
		return new ArrayList<Future<T>>(futures);
	}

	@Override
	public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
		long submitTime;
		if (shutdown.get())
			throw new RejectedExecutionException("shutdown");
		if (tasks.isEmpty())
			throw new IllegalArgumentException("tasks is empty");
		List<HashedWheelFutureImpl<T>> futures = new ArrayList<HashedWheelFutureImpl<T>>(tasks.size());
		for (Callable<T> task : tasks) {
			submitTime = System.currentTimeMillis();
			HashedWheelFutureImpl<T> future = new HashedWheelFutureImpl<T>(task, submitTime, 0, -1);
			schedule(future, 0);
			queuedTaskCount.incrementAndGet();
			futures.add(future);
		}
		T result = null;
		boolean notAllDone = true;
		while (result == null && notAllDone) {
			//TODO: inefficient. register event handlers in HashedWheelFuture.executed instead?
			notAllDone = false;
			for (HashedWheelFutureImpl<T> future : futures) {
				if (future.isDone()) {
					if (result == null && future.exc == null)
						result = future.get();
				} else {
					notAllDone = true;
					if (result != null)
						future.cancel(false);
				}
			}
		}
		if (result == null) //which exception do we throw with ExecutionException?
			throw new ExecutionException("No tasks completed successfully", futures.get((int) (Math.random() * futures.size())).exc);
		return result;
	}

	@Override
	public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
		long submitTime = System.currentTimeMillis();
		if (shutdown.get())
			throw new RejectedExecutionException("shutdown");
		long timeoutTime = submitTime + unit.toMillis(timeout);
		if (tasks.isEmpty())
			throw new IllegalArgumentException("tasks is empty");
		List<HashedWheelFutureImpl<T>> futures = new ArrayList<HashedWheelFutureImpl<T>>(tasks.size());
		for (Callable<T> task : tasks) {
			submitTime = System.currentTimeMillis();
			HashedWheelFutureImpl<T> future = new HashedWheelFutureImpl<T>(task, submitTime, 0, -1);
			schedule(future, 0);
			queuedTaskCount.incrementAndGet();
			futures.add(future);
		}
		T result = null;
		boolean notAllDone = true;
		boolean timedOut = System.currentTimeMillis() >= timeoutTime;
		while (result == null && notAllDone && !timedOut) {
			//TODO: inefficient. register event handlers in HashedWheelFuture.executed instead?
			notAllDone = false;
			for (HashedWheelFutureImpl<T> future : futures) {
				if (future.isDone()) {
					if (result == null && future.exc == null)
						result = future.get();
				} else {
					notAllDone = true;
					timedOut = timedOut || System.currentTimeMillis() >= timeoutTime;
					if (result != null || timedOut)
						future.cancel(false);
				}
			}
		}
		if (result == null)
			if (timedOut)
				throw new TimeoutException();
			else
				throw new ExecutionException("No tasks completed successfully", futures.get((int) (Math.random() * futures.size())).exc);
		return result;
	}

	/**
	 * Execute command in the current thread. 
	 * @param command
	 */
	@Override
	public void execute(Runnable command) {
		//where's the fun in doing the same thing as submit? The javadocs do say
		//"The command may execute [...] in the calling thread, at the discretion of the Executor implementation."
		//so we are still fulfilling the contract.
		command.run();
	}
}
