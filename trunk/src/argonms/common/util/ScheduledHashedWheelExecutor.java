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

package argonms.common.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Delayed;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This implementation employs an efficient single-threaded algorithm based on
 * one described in
 * <a href="http://cseweb.ucsd.edu/users/varghese/PAPERS/twheel.ps.Z">Hashed
 * and Hierarchical Timing Wheels</a> by George Varghese and Tony Lauck. More
 * comprehensive slides are located
 * <a href="http://www.cse.wustl.edu/~cdgill/courses/cs6874/TimingWheels.ppt">
 * here</a>.
 *
 * Note that the actual start of execution time of a scheduled task is only
 * accurate to the value passed to the constructor's tickDuration argument.
 * Tasks will never execute before their scheduled time. They will usually
 * execute within the specified tickDuration after the scheduled time, unless
 * there was a computationally heavy task being executed before it that
 * temporarily delayed the start of the task's tick on the wheel.
 * @author GoldenKevin
 */
public class ScheduledHashedWheelExecutor implements ScheduledExecutorService {
	private static final Logger LOG = Logger.getLogger(ScheduledHashedWheelExecutor.class.getName());

	private final int millisPerTick, millisPerRev;
	private final Thread workerThread;
	private final Set<HashedWheelFuture<?>>[] buckets;
	private final AtomicInteger queuedTaskCount;
	private volatile boolean shuttingDown;
	private volatile boolean shutdownImmediately;
	private volatile boolean terminated;
	private final Lock readLock, writeLock;
	private int wheelCursor;

	private abstract class HashedWheelFuture<V> implements ScheduledFuture<V> {
		protected static final int
			STATE_CANCELED = -1,
			STATE_SCHEDULED = 0,
			STATE_RUNNING = 1,
			STATE_EXECUTED = 2
		;

		protected final AtomicInteger state;
		private final long startTime;
		private final long initDelay;
		private final long periodLength;
		private final boolean fixedRate;
		private int periodNo;
		private long remainingRevolutions;
		private int bucket;
		private long scheduledExecutionTime;

		public HashedWheelFuture(long submitTime, long delay, long period, boolean fixedRate) {
			this.state = new AtomicInteger(STATE_SCHEDULED);
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
			boolean result;
			if (state.compareAndSet(STATE_SCHEDULED, STATE_CANCELED)) {
				buckets[bucket].remove(this);
				queuedTaskCount.decrementAndGet();
				result = true;
			} else if (state.compareAndSet(STATE_RUNNING, STATE_CANCELED)) {
				queuedTaskCount.decrementAndGet();
				if (mayInterruptIfRunning) {
					workerThread.interrupt();
					result = true;
				} else {
					result = false;
				}
			} else {
				assert isDone();
				result = false;
			}
			synchronized(this) {
				notifyAll();
			}
			return result;
		}

		@Override
		public V get() throws InterruptedException, ExecutionException {
			int status = state.get();
			if (status == STATE_CANCELED)
				throw new CancellationException();
			if (status == STATE_EXECUTED)
				return getResult();
			waitForResult();

			status = state.get();
			if (status == STATE_CANCELED)
				throw new CancellationException();
			assert status == STATE_EXECUTED;
			return getResult();
		}

		@Override
		public V get(long arg0, TimeUnit arg1) throws InterruptedException,
				ExecutionException, TimeoutException {
			long submitTime = System.currentTimeMillis();

			int status = state.get();
			if (status == STATE_CANCELED)
				throw new CancellationException();
			if (status == STATE_EXECUTED)
				return getResult();
			boolean timedOut = waitForResult(submitTime + arg1.toMillis(arg0));

			if (timedOut)
				throw new TimeoutException();
			status = state.get();
			if (status == STATE_CANCELED)
				throw new CancellationException();
			assert status == STATE_EXECUTED;
			return getResult();
		}

		@Override
		public boolean isCancelled() {
			return state.get() == STATE_CANCELED;
		}

		@Override
		public boolean isDone() {
			int status = state.get();
			return status == STATE_EXECUTED || status == STATE_CANCELED;
		}

