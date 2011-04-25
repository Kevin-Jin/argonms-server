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

package argonms.loading.string;

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
public class McdbStringDataLoader extends StringDataLoader {
	private static final Logger LOG = Logger.getLogger(McdbStringDataLoader.class.getName());

	protected McdbStringDataLoader() {

	}

	public boolean loadAll() {
		Connection con = DatabaseConnection.getWzConnection();
		try {
			PreparedStatement ps = con.prepareStatement("SELECT `type`,`objectid`,`name` FROM `stringdata` WHERE `type` != 6");
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				switch (rs.getShort(1)) {
					case 1:
						itemNames.put(Integer.valueOf(rs.getInt(2)), rs.getString(3));
						break;
					case 2:
						skillNames.put(Integer.valueOf(rs.getInt(2)), rs.getString(3));
						break;
					case 3:
						mapNames.put(Integer.valueOf(rs.getInt(2)), rs.getString(3));
						break;
					case 4:
						mobNames.put(Integer.valueOf(rs.getInt(2)), rs.getString(3));
						break;
					case 5:
						npcNames.put(Integer.valueOf(rs.getInt(2)), rs.getString(3));
						break;
				}
			}
			rs.close();
			ps.close();
			return true;
		} catch (SQLException e) {
			LOG.log(Level.WARNING, "Error loading string data from the MCDB.", e);
			return false;
		}
	}
}
