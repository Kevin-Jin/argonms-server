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

package argonms.shop.character;

import argonms.common.character.BuddyListEntry;
import argonms.common.character.Cooldown;
import argonms.common.character.LoggedInPlayer;
import argonms.common.character.QuestEntry;
import argonms.common.character.ShopPlayerContinuation;
import argonms.common.character.SkillEntry;
import argonms.common.character.inventory.IInventory;
import argonms.common.character.inventory.Inventory.InventoryType;
import argonms.common.net.external.CommonPackets;
import argonms.common.util.DatabaseManager;
import argonms.common.util.DatabaseManager.DatabaseType;
import argonms.shop.ShopServer;
import argonms.shop.net.external.CashShopPackets;
import argonms.shop.net.external.ShopClient;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author GoldenKevin
 */
public class ShopCharacter extends LoggedInPlayer {
	private static final Logger LOG = Logger.getLogger(ShopCharacter.class.getName());

	public static final int
		PAYPAL_NX = 1,
		MAPLE_POINTS = 2,
		GAME_CARD_NX = 4
	;

	private ShopClient client;

	private ShopBuddyList buddies;
	private int partyId;
	private int guildId;
	private byte maxCharacters;
	private CashShopStaging shopInventory;
	private volatile int mesos;
	private int birthday;
	private final AtomicInteger[] cashShopBalance;
	private final Map<Integer, SkillEntry> skills;
	private final Map<Integer, Cooldown> cooldowns;
	private final Map<Short, QuestEntry> questStatuses;
	private final List<Integer> wishList;
	private ShopPlayerContinuation returnContext;

	private ShopCharacter() {
		super();
		cashShopBalance = new AtomicInteger[4];
		skills = new HashMap<Integer, SkillEntry>();
		cooldowns = new HashMap<Integer, Cooldown>();
		questStatuses = new HashMap<Short, QuestEntry>();
		wishList = new ArrayList<Integer>(10);
	}

	@Override
	public ShopClient getClient() {
		return client;
	}

	@Override
	public ShopBuddyList getBuddyList() {
		return buddies;
	}

	public int getPartyId() {
		return partyId;
	}

	public int getGuildId() {
		return guildId;
	}

	public short getMaxCharacters() {
		return maxCharacters;
	}

	public short increaseMaxCharacters() {
		return ++maxCharacters;
	}

	public int getBirthday() {
		return birthday;
	}

	@Override
	public int getMesos() {
		return mesos;
	}

	public boolean gainMesos(int gain) {
		long newValue = (long) mesos + gain;
		if (newValue <= Integer.MAX_VALUE && newValue >= 0) {
			mesos = (int) newValue;
			return true;
		}
		return false;
	}

	public int getCashShopCurrency(int type) {
		return cashShopBalance[type - 1].get();
	}

	public void setCashShopCurrency(int type, int newVal) {
		cashShopBalance[type - 1].set(newVal);
		getClient().getSession().send(CashShopPackets.writeCashShopCurrencyBalance(this));
	}

	public boolean gainCashShopCurrency(int type, int gain) {
		long newValue = (long) getCashShopCurrency(type) + gain;
		if (newValue <= Integer.MAX_VALUE && newValue >= 0) {
			setCashShopCurrency(type, (int) newValue);
			return true;
		}
		return false;
	}

	public CashShopStaging getCashShopInventory() {
		return shopInventory;
	}

	@Override
	public Map<Integer, SkillEntry> getSkillEntries() {
		return Collections.unmodifiableMap(skills);
	}

	@Override
	public Map<Integer, Cooldown> getCooldowns() {
		return Collections.unmodifiableMap(cooldowns);
	}

	@Override
	public Map<Short, QuestEntry> getAllQuests() {
		return Collections.unmodifiableMap(questStatuses);
	}

	public List<Integer> getWishListSerialNumbers() {
		return Collections.unmodifiableList(wishList);
	}

	public void setWishList(List<Integer> newList) {
		wishList.clear();
		wishList.addAll(newList);
	}

	private void removeCooldown(int skill) {
		cooldowns.remove(Integer.valueOf(skill)).cancel();
	}

	private void addCooldown(final int skill, short time) {
		cooldowns.put(Integer.valueOf(skill), new Cooldown(time * 1000, new Runnable() {
			@Override
			public void run() {
				removeCooldown(skill);
				getClient().getSession().send(CommonPackets.writeCooldown(skill, (short) 0));
			}
		}));
	}

	public void setReturnContext(ShopPlayerContinuation context) {
		returnContext = context;
	}

	public ShopPlayerContinuation getReturnContext() {
		returnContext.compactForReturn();
		return returnContext;
	}

