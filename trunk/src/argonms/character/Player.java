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

package argonms.character;

import argonms.ServerType;
import argonms.character.inventory.Equip;
import argonms.character.inventory.InventorySlot;
import argonms.character.inventory.Inventory;
import argonms.character.inventory.Inventory.InventoryType;
import argonms.character.inventory.InventoryTools;
import argonms.character.inventory.Item;
import argonms.character.inventory.Pet;
import argonms.character.inventory.Ring;
import argonms.game.GameServer;
import argonms.login.LoginClient;
import argonms.map.MapleMap;
import argonms.net.client.RemoteClient;
import argonms.tools.DatabaseConnection;
import java.awt.Point;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.EnumMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author GoldenKevin
 */
public class Player {
	private static final Logger LOG = Logger.getLogger(Player.class.getName());
	private static final byte SERVER_TYPE;

	private RemoteClient client;
	private byte gm;

	private int id;
	private String name;
	private short eyes, hair;
	private byte skin;
	private byte gender;

	private short level;
	private short job;
	private short str, dex, _int, luk, remHp, remMp, maxHp, maxMp, remAp, remSp;
	private int exp;
	private int mesos;
	private short fame;
	private int partner;
	private Map<InventoryType, Inventory> inventories;
	private MapleMap map;
	private int savedMapId;
	private Point position;
	private byte savedSpawnPoint;
	private Pet[] equippedPets;

	static {
		SERVER_TYPE = ServerType.getType();
	}

	private Player () {
		inventories = new EnumMap<InventoryType, Inventory>(InventoryType.class);
		equippedPets = new Pet[3];
	}

	public void saveCharacter() {
		updateStats();
		updateInventory();
	}

	public void updateStats() {
		Connection con = DatabaseConnection.getConnection();
		try {
			PreparedStatement ps = con.prepareStatement("UPDATE `characters` "
					+ "SET `accountid` = ?, `world` = ?, `name` = ?, "
					+ "`gender` = ?, `skin` = ?, `eyes` = ?, `hair` = ?, "
					+ "`level` = ?, `job` = ?, `str` = ?, `dex` = ?, "
					+ "`int` = ?, `luk` = ?, `hp` = ?, `maxhp` = ?, `mp` = ?, "
					+ "`maxmp` = ?, `ap` = ?, `sp` = ?, `exp` = ?, `fame` = ?, "
					+ "`spouse` = ?, `map` = ?, `spawnpoint` = ?, `mesos` = ?, "
					+ "`equipslots` = ?, `useslots` = ?, `setupslots` = ?, "
					+ "`etcslots` = ?, `cashslots` = ?, `storageslots` = ?,"
					+ "`gm` = ? WHERE `id` = ?");
			ps.setInt(1, client.getAccountId());
			ps.setByte(2, client.getWorld());
			ps.setString(3, name);
			ps.setByte(4, gender);
			ps.setInt(5, skin);
			ps.setInt(6, eyes);
			ps.setInt(7, hair);
			ps.setShort(8, level);
			ps.setShort(9, job);
			ps.setShort(10, str);
			ps.setShort(11, dex);
			ps.setShort(12, _int);
			ps.setShort(13, luk);
			ps.setShort(14, remHp);
			ps.setShort(15, maxHp);
			ps.setShort(16, remMp);
			ps.setShort(17, maxMp);
			ps.setShort(18, remAp);
			ps.setShort(18, remSp);
			ps.setInt(19, exp);
			ps.setShort(20, fame);
			ps.setInt(21, partner);
			ps.setInt(22, getMapId());
			ps.setByte(23, map != null ? map.nearestSpawnPoint(position) : savedSpawnPoint);
			ps.setInt(24, mesos);
			ps.setShort(25, inventories.get(InventoryType.EQUIP).getMaxSlots());
			ps.setShort(26, inventories.get(InventoryType.USE).getMaxSlots());
			ps.setShort(27, inventories.get(InventoryType.SETUP).getMaxSlots());
			ps.setShort(28, inventories.get(InventoryType.ETC).getMaxSlots());
			ps.setShort(29, inventories.get(InventoryType.CASH).getMaxSlots());
			ps.setShort(30, inventories.get(InventoryType.STORAGE).getMaxSlots());
			ps.setByte(31, gm);
			ps.setInt(32, id);
			int updateRows = ps.executeUpdate();
			if (updateRows < 1)
				LOG.log(Level.WARNING, "Updating a deleted character with name "
						+ "{0} of account {1}.", new Object[] { name,
						client.getAccountId() });
			ps.close();
		} catch (SQLException ex) {
			LOG.log(Level.WARNING, "Could not save character '" + name
					+ "' of account " + client.getAccountId()
					+ " to database.", ex);
		}
	}

