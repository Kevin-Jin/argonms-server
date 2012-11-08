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

package argonms.common.character;

import argonms.common.character.inventory.Equip;
import argonms.common.character.inventory.IInventory;
import argonms.common.character.inventory.Inventory;
import argonms.common.character.inventory.Inventory.InventoryType;
import argonms.common.character.inventory.InventorySlot;
import argonms.common.character.inventory.InventoryTools;
import argonms.common.character.inventory.Item;
import argonms.common.character.inventory.Pet;
import argonms.common.character.inventory.Ring;
import argonms.common.character.inventory.TamingMob;
import argonms.common.net.external.RemoteClient;
import argonms.common.util.DatabaseManager;
import argonms.common.util.DatabaseManager.DatabaseType;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
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
public abstract class Player {
	private static final Logger LOG = Logger.getLogger(Player.class.getName());

	private int id;
	private byte gm;

	protected String name;
	protected short eyes, hair;
	protected byte skin;
	protected byte gender;

	protected volatile short level;
	protected volatile short job;
	protected volatile short baseStr, baseDex, baseInt, baseLuk, baseMaxHp, baseMaxMp;
	protected volatile short remHp, remMp, remAp, remSp;
	protected volatile int exp;
	protected volatile short fame;

	protected int savedMapId;
	protected byte savedSpawnPoint;

	protected int partner;

	private final Map<InventoryType, Inventory> inventories;
	private final Pet[] pets;

	protected Player() {
		inventories = new EnumMap<InventoryType, Inventory>(InventoryType.class);
		pets = new Pet[3];
	}

	public abstract RemoteClient getClient();

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

	public short getLevel() {
		return level;
	}

	public short getJob() {
		return job;
	}

	public short getStr() {
		return baseStr;
	}

	public short getDex() {
		return baseDex;
	}

	public short getInt() {
		return baseInt;
	}

	public short getLuk() {
		return baseLuk;
	}

	public short getHp() {
		return remHp;
	}

	public short getMaxHp() {
		return baseMaxHp;
	}

	public short getMp() {
		return remMp;
	}

	public short getMaxMp() {
		return baseMaxMp;
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
		return savedMapId;
	}

	public byte getSpawnPoint() {
		return savedSpawnPoint;
	}

	protected Map<InventoryType, Inventory> getInventories() {
		return inventories;
	}

	public Inventory getInventory(InventoryType type) {
		return inventories.get(type);
	}

	public byte getPrivilegeLevel() {
		return gm;
	}

	protected void setName(String name) {
		this.name = name;
	}

	protected void setEyes(short eyes) {
		this.eyes = eyes;
	}

	protected void setHair(short hair) {
		this.hair = hair;
	}

	protected void setSkin(byte skin) {
		this.skin = skin;
	}

	protected void setGender(byte gender) {
		this.gender = gender;
	}

	protected void setStr(short str) {
		this.baseStr = str;
	}

	protected void setDex(short dex) {
		this.baseDex = dex;
	}

	protected void setInt(short _int) {
		this.baseInt = _int;
	}

	protected void setLuk(short luk) {
		this.baseLuk = luk;
	}

	protected void setLevel(short level) {
		this.level = level;
	}

	protected void setJob(short job) {
		this.job = job;
	}

	protected void setGm(byte gm) {
		this.gm = gm;
	}

	public void setId(int id) {
		this.id = id;
	}

	protected void addInventories(Map<InventoryType, Inventory> inventories) {
		this.inventories.putAll(inventories);
	}

