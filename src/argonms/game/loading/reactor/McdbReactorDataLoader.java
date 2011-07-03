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

package argonms.game.loading.reactor;

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
public class McdbReactorDataLoader extends ReactorDataLoader {
	private static final Logger LOG = Logger.getLogger(McdbReactorDataLoader.class.getName());

	protected McdbReactorDataLoader() {

	}

	protected void load(int reactorid) {
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		ReactorStats stats = null;
		try {
			con = DatabaseManager.getConnection(DatabaseType.WZ);
			ps = con.prepareStatement("SELECT * FROM `reactoreventdata` WHERE `reactorid` = ?");
			ps.setInt(1, reactorid);
			rs = ps.executeQuery();
			if (rs.next()) {
				stats = new ReactorStats(reactorid);
				doWork(rs, reactorid, stats);
			}
		} catch (SQLException e) {
			LOG.log(Level.WARNING, "Could not read MCDB data for reactor " + reactorid, e);
		} finally {
			DatabaseManager.cleanup(DatabaseType.WZ, rs, ps, con);
		}
		reactorStats.put(Integer.valueOf(reactorid), stats);
	}

	public boolean loadAll() {
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			con = DatabaseManager.getConnection(DatabaseType.WZ);
			ps = con.prepareStatement("SELECT * FROM `reactoreventdata` ORDER BY `reactorid`");
			rs = ps.executeQuery();
			boolean more = false;
			while (more || rs.next()) {
				int reactorid = rs.getInt(2);
				ReactorStats stats = new ReactorStats(reactorid);
				more = doWork(rs, reactorid, stats);
				reactorStats.put(Integer.valueOf(reactorid), stats);
			}
			return true;
		} catch (SQLException ex) {
			LOG.log(Level.WARNING, "Could not load all reactor data from MCDB.", ex);
			return false;
		} finally {
			DatabaseManager.cleanup(DatabaseType.WZ, rs, ps, con);
		}
	}

	public boolean canLoad(int reactorid) {
		if (reactorStats.containsKey(reactorid))
			return true;
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		boolean exists = false;
		try {
			con = DatabaseManager.getConnection(DatabaseType.WZ);
			ps = con.prepareStatement("SELECT * FROM `reactoreventdata` WHERE `reactorid` = ?");
			ps.setInt(1, reactorid);
			rs = ps.executeQuery();
			if (rs.next())
				exists = true;
		} catch (SQLException e) {
			LOG.log(Level.WARNING, "Could not use MCDB to determine whether reactor " + reactorid + " is valid.", e);
		} finally {
			DatabaseManager.cleanup(DatabaseType.WZ, rs, ps, con);
		}
		return exists;
	}

	private boolean doWork(ResultSet rs, int reactorid, ReactorStats stats) throws SQLException {
		boolean more;
		do {
			State s = new State();
			s.setType(rs.getByte(4));
			s.setItem(rs.getInt(5), (short) 1);
			s.setLt(rs.getShort(6), rs.getShort(7));
			s.setRb(rs.getShort(8), rs.getShort(9));
			s.setNextState(rs.getByte(10));
			stats.addState(rs.getByte(3), s);
		} while ((more = rs.next()) && rs.getInt(2) == reactorid);
		return more;
	}
}
