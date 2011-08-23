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

package argonms.center.net.remoteadmin;

import argonms.common.net.OrderedQueue;
import argonms.common.net.Session;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

//TODO: find some kind of application-level keepalive, or just kick users after
//a idle timeout.
/**
 *
 * @author GoldenKevin
 */
public class TelnetSession implements Session {
	private static final Logger LOG = Logger.getLogger(TelnetSession.class.getName());
	private static final int BUFFER_SIZE = 1024;

	private static final Charset ascii = Charset.forName("US-ASCII");
	private static final String newLine = "\r\n";
	private static final Pattern newLineRegex = Pattern.compile(newLine);
	private static final int newLineLength = newLine.length();

	private final SocketChannel commChn;
	private final AtomicBoolean closeEventsTriggered;
	private final ByteBuffer readBuffer;
	private final CloseListener onClose;
	private final TelnetClient client;

	private final SelectionKey selectionKey;
	private final OrderedQueue sendQueue;

	private final CommandReceivedDelegate commandHandler;
	private final Lock readLock;
	private final StringBuilder inputBuffer;
	private int inputCount;
	private int inputCursor;

	/* package-private */ interface CloseListener {
		public void closed(TelnetSession session);
	}

	/* package-private */ interface CommandReceivedDelegate {
		public void lineReceived(String message, TelnetClient client);
	}

	/* package-private */ TelnetSession(SocketChannel channel, SelectionKey key, TelnetClient client, CommandReceivedDelegate processor, CloseListener onClose) {
		closeEventsTriggered = new AtomicBoolean(false);
		readBuffer = ByteBuffer.allocate(BUFFER_SIZE);
		sendQueue = new OrderedQueue();
		readLock = new ReentrantLock();
		inputBuffer = new StringBuilder(BUFFER_SIZE);

		this.commChn = channel;
		this.onClose = onClose;
		this.client = client;
		this.selectionKey = key;
		this.commandHandler = processor;
	}

	public TelnetClient getClient() {
		return client;
	}

	@Override
	public SocketAddress getAddress() {
		return commChn.socket().getRemoteSocketAddress();
	}

	private void send(ByteBuffer buf) {
		int queueInsertNo;
		synchronized (sendQueue) {
			queueInsertNo = sendQueue.getNextPush();
		}
		sendQueue.insert(queueInsertNo, buf);
		if (sendQueue.willBlock() || !sendMessages() && selectionKey.isValid()) {
			selectionKey.interestOps(selectionKey.interestOps() | SelectionKey.OP_WRITE);
			selectionKey.selector().wakeup();
		}
	}

	private void processLine(String line) {
		switch (client.getState()) {
			case LOGIN:
				client.setUsername(line);
				break;
			case PASSWORD:
				send(newLine);
				client.authenticate(line);
				break;
			case MESSAGE:
				commandHandler.lineReceived(line, client);
				if (commChn.isOpen())
					client.writePrompt();
				break;
		}
	}

	public final void send(String message) {
		send(ascii.encode(CharBuffer.wrap(message)));
	}

	@Override
	public void send(byte[] b) {
		send(ByteBuffer.wrap(b));
	}

	@Override
	public void close(String reason, Throwable reasonExc) {
		if (closeEventsTriggered.compareAndSet(false, true)) {
			try {
				commChn.close();
			} catch (IOException e) {
				LOG.log(Level.FINE, "Error while closing telnet client " + getClient().getAccountName() + " (" + getAddress() + ")", e);
			}

			if (reasonExc == null)
				LOG.log(Level.FINE, "Telnet client {0} ({1}) disconnected: {2}", new Object[] { getClient().getAccountName(), getAddress(), reason });
			else
				LOG.log(Level.FINE, "Telnet client " + getClient().getAccountName() + " (" + getAddress() + ") disconnected: " + reason, reasonExc);
			client.disconnected();
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
			close("Error while writing", ex);
			return false;
		} finally {
			sendQueue.incrementPopCursor(i);
			sendQueue.setCanWrite();
		}
	}

