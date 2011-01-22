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

/**
 *
 * @author GoldenKevin
 */
public class World {
	private static final String[] names = { "Scania", "Bera", "Broa", "Windia", "Khaini", "Bellocan", "Mardia", "Kradia", "Yellonde", "Demethos", "Elnido", "Kastia", "Judis", "Arkenia", "Plana" };

	private String name;
	private String host;
	private int[] channelPorts;
	private short[] loads;
	private byte flag;
	private String eventMessage;

	public World(String name, String host, int[] ports) {
		this.name = name;
		this.host = host;
		this.channelPorts = ports;
		this.loads = new short[channelPorts.length];
		this.eventMessage = "";
	}

	public World(byte worldId, String host, int[] ports) {
		this(names[worldId], host, ports);
	}

	public void incrementLoad(byte channel) {
		loads[channel]++;
	}

	public void decrementLoad(byte channel) {
		loads[channel]--;
	}

	public String getName() {
		return name;
	}

	public String getHost() {
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
