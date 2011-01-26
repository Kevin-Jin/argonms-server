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

package argonms.loading.map;

import argonms.tools.DatabaseConnection;
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
		Connection con = DatabaseConnection.getWzConnection();
		MapStats stats = null;
		try {
			PreparedStatement ps = con.prepareStatement("SELECT * FROM `mapdata` WHERE `mapid` = ?");
			ps.setInt(1, mapid);
			ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				stats = new MapStats();
				doWork(rs, stats);
			}
			rs.close();
			ps.close();
		} catch (SQLException e) {
			LOG.log(Level.WARNING, "Could not read MCDB data for map " + mapid, e);
		}
		mapStats.put(Integer.valueOf(mapid), stats);
	}

	public boolean loadAll() {
		Connection con = DatabaseConnection.getWzConnection();
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = con.prepareStatement("SELECT * FROM `mapdata`");
			rs = ps.executeQuery();
			while (rs.next()) {
				MapStats stats = new MapStats();
				mapStats.put(doWork(rs, stats), stats);
			}
			return true;
		} catch (SQLException ex) {
			LOG.log(Level.WARNING, "Could not load all map data from MCDB.", ex);
			return false;
		} finally {
			try {
				if (rs != null)
					rs.close();
				if (ps != null)
					ps.close();
			} catch (SQLException ex) {
				//Nothing we can do
			}
		}
	}

	public boolean canLoad(int mapid) {
		Connection con = DatabaseConnection.getWzConnection();
		boolean exists = false;
		try {
			PreparedStatement ps = con.prepareStatement("SELECT * FROM `mapdata` WHERE `mapid` = ?");
			ps.setInt(1, mapid);
			ResultSet rs = ps.executeQuery();
			if (rs.next())
				exists = true;
			rs.close();
			ps.close();
		} catch (SQLException e) {
			LOG.log(Level.WARNING, "Could not use MCDB to determine whether map " + mapid + " is valid.", e);
		}
		return exists;
	}

	private int doWork(ResultSet rs, MapStats stats) throws SQLException {
		int mapid = rs.getInt(1);
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
		loadLife(mapid, stats);
		loadReactors(mapid, stats);
		loadFootholds(mapid, stats);
		loadPortals(mapid, stats);
		return mapid;
	}

	private void loadLife(int mapid, MapStats stats) {
		Connection con = DatabaseConnection.getWzConnection();
		try {
			PreparedStatement ps = con.prepareStatement("SELECT * FROM `maplifedata` where `mapid` = ?");
			ps.setInt(1, mapid);
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				int id = rs.getInt(2);
				Life l = new Life();
				l.setType(rs.getInt(3) == 0 ? 'm' : 'n');
				l.setDataId(rs.getInt(4));
				l.setX(rs.getInt(5));
				l.setY(rs.getInt(6));
				l.setCy(rs.getInt(6));
				l.setFoothold(rs.getInt(7));
				l.setRx0(rs.getInt(8));
				l.setRx1(rs.getInt(9));
				l.setMobTime(rs.getInt(10));
				stats.addLife(id, l);
			}
			rs.close();
			ps.close();
		} catch (SQLException e) {
			LOG.log(Level.WARNING, "Could not load spawns of map " + mapid, e);
		}
	}

	private void loadReactors(int mapid, MapStats stats) {
		Connection con = DatabaseConnection.getWzConnection();
		try {
			PreparedStatement ps = con.prepareStatement("SELECT * FROM `mapreactordata` where `mapid` = ?");
			ps.setInt(1, mapid);
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				int id = rs.getInt(2);
				Reactor rt = new Reactor();
				rt.setDataId(rs.getInt(3));
				rt.setX(rs.getInt(4));
				rt.setY(rs.getInt(5));
				rt.setReactorTime(rs.getInt(6));
				rt.setName("");
				stats.addReactor(id, rt);
			}
			rs.close();
			ps.close();
		} catch (SQLException e) {
			LOG.log(Level.WARNING, "Could not load reactors of map " + mapid, e);
		}
	}

	private void loadFootholds(int mapid, MapStats stats) {
		Connection con = DatabaseConnection.getWzConnection();
		try {
			PreparedStatement ps = con.prepareStatement("SELECT * FROM `mapfootholddata` where `mapid` = ?");
			ps.setInt(1, mapid);
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				int id = rs.getInt(2);
				Foothold fh = new Foothold();
				fh.setX1(rs.getInt(3));
				fh.setY1(rs.getInt(4));
				fh.setX2(rs.getInt(5));
				fh.setY2(rs.getInt(6));
				fh.setPrev(rs.getInt(7));
				fh.setNext(rs.getInt(8));
				stats.addFoothold(id, fh);
			}
			rs.close();
			ps.close();
		} catch (SQLException e) {
			LOG.log(Level.WARNING, "Could not load footholds of map " + mapid, e);
		}
	}

	private void loadPortals(int mapid, MapStats stats) {
		Connection con = DatabaseConnection.getWzConnection();
		try {
			PreparedStatement ps = con.prepareStatement("SELECT * FROM `mapportaldata` where `mapid` = ?");
			ps.setInt(1, mapid);
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				int id = rs.getInt(2);
				Portal p = new Portal();
				String name = rs.getString(3);
				p.setPortalName(name);
				p.setX(rs.getInt(4));
				p.setY(rs.getInt(5));
				int to = rs.getInt(6);
				int type = 0;
				String script = rs.getString(8);
				if (to != 999999999) //warp portal
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
			rs.close();
			ps.close();
		} catch (SQLException e) {
			LOG.log(Level.WARNING, "Could not load spawns of map " + mapid, e);
		}
	}
}
