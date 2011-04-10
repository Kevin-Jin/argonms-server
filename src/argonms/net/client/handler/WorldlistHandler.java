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

package argonms.net.client.handler;

import argonms.character.Player;
import argonms.character.PlayerJob;
import argonms.login.LoginClient;
import argonms.login.LoginServer;
import argonms.login.LoginWorld;
import argonms.login.Message;
import argonms.net.client.ClientSendOps;
import argonms.net.client.CommonPackets;
import argonms.net.client.RemoteClient;
import argonms.tools.DatabaseConnection;
import argonms.tools.input.LittleEndianReader;
import argonms.tools.output.LittleEndianByteArrayWriter;
import argonms.tools.output.LittleEndianWriter;

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
public class WorldlistHandler {
	private static final Logger LOG = Logger.getLogger(WorldlistHandler.class.getName());

	private static final byte
		SERVERSTATUS_OK = 0, //No warning
		SERVERSTATUS_WARNING = 1, //"Since There Are Many Concurrent Users in This World, You May Encounter Some Difficulties During the Game Play."
		SERVERSTATUS_MAX = 2 //"The Concurrent Users in This World Have Reached the Max. Please Try Again Later."
	;

	private static void loadAndWriteCharacters(LittleEndianWriter lew, LoginClient c) {
		Connection con = DatabaseConnection.getConnection();
		try {
			ArrayList<Player> players = new ArrayList<Player>(c.getMaxCharacters());

			PreparedStatement ps = con.prepareStatement("SELECT `id` FROM"
					+ "`characters` WHERE `accountid` = ? AND `world` = ?");
			ps.setInt(1, c.getAccountId());
			ps.setInt(2, c.getWorld());
			ResultSet rs = ps.executeQuery();
			while (rs.next())
				players.add(Player.loadPlayer(c, rs.getInt(1)));
			rs.close();
			ps.close();

			lew.writeByte((byte) players.size());
			for (Player p : players)
				writeCharEntry(lew, p);
		} catch (SQLException ex) {
			LOG.log(Level.WARNING, "Could not load characters of account "
					+ c.getAccountId(), ex);
		}
	}

	public static void handleCharlist(LittleEndianReader packet, RemoteClient rc) {
		LoginClient client = (LoginClient) rc;

		client.setWorld(packet.readByte());
		client.setChannel((byte) (packet.readByte() + 1));

		LittleEndianByteArrayWriter writer = new LittleEndianByteArrayWriter();
		writer.writeShort(ClientSendOps.CHARLIST);
		LoginWorld w = LoginServer.getInstance().getWorld(client.getWorld());
		if (w == null || w.getPort(client.getChannel()) == -1) {
			writer.writeByte((byte) 8); //"The connection could not be made because of a system error."
		} else {
			writer.writeByte((byte) 0); //show characters
			loadAndWriteCharacters(writer, client);
			writer.writeInt(client.getMaxCharacters());
		}

		client.getSession().send(writer.getBytes());
	}

	public static void sendServerStatus(LittleEndianReader packet, RemoteClient rc) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(4);
		lew.writeShort(ClientSendOps.SERVERLOAD_MSG);

		LoginWorld w = LoginServer.getInstance().getWorld(Byte.valueOf(rc.getWorld()));
		int collectiveLoads = w.getTotalLoad();

