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

import argonms.common.character.Player;
import argonms.common.character.inventory.IInventory;
import argonms.common.character.inventory.Inventory;
import argonms.common.character.inventory.InventorySlot;
import argonms.common.character.inventory.InventoryTools;
import argonms.common.character.inventory.Pet;
import argonms.common.util.DatabaseManager;
import argonms.common.util.collections.Pair;
import argonms.shop.ShopServer;
import argonms.shop.loading.cashshop.CashShopDataLoader;
import argonms.shop.loading.cashshop.Commodity;
import argonms.shop.net.external.CashShopPackets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author GoldenKevin
 */
public class CashShopStaging implements IInventory {
	private static final Logger LOG = Logger.getLogger(ShopCharacter.class.getName());

	private static final short MAX_SLOTS = 0xFF;

	public static class CashItemGiftNotification {
		private final long uniqueId;
		private final int itemId;
		private final String sender;
		private final String message;

		public CashItemGiftNotification(long uniqueId, int dataId, String sender, String message) {
			this.uniqueId = uniqueId;
			this.itemId = dataId;
			this.sender = sender;
			this.message = message;
		}

		public CashItemGiftNotification(CashPurchaseProperties props, InventorySlot slot, String message) {
			uniqueId = slot.getUniqueId();
			itemId = slot.getDataId();
			sender = props.getGifterCharacterName();
			this.message = message;
		}

		public long getUniqueId() {
			return uniqueId;
		}

		public int getItemId() {
			return itemId;
		}

		public String getSender() {
			return sender;
		}

		public String getMessage() {
			return message;
		}
	}

	public static class CashPurchaseProperties {
		private int purchaserAccountId;
		private String gifterName;
		private int serialNumber;

		private CashPurchaseProperties() {
			
		}

		public CashPurchaseProperties(int account, String character, int sn) {
			purchaserAccountId = account;
			gifterName = character;
			serialNumber = sn;
		}

		public int getPurchaserAccountId() {
			return purchaserAccountId;
		}

		public String getGifterCharacterName() {
			return gifterName;
		}

		public int getSerialNumber() {
			return serialNumber;
		}

		/* package-private */ static CashPurchaseProperties loadFromDatabase(ResultSet rs, int itemId, int defaultAccount) throws SQLException {
			CashPurchaseProperties props = new CashPurchaseProperties();
			props.purchaserAccountId = rs.getInt(1);
			if (rs.wasNull())
				props.purchaserAccountId = defaultAccount;
			props.gifterName = rs.getString(2);
			props.serialNumber = rs.getInt(3);
			return props;
		}

		public static CashPurchaseProperties loadFromDatabase(long uniqueId, int itemId, int defaultAccount) {
			Connection con = null;
			PreparedStatement ps = null;
			ResultSet rs = null;
			try {
				con = DatabaseManager.getConnection(DatabaseManager.DatabaseType.STATE);
				ps = con.prepareStatement("SELECT `purchaseracctid`,`gifterchrname`,`serialnumber` FROM `cashshoppurchases` WHERE `uniqueid` = ?");
				ps.setLong(1, uniqueId);
				rs = ps.executeQuery();
				if (rs.next())
					return loadFromDatabase(rs, itemId, defaultAccount);
			} catch (SQLException ex) {
				LOG.log(Level.WARNING, "Could not load cash shop purchase properties from database", ex);
			} finally {
				DatabaseManager.cleanup(DatabaseManager.DatabaseType.STATE, rs, ps, con);
			}
			return null;
		}
	}

	public interface ItemManipulator {
		public boolean manipulate(InventorySlot item, int serialNumber, Commodity c);
	}

	private final ReadWriteLock locks;
	private final Map<Long, InventorySlot> slots;
	private final Map<Long, CashPurchaseProperties> purchaseProperties;
	private final List<CashItemGiftNotification> giftNotifications;

	public CashShopStaging() {
		//forgo to the overhead of ConcurrentHashMap. with no scripts and
		//commands, we are guaranteed to not do much concurrency in cash shop
		locks = new ReentrantReadWriteLock();
		slots = new LinkedHashMap<Long, InventorySlot>();
		purchaseProperties = new HashMap<Long, CashPurchaseProperties>();
		giftNotifications = new ArrayList<CashItemGiftNotification>();
	}

