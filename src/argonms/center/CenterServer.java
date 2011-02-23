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

package argonms.center;

import argonms.ServerType;
import argonms.net.client.RemoteClient;
import argonms.net.remoteadmin.TelnetListener;
import argonms.center.send.CenterGameInterface;
import argonms.center.send.CenterLoginInterface;
import argonms.center.send.CenterRemoteInterface;
import argonms.net.server.CenterRemoteOps;
import argonms.center.send.CenterShopInterface;
import argonms.center.recv.RemoteServerListener;
import argonms.tools.DatabaseConnection;
import argonms.tools.output.LittleEndianByteArrayWriter;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author GoldenKevin
 */
public class CenterServer {
	private static final Logger LOG = Logger.getLogger(CenterServer.class.getName());

	private static CenterServer instance;

	private RemoteServerListener listener;
	private CenterLoginInterface loginServer;
	private CenterShopInterface shopServer;
	private Map<Byte, CenterGameInterface> gameServers;

	private CenterServer() {
		gameServers = new HashMap<Byte, CenterGameInterface>();
	}

	public void init() {
		Properties prop = new Properties();
		try {
			FileReader fr = new FileReader(System.getProperty("argonms.db.config.file", "db.properties"));
			prop.load(fr);
			fr.close();
			DatabaseConnection.setProps(prop, false);
		} catch (IOException ex) {
			LOG.log(Level.WARNING, "Could not load database properties.", ex);
		}
		try {
			Connection con = DatabaseConnection.getConnection();
			PreparedStatement ps = con.prepareStatement("UPDATE `accounts` SET `connected` = ?");
			ps.setInt(1, RemoteClient.STATUS_NOTLOGGEDIN);
			ps.executeUpdate();
		} catch (SQLException ex) {
			LOG.log(Level.WARNING, "Could not reset logged in status of all accounts.", ex);
		}
		prop = new Properties();
		int port;
		String authKey;
		int telnetPort;
		boolean useNio;
		try {
			FileReader fr = new FileReader(System.getProperty("argonms.center.config.file", "center.properties"));
			prop.load(fr);
			fr.close();
			port = Integer.parseInt(prop.getProperty("argonms.center.port"));
			authKey = prop.getProperty("argonms.center.auth.key");
			telnetPort = Integer.parseInt(prop.getProperty("argonms.center.telnet"));
			useNio = Boolean.parseBoolean(prop.getProperty("argonms.center.usenio"));
		} catch (IOException ex) {
			LOG.log(Level.SEVERE, "Could not load center server properties!", ex);
			return;
		}
		listener = new RemoteServerListener(authKey, useNio);
		listener.bind(port);
		LOG.log(Level.INFO, "Center Server is online.");
		if (telnetPort != -1) {
			new TelnetListener(useNio).bind(telnetPort);
			LOG.log(Level.INFO, "Telnet Server online.");
		}
	}

	public List<CenterGameInterface> getAllServersOfWorld(byte world, byte exclusion) {
		List<CenterGameInterface> servers = new ArrayList<CenterGameInterface>();
		for (Entry<Byte, CenterGameInterface> entry : gameServers.entrySet()) {
			byte serverId = entry.getKey().byteValue();
			CenterGameInterface server = entry.getValue();
			if (serverId != exclusion && server.getWorld() == world)
				servers.add(server);
		}
		return servers;
	}

	public void loginConnected(CenterLoginInterface remote) {
		LOG.log(Level.INFO, "{0} server accepted from {1}.", new Object[] { ServerType.getName(ServerType.LOGIN), remote.getSession().getAddress() });
		loginServer = remote;
		sendConnectedGames(remote);
	}

	public void gameConnected(CenterGameInterface remote) {
		byte serverId = remote.getServerId();
		LOG.log(Level.INFO, "{0} server accepted from {1}.", new Object[] { ServerType.getName(serverId), remote.getSession().getAddress() });
		gameServers.put(Byte.valueOf(serverId), remote);
		notifyGameConnected(serverId, remote.getWorld(), remote.getHost(), remote.getClientPorts());
		sendConnectedShop(remote);
		sendConnectedGamesOfWorld(remote);
	}

	public void shopConnected(CenterShopInterface remote) {
		LOG.log(Level.INFO, "{0} server accepted from {1}.", new Object[] { ServerType.getName(ServerType.SHOP), remote.getSession().getAddress() });
		shopServer = remote;
		notifyShopConnected(remote.getHost(), remote.getClientPort());
		sendConnectedGames(remote);
	}

	public void loginDisconnected() {
		LOG.log(Level.INFO, "{0} server disconnected.", ServerType.getName(ServerType.LOGIN));
		loginServer = null;
	}

	public void gameDisconnected(CenterGameInterface remote) {
		byte serverId = remote.getServerId();
		LOG.log(Level.INFO, "{0} server disconnected.", ServerType.getName(remote.getServerId()));
		notifyGameDisconnected(remote.getWorld(), serverId);
		gameServers.remove(Byte.valueOf(serverId));
	}

	public void shopDisconnected() {
		LOG.log(Level.INFO, "{0} server disconnected.", ServerType.getName(ServerType.SHOP));
		notifyShopDisconnected();
		shopServer = null;
	}

	public void channelPortChanged(byte world, byte serverId, byte channel, int newPort) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(4);
		lew.writeByte(CenterRemoteOps.CHANNEL_PORT_CHANGE);
		lew.writeByte(world);
		lew.writeByte(serverId);
		lew.writeByte(channel);
		lew.writeInt(newPort);
		byte[] bytes = lew.getBytes();

