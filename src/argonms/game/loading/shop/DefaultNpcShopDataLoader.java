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

package argonms.game.loading.shop;

import argonms.common.util.DatabaseManager;
import argonms.common.util.DatabaseManager.DatabaseType;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author GoldenKevin
 */
public class DefaultNpcShopDataLoader extends NpcShopDataLoader {
	private static final Logger LOG = Logger.getLogger(DefaultNpcShopDataLoader.class.getName());

	@Override
	protected void load(int npcid) {
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			con = DatabaseManager.getConnection(DatabaseType.STATE);
			List<NpcShop.ShopSlot> items = new ArrayList<NpcShop.ShopSlot>();
			ps = con.prepareStatement("SELECT `itemid`,`price` FROM `shopitems` WHERE `npcid` = ? ORDER BY `position` ASC");
			ps.setInt(1, npcid);
			rs = ps.executeQuery();
			while (rs.next())
				items.add(new NpcShop.ShopSlot(rs.getInt(1), (short) 1, rs.getInt(2)));

			if (!items.isEmpty()) {
				NpcShop shop = new NpcShop.DefaultNpcShopStock(items);
				loadedShops.put(Integer.valueOf(npcid), shop);
			} else {
				loadedShops.put(Integer.valueOf(npcid), null);
			}
		} catch (SQLException ex) {
			LOG.log(Level.WARNING, "Could not read data from default table for shop of NPC " + npcid, ex);
		} finally {
			DatabaseManager.cleanup(DatabaseType.STATE, rs, ps, con);
		}
	}

	@Override
	public boolean loadAll() {
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		List<NpcShop.ShopSlot> items;
		try {
			con = DatabaseManager.getConnection(DatabaseType.STATE);
			ps = con.prepareStatement("SELECT `npcid`,`itemid`,`price` FROM `shopitems` ORDER BY `npcid`,`position` ASC");
			rs = ps.executeQuery();
			boolean more = false;
			while (more || rs.next()) {
				int npcId = rs.getInt(1);
				items = new ArrayList<NpcShop.ShopSlot>();
				do
					items.add(new NpcShop.ShopSlot(rs.getInt(2), (short) 1, rs.getInt(3)));
				while ((more = rs.next()) && rs.getInt(1) == npcId);
				loadedShops.put(Integer.valueOf(npcId), new NpcShop.DefaultNpcShopStock(items));
			}
		} catch (SQLException ex) {
			LOG.log(Level.WARNING, "Could not load all shop data.", ex);
			return false;
		} finally {
			DatabaseManager.cleanup(DatabaseType.STATE, rs, ps, con);
		}
		return false;
	}

	@Override
	public boolean canLoad(int npcid) {
		return false;
	}
}
