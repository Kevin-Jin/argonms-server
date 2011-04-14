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

package argonms.net.external;

import argonms.character.Player;
import argonms.game.GameClient;
import argonms.shop.ShopClient;
import argonms.tools.DatabaseConnection;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * An extremely primitive autoban system for the server.
 * 
 * Logs any suspicious activity that a client may perform and automatically
 * temporary bans the user if the log reaches a certain tolerance.
 * @author GoldenKevin
 */
public class CheatTracker {
	private static final Logger LOG = Logger.getLogger(CheatTracker.class.getName());

	private static final int TOLERANCE = 10;

	private static Map<Integer, CheatTracker> recent;

	static {
		recent = new HashMap<Integer, CheatTracker>();
	}

	private RemoteClient client;
	private int total;
	private List<Offense> offenses;

	private CheatTracker(RemoteClient rc) {
		this.client = rc;
		this.total = 0;
		this.offenses = new ArrayList<Offense>();
	}

	private void load() {
		Connection con = DatabaseConnection.getConnection();
		try {
			PreparedStatement ps = con.prepareStatement("SELECT `message`,`points` FROM `cheatlog` WHERE `accountid` = ?");
			ps.setInt(1, client.getAccountId());
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				int points = rs.getInt(2);
				offenses.add(new Offense(rs.getString(1), points));
				total += points;
			}
			rs.close();
			ps.close();
		} catch (SQLException ex) {
			LOG.log(Level.WARNING, "Could not load cheatlog for account "
					+ client.getAccountId(), ex);
		}
	}

	public void suspicious(String message) {
		offenses.add(new Offense(message, 1));
		total += 1;
		Connection con = DatabaseConnection.getConnection();
		try {
			PreparedStatement ps = con.prepareStatement("INSERT INTO `cheatlog`(`message`,`points`,`accountid`) VALUES(?,?,?)");
			ps.setString(1, message);
			ps.setInt(2, 1);
			ps.setInt(3, client.getAccountId());
			ps.executeUpdate();
			ps.close();
		} catch (SQLException ex) {
			LOG.log(Level.WARNING, "Could not load cheatlog for account "
					+ client.getAccountId(), ex);
		}
		if (total >= TOLERANCE) {
			//shit's going down.
		}
	}

	private int getAccountId() {
		return client.getAccountId();
	}

	private String getAccountName() {
		return client.getAccountName();
	}

	/**
	 * Get the network address of the remote client.
	 * @return the IP address of the remote client in big-endian
	 * byte order.
	 */
	private byte[] getIpAddress() {
		return ((InetSocketAddress) client.getSession().getAddress()).getAddress().getAddress();
	}

	/**
	 * Gets the hardware address of the remote client (usually
	 * the MAC address). Much more secure to identify a cheater with
	 * this when they have an easily changeable dynamic IP.
	 * @return the MAC address of the remote client.
	 */
	private byte[] getMacAddress() {
		try {
			return NetworkInterface.getByInetAddress(((InetSocketAddress) client.getSession().getAddress()).getAddress()).getHardwareAddress();
		} catch (SocketException ex) {
			LOG.log(Level.WARNING, "Could not get MAC address of account " + client.getAccountId(), ex);
			return null;
		}
	}

	/**
	 * Get the character that this client is associated with.
	 * @return null if this client isn't associated with a player
	 */
	private Player getCharacter() {
		if (client instanceof GameClient)
			return ((GameClient) client).getPlayer();
		if (client instanceof ShopClient)
			return ((ShopClient) client).getPlayer();
		return null;
	}

	/**
	 * Get the id of the character that this client is logged in on.
	 * @return -1 if no player exists on this client
	 */
	private int getCharacterId() {
		Player p = getCharacter();
		return (p != null ? p.getId() : -1);
	}

	/**
	 * Get the name of the character that this client is logged in on.
	 * @return null if this client isn't associated with a player
	 */
	private String getCharacterName() {
		Player p = getCharacter();
		return (p != null ? p.getName() : null);
	}

	public static CheatTracker get(RemoteClient rc) {
		Integer accountid = Integer.valueOf(rc.getAccountId());
		if (!recent.containsKey(accountid)) {
			CheatTracker t = new CheatTracker(rc);
			t.load();
			recent.put(accountid, t);
		}
		return recent.get(accountid);
	}

	private static class Offense {
		private String message;
		private int pointValue;

		public Offense(String message, int points) {
			this.message = message;
			this.pointValue = points;
		}
	}
}
