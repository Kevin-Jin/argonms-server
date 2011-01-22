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

import argonms.LocalServer;
import argonms.loading.DataFileType;
import argonms.loading.item.ItemDataLoader;
import argonms.loading.map.MapDataLoader;
import argonms.loading.mob.MobDataLoader;
import argonms.loading.reactor.ReactorDataLoader;
import argonms.loading.skill.SkillDataLoader;
import argonms.loading.string.StringDataLoader;
import argonms.tools.DatabaseConnection;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author GoldenKevin
 */
public class GameServer implements LocalServer {
	private static final Logger LOG = Logger.getLogger(GameServer.class.getName());

	private static GameServer instance;

	private WorldChannel[] channels;
	private GameCenterInterface gci;
	private byte world;
	private String address;
	private boolean preloadAll;
	private DataFileType wzType;
	private String wzPath;
	private boolean useNio;
	private boolean centerConnected;

	private GameServer(byte world) {
		this.world = world;
	}

	public void init() {
		Properties prop = new Properties();
		try {
			FileReader fr = new FileReader(System.getProperty("argonms.db.config.file", "db.properties"));
			prop.load(fr);
			fr.close();
			DatabaseConnection.setProps(prop);
		} catch (IOException ex) {
			LOG.log(Level.SEVERE, "Could not load database properties!", ex);
			return;
		}
		DatabaseConnection.getConnection();
		prop = new Properties();
		String centerIp;
		int centerPort;
		String authKey;
		try {
			FileReader fr = new FileReader(System.getProperty("argonms.game.config.file", "game" + world + ".properties"));
			prop.load(fr);
			fr.close();
			address = prop.getProperty("argonms.game." + world + ".host");
			wzType = DataFileType.valueOf(prop.getProperty("argonms.game." + world + ".data.type"));
			//wzPath = prop.getProperty("argonms.game." + world + ".data.dir");
			preloadAll = Boolean.parseBoolean(prop.getProperty("argonms.game." + world + ".data.preload"));
			channels = new WorldChannel[Byte.parseByte(prop.getProperty("argonms.game." + world + ".channel.count"))];
			for (int i = 0; i < channels.length; i++)
				channels[i] = new WorldChannel(world, (byte) (i + 1), Integer.parseInt(prop.getProperty("argonms.game." + world + ".channel." + (i + 1) + ".port")));
			centerIp = prop.getProperty("argonms.game." + world + ".center.ip");
			centerPort = Integer.parseInt(prop.getProperty("argonms.game." + world + ".center.port"));
			authKey = prop.getProperty("argonms.game." + world + ".auth.key");
			useNio = Boolean.parseBoolean(prop.getProperty("argonms.game." + world + ".usenio"));
		} catch (IOException ex) {
			LOG.log(Level.SEVERE, "Could not load game" + world + " server properties!", ex);
			return;
		}
		wzPath = System.getProperty("argonms.data.dir");
		gci = new GameCenterInterface(world, authKey, this);
		gci.connect(centerIp, centerPort);
	}

	private void initializeData(boolean preloadAll, DataFileType wzType, String wzPath) {
		StringDataLoader.setInstance(wzType, wzPath);
		SkillDataLoader.setInstance(wzType, wzPath);
		ReactorDataLoader.setInstance(wzType, wzPath);
		MobDataLoader.setInstance(wzType, wzPath);
		ItemDataLoader.setInstance(wzType, wzPath);
		MapDataLoader.setInstance(wzType, wzPath);
		if (preloadAll) {
			long start, end;
			start = System.nanoTime();
			System.out.print("Loading String data...");
			StringDataLoader.getInstance().loadAll();
			System.out.println("\tDone!");
			System.out.print("Loading Skill data...");
			SkillDataLoader.getInstance().loadAll();
			System.out.println("\tDone!");
			System.out.print("Loading Reactor data...");
			ReactorDataLoader.getInstance().loadAll();
			System.out.println("\tDone!");
			System.out.print("Loading Mob data...");
			MobDataLoader.getInstance().loadAll();
			System.out.println("\tDone!");
			System.out.print("Loading Item data...");
			ItemDataLoader.getInstance().loadAll();
			System.out.println("\tDone!");
			System.out.print("Loading Map data...");
			MapDataLoader.getInstance().loadAll();
			System.out.println("\tDone!");
			end = System.nanoTime();
			System.out.println("Preloaded all data in " + ((end - start) / 1000000.0) + "ms.");
		}
	}

	public void centerConnected() {
		LOG.log(Level.FINE, "Link with Center server established.");
		centerConnected = true;
		initializeData(preloadAll, wzType, wzPath);
		for (byte i = 0; i < channels.length; i++)
			channels[i].listen(useNio);
		gci.serverReady();
	}

	public void centerDisconnected() {
		if (centerConnected) {
			LOG.log(Level.SEVERE, "Center server disconnected.");
			centerConnected = false;
		}
	}

	public void shopConnected(String host, int port) {
		LOG.log(Level.INFO, "Shop server accepted from {0}:{1}.", new Object[] { host, port });
	}

	public void shopDisconnected() {
		LOG.log(Level.INFO, "Shop server disconnected.");
	}

	public String getExternalIp() {
		return address;
	}

	public int[] getClientPorts() {
		int[] ports = new int[channels.length];
		for (int i = 0; i < ports.length; i++)
			ports[i] = channels[i].getPort();
		return ports;
	}

	public GameCenterInterface getCenterInterface() {
		return gci;
	}

	public static WorldChannel getChannel(byte c) {
		return getInstance().channels[c];
	}

	public static GameServer getInstance() {
		return instance;
	}

	public static void main(String[] args) {
		instance = new GameServer(Byte.parseByte(System.getProperty("argonms.game.world", "0")));
		instance.init();
	}
}
