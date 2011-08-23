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

import argonms.common.GlobalConstants;
import argonms.common.LocalServer;
import argonms.common.ServerType;
import argonms.common.loading.DataFileType;
import argonms.common.loading.item.ItemDataLoader;
import argonms.common.loading.string.StringDataLoader;
import argonms.common.net.external.MapleAesOfb;
import argonms.common.net.internal.RemoteCenterSession;
import argonms.common.util.DatabaseManager;
import argonms.common.util.DatabaseManager.DatabaseType;
import argonms.common.util.Scheduler;
import argonms.game.loading.map.MapDataLoader;
import argonms.game.loading.mob.MobDataLoader;
import argonms.game.loading.npc.NpcDataLoader;
import argonms.game.loading.quest.QuestDataLoader;
import argonms.game.loading.reactor.ReactorDataLoader;
import argonms.game.loading.shop.NpcShopDataLoader;
import argonms.game.loading.skill.SkillDataLoader;
import argonms.game.net.WorldChannel;
import argonms.game.net.internal.GameCenterInterface;
import argonms.game.script.NpcScriptManager;
import argonms.game.script.PortalScriptManager;
import argonms.game.script.ReactorScriptManager;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
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
	private final byte serverId;
	private byte world;
	private String address;
	private boolean preloadAll;
	private DataFileType wzType;
	private String wzPath, scriptsPath;
	private boolean useNio;
	private boolean centerConnected;
	private final GameRegistry registry;
	private final Map<Byte, Set<Byte>> remoteGameChannelMapping;

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

			registry.setExpRate(Short.parseShort(prop.getProperty("argonms.game." + serverId + ".exprate")));
			registry.setMesoRate(Short.parseShort(prop.getProperty("argonms.game." + serverId + ".mesorate")));
			registry.setDropRate(Short.parseShort(prop.getProperty("argonms.game." + serverId + ".droprate")));
			registry.setItemsWillExpire(Boolean.parseBoolean(prop.getProperty("argonms.game." + serverId + ".itemexpire")));
			registry.setBuffsWillCooldown(Boolean.parseBoolean(prop.getProperty("argonms.game." + serverId + ".enablecooltime")));
			registry.setMultiLevel(Boolean.parseBoolean(prop.getProperty("argonms.game." + serverId + ".enablemultilevel")));
		} catch (IOException ex) {
			LOG.log(Level.SEVERE, "Could not load game" + serverId + " server properties!", ex);
			System.exit(2);
			return;
		}
		boolean mcdb = (wzType == DataFileType.MCDB);
		prop = new Properties();
		try {
			FileReader fr = new FileReader(System.getProperty("argonms.db.config.file", "db.properties"));
			prop.load(fr);
			fr.close();
			DatabaseManager.setProps(prop, mcdb, useNio);
		} catch (IOException ex) {
			LOG.log(Level.SEVERE, "Could not load database properties!", ex);
			System.exit(3);
			return;
		} catch (SQLException ex) {
			LOG.log(Level.SEVERE, "Could not initialize database!", ex);
			System.exit(3);
			return;
		}
		try {
			DatabaseManager.cleanup(DatabaseType.STATE, null, null, DatabaseManager.getConnection(DatabaseType.STATE));
			if (mcdb) {
				Connection con = null;
				PreparedStatement ps = null;
				ResultSet rs = null;
				try {
					con = DatabaseManager.getConnection(DatabaseType.WZ);
					ps = con.prepareStatement("SELECT `version`,`subversion`,`maple_version` FROM `mcdb_info`");
					rs = ps.executeQuery();
					if (rs.next()) {
						int realVersion = rs.getInt(1);
						int realSubversion = rs.getInt(2);
						int realGameVersion = rs.getInt(3);
						if (realVersion != GlobalConstants.MCDB_VERSION || realSubversion != GlobalConstants.MCDB_SUBVERSION) {
							LOG.log(Level.SEVERE, "MCDB version imcompatible. Expected: {0}.{1} Have: {2}.{3}", new Object[] { GlobalConstants.MCDB_VERSION, GlobalConstants.MCDB_SUBVERSION, realVersion, realSubversion });
							System.exit(3);
							return;
						}
						if (realGameVersion != GlobalConstants.MAPLE_VERSION) //carry on despite the warning...
							LOG.log(Level.WARNING, "Your copy of MCDB is based on an incongruent version of the WZ files. ArgonMS: {0} MCDB: {1}", new Object[] { GlobalConstants.MAPLE_VERSION, realGameVersion });
					}
				} finally {
					DatabaseManager.cleanup(DatabaseType.WZ, rs, ps, con);
				}
			}
		} catch (SQLException e) {
			LOG.log(Level.SEVERE, "Could not connect to database!", e);
			System.exit(3);
			return;
		}
		wzPath = System.getProperty("argonms.data.dir");
		scriptsPath = System.getProperty("argonms.scripts.dir");

		try {
			MapleAesOfb.testCipher();
		} catch (Exception ex) {
			LOG.log(Level.SEVERE, "Error initalizing the encryption cipher.  Make sure you're using the Unlimited Strength cryptography jar files.", ex);
			System.exit(6);
			return;
		}
		Scheduler.enable(true, true);

		gci = new GameCenterInterface(serverId, world, this);
		RemoteCenterSession<GameCenterInterface> session = RemoteCenterSession.connect(centerIp, centerPort, authKey, gci);
		if (session != null) {
			session.awaitClose();
			LOG.log(Level.SEVERE, "Lost connection with center server!");
		}
		System.exit(4); //connection with center server lost before we were able to shutdown
	}

	private void initializeData(boolean preloadAll, DataFileType wzType, String wzPath) {
		StringDataLoader.setInstance(wzType, wzPath);
		QuestDataLoader.setInstance(wzType, wzPath);
		SkillDataLoader.setInstance(wzType, wzPath);
		ReactorDataLoader.setInstance(wzType, wzPath);
		MobDataLoader.setInstance(wzType, wzPath);
		ItemDataLoader.setInstance(wzType, wzPath);
		MapDataLoader.setInstance(wzType, wzPath);
		NpcShopDataLoader.setInstance(wzType, wzPath);
		NpcDataLoader.setInstance(wzType, wzPath);
		NpcScriptManager.setInstance(scriptsPath);
		PortalScriptManager.setInstance(scriptsPath);
		ReactorScriptManager.setInstance(scriptsPath);
		long start, end;
		start = System.nanoTime();
		System.out.print("Loading String data...");
		StringDataLoader.getInstance().loadAll();
		System.out.println("\tDone!");
		System.out.print("Loading Quest data...");
		QuestDataLoader.getInstance().loadAll();
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
			System.out.print("Loading Storage data...");
			NpcDataLoader.getInstance().loadAll();
			System.out.println("\tDone!");
		}
		end = System.nanoTime();
		System.out.println("Preloaded data in " + ((end - start) / 1000000.0) + "ms.");
	}

	@Override
	public void centerConnected() {
		LOG.log(Level.FINE, "Link with Center server established.");
		centerConnected = true;
		initializeData(preloadAll, wzType, wzPath);
		boolean doingWork = false;
		for (WorldChannel ch : channels.values()) {
			ch.listen(useNio);
			if (ch.getPort() != -1)
				doingWork = true;
		}
		if (doingWork)
			gci.serverReady();
		else
			System.exit(5);
	}

	@Override
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

	@Override
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
