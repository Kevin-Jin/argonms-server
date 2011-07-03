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

package argonms.common.character;

import argonms.common.character.inventory.Equip;
import argonms.common.character.inventory.Inventory;
import argonms.common.character.inventory.Inventory.InventoryType;
import argonms.common.character.inventory.InventorySlot;
import argonms.common.character.inventory.InventoryTools;
import argonms.common.character.inventory.Item;
import argonms.common.character.inventory.Pet;
import argonms.common.character.inventory.Ring;
import argonms.common.character.inventory.TamingMob;
import argonms.common.net.external.RemoteClient;
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

/**
 *
 * @author GoldenKevin
 */
public interface Player {
	public RemoteClient getClient();
	public String getName();
	public int getDataId();
	public int getId();
	public byte getGender();
	public byte getSkinColor();
	public short getEyes();
	public short getHair();
	public Pet[] getPets();
	public short getLevel();
	public short getJob();
	public short getStr();
	public short getDex();
	public short getInt();
	public short getLuk();
	public short getHp();
	public short getMaxHp();
	public short getMp();
	public short getMaxMp();
	public short getAp();
	public short getSp();
	public int getExp();
	public short getFame();
	public int getSpouseId();
	public int getMapId();
	public byte getSpawnPoint();
	public Inventory getInventory(InventoryType type);
	public byte getPrivilegeLevel();
	public void close();

	public static abstract class LimitedActionCharacter implements Player {
		private int id;
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

		protected LimitedActionCharacter() {
			inventories = new EnumMap<InventoryType, Inventory>(InventoryType.class);
			pets = new Pet[3];
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

		protected void commitInventory(Connection con, int accountId) throws SQLException {
			CharacterTools.commitInventory(con, id, accountId, pets, inventories);
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
			this.str = str;
		}

		protected void setDex(short dex) {
			this.dex = dex;
		}

		protected void setInt(short _int) {
			this._int = _int;
		}

		protected void setLuk(short luk) {
			this.luk = luk;
		}

		protected void setLevel(short level) {
			this.level = level;
		}

		protected void setGm(byte gm) {
			this.gm = gm;
		}

		protected void setId(int id) {
			this.id = id;
		}

		protected void addInventories(Map<InventoryType, Inventory> inventories) {
			inventories.putAll(inventories);
		}

		protected static void loadPlayerStats(ResultSet rs, int id, LimitedActionCharacter p, InventoryType extraInvType, Inventory extraInv) throws SQLException {
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
			if (extraInvType != null)
				p.inventories.put(extraInvType, extraInv);
			p.gm = rs.getByte(34);
		}

		protected static void loadInventory(Connection con, ResultSet rs, LimitedActionCharacter p) throws SQLException {
			PreparedStatement ips = null;
			ResultSet irs = null;
			try {
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
			} finally {
				DatabaseManager.cleanup(DatabaseType.STATE, irs, ips, null);
			}
		}
	}

	public static class CharacterTools {
		private static void insertEquipIntoDb(Equip equip, int inventoryKey,
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

		private static byte indexOf(Pet[] pets, Pet search) {
			for (byte i = 0; i < 3; i++)
				if (pets[i] != null && pets[i] == search)
					return i;
			return -1;
		}

		public static void commitInventory(Connection con, int characterId, int accountId, Pet[] pets, Map<InventoryType, Inventory> inventories) throws SQLException {
			PreparedStatement ps = null, ips = null;
			ResultSet rs = null;
			try {
				ps = con.prepareStatement("INSERT INTO `inventoryitems` "
					+ "(`characterid`,`accountid`,`inventorytype`,`position`,"
					+ "`itemid`,`expiredate`,`uniqueid`,`owner`,`quantity`) "
					+ "VALUES (?,?,?,?,?,?,?,?,?)",
					PreparedStatement.RETURN_GENERATED_KEYS);
				ps.setInt(1, characterId);
				ps.setInt(2, accountId);
				for (Entry<InventoryType, Inventory> ent : inventories.entrySet()) {
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
								ips.setByte(2, indexOf(pets, pet));
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
			} finally {
				DatabaseManager.cleanup(DatabaseType.STATE, null, ips, null);
				DatabaseManager.cleanup(DatabaseType.STATE, rs, ps, null);
			}
		}
	}
}
