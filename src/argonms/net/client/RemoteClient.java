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

package argonms.net.client;

import argonms.tools.DatabaseConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author GoldenKevin
 */
public abstract class RemoteClient {
	private static final Logger LOG = Logger.getLogger(RemoteClient.class.getName());

	public static final byte
		STATUS_NOTLOGGEDIN = 0,
		STATUS_INLOGIN = 1,
		STATUS_INGAME = 2,
		STATUS_INSHOP = 3
	;

	private int id;
	private String name;
	private ClientSession session;
	private byte world, channel;

	public int getAccountId() {
		return id;
	}

	public void setAccountId(int id) {
		this.id = id;
	}

	public String getAccountName() {
		return name;
	}

	public void setAccountName(String name) {
		this.name = name;
	}

	public ClientSession getSession() {
		return session;
	}

	public void setSession(ClientSession s) {
		if (this.session == null) //be sure not to change it after it's been set
			this.session = s;
	}

	public byte getWorld() {
		return world;
	}

	public void setWorld(byte world) {
		this.world = world;
	}

	public byte getChannel() {
		return channel;
	}

	public void setChannel(byte channel) {
		this.channel = channel;
	}

	public void updateState(byte currentState) {
		Connection con = DatabaseConnection.getConnection();
		try {
			PreparedStatement ps = con.prepareStatement("UPDATE `accounts` SET `connected` = ? WHERE `id` = ?");
			ps.setByte(1, STATUS_NOTLOGGEDIN);
			ps.setInt(2, id);
			ps.executeUpdate();
			ps.close();
		} catch (SQLException ex) {
			LOG.log(Level.WARNING, "Could not change connected status of account " + id, ex);
		}
	}

	/**
	 * Notify the other players on the server that this player is logging off
	 * and save the player's stats to the database.
	 * DO NOT USE THIS METHOD TO FORCE THE CLIENT TO CLOSE ITSELF. USE
	 * getSession().close() INSTEAD.
	 */
	public abstract void disconnect();
}
