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

package argonms.net.server;

import argonms.ServerType;
import argonms.center.CenterServer;
import argonms.tools.input.LittleEndianByteArrayReader;
import argonms.tools.output.LittleEndianByteArrayWriter;
import java.net.SocketAddress;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jboss.netty.channel.Channel;

/**
 *
 * @author GoldenKevin
 */
public class CenterRemoteInterface extends Thread {
	private static final Logger LOG = Logger.getLogger(CenterRemoteInterface.class.getName());

	private String interServerPwd;
	private Channel ch;
	private RemoteCenterPacketProcessor pp;
	private String host;
	private int[] clientPorts;
	private byte world;

	public CenterRemoteInterface(Channel channel, String authKey) {
		this.ch = channel;
		this.interServerPwd = authKey;
		this.world = ServerType.UNDEFINED;
	}

	public SocketAddress getRemoteEndpoint() {
		return ch.getRemoteAddress();
	}

	public void setHost(String host) {
		this.host = host;
	}

	public void setClientPorts(int[] ports) {
		this.clientPorts = ports;
	}

	public String getHost() {
		return host;
	}

	public int[] getClientPorts() {
		return clientPorts;
	}

	public void process(byte[] message) {
		LittleEndianByteArrayReader packet = new LittleEndianByteArrayReader(message);
		if (pp != null) {
			pp.process(packet);
		} else {
			String error;

			if (packet.available() >= 4 && packet.readByte() == RemoteCenterOps.AUTH) {
				world = packet.readByte();
				if (packet.readLengthPrefixedString().equals(interServerPwd)) {
					if (!CenterServer.getInstance().isServerConnected(world)) {
						pp = RemoteCenterPacketProcessor.getProcessor(this, world);
						error = null;
					} else {
						LOG.log(Level.FINE, "Duplicate {0} server from {1} is trying to connect -> Disconnecting.", new Object[] { ServerType.getName(world), getRemoteEndpoint() });
						error = ServerType.getName(world) + " server already connected.";
					}
				} else {
					LOG.log(Level.FINE, "{0} server from {1} did not supply the correct auth password -> Disconnecting.", new Object[] { ServerType.getName(world), getRemoteEndpoint() });
					error = "Wrong auth password.";
				}
			} else {
				LOG.log(Level.FINE, "Expected auth packet from remote server at {0} -> Disconnecting.", getRemoteEndpoint());
				error = "Invalid auth packet.";
			}

			LittleEndianByteArrayWriter response = new LittleEndianByteArrayWriter(error == null ? 3 : 3 + error.length());
			response.writeByte(CenterRemoteOps.AUTH_RESPONSE);
			response.writeLengthPrefixedString(error == null ? "" : error);
			send(response.getBytes());
			if (error != null) {
				world = ServerType.UNDEFINED;
				close();
				return;
			}
		}
	}

	public void send(byte[] b) {
		ch.write(b);
	}

	public void setWorld(byte world) {
		this.world = world;
	}

	/**
	 * Notify the CenterServer that we are going to disconnect from the remote
	 * server.
	 * DO NOT USE THIS METHOD TO FORCE CLOSE THE CONNECTION. USE close()
	 * INSTEAD.
	 */
	public void disconnect() {
		if (world != ServerType.UNDEFINED)
			CenterServer.getInstance().serverDisconnected(world);
	}

	public void close() {
		ch.disconnect();
	}
}
