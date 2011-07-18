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

package argonms.common.net.remoteadmin;

import argonms.common.UserPrivileges;
import argonms.common.net.HashFunctions;
import argonms.common.util.DatabaseManager;
import argonms.common.util.DatabaseManager.DatabaseType;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jboss.netty.channel.Channel;

/**
 *
 * @author GoldenKevin
 */
public class TelnetSession {
	private static final Logger LOG = Logger.getLogger(TelnetSession.class.getName());

	private enum State { LOGIN, PASSWORD, MESSAGE }

	public static final byte
		IAC = (byte) 0xFF,
		DONT = (byte) 0xFE,
		DO = (byte) 0xFD,
		WONT = (byte) 0xFC,
		WILL = (byte) 0xFB,
		SB = (byte) 0xFA,
		SE = (byte) 0xF0
	;

	public static final byte
		BINARY = 0,
		ECHO = 1,
		SUPPRESS_GO_AHEAD = 3,
		TERMINAL_TYPE = 24,
		NAWS = 31,
		TERMINAL_SPEED = 32,
		AUTHENTICATION = 37,
		NEW_ENVIRON = 39
	;

	private Channel ch;
	private String username;
	private State state;

	public TelnetSession(Channel channel) {
		this.ch = channel;
		this.state = State.LOGIN;
		channel.write("Please use the same credentials as your in-game account.\r\n");
		channel.write("Login: ");
	}

	public int protocolCmd(byte[] array, int index) {
		int delta = 1;
		switch (array[index]) {
			case WILL:
				delta++;
				switch (array[index + 1]) {
					case TERMINAL_TYPE:
						break;
					case NAWS:
						break;
					case TERMINAL_SPEED:
						break;
					case NEW_ENVIRON:
						break;
					case SUPPRESS_GO_AHEAD:
						ch.write(new byte[] { IAC, DO, SUPPRESS_GO_AHEAD });
						break;
				}
				break;
			case DO:
				delta++;
				switch (array[index + 1]) {
					case ECHO:
						ch.write(new byte[] { IAC, WILL, ECHO });
						break;
					case SUPPRESS_GO_AHEAD:
						ch.write(new byte[] { IAC, WILL, SUPPRESS_GO_AHEAD });
						break;
				}
				break;
		}
		return delta;
	}

	public boolean willEcho() {
		return state != State.PASSWORD;
	}

	public void process(String message) {
		switch (state) {
			case LOGIN:
				this.username = message;
				ch.write("Password: ");
				this.state = State.PASSWORD;
				break;
			case PASSWORD:
				ch.write("\r\n");
				authenticate(message);
				break;
			case MESSAGE:
				processCommand(message);
				break;
		}
	}

	private void authenticate(String pwd) {
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			con = DatabaseManager.getConnection(DatabaseType.STATE);
			ps = con.prepareStatement("SELECT `password`,`salt`,`banexpire`,`gm` FROM `accounts` WHERE `name` = ?");
			ps.setString(1, username);
			rs = ps.executeQuery();
			if (rs.next()) {
				String passhash = rs.getString(1);
				String salt = rs.getString(2);
				int banExpire = rs.getInt(3);
				byte gm = rs.getByte(4);
				if (gm < UserPrivileges.ADMIN) {
					this.state = State.LOGIN;
					ch.write("You do not have sufficient permissions to administer this server.\r\n\r\n");
					ch.write("Login: ");
				} else if ((salt == null || salt.length() == 0) && (passhash.equals(pwd) || HashFunctions.checkSha1Hash(passhash, pwd))) {
					salt = HashFunctions.makeSalt();
					passhash = HashFunctions.makeSaltedSha512Hash(pwd, salt);
					ps = con.prepareStatement("UPDATE `accounts` SET `password` = ?, `salt` = ? WHERE `name` = ?");
					ps.setString(2, passhash);
					ps.setString(3, salt);
					ps.setString(4, username);
					ch.write("Welcome, " + username + "!\r\n\r\n");
					this.state = State.MESSAGE;
					ch.write(">");
				} else if (HashFunctions.checkSaltedSha512Hash(passhash, pwd, salt)) {
					ch.write("Welcome, " + username + "!\r\n\r\n");
					this.state = State.MESSAGE;
					ch.write(">");
				} else if (banExpire > 0) {
					this.state = State.LOGIN;
					ch.write("Your account is currently banned. Try again later.\r\n\r\n");
					ch.write("Login: ");
				} else {
					this.state = State.LOGIN;
					ch.write("You have entered the wrong password. Try again.\r\n\r\n");
					ch.write("Login: ");
				}
			} else {
				this.state = State.LOGIN;
				ch.write("That in-game account does not exist. Try again.\r\n\r\n");
				ch.write("Login: ");
			}
		} catch (SQLException ex) {
			LOG.log(Level.WARNING, "Could not fetch login information for telnet user of account " + username, ex);
			this.state = State.LOGIN;
			ch.write("Internal server error. Try again.\r\n");
			ch.write("Login: ");
		} finally {
			DatabaseManager.cleanup(DatabaseType.STATE, rs, ps, con);
		}
	}

	private void processCommand(String message) {
		if (message.equals("exit") || message.equals("quit")) {
			ch.close();
			return;
		}
		ch.write(">");
	}
}