	protected void loadPlayerStats(ResultSet rs, int id) throws SQLException {
		this.id = id;
		name = rs.getString(4);
		gender = rs.getByte(5);
		skin = rs.getByte(6);
		eyes = rs.getShort(7);
		hair = rs.getShort(8);
		level = rs.getShort(9);
		job = rs.getShort(10);
		baseStr = rs.getShort(11);
		baseDex = rs.getShort(12);
		baseInt = rs.getShort(13);
		baseLuk = rs.getShort(14);
		remHp = rs.getShort(15);
		baseMaxHp = rs.getShort(16);
		remMp = rs.getShort(17);
		baseMaxMp = rs.getShort(18);
		remAp = rs.getShort(19);
		remSp = rs.getShort(20);
		exp = rs.getInt(21);
		fame = rs.getShort(22);
		partner = rs.getInt(23);
		savedMapId = rs.getInt(24);
		savedSpawnPoint = rs.getByte(25);

		inventories.put(InventoryType.EQUIP, new Inventory(rs.getShort(27)));
		inventories.put(InventoryType.USE, new Inventory(rs.getShort(28)));
		inventories.put(InventoryType.SETUP, new Inventory(rs.getShort(29)));
		inventories.put(InventoryType.ETC, new Inventory(rs.getShort(30)));
		inventories.put(InventoryType.CASH, new Inventory(rs.getShort(31)));
		//TODO: get real equipped inventory size?
		inventories.put(InventoryType.EQUIPPED, new Inventory((short) 0));
		gm = rs.getByte(33);
	}

	private static void setEquipUpdateVariables(Equip equip, int inventoryKey, PreparedStatement ps) throws SQLException {
		ps.setInt(1, inventoryKey);
		ps.setByte(2, equip.getUpgradeSlots());
		ps.setByte(3, equip.getLevel());
		ps.setShort(4, equip.getStr());
		ps.setShort(5, equip.getDex());
		ps.setShort(6, equip.getInt());
		ps.setShort(7, equip.getLuk());
		ps.setShort(8, equip.getHp());
		ps.setShort(9, equip.getMp());
		ps.setShort(10, equip.getWatk());
		ps.setShort(11, equip.getMatk());
		ps.setShort(12, equip.getWdef());
		ps.setShort(13, equip.getMdef());
		ps.setShort(14, equip.getAcc());
		ps.setShort(15, equip.getAvoid());
		ps.setShort(16, equip.getHands());
		ps.setShort(17, equip.getSpeed());
		ps.setShort(18, equip.getJump());
	}

	private static byte indexOf(Pet[] pets, Pet search) {
		for (byte i = 0; i < 3; i++)
			if (pets[i] != null && pets[i] == search)
				return i;
		return -1;
	}

