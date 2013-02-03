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

package argonms.login.net.external;

import argonms.common.ServerType;
import argonms.common.net.HashFunctions;
import argonms.common.net.external.CheatTracker;
import argonms.common.net.external.RemoteClient;
import argonms.common.util.DatabaseManager;
import argonms.common.util.DatabaseManager.DatabaseType;
import argonms.common.util.TimeTool;
import argonms.login.character.LoginCharacter;
import java.net.InetSocketAddress;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author GoldenKevin
 */
public class LoginClient extends RemoteClient {
	private static final Logger LOG = Logger.getLogger(LoginClient.class.getName());

	public static final byte
		GENDER_MALE = 0,
		GENDER_FEMALE = 1,
		GENDER_UNDEFINED = 0x0A
	;

	private static final byte
		DELETE_OKAY = 0,
		DELETE_ERROR_SYSTEM = 6,
		DELETE_ERROR_GENERAL = 9,
		DELETE_ERROR_WRONG_BIRTHDAY = 18,
		DELETE_ERROR_GUILD_MASTER = 22,
		DELETE_ERROR_IMPENDING_WEDDING = 24,
		DELETE_ERROR_IMPENDING_WORLD_TRANSFER = 26
	;

	private String pin;
	private byte gender;
	private int birthday;
	private byte chars;
	private long banExpire;
	/**
	 * 00 = This is an ID that has been deleted or blocked from connection
	 * 01 = Your account has been blocked for hacking or illegal use of third-party programs
	 * 02 = Your account has been blocked for using macro/auto-keyboard
	 * 03 = Your account has been blocked for illicit promotion or advertising
	 * 04 = Your account has been blocked for harassment
	 * 05 = Your account has been blocked for using profane language
	 * 06 = Your account has been blocked for scamming
	 * 07 = Your account has been blocked for misconduct
	 * 08 = Your account has been blocked for illegal cash transaction
	 * 09 = Your account has been blocked for illegal charging/funding. Please contact customer support for further details
	 * 10 = Your account has been blocked for temporary request. Please contact customer support for further details
	 * 11 = Your account has been blocked for impersonating GM
	 * 12 = Your account has been blocked for using illegal programs or violating the game policy
	 * 13 = Your account has been blocked for one of cursing, scamming, or illegal trading via Megaphones.
	 */
	private byte banReason;
	private byte gm;

	/**
	 * Get the network address of the remote client.
	 * @return the IP address of the remote client in big-endian
	 * byte order.
	 */
	private long getIpAddress() {
		byte[] bigEndian = ((InetSocketAddress) getSession().getAddress()).getAddress().getAddress();

		//IP addresses are just 4-byte (32-bit) integers represented by 4 bytes
		//in big endian
		//since singed ints can only hold 31-bit without overflow, and Java
		//doesn't have unsigned int, use signed long (63-bit)
		long longValue = 0;
		for (int byt = 0, bitShift = 24; byt < 4; byt++, bitShift -= 8)
			longValue += (long) (bigEndian[byt] & 0xFF) << bitShift;
		return longValue;
	}

