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

package argonms.game.loading.npc;

import argonms.common.util.DatabaseManager;
import argonms.common.util.DatabaseManager.DatabaseType;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author GoldenKevin
 */
public class DefaultNpcDataLoader extends NpcDataLoader {
	private static final Logger LOG = Logger.getLogger(DefaultNpcDataLoader.class.getName());

	private final Map<Integer, NpcStorageKeeper> hardCodedTable;

	protected DefaultNpcDataLoader() {
		hardCodedTable = new HashMap<Integer, NpcStorageKeeper>();
		hardCodedTable.put(Integer.valueOf(1002005), new NpcStorageKeeper(100, 0));
		hardCodedTable.put(Integer.valueOf(1012009), new NpcStorageKeeper(100, 0));
		hardCodedTable.put(Integer.valueOf(1022005), new NpcStorageKeeper(100, 0));
		hardCodedTable.put(Integer.valueOf(1032006), new NpcStorageKeeper(100, 0));
		hardCodedTable.put(Integer.valueOf(1052017), new NpcStorageKeeper(100, 0));
		hardCodedTable.put(Integer.valueOf(1061008), new NpcStorageKeeper(100, 0));
		hardCodedTable.put(Integer.valueOf(1091004), new NpcStorageKeeper(100, 0));
		hardCodedTable.put(Integer.valueOf(2010006), new NpcStorageKeeper(100, 0));
		hardCodedTable.put(Integer.valueOf(2020004), new NpcStorageKeeper(150, 0));
		hardCodedTable.put(Integer.valueOf(2041008), new NpcStorageKeeper(100, 0));
		hardCodedTable.put(Integer.valueOf(2050004), new NpcStorageKeeper(100, 0));
		hardCodedTable.put(Integer.valueOf(2060008), new NpcStorageKeeper(100, 0));
		hardCodedTable.put(Integer.valueOf(2070000), new NpcStorageKeeper(100, 0));
		hardCodedTable.put(Integer.valueOf(2080005), new NpcStorageKeeper(100, 0));
		hardCodedTable.put(Integer.valueOf(2090000), new NpcStorageKeeper(100, 0));
		hardCodedTable.put(Integer.valueOf(2093003), new NpcStorageKeeper(100, 0));
		hardCodedTable.put(Integer.valueOf(2100000), new NpcStorageKeeper(100, 0));
		hardCodedTable.put(Integer.valueOf(2110000), new NpcStorageKeeper(100, 0));
		hardCodedTable.put(Integer.valueOf(9030100), new NpcStorageKeeper(1000, 2000));
		hardCodedTable.put(Integer.valueOf(9120009), new NpcStorageKeeper(100, 0));
		hardCodedTable.put(Integer.valueOf(9201081), new NpcStorageKeeper(100, 0));
		hardCodedTable.put(Integer.valueOf(9270042), new NpcStorageKeeper(100, 0));
	}

	@Override
	protected void load(int npcId) {
		storageCosts.put(Integer.valueOf(npcId), hardCodedTable.get(Integer.valueOf(npcId)));
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			con = DatabaseManager.getConnection(DatabaseType.STATE);
			ps = con.prepareStatement("SELECT `script` FROM `npcscriptnames` WHERE `npcid` = ?");
			ps.setInt(1, npcId);
			rs = ps.executeQuery();
			if (rs.next())
				scriptNames.put(Integer.valueOf(npcId), rs.getString(1));
		} catch (SQLException e) {
			LOG.log(Level.WARNING, "Could not read script name for NPC " + npcId, e);
		} finally {
			DatabaseManager.cleanup(DatabaseType.STATE, rs, ps, con);
		}
		loaded.add(Integer.valueOf(npcId));
	}

	@Override
	public boolean loadAll() {
		storageCosts.putAll(hardCodedTable);
		loaded.addAll(hardCodedTable.keySet());
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			con = DatabaseManager.getConnection(DatabaseType.STATE);
			ps = con.prepareStatement("SELECT `npcid`,`script` FROM `npcscriptnames`");
			rs = ps.executeQuery();
			while (rs.next()) {
				Integer npcId = Integer.valueOf(rs.getInt(1));
				scriptNames.put(npcId, rs.getString(2));
				loaded.add(npcId);
			}
		} catch (SQLException e) {
			LOG.log(Level.WARNING, "Could not load all NPC script names", e);
		} finally {
			DatabaseManager.cleanup(DatabaseType.STATE, rs, ps, con);
		}
		return true;
	}
}
