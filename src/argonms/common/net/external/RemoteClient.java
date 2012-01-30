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

package argonms.common.net.external;

import argonms.common.character.Player;
import argonms.common.net.SessionDataModel;
import argonms.common.util.DatabaseManager;
import argonms.common.util.DatabaseManager.DatabaseType;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author GoldenKevin
 */
public abstract class RemoteClient implements SessionDataModel {
	private static final Logger LOG = Logger.getLogger(RemoteClient.class.getName());
	public static final byte
		STATUS_NOTLOGGEDIN = 0,
		STATUS_MIGRATION = 1,
		STATUS_INLOGIN = 2,
		STATUS_INGAME = 3,
		STATUS_INSHOP = 4
	;

	private ClientSession<?> session;
	private int id;
	private String name;
	private byte world, channel;
	private boolean serverTransition;

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

	@Override
	public ClientSession<?> getSession() {
		return session;
	}

	protected void setSession(ClientSession<?> s) {
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

	public void clientError(String message) {
		LOG.log(Level.WARNING, "Received error from client at {0}:\n{1}",
				new Object[] { getSession().getAddress(), message });
	}

	public void migrateHost() {
		serverTransition = true;
		updateState(STATUS_MIGRATION);
	}

	protected boolean isMigrating() {
		return serverTransition;
	}

	public void updateState(byte currentState) {
		Connection con = null;
		PreparedStatement ps = null;
		try {
			con = DatabaseManager.getConnection(DatabaseType.STATE);
			ps = con.prepareStatement("UPDATE `accounts` SET `connected` = ? WHERE `id` = ?");
			ps.setByte(1, currentState);
			ps.setInt(2, id);
			ps.executeUpdate();
		} catch (SQLException ex) {
			LOG.log(Level.WARNING, "Could not change connected status of account " + id, ex);
		} finally {
			DatabaseManager.cleanup(DatabaseType.STATE, null, ps, con);
		}
	}

	public abstract Player getPlayer();

	public abstract byte getServerId();

	/**
	 * DO NOT USE THIS METHOD TO FORCE THE CLIENT TO CLOSE ITSELF. USE
	 * getSession().close() INSTEAD.
	 */
	@Override
	public abstract void disconnected();
}
