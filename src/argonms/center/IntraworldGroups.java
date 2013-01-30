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

package argonms.center;

import argonms.common.util.DatabaseManager;
import argonms.common.util.DatabaseManager.DatabaseType;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author GoldenKevin
 */
public class IntraworldGroups {
	private static final Logger LOG = Logger.getLogger(IntraworldGroups.class.getName());

	//TODO: partyid will easily be exhausted. either we have to use 64-bit value
	//or reuse old partyids (cf. InventorySlot)
	private static int getStartingPartyId(byte world) {
		int partyId = -1;
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			con = DatabaseManager.getConnection(DatabaseType.STATE);
			ps = con.prepareStatement("SELECT MAX(`partyid`) FROM `parties` WHERE `world` = ?");
			ps.setByte(1, world);
			rs = ps.executeQuery();
			if (rs.next())
				partyId = rs.getInt(1);
		} catch (SQLException ex) {
			LOG.log(Level.WARNING, "Could not get starting party id for world " + world, ex);
		} finally {
			DatabaseManager.cleanup(DatabaseType.STATE, rs, ps, con);
		}
		return partyId;
	}

	private final byte world;

	private final AtomicInteger nextPartyId;
	private final Map<Integer, Party> parties;

	private final Set<String> loadedGuildNames;
	private final Map<Integer, Guild> guilds;

	private final AtomicInteger nextRoomId;
	private final Map<Integer, Chatroom> rooms;

	public IntraworldGroups(byte world) {
		this.world = world;

		nextPartyId = new AtomicInteger(getStartingPartyId(world));
		parties = new ConcurrentHashMap<Integer, Party>();

		loadedGuildNames = Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());
		guilds = new ConcurrentHashMap<Integer, Guild>();

		nextRoomId = new AtomicInteger();
		rooms = new ConcurrentHashMap<Integer, Chatroom>();
	}

	/**
	 * 
	 * @param creator
	 * @return the unique partyId of the newly created party
	 */
	public int makeParty(Party.Member creator) {
		//make sure we never return 0, because the client treats 0 specially
		int partyId = nextPartyId.incrementAndGet();
		parties.put(Integer.valueOf(partyId), new Party(creator));
		return partyId;
	}

	public Party destroyParty(int partyId) {
		return parties.remove(Integer.valueOf(partyId));
	}

	public Party getParty(int partyId) {
		return parties.get(Integer.valueOf(partyId));
	}

	public void setParty(int partyId, Party party) {
		parties.put(Integer.valueOf(partyId), party);
	}

	public boolean guildExists(String name) {
		if (loadedGuildNames.contains(name))
			return true;

		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			con = DatabaseManager.getConnection(DatabaseType.STATE);
			ps = con.prepareStatement("SELECT EXISTS(SELECT 1 FROM `guilds` WHERE `name` = ? AND `world` = ? LIMIT 1)");
			ps.setString(1, name);
			ps.setByte(2, world);
			rs = ps.executeQuery();
			rs.next();
			return rs.getBoolean(1);
		} catch (SQLException ex) {
			LOG.log(Level.WARNING, "Could not determine whether guild " + name + " exists", ex);
			return false;
		} finally {
			DatabaseManager.cleanup(DatabaseType.STATE, rs, ps, con);
		}
	}

	public int makeGuild(String name, int partyId) {
		Party p = getParty(partyId);
		if (p == null)
			return -1;

		int guildId;
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			con = DatabaseManager.getConnection(DatabaseType.STATE);
			ps = con.prepareStatement("INSERT INTO `guilds` (`world`,`name`) VALUES (?,?)", PreparedStatement.RETURN_GENERATED_KEYS);
			ps.setByte(1, world);
			ps.setString(2, name);
			ps.executeUpdate();
			rs = ps.getGeneratedKeys();
			if (!rs.next())
				return -1;
			guildId = rs.getInt(1);
		} catch (SQLException ex) {
			LOG.log(Level.WARNING, "Could not create guild " + name, ex);
			return -1;
		} finally {
			DatabaseManager.cleanup(DatabaseType.STATE, rs, ps, con);
		}

		loadedGuildNames.add(name);
		guilds.put(Integer.valueOf(guildId), new Guild(name, p));
		return guildId;
	}

	/**
	 * 
	 * @param creator
	 * @return the unique roomId of the newly created chatroom
	 */
	public int makeRoom(Chatroom.Avatar creator) {
		//make sure we never return 0, because the client treats 0 specially
		int roomId = nextRoomId.incrementAndGet();
		rooms.put(Integer.valueOf(roomId), new Chatroom(creator));
		return roomId;
	}

	public Chatroom destroyRoom(int roomId) {
		return rooms.remove(Integer.valueOf(roomId));
	}

	public Chatroom getRoom(int roomId) {
		return rooms.get(Integer.valueOf(roomId));
	}

	public void setRoom(int roomid, Chatroom room) {
		rooms.put(Integer.valueOf(roomid), room);
	}
}
