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

package argonms.common.net.external;

import argonms.common.GlobalConstants;
import argonms.common.net.OrderedQueue;
import argonms.common.net.Session;
import argonms.common.util.Rng;
import argonms.common.util.Scheduler;
import argonms.common.util.output.LittleEndianByteArrayWriter;
import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Random;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author GoldenKevin
 */
public class ClientSession<T extends RemoteClient> implements Session {
	private static final Logger LOG = Logger.getLogger(ClientSession.class.getName());
	private static final int INIT_HEADER_LENGTH = 2;
	private static final int HEADER_LENGTH = 4;
	//1kb as the initial buffer size for each client isn't too unreasonable...
	private static final int DEFAULT_BUFFER_SIZE = 1024;
	private static final byte[][] EMPTY_ARRAY = new byte[0][];
	private static final int IDLE_TIME = 15000; //in milliseconds
	private static final int TIMEOUT = 15000; //in milliseconds

	private final SocketChannel commChn;
	private final AtomicBoolean closeEventsTriggered;
	private ByteBuffer readBuffer;
	private final CloseListener<T> onClose;
	private T client;
	private final AtomicInteger queuedReads;
	private volatile Runnable emptyReadQueueHandler;

	private final SelectionKey selectionKey;
	private final OrderedQueue sendQueue;

	private KeepAliveTask heartbeatTask;
	private final Runnable idleTask = new Runnable() {
		@Override
		public void run() {
			startPingTask();
		}
	};
	private ScheduledFuture<?> idleTaskFuture;

	private MessageType nextMessageType;

	private final Lock sendIvLock;
	private byte[] recvIv, sendIv;

	/* package-private */ interface CloseListener<T extends RemoteClient> {
		public void closed(ClientSession<T> session);
	}

	/* package-private */ ClientSession(SocketChannel channel, SelectionKey key, T client, CloseListener<T> onClose) {
		closeEventsTriggered = new AtomicBoolean(false);
		sendQueue = new OrderedQueue();
		heartbeatTask = new KeepAliveTask();
		queuedReads = new AtomicInteger(0);

		//we don't need to lock for receiving - see readMessage()
		sendIvLock = new ReentrantLock();
		Random generator = Rng.getGenerator();
		recvIv = new byte[4];
		sendIv = new byte[4];
		generator.nextBytes(recvIv);
		generator.nextBytes(sendIv);

		this.commChn = channel;
		this.onClose = onClose;
		this.selectionKey = key;
		this.client = client;
	}

	public T getClient() {
		return client;
	}

	public void removeClient() {
		this.client = null;
	}

	@Override
	public SocketAddress getAddress() {
		return commChn.socket().getRemoteSocketAddress();
	}

	public String getAccountName() {
		return getClient() != null ? getClient().getAccountName() : null;
	}

	private void send(int queueInsertNo, ByteBuffer buf) {
		sendQueue.insert(queueInsertNo, buf);
		synchronized (commChn) {
			if (selectionKey.isValid() && !sendQueue.willBlock() && tryFlushSendQueue() == 0) {
				selectionKey.interestOps(selectionKey.interestOps() | SelectionKey.OP_WRITE);
				selectionKey.selector().wakeup();
			}
		}
	}

	@Override
	public void send(byte[] message) {
		//we will have to synchronize here because send can be called from any
		//thread. we have to ensure that this message is sent before any shorter
		//messages that use a newer IV are sent, so use an OrderedQueue.
		byte[] iv;
		int queueInsertNo;
		sendIvLock.lock();
		try {
			iv = sendIv;
			sendIv = MapleAesOfb.nextIv(iv);
			queueInsertNo = sendQueue.getNextPush();
		} finally {
			sendIvLock.unlock();
		}
		byte[] input = new byte[message.length];
		System.arraycopy(message, 0, input, 0, message.length);
		byte[] header = MapleAesOfb.makePacketHeader(input.length, iv);
		byte[] output = new byte[header.length + input.length];
		MapleAesOfb.mapleEncrypt(input);
		MapleAesOfb.aesCrypt(input, iv);
		System.arraycopy(header, 0, output, 0, header.length);
		System.arraycopy(input, 0, output, header.length, input.length);
		send(queueInsertNo, ByteBuffer.wrap(output));
	}

	public void readEnqueued() {
		queuedReads.incrementAndGet();
	}

	public void readDequeued() {
		if (queuedReads.decrementAndGet() == 0 && emptyReadQueueHandler != null)
			emptyReadQueueHandler.run();
	}

	public int getQueuedReads() {
		return queuedReads.get();
	}

	public void setEmptyReadQueueHandler(Runnable runnable) {
		emptyReadQueueHandler = runnable;
	}

	public void receivedPong() {
		heartbeatTask.receivedPong();
	}

	private void startPingTask() {
		heartbeatTask.waitForPong();
		heartbeatTask.sendPing();
	}

	private void stopPingTask() {
		heartbeatTask.stop();
	}

	@Override
	public boolean close(String reason, Throwable reasonExc) {
		if (closeEventsTriggered.compareAndSet(false, true)) {
			synchronized (commChn) {
				try {
					commChn.close();
				} catch (IOException ex) {
					LOG.log(Level.WARNING, "Error while closing client " + getAccountName() + " (" + getAddress() + ")", ex);
				}
			}
			stopPingTask();
			idleTaskFuture.cancel(false);

			if (reasonExc == null)
				LOG.log(Level.FINE, "Client {0} ({1}) disconnected: {2}", new Object[] { getAccountName(), getAddress(), reason });
			else
				LOG.log(Level.FINE, "Client " + getAccountName() + " (" + getAddress() + ") disconnected: " + reason, reasonExc);
			client.disconnected();
			onClose.closed(this);
			return true;
		}
		return false;
	}

