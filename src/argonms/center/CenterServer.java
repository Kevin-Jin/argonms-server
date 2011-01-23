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
import argonms.net.server.CenterRemoteOps;
import argonms.net.server.CenterRemoteInterface;
import argonms.net.server.RemoteServerListener;
import argonms.tools.DatabaseConnection;
import argonms.tools.output.LittleEndianByteArrayWriter;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
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
	private Map<Byte, CenterRemoteInterface> connectedRemotes;

	private CenterServer() {
		connectedRemotes = new HashMap<Byte, CenterRemoteInterface>();
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

	public void serverConnected(byte world, CenterRemoteInterface remote) {
		LOG.log(Level.INFO, "{0} server accepted from {1}.", new Object[]{ServerType.getName(world), remote.getRemoteEndpoint()});
		connectedRemotes.put(Byte.valueOf(world), remote);
		if (ServerType.isGame(world)) {
			notifyGameConnected(world, remote.getHost(), remote.getClientPorts());
			sendConnectedShop(remote);
		} else if (ServerType.isShop(world)) {
			notifyShopConnected(remote.getHost(), remote.getClientPorts()[0]);
			sendConnectedGames(remote);
		} else if (ServerType.isLogin(world)) {
			sendConnectedGames(remote);
		}
	}

	public void serverDisconnected(byte world) {
		if (connectedRemotes.containsKey(Byte.valueOf(world))) {
			LOG.log(Level.INFO, "{0} server disconnected.", ServerType.getName(world));
			connectedRemotes.remove(Byte.valueOf(world));
			if (ServerType.isGame(world)) {
				notifyGameDisconnected(world);
			} else if (ServerType.isShop(world)) {
				notifyShopDisconnected();
			}
		}
	}

	private void notifyGameConnected(byte world, String host, int[] ports) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(host.length() + 5 + ports.length * 4);
		lew.writeByte(CenterRemoteOps.GAME_CONNECTED);
		lew.writeByte(world);
		lew.writeLengthPrefixedString(host);
		lew.writeByte((byte) ports.length);
		for (int i = 0; i < ports.length; i++)
			lew.writeInt(ports[i]);

		CenterRemoteInterface remote = connectedRemotes.get(Byte.valueOf(ServerType.LOGIN));
		if (remote != null)
			remote.send(lew.getBytes());
		remote = connectedRemotes.get(Byte.valueOf(ServerType.SHOP));
		if (remote != null)
			remote.send(lew.getBytes());
	}

	private void notifyGameDisconnected(byte world) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(2);
		lew.writeByte(CenterRemoteOps.GAME_DISCONNECTED);
		lew.writeByte(world);

		CenterRemoteInterface remote = connectedRemotes.get(Byte.valueOf(ServerType.LOGIN));
		if (remote != null)
			remote.send(lew.getBytes());
		remote = connectedRemotes.get(Byte.valueOf(ServerType.SHOP));
		if (remote != null)
			remote.send(lew.getBytes());
	}

	private void notifyShopConnected(String host, int port) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(host.length() + 7);
		lew.writeByte(CenterRemoteOps.SHOP_CONNECTED);
		lew.writeLengthPrefixedString(host);
		lew.writeInt(port);

		for (Entry<Byte, CenterRemoteInterface> entry : connectedRemotes.entrySet())
			if (ServerType.isGame(entry.getKey().byteValue()))
				entry.getValue().send(lew.getBytes());
	}

	private void notifyShopDisconnected() {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(1);
		lew.writeByte(CenterRemoteOps.SHOP_DISCONNECTED);

		for (Entry<Byte, CenterRemoteInterface> entry : connectedRemotes.entrySet())
			if (ServerType.isGame(entry.getKey().byteValue()))
				entry.getValue().send(lew.getBytes());
	}

	private void sendConnectedShop(CenterRemoteInterface game) {
		CenterRemoteInterface remote = connectedRemotes.get(Byte.valueOf(ServerType.SHOP));
		if (remote != null) {
			String host = remote.getHost();
			int port = remote.getClientPorts()[0];
			LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(host.length() + 7);
			lew.writeByte(CenterRemoteOps.SHOP_CONNECTED);
			lew.writeLengthPrefixedString(host);
			lew.writeInt(port);
			game.send(lew.getBytes());
		}
	}

	private void sendConnectedGames(CenterRemoteInterface connected) {
		LittleEndianByteArrayWriter lew;
		CenterRemoteInterface game;
		String host;
		int[] ports;
		for (Entry<Byte, CenterRemoteInterface> entry : connectedRemotes.entrySet()) {
			if (ServerType.isGame(entry.getKey().byteValue())) {
				game = entry.getValue();
				host = game.getHost();
				ports = game.getClientPorts();
				lew = new LittleEndianByteArrayWriter(host.length() + 5 + ports.length * 4);
				lew.writeByte(CenterRemoteOps.GAME_CONNECTED);
				lew.writeByte(entry.getKey().byteValue());
				lew.writeLengthPrefixedString(host);
				lew.writeByte((byte) ports.length);
				for (int i = 0; i < ports.length; i++)
					lew.writeInt(ports[i]);
				connected.send(lew.getBytes());
			}
		}
	}

	public boolean isServerConnected(byte world) {
		return connectedRemotes.containsKey(Byte.valueOf(world));
	}

	public CenterRemoteInterface getShopServer() {
		return connectedRemotes.get(Byte.valueOf(ServerType.SHOP));
	}

	public CenterRemoteInterface getLoginServer() {
		return connectedRemotes.get(Byte.valueOf(ServerType.LOGIN));
	}

	public CenterRemoteInterface getGameServer(byte world) {
		return connectedRemotes.get(Byte.valueOf(world));
	}

	public static CenterServer getInstance() {
		return instance;
	}

	public static void main(String[] args) {
		instance = new CenterServer();
		instance.init();
	}
}
