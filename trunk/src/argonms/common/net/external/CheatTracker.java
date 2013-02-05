/*
 * ArgonMS MapleStory server emulator written in Java
 * Copyright (C) 2011-2013  GoldenKevin
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
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
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
public abstract class CheatTracker {
	private static final Logger LOG = Logger.getLogger(CheatTracker.class.getName());

	public static final short TOLERANCE = 10000;

	private static LockableMap<RemoteClient, OnlineCheatTracker> recent;

	static {
		recent = new LockableMap<RemoteClient, OnlineCheatTracker>(new WeakHashMap<RemoteClient, OnlineCheatTracker>());
	}

	public enum Infraction {
		POSSIBLE_PACKET_EDITING	(1, TOLERANCE / 1000, 30L * 24 * 60 * 60 * 1000), //30 days
		CERTAIN_PACKET_EDITING	(2, TOLERANCE / 2, 60L * 24 * 60 * 60 * 1000); //30 days

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

	private final Lock loadLock;
	private final Map<String, Long> timeLog;
	private final AtomicBoolean banned;
	private int totalPoints;
	private boolean infractionsLoaded;

	private CheatTracker() {
		this.loadLock = new ReentrantLock();
		this.timeLog = new ConcurrentHashMap<String, Long>();
		this.banned = new AtomicBoolean(false);
		this.totalPoints = 0;
		this.infractionsLoaded = false;
	}

	protected abstract void disconnectClient();

	protected abstract int getAccountId();

	protected abstract byte[] getIpAddress();

	/**
	 * Get the id of the character that this client is logged in on.
	 * @return -1 if no player exists on this client
	 */
	protected abstract int getCharacterId();

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
			ps = con.prepareStatement("SELECT `severity` FROM `infractions` "
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
		if (!banned.compareAndSet(false, true))
			return;

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
		if (banned.get())
			return;

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
			if (totalPoints >= TOLERANCE) {
				ban(con);
				if (dcOnBan)
					disconnectClient();
			}
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

	public void ban(Infraction reason, String callerName, String details, Calendar expire) {
		long expireTimeStamp = -1L;
		if (expire != null)
			expireTimeStamp = expire.getTimeInMillis();
		addInfraction(reason, Assigner.GM, callerName, details, expireTimeStamp, TOLERANCE, true);
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

	private static class OnlineCheatTracker extends CheatTracker {
		private final RemoteClient client;

		private OnlineCheatTracker(RemoteClient rc) {
			this.client = rc;
		}

		@Override
		protected void disconnectClient() {
			client.getSession().close("Banned");
		}

		@Override
		protected int getAccountId() {
			return client.getAccountId();
		}

		/**
		 * Get the network address of the remote client.
		 * @return the IP address of the remote client in big-endian
		 * byte order.
		 */
		@Override
		protected byte[] getIpAddress() {
			return ((InetSocketAddress) client.getSession().getAddress()).getAddress().getAddress();
		}

		/**
		 * Get the character that this client is associated with.
		 * @return null if this client isn't associated with a player
		 */
		@Override
		protected int getCharacterId() {
			Player p = client.getPlayer();
			return (p != null ? p.getId() : -1);
		}
	}

	private static class OfflineCheatTracker extends CheatTracker {
		private final int accountId;
		private final int characterId;
		private final byte[] ipAddress;

		private byte[] longToByteArray(long longValue) {
			byte[] bigEndian = new byte[4];
			for (int byt = 0, bitShift = 24; byt < 4; byt++, bitShift -= 8)
				bigEndian[byt] = (byte) ((longValue >>> bitShift) & 0xFF);
			return bigEndian;
		}

		private OfflineCheatTracker(int accountId, int characterId, long recentIp) {
			this.accountId = accountId;
			this.characterId = characterId;
			this.ipAddress = longToByteArray(recentIp);
		}

		@Override
		protected void disconnectClient() {
			
		}

		@Override
		protected int getAccountId() {
			return accountId;
		}

		/**
		 * Get the network address of the remote client.
		 * @return the IP address of the remote client in big-endian
		 * byte order.
		 */
		@Override
		protected byte[] getIpAddress() {
			return ipAddress;
		}

		/**
		 * Get the character that this client is associated with.
		 * @return null if this client isn't associated with a player
		 */
		@Override
		protected int getCharacterId() {
			return characterId;
		}
	}

	public static CheatTracker get(RemoteClient rc) {
		OnlineCheatTracker ct = recent.getWhenSafe(rc); //try getting from cache first
		if (ct == null) {
			ct = new OnlineCheatTracker(rc);
			recent.putWhenSafe(rc, ct);
		}
		return ct;
	}

	public static CheatTracker get(String characterName) {
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			con = DatabaseManager.getConnection(DatabaseType.STATE);
			//only get infractions that haven't expired and aren't pardoned yet
			ps = con.prepareStatement("SELECT `a`.`id`,`c`.`id`,`a`.`recentip` FROM `characters` `c` LEFT JOIN `accounts` `a` ON `c`.`accountid` = `a`.`id` WHERE `c`.`name` = ?");
			ps.setString(1, characterName);
			rs = ps.executeQuery();
			if (!rs.next())
				return null;

			return new OfflineCheatTracker(rs.getInt(1), rs.getInt(2), rs.getLong(3));
		} catch (SQLException ex) {
			LOG.log(Level.WARNING, "Could not load cheatlog for offline character "
					+ characterName, ex);
			return null;
		} finally {
			DatabaseManager.cleanup(DatabaseType.STATE, rs, ps, con);
		}
	}
}
