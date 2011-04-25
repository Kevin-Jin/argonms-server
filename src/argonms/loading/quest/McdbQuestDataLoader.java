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

package argonms.loading.quest;

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
public class McdbQuestDataLoader extends QuestDataLoader {
	private static final Logger LOG = Logger.getLogger(McdbQuestDataLoader.class.getName());

	public boolean loadAll() {
		if (!loadInfo())
			return false;
		if (!loadReq())
			return false;
		if (!loadAct())
			return false;
		return true;
	}

	protected boolean loadInfo() {
		Connection con = DatabaseConnection.getWzConnection();
		try {
			PreparedStatement ps = con.prepareStatement("SELECT `objectid`,`name` FROM `stringdata` WHERE `type` = 6");
			ResultSet rs = ps.executeQuery();
			while (rs.next())
				questNames.put(Short.valueOf(rs.getShort(1)), rs.getString(2));
			rs.close();
			ps.close();
			return true;
		} catch (SQLException e) {
			LOG.log(Level.WARNING, "Error loading quest info data from the MCDB.", e);
			return false;
		}
	}

	protected boolean loadAct() {
		/*Connection con = DatabaseConnection.getWzConnection();
		try {
			
			return true;
		} catch (SQLException e) {
			LOG.log(Level.WARNING, "Error loading quest action data from the MCDB.", e);*/
			return false;
		//}
	}

	protected boolean loadReq() {
		/*Connection con = DatabaseConnection.getWzConnection();
		try {
			
			return true;
		} catch (SQLException e) {
			LOG.log(Level.WARNING, "Error loading quest check data from the MCDB.", e);*/
			return false;
		//}
	}
}