		public boolean hasCommencedExecution() {
			int status = state.get();
			return status == STATE_EXECUTED || status == STATE_CANCELED || status == STATE_RUNNING;
		}

		protected abstract boolean runExpireTask();

		protected abstract Runnable getRunnableTask();

		protected abstract V getResult() throws ExecutionException;

		private void waitForResult() throws InterruptedException {
			synchronized(this) {
				while (!isDone())
					wait();
			}
		}

		private boolean waitForResult(long deadline) throws InterruptedException {
			long now;
			boolean timedOut = false;
			synchronized(this) {
				while (!isDone() && !(timedOut = ((now = System.currentTimeMillis()) >= deadline)))
					wait(deadline - now);
			}
			return timedOut;
		}

		private void executed() {
			if (periodLength < 0 || isShutdown()) {
				if (state.compareAndSet(STATE_RUNNING, STATE_EXECUTED)) {
					queuedTaskCount.decrementAndGet();
					synchronized(this) {
						notifyAll();
					}
				} else {
					assert isCancelled();
				}
			} else if (!isCancelled()) {
				if (fixedRate) {
					scheduledExecutionTime = startTime + initDelay + periodLength * ++periodNo;
					schedule(this, scheduledExecutionTime - System.currentTimeMillis());
				} else {
					scheduledExecutionTime = System.currentTimeMillis() + periodLength;
					schedule(this, periodLength);
				}
			}
		}
	}

	private class VoidHashedWheelFuture extends HashedWheelFuture<Void> {
		private Runnable expireTask;
		private Throwable exc;

		public VoidHashedWheelFuture(Runnable task, boolean fixedRate, long submitTime, long delay, long period) {
			super(submitTime, delay, period, fixedRate);
			expireTask = task;
		}

		@Override
		protected boolean runExpireTask() {
			if (!state.compareAndSet(STATE_SCHEDULED, STATE_RUNNING)) {
				assert isCancelled();
				return false;
			}
			try {
				expireTask.run();
				exc = null;
			} catch (Throwable ex) {
				exc = ex;
			}
			return true;
		}

		@Override
		protected Runnable getRunnableTask() {
			return expireTask;
		}

		@Override
		protected Void getResult() throws ExecutionException {
			if (exc != null)
				throw new ExecutionException(exc);
			return null;
		}
	}

	private class HashedWheelFutureImpl<V> extends HashedWheelFuture<V> {
		private Callable<V> expireTask;
		private Throwable exc;
		private V result;

		public HashedWheelFutureImpl(Callable<V> task, long submitTime, long delay, long period) {
			super(submitTime, delay, period, true);
			expireTask = task;
		}

