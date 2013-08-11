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

package argonms.shop.coupon;

import argonms.common.util.DatabaseManager;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author GoldenKevin
 */
public class CouponFactory {
	private static final Logger LOG = Logger.getLogger(CouponFactory.class.getName());

	private static final CouponFactory INSTANCE = new CouponFactory();

	private ConcurrentMap<String, Coupon> loadedCoupons;

	private CouponFactory() {
		loadedCoupons = new ConcurrentHashMap<String, Coupon>();
	}

	public Coupon getCoupon(String code) {
		Coupon c = new Coupon(code);
		synchronized (c) {
			Coupon existing = loadedCoupons.putIfAbsent(code, c);
			if (existing != null)
				return existing;

			Connection con = null;
			PreparedStatement ps = null;
			ResultSet rs = null;
			try {
				con = DatabaseManager.getConnection(DatabaseManager.DatabaseType.STATE);
				ps = con.prepareStatement("SELECT `maplepoints`,`mesos`,`remaininguses`,`expiredate` FROM `cashshopcoupons` WHERE `code` = ?");
				ps.setString(1, code);
				rs = ps.executeQuery();
				if (rs.next()) {
					c.setMaplePoints(rs.getInt(1));
					c.setMesos(rs.getInt(2));
					c.setRemainingUses(rs.getInt(3));
					c.setExpireDate(rs.getLong(4));
					rs.close();
					ps.close();

					ps = con.prepareStatement("SELECT `accountid` FROM `cashshopcouponusers` `u` LEFT JOIN `cashshopcoupons` `c` ON `u`.`couponentryid` = `c`.`entryid` WHERE `c`.`code` = ?");
					ps.setString(1, code);
					rs = ps.executeQuery();
					while (rs.next())
						c.addUser(rs.getInt(1));
					rs.close();
					ps.close();

					ps = con.prepareStatement("SELECT `sn` FROM `cashshopcouponitems` `i` LEFT JOIN `cashshopcoupons` `c` ON `i`.`couponentryid` = `c`.`entryid` WHERE `c`.`code` = ?");
					ps.setString(1, code);
					rs = ps.executeQuery();
					while (rs.next())
						c.addItem(rs.getInt(1));

					c.onInitialized();
					return c;
				}
			} catch (SQLException ex) {
				LOG.log(Level.WARNING, "Could not fetch coupon " + code, ex);
			} finally {
				DatabaseManager.cleanup(DatabaseManager.DatabaseType.STATE, rs, ps, con);
			}
		}
		loadedCoupons.remove(code);
		return null;
	}

	public void commitCoupon(Coupon c) {
		Connection con = null;
		PreparedStatement ps = null;
		try {
			con = DatabaseManager.getConnection(DatabaseManager.DatabaseType.STATE);
			synchronized (c) {
				ps = con.prepareStatement("INSERT INTO `cashshopcoupons` (`code`,`maplepoints`,`mesos`,`remaininguses`,`expiredate`) VALUES (?,?,?,?,?) "
						+ "ON DUPLICATE KEY UPDATE `maplepoints` = ?, `mesos` = ?, `remaininguses` = ?, `expiredate` = ?");
				ps.setString(1, c.getCode());
				ps.setInt(2, c.getMaplePointsReward());
				ps.setInt(3, c.getMesosReward());
				ps.setInt(4, c.getRemainingUses());
				ps.setLong(5, c.getExpireDate());
				ps.setInt(6, c.getMaplePointsReward());
				ps.setInt(7, c.getMesosReward());
				ps.setInt(8, c.getRemainingUses());
				ps.setLong(9, c.getExpireDate());
				ps.executeUpdate();

				if (c.shouldUpdateUsers()) {
					ps.close();
					ps = con.prepareStatement("DELETE `u`.* FROM `cashshopcouponusers` `u` LEFT JOIN `cashshopcoupons` `c` ON `u`.`couponentryid` = `c`.`entryid` WHERE `c`.`code` = ?");
					ps.setString(1, c.getCode());
					ps.executeUpdate();

					if (!c.getUsers().isEmpty()) {
						ps.close();
						ps = con.prepareStatement("INSERT INTO `cashshopcouponusers` (`couponentryid`,`accountid`) SELECT `entryid`,? FROM `cashshopcoupons` WHERE `code` = ?");
						ps.setString(2, c.getCode());
						for (Integer accountId : c.getUsers()) {
							ps.setInt(1, accountId.intValue());
							ps.addBatch();
						}
						ps.executeBatch();
					}
				}

				if (c.shouldUpdateItems()) {
					ps.close();
					ps = con.prepareStatement("DELETE `i`.* FROM `cashshopcouponitems` `i` LEFT JOIN `cashshopcoupons` `c` ON `i`.`couponentryid` = `c`.`entryid` WHERE `c`.`code` = ?");
					ps.setString(1, c.getCode());
					ps.executeUpdate();

					if (!c.getItems().isEmpty()) {
						ps.close();
						ps = con.prepareStatement("INSERT INTO `cashshopcouponitems` (`couponentryid`,`sn`) SELECT `entryid`,? FROM `cashshopcoupons` WHERE `code` = ?");
						ps.setString(2, c.getCode());
						for (Integer sn : c.getItems()) {
							ps.setInt(1, sn.intValue());
							ps.addBatch();
						}
						ps.executeBatch();
					}
				}
			}
		} catch (SQLException ex) {
			LOG.log(Level.WARNING, "Could not commit coupon " + c.getCode(), ex);
		} finally {
			DatabaseManager.cleanup(DatabaseManager.DatabaseType.STATE, null, ps, con);
		}
	}

	public static CouponFactory getInstance() {
		return INSTANCE;
	}
}