	public void loadPurchaseProperties(int accountId) {
		lockWrite();
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		boolean locked = false;
		try {
			con = DatabaseManager.getConnection(DatabaseManager.DatabaseType.STATE);

			ps = con.prepareStatement("SELECT `purchaseracctid`,`gifterchrname`,`serialnumber` FROM `cashshoppurchases` WHERE `uniqueid` = ?");
			for (Long uniqueId : slots.keySet()) {
				ps.setLong(1, uniqueId.longValue());
				rs = ps.executeQuery();
				if (rs.next())
					purchaseProperties.put(uniqueId, CashPurchaseProperties.loadFromDatabase(rs, slots.get(uniqueId).getDataId(), accountId));
				rs.close();
			}
			ps.close();

			ps = con.prepareStatement("LOCK TABLE `cashitemgiftnotes` WRITE");
			ps.executeUpdate();
			locked = true;
			ps.close();

			ps = con.prepareStatement("SELECT `uniqueid`,`message` FROM `cashitemgiftnotes` WHERE `recipientacctid` = ?");
			ps.setInt(1, accountId);
			rs = ps.executeQuery();
			while (rs.next()) {
				Long oUid = Long.valueOf(rs.getLong(1));
				InventorySlot item = slots.get(oUid);
				CashPurchaseProperties props = purchaseProperties.get(oUid);
				if (item == null || props == null) {
					LOG.log(Level.FINE, "Dropping gift with unique ID {0} for {1}. Missing item data or not owned by recipient.", new Object[] { oUid, Integer.valueOf(accountId) });
					continue;
				}

				giftNotifications.add(new CashItemGiftNotification(props, item, rs.getString(2)));
			}
			rs.close();
			ps.close();

			ps = con.prepareStatement("DELETE FROM `cashitemgiftnotes` WHERE `recipientacctid` = ?");
			ps.setInt(1, accountId);
			ps.executeUpdate();
		} catch (SQLException ex) {
			LOG.log(Level.WARNING, "Could not load cash shop purchase properties from database", ex);
		} finally {
			if (locked) {
				DatabaseManager.cleanup(DatabaseManager.DatabaseType.STATE, rs, ps, null);
				try {
					ps = con.prepareStatement("UNLOCK TABLE");
					ps.executeUpdate();
				} catch (SQLException e) {
					throw new RuntimeException("Could not unlock uniqueid table.", e);
				} finally {
					DatabaseManager.cleanup(DatabaseManager.DatabaseType.STATE, null, ps, con);
				}
			} else {
				DatabaseManager.cleanup(DatabaseManager.DatabaseType.STATE, rs, ps, con);
			}
			unlockWrite();
		}
	}

	public void lockRead() {
		locks.readLock().lock();
	}

	public void unlockRead() {
		locks.readLock().unlock();
	}

	public void lockWrite() {
		locks.writeLock().lock();
	}

	public void unlockWrite() {
		locks.writeLock().unlock();
	}

	//needs to be supported for loading CashShopStaging inventory
	//we better hope that retrieving from database is in slot position order
	@Override
	public void put(short position, InventorySlot item) {
		lockWrite();
		try {
			//if (position != slots.size())
				//throw new UnsupportedOperationException("CashShopStaging has no concept of slot positions - can only append items when (position == getAllValues.size() == getFreeSlots(1).get(1).shortValue())");
			append(item, null);
		} finally {
			unlockWrite();
		}
	}

	//needs to be supported for commiting CashShopStaging inventory
	@Override
	public Map<Short, InventorySlot> getAll() {
		//throw new UnsupportedOperationException("CashShopStaging has no concept of slot positions");
		lockRead();
		try {
			Map<Short, InventorySlot> slotBasedMap = new LinkedHashMap<Short, InventorySlot>();
			short slot = 1;
			for (InventorySlot item : getAllValues()) {
				slotBasedMap.put(Short.valueOf(slot), item);
				slot++;
			}
			return slotBasedMap;
		} finally {
			unlockRead();
		}
	}

	@Override
	public short getMaxSlots() {
		return MAX_SLOTS; //maybe? should test this
	}

	/**
	 * This CashShopStaging must be read locked while the returned collection is in scope.
	 * @return 
	 */
	public Collection<InventorySlot> getAllValues() {
		return slots.values();
	}

