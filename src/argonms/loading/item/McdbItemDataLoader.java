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

package argonms.loading.item;

import argonms.tools.input.WzDatabaseConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author GoldenKevin
 */
public class McdbItemDataLoader extends ItemDataLoader {
	private static final Logger LOG = Logger.getLogger(McdbItemDataLoader.class.getName());
	private Connection con;

	public McdbItemDataLoader() {
		con = WzDatabaseConnection.getConnection();
	}

	protected void load(int itemid) {
		String cat = getCategory(itemid);
		String query;
		Integer oId = Integer.valueOf(itemid);
		boolean equip;
		if (cat.equals("Equip")) {
			query = "SELECT * FROM `itemdata` WHERE `itemid` = ?";
			equip = true;
		} else {
			if (cat.equals("Pet"))
				loadPetData(itemid);
			query = "SELECT * FROM `itemdata` WHERE `itemid` = ?";
			equip = false;
		}
		try {
			PreparedStatement ps = con.prepareStatement(query);
			ps.setInt(1, itemid);
			ResultSet rs = ps.executeQuery();
			if (rs.next())
				doWork(oId, rs, equip);
			rs.close();
			ps.close();
		} catch (SQLException e) {
			LOG.log(Level.WARNING, "Could not read MCDB data for item " + itemid, e);
		}
		loaded.add(oId);
	}

	public boolean loadAll() {
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = con.prepareStatement("SELECT * FROM `itemdata`");
			rs = ps.executeQuery();
			while (rs.next())
				doWork(rs.getInt("itemid"), rs, false);
			ps.close();
			rs.close();

			ps = con.prepareStatement("SELECT * FROM `equipdata`");
			rs = ps.executeQuery();
			while (rs.next())
				doWork(rs.getInt("equipid"), rs, true);
			ps.close();
			rs.close();

			ps = con.prepareStatement("SELECT `id`,`hunger` FROM `petdata`");
			rs = ps.executeQuery();
			while (rs.next())
				petHunger.put(Integer.valueOf(rs.getInt(1)), Integer.valueOf(rs.getInt(2)));
			rs.close();
			ps.close();

			Integer oId;
			ps = con.prepareStatement("SELECT `id`,`command`,`increase`,`prob` FROM `petinteractdata`");
			rs = ps.executeQuery();
			while (rs.next()) {
				oId = Integer.valueOf(rs.getInt(1));
				if (!petCommands.containsKey(oId))
					petCommands.put(oId, new HashMap<Byte, int[]>());
				petCommands.get(oId).put(Byte.valueOf(rs.getByte(2)), new int[] { rs.getInt(4), rs.getInt(3) });
			}
			rs.close();
			ps.close();

			ps = con.prepareStatement("SELECT `petid`,`evol`,`prob` FROM `petevolvedata`");
			rs = ps.executeQuery();
			while (rs.next()) {
				oId = Integer.valueOf(rs.getInt(1));
				if (!evolveChoices.containsKey(oId))
					evolveChoices.put(oId, new ArrayList<int[]>());
				evolveChoices.get(oId).add(new int[] { rs.getInt(2), rs.getInt(3) });
			}
			return true;
		} catch (SQLException ex) {
			LOG.log(Level.WARNING, "Could not load all item data from MCDB.", ex);
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

	private void loadPetData(int itemid) {
		Integer oId = Integer.valueOf(itemid);
		try {
			PreparedStatement ps = con.prepareStatement("SELECT `hunger` FROM `petdata` WHERE `id` = ?");
			ps.setInt(1, itemid);
			ResultSet rs = ps.executeQuery();
			if (rs.next())
				petHunger.put(oId, Integer.valueOf(rs.getInt(1)));
			rs.close();
			ps.close();

			ps = con.prepareStatement("SELECT `command`,`increase`,`prob` FROM `petinteractdata` WHERE `id` = ?");
			ps.setInt(1, itemid);
			rs = ps.executeQuery();
			if (rs.next()) {
				if (!petCommands.containsKey(oId))
					petCommands.put(oId, new HashMap<Byte, int[]>());
				petCommands.get(oId).put(Byte.valueOf(rs.getByte(1)), new int[] { rs.getInt(3), rs.getInt(2) });
			}
			rs.close();
			ps.close();

			ps = con.prepareStatement("SELECT `evol`,`prob` FROM `petevolvedata` WHERE `id` = ?");
			ps.setInt(1, itemid);
			rs = ps.executeQuery();
			if (rs.next()) {
				if (!evolveChoices.containsKey(oId))
					evolveChoices.put(oId, new ArrayList<int[]>());
				evolveChoices.get(oId).add(new int[] { rs.getInt(1), rs.getInt(2) });
			}
			rs.close();
			ps.close();
		} catch (SQLException e) {
			LOG.log(Level.WARNING, "Could not read MCDB data for pet " + itemid, e);
		}
	}

	private void doWork(Integer oId, ResultSet rs, boolean equip) throws SQLException {
		
	}
}
