package argonms.net.client.handler;

import argonms.login.LoginClient;
import argonms.login.LoginServer;
import argonms.login.World;
import argonms.net.client.ClientSendOps;
import argonms.net.client.RemoteClient;
import argonms.tools.DatabaseConnection;
import argonms.tools.input.LittleEndianReader;
import argonms.tools.output.LittleEndianByteArrayWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
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

	public static void handleCharlist(LittleEndianReader packet, RemoteClient rc) {
		LoginClient client = (LoginClient) rc;

		client.setWorld(packet.readByte());
		client.setChannel(packet.readByte());

		LittleEndianByteArrayWriter writer = new LittleEndianByteArrayWriter();
		writer.writeShort(ClientSendOps.CHARLIST);
		World w = LoginServer.getInstance().getWorlds().get(Byte.valueOf(client.getWorld()));
		if (w == null || w.getPorts()[client.getChannel()] == -1) {
			writer.writeByte((byte) 8); //"The connection could not be made because of a system error."
		} else {
			writer.writeByte((byte) 0); //show characters
			client.addCharacters(writer);
			writer.writeInt(client.getMaxCharacters());
		}

		rc.getSession().send(writer.getBytes());
	}

	public static void sendServerStatus(LittleEndianReader packet, RemoteClient rc) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(4);
		lew.writeShort(ClientSendOps.SERVERLOAD_MSG);
		lew.writeShort(SERVERSTATUS_OK);
		rc.getSession().send(lew.getBytes());
	}

	public static void handleWorldListRequest(LittleEndianReader packet, RemoteClient rc) {
		Set<Entry<Byte, World>> worlds = LoginServer.getInstance().getWorlds().entrySet();
		for (Entry<Byte, World> entry : worlds) {
			World world = entry.getValue();
			rc.getSession().send(worldEntry(entry.getKey().byteValue(), world.getName(), world.getFlag(), world.getMessage(), world.getLoads()));
		}
		rc.getSession().send(worldListEnd());
	}

	public static void sendAllChars(LittleEndianReader packet, RemoteClient rc) {
		LoginClient client = (LoginClient) rc;
		LittleEndianByteArrayWriter lew;
		List<Byte> worlds = new ArrayList<Byte>();
		byte amt = 0;
		Connection con = DatabaseConnection.getConnection();
		try {
			PreparedStatement ps = con.prepareStatement("SELECT `world` FROM `characters` WHERE `accountid` = ? ORDER BY `world`, `id`");
			ps.setInt(1, client.getAccountId());
			ResultSet rs = ps.executeQuery();
			byte world;
			while (rs.next()) {
				world = rs.getByte(1);
				if (!worlds.contains(world))
					worlds.add(world);
				amt++;
			}
		} catch (SQLException ex) {
			LOG.log(Level.WARNING, "Could not set load all characters of account " + client.getAccountId(), ex);
		}
		lew = new LittleEndianByteArrayWriter(11);
		lew.writeShort(ClientSendOps.ALL_CHARLIST);
		lew.writeByte((byte) 1);
		lew.writeInt(amt);
		lew.writeInt(amt + (3 - amt % 3)); //amount of rows * 3
		client.getSession().send(lew.getBytes());

		for (byte world : worlds) {
			client.setWorld(world);
			lew = new LittleEndianByteArrayWriter(192);
			lew.writeShort(ClientSendOps.ALL_CHARLIST);
			lew.writeByte((byte) 0);
			lew.writeByte(world);
			client.addCharacters(lew);
			client.getSession().send(lew.getBytes());
		}
	}

	public static void checkName(LittleEndianReader packet, RemoteClient rc) {
		String name = packet.readLengthPrefixedString();
		boolean invalid = false;
		if (name.length() < 4 || name.length() > 12) {
			invalid = true;
		} else {
			Connection con = DatabaseConnection.getConnection();
			try {
				PreparedStatement ps = con.prepareStatement("SELECT `id` FROM `characters` WHERE `name` = ?");
				ps.setString(1, name);
				ResultSet rs = ps.executeQuery();
				invalid = rs.next();
			} catch (SQLException ex) {
				LOG.log(Level.WARNING, "Could not determine if name " + name + " is allowed.", ex);
				invalid = true;
			}
		}
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(5 + name.length());
		lew.writeShort(ClientSendOps.CHECK_NAME_RESP);
		lew.writeLengthPrefixedString(name);
		lew.writeBool(invalid);
		rc.getSession().send(lew.getBytes());
	}

	public static void createCharacter(LittleEndianReader packet, RemoteClient rc) {
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

		Connection con = DatabaseConnection.getConnection();
		try {
			PreparedStatement ps = con.prepareStatement("INSERT INTO `characters`(`accountid`,`world`,`name`,`gender`,`skin`,`eyes`,`hair`,`str`,`dex`,`int`,`luk`) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
			ps.setInt(1, rc.getAccountId());
			ps.setByte(2, rc.getWorld());
			ps.setString(3, name);
			ps.setByte(4, gender);
			ps.setInt(5, skin);
			ps.setInt(6, eyes);
			ps.setInt(7, hair + hairColor);
			ps.setByte(8, str);
			ps.setByte(9, dex);
			ps.setByte(10, _int);
			ps.setByte(11, luk);
			ps.executeUpdate();
		} catch (SQLException ex) {
			LOG.log(Level.WARNING, "Could not save new character '" + name + "' on account " + rc.getAccountId() + " to database.", ex);
		}
		//items table
		//equips table
	}

	private static byte[] worldEntry(byte world, String name, byte flag, String message, short[] loads) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(128);

		lew.writeShort(ClientSendOps.WORLD_ENTRY);
		lew.writeByte(world);
		lew.writeLengthPrefixedString(name);
		lew.writeByte(flag);
		lew.writeLengthPrefixedString(message);
		lew.writeByte((byte) 100); // rate modifier
		lew.writeByte((byte) 0); // event xp * 2.6
		lew.writeByte((byte) 100); // rate modifier
		lew.writeByte((byte) 0); // drop rate * 2.6
		lew.writeByte((byte) 0);
		lew.writeByte((byte) loads.length);

		for (short i = 0; i < loads.length; i++) {
			lew.writeLengthPrefixedString(name + '-' + (i + 1));
			lew.writeInt(loads[i]);
			lew.writeByte(world);
			lew.writeShort(i);
		}
		lew.writeShort((short) 0);

		return lew.getBytes();
	}

	private static byte[] worldListEnd() {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(3);

		lew.writeShort(ClientSendOps.WORLD_ENTRY);
		lew.writeByte((byte) 0xFF);

		return lew.getBytes();
	}
}