	/**
	 * @return 0 if not all queued messages could be sent in a non-blocking
	 * manner, 1 if all queued messages have been successfully sent, -1 if there
	 * is another flush attempt in progress, or -2 if there's an error and the
	 * channel is closed.
	 */
	/* package-private */ byte tryFlushSendQueue() {
		if (!sendQueue.shouldWrite())
			return -1;
		try {
			do {
				int success = 0;
				try {
					Iterator<ByteBuffer> iter = sendQueue.pop().iterator();
					while (iter.hasNext()) {
						ByteBuffer buf = iter.next();
						if (buf.remaining() == commChn.write(buf)) {
							success++;
						} else {
							int i = sendQueue.currentPopBlock() + success;
							sendQueue.insert(i++, buf);
							while (iter.hasNext())
								sendQueue.insert(i++, iter.next());
							return 0;
						}
					}
				} finally {
					sendQueue.incrementPopCursor(success);
				}
			} while (!sendQueue.willBlock());
			return 1;
		} catch (IOException ex) {
			//does an IOException in write always mean an invalid channel?
			close("Error while writing", ex);
			return -2;
		} finally {
			sendQueue.setCanWrite();
		}
	}

	/**
	 * This may only be called from the ClientListener boss thread, in the
	 * Selector loop.
	 */
	/* package-private */ void sendInitPacket() {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(13);
		lew.writeShort(GlobalConstants.MAPLE_VERSION);
		lew.writeShort((short) 0);
		lew.writeBytes(recvIv);
		lew.writeBytes(sendIv);
		lew.writeByte((byte) 8);
		byte[] body = lew.getBytes();

		ByteBuffer buf = ByteBuffer.allocate(INIT_HEADER_LENGTH + body.length);
		buf.order(ByteOrder.LITTLE_ENDIAN);
		buf.putShort((short) body.length);
		buf.put(body);
		buf.flip();
		send(sendQueue.getNextPush(), buf);

		readBuffer = ByteBuffer.allocate(DEFAULT_BUFFER_SIZE);
		readBuffer.limit(HEADER_LENGTH);
		nextMessageType = MessageType.HEADER;
		idleTaskFuture = Scheduler.getWheelTimer().runAfterDelay(idleTask, IDLE_TIME);
	}

	/**
	 * This may only be called from the ClientListener boss thread, in the
	 * Selector loop.
	 */
	/* package-private */ ByteBuffer readBuffer() {
		return readBuffer;
	}

	/**
	 * This may only be called from the ClientListener boss thread, in the
	 * Selector loop.
	 * @param readBytes
	 * @return null if nothing was processed, an array of length 0 if the header
	 * was fully read, or an array of length 2 consisting of this session's
	 * receive IV for the just received message at index 0 and the encrypted
	 * message body at index 1 if the message body was fully read.
	 */
	/* package-private */ byte[][] readMessage(int readBytes) {
		idleTaskFuture.cancel(false);
		if (readBytes == -1) {
			//connection closed
			close("EOF received", null);
			return null;
		}
		if (readBuffer.remaining() != 0) { //buffer is still not full
			//we limited buffer to the expected length of the next packet - continue reading
			idleTaskFuture = Scheduler.getWheelTimer().runAfterDelay(idleTask, IDLE_TIME);
			return null;
		}
		switch (nextMessageType) {
			case HEADER: {
				readBuffer.flip();
				byte[] message = new byte[readBuffer.remaining()];
				readBuffer.get(message);
				if (!MapleAesOfb.checkPacket(message, recvIv)) {
					close("Failed packet test", null);
					return null;
				}
				int length = MapleAesOfb.getPacketLength(message);

				readBuffer.clear();
				if (length > readBuffer.remaining())
					readBuffer = ByteBuffer.allocate(length);
				readBuffer.limit(length);
				nextMessageType = MessageType.BODY;
				idleTaskFuture = Scheduler.getWheelTimer().runAfterDelay(idleTask, IDLE_TIME);
				return EMPTY_ARRAY;
			} case BODY: {
				readBuffer.flip();
				byte[] message = new byte[readBuffer.remaining()];
				readBuffer.get(message);
				//since recvIv can only be touched here (excluding the one-time
				//initialization and sending to client), and this method can
				//only be called from the boss thread of ClientListener in the
				//Selector loop, we don't need recvIv access to be thread-safe.
				byte[] iv = recvIv;
				recvIv = MapleAesOfb.nextIv(iv);
				readBuffer.clear();
				readBuffer.limit(HEADER_LENGTH);
				nextMessageType = MessageType.HEADER;
				idleTaskFuture = Scheduler.getWheelTimer().runAfterDelay(idleTask, IDLE_TIME);
				return new byte[][] { iv, message };
			} default: {
				return null;
			}
		}
	}

	private static byte[] pingMessage() {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(2);
		lew.writeShort(ClientSendOps.PING);
		return lew.getBytes();
	}

	private class KeepAliveTask implements Runnable {
		private final AtomicReference<ScheduledFuture<?>> future;

		public KeepAliveTask() {
			future = new AtomicReference<ScheduledFuture<?>>(null);
		}

		public void sendPing() {
			send(pingMessage());
		}

		public void waitForPong() {
			future.set(Scheduler.getWheelTimer().runAfterDelay(this, TIMEOUT));
		}

		@Override
		public void run() {
			close("Timed out after " + TIMEOUT + " milliseconds", null);
		}

		public void receivedPong() {
			stop();
		}

		public void stop() {
			ScheduledFuture<?> old = future.getAndSet(null);
			if (old != null)
				old.cancel(false);
		}
	}
}
