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
import java.util.Map;
import java.util.Set;

/**
 *
 * @author GoldenKevin
 */
public class CenterGameInterface extends CenterRemoteInterface {
	private String host;
	private byte world;
	private Map<Byte, Integer> clientPorts;
	private byte serverId;
	private GameCenterPacketProcessor pp;

	public CenterGameInterface(CenterRemoteSession session, byte serverId) {
		super(session);
		this.serverId = serverId;
	}

	public void makePacketProcessor() {
		this.pp = new GameCenterPacketProcessor(this);
	}

	public RemoteCenterPacketProcessor getPacketProcessor() {
		return pp;
	}

	public byte getServerId() {
		return serverId;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public void setWorld(byte world) {
		this.world = world;
	}

	public byte getWorld() {
		return world;
	}

	public void setClientPorts(Map<Byte, Integer> clientPorts) {
		this.clientPorts = clientPorts;
	}

	public Set<Byte> getChannels() {
		return clientPorts.keySet();
	}

	public String getHost() {
		return host;
	}

	public Map<Byte, Integer> getClientPorts() {
		return clientPorts;
	}

	public void disconnected() {
		if (online) {
			online = false;
			disconnecting = true;
			CenterServer.getInstance().gameDisconnected(this);
		}
	}
}
