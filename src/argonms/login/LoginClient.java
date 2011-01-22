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

package argonms.login;

import argonms.net.HashFunctions;
import argonms.net.client.RemoteClient;
import argonms.tools.DatabaseConnection;
import argonms.tools.output.LittleEndianByteArrayWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
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
		GENDER_UNDEFINED = 0x0A //lawl
	;

	private String pin;
	private byte gender;
	private int birthday;
	private byte chars;
	private int banExpire;
	private byte banReason;

	/*
	 * 0 = ok
	 * 2 = banned
	 * 4 = wrong password
	 * 5 = no account exists
	 * 7 = already logged in
	 * 8 = system error
	 * Maybe we shouldn't reload all of the account data if we already tried
	 * logging in during this session.
	 */
	public byte loginResult(String pwd) {
		boolean hashUpdate = false;
		byte result = 0;
		Connection con = DatabaseConnection.getConnection();
		try {
			PreparedStatement ps = con.prepareStatement("SELECT `id`,`password`,`salt`,`pin`,`gender`,`birthday`,`characters`,`connected`,`banexpire`,`banreason` FROM `accounts` WHERE `name` = ?");
			ps.setString(1, getAccountName());
			ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				setAccountId(rs.getInt(1));
				String passhash = rs.getString(2);
				String salt = rs.getString(3);
				pin = rs.getString(4);
				gender = rs.getByte(5);
				if (rs.wasNull())
					gender = GENDER_UNDEFINED;
				birthday = rs.getInt(6);
				chars = rs.getByte(7);
				if (rs.wasNull())
					chars = 3;
				byte onlineStatus = rs.getByte(8);
				banExpire = rs.getInt(9);
				banReason = rs.getByte(10);
				rs.close();
				ps.close();
				if ((salt == null || salt.length() == 0) && (passhash.equals(pwd) || HashFunctions.checkSha1Hash(passhash, pwd))) {
					hashUpdate = true;
				} else if (!HashFunctions.checkSaltedSha512Hash(passhash, pwd, salt)) {
					result = 4;
				} else if (onlineStatus != STATUS_NOTLOGGEDIN) {
					result = 7;
				} else if (banExpire > 0) {
					result = 2;
				}

				if (hashUpdate) {
					salt = HashFunctions.makeSalt();
					passhash = HashFunctions.makeSaltedSha512Hash(pwd, salt);
					ps = con.prepareStatement("UPDATE `accounts` SET `connected` = ?, `password` = ?, `salt` = ? WHERE `id` = ?");
					ps.setByte(1, STATUS_INLOGIN);
					ps.setString(2, passhash);
					ps.setString(3, salt);
					ps.setInt(4, getAccountId());
				} else {
					ps = con.prepareStatement("UPDATE `accounts` SET `connected` = ? WHERE `id` = ?");
					ps.setByte(1, STATUS_INLOGIN);
					ps.setInt(2, getAccountId());
				}
				ps.executeUpdate();
				ps.close();
			} else {
				result = 5;
			}
		} catch (SQLException ex) {
			LOG.log(Level.WARNING, "Could not fetch login information of account " + getAccountName(), ex);
			result = 8;
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

	public int getBanExpiration() {
		return banExpire;
	}

	public String getPin() {
		return pin;
	}

	public void setGender(byte gender) {
		this.gender = gender;
		Connection con = DatabaseConnection.getConnection();
		try {
			PreparedStatement ps = con.prepareStatement("UPDATE `accounts` SET `gender` = ? WHERE `id` = ?");
			ps.setByte(1, gender);
			ps.setInt(2, getAccountId());
			ps.executeUpdate();
			ps.close();
		} catch (SQLException ex) {
			LOG.log(Level.WARNING, "Could not set gender of account " + getAccountId(), ex);
		}
		
	}

	public void setPin(String pin) {
		this.pin = pin;
		Connection con = DatabaseConnection.getConnection();
		try {
			PreparedStatement ps = con.prepareStatement("UPDATE `accounts` SET `pin` = ? WHERE `id` = ?");
			ps.setString(1, pin);
			ps.setInt(2, getAccountId());
			ps.executeUpdate();
			ps.close();
		} catch (SQLException ex) {
			LOG.log(Level.WARNING, "Could not set pin of account " + getAccountId(), ex);
		}
	}

	public void addCharacters(LittleEndianByteArrayWriter lew) {
		Connection con = DatabaseConnection.getConnection();
		try {
			PreparedStatement ps = con.prepareStatement("SELECT count(*) FROM `characters` WHERE `accountid` = ? AND `world` = ?");
			ps.setInt(1, getAccountId());
			ps.setInt(2, getWorld());
			ResultSet rs = ps.executeQuery();
			if (rs.next())
				lew.writeByte(rs.getByte(1));
			rs.close();
			ps.close();

			ps = con.prepareStatement("SELECT * FROM `characters` WHERE `accountid` = ? AND `world` = ?");
			ps.setInt(1, getAccountId());
			ps.setInt(2, getWorld());
			rs = ps.executeQuery();
			while (rs.next()) {
				byte gender = rs.getByte(5);
				byte skin = rs.getByte(6);
				int eyes = rs.getInt(7);
				int hair = rs.getInt(8);
				lew.writeInt(rs.getInt(3)); //char id
				lew.writePaddedAsciiString(rs.getString(4), 13); //name
				lew.writeByte(gender); //gender
				lew.writeByte(skin); //skin
				lew.writeByte((byte) eyes); //eyes
				lew.writeByte((byte) hair); //hair
				lew.writeLong(0); //pet1
				lew.writeLong(0); //pet2
				lew.writeLong(0); //pet3
				lew.writeByte(rs.getByte(9)); //level
				lew.writeShort(rs.getShort(10)); //job
				lew.writeShort(rs.getShort(11)); //str
				lew.writeShort(rs.getShort(12)); //dex
				lew.writeShort(rs.getShort(13)); //int
				lew.writeShort(rs.getShort(14)); //luk
				lew.writeShort(rs.getShort(15)); //hp
				lew.writeShort(rs.getShort(16)); //max hp
				lew.writeShort(rs.getShort(17)); //mp
				lew.writeShort(rs.getShort(18)); //max mp
				lew.writeShort(rs.getShort(19)); //ap
				lew.writeShort(rs.getShort(20)); //sp
				lew.writeInt(rs.getInt(21)); //exp
				lew.writeShort(rs.getShort(22)); //fame
				lew.writeInt(rs.getInt(23)); // spouse
				lew.writeInt(rs.getInt(24)); //map
				lew.writeByte(rs.getByte(25)); //spawnpoint
				lew.writeInt(0); //unknown

				lew.writeByte(gender); //gender
				lew.writeByte(skin); //skin
				lew.writeInt(eyes); //eyes
				lew.writeBool(true); //messenger/megaphone
				lew.writeInt(hair); //hair
				//equips stuff.. blah
				lew.writeByte((byte) 0xFF); //end of visible equips
				lew.writeByte((byte) 0xFF); //end of masked equips
				lew.writeInt(0); //cash weapon
				lew.writeInt(0); //unknown
				lew.writeLong(0); //unknown

				if (rs.getBoolean(33)) { //if gm
					lew.writeByte((byte) 0); //world rank disabled
				} else {
					/*writer.WriteByte(1); // world rank enabled (next 4 ints are not sent if disabled)
					writer.WriteInt(chr.getRank()); // world rank
					writer.WriteInt(chr.getRankMove()); // move (negative is downwards)
					writer.WriteInt(chr.getJobRank()); // job rank
					writer.WriteInt(chr.getJobRankMove()); // move (negative is downwards)*/
				}
			}
			rs.close();
			ps.close();
		} catch (SQLException ex) {
			LOG.log(Level.WARNING, "Could not load characters of account " + getAccountId(), ex);
		}
	}

	public void disconnect() {
		if (getAccountId() != 0)
			updateState(STATUS_NOTLOGGEDIN);
	}
}
