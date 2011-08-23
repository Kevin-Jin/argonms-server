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

import argonms.common.GlobalConstants;
import argonms.common.LocalServer;
import argonms.common.ServerType;
import argonms.common.loading.DataFileType;
import argonms.common.loading.item.ItemDataLoader;
import argonms.common.net.external.ClientListener;
import argonms.common.net.external.ClientListener.ClientFactory;
import argonms.common.net.external.MapleAesOfb;
import argonms.common.net.internal.RemoteCenterSession;
import argonms.common.util.DatabaseManager;
import argonms.common.util.DatabaseManager.DatabaseType;
import argonms.common.util.Scheduler;
import argonms.login.net.LoginWorld;
import argonms.login.net.external.ClientLoginPacketProcessor;
import argonms.login.net.external.LoginClient;
import argonms.login.net.internal.LoginCenterInterface;
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
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author GoldenKevin
 */
public class LoginServer implements LocalServer {
	private static final Logger LOG = Logger.getLogger(LoginServer.class.getName());

	private static LoginServer instance;

	private ClientListener<LoginClient> handler;
	private LoginCenterInterface lci;
	private String address;
	private int port;
	private boolean usePin;
	private List<Message> messages;
	private Map<Byte, LoginWorld> onlineWorlds;
	private boolean preloadAll;
	private DataFileType wzType;
	private String wzPath;
	private boolean useNio;
	private int rankingPeriod;
	private boolean centerConnected;

	private LoginServer() {
		messages = new ArrayList<Message>();
		onlineWorlds = new HashMap<Byte, LoginWorld>();
	}

	public void init() {
		Properties prop = new Properties();
		String centerIp;
		int centerPort;
		String authKey;
		try {
			FileReader fr = new FileReader(System.getProperty("argonms.login.config.file", "login.properties"));
			prop.load(fr);
			fr.close();
			address = prop.getProperty("argonms.login.host");
			port = Integer.parseInt(prop.getProperty("argonms.login.port"));
			usePin = Boolean.parseBoolean(prop.getProperty("argonms.login.pin"));
			wzType = DataFileType.valueOf(prop.getProperty("argonms.login.data.type"));
			//wzPath = prop.getProperty("argonms.login.data.dir");
			preloadAll = Boolean.parseBoolean(prop.getProperty("argonms.login.data.preload"));
			centerIp = prop.getProperty("argonms.login.center.ip");
			centerPort = Integer.parseInt(prop.getProperty("argonms.login.center.port"));
			authKey = prop.getProperty("argonms.login.auth.key");
			useNio = Boolean.parseBoolean(prop.getProperty("argonms.login.usenio"));
			rankingPeriod = Integer.parseInt(prop.getProperty("argonms.login.ranking.frequency"));
		} catch (IOException ex) {
			LOG.log(Level.SEVERE, "Could not load login server properties!", ex);
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

		try {
			MapleAesOfb.testCipher();
		} catch (Exception ex) {
			LOG.log(Level.SEVERE, "Error initalizing the encryption cipher.  Make sure you're using the Unlimited Strength cryptography jar files.", ex);
			System.exit(6);
			return;
		}
		Scheduler.enable(true, true);

		lci = new LoginCenterInterface(this);
		RemoteCenterSession<LoginCenterInterface> session = RemoteCenterSession.connect(centerIp, centerPort, authKey, lci);
		if (session != null) {
			session.awaitClose();
			LOG.log(Level.SEVERE, "Lost connection with center server!");
		}
		System.exit(4); //connection with center server lost before we were able to shutdown
	}

	private void initializeData(boolean preloadAll, DataFileType wzType, String wzPath) {
		ItemDataLoader.setInstance(wzType, wzPath);
		if (preloadAll) {
			long start, end;
			start = System.nanoTime();
			System.out.print("Loading Item data...");
			ItemDataLoader.getInstance().loadAll();
			System.out.println("\tDone!");
			end = System.nanoTime();
			System.out.println("Preloaded data in " + ((end - start) / 1000000.0) + "ms.");
		}
	}

	@Override
	public void centerConnected() {
		LOG.log(Level.FINE, "Link with Center server established.");
		centerConnected = true;
		initializeData(preloadAll, wzType, wzPath);
		handler = new ClientListener<LoginClient>(ServerType.LOGIN, (byte) -1, new ClientLoginPacketProcessor(), new ClientFactory<LoginClient>() {
			@Override
			public LoginClient newInstance(byte world, byte channel) {
				return new LoginClient();
			}
		});
		if (handler.bind(port)) {
			LOG.log(Level.INFO, "Login Server is online.");
			lci.serverReady();
		} else {
			System.exit(5);
		}
		Scheduler.getInstance().runRepeatedly(new RankingWorker(), rankingPeriod, rankingPeriod);
	}

	@Override
	public void centerDisconnected() {
		if (centerConnected) {
			LOG.log(Level.SEVERE, "Center server disconnected.");
			centerConnected = false;
		}
	}

	public void gameConnected(byte serverId, byte world, String host, Map<Byte, Integer> ports) {
		try {
			byte[] ip = InetAddress.getByName(host).getAddress();
			LoginWorld w = onlineWorlds.get(Byte.valueOf(world));
			if (w == null) {
				w = new LoginWorld(world);
				onlineWorlds.put(Byte.valueOf(world), w);
			}
			w.addGameServer(ip, ports, serverId);
			LOG.log(Level.INFO, "{0} server accepted from {1}.", new Object[] { ServerType.getName(serverId), host });
		} catch (UnknownHostException e) {
			LOG.log(Level.INFO, "Could not accept " + ServerType.getName(world)
					+ " server because its address could not be resolved!", e);
		}
	}

	public void gameDisconnected(byte serverId, byte world) {
		LOG.log(Level.INFO, "{0} server disconnected.", ServerType.getName(serverId));
		Byte oW = Byte.valueOf(world);
		LoginWorld w = onlineWorlds.get(oW);
		w.removeGameServer(serverId);
		if (w.getChannelCount() == 0)
			onlineWorlds.remove(oW);
	}

	public void changePopulation(byte world, byte channel, short now) {
		onlineWorlds.get(Byte.valueOf(world)).setPopulation(channel, now);
	}

	public boolean usePin() {
		return usePin;
	}

	public Map<Byte, LoginWorld> getAllWorlds() {
		return Collections.unmodifiableMap(onlineWorlds);
	}

	/**
	 * This may return null if no channels of this world have connected yet.
	 * @param world
	 * @return
	 */
	public LoginWorld getWorld(byte world) {
		return onlineWorlds.get(Byte.valueOf(world));
	}

	public List<Message> getMessages() {
		return Collections.unmodifiableList(messages);
	}

	public static LoginServer getInstance() {
		return instance;
	}

	@Override
	public String getExternalIp() {
		return address;
	}

	public int getClientPort() {
		return port;
	}

	public static void main(String[] args) {
		instance = new LoginServer();
		instance.init();
	}
}
