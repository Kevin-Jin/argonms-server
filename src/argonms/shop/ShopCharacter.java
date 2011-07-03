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

package argonms.shop;

import argonms.common.character.Player.LimitedActionCharacter;
import argonms.common.character.inventory.Inventory;
import argonms.common.character.inventory.Inventory.InventoryType;
import argonms.common.tools.DatabaseManager;
import argonms.common.tools.DatabaseManager.DatabaseType;
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
public class ShopCharacter extends LimitedActionCharacter {
	private static final Logger LOG = Logger.getLogger(ShopCharacter.class.getName());

	private ShopClient client;

	private ShopCharacter() {
		super();
	}

	public ShopClient getClient() {
		return client;
	}

	public void close() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public static ShopCharacter loadPlayer(ShopClient c, int id) {
		Connection con = null;
		PreparedStatement ps = null, ips = null;
		ResultSet rs = null, irs = null;
		try {
			con = DatabaseManager.getConnection(DatabaseType.STATE);
			ps = con.prepareStatement("SELECT * FROM `characters` WHERE `id` = ?");
			ps.setInt(1, id);
			rs = ps.executeQuery();
			if (!rs.next()) {
				LOG.log(Level.WARNING, "Client requested to load a non-existant" +
						" character w/ id {0} (account {1}).",
						new Object[] { id, c.getAccountId() });
				return null;
			}
			int accountid = rs.getInt(1);
			c.setAccountId(accountid); //we aren't aware of our accountid yet
			byte world = rs.getByte(2);
			c.setWorld(world); //we aren't aware of our world yet
			ShopCharacter p = new ShopCharacter();
			p.client = c;
			loadPlayerStats(rs, id, p, InventoryType.CASH_SHOP, new Inventory((byte) 0));
			rs.close();
			ps.close();

			ps = con.prepareStatement("SELECT * FROM `inventoryitems` WHERE `characterid` = ?"
					+ " AND `inventorytype` <= " + InventoryType.CASH.byteValue()
					+ " OR `accountid` = ? AND `inventorytype` = " + InventoryType.CASH_SHOP.byteValue());
			ps.setInt(1, id);
			ps.setInt(2, accountid);
			rs = ps.executeQuery();
			loadInventory(con, rs, p);
			return p;
		} catch (SQLException ex) {
			LOG.log(Level.WARNING, "Could not load character " + id + " from database", ex);
			return null;
		} finally {
			DatabaseManager.cleanup(DatabaseType.STATE, irs, ips, null);
			DatabaseManager.cleanup(DatabaseType.STATE, rs, ps, con);
		}
	}
}
