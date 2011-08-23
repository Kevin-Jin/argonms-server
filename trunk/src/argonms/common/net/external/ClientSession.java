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
	private int nextMessageExpectedLength;

	private final Lock sendIvLock;
	private byte[] recvIv, sendIv;

	/* package-private */ interface CloseListener<T extends RemoteClient> {
		public void closed(ClientSession<T> session);
	}

	/* package-private */ ClientSession(SocketChannel channel, SelectionKey key, T client, CloseListener<T> onClose) {
		closeEventsTriggered = new AtomicBoolean(false);
		sendQueue = new OrderedQueue();
		heartbeatTask = new KeepAliveTask();

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

	public boolean isOpen() {
		return commChn.isOpen();
	}

	private void send(int queueInsertNo, ByteBuffer buf) {
		sendQueue.insert(queueInsertNo, buf);
		if (sendQueue.willBlock() || !sendMessages() && selectionKey.isValid()) {
			selectionKey.interestOps(selectionKey.interestOps() | SelectionKey.OP_WRITE);
			selectionKey.selector().wakeup();
		}
	}

	@Override
	public void send(byte[] input) {
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
		byte[] header = MapleAesOfb.getPacketHeader(input.length, iv);
		byte[] output = new byte[header.length + input.length];
		MapleAesOfb.mapleEncrypt(input);
		MapleAesOfb.aesCrypt(input, iv);
		System.arraycopy(header, 0, output, 0, header.length);
		System.arraycopy(input, 0, output, header.length, input.length);
		send(queueInsertNo, ByteBuffer.wrap(output));
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

	public void close() {
		if (closeEventsTriggered.compareAndSet(false, true)) {
			try {
				commChn.close();
			} catch (IOException e) {
				LOG.log(Level.WARNING, "Error closing client " + getAccountName() + " (" + getAddress() + ")", e);
			}
			stopPingTask();
			onClose.closed(this);
		}
	}

	/**
	 *
	 * @return false if these messages needs to remain on the queue, or if there
	 * was a write error. Please make sure to check selectionKey.isValid() if
	 * false is returned before adding this message to this queue or else you
	 * may get a CancelledKeyException.
	 */
	/* package-private */ boolean sendMessages() {
		if (!sendQueue.shouldWrite())
			return true;
		int i = 0;
		try {
			Iterator<ByteBuffer> iter = sendQueue.pop().iterator();
			while (iter.hasNext()) {
				ByteBuffer buf = iter.next();
				if (buf.remaining() == commChn.write(buf)) {
					i++;
				} else {
					int start = sendQueue.currentPopBlock() + i;
					int j = 0;
					sendQueue.insert(start + j++, buf);
					while (iter.hasNext())
						sendQueue.insert(start + j++, iter.next());
					return false;
				}
			}
			return true;
		} catch (IOException ex) {
			//does an IOException in write always mean an invalid channel?
			LOG.log(Level.WARNING, "Error writing message to client " + getAccountName() + " (" + getAddress() + ")", ex);
			close();
			return false;
		} finally {
			sendQueue.incrementPopCursor(i);
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
		//assert sendIvLock is not taken
		send(sendQueue.getNextPush(), buf);

		readBuffer = ByteBuffer.allocate(DEFAULT_BUFFER_SIZE);
		readBuffer.limit(HEADER_LENGTH);
		nextMessageExpectedLength = HEADER_LENGTH;
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
			close();
			return null;
		}
		if (readBytes < nextMessageExpectedLength) {
			//continue reading
			idleTaskFuture = Scheduler.getWheelTimer().runAfterDelay(idleTask, IDLE_TIME);
			return null;
		}
		switch (nextMessageType) {
			case HEADER: {
				byte[] message = new byte[nextMessageExpectedLength];
				readBuffer.flip();
				readBuffer.get(message);
				if (!MapleAesOfb.checkPacket(message, recvIv)) {
					LOG.log(Level.WARNING, "Client failed packet test -> disconnecting");
					close();
					return null;
				}
				int length = MapleAesOfb.getPacketLength(message);

				readBuffer.clear();
				if (length > readBuffer.remaining())
					readBuffer = ByteBuffer.allocate(length);
				readBuffer.limit(length);
				nextMessageExpectedLength = length;
				nextMessageType = MessageType.BODY;
				idleTaskFuture = Scheduler.getWheelTimer().runAfterDelay(idleTask, IDLE_TIME);
				return EMPTY_ARRAY;
			} case BODY: {
				byte[] message = new byte[nextMessageExpectedLength];
				readBuffer.flip();
				readBuffer.get(message);
				//since recvIv can only be touched here (excluding the one-time
				//initialization and sending to client), and this method can
				//only be called from the boss thread of ClientListener in the
				//Selector loop, we don't need recvIv access to be thread-safe.
				byte[] iv = recvIv;
				recvIv = MapleAesOfb.nextIv(iv);
				readBuffer.clear();
				readBuffer.limit(HEADER_LENGTH);
				nextMessageExpectedLength = HEADER_LENGTH;
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
			LOG.log(Level.FINE, "Account {0} timed out after " + TIMEOUT
					+ " milliseconds -> Disconnecting.", getAccountName());
			close();
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
