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

package argonms.login;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 *
 * @author GoldenKevin
 */
public class World {
	private static final String[] names = { "Scania", "Bera", "Broa", "Windia", "Khaini", "Bellocan", "Mardia", "Kradia", "Yellonde", "Demethos", "Elnido", "Kastia", "Judis", "Arkenia", "Plana" };

	private String name;
	private byte[] host;
	private int[] channelPorts;
	private short[] loads;
	private byte flag;
	private String eventMessage;

	public World(String name, String host, int[] ports) throws UnknownHostException {
		this.name = name;
		this.host = InetAddress.getByName(host).getAddress();
		this.channelPorts = ports;
		this.loads = new short[channelPorts.length];
		this.eventMessage = "";
	}

	public World(byte worldId, String host, int[] ports) throws UnknownHostException {
		this(names[worldId], host, ports);
	}

	public void incrementLoad(byte channel) {
		loads[channel - 1]++;
	}

	public void decrementLoad(byte channel) {
		loads[channel - 1]--;
	}

	public String getName() {
		return name;
	}

	public byte[] getHost() {
		return host;
	}

	public int[] getPorts() {
		return channelPorts;
	}

	public byte getFlag() {
		return flag;
	}

	public String getMessage() {
		return eventMessage;
	}

	public short[] getLoads() {
		return loads;
	}
}