	public CashPurchaseProperties getPurchaseProperties(long uniqueId) {
		return purchaseProperties.get(Long.valueOf(uniqueId));
	}

	/**
	 * This CashShopStaging must be read locked while the returned collection is in scope.
	 * @return 
	 */
	public Collection<CashItemGiftNotification> getGiftedItems() {
		return giftNotifications;
	}

	public void newGiftedItem(CashItemGiftNotification gift) {
		lockWrite();
		try {
			giftNotifications.clear();
			giftNotifications.add(gift);
		} finally {
			unlockWrite();
		}
	}

	public InventorySlot getByUniqueId(long uniqueId) {
		lockRead();
		try {
			return slots.get(Long.valueOf(uniqueId));
		} finally {
			unlockRead();
		}
	}

	public void removeByUniqueId(long uniqueId) {
		lockWrite();
		try {
			Long oUid = Long.valueOf(uniqueId);
			slots.remove(oUid);
			purchaseProperties.remove(oUid);
		} finally {
			unlockWrite();
		}
	}

	public void append(InventorySlot item, CashPurchaseProperties props) {
		lockWrite();
		try {
			Long oUid = Long.valueOf(item.getUniqueId());
			slots.put(oUid, item);
			if (props != null)
				purchaseProperties.put(oUid, props);
		} finally {
			unlockWrite();
		}
	}

	public boolean isFull() {
		lockRead();
		try {
			return slots.size() >= getMaxSlots();
		} finally {
			unlockRead();
		}
	}

	public boolean canFit(int add) {
		lockRead();
		try {
			return slots.size() + add <= getMaxSlots();
		} finally {
			unlockRead();
		}
	}

