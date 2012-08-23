/*
 * ArgonMS MapleStory server emulator written in Java
 * Copyright (C) 2011-2012  GoldenKevin
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

package argonms.shop;

import argonms.common.GlobalConstants;
import argonms.common.LocalServer;
import argonms.common.ServerType;
import argonms.common.loading.DataFileType;
import argonms.common.loading.item.ItemDataLoader;
import argonms.common.loading.string.StringDataLoader;
import argonms.common.net.external.ClientListener;
import argonms.common.net.external.ClientListener.ClientFactory;
import argonms.common.net.external.PlayerLog;
import argonms.common.net.internal.RemoteCenterSession;
import argonms.common.util.DatabaseManager;
import argonms.common.util.DatabaseManager.DatabaseType;
import argonms.common.util.Scheduler;
import argonms.common.util.TimeTool;
import argonms.shop.character.ShopCharacter;
import argonms.shop.net.ShopWorld;
import argonms.shop.net.external.ClientShopPacketProcessor;
import argonms.shop.net.external.ShopClient;
import argonms.shop.net.internal.ShopCenterInterface;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author GoldenKevin
 */
public class ShopServer implements LocalServer {
	private static final Logger LOG = Logger.getLogger(ShopServer.class.getName());

	private static ShopServer instance;

	private ClientListener<ShopClient> handler;
	private ShopCenterInterface sci;
	private String address;
	private int port;
	private final Map<Byte, ShopWorld> onlineWorlds;
	private boolean preloadAll;
	private DataFileType wzType;
	private String wzPath;
	private boolean useNio;
	private boolean centerConnected;
	private final PlayerLog<ShopCharacter> storage;

	private ShopServer() {
		onlineWorlds = new HashMap<Byte, ShopWorld>();
		storage = new PlayerLog<ShopCharacter>();
	}

	public void init() {
		Properties prop = new Properties();
		String centerIp;
		int centerPort;
		String authKey;
		TimeZone tz;
		try {
			FileReader fr = new FileReader(System.getProperty("argonms.shop.config.file", "shop.properties"));
			prop.load(fr);
			fr.close();
			address = prop.getProperty("argonms.shop.host");
			port = Integer.parseInt(prop.getProperty("argonms.shop.port"));
			wzType = DataFileType.valueOf(prop.getProperty("argonms.shop.data.type"));
			//wzPath = prop.getProperty("argonms.shop.data.dir");
			preloadAll = Boolean.parseBoolean(prop.getProperty("argonms.shop.data.preload"));
			centerIp = prop.getProperty("argonms.shop.center.ip");
			centerPort = Integer.parseInt(prop.getProperty("argonms.shop.center.port"));
			authKey = prop.getProperty("argonms.shop.auth.key");
			useNio = Boolean.parseBoolean(prop.getProperty("argonms.shop.usenio"));

			String temp = prop.getProperty("argonms.shop.tz");
			tz = temp.isEmpty() ? TimeZone.getDefault() : TimeZone.getTimeZone(temp);
		} catch (IOException ex) {
			LOG.log(Level.SEVERE, "Could not load shop server properties!", ex);
			System.exit(2);
			return;
		}
		wzPath = System.getProperty("argonms.data.dir");

		handler = new ClientListener<ShopClient>(new ClientShopPacketProcessor(), new ClientFactory<ShopClient>() {
			@Override
			public ShopClient newInstance() {
				return new ShopClient();
			}
		});

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

		Scheduler.enable(true, true);
		TimeTool.setInstance(tz);

		sci = new ShopCenterInterface(this);
		RemoteCenterSession<ShopCenterInterface> session = RemoteCenterSession.connect(centerIp, centerPort, authKey, sci);
		if (session != null) {
			session.awaitClose();
			LOG.log(Level.SEVERE, "Lost connection with center server!");
		}
		System.exit(4); //connection with center server lost before we were able to shutdown
	}

	private void initializeData(boolean preloadAll, DataFileType wzType, String wzPath) {
		StringDataLoader.setInstance(wzType, wzPath);
		ItemDataLoader.setInstance(wzType, wzPath);
		long start, end;
		start = System.nanoTime();
		System.out.print("Loading String data...");
		StringDataLoader.getInstance().loadAll();
		System.out.println("\tDone!");
		if (preloadAll) {
			System.out.print("Loading Item data...");
			ItemDataLoader.getInstance().loadAll();
			System.out.println("\tDone!");
		}
		end = System.nanoTime();
		System.out.println("Preloaded data in " + ((end - start) / 1000000.0) + "ms.");
	}

	@Override
	public void registerCenter() {
		LOG.log(Level.INFO, "Center server registered.");
		centerConnected = true;
		new Thread(new Runnable() {
			@Override
			public void run() {
				initializeData(preloadAll, wzType, wzPath);
				if (handler.bind(port)) {
					LOG.log(Level.INFO, "Shop Server is online.");
					sci.serverReady();
				} else {
					System.exit(5);
				}
			}
		}, "data-preloader-thread").start();
	}

	@Override
	public void unregisterCenter() {
		if (centerConnected) {
			LOG.log(Level.INFO, "Center server unregistered.");
			centerConnected = false;
		}
	}

	public void registerGame(byte serverId, byte world, String host, Map<Byte, Integer> ports) {
		try {
			byte[] ip = InetAddress.getByName(host).getAddress();
			ShopWorld w = onlineWorlds.get(Byte.valueOf(world));
			if (w == null) {
				w = new ShopWorld();
				onlineWorlds.put(Byte.valueOf(world), w);
			}
			w.addGameServer(ip, ports, serverId);
			LOG.log(Level.INFO, "{0} server registered as {1}.", new Object[] { ServerType.getName(serverId), host });
		} catch (UnknownHostException e) {
			LOG.log(Level.INFO, "Could not accept " + ServerType.getName(serverId)
					+ " server because its address could not be resolved!", e);
		}
	}

	public void unregisterGame(byte serverId, byte world) {
		LOG.log(Level.INFO, "{0} server unregistered.", ServerType.getName(serverId));
		Byte oW = Byte.valueOf(world);
		ShopWorld w = onlineWorlds.get(oW);
		w.removeGameServer(serverId);
		if (w.getChannelCount() == 0)
			onlineWorlds.remove(oW);
	}

	/**
	 * This may return null if no channels of this world have connected yet.
	 * @param world
	 * @return
	 */
	public ShopWorld getWorld(byte world) {
		return onlineWorlds.get(Byte.valueOf(world));
	}

	public static ShopServer getInstance() {
		return instance;
	}

	@Override
	public String getExternalIp() {
		return address;
	}

	public int getClientPort() {
		return port;
	}

	public void addPlayer(ShopCharacter p) {
		storage.addPlayer(p);
	}

	public void removePlayer(ShopCharacter p) {
		storage.deletePlayer(p);
	}

	public ShopCharacter getPlayerById(int characterid) {
		return storage.getPlayer(characterid);
	}

	public ShopCharacter getPlayerByName(String name) {
		return storage.getPlayer(name);
	}

	public static void main(String[] args) {
		instance = new ShopServer();
		instance.init();
	}
}
