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
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * All methods of this class are thread safe.
 * @author GoldenKevin
 */
public class UnorderedQueue {
	private final Queue<ByteBuffer> queued;
	private final AtomicBoolean writeInProgress;

	public UnorderedQueue() {
		queued = new ConcurrentLinkedQueue<ByteBuffer>();
		writeInProgress = new AtomicBoolean(false);
	}

	/**
	 *
	 * @param orderNo a unique value received from getNextPush()
	 * @param element the ByteBuffer to queue
	 */
	public void insert(ByteBuffer element) {
		queued.offer(element);
	}

	public boolean shouldWrite() {
		return writeInProgress.compareAndSet(false, true);
	}

	public void setCanWrite() {
		writeInProgress.set(false);
	}

	public boolean willBlock() {
		return queued.isEmpty();
	}

	/**
	 *
	 * @return a list of all ByteBuffers queued as of this moment.
	 */
	public List<ByteBuffer> pop() {
		List<ByteBuffer> consecutive = new ArrayList<ByteBuffer>();
		ByteBuffer last;
		while ((last = queued.poll()) != null)
			consecutive.add(last);
		return consecutive;
	}
}
