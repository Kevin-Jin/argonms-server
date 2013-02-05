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

package argonms.game.loading.beauty;

import argonms.common.loading.string.McdbStringDataLoader;
import argonms.common.util.DatabaseManager;
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
public class McdbBeautyDataLoader extends BeautyDataLoader {
	private static final Logger LOG = Logger.getLogger(McdbStringDataLoader.class.getName());

	@Override
	public boolean loadAll() {Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			con = DatabaseManager.getConnection(DatabaseManager.DatabaseType.WZ);
			ps = con.prepareStatement("SELECT `faceid` FROM `facedata`");
			rs = ps.executeQuery();
			while (rs.next())
				eyeStyles.add(Short.valueOf(rs.getShort(1)));
			rs.close();
			ps.close();

			ps = con.prepareStatement("SELECT `hairid` FROM `hairdata`");
			rs = ps.executeQuery();
			while (rs.next())
				hairStyles.add(Short.valueOf(rs.getShort(1)));
			return true;
		} catch (SQLException e) {
			LOG.log(Level.WARNING, "Error loading beauty data from the MCDB.", e);
			return false;
		} finally {
			DatabaseManager.cleanup(DatabaseManager.DatabaseType.WZ, rs, ps, con);
		}
	}
}
