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

package argonms.game.loading.shop;

import argonms.common.tools.DatabaseManager;
import argonms.common.tools.DatabaseManager.DatabaseType;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author GoldenKevin
 */
public class DefaultNpcShopDataLoader extends NpcShopDataLoader {
	private static final Logger LOG = Logger.getLogger(DefaultNpcShopDataLoader.class.getName());

	@Override
	protected int load(int npcid) {
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			con = DatabaseManager.getConnection(DatabaseType.STATE);
			ps = con.prepareStatement("SELECT `shopid` FROM `shops` WHERE `npcid` = ?");
			ps.setInt(1, npcid);
			rs = ps.executeQuery();
			if (rs.next()) {
				int shopId = rs.getInt(1);
				npcToShop.put(Integer.valueOf(npcid), Integer.valueOf(shopId));

				List<NpcShop.ShopSlot> items = new ArrayList<NpcShop.ShopSlot>();
				PreparedStatement ips = null;
				ResultSet irs = null;
				try {
					ips = con.prepareStatement("SELECT `itemid`,`price` FROM `shopitems` WHERE `shopid` = ? ORDER BY `position` ASC");
					ips.setInt(1, shopId);
					irs = ips.executeQuery();
					while (irs.next())
						items.add(new NpcShop.ShopSlot(rs.getInt(1), (short) 1, rs.getInt(2)));
				} finally {
					DatabaseManager.cleanup(DatabaseType.STATE, irs, ips, null);
				}

				NpcShop shop = new NpcShop.DefaultNpcShopStock(items);
				loadedShops.put(Integer.valueOf(shopId), shop);
				return shopId;
			} else {
				return 0;
			}
		} catch (SQLException ex) {
			LOG.log(Level.WARNING, "Could not read data from default table for shop of NPC " + npcid, ex);
			return 0;
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
			Map<Integer, List<NpcShop.ShopSlot>> shopItems = new HashMap<Integer, List<NpcShop.ShopSlot>>();
			ps = con.prepareStatement("SELECT `shopid`,`itemid`,`price` FROM `shopitems` ORDER BY `shopid`,`position` ASC");
			rs = ps.executeQuery();
			boolean more = false;
			while (more || rs.next()) {
				int shopId = rs.getInt(1);
				items = new ArrayList<NpcShop.ShopSlot>();
				do
					items.add(new NpcShop.ShopSlot(rs.getInt(2), (short) 1, rs.getInt(3)));
				while ((more = rs.next()) && rs.getInt(1) == shopId);
				shopItems.put(Integer.valueOf(shopId), items);
			}
			rs.close();
			ps.close();

			ps = con.prepareStatement("SELECT `shopid`,`npcid` FROM `shops`");
			rs = ps.executeQuery();
			while (rs.next()) {
				Integer shopId = Integer.valueOf(rs.getInt(1));
				npcToShop.put(Integer.valueOf(rs.getInt(2)), shopId);
				List<NpcShop.ShopSlot> stock = shopItems.get(shopId);
				loadedShops.put(shopId, new NpcShop.DefaultNpcShopStock(stock != null ? stock : Collections.<NpcShop.ShopSlot>emptyList()));
			}
		} catch (SQLException ex) {
			LOG.log(Level.WARNING, "Could not load all shop data from MCDB.", ex);
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
