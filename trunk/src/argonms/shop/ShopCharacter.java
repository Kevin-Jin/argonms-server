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

import argonms.common.Player;
import argonms.game.character.inventory.Equip;
import argonms.game.character.inventory.Inventory;
import argonms.game.character.inventory.Inventory.InventoryType;
import argonms.game.character.inventory.InventorySlot;
import argonms.game.character.inventory.InventoryTools;
import argonms.game.character.inventory.Item;
import argonms.game.character.inventory.Pet;
import argonms.game.character.inventory.Ring;
import argonms.game.character.inventory.TamingMob;
import argonms.common.tools.DatabaseManager;
import argonms.common.tools.DatabaseManager.DatabaseType;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.EnumMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author GoldenKevin
 */
public class ShopCharacter implements Player {
	private static final Logger LOG = Logger.getLogger(ShopCharacter.class.getName());

	private int id;
	private ShopClient client;
	private byte gm;

	private String name;
	private short eyes, hair;
	private byte skin;
	private byte gender;
	private short level;
	private short job;
	private short str, dex, _int, luk, maxHp, maxMp;
	private short remHp, remMp, remAp, remSp;
	private int exp;
	private short fame;

	private int partner;

	private int map;
	private byte spawnPoint;

	private final Map<InventoryType, Inventory> inventories;
	private final Pet[] pets;

	private ShopCharacter() {
		inventories = new EnumMap<InventoryType, Inventory>(InventoryType.class);
		pets = new Pet[3];
	}

	public ShopClient getClient() {
		return client;
	}

	public String getName() {
		return name;
	}

	public int getDataId() {
		return id;
	}

	public int getId() {
		return id;
	}

	public byte getGender() {
		return gender;
	}

	public byte getSkinColor() {
		return skin;
	}

	public short getEyes() {
		return eyes;
	}

	public short getHair() {
		return hair;
	}

	public Pet[] getPets() {
		return pets;
	}

	public byte getPetPosition(Pet p) {
		for (byte i = 0; i < 3; i++)
			if (pets[i] != null && pets[i] == p)
				return i;
		return -1;
	}

	public short getLevel() {
		return level;
	}

	public short getJob() {
		return job;
	}

	public short getStr() {
		return str;
	}

	public short getDex() {
		return dex;
	}

	public short getInt() {
		return _int;
	}

	public short getLuk() {
		return luk;
	}

	public short getHp() {
		return remHp;
	}

	public short getMaxHp() {
		return maxHp;
	}

	public short getMp() {
		return remMp;
	}

	public short getMaxMp() {
		return maxMp;
	}

	public short getAp() {
		return remAp;
	}

	public short getSp() {
		return remSp;
	}

	public int getExp() {
		return exp;
	}

	public short getFame() {
		return fame;
	}

	public int getSpouseId() {
		return partner;
	}

	public int getMapId() {
		return map;
	}

	public byte getSpawnPoint() {
		return spawnPoint;
	}

	public Inventory getInventory(InventoryType type) {
		return inventories.get(type);
	}

	public byte getPrivilegeLevel() {
		return gm;
	}

	private void insertEquipIntoDb(Equip equip, int inventoryKey,
			Connection con) throws SQLException {
		PreparedStatement ps = null;
		try {
			ps = con.prepareStatement(
					"INSERT INTO `inventoryequipment` ("
					+ "`inventoryitemid`,`str`,`dex`,`int`,"
					+ "`luk`,`hp`,`mp`,`watk`,`matk`,`wdef`,"
					+ "`mdef`,`acc`,`avoid`,`speed`,`jump`,"
					+ "`upgradeslots`) "
					+ "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)");
			ps.setInt(1, inventoryKey);
			ps.setShort(2, equip.getStr());
			ps.setShort(3, equip.getDex());
			ps.setShort(4, equip.getInt());
			ps.setShort(5, equip.getLuk());
			ps.setShort(6, equip.getHp());
			ps.setShort(7, equip.getMp());
			ps.setShort(8, equip.getWatk());
			ps.setShort(9, equip.getMatk());
			ps.setShort(10, equip.getWdef());
			ps.setShort(11, equip.getMdef());
			ps.setShort(12, equip.getAcc());
			ps.setShort(13, equip.getAvoid());
			ps.setShort(14, equip.getSpeed());
			ps.setShort(15, equip.getJump());
			ps.setByte(16, equip.getUpgradeSlots());
			ps.executeUpdate();
		} finally {
			DatabaseManager.cleanup(DatabaseType.STATE, null, ps, null);
		}
	}