	private int asciiControlCodesAndVt100(byte[] message, int i, ByteArrayOutputStream echo) {
		switch (message[i]) {
			case '\r':
			case '\n': //new line
				echo.write(message[i]);
				inputBuffer.insert(inputCursor, (char) message[i]);
				inputCursor++;
				inputCount++;
				break;
			case 0x1B: //Esc - swallow the ones we don't care about
				if (i + 1 < message.length) {
					switch (message[i + 1]) {
						case 0x1B:
							//function keys
							if (i + 3 < message.length && i + 1 == 0x31 && i + 3 == '~')
								i += 3;
							break;
						case '[':
							//vt100 escape sequences
							if (i + 2 < message.length) {
								switch (message[i + 2]) {
									case 'A':
										//cursor up
										//TODO: set cursor just to the right of
										//name>, then echo { 0x1B, '[', 'K' },
										//then echo the last line from history,
										//and finally set cursor just to right
										//of brought up text
										i += 2;
										break;
									case 'B':
										//cursor down
										i += 2;
										break;
									case 'C':
										//cursor right
										if (inputCursor < inputCount) {
											echo.write(message, i, 3);
											inputCursor++;
										}
										i += 2;
										break;
									case 'D':
										//cursor left
										//TODO: this should insert chars and
										//shift existing chars to right instead
										//of replacing existing chars when
										//typing after shifting cursor. wrong
										//VT100 codes perhaps? Windows Telnet
										//server and client have it right, so I
										//guess I'll packet sniff them.
										if (inputCursor > 0) {
											echo.write(message, i, 3);
											inputCursor--;
										}
										i += 2;
										break;
								}
							}
							break;
					}
				}
				break;
			case '\b': //ASCII non-destructive backspace (Windows telnet)
				if (inputCursor <= 0)
					break;
				echo.write(message[i]);
				echo.write(' ');
			case 0x7F: //Ctrl-? destructive backspace (PuTTY)
				if (inputCursor > 0) {
					inputCursor--;
					inputCount--;
					inputBuffer.deleteCharAt(inputCursor);
					echo.write(message[i]);
				}
				break;
		}
		return i;
	}

	/**
	 * This may only be called from the TelnetListener boss thread, in the
	 * Selector loop.
	 */
	/* package-private */ void processRead(byte[] message) {
		ByteArrayOutputStream echo = new ByteArrayOutputStream(message.length);
		readLock.lock();
		try { //only one thread altering inputBuffer please.
			for (int i = 0; i < message.length; i++) {
				if (message[i] > 0x1F && message[i] < 0x7F) {
					//7-bit ASCII printable characters
					echo.write(message[i]);
					//since it's 7-bit ASCII, we just cast the byte to char without
					//having to worry about wide chars.
					inputBuffer.insert(inputCursor, (char) message[i]);
					inputCursor++;
					inputCount++;
				} else if (message[i] == TelnetClient.IAC) {
					//Telnet commands
					i += client.protocolCmd(message, i + 1);
				} else {
					//7-bit ASCII control characters
					i = asciiControlCodesAndVt100(message, i, echo);
				}
			}

			if (client.willEcho())
				send(ByteBuffer.wrap(echo.toByteArray()));
			String[] statements = newLineRegex.split(inputBuffer, -1);
			String statement;
			int cutTo = 0;
			for (int i = 0; i < statements.length - 1; i++) {
				statement = statements[i];
				processLine(statement);
				cutTo += statement.length() + newLineLength;
			}
			inputCursor -= cutTo;
			inputCount -= cutTo;
			inputBuffer.delete(0, cutTo);
		} finally {
			readLock.unlock();
		}
	}

	/**
	 * This may only be called from the TelnetListener boss thread, in the
	 * Selector loop.
	 */
	/* package-private */ ByteBuffer readBuffer() {
		return readBuffer;
	}

	/**
	 * This may only be called from the TelnetListener boss thread, in the
	 * Selector loop.
	 * @param readBytes
	 */
	/* package-private */ byte[] readMessage(int readBytes) {
		if (readBytes == -1) {
			//connection closed
			close("EOF received", null);
			return null;
		}
		byte[] message = new byte[readBytes];
		readBuffer.flip();
		readBuffer.get(message);
		readBuffer.clear();
		return message;
	}
}
