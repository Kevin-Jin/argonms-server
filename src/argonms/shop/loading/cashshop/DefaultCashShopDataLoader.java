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

package argonms.shop.loading.cashshop;

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
public class DefaultCashShopDataLoader extends CashShopDataLoader {
	private static final Logger LOG = Logger.getLogger(DefaultCashShopDataLoader.class.getName());

	protected DefaultCashShopDataLoader() {

	}

	@Override
	public boolean loadAll() {
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			con = DatabaseManager.getConnection(DatabaseType.STATE);
			ps = con.prepareStatement("SELECT `sn`,`itemid`,`quantity`,`price`,`period`,`gender`,`onsale` FROM `commodities`");
			rs = ps.executeQuery();
			while (rs.next())
				commodities.put(Integer.valueOf(rs.getInt(1)), new Commodity(rs.getInt(2), rs.getShort(3), rs.getInt(4), rs.getByte(5), rs.getByte(6), rs.getBoolean(7)));
			rs.close();
			ps.close();

			//`package` doesn't have to be in order, but make sure everything in
			//the same package is adjacent to one another
			ps = con.prepareStatement("SELECT `package`,`sn` FROM `cashpackages` ORDER BY `package`");
			rs = ps.executeQuery();
			if (rs.next()) {
				int currentPackageId = rs.getInt(1);
				List<Integer> currentPackage = new ArrayList<Integer>();
				currentPackage.add(Integer.valueOf(rs.getInt(2)));
				while (rs.next()) {
					int packageId = rs.getInt(1);
					if (currentPackageId != packageId) {
						int[] array = new int[currentPackage.size()];
						for (int i = 0; i < array.length; i++)
							array[i] = currentPackage.get(i).intValue();
						packages.put(Integer.valueOf(currentPackageId), array);
						currentPackageId = packageId;
						currentPackage.clear();
					}
					currentPackage.add(Integer.valueOf(rs.getInt(2)));
				}
				int[] array = new int[currentPackage.size()];
				for (int i = 0; i < array.length; i++)
					array[i] = currentPackage.get(i).intValue();
				packages.put(Integer.valueOf(currentPackageId), array);
			}
			return true;
		} catch (SQLException e) {
			LOG.log(Level.WARNING, "Error loading commodities and cash packages.", e);
			return false;
		} finally {
			DatabaseManager.cleanup(DatabaseType.STATE, rs, ps, con);
		}
	}
}