	public void updateInventory() {
		Connection con = DatabaseConnection.getConnection();
		try {
			//equipped, equips, use, setup, etc, cash
			PreparedStatement ps = con.prepareStatement("DELETE FROM "
					+ "`inventoryitems` WHERE `characterid` = ? AND "
					+ "`inventorytype` <= " + InventoryType.CASH.value());
			ps.setInt(1, id);
			ps.executeUpdate();
			ps.close();

			//cash shop inventory and storage
			ps = con.prepareStatement("DELETE FROM `inventoryitems` WHERE "
					+ "`accountid` = ? AND `inventorytype` > "
					+ InventoryType.CASH.value());
			ps.setInt(1, client.getAccountId());
			ps.executeUpdate();
			ps.close();

			for (Entry<InventoryType, Inventory> ent : inventories.entrySet()) {
				byte inventory = ent.getKey().value();
				for (Entry<Short, InventorySlot> e : ent.getValue().getAll().entrySet()) {
					InventorySlot item = e.getValue();

					ps = con.prepareStatement("INSERT INTO `inventoryitems`"
							+ "(`characterid`,`accountid`,`inventorytype`,"
							+ "`position`,`itemid`,`expiredate`,`uniqueid`,"
							+ "`owner`,`quantity`) VALUES(?,?,?,?,?,?,?,?,?)",
							PreparedStatement.RETURN_GENERATED_KEYS);
					ps.setInt(1, id);
					ps.setInt(2, client.getAccountId());
					ps.setInt(3, inventory);
					ps.setShort(4, e.getKey().shortValue());
					ps.setInt(5, item.getItemId());
					ps.setLong(6, item.getExpiration());
					ps.setInt(7, item.getUniqueId());
					ps.setString(8, item.getOwner());
					ps.setShort(9, item.getQuantity());
					ps.executeUpdate();
					ResultSet rs = ps.getGeneratedKeys();
					int inventoryKey = -1;
					if (rs.next())
						inventoryKey = rs.getInt(1);
					rs.close();
					ps.close();

					switch (item.getType()) {
						case RING:
							Ring ring = (Ring) item;
							ps = con.prepareStatement("INSERT INTO "
									+ "`inventoryrings`(`inventoryitemid`,"
									+ "`partnerchrid`,`partnerringid`) "
									+ "VALUES(?,?,?)");
							ps.setInt(1, inventoryKey);
							ps.setInt(2, ring.getPartnerCharId());
							ps.setInt(3, ring.getPartnerRingId());
							ps.executeUpdate();
							ps.close();
							//fall through to EQUIP!
						case EQUIP:
							Equip equip = (Equip) item;
							ps = con.prepareStatement("INSERT INTO "
									+ "`inventoryequipment` (`inventoryitemid`,"
									+ "`str`,`dex`,`int`,`luk`,`hp`,`mp`,"
									+ "`watk`,`matk`,`wdef`,`mdef`,`acc`,"
									+ "`avoid`,`speed`,`jump`,`upgradeslots`) "
									+ "VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)");
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
							ps.close();
							break;
						case PET:
							Pet pet = (Pet) item;
							ps = con.prepareStatement("INSERT INTO "
									+ "`inventorypets`(`inventoryitemid`,"
									+ "`position`,`name`,`level`,`closeness`,"
									+ "`fullness`,`expired` VALUES(?,?,?,?,?,?,?");
							ps.setInt(1, inventoryKey);
							ps.setByte(2, getPetPosition(pet));
							ps.setString(3, pet.getName());
							ps.setByte(4, pet.getLevel());
							ps.setShort(5, pet.getCloseness());
							ps.setByte(6, pet.getFullness());
							ps.setBoolean(7, pet.isExpired());
							break;
					}
				}
			}
		} catch (SQLException ex) {
			LOG.log(Level.WARNING, "Could not save inventory of character '"
					+ name + "' of account " + client.getAccountId(), ex);
		}
	}

