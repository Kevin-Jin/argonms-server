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

package argonms.common.net.internal;

import argonms.common.net.Session;
import argonms.common.net.SessionCreator;
import argonms.common.util.Scheduler;
import argonms.common.util.output.LittleEndianByteArrayWriter;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author GoldenKevin
 */
public class RemoteCenterSession<T extends RemoteCenterInterface> implements Session, SessionCreator {
	private static final Logger LOG = Logger.getLogger(RemoteCenterSession.class.getName());
	private static final int HEADER_LENGTH = 4;
	//1kb as the initial buffer size for each client isn't too unreasonable...
	private static final int DEFAULT_BUFFER_SIZE = 1024;
	private static final byte[] EMPTY_ARRAY = new byte[0];
	private static final int IDLE_TIME = 15000; //in milliseconds
	private static final int TIMEOUT = 15000; //in milliseconds

	private final SocketChannel commChn;
	private final AtomicBoolean closeEventsTriggered;
	private ByteBuffer readBuffer;
	private final CloseListener<T> onClose;
	private final T server;

	private KeepAliveTask heartbeatTask;
	private final Runnable idleTask = new Runnable() {
		@Override
		public void run() {
			startPingTask();
		}
	};
	private ScheduledFuture<?> idleTaskFuture;

	private MessageType nextMessageType;

	private final ExecutorService workerThreadPool;
	private String interServerPwd;

	private interface CloseListener<T extends RemoteCenterInterface> {
		public void closed(RemoteCenterSession<T> session);
	}

	private RemoteCenterSession(SocketChannel channel, T server, String password, ExecutorService workerThreadPool, CloseListener<T> onClose) {
		closeEventsTriggered = new AtomicBoolean(false);
		heartbeatTask = new KeepAliveTask();

		this.commChn = channel;
		this.onClose = onClose;
		this.server = server;
		this.workerThreadPool = workerThreadPool;
		this.interServerPwd = password;
	}

	public RemoteCenterInterface getModel() {
		return server;
	}

	@Override
	public SocketAddress getAddress() {
		return commChn.socket().getRemoteSocketAddress();
	}

	public void awaitClose() {
		boolean complete;
		do {
			try {
				complete = workerThreadPool.awaitTermination(Integer.MAX_VALUE, TimeUnit.DAYS);
			} catch (InterruptedException ex) {
				complete = false;
			}
		} while (!complete);
	}

