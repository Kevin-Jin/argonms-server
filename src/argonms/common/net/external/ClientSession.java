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
import argonms.common.tools.Rng;
import java.net.SocketAddress;
import java.util.Random;
import org.jboss.netty.channel.Channel;

/**
 *
 * @author GoldenKevin
 */
public class ClientSession<T extends RemoteClient> {
	private Channel ch;
	private MapleAESOFB sendCypher, recvCypher;
	private T client;

	public ClientSession(Channel ch, T client) {
		this.ch = ch;

		Random generator = Rng.getGenerator();
		byte[] ivRecv = { 70, 114, 122, (byte) generator.nextInt(256) };
		byte[] ivSend = { 82, 48, 120, (byte) generator.nextInt(256) };

		this.recvCypher = new MapleAESOFB(ivRecv, GlobalConstants.MAPLE_VERSION);
		this.sendCypher = new MapleAESOFB(ivSend, (short) (0xFFFF - GlobalConstants.MAPLE_VERSION));

		this.client = client;
	}

	public MapleAESOFB getRecvCypher() {
		return recvCypher;
	}

	public MapleAESOFB getSendCypher() {
		return sendCypher;
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
		ch.write(input);
	}

	public void close() {
		ch.disconnect();
	}
}
