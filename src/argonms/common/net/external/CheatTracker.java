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
import argonms.common.util.DatabaseManager;
import argonms.common.util.DatabaseManager.DatabaseType;
import argonms.common.util.collections.LockableMap;
import java.net.InetSocketAddress;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Logs any suspicious activity that a client may perform and automatically
 * temporary bans the user if the log reaches a certain tolerance.
 * Infractions are logged separately per account. Infractions from the same
 * computer will not be combined in a way that will allow a user to be banned if
 * they reached the infraction limit through more than one account but hasn't
 * reached the infraction limit on a single account. However, once an account is
 * banned, all attempts to login from the device that the account was banned on
 * will fail (through MAC and IP address checks). The IP ban will deny any user
 * playing behind the same gateway (router) as a banned account, while the MAC
 * ban will deny any user playing on the same computer as a banned account.
 * This class is thread safe.
 * @author GoldenKevin
 */
public class CheatTracker {
	private static final Logger LOG = Logger.getLogger(CheatTracker.class.getName());

	private static final int TOLERANCE = 10000;

	private static LockableMap<RemoteClient, CheatTracker> recent;

	static {
		recent = new LockableMap<RemoteClient, CheatTracker>(new WeakHashMap<RemoteClient, CheatTracker>());
	}

	public enum Infraction {
		PACKET_EDITING	(1, 2000, 30L * 24 * 60 * 60 * 1000); //30 days

		private static final Map<Byte, Infraction> lookup;

		//initialize reverse lookup
		static {
			lookup = new HashMap<Byte, Infraction>(values().length);
			for (Infraction reason : values())
				lookup.put(Byte.valueOf(reason.byteValue()), reason);
		}

		private final byte value;
		private final short points;
		private final long time;

		private Infraction(int value, int points, long length) {
			this.value = (byte) value;
			this.points = (short) points;
			this.time = length;
		}

		/**
		 * @see LoginClient.banReason
		 */
		public byte byteValue() {
			return value;
		}

		public short points() {
			return points;
		}

		public long duration() {
			return time;
		}

		public static Infraction valueOf(byte reason) {
			return lookup.get(Byte.valueOf(reason));
		}
	}

	public enum Assigner {
		GM		("gm warning"),
		AUTOBAN	("machine detected");

		private final String value;

		private Assigner(String sqlName) {
			value = sqlName;
		}

		public String sqlName() {
			return value;
		}
	}

	private RemoteClient client;
	private int totalPoints;
	private boolean infractionsLoaded;
	private Lock loadLock;
	private Map<String, Long> timeLog;

	private CheatTracker(RemoteClient rc) {
		this.client = rc;
		this.totalPoints = 0;
		this.infractionsLoaded = false;
		this.loadLock = new ReentrantLock();
		this.timeLog = new ConcurrentHashMap<String, Long>();
	}

	private long ipBytesToLong(byte[] b) {
		//IP addresses are just 4-byte (32-bit) integers represented by 4 bytes
		//in big endian
		//since singed ints can only hold 31-bit without overflow, and Java
		//doesn't have unsigned int, use signed long (63-bit)
		long longValue = 0;
		for (int byt = 0, bitShift = 24; byt < 4; byt++, bitShift -= 8)
			longValue += (long) (b[byt] & 0xFF) << bitShift;
		return longValue;
	}

