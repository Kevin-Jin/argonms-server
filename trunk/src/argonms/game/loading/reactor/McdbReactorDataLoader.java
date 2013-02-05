/*
 * ArgonMS MapleStory server emulator written in Java
 * Copyright (C) 2011-2013  GoldenKevin
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

import argonms.common.util.DatabaseManager;
import argonms.common.util.DatabaseManager.DatabaseType;
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

	@Override
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
		con = null;
		ps = null;
		rs = null;
		try {
			con = DatabaseManager.getConnection(DatabaseType.STATE);
			ps = con.prepareStatement("SELECT `script` FROM `reactorscriptnames` WHERE `reactorid` = ?");
			ps.setInt(1, reactorid);
			rs = ps.executeQuery();
			if (rs.next()) {
				if (stats == null)
					stats = new ReactorStats(reactorid);
				stats.setScript(rs.getString(1));
			}
		} catch (SQLException e) {
			LOG.log(Level.WARNING, "Could not read reactor script name for reactor " + reactorid, e);
		} finally {
			DatabaseManager.cleanup(DatabaseType.STATE, rs, ps, con);
		}
		reactorStats.put(Integer.valueOf(reactorid), stats);
	}

	@Override
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
		} catch (SQLException ex) {
			LOG.log(Level.WARNING, "Could not load all reactor data from MCDB.", ex);
			return false;
		} finally {
			DatabaseManager.cleanup(DatabaseType.WZ, rs, ps, con);
		}
		con = null;
		ps = null;
		rs = null;
		try {
			con = DatabaseManager.getConnection(DatabaseType.STATE);
			ps = con.prepareStatement("SELECT `reactorid`,`script` FROM `reactorscriptnames` ORDER BY `reactorid`");
			rs = ps.executeQuery();
			while (rs.next()) {
				int reactorid = rs.getInt(1);
				ReactorStats stats = reactorStats.get(Integer.valueOf(reactorid));
				if (stats == null) {
					stats = new ReactorStats(reactorid);
					reactorStats.put(Integer.valueOf(reactorid), stats);
				}
				stats.setScript(rs.getString(2));
			}
		} catch (SQLException ex) {
			LOG.log(Level.WARNING, "Could not load all reactor scripts from MCDB.", ex);
			return false;
		} finally {
			DatabaseManager.cleanup(DatabaseType.STATE, rs, ps, con);
		}
		return true;
	}

	@Override
	public boolean canLoad(int reactorid) {
		if (reactorStats.containsKey(reactorid))
			return true;
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			con = DatabaseManager.getConnection(DatabaseType.WZ);
			ps = con.prepareStatement("SELECT * FROM `reactoreventdata` WHERE `reactorid` = ?");
			ps.setInt(1, reactorid);
			rs = ps.executeQuery();
			if (rs.next())
				return true;
		} catch (SQLException e) {
			LOG.log(Level.WARNING, "Could not use MCDB to determine whether reactor " + reactorid + " is valid.", e);
		} finally {
			DatabaseManager.cleanup(DatabaseType.WZ, rs, ps, con);
		}
		con = null;
		ps = null;
		rs = null;
		try {
			con = DatabaseManager.getConnection(DatabaseType.STATE);
			ps = con.prepareStatement("SELECT * FROM `reactorscriptnames` WHERE `reactorid` = ?");
			ps.setInt(1, reactorid);
			rs = ps.executeQuery();
			if (rs.next())
				return true;
		} catch (SQLException e) {
			LOG.log(Level.WARNING, "Could not use reactor scripts table to determine whether reactor " + reactorid + " is valid.", e);
		} finally {
			DatabaseManager.cleanup(DatabaseType.STATE, rs, ps, con);
		}
		return false;
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
