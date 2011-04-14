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

import argonms.GlobalConstants;
import argonms.character.skill.SkillEntry;
import argonms.ServerType;
import argonms.character.skill.PlayerStatusEffectValues;
import argonms.character.skill.PlayerStatusEffectValues.PlayerStatusEffect;
import argonms.character.skill.Cooldown;
import argonms.character.inventory.Equip;
import argonms.character.inventory.InventorySlot;
import argonms.character.inventory.Inventory;
import argonms.character.inventory.Inventory.InventoryType;
import argonms.character.inventory.InventoryTools;
import argonms.character.inventory.Item;
import argonms.character.inventory.TamingMob;
import argonms.character.inventory.Pet;
import argonms.character.inventory.Ring;
import argonms.character.skill.Skills;
import argonms.character.skill.StatusEffectTools;
import argonms.game.GameServer;
import argonms.loading.StatusEffectsData;
import argonms.loading.skill.SkillDataLoader;
import argonms.login.LoginClient;
import argonms.map.MapEntity;
import argonms.map.GameMap;
import argonms.map.entity.Mob;
import argonms.net.external.CommonPackets;
import argonms.net.external.RemoteClient;
import argonms.tools.DatabaseConnection;
import argonms.tools.Rng;
import argonms.tools.Timer;
import argonms.tools.collections.LockableList;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.TreeMap;
import java.util.concurrent.ScheduledFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author GoldenKevin
 */
public class Player extends MapEntity {
	private static final Logger LOG = Logger.getLogger(Player.class.getName());

	private RemoteClient client;
	private byte gm;

	private String name;
	private short eyes, hair;
	private byte skin;
	private byte gender;

	private final Map<Byte, KeyBinding> bindings;
	private final List<SkillMacro> skillMacros;

	private short level;
	private short job;
	private short baseStr, baseDex, baseInt, baseLuk, baseMaxHp, baseMaxMp;
	private short remHp, remMp, remAp, remSp, maxHp, maxMp;
	private int addStr, addDex, addInt, addLuk, addMaxHp, addMaxMp,
			addWatk, addWdef, addMatk, addMdef, addAcc, addAvo, addHands, addSpeed, addJump;
	private int exp;
	private int mesos;
	private short fame;
	private GameMap map;
	private int savedMapId;
	private byte savedSpawnPoint;
	private int partner;
	private BuddyList buddies;

	private final Map<InventoryType, Inventory> inventories;
	private final Pet[] equippedPets;
	private TamingMob equippedMount;

	private final Map<Integer, SkillEntry> skillEntries;
	private final Map<Integer, Cooldown> cooldowns;
	private final Map<PlayerStatusEffect, PlayerStatusEffectValues> activeEffects;
	private final Map<Integer, ScheduledFuture<?>> skillCancels;
	private final Map<Integer, ScheduledFuture<?>> itemEffectCancels;
	private final Map<Integer, ScheduledFuture<?>> diseaseCancels;

	private final LockableList<MapEntity> visibleEntities;
	private final List<Mob> controllingMobs;

	private int guild;
	private Party party;

	private Player () {
		bindings = new TreeMap<Byte, KeyBinding>();
		skillMacros = new ArrayList<SkillMacro>(5);
		inventories = new EnumMap<InventoryType, Inventory>(InventoryType.class);
		equippedPets = new Pet[3];
		skillEntries = new HashMap<Integer, SkillEntry>();
		cooldowns = new HashMap<Integer, Cooldown>();
		activeEffects = new EnumMap<PlayerStatusEffect, PlayerStatusEffectValues>(PlayerStatusEffect.class);
		skillCancels = new HashMap<Integer, ScheduledFuture<?>>();
		itemEffectCancels = new HashMap<Integer, ScheduledFuture<?>>();
		diseaseCancels = new HashMap<Integer, ScheduledFuture<?>>();
		visibleEntities = new LockableList<MapEntity>(new ArrayList<MapEntity>());
		controllingMobs = new ArrayList<Mob>();
	}

	public void saveCharacter() {
		int prevTransactionIsolation = Connection.TRANSACTION_REPEATABLE_READ;
		boolean prevAutoCommit = true;
		Connection con = DatabaseConnection.getConnection();
		try { //this speeds up saving by a lot
			prevTransactionIsolation = con.getTransactionIsolation();
			prevAutoCommit = con.getAutoCommit();
			con.setTransactionIsolation(Connection.TRANSACTION_READ_UNCOMMITTED);
			con.setAutoCommit(false);
			updateDbStats(con);
			updateDbInventory(con);
			if (ServerType.isGame(client.getServerId())) {
				updateDbSkills(con);
				updateDbCooldowns(con);
				updateDbBindings(con);
			}
			con.commit();
		} catch (SQLException ex) {
			LOG.log(Level.WARNING, "Could not save character " + getId()
					+ ". Rolling back all changes...", ex);
			try {
				con.rollback();
			} catch (SQLException ex2) {
				LOG.log(Level.WARNING, "Error rolling back character.", ex2);
			}
		} finally {
			try {
				con.setAutoCommit(prevAutoCommit);
				con.setTransactionIsolation(prevTransactionIsolation);
			} catch (SQLException ex) {
				LOG.log(Level.WARNING, "Could not reset Connection config "
						+ "after saving character " + getId(), ex);
			}
		}
	}

	public void updateDbStats(Connection con) {
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
					+ "`buddyslots` = ?, `gm` = ? WHERE `id` = ?");
			ps.setInt(1, client.getAccountId());
			ps.setByte(2, client.getWorld());
			ps.setString(3, name);
			ps.setByte(4, gender);
			ps.setInt(5, skin);
			ps.setInt(6, eyes);
			ps.setInt(7, hair);
			ps.setShort(8, level);
			ps.setShort(9, job);
			ps.setShort(10, baseStr);
			ps.setShort(11, baseDex);
			ps.setShort(12, baseInt);
			ps.setShort(13, baseLuk);
			ps.setShort(14, remHp);
			ps.setShort(15, baseMaxHp);
			ps.setShort(16, remMp);
			ps.setShort(17, baseMaxMp);
			ps.setShort(18, remAp);
			ps.setShort(19, remSp);
			ps.setInt(20, exp);
			ps.setShort(21, fame);
			ps.setInt(22, partner);
			ps.setInt(23, getMapId());
			ps.setByte(24, map != null ? map.nearestSpawnPoint(getPosition()) : savedSpawnPoint);
			ps.setInt(25, mesos);
			ps.setShort(26, inventories.get(InventoryType.EQUIP).getMaxSlots());
			ps.setShort(27, inventories.get(InventoryType.USE).getMaxSlots());
			ps.setShort(28, inventories.get(InventoryType.SETUP).getMaxSlots());
			ps.setShort(29, inventories.get(InventoryType.ETC).getMaxSlots());
			ps.setShort(30, inventories.get(InventoryType.CASH).getMaxSlots());
			ps.setShort(31, inventories.get(InventoryType.STORAGE).getMaxSlots());
			ps.setShort(32, buddies.getCapacity());
			ps.setByte(33, gm);
			ps.setInt(34, getId());
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