	public static Player loadPlayer(RemoteClient c, int id) {
		Connection con = DatabaseConnection.getConnection();
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = con.prepareStatement("SELECT * FROM `characters` WHERE `id` = ?");
			ps.setInt(1, id);
			rs = ps.executeQuery();
			if (!rs.next()) {
				LOG.log(Level.WARNING, "Client requested to load a non-existant" +
						" character w/ id {0} (account {1}).", new Object[]{id, c.getAccountId()});
				return null;
			}
			int accountid = rs.getInt(1);
			if (c.getServerType() != ServerType.LOGIN) { //game/shop server
				c.setAccountId(accountid); //we aren't aware of our accountid yet
			} else if (accountid != c.getAccountId()) { //login server
				LOG.log(Level.WARNING, "Client account {0} is trying to load character {1} which belongs to account {2}", new Object[] { c.getAccountId(), id, accountid });
				return null;
			}
			byte world = rs.getByte(2);
			if (world != c.getWorld()) {
				LOG.log(Level.WARNING, "Client account {0} is trying to load character {1} on world {2} but exists on world {3}", new Object[] { accountid, id, c.getWorld(), world });
				return null;
			}
			Player p = new Player();
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
			p.savedMapId = rs.getInt(24);
			if (ServerType.isGame(SERVER_TYPE))
				p.map = GameServer.getChannel(c.getChannel()).getMapFactory().getMap(p.savedMapId);
			p.savedSpawnPoint = rs.getByte(25);
			p.mesos = rs.getInt(26);
			p.inventories.put(InventoryType.EQUIP, new Inventory(rs.getByte(27)));
			p.inventories.put(InventoryType.USE, new Inventory(rs.getByte(28)));
			p.inventories.put(InventoryType.SETUP, new Inventory(rs.getByte(29)));
			p.inventories.put(InventoryType.ETC, new Inventory(rs.getByte(30)));
			p.inventories.put(InventoryType.CASH, new Inventory(rs.getByte(31)));
			//TODO: get real equipped inventory size?
			p.inventories.put(InventoryType.EQUIPPED, new Inventory((byte) 0));
			if (ServerType.isGame(SERVER_TYPE))
				p.inventories.put(InventoryType.STORAGE, new Inventory(rs.getByte(32)));
			else if(ServerType.isShop(SERVER_TYPE))
				//TODO: get real cash shop inventory size?
				p.inventories.put(InventoryType.CASH_SHOP, new Inventory((byte) 0));
			p.gm = rs.getByte(33);
			rs.close();
			ps.close();

			String q = "SELECT * FROM `inventoryitems` WHERE `characterid` = ?";
			if (ServerType.isLogin(SERVER_TYPE))
				q += " AND inventorytype <= " + InventoryType.CASH.value();
			else if (ServerType.isGame(SERVER_TYPE))
				q += " AND inventorytype < " + InventoryType.STORAGE.value();
			else if (ServerType.isShop(SERVER_TYPE))
				q += " AND inventorytype != " + InventoryType.STORAGE.value();
			ps = con.prepareStatement(q);
			ps.setInt(1, id);
			rs = ps.executeQuery();
			while (rs.next()) {
				InventorySlot item;
				InventoryType inventoryType = InventoryType.get(rs.getByte(4));
				int itemid = rs.getInt(6);
				int inventoryKey = rs.getInt(1);
				if (inventoryType == InventoryType.EQUIP) {
					PreparedStatement eps;
					ResultSet ers;
					Equip e;
					if (InventoryTools.isRing(itemid)) {
						e = new Ring(itemid);
						eps = con.prepareStatement("SELECT * FROM `inventoryrings` WHERE `inventoryitemid` = ?");
						eps.setInt(1, inventoryKey);
						ers = eps.executeQuery();
						if (ers.next()) {
							((Ring) e).setPartnerCharId(ers.getInt(3));
							((Ring) e).setPartnerRingId(ers.getInt(4));
						}
						ers.close();
						eps.close();
					} else {
						e = new Equip(itemid);
					}
					eps = con.prepareStatement("SELECT * FROM `inventoryequipment` WHERE `inventoryitemid` = ?");
					eps.setInt(1, inventoryKey);
					ers = eps.executeQuery();
					if (ers.next()) {
						e.setStr(ers.getShort(3));
						e.setDex(ers.getShort(4));
						e.setInt(ers.getShort(5));
						e.setLuk(ers.getShort(6));
						e.setHp(ers.getShort(7));
						e.setMp(ers.getShort(8));
						e.setWatk(ers.getShort(9));
						e.setMatk(ers.getShort(10));
						e.setWdef(ers.getShort(11));
						e.setMdef(ers.getShort(12));
						e.setAcc(ers.getShort(13));
						e.setAvoid(ers.getShort(14));
						e.setSpeed(ers.getShort(15));
						e.setJump(ers.getShort(16));
						e.setUpgradeSlots(ers.getByte(18));
					}
					ers.close();
					eps.close();
					item = e;
				} else {
					if (InventoryTools.isPet(itemid)) {
						PreparedStatement pps = con.prepareStatement("SELECT * FROM `inventorypets` WHERE `inventoryitemid` = ?");
						pps.setInt(1, inventoryKey);
						ResultSet prs = pps.executeQuery();
						item = new Pet(itemid, prs.getString(4), prs.getByte(5),
								prs.getShort(6), prs.getByte(7),
								prs.getBoolean(8));
						byte pos = prs.getByte(3);
						if (pos >= 0 && pos < 3)
							p.equippedPets[pos] = (Pet) item;
						prs.close();
						pps.close();
					} else {
						item = new Item(itemid);
						item.setQuantity(rs.getShort(10));
					}
				}
				item.setExpiration(rs.getLong(7));
				item.setUniqueId(rs.getInt(8));
				item.setOwner(rs.getString(9));
				p.inventories.get(inventoryType).put(rs.getShort(5), item);
			}
			return p;
		} catch (SQLException ex) {
			LOG.log(Level.WARNING, "Could not load character " + id + " from database", ex);
			return null;
		} finally {
			try {
				if (rs != null)
					rs.close();
				if (ps != null)
					ps.close();
			} catch (SQLException e) {
				//nothing we can do
			}
		}
	}

	public int getId() {
		return id;
	}

	public String getName() {
		return name;
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

	public Inventory getInventory(InventoryType type) {
		return inventories.get(type);
	}

	public short getFame() {
		return fame;
	}

	public int getMapId() {
		return (map != null ? map.getMapId() : savedMapId);
	}

	public byte getSpawnPoint() {
		return savedSpawnPoint;
	}

	public int getSpouseId() {
		return partner;
	}

	public Pet[] getPets() {
		return equippedPets;
	}

	public byte getPetPosition(Pet p) {
		for (byte i = 0; i < 3; i++)
			if (equippedPets[i] != null && equippedPets[i] == p)
				return i;
		return -1;
	}

	public static Player saveNewPlayer(LoginClient account, String name,
			int eyes, int hair, int skin, byte gender, byte str, byte dex,
			byte _int, byte luk, int top, int bottom, int shoes, int weapon) {
		Player p = new Player();
		p.client = account;
		p.name = name;
		p.eyes = (short) eyes;
		p.hair = (short) hair;
		p.skin = (byte) skin;
		p.gender = gender;
		p.str = str;
		p.dex = dex;
		p._int = _int;
		p.luk = luk;
		p.level = 1;
		p.gm = account.getGm();

		Connection con = DatabaseConnection.getConnection();
		try {
			PreparedStatement ps = con.prepareStatement("INSERT INTO"
					+ "`characters`(`accountid`,`world`,`name`,`gender`,`skin`,"
					+ "`eyes`,`hair`,`str`,`dex`,`int`,`luk`,`gm`) VALUES"
					+ " (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
					PreparedStatement.RETURN_GENERATED_KEYS);
			ps.setInt(1, account.getAccountId());
			ps.setByte(2, account.getWorld());
			ps.setString(3, name);
			ps.setByte(4, gender);
			ps.setInt(5, skin);
			ps.setInt(6, eyes);
			ps.setInt(7, hair);
			ps.setShort(8, str);
			ps.setShort(9, dex);
			ps.setShort(10, _int);
			ps.setShort(11, luk);
			ps.setByte(12, account.getGm());
			ps.executeUpdate();
			ResultSet rs = ps.getGeneratedKeys();
			if (rs.next())
				p.id = rs.getInt(1);
			rs.close();
			ps.close();
		} catch (SQLException ex) {
			LOG.log(Level.WARNING, "Could not create new character '" + name +
					"' on account " + account.getAccountId(), ex);
		}

		p.inventories.put(InventoryType.EQUIP, new Inventory((byte) 24));
		p.inventories.put(InventoryType.USE, new Inventory((byte) 24));
		p.inventories.put(InventoryType.SETUP, new Inventory((byte) 24));
		p.inventories.put(InventoryType.ETC, new Inventory((byte) 24));
		p.inventories.put(InventoryType.CASH, new Inventory((byte) 24));
		//TODO: get real equipped inventory size?
		p.inventories.put(InventoryType.EQUIPPED, new Inventory((byte) 0));

		Inventory equipment = p.inventories.get(InventoryType.EQUIP);
		Inventory equipped = p.inventories.get(InventoryType.EQUIPPED);
		Inventory etc = p.inventories.get(InventoryType.ETC);
		InventoryTools.equip(equipment, equipped,
				InventoryTools.addToInventory(equipment, top, (short) 1)
				.get(0).shortValue(), (short) -5);
		InventoryTools.equip(equipment, equipped,
				InventoryTools.addToInventory(equipment, bottom, (short) 1)
				.get(0).shortValue(), (short) -6);
		InventoryTools.equip(equipment, equipped,
				InventoryTools.addToInventory(equipment, shoes, (short) 1)
				.get(0).shortValue(), (short) -7);
		InventoryTools.equip(equipment, equipped,
				InventoryTools .addToInventory(equipment, weapon, (short) 1)
				.get(0).shortValue(), (short) -11);
		InventoryTools.addToInventory(etc, 4161001, (short) 1);
		p.updateInventory();

		return p;
	}
}