		if (loginServer != null)
			loginServer.getSession().send(bytes);
		if (shopServer != null)
			shopServer.getSession().send(bytes);
		for (CenterGameInterface gameServer : getAllServersOfWorld(world, serverId))
			gameServer.getSession().send(bytes);
	}

	private void notifyGameConnected(byte serverId, byte world, String host, Map<Byte, Integer> ports) {
		byte size = (byte) ports.size();
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(host.length() + 5 + size * 5);
		lew.writeByte(CenterRemoteOps.GAME_CONNECTED);
		lew.writeByte(serverId);
		lew.writeByte(world);
		lew.writeLengthPrefixedString(host);
		lew.writeByte(size);
		for (Entry<Byte, Integer> entry : ports.entrySet()) {
			lew.writeByte(entry.getKey().byteValue());
			lew.writeInt(entry.getValue().intValue());
		}
		byte[] bytes = lew.getBytes();

		if (loginServer != null)
			loginServer.getSession().send(bytes);
		if (shopServer != null)
			shopServer.getSession().send(bytes);
		for (CenterGameInterface gameServer : getAllServersOfWorld(world, serverId))
			gameServer.getSession().send(bytes);
	}

	private void notifyGameDisconnected(byte world, byte serverId) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(2);
		lew.writeByte(CenterRemoteOps.GAME_DISCONNECTED);
		lew.writeByte(serverId);
		lew.writeByte(world);
		byte[] bytes = lew.getBytes();

		if (loginServer != null)
			loginServer.getSession().send(bytes);
		if (shopServer != null)
			shopServer.getSession().send(bytes);
		for (CenterGameInterface gameServer : getAllServersOfWorld(world, serverId))
			gameServer.getSession().send(bytes);
	}

	private void notifyShopConnected(String host, int port) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(host.length() + 7);
		lew.writeByte(CenterRemoteOps.SHOP_CONNECTED);
		lew.writeLengthPrefixedString(host);
		lew.writeInt(port);
		byte[] bytes = lew.getBytes();

		for (CenterGameInterface gameServer : gameServers.values())
			gameServer.getSession().send(bytes);
	}

	private void notifyShopDisconnected() {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(1);
		lew.writeByte(CenterRemoteOps.SHOP_DISCONNECTED);
		byte[] bytes = lew.getBytes();

		for (CenterGameInterface gameServer : gameServers.values())
			gameServer.getSession().send(bytes);
	}

	private void sendConnectedShop(CenterGameInterface game) {
		if (shopServer != null) {
			String host = shopServer.getHost();
			int port = shopServer.getClientPort();
			LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(host.length() + 7);
			lew.writeByte(CenterRemoteOps.SHOP_CONNECTED);
			lew.writeLengthPrefixedString(host);
			lew.writeInt(port);
			game.getSession().send(lew.getBytes());
		}
	}

	private void sendConnectedGames(CenterRemoteInterface connected) {
		LittleEndianByteArrayWriter lew;
		CenterGameInterface game;
		String host;
		Map<Byte, Integer> ports;
		for (Entry<Byte, CenterGameInterface> entry : gameServers.entrySet()) {
			game = entry.getValue();
			host = game.getHost();
			ports = game.getClientPorts();
			byte size = (byte) ports.size();
			lew = new LittleEndianByteArrayWriter(host.length() + 5 + size * 5);
			lew.writeByte(CenterRemoteOps.GAME_CONNECTED);
			lew.writeByte(entry.getKey().byteValue());
			lew.writeByte(entry.getValue().getWorld());
			lew.writeLengthPrefixedString(host);
			lew.writeByte(size);
			for (Entry<Byte, Integer> port : ports.entrySet()) {
				lew.writeByte(port.getKey().byteValue());
				lew.writeInt(port.getValue().intValue());
			}
			connected.getSession().send(lew.getBytes());
		}
	}

	private void sendConnectedGamesOfWorld(CenterGameInterface connected) {
		byte ourServerId = connected.getServerId();
		byte ourWorld = connected.getWorld();
		LittleEndianByteArrayWriter lew;
		CenterGameInterface server;
		String host;
		Map<Byte, Integer> ports;
		for (Entry<Byte, CenterGameInterface> entry : gameServers.entrySet()) {
			byte serverId = entry.getKey().byteValue();
			server = entry.getValue();
			if (serverId != ourServerId && server.getWorld() == ourWorld) {
				host = server.getHost();
				ports = server.getClientPorts();
				byte size = (byte) ports.size();
				lew = new LittleEndianByteArrayWriter(host.length() + 5 + size * 5);
				lew.writeByte(CenterRemoteOps.GAME_CONNECTED);
				lew.writeByte(entry.getKey().byteValue());
				lew.writeByte(server.getWorld());
				lew.writeLengthPrefixedString(host);
				lew.writeByte(size);
				for (Entry<Byte, Integer> port : ports.entrySet()) {
					lew.writeByte(port.getKey().byteValue());
					lew.writeInt(port.getValue().intValue());
				}
				connected.getSession().send(lew.getBytes());
			}
		}
	}

	public boolean isServerConnected(byte serverId) {
		if (ServerType.isGame(serverId))
			return gameServers.containsKey(Byte.valueOf(serverId));
		if (ServerType.isLogin(serverId))
			return loginServer != null;
		if (ServerType.isShop(serverId))
			return shopServer != null;
		return false;
	}

	public CenterShopInterface getShopServer() {
		return shopServer;
	}

	public CenterLoginInterface getLoginServer() {
		return loginServer;
	}

	public CenterGameInterface getGameServer(byte serverId) {
		return gameServers.get(Byte.valueOf(serverId));
	}

	public static CenterServer getInstance() {
		return instance;
	}

	public static void main(String[] args) {
		instance = new CenterServer();
		instance.init();
	}
}