	private void prepareExitChannel() {
		saveCharacter();
	}

	public void prepareChannelChange() {
		if (partyId != 0)
			ShopServer.getInstance().getCrossServerInterface().sendPartyMemberLogOffNotifications(this, false);
		if (guildId != 0)
			ShopServer.getInstance().getCrossServerInterface().sendGuildMemberLogOffNotifications(this, false);
		prepareExitChannel();
	}

	public void prepareLogOff() {
		ShopServer.getInstance().getCrossServerInterface().sendBuddyLogOffNotifications(this);
		if (partyId != 0)
			ShopServer.getInstance().getCrossServerInterface().sendPartyMemberLogOffNotifications(this, true);
		if (guildId != 0)
			ShopServer.getInstance().getCrossServerInterface().sendGuildMemberLogOffNotifications(this, true);
		prepareExitChannel();
	}

	public void saveCharacter() {
		int prevTransactionIsolation = Connection.TRANSACTION_REPEATABLE_READ;
		boolean prevAutoCommit = true;
		Connection con = null;
		try {
			con = DatabaseManager.getConnection(DatabaseType.STATE);
			prevTransactionIsolation = con.getTransactionIsolation();
			prevAutoCommit = con.getAutoCommit();
			con.setTransactionIsolation(Connection.TRANSACTION_READ_UNCOMMITTED);
			con.setAutoCommit(false);
			updateDbAccount(con);
			updateDbStats(con);
			updateDbInventory(con);
			updateDbWishList(con);
			con.commit();
		} catch (Throwable ex) {
			LOG.log(Level.WARNING, "Could not save character " + getDataId() + ". Rolling back all changes...", ex);
			if (con != null) {
				try {
					con.rollback();
				} catch (SQLException ex2) {
					LOG.log(Level.WARNING, "Error rolling back character.", ex2);
				}
			}
		} finally {
			if (con != null) {
				try {
					con.setAutoCommit(prevAutoCommit);
					con.setTransactionIsolation(prevTransactionIsolation);
				} catch (SQLException ex) {
					LOG.log(Level.WARNING, "Could not reset Connection config after saving character " + getDataId(), ex);
				}
			}
			DatabaseManager.cleanup(DatabaseType.STATE, null, null, con);
		}
	}

	private void updateDbAccount(Connection con) throws SQLException {
		PreparedStatement ps = null;
		try {
			ps = con.prepareStatement("UPDATE `accounts` SET `characters` = ?, `paypalnx` = ?, `maplepoints` = ?, `gamecardnx` = ? WHERE `id` = ?");
			ps.setByte(1, maxCharacters);
			ps.setInt(2, getCashShopCurrency(PAYPAL_NX));
			ps.setInt(3, getCashShopCurrency(MAPLE_POINTS));
			ps.setInt(4, getCashShopCurrency(GAME_CARD_NX));
			ps.setInt(5, client.getAccountId());
			ps.executeUpdate();
		} catch (SQLException e) {
			throw new SQLException("Failed to save account-info of character " + name, e);
		} finally {
			DatabaseManager.cleanup(DatabaseType.STATE, null, ps, null);
		}
	}

	private void updateDbStats(Connection con) throws SQLException {
		PreparedStatement ps = null;
		try {
			ps = con.prepareStatement("UPDATE `characters` SET "
					+ "`mesos` = ?, `equipslots` = ?, `useslots` = ?, `setupslots` = ?, `etcslots` = ?, `cashslots` = ? "
					+ "WHERE `id` = ?");
			ps.setInt(1, mesos);
			ps.setShort(2, getInventory(InventoryType.EQUIP).getMaxSlots());
			ps.setShort(3, getInventory(InventoryType.USE).getMaxSlots());
			ps.setShort(4, getInventory(InventoryType.SETUP).getMaxSlots());
			ps.setShort(5, getInventory(InventoryType.ETC).getMaxSlots());
			ps.setShort(6, getInventory(InventoryType.CASH).getMaxSlots());
			ps.setInt(7, getDataId());
			int updateRows = ps.executeUpdate();
			if (updateRows < 1)
				LOG.log(Level.WARNING, "Updating a deleted character with name {0} of account {1}.",
						new Object[] { name, client.getAccountId() });
		} catch (SQLException e) {
			throw new SQLException("Failed to save stats of character " + name, e);
		} finally {
			DatabaseManager.cleanup(DatabaseType.STATE, null, ps, null);
		}
	}