		//each channel can hold 2400
		int max = 2400 * w.getChannelCount();
		if (collectiveLoads >= max)
			lew.writeShort(SERVERSTATUS_MAX);
		else if (collectiveLoads >= 0.9 * max) // >90% full
			lew.writeShort(SERVERSTATUS_WARNING);
		else
			lew.writeShort(SERVERSTATUS_OK);
		rc.getSession().send(lew.getBytes());
	}

	public static void handleWorldListRequest(LittleEndianReader packet, RemoteClient rc) {
		for (Entry<Byte, LoginWorld> entry : LoginServer.getInstance().getAllWorlds().entrySet())
			rc.getSession().send(worldEntry(entry.getKey().byteValue(), entry.getValue()));
		rc.getSession().send(worldListEnd());
	}

	public static void handleViewAllChars(LittleEndianReader packet, RemoteClient rc) {
		LoginClient client = (LoginClient) rc;
		LittleEndianByteArrayWriter lew;
		Set<Byte> worlds = new TreeSet<Byte>();
		byte totalChars = 0;
		Connection con = DatabaseConnection.getConnection();
		try {
			PreparedStatement ps = con.prepareStatement("SELECT `world` FROM `characters` WHERE `accountid` = ?");
			ps.setInt(1, client.getAccountId());
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				byte world = rs.getByte(1);
				if (LoginServer.getInstance().getWorld(world) != null) {
					worlds.add(Byte.valueOf(world));
					totalChars++;
				}
			}
			rs.close();
			ps.close();
		} catch (SQLException ex) {
			LOG.log(Level.WARNING, "Could not set load all characters of account " + client.getAccountId(), ex);
		}
		lew = new LittleEndianByteArrayWriter(11);
		lew.writeShort(ClientSendOps.ALL_CHARLIST);
		lew.writeByte((byte) 1);
		lew.writeInt(totalChars);
		lew.writeInt(totalChars + (3 - totalChars % 3)); //amount of rows * 3
		client.getSession().send(lew.getBytes());

		for (Byte world : worlds) {
			lew = new LittleEndianByteArrayWriter(192);
			lew.writeShort(ClientSendOps.ALL_CHARLIST);
			lew.writeByte((byte) 0);
			lew.writeByte(world.byteValue());
			client.setWorld(world.byteValue());
			loadAndWriteCharacters(lew, client);
			client.getSession().send(lew.getBytes());
		}
	}

	private static void sendToGameServer(LoginClient c, int charid, String macs) {
		if (c.hasBannedMac(macs)) {
			c.getSession().close();
			return;
		}
		LoginWorld w = LoginServer.getInstance().getWorld(c.getWorld());
		//TODO: if client can't connect to the game server, then it's connected
		//state will always be STATUS_MIGRATION. There's got to be a way to
		//find out if the client's connection timed out and then
		//updateState(STATUS_NOTLOGGEDIN) if it is.
		c.migrateHost();
		c.getSession().send(writeServerAddress(w.getHost(c.getChannel()),
				w.getPort(c.getChannel()), charid));
	}

	public static void handlePickFromAllChars(LittleEndianReader packet, RemoteClient rc) {
		LoginClient client = (LoginClient) rc;
		int charid = packet.readInt();
		byte world = (byte) packet.readInt();
		String macs = packet.readLengthPrefixedString();
		//packet.readLengthPrefixedString(); what the hell is this?
		client.setWorld(world);
		client.setChannel(LoginServer.getInstance().getWorld(world).getLeastPopulatedChannel());
		sendToGameServer(client, charid, macs);
	}

	public static void handlePickFromWorldCharlist(LittleEndianReader packet, RemoteClient rc) {
		int charid = packet.readInt();
		String macs = packet.readLengthPrefixedString();
		//packet.readLengthPrefixedString(); what the hell is this?
		sendToGameServer((LoginClient) rc, charid, macs);
	}

	private static boolean allowedName(String name) {
		boolean valid = true;
		if (name.length() < 4 || name.length() > 12) {
			valid = false;
		} else {
			Connection con = DatabaseConnection.getConnection();
			try {
				PreparedStatement ps = con.prepareStatement("SELECT `id` FROM `characters` WHERE `name` = ?");
				ps.setString(1, name);
				ResultSet rs = ps.executeQuery();
				valid = !rs.next();
				rs.close();
				ps.close();
			} catch (SQLException ex) {
				LOG.log(Level.WARNING, "Could not determine if name " + name + " is allowed.", ex);
				valid = false;
			}
		}
		return valid;
	}

	public static void handleNameCheck(LittleEndianReader packet, RemoteClient rc) {
		String name = packet.readLengthPrefixedString();
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(5 + name.length());
		lew.writeShort(ClientSendOps.CHECK_NAME_RESP);
		lew.writeLengthPrefixedString(name);
		lew.writeBool(!allowedName(name));
		rc.getSession().send(lew.getBytes());
	}

	public static void handleCreateCharacter(LittleEndianReader packet, RemoteClient rc) {
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
			if ((eyes < 20000 || eyes > 20002) ||
					(hair != 30000 && hair != 30020 && hair != 30030) ||
					(top != 1040002 && top != 1040006 && top != 1040010) ||
					(bottom != 1060006 && bottom != 1060002))
				valid = false;
		else if (gender == 1)
			if ((eyes < 21000 || eyes > 21002) ||
					(hair != 31000 && hair != 31040 && hair != 31050) ||
					(top != 1041002 && top != 1041006 && top != 1041010 && top != 1041011) ||
					(bottom != 1061002 && bottom != 1061008))
				valid = false;
		else
			valid = false;
		if ((skin < 0 || skin > 3) ||
				(weapon != 1302000 && weapon != 1322005 && weapon != 1312004) ||
				(shoes != 1072001 && shoes != 1072005 && shoes != 1072037 && shoes != 1072038) ||
				(hairColor != 0 && hairColor != 2 && hairColor != 3 && hairColor != 7))
			valid = false;

		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter();
		lew.writeShort(ClientSendOps.CHAR_CREATED);
		lew.writeBool(!valid);
		if (valid) {
			Player p = Player.saveNewPlayer((LoginClient) rc, name, eyes,
					hair + hairColor, skin, gender, str, dex, _int, luk,
					top, bottom, shoes, weapon);
			writeCharEntry(lew, p);
		} else {
			LOG.log(Level.WARNING, "Player from account {0} tried to create a "
					+ "stats hacked character named {1} ",
					new Object[] { rc.getAccountId(), name });
		}
		rc.getSession().send(lew.getBytes());
	}

	public static void handleDeleteChar(LittleEndianReader packet, RemoteClient rc) {
		int date = packet.readInt();
		int cid = packet.readInt();

		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(7);
		lew.writeShort(ClientSendOps.DELETE_CHAR_RESPONSE);
		lew.writeInt(cid);
		lew.writeByte(((LoginClient) rc).deleteCharacter(cid, date));
		rc.getSession().send(lew.getBytes());
	}

	public static void backToLogin(LittleEndianReader packet, RemoteClient rc) {
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
		lew.writeLengthPrefixedString(w.getMessage());
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

		List<Message> messages = LoginServer.getInstance().getMessages();
		lew.writeShort((short) messages.size()); //num of messages
		for (Message msg : messages) {
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

	private static void writeCharEntry(LittleEndianWriter lew, Player p) {
		CommonPackets.writeCharStats(lew, p);
		CommonPackets.writeAvatar(lew, p, false);
		if (!PlayerJob.isModerator(p.getJob())) {
			lew.writeBool(true);
			lew.writeInt(0); //world rank
			lew.writeInt(0); //world rank change
			lew.writeInt(0); //job rank
			lew.writeInt(0); //job rank change
		} else {
			lew.writeBool(false);
		}
	}
}
