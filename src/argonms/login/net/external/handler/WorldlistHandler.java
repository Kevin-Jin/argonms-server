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

package argonms.login.net.external.handler;

import argonms.common.UserPrivileges;
import argonms.common.character.PlayerJob;
import argonms.common.net.external.CheatTracker;
import argonms.common.net.external.ClientSendOps;
import argonms.common.net.external.CommonPackets;
import argonms.common.util.DatabaseManager;
import argonms.common.util.DatabaseManager.DatabaseType;
import argonms.common.util.input.LittleEndianReader;
import argonms.common.util.output.LittleEndianByteArrayWriter;
import argonms.common.util.output.LittleEndianWriter;
import argonms.login.BalloonMessage;
import argonms.login.LoginServer;
import argonms.login.character.LoginCharacter;
import argonms.login.net.LoginWorld;
import argonms.login.net.external.LoginClient;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author GoldenKevin
 */
public final class WorldlistHandler {
	private static final Logger LOG = Logger.getLogger(WorldlistHandler.class.getName());

	private static final byte
		SERVERSTATUS_OK = 0, //No warning
		SERVERSTATUS_WARNING = 1, //"Since There Are Many Concurrent Users in This World, You May Encounter Some Difficulties During the Game Play."
		SERVERSTATUS_MAX = 2 //"The Concurrent Users in This World Have Reached the Max. Please Try Again Later."
	;