	private void updateDbInventory(Connection con) throws SQLException {
		String invUpdate = "DELETE FROM `inventoryitems` WHERE "
				+ "`characterid` = ? AND `inventorytype` <= " + InventoryType.CASH.byteValue()
				+ " OR `accountid` = ? AND `inventorytype` = " + InventoryType.CASH_SHOP.byteValue();
		PreparedStatement ps = null, ips = null;
		ResultSet rs = null;
		try {
			ps = con.prepareStatement(invUpdate);
			ps.setInt(1, getDataId());
			ps.setInt(2, client.getAccountId());
			ps.executeUpdate();
			ps.close();

			EnumMap<InventoryType, IInventory> union = new EnumMap<InventoryType, IInventory>(getInventories());
			union.put(InventoryType.CASH_SHOP, shopInventory);
			commitInventory(con, union);
		} catch (SQLException e) {
			throw new SQLException("Failed to save inventory of character " + name, e);
		} finally {
			DatabaseManager.cleanup(DatabaseType.STATE, null, ips, null);
			DatabaseManager.cleanup(DatabaseType.STATE, rs, ps, null);
		}
	}

	private void updateDbWishList(Connection con) throws SQLException {
		PreparedStatement ps = null;
		try {
			ps = con.prepareStatement("DELETE FROM `wishlists` WHERE `characterid` = ?");
			ps.setInt(1, getDataId());
			ps.executeUpdate();
			ps.close();

			ps = con.prepareStatement("INSERT INTO `wishlists` (`characterid`,`sn`) VALUES (?,?)");
			ps.setInt(1, getDataId());
			for (Integer sn : wishList) {
				ps.setInt(2, sn.intValue());
				ps.addBatch();
			}
			ps.executeBatch();
		} catch (SQLException e) {
			throw new SQLException("Failed to save wishlist of character " + name, e);
		} finally {
			DatabaseManager.cleanup(DatabaseType.STATE, null, ps, null);
		}
	}

	@Override
	protected void loadInventory(Connection con, ResultSet rs, Map<InventoryType, ? extends IInventory> inventories) throws SQLException {
		super.loadInventory(con, rs, inventories);
		shopInventory.loadPurchaseProperties(getClient().getAccountId());
	}