	private void updateDbInventory(Connection con) throws SQLException {
		String invUpdate = "DELETE FROM `inventoryitems` WHERE "
				+ "`characterid` = ? AND `inventorytype` <= "
				+ InventoryType.CASH.byteValue() + " OR `accountid` = ? AND "
				+ "`inventorytype` = " + InventoryType.CASH_SHOP.byteValue();
		PreparedStatement ps = null, ips = null;
		ResultSet rs = null;
		try {
			ps = con.prepareStatement(invUpdate);
			ps.setInt(1, getDataId());
			ps.setInt(2, client.getAccountId());
			ps.executeUpdate();
			ps.close();

			ps = con.prepareStatement("INSERT INTO `inventoryitems` "
					+ "(`characterid`,`accountid`,`inventorytype`,`position`,"
					+ "`itemid`,`expiredate`,`uniqueid`,`owner`,`quantity`) "
					+ "VALUES (?,?,?,?,?,?,?,?,?)",
					PreparedStatement.RETURN_GENERATED_KEYS);
			ps.setInt(2, client.getAccountId());
			for (Entry<InventoryType, Inventory> ent : inventories.entrySet()) {
				if (ent.getKey() == InventoryType.CASH_SHOP)
					//null keys are not affected by foreign key constraints,
					//so we won't get account wide inventories deleted when
					//this character is deleted.
					ps.setNull(1, Types.INTEGER);
				else
					ps.setInt(1, getDataId());
				ps.setInt(3, ent.getKey().byteValue());
				for (Entry<Short, InventorySlot> e : ent.getValue().getAll().entrySet()) {
					InventorySlot item = e.getValue();

					ps.setShort(4, e.getKey().shortValue());
					ps.setInt(5, item.getDataId());
					ps.setLong(6, item.getExpiration());
					ps.setLong(7, item.getUniqueId());
					ps.setString(8, item.getOwner());
					ps.setShort(9, item.getQuantity());
					//TODO: refactor so we can use addBatch here for inventories
					//(equip, ring, pet, mount) and for items. Run getGeneratedKeys()
					//after executeBatch to get generated keys for each item
					//in iteration order...
					ps.executeUpdate(); //need the generated keys, so no batch
					rs = ps.getGeneratedKeys();
					int inventoryKey = rs.next() ? rs.getInt(1) : -1;
					rs.close();

					switch (item.getType()) {
						case RING:
							Ring ring = (Ring) item;
							ips = con.prepareStatement(
									"INSERT INTO `inventoryrings` ("
									+ "`inventoryitemid`,`partnerchrid`,"
									+ "`partnerringid`) VALUES(?,?,?)");
							ips.setInt(1, inventoryKey);
							ips.setInt(2, ring.getPartnerCharId());
							ips.setLong(3, ring.getPartnerRingId());
							ips.executeUpdate();
							ips.close();
							insertEquipIntoDb(ring, inventoryKey, con);
							break;
						case EQUIP:
							insertEquipIntoDb((Equip) item, inventoryKey, con);
							break;
						case PET:
							Pet pet = (Pet) item;
							ips = con.prepareStatement(
									"INSERT INTO `inventorypets` ("
									+ "`inventoryitemid`,`position`,`name`,"
									+ "`level`,`closeness`,`fullness`,`expired`) "
									+ "VALUES (?,?,?,?,?,?,?)");
							ips.setInt(1, inventoryKey);
							ips.setByte(2, getPetPosition(pet));
							ips.setString(3, pet.getName());
							ips.setByte(4, pet.getLevel());
							ips.setShort(5, pet.getCloseness());
							ips.setByte(6, pet.getFullness());
							ips.setBoolean(7, pet.isExpired());
							ips.executeUpdate();
							break;
						case MOUNT:
							TamingMob mount = (TamingMob) item;
							ips = con.prepareStatement(
									"INSERT INTO `inventorymounts` ("
									+ "`inventoryitemid`,`level`,`exp`,"
									+ "`tiredness`) VALUES (?,?,?,?)");
							ips.setInt(1, inventoryKey);
							ips.setByte(2, mount.getMountLevel());
							ips.setShort(3, mount.getExp());
							ips.setByte(4, mount.getTiredness());
							ips.executeUpdate();
							ips.close();
							insertEquipIntoDb(mount, inventoryKey, con);
							break;
					}
				}
			}
		} catch (SQLException e) {
			throw new SQLException("Failed to save inventory of character " + name, e);
		} finally {
			DatabaseManager.cleanup(DatabaseType.STATE, null, ips, null);
			DatabaseManager.cleanup(DatabaseType.STATE, rs, ps, null);
		}
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
			p.id = id;
			p.name = rs.getString(4);
			p.gender = rs.getByte(5);
			p.skin = rs.getByte(6);
			p.eyes = rs.getShort(7);
			p.hair = rs.getShort(8);
			p.level = rs.getShort(9);
			p.job = rs.getShort(10);
			p.str = rs.getShort(11);
			p.dex = rs.getShort(12);
			p._int = rs.getShort(13);
			p.luk = rs.getShort(14);
			p.remHp = rs.getShort(15);
			p.maxHp = rs.getShort(16);
			p.remMp = rs.getShort(17);
			p.maxMp = rs.getShort(18);
			p.remAp = rs.getShort(19);
			p.remSp = rs.getShort(20);
			p.exp = rs.getInt(21);
			p.fame = rs.getShort(22);
			p.partner = rs.getInt(23);
			p.map = rs.getInt(24);
			p.spawnPoint = rs.getByte(25);

			p.inventories.put(InventoryType.EQUIP, new Inventory(rs.getByte(27)));
			p.inventories.put(InventoryType.USE, new Inventory(rs.getByte(28)));
			p.inventories.put(InventoryType.SETUP, new Inventory(rs.getByte(29)));
			p.inventories.put(InventoryType.ETC, new Inventory(rs.getByte(30)));
			p.inventories.put(InventoryType.CASH, new Inventory(rs.getByte(31)));
			//TODO: get real equipped inventory size?
			p.inventories.put(InventoryType.EQUIPPED, new Inventory((byte) 0));
			//TODO: get real cash shop inventory size?
			p.inventories.put(InventoryType.CASH_SHOP, new Inventory((byte) 0));
			p.gm = rs.getByte(34);
			rs.close();
			ps.close();

			ps = con.prepareStatement("SELECT * FROM `inventoryitems` WHERE `characterid` = ?"
					+ " AND `inventorytype` <= " + InventoryType.CASH.byteValue()
					+ " OR `accountid` = ? AND `inventorytype` = " + InventoryType.CASH_SHOP.byteValue());
			ps.setInt(1, id);
			ps.setInt(2, accountid);
			rs = ps.executeQuery();
			while (rs.next()) {
				InventorySlot item;
				InventoryType inventoryType = InventoryType.valueOf(rs.getByte(4));
				short position = rs.getShort(5);
				int itemid = rs.getInt(6);
				int inventoryKey = rs.getInt(1);
				if (inventoryType == InventoryType.EQUIP || inventoryType == InventoryType.EQUIPPED) {
					Equip e;
					if (InventoryTools.isRing(itemid)) {
						e = new Ring(itemid);
						ips = con.prepareStatement("SELECT * FROM "
								+ "`inventoryrings` WHERE "
								+ "`inventoryitemid` = ?");
						ips.setInt(1, inventoryKey);
						irs = ips.executeQuery();
						if (irs.next()) {
							((Ring) e).setPartnerCharId(irs.getInt(3));
							((Ring) e).setPartnerRingId(irs.getLong(4));
						}
						irs.close();
						ips.close();
					} else if (InventoryTools.isMount(itemid)) {
						e = new TamingMob(itemid);
						ips = con.prepareStatement("SELECT * FROM "
								+ "`inventorymounts` WHERE "
								+ "`inventoryitemid` = ?");
						ips.setInt(1, inventoryKey);
						irs = ips.executeQuery();
						if (irs.next()) {
							((TamingMob) e).setLevel(irs.getByte(3));
							((TamingMob) e).setExp(irs.getShort(4));
							((TamingMob) e).setTiredness(irs.getByte(5));
						}
						irs.close();
						ips.close();
					} else {
						e = new Equip(itemid);
					}
					ips = con.prepareStatement("SELECT * FROM "
							+ "`inventoryequipment` WHERE "
							+ "`inventoryitemid` = ?");
					ips.setInt(1, inventoryKey);
					irs = ips.executeQuery();
					if (irs.next()) {
						e.setStr(irs.getShort(3));
						e.setDex(irs.getShort(4));
						e.setInt(irs.getShort(5));
						e.setLuk(irs.getShort(6));
						e.setHp(irs.getShort(7));
						e.setMp(irs.getShort(8));
						e.setWatk(irs.getShort(9));
						e.setMatk(irs.getShort(10));
						e.setWdef(irs.getShort(11));
						e.setMdef(irs.getShort(12));
						e.setAcc(irs.getShort(13));
						e.setAvoid(irs.getShort(14));
						e.setSpeed(irs.getShort(15));
						e.setJump(irs.getShort(16));
						e.setUpgradeSlots(irs.getByte(17));
					}
					irs.close();
					ips.close();
					item = e;
				} else {
					if (InventoryTools.isPet(itemid)) {
						Pet pet = new Pet(itemid);
						ips = con.prepareStatement("SELECT * "
								+ "FROM `inventorypets` WHERE "
								+ "`inventoryitemid` = ?");
						ips.setInt(1, inventoryKey);
						irs = ips.executeQuery();
						if (irs.next()) {
							pet.setName(irs.getString(4));
							pet.setLevel(irs.getByte(5));
							pet.setCloseness(irs.getShort(6));
							pet.setFullness(irs.getByte(7));
							pet.setExpired(irs.getBoolean(8));
							byte pos = irs.getByte(3);
							if (pos >= 0 && pos < 3)
								p.pets[pos] = pet;
						}
						irs.close();
						ips.close();
						item = pet;
					} else {
						item = new Item(itemid);
						item.setQuantity(rs.getShort(10));
					}
				}
				item.setExpiration(rs.getLong(7));
				item.setUniqueId(rs.getLong(8));
				item.setOwner(rs.getString(9));
				p.inventories.get(inventoryType).put(position, item);
			}
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
