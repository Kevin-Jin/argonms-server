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

package argonms.login.net;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

/**
 *
 * @author GoldenKevin
 */
public class LoginWorld {
	private static final String[] names = { "Scania", "Bera", "Broa", "Windia", "Khaini", "Bellocan", "Mardia", "Kradia", "Yellonde", "Demethos", "Elnido", "Kastia", "Judis", "Arkenia", "Plana" };

	private String name;
	private Map<Byte, byte[]> hosts;
	private Map<Byte, Integer> channelPorts;
	private TreeMap<Byte, Load> loads; //for convenience methods - not like I really need it sorted...
	private Map<Byte, Set<Byte>> gameToChannelMapping;
	private byte flag;
	private String eventMessage;

	public LoginWorld(String name) {
		this.name = name;
		this.hosts = new HashMap<Byte, byte[]>();
		this.channelPorts = new HashMap<Byte, Integer>();
		this.loads = new TreeMap<Byte, Load>();
		this.gameToChannelMapping = new HashMap<Byte, Set<Byte>>();
	}

	public LoginWorld(int worldId) {
		this(names[worldId]);
	}

	public void addGameServer(byte[] ip, Map<Byte, Integer> ports, byte serverId) {
		for (Entry<Byte, Integer> entry : ports.entrySet()) {
			Byte oCh = entry.getKey();
			hosts.put(oCh, ip);
			channelPorts.put(oCh, entry.getValue());
			loads.put(oCh, new Load());
		}
		gameToChannelMapping.put(Byte.valueOf(serverId), ports.keySet());
	}

	public void removeGameServer(byte serverId) {
		Byte oSi = Byte.valueOf(serverId);
		for (Byte ch : gameToChannelMapping.get(oSi)) {
			hosts.remove(ch);
			channelPorts.remove(ch);
			loads.remove(ch);
		}
		gameToChannelMapping.remove(oSi);
	}

	public void setPopulation(byte channel, short now) {
		loads.get(Byte.valueOf(channel)).set(now);
	}

	public int getTotalLoad() {
		int sum = 0;
		for (Load l : loads.values())
			sum += l.get();
		return sum;
	}

	public byte getLeastPopulatedChannel() {
		byte ch = loads.firstKey().byteValue();
		short least = loads.get(Byte.valueOf(ch)).get();
		for (Entry<Byte, Load> entry : loads.entrySet()) {
			byte curCh = entry.getKey().byteValue();
			short curLoad = entry.getValue().get();
			if (getPort(curCh) != -1 && curLoad < least) {
				ch = curCh;
				least = curLoad;
			}
		}
		return ch;
	}

	public int getChannelCount() {
		//channelPort.size() and loads.size() work just fine too.
		return hosts.size();
	}

	public byte getLargestNumberedChannel() {
		return loads.lastKey().byteValue();
	}

	public String getName() {
		return name;
	}

	public byte[] getHost(byte channel) {
		return hosts.get(Byte.valueOf(channel));
	}

	public int getPort(byte channel) {
		Integer p = channelPorts.get(Byte.valueOf(channel));
		return p != null ? p.intValue() : -1;
	}

	public void setPort(byte channel, int newPort) {
		channelPorts.put(Byte.valueOf(channel), Integer.valueOf(newPort));
	}

	public byte getFlag() {
		return flag;
	}

	public String getMessage() {
		return eventMessage;
	}

	public short getLoad(byte channel) {
		Load l = loads.get(Byte.valueOf(channel));
		return l != null ? l.get() : -1;
	}

	private static class Load {
		private short value;

		public Load() {
			this.value = 0;
		}

		public void set(short now) {
			this.value = now;
		}

		public short get() {
			return value;
		}
	}
}
