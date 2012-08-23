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

package argonms.common;

import argonms.common.util.DatabaseManager;
import argonms.common.util.DatabaseManager.DatabaseType;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 *
 * @author GoldenKevin
 */
public final class UniqueIdGenerator {
	public static long getAndAdd(int delta) throws Exception {
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		boolean locked = false;
		try {
			con = DatabaseManager.getConnection(DatabaseType.STATE);
			ps = con.prepareStatement("LOCK TABLE `uniqueid` WRITE");
			ps.executeUpdate();
			locked = true;
			ps.close();
			ps = con.prepareStatement("SELECT `nextuid` FROM `uniqueid`");
			rs = ps.executeQuery();
			long current;
			if (rs.next()) {
				current = rs.getLong(1);
				rs.close();
				ps.close();
				ps = con.prepareStatement("UPDATE `uniqueid` SET `nextuid` = `nextuid` + ?");
				ps.setInt(1, delta);
				ps.executeUpdate();
			} else {
				current = 1;
				rs.close();
				ps.close();
				ps = con.prepareStatement("INSERT INTO `uniqueid` (`nextuid`) VALUES (1 + ?)");
				ps.setInt(1, delta);
				ps.executeUpdate();
			}
			return current;
		} catch (SQLException e) {
			throw new Exception("Database access error while acquiring next unique id.", e);
		} finally {
			if (locked) {
				DatabaseManager.cleanup(DatabaseType.STATE, rs, ps, null);
				try {
					ps = con.prepareStatement("UNLOCK TABLE");
					ps.executeUpdate();
				} catch (SQLException e) {
					throw new Exception("Could not unlock uniqueid table.", e);
				} finally {
					DatabaseManager.cleanup(DatabaseType.STATE, rs, ps, con);
				}
			} else {
				DatabaseManager.cleanup(DatabaseType.STATE, rs, ps, con);
			}
		}
	}

	public static long getAndIncrement() throws Exception {
		return getAndAdd(1);
	}

	private UniqueIdGenerator() {
		//uninstantiable...
	}
}
