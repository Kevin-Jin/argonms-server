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
import argonms.tools.Timer;
import argonms.tools.output.LittleEndianByteArrayWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.concurrent.ScheduledFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author GoldenKevin
 */
public abstract class RemoteClient {
	private static final Logger LOG = Logger.getLogger(RemoteClient.class.getName());
	private static final int TIMEOUT = 10000; //in milliseconds
	public static final byte
		STATUS_NOTLOGGEDIN = 0,
		STATUS_MIGRATION = 1,
		STATUS_INLOGIN = 2,
		STATUS_INGAME = 3,
		STATUS_INSHOP = 4
	;

	private int id;
	private String name;
	private ClientSession session;
	private byte world, channel;
	private KeepAliveTask heartbeat;
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

	public ClientSession getSession() {
		return session;
	}

	public void setSession(ClientSession s) {
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

	public void receivedPong() {
		heartbeat.receivedResponse();
	}

	public void startPingTask() {
		heartbeat = new KeepAliveTask();
		ScheduledFuture<?> f = Timer.getInstance().runRepeatedly(heartbeat, 0, TIMEOUT);
		heartbeat.setFuture(f);
	}

	public void stopPingTask() {
		if (heartbeat != null) {
			heartbeat.stop();
			heartbeat = null;
		}
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
		Connection con = DatabaseConnection.getConnection();
		try {
			PreparedStatement ps = con.prepareStatement("UPDATE `accounts` SET `connected` = ? WHERE `id` = ?");
			ps.setByte(1, currentState);
			ps.setInt(2, id);
			ps.executeUpdate();
			ps.close();
		} catch (SQLException ex) {
			LOG.log(Level.WARNING, "Could not change connected status of account " + id, ex);
		}
	}

	public abstract byte getServerId();

	/**
	 * Notify the other players on the server that this player is logging off
	 * and save the player's stats to the database.
	 * DO NOT USE THIS METHOD TO FORCE THE CLIENT TO CLOSE ITSELF. USE
	 * getSession().close() INSTEAD.
	 */
	public abstract void disconnect();

	private static byte[] pingMessage() {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(2);
		lew.writeShort(ClientSendOps.PING);
		return lew.getBytes();
	}

	private class KeepAliveTask implements Runnable {
		private long lastPong;
		private ScheduledFuture<?> future;

		public KeepAliveTask() {
			lastPong = System.currentTimeMillis();
		}

		public void run() {
			if (System.currentTimeMillis() - lastPong > TIMEOUT) {
				LOG.log(Level.INFO, "Account {0} timed out after " + TIMEOUT
						+ " milliseconds.", getAccountName());
				getSession().close();
			} else {
				getSession().send(pingMessage());
			}
		}

		public void setFuture(ScheduledFuture<?> future) {
			this.future = future;
		}

		public void receivedResponse() {
			lastPong = System.currentTimeMillis();
		}

		public void stop() {
			future.cancel(true);
			future = null;
		}
	}
}