	private static void loadAndWriteCharacters(LittleEndianWriter lew, LoginClient c) {
		ArrayList<LoginCharacter> players = new ArrayList<LoginCharacter>(c.getMaxCharacters());
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			con = DatabaseManager.getConnection(DatabaseType.STATE);
			ps = con.prepareStatement("SELECT `id` FROM `characters` WHERE `accountid` = ? AND `world` = ?");
			ps.setInt(1, c.getAccountId());
			ps.setInt(2, c.getWorld());
			rs = ps.executeQuery();
			while (rs.next())
				players.add(LoginCharacter.loadPlayer(c, rs.getInt(1)));
		} catch (SQLException ex) {
			LOG.log(Level.WARNING, "Could not load characters of account " + c.getAccountId(), ex);
		} finally {
			DatabaseManager.cleanup(DatabaseType.STATE, rs, ps, con);
		}
		lew.writeByte((byte) players.size());
		for (LoginCharacter p : players)
			writeCharEntry(lew, p);
	}

	public static void handleCharlist(LittleEndianReader packet, LoginClient lc) {
		lc.setWorld(packet.readByte());
		lc.setChannel((byte) (packet.readByte() + 1));

		LittleEndianByteArrayWriter writer = new LittleEndianByteArrayWriter();
		writer.writeShort(ClientSendOps.CHARLIST);
		LoginWorld w = LoginServer.getInstance().getWorld(lc.getWorld());
		if (w == null || w.getPort(lc.getChannel()) == -1) {
			writer.writeByte((byte) 8); //"The connection could not be made because of a system error."
		} else {
			writer.writeByte((byte) 0); //show characters
			loadAndWriteCharacters(writer, lc);
			writer.writeInt(lc.getMaxCharacters());
		}

		lc.getSession().send(writer.getBytes());
	}

	public static void sendServerStatus(LittleEndianReader packet, LoginClient lc) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(4);
		lew.writeShort(ClientSendOps.SERVERLOAD_MSG);

		LoginWorld w = LoginServer.getInstance().getWorld(Byte.valueOf(lc.getWorld()));
		int collectiveLoads = w.getTotalLoad();

		//each channel can hold 2400
		int max = 2400 * w.getChannelCount();
		if (collectiveLoads >= max)
			lew.writeShort(SERVERSTATUS_MAX);
		else if (collectiveLoads >= 0.9 * max) // >90% full
			lew.writeShort(SERVERSTATUS_WARNING);
		else
			lew.writeShort(SERVERSTATUS_OK);
		lc.getSession().send(lew.getBytes());
	}

	public static void handleWorldListRequest(LittleEndianReader packet, LoginClient rc) {
		for (Entry<Byte, LoginWorld> entry : LoginServer.getInstance().getAllWorlds().entrySet())
			rc.getSession().send(worldEntry(entry.getKey().byteValue(), entry.getValue()));
		rc.getSession().send(worldListEnd());
	}

	public static void handleViewAllChars(LittleEndianReader packet, LoginClient lc) {
		LittleEndianByteArrayWriter lew;
		Set<Byte> worlds = new TreeSet<Byte>();
		byte totalChars = 0;
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			con = DatabaseManager.getConnection(DatabaseType.STATE);
			ps = con.prepareStatement("SELECT `world` FROM `characters` WHERE `accountid` = ?");
			ps.setInt(1, lc.getAccountId());
			rs = ps.executeQuery();
			while (rs.next()) {
				byte world = rs.getByte(1);
				if (LoginServer.getInstance().getWorld(world) != null) {
					worlds.add(Byte.valueOf(world));
					totalChars++;
				}
			}
		} catch (SQLException ex) {
			LOG.log(Level.WARNING, "Could not set load all characters of account " + lc.getAccountId(), ex);
		} finally {
			DatabaseManager.cleanup(DatabaseType.STATE, rs, ps, con);
		}
		lew = new LittleEndianByteArrayWriter(11);
		lew.writeShort(ClientSendOps.ALL_CHARLIST);
		lew.writeByte((byte) 1);
		lew.writeInt(totalChars);
		lew.writeInt(totalChars + (3 - totalChars % 3)); //amount of rows * 3
		lc.getSession().send(lew.getBytes());

		for (Byte world : worlds) {
			lew = new LittleEndianByteArrayWriter(192);
			lew.writeShort(ClientSendOps.ALL_CHARLIST);
			lew.writeByte((byte) 0);
			lew.writeByte(world.byteValue());
			lc.setWorld(world.byteValue());
			loadAndWriteCharacters(lew, lc);
			lc.getSession().send(lew.getBytes());
		}
	}

	private static void sendToGameServer(LoginClient c, int charid, String macs) {
		if (c.hasBannedMac(macs)) {
			c.getSession().close("Banned", null);
			return;
		}
		LoginWorld w = LoginServer.getInstance().getWorld(c.getWorld());
		//TODO: if client can't connect to the game server, then it's connected
		//state will always be STATUS_MIGRATION. There's got to be a way to
		//find out if the client's connection timed out and then
		//updateState(STATUS_NOTLOGGEDIN) if it is.
		c.setMigratingHost();
		c.getSession().send(writeServerAddress(w.getHost(c.getChannel()), w.getPort(c.getChannel()), charid));
	}

	public static void handlePickFromAllChars(LittleEndianReader packet, LoginClient lc) {
		int charid = packet.readInt();
		byte world = (byte) packet.readInt();
		String macs = packet.readLengthPrefixedString();
		//packet.readLengthPrefixedString(); what the hell is this?
		lc.setWorld(world);
		lc.setChannel(LoginServer.getInstance().getWorld(world).getLeastPopulatedChannel());
		sendToGameServer(lc, charid, macs);
	}

	public static void handlePickFromWorldCharlist(LittleEndianReader packet, LoginClient lc) {
		int charid = packet.readInt();
		String macs = packet.readLengthPrefixedString();
		//packet.readLengthPrefixedString(); what the hell is this?
		sendToGameServer(lc, charid, macs);
	}

	private static boolean allowedName(String name) {
		boolean valid = true;
		if (name.length() < 4 || name.length() > 12) {
			valid = false;
		} else {
			Connection con = null;
			PreparedStatement ps = null;
			ResultSet rs = null;
			try {
				con = DatabaseManager.getConnection(DatabaseType.STATE);
				ps = con.prepareStatement("SELECT `id` FROM `characters` WHERE `name` = ?");
				ps.setString(1, name);
				rs = ps.executeQuery();
				valid = !rs.next();
			} catch (SQLException ex) {
				LOG.log(Level.WARNING, "Could not determine if name " + name + " is allowed.", ex);
				valid = false;
			} finally {
				DatabaseManager.cleanup(DatabaseType.STATE, rs, ps, con);
			}
		}
		return valid;
	}

	public static void handleNameCheck(LittleEndianReader packet, LoginClient rc) {
		String name = packet.readLengthPrefixedString();
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(5 + name.length());
		lew.writeShort(ClientSendOps.CHECK_NAME_RESP);
		lew.writeLengthPrefixedString(name);
		lew.writeBool(!allowedName(name));
		rc.getSession().send(lew.getBytes());
	}

	public static void handleCreateCharacter(LittleEndianReader packet, LoginClient lc) {
		String name = packet.readLengthPrefixedString();
		int eyes = packet.readInt();
		int hair = packet.readInt();
		int hairColor = packet.readInt();
		int skin = packet.readInt();
		int top = packet.readInt();
		int bottom = packet.readInt();
		int shoes = packet.readInt();
		int weapon = packet.readInt();
		byte gender = packet.readByte();
		byte str = packet.readByte();
		byte dex = packet.readByte();
		byte _int = packet.readByte();
		byte luk = packet.readByte();

		boolean valid = allowedName(name);
		if (str + dex + _int + luk != 25 || str < 4 || dex < 4 || _int < 4 || luk < 4)
			valid = false;
		if (gender == 0)
			if ((eyes < 20000 || eyes > 20002) || (hair != 30000 && hair != 30020 && hair != 30030) ||
					(top != 1040002 && top != 1040006 && top != 1040010) || (bottom != 1060006 && bottom != 1060002))
				valid = false;
		else if (gender == 1)
			if ((eyes < 21000 || eyes > 21002) || (hair != 31000 && hair != 31040 && hair != 31050) ||
					(top != 1041002 && top != 1041006 && top != 1041010 && top != 1041011) || (bottom != 1061002 && bottom != 1061008))
				valid = false;
		else
			valid = false;
		if ((skin < 0 || skin > 3) || (weapon != 1302000 && weapon != 1322005 && weapon != 1312004) ||
				(shoes != 1072001 && shoes != 1072005 && shoes != 1072037 && shoes != 1072038) ||
				(hairColor != 0 && hairColor != 2 && hairColor != 3 && hairColor != 7))
			valid = false;

		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter();
		lew.writeShort(ClientSendOps.CHAR_CREATED);
		lew.writeBool(!valid);
		if (valid) {
			LoginCharacter p = LoginCharacter.saveNewPlayer(lc, name,
					eyes, hair + hairColor, skin, gender, str, dex, _int, luk, top, bottom, shoes, weapon);
			writeCharEntry(lew, p);
		} else {
			CheatTracker.get(lc).suspicious(CheatTracker.Infraction.PACKET_EDITING, "Tried to create a stats hacked character (" + name + ")");
		}
		lc.getSession().send(lew.getBytes());
	}

	public static void handleDeleteChar(LittleEndianReader packet, LoginClient lc) {
		int date = packet.readInt();
		int cid = packet.readInt();

		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(7);
		lew.writeShort(ClientSendOps.DELETE_CHAR_RESPONSE);
		lew.writeInt(cid);
		lew.writeByte(lc.deleteCharacter(cid, date));
		lc.getSession().send(lew.getBytes());
	}

	public static void backToLogin(LittleEndianReader packet, LoginClient rc) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(3);
		lew.writeShort(ClientSendOps.RELOG_RESPONSE);
		lew.writeByte((byte) 1);
		rc.getSession().send(lew.getBytes());
	}

	private static byte[] worldEntry(byte world, LoginWorld w) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(128);

		lew.writeShort(ClientSendOps.WORLD_ENTRY);
		lew.writeByte(world);
		lew.writeLengthPrefixedString(w.getName());
		lew.writeByte(w.getFlag());
		lew.writeLengthPrefixedString(w.getChannelListMessage());
		lew.writeByte((byte) 100); // rate modifier
		lew.writeByte((byte) 0); // event xp * 2.6
		lew.writeByte((byte) 100); // rate modifier
		lew.writeByte((byte) 0); // drop rate * 2.6
		lew.writeByte((byte) 0);

		byte max = w.getLargestNumberedChannel();
		lew.writeByte(max);
		for (byte b = 1; b <= max; b++) {
			lew.writeLengthPrefixedString(w.getName() + '-' + b);
			lew.writeInt(w.getLoad(b));
			lew.writeByte(world);
			lew.writeByte(b);
			lew.writeBool(false);
		}

		//we really gotta find a way to show empty channels -
		//giving the user an error upon clicking a non loaded channel is not good.
		/*lew.writeByte((byte) w.getChannelCount());
		for (Entry<Byte, Load> entry : w.getAllLoads().entrySet()) {
			lew.writeLengthPrefixedString(w.getName() + '-' + entry.getKey());
			lew.writeInt(entry.getValue().value());
			lew.writeByte(world);
			lew.writeShort(entry.getKey().byteValue());
		}*/

		List<BalloonMessage> messages = LoginServer.getInstance().getWorldListMessages();
		lew.writeShort((short) messages.size()); //num of messages
		for (BalloonMessage msg : messages) {
			lew.writePos(msg.getPosition()); //(0, 0) = on Scania
			lew.writeLengthPrefixedString(msg.getText());
		}

		return lew.getBytes();
	}

	private static byte[] worldListEnd() {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(3);

		lew.writeShort(ClientSendOps.WORLD_ENTRY);
		lew.writeByte((byte) 0xFF);

		return lew.getBytes();
	}

	private static byte[] writeServerAddress(byte[] host, int port, int charid) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(19);
		lew.writeShort(ClientSendOps.CHANNEL_ADDRESS);
		lew.writeShort((short) 0);
		lew.writeBytes(host);
		lew.writeShort((short) port);
		lew.writeInt(charid);
		lew.writeInt(0);
		lew.writeByte((byte) 0);
		return lew.getBytes();
	}

	private static void writeCharEntry(LittleEndianWriter lew, LoginCharacter p) {
		CommonPackets.writeCharStats(lew, p);
		CommonPackets.writeAvatar(lew, p, false);
		if (!PlayerJob.isModerator(p.getJob()) && p.getPrivilegeLevel() <= UserPrivileges.USER) {
			lew.writeBool(true);
			lew.writeInt(p.getWorldRank());
			lew.writeInt(p.getWorldRankChange());
			lew.writeInt(p.getJobRank());
			lew.writeInt(p.getJobRankChange());
		} else {
			lew.writeBool(false);
		}
	}

	private WorldlistHandler() {
		//uninstantiable...
	}
}