	public static ShopCharacter loadPlayer(ShopClient c, int id) {
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			con = DatabaseManager.getConnection(DatabaseType.STATE);
			ps = con.prepareStatement("SELECT `c`.*,`a`.`name`,`a`.`characters`,`a`.`birthday`,`a`.`paypalnx`,`a`.`maplepoints`,`a`.`gamecardnx` "
					+ "FROM `characters` `c` LEFT JOIN `accounts` `a` ON `c`.`accountid` = `a`.`id` "
					+ "WHERE `c`.`id` = ?");
			ps.setInt(1, id);
			rs = ps.executeQuery();
			if (!rs.next()) {
				LOG.log(Level.WARNING, "Client requested to load a nonexistent character w/ id {0} (account {1}).",
						new Object[] { id, c.getAccountId() });
				return null;
			}
			int accountid = rs.getInt(1);
			c.setAccountId(accountid); //we aren't aware of our accountid yet
			byte world = rs.getByte(2);
			c.setWorld(world); //we aren't aware of our world yet
			ShopCharacter p = new ShopCharacter();
			p.client = c;
			p.loadPlayerStats(rs, id);
			p.mesos = rs.getInt(26);
			short maxBuddies = rs.getShort(32);
			c.setAccountName(rs.getString(42));
			p.maxCharacters = rs.getByte(43);
			p.birthday = rs.getInt(44);
			p.cashShopBalance[PAYPAL_NX - 1] = new AtomicInteger(rs.getInt(45));
			p.cashShopBalance[MAPLE_POINTS - 1] = new AtomicInteger(rs.getInt(46));
			p.cashShopBalance[GAME_CARD_NX - 1] = new AtomicInteger(rs.getInt(47));
			rs.close();
			ps.close();

			p.shopInventory = new CashShopStaging();
			EnumMap<InventoryType, IInventory> invUnion = new EnumMap<InventoryType, IInventory>(p.getInventories());
			invUnion.put(InventoryType.CASH_SHOP, p.shopInventory);
			ps = con.prepareStatement("SELECT * FROM `inventoryitems` WHERE "
					+ "`characterid` = ? AND `inventorytype` <= " + InventoryType.CASH.byteValue()
					+ " OR `accountid` = ? AND `inventorytype` = " + InventoryType.CASH_SHOP.byteValue());
			ps.setInt(1, id);
			ps.setInt(2, accountid);
			rs = ps.executeQuery();
			p.loadInventory(con, rs, invUnion);
			rs.close();
			ps.close();

			ps = con.prepareStatement("SELECT `skillid`,`level`,`mastery` "
					+ "FROM `skills` WHERE `characterid` = ?");
			ps.setInt(1, id);
			rs = ps.executeQuery();
			while (rs.next())
				p.skills.put(Integer.valueOf(rs.getInt(1)), new SkillEntry(rs.getByte(2), rs.getByte(3)));
			rs.close();
			ps.close();

			ps = con.prepareStatement("SELECT `skillid`,`remaining` "
					+ "FROM `cooldowns` WHERE `characterid` = ?");
			ps.setInt(1, id);
			rs = ps.executeQuery();
			while (rs.next())
				p.addCooldown(rs.getInt(1), rs.getShort(2));
			rs.close();
			ps.close();

			List<BuddyListEntry> buddies = new ArrayList<BuddyListEntry>();
			ps = con.prepareStatement("SELECT `e`.`buddy` AS `id`,"
					+ "IF(ISNULL(`c`.`name`),`e`.`buddyname`,`c`.`name`) AS `name`,`e`.`status` "
					+ "FROM `buddyentries` `e` LEFT JOIN `characters` `c` ON `c`.`id` = `e`.`buddy` "
					+ "WHERE `owner` = ?");
			ps.setInt(1, id);
			rs = ps.executeQuery();
			while (rs.next()) {
				byte status = rs.getByte(3);
				if (status != BuddyListEntry.STATUS_INVITED)
					buddies.add(new BuddyListEntry(rs.getInt(1), rs.getString(2), status));
			}
			rs.close();
			ps.close();
			p.buddies = new ShopBuddyList(maxBuddies, buddies);

			ps = con.prepareStatement("SELECT `partyid` FROM `parties` WHERE `characterid` = ?");
			ps.setInt(1, id);
			rs = ps.executeQuery();
			if (rs.next())
				p.partyId = rs.getInt(1);
			rs.close();
			ps.close();

			ps = con.prepareStatement("SELECT `g`.`id` FROM `guilds` `g` LEFT JOIN `guildmembers` `m` ON `g`.`id` = `m`.`guildid` WHERE `m`.`characterid` = ?");
			ps.setInt(1, id);
			rs = ps.executeQuery();
			if (rs.next())
				p.guildId = rs.getInt(1);
			rs.close();
			ps.close();

			ps = con.prepareStatement("SELECT `id`,`questid`,`state`,`completed` "
					+ "FROM `queststatuses` WHERE `characterid` = ?");
			ps.setInt(1, id);
			rs = ps.executeQuery();
			PreparedStatement mps = null;
			ResultSet mrs;
			try {
				mps = con.prepareStatement("SELECT `mobid`,`count` "
						+ "FROM `questmobprogress` WHERE `queststatusid` = ?");
				while (rs.next()) {
					int questEntryId = rs.getInt(1);
					short questId = rs.getShort(2);
					Map<Integer, AtomicInteger> mobProgress = new LinkedHashMap<Integer, AtomicInteger>();
					mps.setInt(1, questEntryId);
					mrs = null;
					try {
						mrs = mps.executeQuery();
						while (mrs.next())
							mobProgress.put(Integer.valueOf(mrs.getInt(1)), new AtomicInteger(mrs.getShort(2)));
					} finally {
						DatabaseManager.cleanup(DatabaseType.STATE, mrs, null, null);
					}
					QuestEntry status = new QuestEntry(rs.getByte(3), mobProgress);
					status.setCompletionTime(rs.getLong(4));
					p.questStatuses.put(Short.valueOf(questId), status);
				}
			} finally {
				DatabaseManager.cleanup(DatabaseType.STATE, null, mps, null);
			}

			ps = con.prepareStatement("SELECT `sn` FROM `wishlists` WHERE `characterid` = ?");
			ps.setInt(1, id);
			rs = ps.executeQuery();
			while (rs.next())
				p.wishList.add(Integer.valueOf(rs.getInt(1)));
			return p;
		} catch (SQLException ex) {
			LOG.log(Level.WARNING, "Could not load character " + id + " from database", ex);
			return null;
		} finally {
			DatabaseManager.cleanup(DatabaseType.STATE, rs, ps, con);
		}
	}

	public static int getAccountIdFromName(String name) {
		int id = -1;
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			con = DatabaseManager.getConnection(DatabaseType.STATE);
			ps = con.prepareStatement("SELECT `a`.`id` FROM `characters` `c` LEFT JOIN `accounts` `a` ON `c`.`accountid` = `a`.`id` WHERE `c`.`name` = ?");
			ps.setString(1, name);
			rs = ps.executeQuery();
			if (rs.next())
				id = rs.getInt(1);
		} catch (SQLException ex) {
			LOG.log(Level.WARNING, "Could not find account id of character " + name, ex);
		} finally {
			DatabaseManager.cleanup(DatabaseType.STATE, rs, ps, con);
		}
		return id;
	}
}