	public static void commitInventory(int characterId, int accountId, Pet[] pets, Connection con, Map<InventoryType, ? extends IInventory> inventories) throws SQLException {
		PreparedStatement ps = null, eps = null, rps = null, pps = null, mps = null;
		ResultSet rs = null;
		try {
			ps = con.prepareStatement("INSERT INTO `inventoryitems` "
				+ "(`characterid`,`accountid`,`inventorytype`,`position`,`itemid`,`expiredate`,`uniqueid`,`owner`,`quantity`) "
				+ "VALUES (?,?,?,?,?,?,?,?,?)",
				Statement.RETURN_GENERATED_KEYS);
			eps = con.prepareStatement("INSERT INTO `inventoryequipment` "
					+ "(`inventoryitemid`,`upgradeslots`,`level`,`str`,`dex`,`int`,`luk`,`hp`,`mp`,`watk`,`matk`,`wdef`,`mdef`,`acc`,`avoid`,`hands`,`speed`,`jump`) "
					+ "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)");
			rps = con.prepareStatement("INSERT INTO `inventoryrings` "
					+ "(`inventoryitemid`,`partnerchrid`,`partnerringid`) "
					+ "VALUES(?,?,?)");
			pps = con.prepareStatement("INSERT INTO `inventorypets` "
					+ "(`inventoryitemid`,`position`,`name`,`level`,`closeness`,`fullness`,`expired`) "
					+ "VALUES (?,?,?,?,?,?,?)");
			mps = con.prepareStatement("INSERT INTO `inventorymounts` "
					+ "(`inventoryitemid`,`level`,`exp`,`tiredness`) "
					+ "VALUES (?,?,?,?)");
			ps.setInt(1, characterId);
			ps.setInt(2, accountId);
			for (Entry<InventoryType, ? extends IInventory> ent : inventories.entrySet()) {
				switch (ent.getKey()) {
					case STORAGE:
					case CASH_SHOP:
						ps.setNull(1, Types.INTEGER);
						break;
					default:
						ps.setInt(1, characterId);
						break;
				}
				ps.setInt(3, ent.getKey().byteValue());
				Map<Short, InventorySlot> iv = ent.getValue().getAll();
				synchronized(iv) {
					for (Entry<Short, InventorySlot> e : iv.entrySet()) {
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

						switch (item.getType()) {
							case RING: {
								Ring ring = (Ring) item;

								ps.executeUpdate(); //need the generated keys, so no batch
								rs = ps.getGeneratedKeys();
								int inventoryKey = rs.next() ? rs.getInt(1) : -1;
								rs.close();

								setEquipUpdateVariables(ring, inventoryKey, eps);
								eps.addBatch();

								rps.setInt(1, inventoryKey);
								rps.setInt(2, ring.getPartnerCharId());
								rps.setLong(3, ring.getPartnerRingId());
								rps.addBatch();
								break;
							}
							case EQUIP: {
								ps.executeUpdate(); //need the generated keys, so no batch
								rs = ps.getGeneratedKeys();
								int inventoryKey = rs.next() ? rs.getInt(1) : -1;
								rs.close();

								setEquipUpdateVariables((Equip) item, inventoryKey, eps);
								eps.addBatch();
								break;
							}
							case PET: {
								Pet pet = (Pet) item;

								ps.executeUpdate(); //need the generated keys, so no batch
								rs = ps.getGeneratedKeys();
								int inventoryKey = rs.next() ? rs.getInt(1) : -1;
								rs.close();

								pps.setInt(1, inventoryKey);
								pps.setByte(2, indexOf(pets, pet));
								pps.setString(3, pet.getName());
								pps.setByte(4, pet.getLevel());
								pps.setShort(5, pet.getCloseness());
								pps.setByte(6, pet.getFullness());
								pps.setBoolean(7, pet.isExpired());
								pps.addBatch();
								break;
							}
							case MOUNT: {
								TamingMob mount = (TamingMob) item;

								ps.executeUpdate(); //need the generated keys, so no batch
								rs = ps.getGeneratedKeys();
								int inventoryKey = rs.next() ? rs.getInt(1) : -1;
								rs.close();

								setEquipUpdateVariables(mount, inventoryKey, eps);
								eps.addBatch();

								mps.setInt(1, inventoryKey);
								mps.setByte(2, mount.getMountLevel());
								mps.setShort(3, mount.getExp());
								mps.setByte(4, mount.getTiredness());
								mps.addBatch();
								break;
							}
							case ITEM:
								ps.addBatch();
								break;
						}
					}
				}
			}
			ps.executeBatch();
			eps.executeBatch();
			rps.executeBatch();
			pps.executeBatch();
			mps.executeBatch();
		} finally {
			DatabaseManager.cleanup(DatabaseType.STATE, null, mps, null);
			DatabaseManager.cleanup(DatabaseType.STATE, null, pps, null);
			DatabaseManager.cleanup(DatabaseType.STATE, null, rps, null);
			DatabaseManager.cleanup(DatabaseType.STATE, null, eps, null);
			DatabaseManager.cleanup(DatabaseType.STATE, rs, ps, null);
		}
	}

	protected void commitInventory(Connection con, Map<InventoryType, ? extends IInventory> inventories) throws SQLException {
		commitInventory(getDataId(), getClient().getAccountId(), pets, con, inventories);
	}