		@Override
		protected boolean runExpireTask() {
			if (!state.compareAndSet(STATE_SCHEDULED, STATE_RUNNING)) {
				assert isCancelled();
				return false;
			}
			try {
				result = expireTask.call();
				exc = null;
			} catch (Throwable ex) {
				exc = ex;
			}
			return true;
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
		private Throwable exc;
		private V result;

		public HashedWheelFutureKnownResult(Runnable task, V result, long submitTime, long delay, long period) {
			super(submitTime, delay, period, true);
			expireTask = task;
			this.result = result;
		}

		@Override
		protected boolean runExpireTask() {
			if (!state.compareAndSet(STATE_SCHEDULED, STATE_RUNNING)) {
				assert isCancelled();
				return false;
			}
			try {
				expireTask.run();
				exc = null;
			} catch (Throwable ex) {
				exc = ex;
			}
			return true;
		}

		@Override
		protected Runnable getRunnableTask() {
			return expireTask;
		}

		@Override
		protected V getResult() throws ExecutionException {
			if (exc != null)
				throw new ExecutionException(exc);
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
			if (shuttingDown && queuedTaskCount.get() == 0 || shutdownImmediately)
				return -1;
			if (tick == Integer.MAX_VALUE) {
				startTime += tick * millisPerTick;
				tick = 0;
			}
			tick++;
			long scheduledTime = startTime + tick * millisPerTick;
			long sleepTime;
			while ((sleepTime = scheduledTime - System.currentTimeMillis()) > 0) {
				try {
					Thread.sleep(sleepTime);
				} catch (InterruptedException e) {
					if (shutdownImmediately)
						return -1;
					//otherwise, it's probably from when Future.cancel(true) was
					//called. just clear the interrupted status.
				}
			}
			return scheduledTime;
		}

		@Override
		public void run() {
			startTime = System.currentTimeMillis();
			long tickScheduledExecute;
			while ((tickScheduledExecute = syncWithTick()) != -1) {
				Iterator<HashedWheelFuture<?>> iter;
				writeLock.lock();
				try {
					wheelCursor = (wheelCursor + 1) % buckets.length;
					iter = buckets[wheelCursor].iterator();
				} finally {
					writeLock.unlock();
				}
				while (iter.hasNext() && !shutdownImmediately) {
					HashedWheelFuture<?> future = iter.next();
					if (future.remainingRevolutions > 0) {
						future.remainingRevolutions--;
					} else {
						iter.remove();
						if (future.scheduledExecutionTime > tickScheduledExecute) {
							//NEVER execute a task if the tick's start time is
							//before the task's scheduled time even if the
							//current time is after it
							long delta = future.scheduledExecutionTime - tickScheduledExecute;
							if (LOG.isLoggable(Level.FINE))
								LOG.log(Level.FINE, "Executed task in wrong tick of HashedWheelExecutor. (Too soon by {0}ms, or {1} tick(s)) -> Rescheduling.", new Object[] { delta, (int) Math.ceil((double) delta / millisPerTick) });
							if (!future.isCancelled())
								schedule(future, delta);
						} else {
							if (LOG.isLoggable(Level.FINE) && tickScheduledExecute - future.scheduledExecutionTime > millisPerTick) {
								long delta = tickScheduledExecute - future.scheduledExecutionTime;
								LOG.log(Level.FINE, "Executed task in wrong tick of HashedWheelExecutor. (Too late by {0}ms, or {1} tick(s))", new Object[] { delta, (int) Math.floor((double) delta / millisPerTick) });
							}
							if (future.runExpireTask())
								future.executed();
						}
					}
				}
			}
			terminated = true;
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
	@SuppressWarnings("unchecked")
	public ScheduledHashedWheelExecutor(int buckets, long tickDuration, TimeUnit unit) {
		if (buckets <= 0)
			throw new IllegalArgumentException("buckets must be positive");
		if (tickDuration <= 0)
			throw new IllegalArgumentException("tickDuration must be positive");
		millisPerTick = (int) unit.toMillis(tickDuration);
		millisPerRev = millisPerTick * buckets;
		workerThread = new Thread(new Worker(), "hashed-wheel-timer-worker-thread");
		this.buckets = new Set[buckets];
		for (int i = 0; i < buckets; i++)
			this.buckets[i] = Collections.newSetFromMap(new ConcurrentHashMap<HashedWheelFuture<?>, Boolean>());
		queuedTaskCount = new AtomicInteger(0);

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
		long revolutions;
		int relativeIndex;
		if (delay > millisPerTick) {
			revolutions = delay / millisPerRev;
			delay %= millisPerRev;
			//don't do ceiling - just always add 1 to the floor, even if
			//remainder of (delay / millisPerTick) == 0
			//avoids a lot of frequent rescheduling, which could be a
			//performance killer
			relativeIndex = (int) (delay / millisPerTick) + 1;
		} else {
			revolutions = 0;
			relativeIndex = 1;
		}
		readLock.lock();
		try {
			int bucket = (wheelCursor + relativeIndex) % buckets.length;
			future.bucket = bucket;
			future.remainingRevolutions = revolutions;
			buckets[bucket].add(future);
		} finally {
			readLock.unlock();
		}
	}

	@Override
	public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
		long submitTime = System.currentTimeMillis();
		if (isShutdown())
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
		if (isShutdown())
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
		if (isShutdown())
			throw new RejectedExecutionException("shutdown");

		long millisDelay = initialDelay <= 0 ? initialDelay : Math.max(unit.toMillis(initialDelay), millisPerTick);
		HashedWheelFuture<Void> future = new VoidHashedWheelFuture(command, true, submitTime, millisDelay, period);
		schedule(future, millisDelay);
		queuedTaskCount.incrementAndGet();
		return future;
	}

	@Override
	public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) {
		long submitTime = System.currentTimeMillis();
		if (isShutdown())
			throw new RejectedExecutionException("shutdown");

		long millisDelay = initialDelay <= 0 ? initialDelay : Math.max(unit.toMillis(initialDelay), millisPerTick);
		HashedWheelFuture<Void> future = new VoidHashedWheelFuture(command, false, submitTime, millisDelay, delay);
		schedule(future, millisDelay);
		queuedTaskCount.incrementAndGet();
		return future;
	}

