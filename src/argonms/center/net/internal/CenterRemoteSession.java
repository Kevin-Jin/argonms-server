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

package argonms.center.net.internal;

import argonms.center.CenterServer;
import argonms.common.ServerType;
import argonms.common.net.Session;
import argonms.common.net.UnorderedQueue;
import argonms.common.net.internal.CenterRemoteOps;
import argonms.common.net.internal.RemoteCenterOps;
import argonms.common.util.Scheduler;
import argonms.common.util.input.LittleEndianByteArrayReader;
import argonms.common.util.input.LittleEndianReader;
import argonms.common.util.output.LittleEndianByteArrayWriter;
import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author GoldenKevin
 */
public class CenterRemoteSession implements Session {
	private static final Logger LOG = Logger.getLogger(CenterRemoteSession.class.getName());
	private static final int HEADER_LENGTH = 4;
	//1kb as the initial buffer size for each client isn't too unreasonable...
	private static final int DEFAULT_BUFFER_SIZE = 1024;
	private static final byte[] EMPTY_ARRAY = new byte[0];
	private static final int IDLE_TIME = 15000; //in milliseconds
	private static final int TIMEOUT = 15000; //in milliseconds

	private final SocketChannel commChn;
	private final AtomicBoolean closeEventsTriggered;
	private ByteBuffer readBuffer;
	private CenterRemoteInterface cri;

	private final SelectionKey selectionKey;
	private final UnorderedQueue sendQueue;

	private KeepAliveTask heartbeatTask;
	private final Runnable idleTask = new Runnable() {
		@Override
		public void run() {
			startPingTask();
		}
	};
	private ScheduledFuture<?> idleTaskFuture;

	private MessageType nextMessageType;

	private final String interServerPwd;

	/* package-private */ CenterRemoteSession(SocketChannel channel, SelectionKey key, String authKey) {
		closeEventsTriggered = new AtomicBoolean(false);
		readBuffer = ByteBuffer.allocate(DEFAULT_BUFFER_SIZE);
		readBuffer.order(ByteOrder.LITTLE_ENDIAN);
		readBuffer.limit(HEADER_LENGTH);
		sendQueue = new UnorderedQueue();
		heartbeatTask = new KeepAliveTask();
		nextMessageType = MessageType.HEADER;

		this.commChn = channel;
		this.selectionKey = key;
		this.interServerPwd = authKey;

		idleTaskFuture = Scheduler.getWheelTimer().runAfterDelay(idleTask, IDLE_TIME);
	}

	public CenterRemoteInterface getModel() {
		return cri;
	}

	@Override
	public SocketAddress getAddress() {
		return commChn.socket().getRemoteSocketAddress();
	}

	public String getServerName() {
		return cri != null ? cri.getServerName() : null;
	}

	@Override
	public void send(byte[] b) {
		ByteBuffer buf = ByteBuffer.allocate(b.length + HEADER_LENGTH);
		buf.order(ByteOrder.LITTLE_ENDIAN);
		buf.putInt(b.length);
		buf.put(b);
		buf.flip();
		sendQueue.insert(buf);
		try {
			if (selectionKey.isValid() && tryFlushSendQueue() == 0) {
				selectionKey.interestOps(selectionKey.interestOps() | SelectionKey.OP_WRITE);
				selectionKey.selector().wakeup();
			}
		} catch (CancelledKeyException e) {
			//don't worry about it - session is already closed
		}
	}

