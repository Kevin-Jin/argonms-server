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

package argonms.game.loading.map;

import argonms.common.GlobalConstants;
import argonms.common.tools.DatabaseManager;
import argonms.common.tools.DatabaseManager.DatabaseType;
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
public class McdbMapDataLoader extends MapDataLoader {
	private static final Logger LOG = Logger.getLogger(McdbMapDataLoader.class.getName());

	protected McdbMapDataLoader() {
		
	}

	protected void load(int mapid) {
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		MapStats stats = null;
		try {
			con = DatabaseManager.getConnection(DatabaseType.WZ);
			ps = con.prepareStatement("SELECT * FROM `mapdata` WHERE `mapid` = ?");
			ps.setInt(1, mapid);
			rs = ps.executeQuery();
			if (rs.next()) {
				stats = new MapStats(mapid);
				doWork(rs, mapid, stats, con);
			}
		} catch (SQLException e) {
			LOG.log(Level.WARNING, "Could not read MCDB data for map " + mapid, e);
		} finally {
			DatabaseManager.cleanup(DatabaseType.WZ, rs, ps, con);
		}
		mapStats.put(Integer.valueOf(mapid), stats);
	}

	public boolean loadAll() {
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			con = DatabaseManager.getConnection(DatabaseType.WZ);
			ps = con.prepareStatement("SELECT * FROM `mapdata`");
			rs = ps.executeQuery();
			while (rs.next()) {
				int mapid = rs.getInt(1);
				MapStats stats = new MapStats(mapid);
				doWork(rs, mapid, stats, con);
				mapStats.put(Integer.valueOf(mapid), stats);
			}
			return true;
		} catch (SQLException ex) {
			LOG.log(Level.WARNING, "Could not load all map data from MCDB.", ex);
			return false;
		} finally {
			DatabaseManager.cleanup(DatabaseType.WZ, rs, ps, con);
		}
	}

	public boolean canLoad(int mapid) {
		if (mapStats.containsKey(mapid))
			return true;
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		boolean exists = false;
		try {
			con = DatabaseManager.getConnection(DatabaseType.WZ);
			ps = con.prepareStatement("SELECT * FROM `mapdata` WHERE `mapid` = ?");
			ps.setInt(1, mapid);
			rs = ps.executeQuery();
			if (rs.next())
				exists = true;
		} catch (SQLException e) {
			LOG.log(Level.WARNING, "Could not use MCDB to determine whether map " + mapid + " is valid.", e);
		} finally {
			DatabaseManager.cleanup(DatabaseType.WZ, rs, ps, con);
		}
		return exists;
	}

	private void doWork(ResultSet rs, int mapid, MapStats stats, Connection con) throws SQLException {
		stats.setReturnMap(rs.getInt(6));
		stats.setForcedReturn(rs.getInt(7));
		stats.setDecHp(rs.getInt(10));
		stats.setProtectItem(rs.getInt(11));
		stats.setMobRate(rs.getFloat(16));
		if (rs.getInt(17) != 0)
			stats.setTown();
		if (rs.getInt(18) != 0)
			stats.setClock();
		if (rs.getInt(19) != 0)
			stats.setShip();
		loadLife(mapid, stats, con);
		loadReactors(mapid, stats, con);
		loadFootholds(mapid, stats, con);
		loadPortals(mapid, stats, con);
		stats.finished();
	}

	private void loadLife(int mapid, MapStats stats, Connection con) throws SQLException {
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = con.prepareStatement("SELECT * FROM `maplifedata` where `mapid` = ?");
			ps.setInt(1, mapid);
			rs = ps.executeQuery();
			while (rs.next()) {
				int id = rs.getInt(2);
				SpawnData l = new SpawnData();
				l.setType(rs.getInt(3) == 0 ? 'm' : 'n');
				l.setDataId(rs.getInt(4));
				l.setX(rs.getShort(5));
				l.setY(rs.getShort(6));
				l.setCy(rs.getShort(6));
				l.setFoothold(rs.getShort(7));
				l.setRx0(rs.getShort(8));
				l.setRx1(rs.getShort(9));
				l.setMobTime(rs.getInt(10));
				stats.addLife(id, l);
			}
		} catch (SQLException e) {
			throw new SQLException("Failed to load spawns of map " + mapid, e);
		} finally {
			DatabaseManager.cleanup(DatabaseType.WZ, rs, ps, null);
		}
	}

	private void loadReactors(int mapid, MapStats stats, Connection con) throws SQLException {
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = con.prepareStatement("SELECT * FROM `mapreactordata` where `mapid` = ?");
			ps.setInt(1, mapid);
			rs = ps.executeQuery();
			while (rs.next()) {
				int id = rs.getInt(2);
				ReactorData rt = new ReactorData();
				rt.setDataId(rs.getInt(3));
				rt.setX(rs.getShort(4));
				rt.setY(rs.getShort(5));
				rt.setReactorTime(rs.getInt(6));
				rt.setName("");
				stats.addReactor(id, rt);
			}
		} catch (SQLException e) {
			throw new SQLException("Failed to load reactors of map " + mapid, e);
		} finally {
			DatabaseManager.cleanup(DatabaseType.WZ, rs, ps, null);
		}
	}

	private void loadFootholds(int mapid, MapStats stats, Connection con) throws SQLException {
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = con.prepareStatement("SELECT * FROM `mapfootholddata` where `mapid` = ?");
			ps.setInt(1, mapid);
			rs = ps.executeQuery();
			while (rs.next()) {
				Foothold fh = new Foothold(rs.getShort(2));
				fh.setX1(rs.getShort(3));
				fh.setY1(rs.getShort(4));
				fh.setX2(rs.getShort(5));
				fh.setY2(rs.getShort(6));
				fh.setPrev(rs.getShort(7));
				fh.setNext(rs.getShort(8));
				stats.addFoothold(fh);
			}
		} catch (SQLException e) {
			throw new SQLException("Failed to load footholds of map " + mapid, e);
		} finally {
			DatabaseManager.cleanup(DatabaseType.WZ, rs, ps, null);
		}
	}

	private void loadPortals(int mapid, MapStats stats, Connection con) throws SQLException {
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = con.prepareStatement("SELECT * FROM `mapportaldata` where `mapid` = ?");
			ps.setInt(1, mapid);
			rs = ps.executeQuery();
			while (rs.next()) {
				int id = rs.getInt(2);
				PortalData p = new PortalData();
				String name = rs.getString(3);
				p.setPortalName(name);
				p.setPosition(rs.getShort(4), rs.getShort(5));
				int to = rs.getInt(6);
				byte type = 0;
				String script = rs.getString(8);
				if (to != GlobalConstants.NULL_MAP) //warp portal
					type = 2; //1 or 2?
				else if (name.equals("sp")) //spawnpoint
					type = 0;
				else if (name.equals("tp")) //mystic doors
					type = 6;
				else if (script != null && !script.isEmpty()) //scripted portal
					type = 7; //7 or 8?
				p.setPortalType(type);
				p.setTargetMapId(to);
				p.setTargetName(rs.getString(7));
				p.setScript(script);
				stats.addPortal(id, p);
			}
		} catch (SQLException e) {
			throw new SQLException("Failed to load portals of map " + mapid, e);
		} finally {
			DatabaseManager.cleanup(DatabaseType.WZ, rs, ps, null);
		}
	}
}
