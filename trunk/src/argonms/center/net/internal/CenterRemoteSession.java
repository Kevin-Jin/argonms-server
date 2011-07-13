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

package argonms.center.net.internal;

import argonms.center.CenterServer;
import argonms.common.ServerType;
import argonms.common.net.internal.CenterRemoteOps;
import argonms.common.net.internal.RemoteCenterOps;
import argonms.common.tools.input.LittleEndianByteArrayReader;
import argonms.common.tools.output.LittleEndianByteArrayWriter;
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
	private Channel commChn;
	private CenterRemoteInterface cri;

	public CenterRemoteSession(Channel channel, String authKey) {
		this.commChn = channel;
		this.interServerPwd = authKey;
	}

	public SocketAddress getAddress() {
		return commChn.getRemoteAddress();
	}

	public void process(byte[] message) {
		LittleEndianByteArrayReader packet = new LittleEndianByteArrayReader(message);
		if (cri != null) {
			cri.getPacketProcessor().process(packet);
		} else {
			String error;

			if (packet.available() >= 4 && packet.readByte() == RemoteCenterOps.AUTH) {
				byte serverId = packet.readByte();
				if (packet.readLengthPrefixedString().equals(interServerPwd)) {
					if (!CenterServer.getInstance().isServerConnected(serverId)) {
						cri = CenterRemoteInterface.makeByServerId(serverId, this);
						cri.makePacketProcessor(); //don't leak Interface reference in its constructor
						error = null;
						if (ServerType.isGame(serverId)) {
							byte world = packet.readByte();
							byte[] channels = new byte[packet.readByte()];
							for (int i = 0; i < channels.length; i++)
								channels[i] = packet.readByte();
							List<CenterGameInterface> servers = CenterServer.getInstance().getAllServersOfWorld(world, serverId);
							List<Byte> conflicts = new ArrayList<Byte>();
							for (CenterGameInterface server : servers) {
								if (!server.isShuttingDown()) {
									for (int i = 0; i < channels.length; i++) {
										Byte ch = Byte.valueOf(channels[i]);
										if (server.getChannels().contains(ch))
											conflicts.add(ch);
									}
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
				cri = null;
				close();
				return;
			}
		}
	}

	public void send(byte[] b) {
		commChn.write(b);
	}

	/**
	 * DO NOT USE THIS METHOD TO FORCE CLOSE THE CONNECTION. USE close()
	 * INSTEAD.
	 */
	public void disconnected() {
		if (cri != null)
			cri.disconnected();
	}

	public void close() {
		commChn.disconnect();
	}
}