	@Override
	public void shutdown() {
		shuttingDown = true;
	}

	@Override
	public List<Runnable> shutdownNow() {
		shutdownImmediately = true;
		List<Runnable> notRun = new ArrayList<Runnable>(queuedTaskCount.get());
		Runnable r;
		writeLock.lock();
		try {
			for (int i = 0; i < buckets.length; i++) {
				for (Iterator<HashedWheelFuture<?>> iter = buckets[i].iterator(); iter.hasNext(); ) {
					HashedWheelFuture<?> f = iter.next();
					iter.remove();
					if (!f.hasCommencedExecution() && (r = f.getRunnableTask()) != null)
						notRun.add(r);
				}
			}
		} finally {
			writeLock.unlock();
		}
		workerThread.interrupt();
		return notRun;
	}

	@Override
	public boolean isShutdown() {
		return shuttingDown || shutdownImmediately;
	}

	@Override
	public boolean isTerminated() {
		return terminated;
	}

	@Override
	public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
		workerThread.join(unit.toMillis(timeout));
		return !workerThread.isAlive();
	}

	@Override
	public <T> Future<T> submit(Callable<T> task) {
		long submitTime = System.currentTimeMillis();
		if (isShutdown())
			throw new RejectedExecutionException("shutdown");
		HashedWheelFuture<T> future = new HashedWheelFutureImpl<T>(task, submitTime, 0, -1);
		schedule(future, 0);
		queuedTaskCount.incrementAndGet();
		return future;
	}

	@Override
	public <T> Future<T> submit(Runnable task, T result) {
		long submitTime = System.currentTimeMillis();
		if (isShutdown())
			throw new RejectedExecutionException("shutdown");
		HashedWheelFuture<T> future = new HashedWheelFutureKnownResult<T>(task, result, submitTime, 0, -1);
		schedule(future, 0);
		queuedTaskCount.incrementAndGet();
		return future;
	}

	@Override
	public Future<?> submit(Runnable task) {
		long submitTime = System.currentTimeMillis();
		if (isShutdown())
			throw new RejectedExecutionException("shutdown");
		HashedWheelFuture<Void> future = new VoidHashedWheelFuture(task, true, submitTime, 0, -1);
		schedule(future, 0);
		queuedTaskCount.incrementAndGet();
		return future;
	}