	private CheatTracker.Infraction loadBanStatusInternal(Connection con, ResultSet rs) throws SQLException {
		EnumMap<CheatTracker.Infraction, Integer> infractionPoints = new EnumMap<CheatTracker.Infraction, Integer>(CheatTracker.Infraction.class);
		int highestPoints = 0;
		CheatTracker.Infraction mainBanReason = null;

		PreparedStatement ips = null, rbps = null;
		ResultSet irs;

		try {
			//load only non-expired infractions - even though the ban
			//may have been given with infractions that have already
			//expired, the longest lasting infraction will still remain
			//the same, and we could just choose the most outstanding
			//points from the active infractions to send to the client
			ips = con.prepareStatement("SELECT `expiredate`,`reason`,`severity` FROM `infractions` WHERE `accountid` = ? AND `pardoned` = 0 AND `expiredate` > (UNIX_TIMESTAMP() * 1000) ORDER BY `expiredate` DESC");
			rbps = con.prepareStatement("DELETE FROM `bans` WHERE `banid` = ?");
			//there could be multiple bans (account bans, some mac, and others IP)
			//if that's the case, load all of them and calculate the longest lasting
			//infraction and the most outstanding infraction reason from a union
			//of the infractions of all bans.
			while (rs.next()) {
				boolean release = true;
				int totalPoints = 0;
				ips.setInt(1, rs.getInt(2));
				irs = null;
				try {
					irs = ips.executeQuery();
					while (irs.next()) {
						long infractionExpire = irs.getLong(1);
						CheatTracker.Infraction infractionReason = CheatTracker.Infraction.valueOf(irs.getByte(2));
						short severity = irs.getShort(3);

						//ban expire time is based on the shortest amount of
						//time before total points goes below tolerance.
						//assume ResultSet is iterated in order of `expiredate`
						//descending
						if (release && (totalPoints += severity) >= CheatTracker.TOLERANCE) {
							banExpire = infractionExpire;
							release = false;
						}

						//meanwhile, the ban reason sent the client doesn't have
						//to be paired up to the longest lasting ban. instead,
						//send the reason that is most responsible for the ban
						//(i.e. the reason that has the highest sum of points)
						Integer runningPoints = infractionPoints.get(infractionReason);
						int updatedPoints = ((runningPoints != null ? runningPoints.intValue() : 0) + severity);
						infractionPoints.put(infractionReason, Integer.valueOf(updatedPoints));
						if (updatedPoints > highestPoints) {
							highestPoints = updatedPoints;
							mainBanReason = infractionReason;
						}
					}
				} finally {
					DatabaseManager.cleanup(DatabaseType.STATE, irs, null, null);
				}
				if (release) {
					rbps.setInt(1, rs.getInt(1));
					rbps.addBatch();
				}
			}
			rbps.executeBatch();
		} finally {
			DatabaseManager.cleanup(DatabaseType.STATE, null, rbps, null);
			DatabaseManager.cleanup(DatabaseType.STATE, null, ips, null);
		}
		return mainBanReason;
	}