	private void load() {
		totalPoints = 0;

		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			con = DatabaseManager.getConnection(DatabaseType.STATE);
			//only get infractions that haven't expired and aren't pardoned yet
			ps = con.prepareStatement("SELECT `severity` FROM `infractions`"
					+ "WHERE `accountid` = ? AND `pardoned` = 0 AND `expiredate` > (UNIX_TIMESTAMP() * 1000)");
			ps.setInt(1, getAccountId());
			rs = ps.executeQuery();
			while (rs.next())
				totalPoints += rs.getShort(1);
			infractionsLoaded = true;
		} catch (SQLException ex) {
			LOG.log(Level.WARNING, "Could not load cheatlog for account "
					+ getAccountId(), ex);
		} finally {
			DatabaseManager.cleanup(DatabaseType.STATE, rs, ps, con);
		}
	}

	//no matter how minor the infractions are, always MAC, IP, and account name
	//ban a player if they exceed the tolerance. it's pointless to just choose
	//one as they can be easily bypassed individually.
	private void ban(Connection con) throws SQLException {
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = con.prepareStatement("INSERT INTO `bans` (`accountid`,`ip`) VALUES (?,?)", Statement.RETURN_GENERATED_KEYS);
			ps.setInt(1, getAccountId());
			ps.setLong(2, ipBytesToLong(getIpAddress()));
			ps.executeUpdate();
			rs = ps.getGeneratedKeys();
			int entryId = rs.next() ? rs.getInt(1) : -1;
			rs.close();
			ps.close();

			//storing macs in binary saves us 2 bytes per address compared
			//to if we used a more readable 8-byte/64-bit signed integer
			ps = con.prepareStatement("SELECT `recentmacs` FROM `accounts` WHERE `id` = ?");
			ps.setInt(1, getAccountId());
			rs = ps.executeQuery();
			byte[] macListCombined = rs.next() ? rs.getBytes(1) : null;

			if (macListCombined != null) {
				rs.close();
				ps.close();

				int macCount = macListCombined.length / 6;
				byte[] macAddress = new byte[6];
				ps = con.prepareStatement("INSERT INTO `macbans` (`banid`,`mac`) VALUES (?,?)");
				ps.setInt(1, entryId);
				for (int i = 0; i < macCount; i++) {
					System.arraycopy(macListCombined, i * 6, macAddress, 0, 6);
					ps.setBytes(2, macAddress);
					ps.executeUpdate();
				}
			}
		} finally {
			DatabaseManager.cleanup(DatabaseType.STATE, rs, ps, null);
		}
	}

	private void addInfraction(Infraction reason, Assigner type, String reporter, String message, long overrideExpire, short overridePoints, boolean dcOnBan) {
		long now = System.currentTimeMillis();
		short points = overridePoints == -1 ? reason.points() : overridePoints;
		loadLock.lock();
		try {
			//only load past infractions if we want to add one (in order to
			//determine if the user has reached its infraction limit). that way,
			//we don't have to do an expensive SQL query if we're only getting
			//an instance of CheatTracker to log timestamps
			if (!infractionsLoaded)
				load();
		} finally {
			loadLock.unlock();
		}
		totalPoints += points;
		Connection con = null;
		PreparedStatement ps = null;
		try {
			con = DatabaseManager.getConnection(DatabaseType.STATE);
			ps = con.prepareStatement("INSERT INTO `infractions` (`accountid`,`characterid`,`receivedate`,`expiredate`,`assignertype`,`assignername`,`assignercomment`,`reason`,`severity`) VALUES (?,?,?,?,?,?,?,?,?)");
			ps.setInt(1, getAccountId());
			int cid = getCharacterId();
			if (cid != -1)
				ps.setInt(2, cid);
			else
				ps.setNull(2, Types.INTEGER);
			ps.setLong(3, now);
			ps.setLong(4, overrideExpire == -1L ? (now + reason.duration()) : overrideExpire);
			ps.setString(5, type.sqlName());
			ps.setString(6, reporter);
			ps.setString(7, message);
			ps.setByte(8, reason.byteValue());
			ps.setShort(9, points);
			ps.executeUpdate();
			/*if (totalPoints >= TOLERANCE) {
				ban(con);
				if (dcOnBan)
					client.getSession().close("Banned", null);
			}*/
		} catch (SQLException ex) {
			LOG.log(Level.WARNING, "Could not load cheatlog for account "
					+ getAccountId(), ex);
		} finally {
			DatabaseManager.cleanup(DatabaseType.STATE, null, ps, con);
		}
	}

	public void suspicious(Infraction reason, String details) {
		StackTraceElement caller = Thread.currentThread().getStackTrace()[2];
		addInfraction(reason, Assigner.AUTOBAN, caller.toString(), details, -1L, (short) -1, true);
	}

	public void logTime(String key, long timeStamp) {
		timeLog.put(key, Long.valueOf(timeStamp));
	}

	/**
	 *
	 * @param key
	 * @return 0 if no time was logged for the key
	 */
	public long getLoggedTime(String key) {
		Long t = timeLog.get(key);
		return t != null ? t.longValue() : 0;
	}

	private int getAccountId() {
		return client.getAccountId();
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
	 * Get the character that this client is associated with.
	 * @return null if this client isn't associated with a player
	 */
	private Player getCharacter() {
		return client.getPlayer();
	}

	/**
	 * Get the id of the character that this client is logged in on.
	 * @return -1 if no player exists on this client
	 */
	private int getCharacterId() {
		Player p = getCharacter();
		return (p != null ? p.getId() : -1);
	}

	public static CheatTracker get(RemoteClient rc) {
		CheatTracker ct = recent.getWhenSafe(rc); //try getting from cache first
		if (ct == null) {
			ct = new CheatTracker(rc);
			recent.putWhenSafe(rc, ct);
		}
		return ct;
	}
}
