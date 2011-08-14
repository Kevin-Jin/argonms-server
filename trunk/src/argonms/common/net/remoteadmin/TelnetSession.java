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

//TODO: do not echo \b (backspace char) if no more user typed characters left
//(use a counter of user typed characters, adding to it when a character is
//typed and removing when a \b is typed; when the counter <= 0, don't echo \b)
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

	private void loginRetry(String message) {
		this.state = State.LOGIN;
		ch.write(message + "\r\n\r\n");
		ch.write("Login: ");
	}

	private long loadBanExpire(Connection con, int accountId) throws SQLException {
		long banExpire = 0;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = con.prepareStatement("SELECT `b`.`accountid` FROM `bans` `b` LEFT JOIN `accounts` `a` ON `b`.`accountid` = `a`.`id` WHERE `b`.`accountid` = ? OR `b`.`ip` = `a`.`recentip`");
			ps.setInt(1, accountId);
			rs = ps.executeQuery();
			//there could be two different bans for the account and the ip address
			//if that's the case, load both and calculate the longest lasting
			//infraction and the most outstanding infraction reason from a union
			//of the infractions of the two bans.
			while (rs.next()) {
				PreparedStatement ips = null;
				ResultSet irs = null;
				try {
					//load only non-expired infractions - even though the ban
					//may have been given with infractions that have already
					//expired, the longest lasting infraction will still remain
					//the same, and we could just choose the most outstanding
					//points from the active infractions to send to the client
					ips = con.prepareStatement("SELECT MAX(`expiredate`) FROM `infractions` WHERE `accountid` = ? AND `pardoned` = 0 AND `expiredate` > (UNIX_TIMESTAMP() * 1000)");
					ips.setInt(1, rs.getInt(1));
					irs = ips.executeQuery();
					if (irs.next())
						banExpire = rs.getLong(1);
				} finally {
					DatabaseManager.cleanup(DatabaseType.STATE, irs, ips, null);
				}
			}
		} finally {
			DatabaseManager.cleanup(DatabaseType.STATE, rs, ps, null);
		}
		return banExpire;
	}

	private void authenticate(String pwd) {
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			con = DatabaseManager.getConnection(DatabaseType.STATE);
			ps = con.prepareStatement("SELECT `id`,`password`,`salt`,`gm` FROM `accounts` WHERE `name` = ?");
			ps.setString(1, username);
			rs = ps.executeQuery();
			if (rs.next()) {
				long banExpire = loadBanExpire(con, rs.getInt(1));
				byte[] passhash = rs.getBytes(2);
				byte[] salt = rs.getBytes(3);
				byte gm = rs.getByte(4);

				boolean correct, hashUpdate, hasSalt = (salt != null && salt.length != 0);
				switch (passhash.length) {
					case 20: //sha-1 (160 bits = 20 bytes)
						correct = hasSalt && HashFunctions.checkSaltedSha1Hash(passhash, pwd, salt) || !hasSalt && HashFunctions.checkSha1Hash(passhash, pwd);
						//only update to SHA512 w/ salt if we are sure the given password matches the SHA1 hash
						hashUpdate = correct;
						break;
					case 64: //sha-512 (512 bits = 64 bytes)
						correct = hasSalt && HashFunctions.checkSaltedSha512Hash(passhash, pwd, salt) || !hasSalt && HashFunctions.checkSha512Hash(passhash, pwd);
						//only update to SHA512 w/ salt if we are sure the given password matches and we don't already have a salt
						hashUpdate = correct && !hasSalt;
						break;
					case 5:
					case 6:
					case 7:
					case 8:
					case 9:
					case 10:
					case 11:
					case 12: //plaintext - client only sends password (5 <= chars <= 12)
						correct = new String(passhash, HashFunctions.ASCII).equals(pwd);
						//only update to SHA512 w/ salt if we are sure the given password matches the plaintext
						hashUpdate = correct;
						break;
					default:
						correct = false;
						//don't update to SHA512 w/ salt if we can't verify the given password
						hashUpdate = false;
						break;
				}
				if (correct) {
					if (gm < UserPrivileges.ADMIN) {
						loginRetry("You do not have sufficient permissions to administer this server.");
					} else if (banExpire > System.currentTimeMillis()) {
						loginRetry("Your account is currently banned. Try again later.");
					} else {
						rs.close();
						ps.close();
						if (hashUpdate) {
							salt = HashFunctions.makeSalt();
							passhash = HashFunctions.makeSaltedSha512Hash(pwd, salt);
							ps = con.prepareStatement("UPDATE `accounts` SET `password` = ?, `salt` = ? WHERE `name` = ?");
							ps.setBytes(2, passhash);
							ps.setBytes(3, salt);
							ps.setString(4, username);
							ps.executeUpdate();
						}
						this.state = State.MESSAGE;
						ch.write("Welcome, " + username + "!\r\n\r\n");
						ch.write(username + ">");
					}
				} else {
					loginRetry("You have entered the wrong password. Try again.");
				}
			} else {
				loginRetry("That in-game account does not exist. Try again.");
			}
		} catch (SQLException ex) {
			LOG.log(Level.WARNING, "Could not fetch login information for telnet user of account " + username, ex);
			loginRetry("Internal server error. Try again.");
		} finally {
			DatabaseManager.cleanup(DatabaseType.STATE, rs, ps, con);
		}
	}

	private void processCommand(String message) {
		if (message.equals("exit") || message.equals("quit")) {
			ch.close();
			return;
		} else if (message.equals("help")) {
			ch.write("EXIT\t\tCloses the current telnet session.\r\n"
					+ "HELP\t\tDisplays this message.\r\n"
					+ "\r\n");
		} else if (!message.trim().isEmpty()) {
			ch.write('\'' + message.trim().split(" ")[0] + "\' is not recognized as a command.\r\n\r\n");
		}
		ch.write(username + ">");
	}
}