	//maybe we should encrypt because we do send a plaintext password after all
	/**
	 * Beware that this method will block if the specified message cannot be
	 * sent in its entirety.
	 * @param b the message to send
	 */
	@Override
	public void send(byte[] b) {
		ByteBuffer buf = ByteBuffer.allocate(b.length + HEADER_LENGTH);
		buf.order(ByteOrder.LITTLE_ENDIAN);
		buf.putInt(b.length);
		buf.put(b);
		buf.flip();
		try {
			//spin loop if we don't write the entire buffer (although in
			//blocking mode, that should never happen...)
			while (buf.remaining() != commChn.write(buf));
		} catch (IOException ex) {
			//does an IOException in write always mean an invalid channel?
			close("Error while writing", ex);
		}
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
			try {
				commChn.close();
			} catch (IOException ex) {
				LOG.log(Level.WARNING, "Error while closing center server ( " + getAddress() + ")", ex);
			}
			stopPingTask();
			idleTaskFuture.cancel(false);

			if (reasonExc == null)
				LOG.log(Level.FINE, "Disconnected from center server ({0}): {1}", new Object[] { getAddress(), reason });
			else
				LOG.log(Level.FINE, "Disconnected from center server (" + getAddress() + "): " + reason, reasonExc);
			server.disconnected();
			onClose.closed(this);
			return true;
		}
		return false;
	}

	/**
	 * This may only be called from the RemoteCenterSession worker thread, in
	 * the read loop.
	 */
	private void sendInitPacket() {
		send(server.auth(interServerPwd));
		interServerPwd = null;

		readBuffer = ByteBuffer.allocate(DEFAULT_BUFFER_SIZE);
		readBuffer.order(ByteOrder.LITTLE_ENDIAN);
		readBuffer.limit(HEADER_LENGTH);
		nextMessageType = MessageType.HEADER;

		idleTaskFuture = Scheduler.getWheelTimer().runAfterDelay(idleTask, IDLE_TIME);
	}

	/**
	 * This may only be called from the RemoteCenterSession worker thread, in
	 * the read loop.
	 */
	private ByteBuffer readBuffer() {
		return readBuffer;
	}

	/**
	 * This may only be called from the RemoteCenterSession worker thread, in
	 * the read loop.
	 */
	private byte[] readMessage(int readBytes) {
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
				int length = readBuffer.getInt();

				readBuffer.clear();
				if (length > readBuffer.remaining()) {
					readBuffer = ByteBuffer.allocate(length);
					readBuffer.order(ByteOrder.LITTLE_ENDIAN);
				}
				readBuffer.limit(length);
				nextMessageType = MessageType.BODY;
				idleTaskFuture = Scheduler.getWheelTimer().runAfterDelay(idleTask, IDLE_TIME);
				return EMPTY_ARRAY;
			} case BODY: {
				readBuffer.flip();
				byte[] message = new byte[readBuffer.remaining()];
				readBuffer.get(message);
				readBuffer.clear();
				readBuffer.limit(HEADER_LENGTH);
				nextMessageType = MessageType.HEADER;
				idleTaskFuture = Scheduler.getWheelTimer().runAfterDelay(idleTask, IDLE_TIME);
				return message;
			} default: {
				return null;
			}
		}
	}

	private static byte[] pingMessage() {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(1);
		lew.writeByte(RemoteCenterOps.PING);
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

	/**
	 * This method will block until we lose connection with the center server,
	 * so be careful where you place it!
	 * @param ip the name of the host of the center server
	 * @param port the center server's listening port
	 */
	public static <T extends RemoteCenterInterface> RemoteCenterSession<T> connect(final String ip, final int port, final String authKey, final T serverState) {
		ExecutorService workerThreadPool = Executors.newSingleThreadExecutor(new ThreadFactory() {
			private final ThreadGroup group;

			{
				SecurityManager s = System.getSecurityManager();
				group = (s != null)? s.getThreadGroup() :
									 Thread.currentThread().getThreadGroup();
			}

			@Override
			public Thread newThread(Runnable r) {
				Thread t = new Thread(group, r, "internal-worker-thread", 0);
				if (t.isDaemon())
					t.setDaemon(false);
				if (t.getPriority() != Thread.NORM_PRIORITY)
					t.setPriority(Thread.NORM_PRIORITY);
				return t;
			}
		});

		try {
			final SocketChannel center = SocketChannel.open();
			center.socket().setTcpNoDelay(false);
			center.configureBlocking(true);
			center.connect(new InetSocketAddress(ip, port));
			final RemoteCenterSession<T> session = new RemoteCenterSession<T>(center, serverState, authKey, workerThreadPool, new CloseListener<T>() {
				@Override
				public void closed(RemoteCenterSession<T> session) {
				}
			});
			serverState.setSession(session);
			LOG.log(Level.FINE, "Connected to Center server at {0}", session.getAddress());
			workerThreadPool.submit(new Runnable() {
				@Override
				public void run() {
					session.sendInitPacket();
					while (center.isOpen()) {
						try {
							int read = center.read(session.readBuffer());
							byte[] message = session.readMessage(read);
							if (message != null && message.length != 0) {
								//the body was received successfully
								try {
									serverState.process(message);
								} catch (Exception ex) {
									LOG.log(Level.WARNING, "Uncaught exception while processing packet from Center server (" + session.getAddress() + ")", ex);
								}
							}
						} catch (IOException ex) {
							//does an IOException in read always mean an invalid channel?
							session.close("Error while reading", ex);
						}
					}
				}
			});
			workerThreadPool.shutdown();
			return session;
		} catch (IOException ex) {
			LOG.log(Level.SEVERE, "Could not connect to center server at " + ip + ":" + port, ex);
			return null;
		}
	}
}
