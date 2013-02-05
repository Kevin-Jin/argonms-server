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

package argonms.common.net;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * All methods of this class are thread safe.
 * @author GoldenKevin
 */
public class OrderedQueue {
	private final SortedMap<Integer, ByteBuffer> queued;
	private final AtomicInteger nextPopCursor, nextPushCursor;
	private final AtomicBoolean writeInProgress;

	public OrderedQueue() {
		queued = new ConcurrentSkipListMap<Integer, ByteBuffer>();
		nextPopCursor = new AtomicInteger(0);
		nextPushCursor = new AtomicInteger(0);
		writeInProgress = new AtomicBoolean(false);
	}

	/**
	 *
	 * @return a unique value that relates to the order of this call to this
	 * method compared to other calls to this same method.
	 */
	public int getNextPush() {
		return nextPushCursor.getAndIncrement();
	}

	/**
	 *
	 * @param orderNo a unique value received from getNextPush()
	 * @param element the ByteBuffer to queue
	 */
	public void insert(int orderNo, ByteBuffer element) {
		queued.put(Integer.valueOf(orderNo), element);
	}

	public int currentPopBlock() {
		return nextPopCursor.get();
	}

	public void incrementPopCursor(int amount) {
		nextPopCursor.addAndGet(amount);
	}

	public boolean shouldWrite() {
		return writeInProgress.compareAndSet(false, true);
	}

	public void setCanWrite() {
		writeInProgress.set(false);
	}

	public boolean willBlock() {
		return !queued.containsKey(Integer.valueOf(nextPopCursor.get()));
	}

	/**
	 *
	 * @return a list of ByteBuffers in order of the order number passed to
	 * insert(). The list has no gaps in order numbers.
	 */
	public List<ByteBuffer> pop() {
		List<ByteBuffer> consecutive = new ArrayList<ByteBuffer>();
		Iterator<Entry<Integer, ByteBuffer>> elements = queued.entrySet().iterator();
		if (elements.hasNext()) {
			Entry<Integer, ByteBuffer> lastPopped = elements.next();
			int smallestKey = lastPopped.getKey().intValue();
			//caching nextPopCursor is safe since we mutexed the mutating of it
			//with the AtomicBoolean writeInProgress.
			int firstToPop = nextPopCursor.get();
			if (smallestKey == firstToPop) {
				for (int i = firstToPop; i == lastPopped.getKey().intValue(); i++) {
					consecutive.add(lastPopped.getValue());
					elements.remove();
					if (elements.hasNext())
						lastPopped = elements.next();
				}
			} else if (smallestKey < firstToPop) {
				//nextPushCursor overflowed while we still have keys from before
				//overflow (nextPopCursor still hasn't overflowed), so the
				//newer, negative, overflowed keys now come before older ones.
				//do a less efficient iteration that preserves ordering of all
				//non-overflowed and overflowed keys until all non-overflowed
				//keys have been processed and removed.
				ByteBuffer buf;
				for (int i = firstToPop; (buf = queued.get(Integer.valueOf(i))) != null; i++) {
					consecutive.add(buf);
					queued.remove(Integer.valueOf(i));
				}
			}
		}
		return consecutive;
	}
}