	public static void loadInventory(Pet[] pets, Connection con, ResultSet rs, Map<InventoryType, ? extends IInventory> inventories) throws SQLException {
		PreparedStatement ips = null;
		ResultSet irs = null;
		try {
			while (rs.next()) {
				InventorySlot item;
				InventoryType inventoryType = InventoryType.valueOf(rs.getByte(4));
				short position = rs.getShort(5);
				int itemid = rs.getInt(6);
				int inventoryKey = rs.getInt(1);
				if (InventoryTools.isEquip(itemid)) {
					Equip e;
					if (InventoryTools.isRing(itemid)) {
						e = new Ring(itemid);
						ips = con.prepareStatement("SELECT * FROM `inventoryrings` WHERE `inventoryitemid` = ?");
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
						ips = con.prepareStatement("SELECT * FROM `inventorymounts` WHERE `inventoryitemid` = ?");
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
					ips = con.prepareStatement("SELECT * FROM `inventoryequipment` WHERE `inventoryitemid` = ?");
					ips.setInt(1, inventoryKey);
					irs = ips.executeQuery();
					if (irs.next()) {
						e.setUpgradeSlots(irs.getByte(3));
						e.setLevel(irs.getByte(4));
						e.setStr(irs.getShort(5));
						e.setDex(irs.getShort(6));
						e.setInt(irs.getShort(7));
						e.setLuk(irs.getShort(8));
						e.setHp(irs.getShort(9));
						e.setMp(irs.getShort(10));
						e.setWatk(irs.getShort(11));
						e.setMatk(irs.getShort(12));
						e.setWdef(irs.getShort(13));
						e.setMdef(irs.getShort(14));
						e.setAcc(irs.getShort(15));
						e.setAvoid(irs.getShort(16));
						e.setHands(irs.getShort(17));
						e.setSpeed(irs.getShort(18));
						e.setJump(irs.getShort(19));
					}
					irs.close();
					ips.close();
					item = e;
				} else {
					if (InventoryTools.isPet(itemid)) {
						Pet pet = new Pet(itemid);
						ips = con.prepareStatement("SELECT * FROM `inventorypets` WHERE `inventoryitemid` = ?");
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
								pets[pos] = pet;
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
				inventories.get(inventoryType).put(position, item);
			}
		} finally {
			DatabaseManager.cleanup(DatabaseType.STATE, irs, ips, null);
		}
	}

	protected void loadInventory(Connection con, ResultSet rs, Map<InventoryType, ? extends IInventory> inventories) throws SQLException {
		loadInventory(pets, con, rs, inventories);
	}

	public static String getNameFromId(int characterid) {
		String name = null;
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			con = DatabaseManager.getConnection(DatabaseType.STATE);
			ps = con.prepareStatement("SELECT `name` FROM `characters` WHERE `id` = ?");
			ps.setInt(1, characterid);
			rs = ps.executeQuery();
			if (rs.next())
				name = rs.getString(1);
		} catch (SQLException ex) {
			LOG.log(Level.WARNING, "Could not find name of character " + characterid, ex);
		} finally {
			DatabaseManager.cleanup(DatabaseType.STATE, rs, ps, con);
		}
		return name;
	}

	public static int getIdFromName(String name) {
		int id = -1;
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			con = DatabaseManager.getConnection(DatabaseType.STATE);
			ps = con.prepareStatement("SELECT `id` FROM `characters` WHERE `name` = ?");
			ps.setString(1, name);
			rs = ps.executeQuery();
			if (rs.next())
				id = rs.getInt(1);
		} catch (SQLException ex) {
			LOG.log(Level.WARNING, "Could not find id of character " + name, ex);
		} finally {
			DatabaseManager.cleanup(DatabaseType.STATE, rs, ps, con);
		}
		return id;
	}

	public static boolean characterExists(String name) {
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			con = DatabaseManager.getConnection(DatabaseType.STATE);
			ps = con.prepareStatement("SELECT EXISTS(SELECT 1 FROM `characters` WHERE `name` = ? LIMIT 1)");
			ps.setString(1, name);
			rs = ps.executeQuery();
			return (rs.next() && rs.getBoolean(1));
		} catch (SQLException ex) {
			LOG.log(Level.WARNING, "Could not determine if character " + name + " exists", ex);
			return false;
		} finally {
			DatabaseManager.cleanup(DatabaseType.STATE, rs, ps, con);
		}
	}
}
