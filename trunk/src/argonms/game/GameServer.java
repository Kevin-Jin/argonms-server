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
import argonms.ServerType;
import argonms.loading.DataFileType;
import argonms.loading.item.ItemDataLoader;
import argonms.loading.map.MapDataLoader;
import argonms.loading.mob.MobDataLoader;
import argonms.loading.reactor.ReactorDataLoader;
import argonms.loading.shop.NpcShopDataLoader;
import argonms.loading.skill.SkillDataLoader;
import argonms.loading.string.StringDataLoader;
import argonms.tools.DatabaseConnection;
import argonms.tools.Timer;

import java.io.FileReader;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author GoldenKevin
 */
public class GameServer implements LocalServer {
	private static final Logger LOG = Logger.getLogger(GameServer.class.getName());

	private static GameServer instance;

	private Map<Byte, WorldChannel> channels;
	private GameCenterInterface gci;
	private byte serverId;
	private byte world;
	private String address;
	private boolean preloadAll;
	private DataFileType wzType;
	private String wzPath;
	private boolean useNio;
	private boolean centerConnected;
	private GameRegistry registry;
	private Map<Byte, Set<Byte>> remoteGameChannelMapping;

	private GameServer(byte serverid) {
		this.serverId = serverid;
		this.registry = new GameRegistry();
		this.remoteGameChannelMapping = new HashMap<Byte, Set<Byte>>();
	}

	public byte getServerId() {
		return serverId;
	}

	public Map<Byte, WorldChannel> getChannels() {
		return Collections.unmodifiableMap(channels);
	}

	public void init() {
		Properties prop = new Properties();
		String centerIp;
		int centerPort;
		String authKey;
		try {
			FileReader fr = new FileReader(System.getProperty("argonms.game.config.file", "game" + serverId + ".properties"));
			prop.load(fr);
			fr.close();
			address = prop.getProperty("argonms.game." + serverId + ".host");
			wzType = DataFileType.valueOf(prop.getProperty("argonms.game." + serverId + ".data.type"));
			//wzPath = prop.getProperty("argonms.game." + serverId + ".data.dir");
			preloadAll = Boolean.parseBoolean(prop.getProperty("argonms.game." + serverId + ".data.preload"));
			world = Byte.parseByte(prop.getProperty("argonms.game." + serverId + ".world"));
			String[] chList = prop.getProperty("argonms.game." + serverId + ".channels").replaceAll("\\s", "").split(",");
			channels = new HashMap<Byte, WorldChannel>(chList.length);
			List<Byte> localChannels = new ArrayList<Byte>(channels.size());
			for (int i = 0; i < chList.length; i++) {
				byte chNum = Byte.parseByte(chList[i]);
				WorldChannel ch = new WorldChannel(world, chNum, Integer.parseInt(prop.getProperty("argonms.game." + serverId + ".channel." + chNum + ".port")));
				channels.put(Byte.valueOf(chNum), ch);
				localChannels.add(Byte.valueOf(chNum));
			}
			for (Entry<Byte, WorldChannel> entry : channels.entrySet()) {
				byte[] selfExcluded = new byte[localChannels.size() - 1];
				byte index = 0;
				for (Byte b : localChannels)
					if (!entry.getKey().equals(b))
						selfExcluded[index++] = b.byteValue();
				entry.getValue().createWorldComm(selfExcluded);
			}
			centerIp = prop.getProperty("argonms.game." + serverId + ".center.ip");
			centerPort = Integer.parseInt(prop.getProperty("argonms.game." + serverId + ".center.port"));
			authKey = prop.getProperty("argonms.game." + serverId + ".auth.key");
			useNio = Boolean.parseBoolean(prop.getProperty("argonms.game." + serverId + ".usenio"));
		} catch (IOException ex) {
			LOG.log(Level.SEVERE, "Could not load game" + serverId + " server properties!", ex);
			return;
		}
		prop = new Properties();
		try {
			FileReader fr = new FileReader(System.getProperty("argonms.db.config.file", "db.properties"));
			prop.load(fr);
			fr.close();
			DatabaseConnection.setProps(prop, wzType == DataFileType.MCDB);
		} catch (IOException ex) {
			LOG.log(Level.SEVERE, "Could not load database properties!", ex);
			return;
		}
		DatabaseConnection.getConnection();
		wzPath = System.getProperty("argonms.data.dir");
		gci = new GameCenterInterface(serverId, world, authKey, this);
		gci.connect(centerIp, centerPort);
	}