	public void updateDbInventory(Connection con) {
		try {
			String invUpdate = "DELETE FROM `inventoryitems` WHERE "
					+ "`characterid` = ? AND `inventorytype` <= "
					+ InventoryType.CASH.value();
			boolean invUpdateSpecifyAccId = false;
			if (inventories.containsKey(InventoryType.STORAGE)) { //game server
				invUpdate += " OR `accountid` = ? AND `inventorytype` = "
						+ InventoryType.STORAGE.value();
				invUpdateSpecifyAccId = true;
			} else if (inventories.containsKey(InventoryType.CASH_SHOP)) { //shop server
				invUpdate += " OR `accountid` = ? AND `inventorytype` = "
						+ InventoryType.CASH_SHOP.value();
				invUpdateSpecifyAccId = true;
			}
			PreparedStatement ps = con.prepareStatement(invUpdate);
			ps.setInt(1, getId());
			if (invUpdateSpecifyAccId)
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
				switch (ent.getKey()) {
					case STORAGE:
					case CASH_SHOP:
						//null keys are not affected by foreign key constraints,
						//so we won't get account wide inventories deleted when
						//this character is deleted.
						ps.setNull(1, Types.INTEGER);
						break;
					default:
						ps.setInt(1, getId());
						break;
				}
				ps.setInt(3, ent.getKey().value());
				for (Entry<Short, InventorySlot> e : ent.getValue().getAll().entrySet()) {
					InventorySlot item = e.getValue();

					ps.setShort(4, e.getKey().shortValue());
					ps.setInt(5, item.getItemId());
					ps.setLong(6, item.getExpiration());
					ps.setLong(7, item.getUniqueId());
					ps.setString(8, item.getOwner());
					ps.setShort(9, item.getQuantity());
					//TODO: refactor so we can use addBatch here for inventories
					//(equip, ring, pet, mount) and for items. Run getGeneratedKeys()
					//after executeBatch to get generated keys for each item
					//in iteration order...
					ps.executeUpdate(); //need the generated keys, so no batch
					ResultSet rs = ps.getGeneratedKeys();
					int inventoryKey = -1;
					if (rs.next())
						inventoryKey = rs.getInt(1);
					rs.close();

					switch (item.getType()) {
						case RING:
							Ring ring = (Ring) item;
							PreparedStatement rps = con.prepareStatement(
									"INSERT INTO `inventoryrings` ("
									+ "`inventoryitemid`,`partnerchrid`,"
									+ "`partnerringid`) VALUES(?,?,?)");
							rps.setInt(1, inventoryKey);
							rps.setInt(2, ring.getPartnerCharId());
							rps.setLong(3, ring.getPartnerRingId());
							rps.executeUpdate();
							rps.close();
							insertEquipIntoDb(ring, inventoryKey, con);
							break;
						case EQUIP:
							insertEquipIntoDb((Equip) item, inventoryKey, con);
							break;
						case PET:
							Pet pet = (Pet) item;
							PreparedStatement pps = con.prepareStatement(
									"INSERT INTO `inventorypets` ("
									+ "`inventoryitemid`,`position`,`name`,"
									+ "`level`,`closeness`,`fullness`,`expired`) "
									+ "VALUES (?,?,?,?,?,?,?)");
							pps.setInt(1, inventoryKey);
							pps.setByte(2, getPetPosition(pet));
							pps.setString(3, pet.getName());
							pps.setByte(4, pet.getLevel());
							pps.setShort(5, pet.getCloseness());
							pps.setByte(6, pet.getFullness());
							pps.setBoolean(7, pet.isExpired());
							pps.executeUpdate();
							pps.close();
							break;
						case MOUNT:
							TamingMob mount = (TamingMob) item;
							PreparedStatement mps = con.prepareStatement(
									"INSERT INTO `inventorymounts` ("
									+ "`inventoryitemid`,`level`,`exp`,"
									+ "`tiredness`) VALUES (?,?,?,?)");
							mps.setInt(1, inventoryKey);
							mps.setByte(2, mount.getMountLevel());
							mps.setShort(3, mount.getExp());
							mps.setByte(4, mount.getTiredness());
							mps.executeUpdate();
							mps.close();
							insertEquipIntoDb(mount, inventoryKey, con);
							break;
					}
				}
			}
			ps.close();
		} catch (SQLException ex) {
			LOG.log(Level.WARNING, "Could not save inventory of character '"
					+ name + "' of account " + client.getAccountId(), ex);
		}
	}

	private void insertEquipIntoDb(Equip equip, int inventoryKey,
			Connection con) throws SQLException {
		PreparedStatement eps = con.prepareStatement(
				"INSERT INTO `inventoryequipment` ("
				+ "`inventoryitemid`,`str`,`dex`,`int`,"
				+ "`luk`,`hp`,`mp`,`watk`,`matk`,`wdef`,"
				+ "`mdef`,`acc`,`avoid`,`speed`,`jump`,"
				+ "`upgradeslots`) "
				+ "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)");
		eps.setInt(1, inventoryKey);
		eps.setShort(2, equip.getStr());
		eps.setShort(3, equip.getDex());
		eps.setShort(4, equip.getInt());
		eps.setShort(5, equip.getLuk());
		eps.setShort(6, equip.getHp());
		eps.setShort(7, equip.getMp());
		eps.setShort(8, equip.getWatk());
		eps.setShort(9, equip.getMatk());
		eps.setShort(10, equip.getWdef());
		eps.setShort(11, equip.getMdef());
		eps.setShort(12, equip.getAcc());
		eps.setShort(13, equip.getAvoid());
		eps.setShort(14, equip.getSpeed());
		eps.setShort(15, equip.getJump());
		eps.setByte(16, equip.getUpgradeSlots());
		eps.executeUpdate();
		eps.close();
	}

	public void updateDbSkills(Connection con) {
		try {
			PreparedStatement ps = con.prepareStatement("DELETE FROM `skills` "
					+ "WHERE `characterid` = ?");
			ps.setInt(1, getId());
			ps.executeUpdate();
			ps.close();

			ps = con.prepareStatement("INSERT INTO `skills` (`characterid`," +
					"`skillid`,`level`,`mastery`) VALUES (?,?,?,?)");
			ps.setInt(1, getId());
			for (Entry<Integer, SkillEntry> skill : skillEntries.entrySet()) {
				SkillEntry skillLevel = skill.getValue();
				ps.setInt(2, skill.getKey().intValue());
				ps.setByte(3, skillLevel.getLevel());
				ps.setByte(4, skillLevel.getMasterLevel());
				ps.addBatch();
			}
			ps.executeBatch();
			ps.close();
		} catch (SQLException ex) {
			LOG.log(Level.WARNING, "Could not save skill levels of "
					+ "character " + name, ex);
		}
	}

	public void updateDbCooldowns(Connection con) {
		try {
			PreparedStatement ps = con.prepareStatement("DELETE FROM `cooldowns` "
					+ "WHERE `characterid` = ?");
			ps.setInt(1, getId());
			ps.executeUpdate();
			ps.close();

			ps = con.prepareStatement("INSERT INTO `cooldowns`" +
					"(`characterid`,`skill`,`remaining`) VALUES (?,?,?)");
			ps.setInt(1, getId());
			for (Entry<Integer, Cooldown> cooling : cooldowns.entrySet()) {
				ps.setInt(1, cooling.getKey().intValue());
				ps.setShort(2, cooling.getValue().getSecondsRemaining());
				ps.addBatch();
			}
			ps.executeBatch();
			ps.close();
		} catch (SQLException ex) {
			LOG.log(Level.WARNING, "Could not save cooldowns of "
					+ "character " + name, ex);
		}
	}

	public void updateDbBindings(Connection con) {
		try {
			PreparedStatement ps = con.prepareStatement("DELETE FROM `keymaps` "
					+ "WHERE `characterid` = ?");
			ps.setInt(1, getId());
			ps.executeUpdate();
			ps.close();

			//thank goodness for Connection.setAutoCommit(false) and
			//rewriteBatchedStatements=true or else this would take over 2 secs!
			ps = con.prepareStatement("INSERT INTO `keymaps` (`characterid`,"
					+ "`key`,`type`,`action`) VALUES (?,?,?,?)");
			ps.setInt(1, getId());
			for (Entry<Byte, KeyBinding> entry : bindings.entrySet()) {
				KeyBinding binding = entry.getValue();
				ps.setByte(2, entry.getKey().byteValue());
				ps.setByte(3, binding.getType());
				ps.setInt(4, binding.getAction());
				ps.addBatch();
			}
			ps.executeBatch();
			ps.close();

			ps = con.prepareStatement("DELETE FROM `skillmacros` WHERE "
					+ "`characterid` = ?");
			ps.setInt(1, getId());
			ps.executeUpdate();
			ps.close();

			byte pos = 0;
			ps = con.prepareStatement("INSERT INTO `skillmacros` "
					+ "(`characterid`,`position`,`name`,`shout`,`skill1`,"
					+ "`skill2`,`skill3`) VALUES (?,?,?,?,?,?,?)");
			ps.setInt(1, getId());
			for (SkillMacro macro : skillMacros) {
				ps.setByte(2, pos++);
				ps.setString(3, macro.getName());
				ps.setBoolean(4, macro.shout());
				ps.setInt(5, macro.getFirstSkill());
				ps.setInt(6, macro.getSecondSkill());
				ps.setInt(7, macro.getThirdSkill());
				ps.addBatch();
			}
			ps.executeBatch();
			ps.close();
		} catch (SQLException ex) {
			LOG.log(Level.WARNING, "Could not save keymap and skill macros of "
					+ "character " + name, ex);
		}
	}

	public static Player loadPlayer(RemoteClient c, int id) {
		Connection con = DatabaseConnection.getConnection();
		PreparedStatement ps = null;
		ResultSet rs = null;
		boolean gameServer = ServerType.isGame(c.getServerId());
		boolean shopServer = ServerType.isShop(c.getServerId());
		try {
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
			if (gameServer || shopServer) {
				c.setAccountId(accountid); //we aren't aware of our accountid yet
			} else if (accountid != c.getAccountId()) { //login server
				LOG.log(Level.WARNING, "Client account {0} is trying to load "
						+ "character {1} which belongs to account {2}",
						new Object[] { c.getAccountId(), id, accountid });
				return null;
			}
			byte world = rs.getByte(2);
			if (shopServer) {
				c.setWorld(world); //we aren't aware of our world yet
			} else if (world != c.getWorld()) {
				LOG.log(Level.WARNING, "Client account {0} is trying to load "
						+ "character {1} on world {2} but exists on world {3}",
						new Object[] { accountid, id, c.getWorld(), world });
				return null;
			}
			Player p = new Player();
			p.client = c;
			p.setId(id);
			p.name = rs.getString(4);
			p.gender = rs.getByte(5);
			p.skin = rs.getByte(6);
			p.eyes = rs.getShort(7);
			p.hair = rs.getShort(8);
			p.level = rs.getShort(9);
			p.job = rs.getShort(10);
			p.baseStr = rs.getShort(11);
			p.baseDex = rs.getShort(12);
			p.baseInt = rs.getShort(13);
			p.baseLuk = rs.getShort(14);
			p.baseMaxHp = rs.getShort(16);
			p.baseMaxMp = rs.getShort(18);
			p.remAp = rs.getShort(19);
			p.remSp = rs.getShort(20);
			p.exp = rs.getInt(21);
			p.fame = rs.getShort(22);
			p.partner = rs.getInt(23);
			p.savedMapId = rs.getInt(24);
			p.savedSpawnPoint = rs.getByte(25);
			if (gameServer) {
				p.map = GameServer.getChannel(c.getChannel()).getMapFactory().getMap(p.savedMapId);
				int forcedReturn = p.map.getForcedReturnMap();
				if (forcedReturn != GlobalConstants.NULL_MAP) {
					p.map = GameServer.getChannel(p.getClient().getChannel()).getMapFactory().getMap(forcedReturn);
					p.savedSpawnPoint = 0;
				}
				p.setPosition(p.map.getPortalPosition(p.savedSpawnPoint));

				//login and shop servers don't need remHp & remMp, so sending 0
				//for remHp/remMp in CommonPackets.writeCharStats is fine
				p.maxHp = p.baseMaxHp;
				p.remHp = (short) Math.min(rs.getShort(15), p.maxHp);
				p.maxMp = p.baseMaxMp;
				p.remMp = (short) Math.min(rs.getShort(17), p.maxMp);
				//TODO: move more game/shop server exclusive stat loading into
				//this block?
			}
			p.mesos = rs.getInt(26);

			String invQuery = "SELECT * FROM `inventoryitems` WHERE `characterid` = ? "
					+ "AND `inventorytype` <= " + InventoryType.CASH.value();
			boolean invQuerySpecifyAccId = false;
			p.inventories.put(InventoryType.EQUIP, new Inventory(rs.getByte(27)));
			p.inventories.put(InventoryType.USE, new Inventory(rs.getByte(28)));
			p.inventories.put(InventoryType.SETUP, new Inventory(rs.getByte(29)));
			p.inventories.put(InventoryType.ETC, new Inventory(rs.getByte(30)));
			p.inventories.put(InventoryType.CASH, new Inventory(rs.getByte(31)));
			//TODO: get real equipped inventory size?
			p.inventories.put(InventoryType.EQUIPPED, new Inventory((byte) 0));
			if (gameServer) {
				p.inventories.put(InventoryType.STORAGE, new Inventory(rs.getByte(32)));
				invQuery += " OR `accountid` = ? AND `inventorytype` = " + InventoryType.STORAGE.value();
				invQuerySpecifyAccId = true;
			} else if (shopServer) {
				//TODO: get real cash shop inventory size?
				p.inventories.put(InventoryType.CASH_SHOP, new Inventory((byte) 0));
				invQuery += " OR `accountid` = ? AND `inventorytype` = " + InventoryType.CASH_SHOP.value();
				invQuerySpecifyAccId = true;
			}
			p.buddies = new BuddyList(rs.getShort(33));
			p.gm = rs.getByte(34);
			rs.close();
			ps.close();

			ps = con.prepareStatement(invQuery);
			ps.setInt(1, id);
			if (invQuerySpecifyAccId)
				ps.setInt(2, accountid);
			rs = ps.executeQuery();
			while (rs.next()) {
				InventorySlot item;
				InventoryType inventoryType = InventoryType.get(rs.getByte(4));
				short position = rs.getShort(5);
				int itemid = rs.getInt(6);
				int inventoryKey = rs.getInt(1);
				if (inventoryType == InventoryType.EQUIP || inventoryType == InventoryType.EQUIPPED) {
					PreparedStatement eps;
					ResultSet ers;
					Equip e;
					if (InventoryTools.isRing(itemid)) {
						e = new Ring(itemid);
						eps = con.prepareStatement("SELECT * FROM "
								+ "`inventoryrings` WHERE "
								+ "`inventoryitemid` = ?");
						eps.setInt(1, inventoryKey);
						ers = eps.executeQuery();
						if (ers.next()) {
							((Ring) e).setPartnerCharId(ers.getInt(3));
							((Ring) e).setPartnerRingId(ers.getLong(4));
						}
						ers.close();
						eps.close();
					} else if (InventoryTools.isMount(itemid)) {
						e = new TamingMob(itemid);
						eps = con.prepareStatement("SELECT * FROM "
								+ "`inventorymounts` WHERE "
								+ "`inventoryitemid` = ?");
						eps.setInt(1, inventoryKey);
						ers = eps.executeQuery();
						if (ers.next()) {
							((TamingMob) e).setLevel(ers.getByte(3));
							((TamingMob) e).setExp(ers.getShort(4));
							((TamingMob) e).setTiredness(ers.getByte(5));
							if (position == -18)
								p.equippedMount = (TamingMob) e;
						}
						ers.close();
						eps.close();
					} else {
						e = new Equip(itemid);
					}
					eps = con.prepareStatement("SELECT * FROM "
							+ "`inventoryequipment` WHERE "
							+ "`inventoryitemid` = ?");
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
						e.setUpgradeSlots(ers.getByte(17));
					}
					if (gameServer)
						p.equipChanged(e, true);
					ers.close();
					eps.close();
					item = e;
				} else {
					if (InventoryTools.isPet(itemid)) {
						Pet pet = new Pet(itemid);
						PreparedStatement pps = con.prepareStatement("SELECT * "
								+ "FROM `inventorypets` WHERE "
								+ "`inventoryitemid` = ?");
						pps.setInt(1, inventoryKey);
						ResultSet prs = pps.executeQuery();
						if (prs.next()) {
							pet.setName(prs.getString(4));
							pet.setLevel(prs.getByte(5));
							pet.setCloseness(prs.getShort(6));
							pet.setFullness(prs.getByte(7));
							pet.setExpired(prs.getBoolean(8));
							byte pos = prs.getByte(3);
							if (pos >= 0 && pos < 3)
								p.equippedPets[pos] = pet;
						}
						prs.close();
						pps.close();
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
			rs.close();
			ps.close();

			if (gameServer) {
				ps = con.prepareStatement("SELECT `skillid`,`level`,`mastery` "
						+ "FROM `skills` WHERE `characterid` = ?");
				ps.setInt(1, id);
				rs = ps.executeQuery();
				while (rs.next())
					p.skillEntries.put(Integer.valueOf(rs.getInt(1)),
							new SkillEntry(rs.getByte(2), rs.getByte(3)));
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

				ps = con.prepareStatement("SELECT `key`,`type`,`action` "
						+ "FROM `keymaps` WHERE `characterid` = ?");
				ps.setInt(1, id);
				rs = ps.executeQuery();
				while (rs.next()) {
					byte key = rs.getByte(1);
					byte type = rs.getByte(2);
					int action = rs.getInt(3);
					p.bindings.put(Byte.valueOf(key), new KeyBinding(type, action));
				}
				rs.close();
				ps.close();

				ps = con.prepareStatement("SELECT `name`,`shout`,"
						+ "`skill1`,`skill2`,`skill3` FROM `skillmacros` "
						+ "WHERE `characterid` = ? ORDER BY `position`");
				ps.setInt(1, id);
				rs = ps.executeQuery();
				while (rs.next()) {
					p.skillMacros.add(new SkillMacro(rs.getString(1),
							rs.getBoolean(2), rs.getInt(3), rs.getInt(4),
							rs.getInt(5)));
				}
				rs.close();
				ps.close();
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

	public RemoteClient getClient() {
		return client;
	}

	public byte getPrivilegeLevel() {
		return gm;
	}

	public String getName() {
		return name;
	}

	public KeyBinding[] getKeyMap() {
		KeyBinding[] ret = new KeyBinding[90];
		for (Entry<Byte, KeyBinding> entry : bindings.entrySet())
			ret[entry.getKey().byteValue()] = entry.getValue();
		return ret;
	}

	public void bindKey(byte key, byte type, int action) {
		//if (type == 0)
			//bindings.remove(Byte.valueOf(key));
		//else
			bindings.put(Byte.valueOf(key), new KeyBinding(type, action));
	}

	public List<SkillMacro> getMacros() {
		return skillMacros;
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

	public int getExp() {
		return exp;
	}

	public void setExp(int newExp) {
		this.exp = newExp;
		getClient().getSession().send(CommonPackets.writeUpdatePlayerStats(Collections.singletonMap(ClientUpdateKey.EXP, Integer.valueOf(exp)), false));
	}

	public void gainExp(int gain, boolean isKiller, boolean fromQuest) {
		if (level < 200) {
			getClient().getSession().send(CommonPackets.writeShowExpGain(gain, isKiller, fromQuest));

			Map<ClientUpdateKey, Number> updatedStats = new EnumMap<ClientUpdateKey, Number>(ClientUpdateKey.class);
			long newExp = exp + gain; //should solve many overflow errors
			if (newExp >= ExpTables.getForLevel(level))
				newExp = levelUp(newExp, updatedStats);
			updatedStats.put(ClientUpdateKey.EXP, Integer.valueOf(exp = (int) newExp));
			getClient().getSession().send(CommonPackets.writeUpdatePlayerStats(updatedStats, false));
		}
	}

	private long levelUp(long exp, Map<ClientUpdateKey, Number> stats) {
		boolean singleLevelOnly = !GameServer.getVariables().doMultiLevel();
		//TODO: send new level to party mates
		Random generator = Rng.getGenerator();
		int intMpBonus = baseInt / 10;
		//local variables are faster and safer (concurrent access and overflow)
		//than directly modifying the heap variables...
		short hpInc = 0, mpInc = 0, apInc = 0, spInc = 0;
		do {
			switch (PlayerJob.getJobPath(job)) {
				case PlayerJob.CLASS_BEGINNER:
					hpInc += generator.nextInt(16 - 12 + 1) + 12;
					mpInc += generator.nextInt(12 - 10 + 1) + 10 + intMpBonus;
					break;
				case PlayerJob.CLASS_WARRIOR:
					hpInc += generator.nextInt(28 - 24 + 1) + 24;
					mpInc += generator.nextInt(6 - 4 + 1) + 4 + intMpBonus;
					spInc += 3;
					break;
				case PlayerJob.CLASS_MAGICIAN:
					hpInc += generator.nextInt(14 - 10 + 1) + 10;
					mpInc += generator.nextInt(24 - 22 + 1) + 22 + intMpBonus;
					spInc += 3;
					break;
				case PlayerJob.CLASS_BOWMAN:
				case PlayerJob.CLASS_THIEF:
				case PlayerJob.CLASS_GAMEMASTER:
					hpInc += generator.nextInt(24 - 20 + 1) + 20;
					mpInc += generator.nextInt(16 - 14 + 1) + 14 + intMpBonus;
					spInc += 3;
					break;
				case PlayerJob.CLASS_PIRATE:
					hpInc += generator.nextInt(28 - 22 + 1) + 22;
					mpInc += generator.nextInt(23 - 18 + 1) + 18 + intMpBonus;
					spInc += 3;
					break;
			}
			byte skillLevel;
			if ((skillLevel = getSkillLevel(Skills.IMPROVED_MAXHP_INCREASE)) != 0)
				hpInc += SkillDataLoader.getInstance()
						.getSkill(Skills.IMPROVED_MAXHP_INCREASE)
						.getLevel(skillLevel)
						.getX();
			if ((skillLevel = getSkillLevel(Skills.IMPROVE_MAXHP)) != 0)
				hpInc += SkillDataLoader.getInstance()
						.getSkill(Skills.IMPROVE_MAXHP)
						.getLevel(skillLevel)
						.getX();
			if ((skillLevel = getSkillLevel(Skills.IMPROVED_MAXMP_INCREASE)) != 0)
				mpInc += SkillDataLoader.getInstance()
						.getSkill(Skills.IMPROVED_MAXMP_INCREASE)
						.getLevel(skillLevel)
						.getX();
			apInc += 5;
			exp -= ExpTables.getForLevel(level++);
			if (singleLevelOnly && exp >= ExpTables.getForLevel(level))
				exp = ExpTables.getForLevel(level) - 1;
		} while (level < 200 && exp >= ExpTables.getForLevel(level));

		remHp = updateMaxHp((short) Math.min(baseMaxHp + hpInc, 30000));
		remMp = updateMaxMp((short) Math.min(baseMaxMp + mpInc, 30000));
		remAp = (short) Math.min(remAp + apInc, Short.MAX_VALUE);
		remSp = (short) Math.min(remSp + spInc, Short.MAX_VALUE);

		stats.put(ClientUpdateKey.LEVEL, Short.valueOf(level));
		stats.put(ClientUpdateKey.MAXHP, Short.valueOf(baseMaxHp));
		stats.put(ClientUpdateKey.MAXMP, Short.valueOf(baseMaxMp));
		stats.put(ClientUpdateKey.HP, Short.valueOf(remHp));
		stats.put(ClientUpdateKey.MP, Short.valueOf(remMp));
		stats.put(ClientUpdateKey.AVAILABLEAP, Short.valueOf(remAp));
		stats.put(ClientUpdateKey.AVAILABLESP, Short.valueOf(remSp));

		getMap().sendToAll(CommonPackets.writeShowLevelUp(this), this);

		return level < 200 ? exp : 0;
	}

	public short getLevel() {
		return level;
	}

	public void setLevel(short newLevel) {
		this.level = newLevel;
		getClient().getSession().send(CommonPackets.writeUpdatePlayerStats(Collections.singletonMap(ClientUpdateKey.LEVEL, Short.valueOf(level)), false));
	}

	public short getJob() {
		return job;
	}

	public void setJob(short newJob) {
		this.job = newJob;
		getClient().getSession().send(CommonPackets.writeUpdatePlayerStats(Collections.singletonMap(ClientUpdateKey.JOB, Short.valueOf(job)), false));
	}

	public short getStr() {
		return baseStr;
	}

	public int getCurrentStr() {
		return baseStr + addStr;
	}

	public void setStr(short newStr) {
		this.baseStr = newStr;
		getClient().getSession().send(CommonPackets.writeUpdatePlayerStats(Collections.singletonMap(ClientUpdateKey.STR, Short.valueOf(baseStr)), false));
	}

	public short getDex() {
		return baseDex;
	}

	public int getCurrentDex() {
		return baseDex + addDex;
	}

	public void setDex(short newDex) {
		this.baseDex = newDex;
		getClient().getSession().send(CommonPackets.writeUpdatePlayerStats(Collections.singletonMap(ClientUpdateKey.DEX, Short.valueOf(baseDex)), false));
	}

	public short getInt() {
		return baseInt;
	}

	public int getCurrentInt() {
		return baseInt + addInt;
	}

	public void setInt(short newInt) {
		this.baseInt = newInt;
		getClient().getSession().send(CommonPackets.writeUpdatePlayerStats(Collections.singletonMap(ClientUpdateKey.INT, Short.valueOf(baseInt)), false));
	}

	public short getLuk() {
		return baseLuk;
	}

	public int getCurrentLuk() {
		return baseLuk + addLuk;
	}

	public void setLuk(short newLuk) {
		this.baseLuk = newLuk;
		getClient().getSession().send(CommonPackets.writeUpdatePlayerStats(Collections.singletonMap(ClientUpdateKey.LUK, Short.valueOf(baseLuk)), false));
	}

	public short getHp() {
		return remHp;
	}

	public void setHp(short newHp) {
		//TODO: send new hp to party mates (I think v0.62 supports it...)
		if (newHp < 0)
			newHp = 0;
		else if (newHp > maxHp)
			newHp = maxHp;
		this.remHp = newHp;
		getClient().getSession().send(CommonPackets.writeUpdatePlayerStats(Collections.singletonMap(ClientUpdateKey.HP, Short.valueOf(remHp)), false));
		if (remHp == 0)
			died();
	}

	public void gainHp(int gain) {
		setHp((short) Math.min(remHp + gain, Short.MAX_VALUE));
	}

	public short getMaxHp() {
		return baseMaxHp;
	}

	public short getCurrentMaxHp() {
		return maxHp;
	}

	public void died() {
		getClient().getSession().send(CommonPackets.writeEnableActions());
		//TODO: lose exp
	}

	private short updateMaxHp(short newMax) {
		this.baseMaxHp = newMax;
		recalculateMaxHp();
		return maxHp;
	}

	public void setMaxHp(short newMax) {
		updateMaxHp(newMax);
		if (remHp > maxHp)
			remHp = maxHp;
		getClient().getSession().send(CommonPackets.writeUpdatePlayerStats(Collections.singletonMap(ClientUpdateKey.MAXHP, Short.valueOf(baseMaxHp)), false));
	}

	public void recalculateMaxHp(short hhbPerc) {
		if (hhbPerc == 0)
			this.maxHp = (short) Math.min(30000, baseMaxHp + addMaxHp);
		else
			this.maxHp = (short) Math.min(30000, baseMaxHp + addMaxHp
					+ Math.round((baseMaxHp + addMaxHp) * hhbPerc / 100.0));
	}

	private void recalculateMaxHp() {
		PlayerStatusEffectValues hhb = getEffectValue(PlayerStatusEffect.HYPER_BODY_HP);
		short mod = (hhb == null) ? 0 : hhb.getModifier();
		recalculateMaxHp(mod);
	}

	public void doDecHp(int protectItem, int dec) {
		if (!getInventory(InventoryType.EQUIPPED).hasItem(protectItem, (short) 1)) {
			PlayerStatusEffectValues mg = getEffectValue(PlayerStatusEffect.MAGIC_GUARD);
			if (mg != null) {
				int delta = dec * mg.getModifier() / 100;
				dec -= delta;
				gainMp(-delta);
			}
			gainHp(-dec);
		}
	}

	public short getMp() {
		return remMp;
	}

	public void setMp(short newMp) {
		if (newMp < 0)
			newMp = 0;
		else if (newMp > maxMp)
			newMp = maxMp;
		this.remMp = newMp;
		getClient().getSession().send(CommonPackets.writeUpdatePlayerStats(Collections.singletonMap(ClientUpdateKey.MP, Short.valueOf(remMp)), false));
	}

	public void gainMp(int gain) {
		setMp((short) Math.min(remMp + gain, Short.MAX_VALUE));
	}

	public short getMaxMp() {
		return baseMaxMp;
	}

	public short getCurrentMaxMp() {
		return maxMp;
	}

	private short updateMaxMp(short newMax) {
		this.baseMaxMp = newMax;
		recalculateMaxMp();
		return maxMp;
	}

	public void setMaxMp(short newMax) {
		updateMaxMp(newMax);
		if (remMp > maxMp)
			remMp = maxMp;
		getClient().getSession().send(CommonPackets.writeUpdatePlayerStats(Collections.singletonMap(ClientUpdateKey.MAXMP, Short.valueOf(baseMaxMp)), false));
	}

	/**
	 *
	 * @param mhbPerc Mana hyper body percent.
	 */
	public void recalculateMaxMp(short mhbPerc) {
		if (mhbPerc == 0)
			this.maxMp = (short) Math.min(30000, baseMaxMp + addMaxMp);
		else
			this.maxMp = (short) Math.min(30000, baseMaxMp + addMaxMp
					+ Math.round((baseMaxMp + addMaxMp) * mhbPerc / 100.0));
	}

	private void recalculateMaxMp() {
		PlayerStatusEffectValues mhb = getEffectValue(PlayerStatusEffect.HYPER_BODY_MP);
		short mod = (mhb == null) ? 0 : mhb.getModifier();
		recalculateMaxMp(mod);
	}

	public short getAp() {
		return remAp;
	}

	public void setAp(short newAp) {
		this.remAp = newAp;
		getClient().getSession().send(CommonPackets.writeUpdatePlayerStats(Collections.singletonMap(ClientUpdateKey.AVAILABLEAP, Short.valueOf(remAp)), false));
	}

	public short getSp() {
		return remSp;
	}

	public void setSp(short newSp) {
		this.remSp = newSp;
		getClient().getSession().send(CommonPackets.writeUpdatePlayerStats(Collections.singletonMap(ClientUpdateKey.AVAILABLESP, Short.valueOf(remSp)), false));
	}

	public short getFame() {
		return fame;
	}

	public void setFame(short newFame) {
		this.fame = newFame;
		getClient().getSession().send(CommonPackets.writeUpdatePlayerStats(Collections.singletonMap(ClientUpdateKey.FAME, Integer.valueOf(fame)), false));
	}

	public Inventory getInventory(InventoryType type) {
		return inventories.get(type);
	}

	public int getMesos() {
		return mesos;
	}

	public void setMesos(int newValue, boolean fromDrop) {
		this.mesos = newValue;
		getClient().getSession().send(CommonPackets.writeUpdatePlayerStats(Collections.singletonMap(ClientUpdateKey.MESO, Integer.valueOf(mesos)), fromDrop));
	}

	public void setMesos(int newValue) {
		setMesos(newValue, false);
	}

	public boolean gainMesos(int gain, boolean fromQuest, boolean fromDrop) {
		long newValue = (long) mesos + gain;
		if (newValue <= Integer.MAX_VALUE) {
			setMesos((int) newValue, fromDrop);
			if (!fromQuest) {
				if (gain > 0) //don't show when we're dropping mesos, only show when we're picking up
					getClient().getSession().send(CommonPackets.writeShowMesoGain(gain));
			} else {
				getClient().getSession().send(CommonPackets.writeShowPointsGainFromQuest(gain, (byte) 5));
			}
			return true;
		}
		return false;
	}

	public void gainMesos(int gain, boolean fromQuest) {
		gainMesos(gain, fromQuest, false);
	}

	public void addWatk(int inc) {
		addWatk += inc;
	}

	public void addWdef(int inc) {
		addWdef += inc;
	}

	public void addMatk(int inc) {
		addMatk += inc;
	}

	public void addMdef(int inc) {
		addMdef += inc;
	}

	public void addAcc(int inc) {
		addAcc += inc;
	}

	public void addAvoid(int inc) {
		addAvo += inc;
	}

	public void addHands(int inc) {
		addHands += inc;
	}

	public void addSpeed(int inc) {
		addSpeed += inc;
	}

	public void addJump(int inc) {
		addJump += inc;
	}

	public GameMap getMap() {
		return map;
	}

	public GameMap getReturnMap() {
		return GameServer.getChannel(client.getChannel())
				.getMapFactory().getMap(map.getReturnMap());
	}

	public GameMap getForcedReturnMap() {
		return GameServer.getChannel(client.getChannel())
				.getMapFactory().getMap(map.getForcedReturnMap());
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

	public BuddyList getBuddyList() {
		return buddies;
	}

	//Scrolling equips will screw this up, so remember
	//to call equipChanged(e, false) before scrolling and
	//then call equipChanged(e, true) after scrolling
	public void equipChanged(Equip e, boolean putOn) {
		short stat;
		if (putOn) {
			stat = e.getHp();
			if (stat > 0) {
				addMaxHp += stat;
				recalculateMaxHp();
			}
			stat = e.getMp();
			if (stat > 0) {
				addMaxMp += stat;
				recalculateMaxMp();
			}
			addStr += e.getStr();
			addDex += e.getDex();
			addInt += e.getInt();
			addLuk += e.getLuk();
			addWatk += e.getWatk();
			addWdef += e.getWdef();
			addMatk += e.getMatk();
			addMdef += e.getMdef();
			addAcc += e.getAcc();
			addAvo += e.getAvoid();
			addHands += e.getHands();
			addSpeed += e.getSpeed();
			addJump += e.getJump();
		} else {
			stat = e.getHp();
			if (stat > 0) {
				addMaxHp -= stat;
				recalculateMaxHp();
				if (remHp > maxHp)
					remHp = maxHp;
			}
			stat = e.getMp();
			if (stat > 0) {
				addMaxMp -= stat;
				recalculateMaxMp();
				if (remMp > maxMp)
					remMp = maxMp;
			}
			addStr -= e.getStr();
			addDex -= e.getDex();
			addInt -= e.getInt();
			addLuk -= e.getLuk();
			addWatk -= e.getWatk();
			addWdef -= e.getWdef();
			addMatk -= e.getMatk();
			addMdef -= e.getMdef();
			addAcc -= e.getAcc();
			addAvo -= e.getAvoid();
			addHands -= e.getHands();
			addSpeed -= e.getSpeed();
			addJump -= e.getJump();
		}
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

	public TamingMob getEquippedMount() {
		return equippedMount;
	}

	public Map<Integer, SkillEntry> getSkillEntries() {
		return Collections.unmodifiableMap(skillEntries);
	}

	public byte getSkillLevel(int skill) {
		SkillEntry skillLevel = skillEntries.get(Integer.valueOf(skill));
		return skillLevel != null ? skillLevel.getLevel() : 0;
	}

	/**
	 *
	 * @param skill
	 * @param level
	 * @param masterLevel set to -1 if you do not wish to change the max level
	 */
	public void setSkillLevel(int skill, byte level, byte masterLevel) {
		SkillEntry skillLevel = skillEntries.get(Integer.valueOf(skill));
		if (skillLevel == null) {
			if (masterLevel == -1)
				masterLevel = 0;
			skillLevel = new SkillEntry(level, masterLevel);
			skillEntries.put(Integer.valueOf(skill), skillLevel);
		} else {
			skillLevel.changeCurrentLevel(level);
			if (masterLevel != -1)
				skillLevel.changeMasterLevel(masterLevel);
		}
		getClient().getSession().send(CommonPackets.writeUpdateSkillLevel(
				skill, skillLevel.getLevel(), skillLevel.getMasterLevel()));
	}

	public void addCooldown(final int skill, short time) {
		cooldowns.put(Integer.valueOf(skill), new Cooldown(time * 1000, new Runnable() {
			public void run() {
				getClient().getSession().send(CommonPackets.writeCooldown(skill, (short) 0));
				removeCooldown(skill);
			}
		}));
	}

	public void removeCooldown(int skill) {
		cooldowns.remove(Integer.valueOf(skill)).cancel();
	}

	public Map<Integer, Cooldown> getCooldowns() {
		return cooldowns;
	}

	public Map<PlayerStatusEffect, Short> applyEffect(final StatusEffectsData e) {
		Map<PlayerStatusEffect, Short> updatedStats = new EnumMap<PlayerStatusEffect, Short>(PlayerStatusEffect.class);
		synchronized (activeEffects) {
			for (PlayerStatusEffect buff : e.getEffects()) {
				PlayerStatusEffectValues value = StatusEffectTools.applyEffect(this, e, buff);
				updatedStats.put(buff, Short.valueOf(value.getModifier()));
				activeEffects.put(buff, value);
			}
		}
		ScheduledFuture<?> cancelTask = Timer.getInstance().runAfterDelay(new Runnable() {
			public void run() {
				dispelEffect(e);
			}
		}, e.getDuration());
		switch (e.getSourceType()) {
			case ITEM:
				synchronized (itemEffectCancels) {
					itemEffectCancels.put(e.getDataId(), cancelTask);
				}
				break;
			case PLAYER_SKILL:
				synchronized (skillCancels) {
					skillCancels.put(e.getDataId(), cancelTask);
				}
				break;
			case MOB_SKILL:
				synchronized (diseaseCancels) {
					diseaseCancels.put(e.getDataId(), cancelTask);
				}
				break;
		}
		return updatedStats;
	}

	public Map<PlayerStatusEffect, PlayerStatusEffectValues> getAllEffects() {
		return activeEffects;
	}

	public PlayerStatusEffectValues getEffectValue(PlayerStatusEffect buff) {
		return activeEffects.get(buff);
	}

	public void dispelEffect(StatusEffectsData e) {
		synchronized (activeEffects) {
			for (PlayerStatusEffect buff : e.getEffects())
				StatusEffectTools.dispelEffect(this, buff, activeEffects.remove(buff));
		}
		switch (e.getSourceType()) {
			case ITEM:
				synchronized (itemEffectCancels) {
					itemEffectCancels.remove(e.getDataId()).cancel(true);
				}
				break;
			case PLAYER_SKILL:
				synchronized (skillCancels) {
					skillCancels.remove(e.getDataId()).cancel(true);
				}
				break;
			case MOB_SKILL:
				synchronized (diseaseCancels) {
					diseaseCancels.remove(e.getDataId()).cancel(true);
				}
				break;
		}
	}

	public boolean isEffectActive(PlayerStatusEffect buff) {
		return activeEffects.containsKey(buff);
	}

	public boolean isSkillActive(int skillid) {
		return skillCancels.containsKey(Integer.valueOf(skillid));
	}

	public boolean isItemEffectActive(int itemid) {
		return itemEffectCancels.containsKey(Integer.valueOf(itemid));
	}

	public boolean isDebuffActive(int mobSkillId) {
		return diseaseCancels.containsKey(Integer.valueOf(mobSkillId));
	}

	public int getItemEffect() {
		return 0;
	}

	public int getChair() {
		return 0;
	}

	public boolean changeMap(int mapid) {
		return changeMap(mapid, (byte) 0);
	}

	public boolean changeMap(int mapid, byte initialPortal) {
		GameMap goTo = GameServer.getChannel(client.getChannel()).getMapFactory().getMap(mapid);
		if (goTo != null) {
			map.removePlayer(this);
			map = goTo;
			setPosition(map.getPortalPosition(initialPortal));
			client.getSession().send(CommonPackets.writeChangeMap(mapid, initialPortal, this));
			if (!isVisible())
				getClient().getSession().send(CommonPackets.writeShowHide());
			map.spawnPlayer(this);
			return true;
		}
		return false;
	}

	public void prepareChannelChange() {
		GameServer.getChannel(client.getChannel()).removePlayer(this);
		if (map != null)
			map.removePlayer(this);
		client.migrateHost();
	}

	public int getGuildId() {
		return guild;
	}

	public Party getParty() {
		return party;
	}

	public void close() {
		for (ScheduledFuture<?> cancelTask : skillCancels.values())
			cancelTask.cancel(true);
		for (ScheduledFuture<?> cancelTask : itemEffectCancels.values())
			cancelTask.cancel(true);
		for (ScheduledFuture<?> cancelTask : diseaseCancels.values())
			cancelTask.cancel(true);
		if (map != null)
			map.removePlayer(this);
		saveCharacter();
		for (Cooldown cooling : cooldowns.values())
			cooling.cancel();
		client = null;
	}

	public boolean canSeeEntity(MapEntity o) {
		visibleEntities.lockRead();
		try {
			return visibleEntities.contains(o);
		} finally {
			visibleEntities.unlockRead();
		}
	}

	public void addToVisibleMapEntities(MapEntity o) {
		visibleEntities.addWhenSafe(o);
	}

	public LockableList<MapEntity> getVisibleMapEntities() {
		return visibleEntities;
	}

	public void removeVisibleMapEntity(MapEntity o) {
		visibleEntities.removeWhenSafe(o);
	}

	public void clearVisibleEntities() {
		visibleEntities.lockWrite();
		try {
			visibleEntities.clear();
		} finally {
			visibleEntities.unlockWrite();
		}
	}

	public List<Mob> getControlledMobs() {
		return Collections.unmodifiableList(controllingMobs);
	}

	public void controlMonster(Mob m) {
		controllingMobs.add(m);
	}

	public void uncontrolMonster(Mob m) {
		controllingMobs.remove(m);
	}

	public void clearControlledMobs() {
		controllingMobs.clear();
	}

	public void checkMonsterAggro(Mob monster) {
		if (!monster.controllerHasAggro()) {
			Player controller = monster.getController();
			if (controller == this) {
				monster.setControllerHasAggro(true);
			} else {
				if (controller != null) {
					controller.uncontrolMonster(monster);
					controller.getClient().getSession().send(CommonPackets.writeStopControllingMonster(monster));
				}
				monster.setController(this);
				controlMonster(monster);
				getClient().getSession().send(CommonPackets.writeControlMonster(monster, true));
				monster.setControllerHasAggro(true);
				monster.setControllerKnowsAboutAggro(false);
			}
		}
	}

	public MapEntityType getEntityType() {
		return MapEntityType.PLAYER;
	}

	public boolean isAlive() {
		return remHp > 0;
	}

	public boolean isVisible() {
		return !isEffectActive(PlayerStatusEffect.HIDE);
	}

	public byte[] getCreationMessage() {
		return CommonPackets.writeShowPlayer(this);
	}

	public byte[] getShowEntityMessage() {
		return getCreationMessage();
	}

	public byte[] getOutOfViewMessage() {
		return null;
	}

	public byte[] getDestructionMessage() {
		return CommonPackets.writeRemovePlayer(this);
	}

	public boolean isNonRangedType() {
		return true;
	}

	public String toString() {
		return "[Player: " + getName() + ']';
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
		p.baseStr = str;
		p.baseDex = dex;
		p.baseInt = _int;
		p.baseLuk = luk;
		p.level = 1;
		p.gm = account.getGm();

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
				.getRight().get(0).shortValue(), (short) -5);
		InventoryTools.equip(equipment, equipped,
				InventoryTools.addToInventory(equipment, bottom, (short) 1)
				.getRight().get(0).shortValue(), (short) -6);
		InventoryTools.equip(equipment, equipped,
				InventoryTools.addToInventory(equipment, shoes, (short) 1)
				.getRight().get(0).shortValue(), (short) -7);
		InventoryTools.equip(equipment, equipped,
				InventoryTools .addToInventory(equipment, weapon, (short) 1)
				.getRight().get(0).shortValue(), (short) -11);
		InventoryTools.addToInventory(etc, 4161001, (short) 1);

		p.bindings.put(Byte.valueOf((byte) 2), new KeyBinding((byte) 4, 10));
		p.bindings.put(Byte.valueOf((byte) 3), new KeyBinding((byte) 4, 12));
		p.bindings.put(Byte.valueOf((byte) 4), new KeyBinding((byte) 4, 13));
		p.bindings.put(Byte.valueOf((byte) 5), new KeyBinding((byte) 4, 18));
		p.bindings.put(Byte.valueOf((byte) 6), new KeyBinding((byte) 4, 21));
		p.bindings.put(Byte.valueOf((byte) 16), new KeyBinding((byte) 4, 8));
		p.bindings.put(Byte.valueOf((byte) 17), new KeyBinding((byte) 4, 5));
		p.bindings.put(Byte.valueOf((byte) 18), new KeyBinding((byte) 4, 0));
		p.bindings.put(Byte.valueOf((byte) 19), new KeyBinding((byte) 4, 4));
		p.bindings.put(Byte.valueOf((byte) 23), new KeyBinding((byte) 4, 1));
		p.bindings.put(Byte.valueOf((byte) 25), new KeyBinding((byte) 4, 19));
		p.bindings.put(Byte.valueOf((byte) 26), new KeyBinding((byte) 4, 14));
		p.bindings.put(Byte.valueOf((byte) 27), new KeyBinding((byte) 4, 15));
		p.bindings.put(Byte.valueOf((byte) 29), new KeyBinding((byte) 5, 52));
		p.bindings.put(Byte.valueOf((byte) 31), new KeyBinding((byte) 4, 2));
		p.bindings.put(Byte.valueOf((byte) 34), new KeyBinding((byte) 4, 17));
		p.bindings.put(Byte.valueOf((byte) 35), new KeyBinding((byte) 4, 11));
		p.bindings.put(Byte.valueOf((byte) 37), new KeyBinding((byte) 4, 3));
		p.bindings.put(Byte.valueOf((byte) 38), new KeyBinding((byte) 4, 20));
		p.bindings.put(Byte.valueOf((byte) 40), new KeyBinding((byte) 4, 16));
		p.bindings.put(Byte.valueOf((byte) 43), new KeyBinding((byte) 4, 9));
		p.bindings.put(Byte.valueOf((byte) 44), new KeyBinding((byte) 5, 50));
		p.bindings.put(Byte.valueOf((byte) 45), new KeyBinding((byte) 5, 51));
		p.bindings.put(Byte.valueOf((byte) 46), new KeyBinding((byte) 4, 6));
		p.bindings.put(Byte.valueOf((byte) 50), new KeyBinding((byte) 4, 7));
		p.bindings.put(Byte.valueOf((byte) 56), new KeyBinding((byte) 5, 53));
		p.bindings.put(Byte.valueOf((byte) 59), new KeyBinding((byte) 6, 100));
		p.bindings.put(Byte.valueOf((byte) 60), new KeyBinding((byte) 6, 101));
		p.bindings.put(Byte.valueOf((byte) 61), new KeyBinding((byte) 6, 102));
		p.bindings.put(Byte.valueOf((byte) 62), new KeyBinding((byte) 6, 103));
		p.bindings.put(Byte.valueOf((byte) 63), new KeyBinding((byte) 6, 104));
		p.bindings.put(Byte.valueOf((byte) 64), new KeyBinding((byte) 6, 105));
		p.bindings.put(Byte.valueOf((byte) 65), new KeyBinding((byte) 6, 106));

		int prevTransactionIsolation = Connection.TRANSACTION_REPEATABLE_READ;
		boolean prevAutoCommit = true;
		Connection con = DatabaseConnection.getConnection();
		try {
			prevTransactionIsolation = con.getTransactionIsolation();
			prevAutoCommit = con.getAutoCommit();
			con.setTransactionIsolation(Connection.TRANSACTION_READ_UNCOMMITTED);
			con.setAutoCommit(false);
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
				p.setId(rs.getInt(1));
			rs.close();
			ps.close();

			p.updateDbInventory(con);
			p.updateDbBindings(con);
			con.commit();
		} catch (SQLException ex) {
			LOG.log(Level.WARNING, "Could not create new character " + name
					+ " on account" + account.getAccountId()
					+ ". Rolling back all changes...", ex);
			try {
				con.rollback();
			} catch (SQLException ex2) {
				LOG.log(Level.WARNING, "Error rolling back character.", ex2);
			}
		} finally {
			try {
				con.setAutoCommit(prevAutoCommit);
				con.setTransactionIsolation(prevTransactionIsolation);
			} catch (SQLException ex) {
				LOG.log(Level.WARNING, "Could not reset Connection config "
						+ "after creating character " + p.getId(), ex);
			}
		}

		return p;
	}

	public static String getNameFromId(int characterid) {
		String name = null;
		Connection con = DatabaseConnection.getConnection();
		try {
			PreparedStatement ps = con.prepareStatement("SELECT `name` FROM"
					+ "`characters` WHERE id = ?");
			ps.setInt(1, characterid);
			ResultSet rs = ps.executeQuery();
			if (rs.next())
				name = rs.getString(1);
			rs.close();
			ps.close();
		} catch (SQLException ex) {
			LOG.log(Level.WARNING, "Could not find name of character "
					+ characterid, ex);
		}
		return name;
	}
}
