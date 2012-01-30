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

package argonms.game.loading.shop;

import argonms.common.util.DatabaseManager;
import argonms.common.util.DatabaseManager.DatabaseType;
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
public class McdbNpcShopDataLoader extends NpcShopDataLoader {
	private static final Logger LOG = Logger.getLogger(McdbNpcShopDataLoader.class.getName());

	private final Map<Integer, Map<Integer, Double>> rechargeTiers;

	protected McdbNpcShopDataLoader() {
		rechargeTiers = new HashMap<Integer, Map<Integer, Double>>();
	}

	private boolean loadRechargeTier(int tier, ResultSet rs) throws SQLException {
		boolean more;
		Map<Integer, Double> tierData = new HashMap<Integer, Double>();
		do {
			int itemId = rs.getInt(2);
			double price = rs.getDouble(3);
			tierData.put(Integer.valueOf(itemId), Double.valueOf(price));
		} while ((more = rs.next()) && rs.getInt(1) == tier);
		rechargeTiers.put(Integer.valueOf(tier), Collections.unmodifiableMap(tierData));
		return more;
	}

	@Override
	protected int load(int npcid) {
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			con = DatabaseManager.getConnection(DatabaseType.WZ);
			ps = con.prepareStatement("SELECT `shopid`,`rechargetier` FROM `shopdata` WHERE `npcid` = ?");
			ps.setInt(1, npcid);
			rs = ps.executeQuery();
			if (rs.next()) {
				int shopId = rs.getInt(1);
				npcToShop.put(Integer.valueOf(npcid), Integer.valueOf(shopId));

				Map<Integer, Double> rechargeables;
				int rechargeTier = rs.getInt(2);
				if (rechargeTier == 0) {
					rechargeables = Collections.emptyMap();
				} else {
					if (!rechargeTiers.containsKey(Integer.valueOf(rechargeTier))) {
						PreparedStatement rps = null;
						ResultSet rrs = null;
						try {
							rps = con.prepareStatement("SELECT `itemid`,`price` FROM `rechargedata` WHERE `id` = ?");
							rps.setInt(1, rechargeTier);
							rrs = rps.executeQuery();
							if (rrs.next())
								loadRechargeTier(rechargeTier, rrs);
						} finally {
							DatabaseManager.cleanup(DatabaseType.WZ, rrs, rps, null);
						}
					}
					rechargeables = rechargeTiers.get(Integer.valueOf(rechargeTier));
				}

				List<NpcShop.ShopSlot> items = new ArrayList<NpcShop.ShopSlot>();
				PreparedStatement ips = null;
				ResultSet irs = null;
				try {
					ips = con.prepareStatement("SELECT `itemid`,`quantity`,`price` FROM `shopitemdata` WHERE `shopid` = ? ORDER BY `sort` DESC");
					ips.setInt(1, shopId);
					irs = ips.executeQuery();
					while (irs.next())
						items.add(new NpcShop.ShopSlot(irs.getInt(1), irs.getShort(2), irs.getInt(3)));
				} finally {
					DatabaseManager.cleanup(DatabaseType.WZ, irs, ips, null);
				}

				//saves a bit of memory - shops that use the same recharge tier
				//just use aliases of the same immutable map
				NpcShop shop = new NpcShop.McdbNpcShopStock(rechargeables, items);
				loadedShops.put(Integer.valueOf(shopId), shop);
				return shopId;
			} else {
				return 0;
			}
		} catch (SQLException ex) {
			LOG.log(Level.WARNING, "Could not read MCDB data for shop of NPC " + npcid, ex);
			return 0;
		} finally {
			DatabaseManager.cleanup(DatabaseType.WZ, rs, ps, con);
		}
	}

	@Override
	public boolean loadAll() {
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		List<NpcShop.ShopSlot> items;
		try {
			con = DatabaseManager.getConnection(DatabaseType.WZ);
			ps = con.prepareStatement("SELECT `id`,`itemid`,`price` FROM `rechargedata` ORDER BY `id` ASC");
			rs = ps.executeQuery();
			boolean more = false;
			while (more || rs.next())
				more = loadRechargeTier(rs.getInt(1), rs);
			rs.close();
			ps.close();

			Map<Integer, List<NpcShop.ShopSlot>> shopItems = new HashMap<Integer, List<NpcShop.ShopSlot>>();
			ps = con.prepareStatement("SELECT `shopid`,`itemid`,`quantity`,`price` FROM `shopitemdata` ORDER BY `shopid`,`sort` DESC");
			rs = ps.executeQuery();
			more = false;
			while (more || rs.next()) {
				int shopId = rs.getInt(1);
				items = new ArrayList<NpcShop.ShopSlot>();
				do
					items.add(new NpcShop.ShopSlot(rs.getInt(2), rs.getShort(3), rs.getInt(4)));
				while ((more = rs.next()) && rs.getInt(1) == shopId);
				shopItems.put(Integer.valueOf(shopId), items);
			}
			rs.close();
			ps.close();

			ps = con.prepareStatement("SELECT `shopid`,`npcid`,`rechargetier` FROM `shopdata`");
			rs = ps.executeQuery();
			while (rs.next()) {
				int shopId = rs.getInt(1);
				npcToShop.put(Integer.valueOf(rs.getInt(2)), Integer.valueOf(shopId));
				int rechargeTier = rs.getInt(3);
				Map<Integer, Double> rechargeables = rechargeTier != 0 ? rechargeTiers.get(Integer.valueOf(rechargeTier)) : Collections.<Integer, Double>emptyMap();
				loadedShops.put(Integer.valueOf(shopId), new NpcShop.McdbNpcShopStock(rechargeables != null ? rechargeables : Collections.<Integer, Double>emptyMap(), shopItems.get(Integer.valueOf(shopId))));
			}
		} catch (SQLException ex) {
			LOG.log(Level.WARNING, "Could not load all shop data from MCDB.", ex);
			return false;
		} finally {
			DatabaseManager.cleanup(DatabaseType.WZ, rs, ps, con);
		}
		return false;
	}

	@Override
	public boolean canLoad(int npcid) {
		if (npcToShop.containsKey(Integer.valueOf(npcid)))
			return true;
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			con = DatabaseManager.getConnection(DatabaseType.WZ);
			ps = con.prepareStatement("SELECT * FROM `shopdata` WHERE `npcid` = ?");
			ps.setInt(1, npcid);
			rs = ps.executeQuery();
			if (rs.next())
				return true;
			return false;
		} catch (SQLException ex) {
			LOG.log(Level.WARNING, "Could not use MCDB to determine whether npc " + npcid + " has a shop.", ex);
			return false;
		} finally {
			DatabaseManager.cleanup(DatabaseType.WZ, rs, ps, con);
		}
	}
}
