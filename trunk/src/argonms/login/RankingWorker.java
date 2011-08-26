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

import argonms.common.util.DatabaseManager;
import argonms.common.util.DatabaseManager.DatabaseType;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Types;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Offloads all calculation to the MySQL server so less data (or even none
 * entirely) will have to be transferred between the MySQL server and the login
 * server. The reduction in bandwidth usage and network processing will offset
 * Java's better processing performance over MySQL script execution.
 *
 * Inspired by Vana's RankingCalculator.
 * @author GoldenKevin
 */
public class RankingWorker implements Runnable {
	private static final Logger LOG = Logger.getLogger(RankingWorker.class.getName());

	@Override
	public void run() {
		Connection con = null;
		CallableStatement ps = null;
		long start, end;
		start = System.nanoTime();
		try {
			con = DatabaseManager.getConnection(DatabaseType.STATE);
			ps = con.prepareCall("{call updateranks(?,?)}");
			ps.setString(1, "overall");
			ps.setNull(2, Types.TINYINT);
			ps.executeUpdate();
			ps.setString(1, "world");
			for (Byte world : LoginServer.getInstance().getAllWorlds().keySet()) {
				ps.setByte(2, world.byteValue());
				ps.executeUpdate();
			}
			ps.setString(1, "job");
			for (byte jobClass = 0; jobClass <= 5; jobClass++) { //beginner, warrior, magician, bowman, thief, pirate respectively
				ps.setByte(2, jobClass);
				ps.executeUpdate();
			}
			ps.setString(1, "fame");
			ps.setNull(2, Types.TINYINT);
			ps.executeUpdate();
			end = System.nanoTime();
			LOG.log(Level.FINE, "Sucessfully updated rankings in {0} milliseconds.", (end - start) / 1000000.0);
		} catch (SQLException ex) {
			LOG.log(Level.WARNING, "Error updating the rankings.", ex);
		} finally {
			DatabaseManager.cleanup(DatabaseType.STATE, null, ps, con);
		}
	}
}
