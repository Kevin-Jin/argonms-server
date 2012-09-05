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

import argonms.common.net.SessionCreator;
import argonms.common.net.external.ClientSession.CloseListener;
import argonms.common.util.input.LittleEndianByteArrayReader;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author GoldenKevin
 */
public class ClientListener<T extends RemoteClient> implements SessionCreator {
	public interface ClientFactory<T extends RemoteClient> {
		public T newInstance();
	}

	private static final Logger LOG = Logger.getLogger(ClientListener.class.getName());
	private final ExecutorService bossThreadPool, workerThreadPool;
	private final ClientPacketProcessor<T> pp;
	private final ClientFactory<T> clientCtor;
	private ServerSocketChannel listener;
	private final AtomicBoolean closeEventsTriggered;

	public ClientListener(ClientPacketProcessor<T> packetProcessor, ClientFactory<T> clientFactory) {
		closeEventsTriggered = new AtomicBoolean(false);
		bossThreadPool = Executors.newSingleThreadExecutor(new ThreadFactory() {
			private final ThreadGroup group;

			{
				SecurityManager s = System.getSecurityManager();
				group = (s != null)? s.getThreadGroup() :
									 Thread.currentThread().getThreadGroup();
			}

			@Override
			public Thread newThread(Runnable r) {
				Thread t = new Thread(group, r, "external-boss-thread", 0);
				if (t.isDaemon())
					t.setDaemon(false);
				if (t.getPriority() != Thread.NORM_PRIORITY)
					t.setPriority(Thread.NORM_PRIORITY);
				return t;
			}
		});
		workerThreadPool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 2, new ThreadFactory() {
			private final ThreadGroup group;
			private final AtomicInteger threadNumber = new AtomicInteger(1);

			{
				SecurityManager s = System.getSecurityManager();
				group = (s != null)? s.getThreadGroup() :
									 Thread.currentThread().getThreadGroup();
			}

			@Override
			public Thread newThread(Runnable r) {
				Thread t = new Thread(group, r, "external-worker-pool-thread-" + threadNumber.getAndIncrement(), 0);
				if (t.isDaemon())
					t.setDaemon(false);
				if (t.getPriority() != Thread.NORM_PRIORITY)
					t.setPriority(Thread.NORM_PRIORITY);
				return t;
			}
		});

		pp = packetProcessor;
		clientCtor = clientFactory;
	}

	public boolean bind(int port) {
		try {
			listener = ServerSocketChannel.open();
			listener.socket().bind(new InetSocketAddress(port));
			LOG.log(Level.INFO, "Listening on port {0}", port);
			listener.configureBlocking(false);
			bossThreadPool.submit(new Runnable() {
				@Override
				public void run() {
					try {
						Selector selector = Selector.open();
						SelectionKey acceptorKey = listener.register(selector, SelectionKey.OP_ACCEPT);
						final Map<SelectionKey, ClientSession<T>> connected = new ConcurrentHashMap<SelectionKey, ClientSession<T>>();
						while (selector.isOpen()) {
							selector.select();
							Set<SelectionKey> keys = selector.selectedKeys();

							for (Iterator<SelectionKey> keyIter = keys.iterator(); keyIter.hasNext(); ) {
								SelectionKey key = keyIter.next();
								keyIter.remove();

								if (key == acceptorKey) {
									if (key.isValid() && key.isAcceptable()) {
										try {
											SocketChannel client = listener.accept();
											client.socket().setTcpNoDelay(true);
											client.configureBlocking(false);
											LOG.log(Level.FINE, "Client connected from {0}", client.socket().getRemoteSocketAddress());
											final SelectionKey acceptedKey = client.register(selector, SelectionKey.OP_READ);
											T clientState = clientCtor.newInstance();
											ClientSession<T> session = new ClientSession<T>(client, acceptedKey, clientState, new CloseListener<T>() {
												@Override
												public void closed(ClientSession<T> session) {
													connected.remove(acceptedKey);
												}
											});
											clientState.setSession(session);
											connected.put(acceptedKey, session);
											session.sendInitPacket();
										} catch (IOException ex) {
											close("Error while accepting client", ex);
										}
									}
								} else {
									SocketChannel client = (SocketChannel) key.channel();
									final ClientSession<T> session = connected.get(key);
									try {
										if (key.isValid() && key.isReadable()) {
											try {
												int read = client.read(session.readBuffer());
												byte[][] ivAndMessage = session.readMessage(read);
												if (ivAndMessage != null) {
													//the header or the body was received successfully
													if (ivAndMessage.length == 0) {
														//header received, try a non-blocking read to see if we also got the message body
														read = client.read(session.readBuffer());
														ivAndMessage = session.readMessage(read);
													}
													if (ivAndMessage != null && ivAndMessage.length == 2) {
														session.readEnqueued();
														//decrypt the body and handle it on a worker thread
														final byte[] iv = ivAndMessage[0];
														final byte[] body = ivAndMessage[1];
														workerThreadPool.submit(new Runnable() {
															@Override
															public void run() {
																try {
																	ClientEncryption.aesOfbCrypt(body, iv);
																	ClientEncryption.mapleDecrypt(body);
																	pp.process(new LittleEndianByteArrayReader(body), session.getClient());
																} catch (Exception ex) {
																	LOG.log(Level.WARNING, "Uncaught exception while processing packet from client " + session.getAccountName() + " (" + session.getAddress() + ")", ex);
																} finally {
																	session.readDequeued();
																}
															}
														});
													}
												}
											} catch (IOException ex) {
												//does an IOException in read always mean an invalid channel?
												session.close("Error while reading", ex);
											}
										}
										if (key.isValid() && key.isWritable())
											if (session.tryFlushSendQueue() == 1)
												key.interestOps(key.interestOps() & ~SelectionKey.OP_WRITE);
									} catch (CancelledKeyException e) {
										//don't worry about it - session is already closed
									}
								}
							}
						}
					} catch (IOException ex) {
						close("Error while opening", ex);
					}
				}
			});
			return true;
		} catch (IOException ex) {
			LOG.log(Level.SEVERE, "Could not bind on port " + port, ex);
			return false;
		}
	}

	public void close(String reason, Throwable reasonExc) {
		if (closeEventsTriggered.compareAndSet(false, true)) {
			try {
				listener.close();
			} catch (IOException ex) {
				LOG.log(Level.WARNING, "Error while unbinding external facing selector (" + listener.socket().getLocalSocketAddress() + ")", ex);
			}
			if (reasonExc == null)
				LOG.log(Level.FINE, "External facing selector ({0}) closed: {1}", new Object[] { listener.socket().getLocalSocketAddress(), reason });
			else
				LOG.log(Level.FINE, "External facing selector (" + listener.socket().getLocalSocketAddress() + ") closed: " + reason, reasonExc);
			bossThreadPool.shutdown();
			workerThreadPool.shutdown();
		}
	}
}
