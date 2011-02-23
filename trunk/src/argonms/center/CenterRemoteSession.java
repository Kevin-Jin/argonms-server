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

package argonms.center;

import argonms.center.recv.RemoteCenterPacketProcessor;
import argonms.center.send.CenterRemoteInterface;
import argonms.center.send.CenterGameInterface;
import argonms.ServerType;
import argonms.center.CenterServer;
import argonms.center.recv.RemoteCenterPacketProcessor;
import argonms.net.server.CenterRemoteOps;
import argonms.net.server.RemoteCenterOps;
import argonms.tools.input.LittleEndianByteArrayReader;
import argonms.tools.output.LittleEndianByteArrayWriter;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jboss.netty.channel.Channel;

/**
 *
 * @author GoldenKevin
 */
public class CenterRemoteSession {
	private static final Logger LOG = Logger.getLogger(CenterRemoteSession.class.getName());

	private String interServerPwd;
	private Channel ch;
	private RemoteCenterPacketProcessor pp;
	private CenterRemoteInterface cri;
	private byte serverId;

	public CenterRemoteSession(Channel channel, String authKey) {
		this.ch = channel;
		this.interServerPwd = authKey;
		this.serverId = ServerType.UNDEFINED;
	}

	public SocketAddress getAddress() {
		return ch.getRemoteAddress();
	}

	public void process(byte[] message) {
		LittleEndianByteArrayReader packet = new LittleEndianByteArrayReader(message);
		if (pp != null) {
			pp.process(packet);
		} else {
			String error;

			if (packet.available() >= 4 && packet.readByte() == RemoteCenterOps.AUTH) {
				serverId = packet.readByte();
				if (packet.readLengthPrefixedString().equals(interServerPwd)) {
					if (!CenterServer.getInstance().isServerConnected(serverId)) {
						cri = CenterRemoteInterface.getByServerId(serverId, this);
						pp = cri.createPacketProcessor();
						error = null;
						if (ServerType.isGame(serverId)) {
							byte world = packet.readByte();
							byte[] channels = new byte[packet.readByte()];
							for (int i = 0; i < channels.length; i++)
								channels[i] = packet.readByte();
							List<CenterGameInterface> servers = CenterServer.getInstance().getAllServersOfWorld(world, serverId);
							List<Byte> conflicts = new ArrayList<Byte>();
							for (CenterGameInterface server : servers) {
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
								LOG.log(Level.FINE, "{0} server is trying to add duplicate {2}", new Object[] { ServerType.getName(serverId), list });
								error = list + " already connected.";
							}
						}
					} else {
						LOG.log(Level.FINE, "Duplicate {0} server from {1} is trying to connect -> Disconnecting.", new Object[] { ServerType.getName(serverId), getAddress() });
						error = ServerType.getName(serverId) + " server already connected.";
					}
				} else {
					LOG.log(Level.FINE, "{0} server from {1} did not supply the correct auth password -> Disconnecting.", new Object[] { ServerType.getName(serverId), getAddress() });
					error = "Wrong auth password.";
				}
			} else {
				LOG.log(Level.FINE, "Expected auth packet from remote server at {0} -> Disconnecting.", getAddress());
				error = "Invalid auth packet.";
			}

			LittleEndianByteArrayWriter response = new LittleEndianByteArrayWriter(error == null ? 3 : 3 + error.length());
			response.writeByte(CenterRemoteOps.AUTH_RESPONSE);
			response.writeLengthPrefixedString(error == null ? "" : error);
			send(response.getBytes());
			if (error != null) {
				serverId = ServerType.UNDEFINED;
				cri = null;
				close();
				return;
			}
		}
	}

	public void send(byte[] b) {
		ch.write(b);
	}

	public void setServerId(byte serverId) {
		this.serverId = serverId;
	}

	/**
	 * Notify the CenterServer that we are going to disconnect from the remote
	 * server.
	 * DO NOT USE THIS METHOD TO FORCE CLOSE THE CONNECTION. USE close()
	 * INSTEAD.
	 */
	public void disconnect() {
		if (serverId != ServerType.UNDEFINED && cri != null)
			cri.disconnect();
	}

	public void close() {
		ch.disconnect();
	}
}
