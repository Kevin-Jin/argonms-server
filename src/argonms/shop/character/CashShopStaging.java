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

import argonms.common.character.inventory.IInventory;
import argonms.common.character.inventory.InventorySlot;
import argonms.common.util.DatabaseManager;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
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

	public static class CashItemGift {
		private final long uniqueId;
		private final int itemId;
		private final String sender;
		private final String message;

		public CashItemGift(long uniqueId, int dataId, String sender, String message) {
			this.uniqueId = uniqueId;
			this.itemId = dataId;
			this.sender = sender;
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

	private final ReadWriteLock locks;
	private final Map<Long, InventorySlot> slots;
	private final Map<Long, CashPurchaseProperties> purchaseProperties;
	private final List<CashItemGift> gifts;

	public CashShopStaging() {
		//forgo to the overhead of ConcurrentHashMap. with no scripts and
		//commands, we are guaranteed to not do much concurrency in cash shop
		locks = new ReentrantReadWriteLock();
		slots = new LinkedHashMap<Long, InventorySlot>();
		purchaseProperties = new HashMap<Long, CashPurchaseProperties>();
		gifts = new ArrayList<CashItemGift>();
	}

	public void loadPurchaseProperties(int accountId) {
		lockWrite();
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
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
		} catch (SQLException ex) {
			LOG.log(Level.WARNING, "Could not load cash shop purchase properties from database", ex);
		} finally {
			DatabaseManager.cleanup(DatabaseManager.DatabaseType.STATE, rs, ps, null);
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

	public Collection<CashItemGift> getGiftedItems() {
		return gifts;
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
}
