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

package argonms.game;

import argonms.character.Player;
import argonms.map.GameMap;
import argonms.map.MapFactory;
import argonms.net.client.ClientListener;
import argonms.net.client.PlayerLog;
import argonms.net.server.RemoteCenterOps;
import argonms.tools.Timer;
import argonms.tools.output.LittleEndianByteArrayWriter;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author GoldenKevin
 */
public class WorldChannel {
	private static final Logger LOG = Logger.getLogger(WorldChannel.class.getName());

	private ClientListener handler;
	private byte world, channel;
	private int port;
	private MapFactory mapFactory;
	private PlayerLog storage;
	private InterChannelCommunication worldComm;

	protected WorldChannel(byte world, byte channel, int port) {
		this.world = world;
		this.channel = channel;
		this.port = port;
		this.mapFactory = new MapFactory();
		this.storage = new PlayerLog();
	}

	public void listen(boolean useNio) {
		Timer.getInstance().runRepeatedly(new Runnable() {
			public void run() {
				for (GameMap map : mapFactory.getMaps().values())
					map.respawnMobs();
			}
		}, 0, 10000);
		handler = new ClientListener(world, channel, useNio);
		if (handler.bind(port))
			LOG.log(Level.INFO, "Channel {0} is online.", channel);
		else
			shutdown();
	}

	public void addPlayer(Player p) {
		storage.addPlayer(p);
		sendNewLoad(storage.getConnectedCount());
	}

	public void removePlayer(Player p) {
		storage.deletePlayer(p);
		sendNewLoad(storage.getConnectedCount());
	}

	public Player getPlayerById(int characterid) {
		return storage.getPlayer(characterid);
	}

	public Player getPlayerByName(String name) {
		return storage.getPlayer(name);
	}

	public boolean isPlayerConnected(int characterid) {
		return storage.getPlayer(characterid) != null;
	}

	private void sendNewLoad(short now) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(3);
		lew.writeByte(RemoteCenterOps.POPULATION_CHANGED);
		lew.writeByte(channel);
		lew.writeShort(now);
		GameServer.getInstance().getCenterInterface().send(lew.getBytes());
	}

	public void startup(int port) {
		if (port == -1) {
			this.port = port;
			if (handler.bind(port)) {
				LOG.log(Level.INFO, "Channel {0} is online.", channel);
				sendNewPort();
			}
		}
	}

	public void shutdown() {
		port = -1;
		sendNewPort();
	}

	private void sendNewPort() {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(4);
		lew.writeByte(RemoteCenterOps.MODIFY_CHANNEL_PORT);
		lew.writeByte(world);
		lew.writeByte(channel);
		lew.writeInt(port);
		GameServer.getInstance().getCenterInterface().send(lew.getBytes());
	}

	public int getPort() {
		return port;
	}

	public MapFactory getMapFactory() {
		return mapFactory;
	}

	public void createWorldComm(byte[] local) {
		worldComm = new InterChannelCommunication(local, this);
	}

	public InterChannelCommunication getInterChannelInterface() {
		return worldComm;
	}
}
