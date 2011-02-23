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

import argonms.LocalServer;
import argonms.ServerType;
import argonms.loading.DataFileType;
import argonms.loading.item.ItemDataLoader;
import argonms.net.client.ClientListener;
import argonms.tools.DatabaseConnection;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
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

	private ClientListener handler;
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
		} catch (IOException ex) {
			LOG.log(Level.SEVERE, "Could not load login server properties!", ex);
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
		lci = new LoginCenterInterface(authKey, this);
		lci.connect(centerIp, centerPort);
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

	public void centerConnected() {
		LOG.log(Level.FINE, "Link with Center server established.");
		centerConnected = true;
		initializeData(preloadAll, wzType, wzPath);
		handler = new ClientListener(ServerType.LOGIN, (byte) -1, useNio);
		if (handler.bind(port)) {
			LOG.log(Level.INFO, "Login Server is online.");
			lci.serverReady();
		}
	}

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

	public void changePopulation(byte world, byte channel, boolean increase) {
		if (increase)
			onlineWorlds.get(Byte.valueOf(world)).incrementLoad(channel);
		else
			onlineWorlds.get(Byte.valueOf(world)).decrementLoad(channel);
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