	@Override
	public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
		long submitTime = System.currentTimeMillis();
		if (isShutdown())
			throw new RejectedExecutionException("shutdown");
		List<HashedWheelFuture<T>> futures = new ArrayList<HashedWheelFuture<T>>(tasks.size());
		for (Callable<T> task : tasks) {
			HashedWheelFuture<T> future = new HashedWheelFutureImpl<T>(task, submitTime, 0, -1);
			schedule(future, 0);
			queuedTaskCount.incrementAndGet();
			futures.add(future);
		}
		boolean interrupted = false;
		for (HashedWheelFuture<T> future : futures) {
			if (future.isDone())
				continue;

			if (!interrupted) {
				try {
					future.waitForResult();
				} catch (InterruptedException e) {
					interrupted = true;
				}
			}
			if (interrupted)
				future.cancel(false);
		}
		return new ArrayList<Future<T>>(futures);
	}

	@Override
	public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException {
		long submitTime = System.currentTimeMillis();
		if (isShutdown())
			throw new RejectedExecutionException("shutdown");
		long deadline = submitTime + unit.toMillis(timeout);
		List<HashedWheelFuture<T>> futures = new ArrayList<HashedWheelFuture<T>>(tasks.size());
		for (Callable<T> task : tasks) {
			HashedWheelFuture<T> future = new HashedWheelFutureImpl<T>(task, submitTime, 0, -1);
			schedule(future, 0);
			queuedTaskCount.incrementAndGet();
			futures.add(future);
		}
		boolean interrupted = false, timedOut = false;
		for (HashedWheelFuture<T> future : futures) {
			if (future.isDone())
				continue;

			if (!interrupted && !timedOut) {
				try {
					timedOut = future.waitForResult(deadline);
				} catch (InterruptedException e) {
					interrupted = true;
				}
			}
			if (interrupted || timedOut)
				future.cancel(false);
		}
		return new ArrayList<Future<T>>(futures);
	}

	@Override
	public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
		long submitTime;
		if (isShutdown())
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
		boolean allDone = false, interrupted = false, gotResult = false;
		//spin lock until we get first result.
		//TODO: eliminate spin loop. register event handlers in
		//HashedWheelFuture.executed instead and just wait on some object until
		//the first task completes and notifies that object?
		while (!allDone) {
			//even if we got our result or got interrupted, we still need to
			//iterate and cancel the remaining tasks ("Upon normal or
			//exceptional return, tasks that have not completed are cancelled").
			//hence allDone, which is true only when all tasks are done (i.e.
			//executed or canceled).
			allDone = true;
			for (HashedWheelFutureImpl<T> future : futures) {
				if (future.isDone()) {
					if (!gotResult && future.exc == null) {
						result = future.get();
						gotResult = true;
					}
				} else {
					if (interrupted || gotResult)
						future.cancel(false);
					else
						allDone = false;
				}
			}
			//since we're using a spin loop and never call sleep, we do not get
			//InterruptedExceptions, so it's imperative we regularly check if
			//the thread's interrupted flag is set.
			interrupted = Thread.interrupted();
		}
		if (interrupted)
			throw new InterruptedException();
		if (!gotResult)
			throw new ExecutionException("No tasks completed successfully", null);
		return result;
	}

	@Override
	public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
		long submitTime = System.currentTimeMillis();
		if (isShutdown())
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
		boolean allDone = false, interrupted = false, gotResult = false, timedOut = false;
		//spin lock until we get first result or timed out.
		//TODO: eliminate spin loop. register event handlers in
		//HashedWheelFuture.executed instead and just wait on some object (with
		//a timeout) until the first task completes and notifies that object?
		while (!allDone) {
			//even if we got our result, got interrupted, or we timed out, we
			//still need to iterate and cancel the remaining tasks ("Upon normal
			//or exceptional return, tasks that have not completed are
			//cancelled"). hence allDone, which is true only when all tasks are
			//done (i.e. executed or canceled).
			allDone = true;
			for (HashedWheelFutureImpl<T> future : futures) {
				if (future.isDone()) {
					if (!gotResult && future.exc == null) {
						result = future.get();
						gotResult = true;
					}
				} else {
					timedOut = timedOut || System.currentTimeMillis() >= timeoutTime;
					if (interrupted || gotResult || timedOut)
						future.cancel(false);
					else
						allDone = false;
				}
			}
			//since we're using a spin loop and never call sleep, we do not get
			//InterruptedExceptions, so it's imperative we regularly check if
			//the thread's interrupted flag is set.
			interrupted = Thread.interrupted();
		}
		if (timedOut)
			throw new TimeoutException();
		if (interrupted)
			throw new InterruptedException();
		if (!gotResult)
			throw new ExecutionException("No tasks completed successfully", null);
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