	public static void attachCashPurchaseProperties(long uniqueId, CashPurchaseProperties props) {
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			con = DatabaseManager.getConnection(DatabaseManager.DatabaseType.STATE);
			ps = con.prepareStatement("UPDATE `cashshoppurchases` SET `purchaseracctid` = ?, `gifterchrname` = ?, `serialnumber` = ? WHERE `uniqueid` = ?");
			ps.setInt(1, props.purchaserAccountId);
			ps.setString(2, props.gifterName);
			ps.setInt(3, props.getSerialNumber());
			ps.setLong(4, uniqueId);
			ps.executeUpdate();
		} catch (SQLException ex) {
			LOG.log(Level.WARNING, "Could not attach cash shop purchase properties to database", ex);
		} finally {
			DatabaseManager.cleanup(DatabaseManager.DatabaseType.STATE, rs, ps, con);
		}
	}

	public static Pair<InventorySlot, CashPurchaseProperties> createItem(Commodity c, int serialNumber, int senderAcctId, String senderName) {
		InventorySlot item = InventoryTools.makeItemWithId(c.itemDataId);
		item.setExpiration(System.currentTimeMillis() + (c.period * 1000L * 60 * 60 * 24));
		if (c.quantity != 1)
			item.setQuantity(c.quantity);

		CashShopStaging.CashPurchaseProperties props = new CashShopStaging.CashPurchaseProperties(senderAcctId, senderName, serialNumber);
		CashShopStaging.attachCashPurchaseProperties(item.getUniqueId(), props);

		return new Pair<InventorySlot, CashPurchaseProperties>(item, props);
	}

	public static boolean giveGift(int senderAcctId, String senderName, int recipientAcctId, int[] serialNumbers, String message, ItemManipulator itemManipulator) {
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		boolean locked = false;
		try {
			con = DatabaseManager.getConnection(DatabaseManager.DatabaseType.STATE);

			ShopCharacter recipient = null;
			ps = con.prepareStatement("SELECT `id` FROM `characters` WHERE `accountid` = ?");
			ps.setInt(1, recipientAcctId);
			rs = ps.executeQuery();
			while (recipient == null && rs.next())
				recipient = ShopServer.getInstance().getPlayerById(rs.getInt(1));
			rs.close();
			ps.close();
			if (recipient != null) {
				if (!recipient.getCashShopInventory().canFit(serialNumbers.length))
					return false;

				CashShopDataLoader csdl = CashShopDataLoader.getInstance();
				for (int serialNumber : serialNumbers) {
					Commodity c = csdl.getCommodity(serialNumber);
					Pair<InventorySlot, CashPurchaseProperties> item = createItem(c, serialNumber, senderAcctId, senderName);
					if (itemManipulator != null && !itemManipulator.manipulate(item.left, serialNumber, c))
						continue;

					recipient.getCashShopInventory().append(item.left, item.right);
					recipient.getCashShopInventory().newGiftedItem(new CashItemGiftNotification(item.left.getUniqueId(), item.left.getDataId(), senderName, message));
					recipient.getClient().getSession().send(CashShopPackets.writeCashItemStagingInventory(recipient));
					recipient.getClient().getSession().send(CashShopPackets.writeGiftedCashItems(recipient));
				}
				return true;
			}

			ps = con.prepareStatement("SELECT MAX(`position`) FROM `inventoryitems` WHERE `accountid` = ? AND `inventorytype` = " + Inventory.InventoryType.CASH_SHOP.byteValue());
			ps.setInt(1, recipientAcctId);
			rs = ps.executeQuery();
			short position = (short) ((rs.next() ? rs.getShort(1) : 0) + 1);
			if (position - 1 + serialNumbers.length > MAX_SLOTS)
				return false;
			rs.close();
			ps.close();

			final Map<Short, InventorySlot> inv = new LinkedHashMap<Short, InventorySlot>(serialNumbers.length);
			CashShopDataLoader csdl = CashShopDataLoader.getInstance();
			for (int serialNumber : serialNumbers) {
				Commodity c = csdl.getCommodity(serialNumber);
				final Pair<InventorySlot, CashPurchaseProperties> item = createItem(c, serialNumber, senderAcctId, senderName);
				if (itemManipulator != null && !itemManipulator.manipulate(item.left, serialNumber, c))
					continue;

				inv.put(Short.valueOf(position), item.left);
				position++;
			}

			Player.commitInventory(recipientAcctId, recipientAcctId, new Pet[3], con, Collections.singletonMap(Inventory.InventoryType.CASH_SHOP, new IInventory() {
				@Override
				public void put(short position, InventorySlot item) {
					throw new UnsupportedOperationException("Player.commitInventory should not be mutating inventory.");
				}

				@Override
				public Map<Short, InventorySlot> getAll() {
					return inv;
				}

				@Override
				public short getMaxSlots() {
					throw new UnsupportedOperationException("Player.commitInventory should not need capacity.");
				}
			}));

			ps = con.prepareStatement("LOCK TABLE `cashitemgiftnotes` WRITE");
			ps.executeUpdate();
			locked = true;
			ps.close();

			ps = con.prepareStatement("INSERT INTO `cashitemgiftnotes` (`uniqueid`,`recipientacctid`,`message`) VALUES (?,?,?)");
			ps.setInt(2, recipientAcctId);
			ps.setString(3, message);
			for (InventorySlot item : inv.values()) {
				ps.setLong(1, item.getUniqueId());
				ps.addBatch();
			}
			ps.executeBatch();
			return true;
		} catch (SQLException ex) {
			LOG.log(Level.WARNING, "Could not insert new cash item gift to database", ex);
			return false;
		} finally {
			DatabaseManager.cleanup(DatabaseManager.DatabaseType.STATE, rs, ps, null);
			if (locked) {
				try {
					ps = con.prepareStatement("UNLOCK TABLE");
					ps.executeUpdate();
				} catch (SQLException e) {
					throw new RuntimeException("Could not unlock uniqueid table.", e);
				} finally {
					DatabaseManager.cleanup(DatabaseManager.DatabaseType.STATE, rs, ps, con);
				}
			}
		}
	}

	public static int[] getBestItems() {
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		int[] bestItems = new int[5];
		try {
			con = DatabaseManager.getConnection(DatabaseManager.DatabaseType.STATE);
			ps = con.prepareStatement("SELECT `serialnumber` FROM `cashshoppurchases` WHERE `serialnumber` IS NOT NULL GROUP BY `serialnumber` ORDER BY COUNT(*) DESC LIMIT 5");
			rs = ps.executeQuery();
			for (int i = 0; rs.next(); i++)
				bestItems[i] = rs.getInt(1);
		} catch (SQLException ex) {
			LOG.log(Level.WARNING, "Could not get best cash items from database", ex);
		} finally {
			DatabaseManager.cleanup(DatabaseManager.DatabaseType.STATE, rs, ps, con);
		}
		return bestItems;
	}
}
