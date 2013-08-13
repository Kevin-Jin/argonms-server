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

package argonms.shop.loading.limitedcommodity;

import argonms.common.loading.DataFileType;
import argonms.common.util.DatabaseManager;
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
public abstract class LimitedCommodityDataLoader {
	private static final Logger LOG = Logger.getLogger(LimitedCommodityDataLoader.class.getName());

	private static LimitedCommodityDataLoader instance;

	protected final Map<Integer, LimitedCommodity> limitedCommodities;

	protected LimitedCommodityDataLoader() {
		limitedCommodities = new HashMap<Integer, LimitedCommodity>();
	}

	protected int getUsed(int itemId) {
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			con = DatabaseManager.getConnection(DatabaseManager.DatabaseType.STATE);
			ps = con.prepareStatement("SELECT `used` FROM `cashshoplimitedcommodities` WHERE `itemid` = ?");
			ps.setInt(1, itemId);
			rs = ps.executeQuery();
			if (rs.next())
				return rs.getInt(1);
		} catch (SQLException e) {
			LOG.log(Level.WARNING, "Could not determine remainder of limited commodity from database", e);
		} finally {
			DatabaseManager.cleanup(DatabaseManager.DatabaseType.STATE, rs, ps, con);
		}
		return 0;
	}

	public void commitUsed(int itemId, int used) {
		Connection con = null;
		PreparedStatement ps = null;
		try {
			con = DatabaseManager.getConnection(DatabaseManager.DatabaseType.STATE);
			ps = con.prepareStatement("INSERT INTO `cashshoplimitedcommodities` (`itemid`,`used`) VALUES (?,?) ON DUPLICATE KEY UPDATE `used` = ?");
			ps.setInt(1, itemId);
			ps.setInt(2, used);
			ps.setInt(3, used);
			ps.executeUpdate();
		} catch (SQLException e) {
			LOG.log(Level.WARNING, "Could not update remainder of limited commodity in database", e);
		} finally {
			DatabaseManager.cleanup(DatabaseManager.DatabaseType.STATE, null, ps, con);
		}
	}

	public abstract boolean loadAll();

	public LimitedCommodity getLimitedCommodity(int itemId) {
		return limitedCommodities.get(Integer.valueOf(itemId));
	}

	public Map<Integer, LimitedCommodity> getAllLimitedCommodities() {
		return limitedCommodities;
	}

	public static void setInstance(DataFileType wzType, String wzPath) {
		if (instance == null) {
			switch (wzType) {
				default:
					instance = new JsonLimitedCommodityDataLoader();
					break;
			}
		}
	}

	public static LimitedCommodityDataLoader getInstance() {
		return instance;
	}
}
