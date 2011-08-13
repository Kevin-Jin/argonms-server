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

import argonms.common.util.Rng;
import java.net.SocketAddress;
import java.util.Random;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.jboss.netty.channel.Channel;

/**
 *
 * @author GoldenKevin
 */
public class ClientSession<T extends RemoteClient> {
	private Channel ch;
	private byte[] recvIv, sendIv;
	private Lock recvIvLock, sendIvLock;
	private T client;

	public ClientSession(Channel ch, T client) {
		this.ch = ch;

		Random generator = Rng.getGenerator();
		recvIv = new byte[4];
		sendIv = new byte[4];
		generator.nextBytes(recvIv);
		generator.nextBytes(sendIv);
		recvIvLock = new ReentrantLock();
		sendIvLock = new ReentrantLock();

		this.client = client;
	}

	public void lockRecv() {
		recvIvLock.lock();
	}

	public byte[] getRecvIv() {
		return recvIv;
	}

	public void setRecvIv(byte[] iv) {
		recvIv = iv;
	}

	public void unlockRecv() {
		recvIvLock.unlock();
	}

	public void lockSend() {
		sendIvLock.lock();
	}

	public byte[] getSendIv() {
		return sendIv;
	}

	public void setSendIv(byte[] iv) {
		sendIv = iv;
	}

	public void unlockSend() {
		sendIvLock.unlock();
	}

	public T getClient() {
		return client;
	}

	public void removeClient() {
		this.client = null;
	}

	public SocketAddress getAddress() {
		return ch.getRemoteAddress();
	}

	public boolean isOpen() {
		return ch.isOpen();
	}

	public void send(byte[] input) {
		//for some strange reason, client doesn't like it at all when we send
		//packets concurrently, even if we sync up the IV perfectly. it'll drop
		//packets left and right. so, we'll have to mutex each write request,
		//which shouldn't be noticeable ever in normal gameplay. just beware if
		//you want to schedule a million threads that send a message to the
		//player at the exact same time. if those messages don't freeze the
		//client, then the client will probably not receive all those messages
		//at the exact same time as you intended. mutexing here also makes the
		//access to the only server-side critical section in sending (i.e.
		//getting the send IV and then updating it) thread-safe, since
		//Channel.write directly invokes the channel pipeline on the current
		//thread without queueing it on a worker thread (I believe the pipeline
		//is handled in the current thread, while non-blocking writes are queued
		//in a worker thread), so locking here will lock in the critical section
		//in MaplePacketEncoder.encode for this write request.
		lockSend();
		try {
			ch.write(input);
		} finally {
			unlockSend();
		}
	}

	public void close() {
		ch.disconnect();
	}
}