	/* package-private */ void process(byte[] message) {
		LittleEndianByteArrayReader packet = new LittleEndianByteArrayReader(message);
		if (cri != null) {
			cri.getPacketProcessor().process(packet);
		} else {
			recvInitPacket(packet);
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
	public boolean close(String reason) {
		if (closeEventsTriggered.compareAndSet(false, true)) {
			try {
				commChn.close();
			} catch (IOException ex) {
				LOG.log(Level.WARNING, "Error while closing " + getServerName() + " server (" + getAddress() + ")", ex);
			}
			stopPingTask();
			//idleTaskFuture has to be non-null as long as the this pointer was not leaked in the constructor
			idleTaskFuture.cancel(false);

			LOG.log(Level.FINE, "{0} server ({1}) disconnected: {2}", new Object[] { getServerName(), getAddress(), reason });
			if (cri != null)
				cri.disconnected();
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
	/* package-private */ int tryFlushSendQueue() {
		if (!sendQueue.shouldWrite())
			return -1;
		try {
			do {
				int success = 0;
				Iterator<ByteBuffer> iter = sendQueue.pop().iterator();
				while (iter.hasNext()) {
					ByteBuffer buf = iter.next();
					if (buf.remaining() == commChn.write(buf)) {
						success++;
					} else {
						sendQueue.insert(buf);
						while (iter.hasNext())
							sendQueue.insert(iter.next());
						return 0;
					}
				}
			} while (!sendQueue.willBlock());
			return 1;
		} catch (IOException ex) {
			//does an IOException in write always mean an invalid channel?
			close(ex.getMessage());
			return -2;
		} finally {
			sendQueue.setCanWrite();
		}
	}

	private void recvInitPacket(LittleEndianReader packet) {
		String response, localError = null;

		if (packet.available() >= 4 && packet.readByte() == RemoteCenterOps.AUTH) {
			byte serverId = packet.readByte();
			if (packet.readLengthPrefixedString().equals(interServerPwd)) {
				if (!CenterServer.getInstance().isServerConnected(serverId)) {
					cri = CenterRemoteInterface.makeByServerId(serverId, this);
					cri.makePacketProcessor(); //don't leak Interface reference in its constructor
					response = null;
					if (ServerType.isGame(serverId)) {
						byte world = packet.readByte();
						byte[] channels = new byte[packet.readByte()];
						for (int i = 0; i < channels.length; i++)
							channels[i] = packet.readByte();
						List<CenterGameInterface> servers = CenterServer.getInstance().getAllServersOfWorld(world, serverId);
						List<Byte> conflicts = new ArrayList<Byte>();
						for (CenterGameInterface server : servers) {
							if (server.isShuttingDown())
								continue;

							for (int i = 0; i < channels.length; i++) {
								Byte ch = Byte.valueOf(channels[i]);
								if (server.getChannels().contains(ch))
									conflicts.add(ch);
							}
						}
						if (!conflicts.isEmpty()) {
							StringBuilder sb = new StringBuilder("World ").append(world).append(", ").append("channel");
							sb.append(conflicts.size() == 1 ? " " : "s ");
							for (Byte ch : conflicts)
								sb.append(ch).append(", ");
							String list = sb.substring(0, sb.length() - 2);
							localError = "Trying to add duplicate " + list;
							response = list + " already connected.";
						}
					}
				} else {
					localError = "Another server with the same ID is already connected";
					response = ServerType.getName(serverId) + " server already connected.";
				}
			} else {
				localError = "Supplied wrong auth password";
				response = "Wrong auth password.";
			}
		} else {
			localError = "Malformed auth packet";
			response = "Invalid auth packet.";
		}

		LittleEndianByteArrayWriter responsePacket = new LittleEndianByteArrayWriter(response == null ? 3 : 3 + response.length());
		responsePacket.writeByte(CenterRemoteOps.AUTH_RESPONSE);
		responsePacket.writeLengthPrefixedString(response == null ? "" : response);
		send(responsePacket.getBytes());
		if (response != null) {
			close(localError);
			cri = null;
		}
	}

	/**
	 * This may only be called from the RemoteServerListener boss thread, in the
	 * Selector loop.
	 */
	/* package-private */ ByteBuffer readBuffer() {
		return readBuffer;
	}

	/**
	 * This may only be called from the RemoteServerListener boss thread, in the
	 * Selector loop.
	 * @param readBytes
	 * @return null if nothing was processed, an array of length 0 if the header
	 * was fully read, or the just received message if the body was fully read.
	 */
	/* package-private */ byte[] readMessage(int readBytes) {
		idleTaskFuture.cancel(false);
		if (readBytes == -1) {
			//connection closed
			close("EOF received");
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
			}
			case BODY: {
				readBuffer.flip();
				byte[] message = new byte[readBuffer.remaining()];
				readBuffer.get(message);
				readBuffer.clear();
				readBuffer.limit(HEADER_LENGTH);
				nextMessageType = MessageType.HEADER;
				idleTaskFuture = Scheduler.getWheelTimer().runAfterDelay(idleTask, IDLE_TIME);
				return message;
			}
			default:
				return null;
		}
	}

	private static byte[] pingMessage() {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(1);
		lew.writeByte(CenterRemoteOps.PING);
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
			close("Timed out after " + TIMEOUT + " milliseconds");
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