	private void loadBanStatusFromIdAndIp(Connection con) throws SQLException {
		banExpire = 0;

		CheatTracker.Infraction banStatus;

		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = con.prepareStatement("SELECT `banid`,`accountid` FROM `bans` WHERE `accountid` = ? OR `ip` = ?");
			ps.setInt(1, getAccountId());
			ps.setLong(2, getIpAddress());
			rs = ps.executeQuery();
			banStatus = loadBanStatusInternal(con, rs);
		} finally {
			DatabaseManager.cleanup(DatabaseType.STATE, rs, ps, null);
		}
		banReason = banStatus == null ? 0 : banStatus.byteValue();
	}

	private void loadBanStatusFromBanId(Connection con, int banId) throws SQLException {
		banExpire = 0;

		CheatTracker.Infraction banStatus;

		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = con.prepareStatement("SELECT `banid`,`accountid` FROM `bans` WHERE `banid` = ?");
			ps.setInt(1, banId);
			rs = ps.executeQuery();
			banStatus = loadBanStatusInternal(con, rs);
		} finally {
			DatabaseManager.cleanup(DatabaseType.STATE, rs, ps, null);
		}
		banReason = banStatus == null ? 0 : banStatus.byteValue();
	}

	//TODO: Maybe we shouldn't reload all of the account data if we already
	//tried logging in during this session.
	/**
	 * 0 = ok
	 * 2 = banned
	 * 4 = wrong password
	 * 5 = no account exists
	 * 7 = already logged in
	 * 8 = system error
	 */
	public byte loginResult(String pwd) {
		byte result;
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			con = DatabaseManager.getConnection(DatabaseType.STATE);
			ps = con.prepareStatement("SELECT `id`,`password`,`salt`,`pin`,`gender`,`birthday`,`characters`,`connected`,`gm` FROM `accounts` WHERE `name` = ?");
			ps.setString(1, getAccountName());
			rs = ps.executeQuery();
			if (rs.next()) {
				setAccountId(rs.getInt(1));
				byte[] passhash = rs.getBytes(2);
				byte[] salt = rs.getBytes(3);
				pin = rs.getString(4);
				gender = rs.getByte(5);
				birthday = rs.getInt(6);
				chars = rs.getByte(7);
				byte onlineStatus = rs.getByte(8);
				gm = rs.getByte(9);
				loadBanStatusFromIdAndIp(con);

				boolean correct, hashUpdate, hasSalt = (salt != null);
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
					if (onlineStatus != STATUS_NOTLOGGEDIN) {
						//TODO: there is a high chance that the player is not really
						//in game if they have an onlineStatus of STATUS_MIGRATION.
						//Make a way to check if they really are/will be connected
						//to a server
						result = 7;
					} else if (banExpire > System.currentTimeMillis()) {
						result = 2;
					} else {
						rs.close();
						ps.close();
						if (hashUpdate) {
							salt = HashFunctions.makeSalt();
							passhash = HashFunctions.makeSaltedSha512Hash(pwd, salt);
							ps = con.prepareStatement("UPDATE `accounts` SET `connected` = ?, `password` = ?, `salt` = ? WHERE `id` = ?");
							ps.setByte(1, STATUS_INLOGIN);
							ps.setBytes(2, passhash);
							ps.setBytes(3, salt);
							ps.setInt(4, getAccountId());
						} else {
							ps = con.prepareStatement("UPDATE `accounts` SET `connected` = ? WHERE `id` = ?");
							ps.setByte(1, STATUS_INLOGIN);
							ps.setInt(2, getAccountId());
						}
						ps.executeUpdate();
						result = 0;
					}
				} else {
					result = 4;
				}
			} else {
				result = 5;
			}
		} catch (SQLException ex) {
			LOG.log(Level.SEVERE, "Could not fetch login information of account " + getAccountName(), ex);
			result = 8;
		} finally {
			DatabaseManager.cleanup(DatabaseType.STATE, rs, ps, con);
		}
		return result;
	}

	public byte getGender() {
		return gender;
	}

	public byte getMaxCharacters() {
		return chars;
	}

	public byte getBanReason() {
		return banReason;
	}

	public long getBanExpiration() {
		return TimeTool.unixToWindowsTime(banExpire);
	}

	public String getPin() {
		return pin;
	}

	public byte getGm() {
		return gm;
	}

	public void setGender(byte gender) {
		this.gender = gender;
		Connection con = null;
		PreparedStatement ps = null;
		try {
			con = DatabaseManager.getConnection(DatabaseType.STATE);
			ps = con.prepareStatement("UPDATE `accounts` SET `gender` = ? WHERE `id` = ?");
			ps.setByte(1, gender);
			ps.setInt(2, getAccountId());
			ps.executeUpdate();
		} catch (SQLException ex) {
			LOG.log(Level.WARNING, "Could not set gender of account " + getAccountId(), ex);
		} finally {
			DatabaseManager.cleanup(DatabaseType.STATE, null, ps, con);
		}
	}

	public void setPin(String pin) {
		this.pin = pin;
		Connection con = null;
		PreparedStatement ps = null;
		try {
			con = DatabaseManager.getConnection(DatabaseType.STATE);
			ps = con.prepareStatement("UPDATE `accounts` SET `pin` = ? WHERE `id` = ?");
			ps.setString(1, pin);
			ps.setInt(2, getAccountId());
			ps.executeUpdate();
		} catch (SQLException ex) {
			LOG.log(Level.WARNING, "Could not set pin of account " + getAccountId(), ex);
		} finally {
			DatabaseManager.cleanup(DatabaseType.STATE, null, ps, con);
		}
	}

	public byte deleteCharacter(int characterid, int enteredBirthday) {
		if (birthday != 0 && birthday != enteredBirthday)
			return DELETE_ERROR_WRONG_BIRTHDAY;

		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			con = DatabaseManager.getConnection(DatabaseType.STATE);
			ps = con.prepareStatement("SELECT EXISTS(SELECT 1 FROM `guildmembers` WHERE `characterid` = ? AND `rank` = 1 LIMIT 1)");
			ps.setInt(1, characterid);
			rs = ps.executeQuery();
			rs.next();
			if (rs.getBoolean(1))
				return DELETE_ERROR_GUILD_MASTER;

			ps = con.prepareStatement("DELETE FROM `characters` WHERE `id` = ?");
			ps.setInt(1, characterid);
			int rowsUpdated = ps.executeUpdate();
			if (rowsUpdated != 0)
				return DELETE_OKAY;
			else
				return DELETE_ERROR_SYSTEM;
		} catch (SQLException ex) {
			LOG.log(Level.WARNING, "Could not delete character " + characterid + " of account " + getAccountId(), ex);
			return DELETE_ERROR_SYSTEM;
		} finally {
			DatabaseManager.cleanup(DatabaseType.STATE, null, ps, con);
		}
	}

	private byte[] macStringToBytes(String str) {
		//MAC addresses are just 6-byte (48-bit) integers commonly notated by 6
		//hexadecimal bytes in big endian, delimited by a single character
		//(hyphens in MapleStory)
		byte[] bytes = new byte[6];
		for (int byt = 0, strStart = 0; byt < 6; byt++, strStart += 3)
			bytes[byt] = (byte) Short.parseShort(str.substring(strStart, strStart + 2), 16);
		return bytes;
	}

	public boolean hasBannedMac(String macData) {
		//storing macs in binary saves us 2 bytes per address compared to if we
		//used a more readable 8-byte/64-bit signed integer
		String[] macStrings = macData.split(", ");
		byte[][] macListArray = new byte[macStrings.length][];
		byte[] macListCombined = new byte[macStrings.length * 6];
		StringBuilder checkBanQuery = new StringBuilder("SELECT `banid` FROM `macbans` WHERE `mac` IN (");
		byte[] mac;
		for (int i = 0; i < macListArray.length; i++) {
			mac = macStringToBytes(macStrings[i]);
			System.arraycopy(mac, 0, macListCombined, i * 6, 6);
			macListArray[i] = mac;
			checkBanQuery.append("?, ");
		}
		if (macListArray.length > 0) {
			int length = checkBanQuery.length();
			checkBanQuery.replace(length - 2, length, ")");
		}

		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			con = DatabaseManager.getConnection(DatabaseType.STATE);

			ps = con.prepareStatement("UPDATE `accounts` SET `recentmacs` = ?, `recentip` = ? WHERE `id` = ?");
			ps.setBytes(1, macListCombined);
			ps.setLong(2, getIpAddress());
			ps.setInt(3, getAccountId());
			ps.executeUpdate();
			ps.close();

			ps = con.prepareStatement(checkBanQuery.toString());
			for (int i = 0; i < macListArray.length; i++)
				ps.setBytes(i + 1, macListArray[i]);
			rs = ps.executeQuery();
			//don't load duplicate ban ids
			Set<Integer> banIds = new HashSet<Integer>();
			while (rs.next())
				banIds.add(Integer.valueOf(rs.getInt(1)));
			for (Integer banId : banIds) {
				loadBanStatusFromBanId(con, banId.intValue());
				if (banExpire > System.currentTimeMillis())
					return true;
			}
			return false;
		} catch (SQLException e) {
			LOG.log(Level.WARNING, "Could not update and check MAC addresses of account " + getAccountId(), e);
			return false;
		} finally {
			DatabaseManager.cleanup(DatabaseType.STATE, rs, ps, con);
		}
	}

	@Override
	public LoginCharacter getPlayer() {
		return null;
	}

	@Override
	public byte getServerId() {
		return ServerType.LOGIN;
	}

	private void dissociate() {
		getSession().removeClient();
		setSession(null);
	}

	@Override
	public void disconnected() {
		if (getSession().getQueuedReads() == 0) {
			dissociate();
		} else {
			getSession().setEmptyReadQueueHandler(new Runnable() {
				@Override
				public void run() {
					dissociate();
				}
			});
		}
		if (!isMigrating() && getAccountId() != 0)
			updateState(STATUS_NOTLOGGEDIN);
	}
}
