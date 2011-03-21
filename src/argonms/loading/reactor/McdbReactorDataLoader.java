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

package argonms.loading.reactor;

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
public class McdbReactorDataLoader extends ReactorDataLoader {
	private static final Logger LOG = Logger.getLogger(McdbReactorDataLoader.class.getName());

	protected McdbReactorDataLoader() {

	}

	protected void load(int reactorid) {
		Connection con = DatabaseConnection.getWzConnection();
		ReactorStats stats = null;
		try {
			PreparedStatement ps = con.prepareStatement("SELECT * FROM `reactoreventdata` WHERE `reactorid` = ?");
			ps.setInt(1, reactorid);
			ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				stats = new ReactorStats();
				doWork(rs, stats);
			}
			rs.close();
			ps.close();
		} catch (SQLException e) {
			LOG.log(Level.WARNING, "Could not read MCDB data for reactor " + reactorid, e);
		}
		reactorStats.put(Integer.valueOf(reactorid), stats);
	}

	public boolean loadAll() {
		Connection con = DatabaseConnection.getWzConnection();
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = con.prepareStatement("SELECT * FROM `reactoreventdata`");
			rs = ps.executeQuery();
			while (rs.next()) {
				ReactorStats stats = new ReactorStats();
				reactorStats.put(Integer.valueOf(doWork(rs, stats)), stats);
			}
			return true;
		} catch (SQLException ex) {
			LOG.log(Level.WARNING, "Could not load all reactor data from MCDB.", ex);
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

	public boolean canLoad(int reactorid) {
		if (reactorStats.containsKey(reactorid))
			return true;
		Connection con = DatabaseConnection.getWzConnection();
		boolean exists = false;
		try {
			PreparedStatement ps = con.prepareStatement("SELECT * FROM `reactoreventdata` WHERE `reactorid` = ?");
			ps.setInt(1, reactorid);
			ResultSet rs = ps.executeQuery();
			if (rs.next())
				exists = true;
			rs.close();
			ps.close();
		} catch (SQLException e) {
			LOG.log(Level.WARNING, "Could not use MCDB to determine whether reactor " + reactorid + " is valid.", e);
		}
		return exists;
	}

	private int doWork(ResultSet rs, ReactorStats stats) throws SQLException {
		int reactorid = rs.getInt(2);
		do {
			State s = new State();
			s.setType(rs.getByte(4));
			s.setItem(rs.getInt(5), (short) 1);
			s.setLt(rs.getShort(6), rs.getShort(7));
			s.setRb(rs.getShort(8), rs.getShort(9));
			s.setNextState(rs.getByte(10));
			stats.addState(rs.getByte(3), s);
		} while (rs.next() && rs.getInt(2) == reactorid);
		return reactorid;
	}
}