	private void initializeData(boolean preloadAll, DataFileType wzType, String wzPath) {
		StringDataLoader.setInstance(wzType, wzPath);
		SkillDataLoader.setInstance(wzType, wzPath);
		ReactorDataLoader.setInstance(wzType, wzPath);
		MobDataLoader.setInstance(wzType, wzPath);
		ItemDataLoader.setInstance(wzType, wzPath);
		MapDataLoader.setInstance(wzType, wzPath);
		NpcShopDataLoader.setInstance(wzType, wzPath);
		long start, end;
		start = System.nanoTime();
		System.out.print("Loading String data...");
		StringDataLoader.getInstance().loadAll();
		System.out.println("\tDone!");
		if (preloadAll) {
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
			System.out.print("Loading Shop data...");
			NpcShopDataLoader.getInstance().loadAll();
			System.out.println("\tDone!");
		}
		end = System.nanoTime();
		System.out.println("Preloaded data in " + ((end - start) / 1000000.0) + "ms.");
	}

	public void centerConnected() {
		LOG.log(Level.FINE, "Link with Center server established.");
		centerConnected = true;
		initializeData(preloadAll, wzType, wzPath);
		Timer.enable();
		for (WorldChannel ch : channels.values())
			ch.listen(useNio);
		gci.serverReady();
	}

	public void centerDisconnected() {
		if (centerConnected) {
			LOG.log(Level.SEVERE, "Center server disconnected.");
			centerConnected = false;
		}
	}

	public void gameConnected(byte serverId, String host, Map<Byte, Integer> ports) {
		try {
			byte[] ip = InetAddress.getByName(host).getAddress();
			remoteGameChannelMapping.put(Byte.valueOf(serverId), ports.keySet());
			for (WorldChannel ch : channels.values())
				ch.getInterChannelInterface().addRemoteChannels(ip, ports);
			LOG.log(Level.INFO, "{0} server accepted from {1}.", new Object[] { ServerType.getName(serverId), host });
		} catch (UnknownHostException e) {
			LOG.log(Level.INFO, "Could not accept " + ServerType.getName(world)
					+ " server because its address could not be resolved!", e);
		}
	}

	public void gameDisconnected(byte serverId) {
		LOG.log(Level.INFO, "{0} server disconnected.", ServerType.getName(serverId));
		Set<Byte> remove = remoteGameChannelMapping.remove(Byte.valueOf(serverId));
		for (WorldChannel ch : channels.values())
			ch.getInterChannelInterface().removeRemoteChannels(remove);
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

	public Map<Byte, Integer> getClientPorts() {
		Map<Byte, Integer> ports = new HashMap<Byte, Integer>(channels.size());
		for (Entry<Byte, WorldChannel> entry : channels.entrySet())
			ports.put(entry.getKey(), Integer.valueOf(entry.getValue().getPort()));
		return ports;
	}

	public GameCenterInterface getCenterInterface() {
		return gci;
	}

	public byte channelOfPlayer(int characterid) {
		for (Entry<Byte, WorldChannel> entry : channels.entrySet())
			if (entry.getValue().isPlayerConnected(characterid))
				return entry.getKey().byteValue();
		return -1;
	}

	public GameRegistry getRegistry() {
		return registry;
	}

	public static GameServer getInstance() {
		return instance;
	}

	public static WorldChannel getChannel(byte ch) {
		return instance.channels.get(Byte.valueOf(ch));
	}

	public static GameRegistry getVariables() {
		return instance.getRegistry();
	}

	public static void main(String[] args) {
		instance = new GameServer(Byte.parseByte(System.getProperty("argonms.game.serverid", "0")));
		instance.init();
	}
}
