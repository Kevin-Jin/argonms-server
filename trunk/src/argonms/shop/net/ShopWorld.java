/*
 * ArgonMS MapleStory server emulator written in Java
 * Copyright (C) 2011-2013  GoldenKevin
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

package argonms.shop.net;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 *
 * @author GoldenKevin
 */
public class ShopWorld {
	private final Map<Byte, byte[]> hosts;
	private final Map<Byte, Integer> channelPorts;
	private final Map<Byte, Set<Byte>> gameToChannelMapping;

	public ShopWorld() {
		this.hosts = new HashMap<Byte, byte[]>();
		this.channelPorts = new HashMap<Byte, Integer>();
		this.gameToChannelMapping = new HashMap<Byte, Set<Byte>>();
	}

	public void addGameServer(byte[] ip, Map<Byte, Integer> ports, byte serverId) {
		for (Entry<Byte, Integer> entry : ports.entrySet()) {
			Byte oCh = entry.getKey();
			hosts.put(oCh, ip);
			channelPorts.put(oCh, entry.getValue());
		}
		gameToChannelMapping.put(Byte.valueOf(serverId), ports.keySet());
	}

	public Set<Byte> removeGameServer(byte serverId) {
		Byte oSi = Byte.valueOf(serverId);
		for (Byte ch : gameToChannelMapping.get(oSi)) {
			hosts.remove(ch);
			channelPorts.remove(ch);
		}
		return gameToChannelMapping.remove(oSi);
	}

	public int getChannelCount() {
		return hosts.size(); //channelPort.size() and loads.size() works too.
	}

	public byte[] getHost(byte channel) {
		return hosts.get(Byte.valueOf(channel));
	}

	public int getPort(byte channel) {
		return channelPorts.get(Byte.valueOf(channel));
	}

	public void setPort(byte channel, int newPort) {
		channelPorts.put(Byte.valueOf(channel), Integer.valueOf(newPort));
	}
}
