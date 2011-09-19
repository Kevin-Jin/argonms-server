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

package argonms.game.character;

import argonms.common.character.BuddyList;
import argonms.common.GlobalConstants;
import argonms.common.character.BuddyListEntry;
import argonms.common.character.Cooldown;
import argonms.common.character.KeyBinding;
import argonms.common.character.Player.LoggedInPlayer;
import argonms.common.character.PlayerJob;
import argonms.common.character.PlayerStatusEffect;
import argonms.common.character.QuestEntry;
import argonms.common.character.SkillEntry;
import argonms.common.character.Skills;
import argonms.common.character.inventory.Equip;
import argonms.common.character.inventory.IInventory;
import argonms.common.character.inventory.Inventory;
import argonms.common.character.inventory.Inventory.InventoryType;
import argonms.common.character.inventory.InventorySlot;
import argonms.common.character.inventory.InventoryTools;
import argonms.common.character.inventory.Pet;
import argonms.common.character.inventory.TamingMob;
import argonms.common.loading.StatusEffectsData;
import argonms.common.net.external.CommonPackets;
import argonms.common.net.external.PacketSubHeaders;
import argonms.common.util.DatabaseManager;
import argonms.common.util.DatabaseManager.DatabaseType;
import argonms.common.util.Rng;
import argonms.common.util.collections.LockableList;
import argonms.common.util.collections.Pair;
import argonms.game.GameServer;
import argonms.game.character.BuffState.ItemState;
import argonms.game.character.BuffState.MobSkillState;
import argonms.game.character.BuffState.SkillState;
import argonms.game.character.inventory.StorageInventory;
import argonms.game.field.GameMap;
import argonms.game.field.MapEntity;
import argonms.game.field.entity.Minigame.MinigameResult;
import argonms.game.field.entity.Miniroom;
import argonms.game.field.entity.Miniroom.MiniroomType;
import argonms.game.field.entity.Mob;
import argonms.game.field.entity.Mob.MobDeathHook;
import argonms.game.field.entity.PlayerSkillSummon;
import argonms.game.loading.quest.QuestChecks;
import argonms.game.loading.quest.QuestChecks.QuestRequirementType;
import argonms.game.loading.quest.QuestDataLoader;
import argonms.game.loading.skill.SkillDataLoader;
import argonms.game.net.external.GameClient;
import argonms.game.net.external.GamePackets;
import argonms.game.net.external.handler.BuddyListHandler;
import java.lang.ref.WeakReference;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ScheduledFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author GoldenKevin
 */
public class GameCharacter extends MapEntity implements LoggedInPlayer {
	private static final Logger LOG = Logger.getLogger(GameCharacter.class.getName());

	private GameClient client;
	private byte gm;

	private String name;
	private short eyes, hair;
	private byte skin;
	private byte gender;
	private short level;
	private short job;
	private short baseStr, baseDex, baseInt, baseLuk, baseMaxHp, baseMaxMp;
	private short remHp, remMp, remAp, remSp, maxHp, maxMp;
	private int addStr, addDex, addInt, addLuk, addMaxHp, addMaxMp,
			addWatk, addWdef, addMatk, addMdef, addAcc, addAvo, addHands, addSpeed, addJump;
	private int exp;
	private short fame;

	private final LockableList<Mob> controllingMobs;
	private GameMap map;
	private int savedMapId;
	private byte savedSpawnPoint;

	private int partner;
	private BuddyList buddies;
	private int guild;
	private Party party;

	private int mesos;
	private final Map<InventoryType, Inventory> inventories;
	private StorageInventory storage;
	private final Pet[] equippedPets;

	private final Map<Byte, KeyBinding> bindings;
	private final List<SkillMacro> skillMacros;
	private final Map<Integer, SkillEntry> skillEntries;
	private final Map<Integer, Cooldown> cooldowns;
	private final Map<PlayerStatusEffect, PlayerStatusEffectValues> activeEffects;
	private final Map<Integer, Pair<SkillState, ScheduledFuture<?>>> skillFutures;
	private final Map<Integer, Pair<ItemState, ScheduledFuture<?>>> itemEffectFutures;
	private final Map<Short, Pair<MobSkillState, ScheduledFuture<?>>> diseaseFutures;
	private final Map<Integer, PlayerSkillSummon> summons;
	private short energyCharge;

	private final Map<Short, QuestEntry> questStatuses;
	private final Map<QuestRequirementType, Map<Number, List<Short>>> questReqWatching;

	private Miniroom miniroom;
	private final Map<MiniroomType, Map<MinigameResult, Integer>> minigameStats;

	private GameCharacter () {
		inventories = new EnumMap<InventoryType, Inventory>(InventoryType.class);
		equippedPets = new Pet[3];
		bindings = new TreeMap<Byte, KeyBinding>();
		skillMacros = new ArrayList<SkillMacro>(5);
		skillEntries = new HashMap<Integer, SkillEntry>();
		cooldowns = new HashMap<Integer, Cooldown>();
		activeEffects = new EnumMap<PlayerStatusEffect, PlayerStatusEffectValues>(PlayerStatusEffect.class);
		skillFutures = new HashMap<Integer, Pair<SkillState, ScheduledFuture<?>>>();
		itemEffectFutures = new HashMap<Integer, Pair<ItemState, ScheduledFuture<?>>>();
		diseaseFutures = new HashMap<Short, Pair<MobSkillState, ScheduledFuture<?>>>();
		summons = new HashMap<Integer, PlayerSkillSummon>();
		controllingMobs = new LockableList<Mob>(new ArrayList<Mob>());
		questStatuses = new HashMap<Short, QuestEntry>();
		questReqWatching = new EnumMap<QuestRequirementType, Map<Number, List<Short>>>(QuestRequirementType.class);
		minigameStats = new EnumMap<MiniroomType, Map<MinigameResult, Integer>>(MiniroomType.class);
	}

	@Override
	public int getDataId() {
		return getId();
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
			updateDbSkills(con);
			updateDbCooldowns(con);
			updateDbBindings(con);
			updateDbBuddies(con);
			updateDbQuests(con);
			updateDbMinigameStats(con);
			con.commit();
		} catch (SQLException ex) {
			LOG.log(Level.WARNING, "Could not save character " + getDataId()
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
						+ "after saving character " + getDataId(), ex);
			}
			DatabaseManager.cleanup(DatabaseType.STATE, null, null, con);
		}
	}

	private void updateDbAccount(Connection con) throws SQLException {
		PreparedStatement ps = null;
		try {
			ps = con.prepareStatement("UPDATE `accounts` "
					+ "SET `storageslots` = ?, `storagemesos` = ? "
					+ "WHERE `id` = ?");
			ps.setShort(1, storage.getMaxSlots());
			ps.setInt(2, storage.getMesos());
			ps.setInt(3, client.getAccountId());
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
			ps = con.prepareStatement("UPDATE `characters` "
					+ "SET `accountid` = ?, `world` = ?, `name` = ?, "
					+ "`gender` = ?, `skin` = ?, `eyes` = ?, `hair` = ?, "
					+ "`level` = ?, `job` = ?, `str` = ?, `dex` = ?, "
					+ "`int` = ?, `luk` = ?, `hp` = ?, `maxhp` = ?, `mp` = ?, "
					+ "`maxmp` = ?, `ap` = ?, `sp` = ?, `exp` = ?, `fame` = ?, "
					+ "`spouse` = ?, `map` = ?, `spawnpoint` = ?, `mesos` = ?, "
					+ "`equipslots` = ?, `useslots` = ?, `setupslots` = ?, "
					+ "`etcslots` = ?, `cashslots` = ?, "
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
			ps.setShort(31, buddies.getCapacity());
			ps.setByte(32, gm);
			ps.setInt(33, getDataId());
			int updateRows = ps.executeUpdate();
			if (updateRows < 1)
				LOG.log(Level.WARNING, "Updating a deleted character with name "
						+ "{0} of account {1}.", new Object[] { name,
						client.getAccountId() });
		} catch (SQLException e) {
			throw new SQLException("Failed to save stats of character " + name, e);
		} finally {
			DatabaseManager.cleanup(DatabaseType.STATE, null, ps, null);
		}
	}

	private void updateDbInventory(Connection con) throws SQLException {
		String invUpdate = "DELETE FROM `inventoryitems` WHERE "
				+ "`characterid` = ? AND `inventorytype` <= "
				+ InventoryType.CASH.byteValue() + " OR `accountid` = ? AND "
				+ "`inventorytype` = " + InventoryType.STORAGE.byteValue();
		PreparedStatement ps = null, ips = null;
		ResultSet rs = null;
		try {
			ps = con.prepareStatement(invUpdate);
			ps.setInt(1, getDataId());
			ps.setInt(2, client.getAccountId());
			ps.executeUpdate();
			ps.close();

			EnumMap<InventoryType, IInventory> union = new EnumMap<InventoryType, IInventory>(inventories);
			union.put(InventoryType.STORAGE, storage);
			CharacterTools.commitInventory(con, getDataId(), client.getAccountId(), equippedPets, union);
		} catch (SQLException e) {
			throw new SQLException("Failed to save inventory of character " + name, e);
		} finally {
			DatabaseManager.cleanup(DatabaseType.STATE, null, ips, null);
			DatabaseManager.cleanup(DatabaseType.STATE, rs, ps, null);
		}
	}

	private void updateDbSkills(Connection con) throws SQLException {
		PreparedStatement ps = null;
		try {
			ps = con.prepareStatement("DELETE FROM `skills` "
					+ "WHERE `characterid` = ?");
			ps.setInt(1, getDataId());
			ps.executeUpdate();
			ps.close();

			ps = con.prepareStatement("INSERT INTO `skills` (`characterid`," +
					"`skillid`,`level`,`mastery`) VALUES (?,?,?,?)");
			ps.setInt(1, getDataId());
			for (Entry<Integer, SkillEntry> skill : skillEntries.entrySet()) {
				SkillEntry skillLevel = skill.getValue();
				ps.setInt(2, skill.getKey().intValue());
				ps.setByte(3, skillLevel.getLevel());
				ps.setByte(4, skillLevel.getMasterLevel());
				ps.addBatch();
			}
			ps.executeBatch();
		} catch (SQLException e) {
			throw new SQLException("Failed to save skill levels of character " + name, e);
		} finally {
			DatabaseManager.cleanup(DatabaseType.STATE, null, ps, null);
		}
	}

	private void updateDbCooldowns(Connection con) throws SQLException {
		PreparedStatement ps = null;
		try {
			ps = con.prepareStatement("DELETE FROM `cooldowns` "
					+ "WHERE `characterid` = ?");
			ps.setInt(1, getDataId());
			ps.executeUpdate();
			ps.close();

			ps = con.prepareStatement("INSERT INTO `cooldowns`" +
					"(`characterid`,`skillid`,`remaining`) VALUES (?,?,?)");
			ps.setInt(1, getDataId());
			for (Entry<Integer, Cooldown> cooling : cooldowns.entrySet()) {
				ps.setInt(2, cooling.getKey().intValue());
				ps.setShort(3, cooling.getValue().getSecondsRemaining());
				ps.addBatch();
			}
			ps.executeBatch();
		} catch (SQLException e) {
			throw new SQLException("Failed to save cooldowns of character " + name, e);
		} finally {
			DatabaseManager.cleanup(DatabaseType.STATE, null, ps, null);
		}
	}

	private void updateDbBindings(Connection con) throws SQLException {
		PreparedStatement ps = null;
		try {
			ps = con.prepareStatement("DELETE FROM `keymaps` "
					+ "WHERE `characterid` = ?");
			ps.setInt(1, getDataId());
			ps.executeUpdate();
			ps.close();

			ps = con.prepareStatement("INSERT INTO `keymaps` (`characterid`,"
					+ "`key`,`type`,`action`) VALUES (?,?,?,?)");
			ps.setInt(1, getDataId());
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
			ps.setInt(1, getDataId());
			ps.executeUpdate();
			ps.close();

			byte pos = 0;
			ps = con.prepareStatement("INSERT INTO `skillmacros` "
					+ "(`characterid`,`position`,`name`,`shout`,`skill1`,"
					+ "`skill2`,`skill3`) VALUES (?,?,?,?,?,?,?)");
			ps.setInt(1, getDataId());
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
		} catch (SQLException e) {
			throw new SQLException("Failed to save keymap/macros of character " + name, e);
		} finally {
			DatabaseManager.cleanup(DatabaseType.STATE, null, ps, null);
		}
	}

	private void updateDbBuddies(Connection con) throws SQLException {
		PreparedStatement ps = null;
		try {
			ps = con.prepareStatement("DELETE FROM `buddyentries` WHERE "
					+ "`owner` = ?");
			ps.setInt(1, getDataId());
			ps.executeUpdate();
			ps.close();

			ps = con.prepareStatement("INSERT INTO `buddyentries` (`owner`,"
					+ "`buddy`,`buddyname`,`status`) VALUES (?,?,?,?)");
			ps.setInt(1, getDataId());
			for (BuddyListEntry buddy : buddies.getBuddies()) {
				ps.setInt(2, buddy.getId());
				ps.setString(3, buddy.getName());
				ps.setByte(4, buddy.getStatus());
				ps.addBatch();
			}
			ps.setByte(4, BuddyListHandler.STATUS_INVITED);
			for (Entry<Integer, String> invite : buddies.getInvites()) {
				ps.setInt(2, invite.getKey().intValue());
				ps.setString(3, invite.getValue());
				ps.addBatch();
			}
			ps.executeBatch();
		} finally {
			DatabaseManager.cleanup(DatabaseType.STATE, null, ps, null);
		}
	}

	private void updateDbQuests(Connection con) throws SQLException {
		PreparedStatement ps = null, mps = null;
		try {
			ps = con.prepareStatement("DELETE FROM "
					+ "`queststatuses` WHERE `characterid` = ?");
			ResultSet rs;
			ps.setInt(1, getDataId());
			ps.executeUpdate();
			ps.close();

			ps = con.prepareStatement("INSERT INTO `queststatuses` "
					+ "(`characterid`,`questid`,`state`,`completed`) VALUES "
					+ "(?,?,?, ?)",
					Statement.RETURN_GENERATED_KEYS);
			ps.setInt(1, getDataId());
			for (Entry<Short, QuestEntry> entry : questStatuses.entrySet()) {
				QuestEntry status = entry.getValue();
				ps.setShort(2, entry.getKey().shortValue());
				ps.setByte(3, status.getState());
				ps.setLong(4, status.getCompletionTime());
				ps.executeUpdate();
				rs = ps.getGeneratedKeys();
				int questEntryId = rs.next() ? rs.getInt(1) : -1;
				rs.close();
				mps = con.prepareStatement("INSERT INTO `questmobprogress` "
						+ "(`queststatusid`,`mobid`,`count`) VALUES (?,?,?)");
				mps.setInt(1, questEntryId);
				for (Entry<Integer, Short> mobProgress : status.getAllMobCounts().entrySet()) {
					mps.setInt(2, mobProgress.getKey().intValue());
					mps.setShort(3, mobProgress.getValue().shortValue());
					mps.addBatch();
				}
				mps.executeBatch();
				mps.close();
			}
		} catch (SQLException e) {
			throw new SQLException("Failed to save quest states of character " + name, e);
		} finally {
			DatabaseManager.cleanup(DatabaseType.STATE, null, mps, null);
			DatabaseManager.cleanup(DatabaseType.STATE, null, ps, null);
		}
	}

	private void updateDbMinigameStats(Connection con) throws SQLException {
		PreparedStatement ps = null;
		try {
			ps = con.prepareStatement("DELETE FROM "
					+ "`minigamescores` WHERE `characterid` = ?");
			ps.setInt(1, getDataId());
			ps.executeUpdate();
			ps.close();

			ps = con.prepareStatement("INSERT INTO `minigamescores` "
					+ "(`characterid`,`gametype`,`wins`,`ties`,`losses`) "
					+ "VALUES (?, ?, ?, ?, ?)");
			ps.setInt(1, getDataId());
			for (Entry<MiniroomType, Map<MinigameResult, Integer>> stats : minigameStats.entrySet()) {
				ps.setByte(2, stats.getKey().byteValue());
				ps.setInt(3, stats.getValue().get(MinigameResult.WIN).intValue());
				ps.setInt(4, stats.getValue().get(MinigameResult.TIE).intValue());
				ps.setInt(5, stats.getValue().get(MinigameResult.LOSS).intValue());
				ps.addBatch();
			}
			ps.executeBatch();
		} catch (SQLException e) {
			throw new SQLException("Failed to save minigame stats of character " + name, e);
		} finally {
			DatabaseManager.cleanup(DatabaseType.STATE, null, ps, null);
		}
	}

	public static GameCharacter loadPlayer(GameClient c, int id) {
		Connection con = null;
		PreparedStatement ps = null, ips = null;
		ResultSet rs = null, irs = null;
		try {
			con = DatabaseManager.getConnection(DatabaseType.STATE);
			ps = con.prepareStatement("SELECT `c`.*,`a`.`name`,"
					+ "`a`.`storageslots`,`a`.`storagemesos` FROM `characters` `c` "
					+ "LEFT JOIN `accounts` `a` ON `c`.`accountid` = `a`.`id` "
					+ "WHERE `c`.`id` = ?");
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
			if (world != c.getWorld()) { //we are aware of our world
				LOG.log(Level.WARNING, "Client account {0} is trying to load "
						+ "character {1} on world {2} but exists on world {3}",
						new Object[] { accountid, id, c.getWorld(), world });
				return null;
			}
			GameCharacter p = new GameCharacter();
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
			p.map = GameServer.getChannel(c.getChannel()).getMapFactory().getMap(p.savedMapId);
			int forcedReturn = p.map.getForcedReturnMap();
			if (forcedReturn != GlobalConstants.NULL_MAP) {
				p.map = GameServer.getChannel(p.getClient().getChannel()).getMapFactory().getMap(forcedReturn);
				p.savedSpawnPoint = 0;
			}
			p.setPosition(p.map.getPortalPosition(p.savedSpawnPoint));

			p.maxHp = p.baseMaxHp;
			p.remHp = (short) Math.min(rs.getShort(15), p.maxHp);
			p.maxMp = p.baseMaxMp;
			p.remMp = (short) Math.min(rs.getShort(17), p.maxMp);

			p.mesos = rs.getInt(26);
			p.inventories.put(InventoryType.EQUIP, new Inventory(rs.getShort(27)));
			p.inventories.put(InventoryType.USE, new Inventory(rs.getShort(28)));
			p.inventories.put(InventoryType.SETUP, new Inventory(rs.getShort(29)));
			p.inventories.put(InventoryType.ETC, new Inventory(rs.getShort(30)));
			p.inventories.put(InventoryType.CASH, new Inventory(rs.getShort(31)));
			//TODO: get real equipped inventory size?
			p.inventories.put(InventoryType.EQUIPPED, new Inventory((short) 0));
			p.buddies = new BuddyList(rs.getShort(32));
			p.gm = rs.getByte(33);
			c.setAccountName(rs.getString(42));
			p.storage = new StorageInventory(rs.getShort(43), rs.getInt(44));
			rs.close();
			ps.close();

			EnumMap<InventoryType, IInventory> invUnion = new EnumMap<InventoryType, IInventory>(p.inventories);
			invUnion.put(InventoryType.STORAGE, p.storage);
			ps = con.prepareStatement("SELECT * FROM `inventoryitems` WHERE `characterid` = ?"
					+ " AND `inventorytype` <= " + InventoryType.CASH.byteValue()
					+ " OR `accountid` = ? AND `inventorytype` = " + InventoryType.STORAGE.byteValue());
			ps.setInt(1, id);
			ps.setInt(2, accountid);
			rs = ps.executeQuery();
			CharacterTools.loadInventory(con, rs, p.equippedPets, invUnion);
			rs.close();
			ps.close();
			for (InventorySlot equip : p.inventories.get(InventoryType.EQUIPPED).getAll().values())
				p.equipChanged((Equip) equip, true);

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

			ps = con.prepareStatement("SELECT `e`.`buddy` AS `id`, "
					+ "IF(ISNULL(`c`.`name`), `e`.`buddyname`, `c`.`name`) AS "
					+ "`name`, `e`.`status` FROM `buddyentries` `e` LEFT JOIN "
					+ "`characters` `c` ON `c`.`id` = `e`.`buddy` WHERE "
					+ "`owner` = ?");
			ps.setInt(1, id);
			rs = ps.executeQuery();
			while (rs.next()) {
				byte status = rs.getByte(3);
				if (status != BuddyListHandler.STATUS_INVITED)
					p.buddies.addBuddy(new BuddyListEntry(rs.getInt(1),
							rs.getString(2), status));
				else
					p.buddies.addInvite(rs.getInt(1), rs.getString(2));
			}
			rs.close();
			ps.close();

			ps = con.prepareStatement("SELECT `id`,`questid`,`state`,"
					+ "`completed` FROM `queststatuses` WHERE "
					+ "`characterid` = ?");
			ps.setInt(1, id);
			rs = ps.executeQuery();
			PreparedStatement mps = null;
			ResultSet mrs = null;
			while (rs.next()) {
				int questEntryId = rs.getInt(1);
				short questId = rs.getShort(2);
				Map<Integer, Short> mobProgress = new HashMap<Integer, Short>();
				try {
					mps = con.prepareStatement("SELECT "
							+ "`mobid`,`count` FROM `questmobprogress` WHERE "
							+ "`queststatusid` = ?");
					mps.setInt(1, questEntryId);
					mrs = mps.executeQuery();
					while (mrs.next())
						mobProgress.put(Integer.valueOf(mrs.getInt(1)), Short.valueOf(mrs.getShort(2)));
					mrs.close();
					mps.close();
					QuestEntry status = new QuestEntry(rs.getByte(3), mobProgress);
					status.setCompletionTime(rs.getLong(4));
					p.questStatuses.put(Short.valueOf(questId), status);
					if (status.getState() == QuestEntry.STATE_STARTED) {
						QuestChecks qc = QuestDataLoader.getInstance().getCompleteReqs(questId);
						if (qc != null) {
							for (Entry<Integer, Short> mob : qc.getReqMobCounts().entrySet())
								if (status.getMobCount(mob.getKey().intValue()) < mob.getValue().shortValue())
									p.addToWatchedList(questId, QuestRequirementType.MOB, mob.getKey());
							for (Integer itemId : qc.getReqItems().keySet())
								p.addToWatchedList(questId, QuestRequirementType.ITEM, itemId);
							for (Integer petId : qc.getReqPets())
								p.addToWatchedList(questId, QuestRequirementType.PET, petId);
							for (Short reqQuestId : qc.getReqQuests().keySet())
								p.addToWatchedList(questId, QuestRequirementType.QUEST, reqQuestId);
							if (qc.requiresMesos())
								p.addToWatchedList(questId, QuestRequirementType.MESOS);
						}
					}
				} finally {
					DatabaseManager.cleanup(DatabaseType.STATE, mrs, mps, null);
				}
			}
			rs.close();
			ps.close();

			ps = con.prepareStatement("SELECT * FROM `minigamescores` "
					+ "WHERE `characterid` = ?");
			ps.setInt(1, id);
			rs = ps.executeQuery();
			while (rs.next()) {
				Map<MinigameResult, Integer> stats = new EnumMap<MinigameResult, Integer>(MinigameResult.class);
				stats.put(MinigameResult.WIN, rs.getInt(4));
				stats.put(MinigameResult.TIE, rs.getInt(5));
				stats.put(MinigameResult.LOSS, rs.getInt(6));
				p.minigameStats.put(MiniroomType.valueOf(rs.getByte(3)), stats);
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

	@Override
	public GameClient getClient() {
		return client;
	}

	@Override
	public byte getPrivilegeLevel() {
		return gm;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public byte getGender() {
		return gender;
	}

	@Override
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
		getClient().getSession().send(GamePackets.writeUpdatePlayerStats(Collections.singletonMap(ClientUpdateKey.EXP, Integer.valueOf(exp)), false));
	}

	public void gainExp(int gain, boolean isKiller, boolean fromQuest) {
		if (level < GlobalConstants.MAX_LEVEL) {
			getClient().getSession().send(GamePackets.writeShowExpGain(gain, isKiller, fromQuest));

			Map<ClientUpdateKey, Number> updatedStats = new EnumMap<ClientUpdateKey, Number>(ClientUpdateKey.class);
			long newExp = (long) exp + gain; //should solve many overflow errors
			if (newExp >= ExpTables.getForLevel(level))
				newExp = levelUp(newExp, updatedStats);
			updatedStats.put(ClientUpdateKey.EXP, Integer.valueOf(exp = (int) newExp));
			getClient().getSession().send(GamePackets.writeUpdatePlayerStats(updatedStats, false));
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
		} while (level < GlobalConstants.MAX_LEVEL && exp >= ExpTables.getForLevel(level));

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

		getMap().sendToAll(GamePackets.writeShowLevelUp(this), this);

		return level < GlobalConstants.MAX_LEVEL ? exp : 0;
	}

	public short getLevel() {
		return level;
	}

	public void setLevel(short newLevel) {
		this.level = newLevel;
		getClient().getSession().send(GamePackets.writeUpdatePlayerStats(Collections.singletonMap(ClientUpdateKey.LEVEL, Short.valueOf(level)), false));
	}

	public short getJob() {
		return job;
	}

	public void setJob(short newJob) {
		this.job = newJob;
		getMap().sendToAll(GamePackets.writeShowJobChange(this));
		getClient().getSession().send(GamePackets.writeUpdatePlayerStats(Collections.singletonMap(ClientUpdateKey.JOB, Short.valueOf(job)), false));
	}

	public short getStr() {
		return baseStr;
	}

	public int getCurrentStr() {
		return baseStr + addStr;
	}

	public short incrementLocalStr() {
		return ++this.baseStr;
	}

	public void setStr(short newStr) {
		this.baseStr = newStr;
		getClient().getSession().send(GamePackets.writeUpdatePlayerStats(Collections.singletonMap(ClientUpdateKey.STR, Short.valueOf(baseStr)), false));
	}

	public short getDex() {
		return baseDex;
	}

	public int getCurrentDex() {
		return baseDex + addDex;
	}

	public short incrementLocalDex() {
		return ++this.baseDex;
	}

	public void setDex(short newDex) {
		this.baseDex = newDex;
		getClient().getSession().send(GamePackets.writeUpdatePlayerStats(Collections.singletonMap(ClientUpdateKey.DEX, Short.valueOf(baseDex)), false));
	}

	public short getInt() {
		return baseInt;
	}

	public int getCurrentInt() {
		return baseInt + addInt;
	}

	public short incrementLocalInt() {
		return ++this.baseInt;
	}

	public void setInt(short newInt) {
		this.baseInt = newInt;
		getClient().getSession().send(GamePackets.writeUpdatePlayerStats(Collections.singletonMap(ClientUpdateKey.INT, Short.valueOf(baseInt)), false));
	}

	public short getLuk() {
		return baseLuk;
	}

	public int getCurrentLuk() {
		return baseLuk + addLuk;
	}

	public short incrementLocalLuk() {
		return ++this.baseLuk;
	}

	public void setLuk(short newLuk) {
		this.baseLuk = newLuk;
		getClient().getSession().send(GamePackets.writeUpdatePlayerStats(Collections.singletonMap(ClientUpdateKey.LUK, Short.valueOf(baseLuk)), false));
	}

	public short getHp() {
		return remHp;
	}

	public void setLocalHp(short newHp) {
		if (newHp < 0)
			newHp = 0;
		else if (newHp > maxHp)
			newHp = maxHp;
		this.remHp = newHp;
	}

	public void setHp(short newHp) {
		//TODO: send new hp to party mates (I think v0.62 supports it...)
		setLocalHp(newHp);
		getClient().getSession().send(GamePackets.writeUpdatePlayerStats(Collections.singletonMap(ClientUpdateKey.HP, Short.valueOf(remHp)), false));
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
		getClient().getSession().send(GamePackets.writeEnableActions());
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
		getClient().getSession().send(GamePackets.writeUpdatePlayerStats(Collections.singletonMap(ClientUpdateKey.MAXHP, Short.valueOf(baseMaxHp)), false));
	}

	public short incrementMaxHp(int gain) {
		updateMaxHp((short) Math.min(baseMaxHp + gain, 30000));
		return baseMaxHp;
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
		if (!getInventory(InventoryType.EQUIPPED).hasItem(protectItem, 1)) {
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

	public void setLocalMp(short newMp) {
		if (newMp < 0)
			newMp = 0;
		else if (newMp > maxMp)
			newMp = maxMp;
		this.remMp = newMp;
	}

	public void setMp(short newMp) {
		setLocalMp(newMp);
		getClient().getSession().send(GamePackets.writeUpdatePlayerStats(Collections.singletonMap(ClientUpdateKey.MP, Short.valueOf(remMp)), false));
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
		getClient().getSession().send(GamePackets.writeUpdatePlayerStats(Collections.singletonMap(ClientUpdateKey.MAXMP, Short.valueOf(baseMaxMp)), false));
	}

	public short incrementMaxMp(int gain) {
		updateMaxMp((short) Math.min(baseMaxMp + gain, 30000));
		return baseMaxMp;
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

	public short decrementLocalAp() {
		return --this.remAp;
	}

	public void setAp(short newAp) {
		this.remAp = newAp;
		getClient().getSession().send(GamePackets.writeUpdatePlayerStats(Collections.singletonMap(ClientUpdateKey.AVAILABLEAP, Short.valueOf(remAp)), false));
	}

	public short getSp() {
		return remSp;
	}

	public void setSp(short newSp) {
		this.remSp = newSp;
		getClient().getSession().send(GamePackets.writeUpdatePlayerStats(Collections.singletonMap(ClientUpdateKey.AVAILABLESP, Short.valueOf(remSp)), false));
	}

	public short getFame() {
		return fame;
	}

	public void setFame(short newFame) {
		this.fame = newFame;
		getClient().getSession().send(GamePackets.writeUpdatePlayerStats(Collections.singletonMap(ClientUpdateKey.FAME, Integer.valueOf(fame)), false));
	}

	public void gainFame(int gain, boolean fromQuest) {
		setFame((short) Math.min(fame + gain, Short.MAX_VALUE));
		if (fromQuest)
			GamePackets.writeShowPointsGainFromQuest(gain, PacketSubHeaders.STATUS_INFO_FAME);
	}

	public Inventory getInventory(InventoryType type) {
		return inventories.get(type);
	}

	public StorageInventory getStorageInventory() {
		return storage;
	}

	public int getMesos() {
		return mesos;
	}

	public void setLocalMesos(int newValue) {
		this.mesos = newValue;
	}

	public void setMesos(int newValue, boolean fromDrop) {
		this.mesos = newValue;
		getClient().getSession().send(GamePackets.writeUpdatePlayerStats(Collections.singletonMap(ClientUpdateKey.MESO, Integer.valueOf(mesos)), fromDrop));

		//quests with meso requirements
		mesosChanged();
	}

	public void setMesos(int newValue) {
		setMesos(newValue, false);
	}

	public boolean gainMesos(int gain, boolean fromQuest, boolean fromDrop) {
		long newValue = (long) mesos + gain;
		if (newValue <= Integer.MAX_VALUE && newValue >= 0) {
			setMesos((int) newValue, fromDrop);
			if (!fromQuest) {
				if (gain > 0) //don't show when we're dropping mesos, only show when we're picking up
					getClient().getSession().send(GamePackets.writeShowMesoGain(gain));
			} else {
				getClient().getSession().send(GamePackets.writeShowPointsGainFromQuest(gain, (byte) 5));
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
		return (map != null ? map.getDataId() : savedMapId);
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

	public byte getBuddyListCapacity() {
		return (byte) buddies.getCapacity();
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

	public TamingMob getEquippedMount() {
		return (TamingMob) getInventory(InventoryType.EQUIPPED).get((short) -18);
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

	public Map<Integer, SkillEntry> getSkillEntries() {
		return Collections.unmodifiableMap(skillEntries);
	}

	public byte getSkillLevel(int skill) {
		SkillEntry skillLevel = skillEntries.get(Integer.valueOf(skill));
		return skillLevel != null ? skillLevel.getLevel() : 0;
	}

	public byte getMasterSkillLevel(int skill) {
		if (!Skills.isFourthJob(skill))
			return SkillDataLoader.getInstance().getSkill(skill).maxLevel();
		SkillEntry skillLevel = skillEntries.get(Integer.valueOf(skill));
		return skillLevel != null ? skillLevel.getMasterLevel() : 0;
	}

	/**
	 *
	 * @param skill
	 * @param level
	 * @param masterLevel set to -1 if you do not wish to change the max level
	 */
	public void setSkillLevel(int skill, byte level, byte masterLevel) {
		if (level == 0)
			skillEntries.remove(Integer.valueOf(skill));
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
		getClient().getSession().send(GamePackets.writeUpdateSkillLevel(
				skill, skillLevel.getLevel(), skillLevel.getMasterLevel()));
	}

	public void addCooldown(final int skill, short time) {
		cooldowns.put(Integer.valueOf(skill), new Cooldown(time * 1000, new Runnable() {
			public void run() {
				removeCooldown(skill);
				getClient().getSession().send(CommonPackets.writeCooldown(skill, (short) 0));
			}
		}));
	}

	public void removeCooldown(int skill) {
		cooldowns.remove(Integer.valueOf(skill)).cancel();
	}

	public Map<Integer, Cooldown> getCooldowns() {
		return cooldowns;
	}

	public void addToActiveEffects(PlayerStatusEffect buff, PlayerStatusEffectValues value) {
		synchronized (activeEffects) {
			activeEffects.put(buff, value);
		}
	}

	public void addCancelEffectTask(StatusEffectsData e, ScheduledFuture<?> cancelTask, byte level, long endTime) {
		switch (e.getSourceType()) {
			case ITEM:
				synchronized (itemEffectFutures) {
					itemEffectFutures.put(Integer.valueOf(e.getDataId()), new Pair<ItemState, ScheduledFuture<?>>(new ItemState(endTime), cancelTask));
				}
				break;
			case PLAYER_SKILL:
				synchronized (skillFutures) {
					skillFutures.put(Integer.valueOf(e.getDataId()), new Pair<SkillState, ScheduledFuture<?>>(new SkillState(level, endTime), cancelTask));
				}
				break;
			case MOB_SKILL:
				synchronized (diseaseFutures) {
					diseaseFutures.put(Short.valueOf((short) e.getDataId()), new Pair<MobSkillState, ScheduledFuture<?>>(new MobSkillState(level, endTime), cancelTask));
				}
				break;
		}
	}

	public Map<PlayerStatusEffect, PlayerStatusEffectValues> getAllEffects() {
		return activeEffects;
	}

	public PlayerStatusEffectValues getEffectValue(PlayerStatusEffect buff) {
		return activeEffects.get(buff);
	}

	public PlayerStatusEffectValues removeFromActiveEffects(PlayerStatusEffect e) {
		synchronized (activeEffects) {
			return activeEffects.remove(e);
		}
	}

	public void removeCancelEffectTask(StatusEffectsData e) {
		Pair<? extends BuffState, ScheduledFuture<?>> cancelTask;
		switch (e.getSourceType()) {
			case ITEM:
				synchronized (itemEffectFutures) {
					cancelTask = itemEffectFutures.remove(Integer.valueOf(e.getDataId()));
				}
				break;
			case PLAYER_SKILL:
				synchronized (skillFutures) {
					cancelTask = skillFutures.remove(Integer.valueOf(e.getDataId()));
				}
				break;
			case MOB_SKILL:
				synchronized (diseaseFutures) {
					cancelTask = diseaseFutures.remove(Short.valueOf((short) e.getDataId()));
				}
				break;
			default:
				cancelTask = null;
				break;
		}
		if (cancelTask != null)
			cancelTask.right.cancel(false);
	}

	public boolean isEffectActive(PlayerStatusEffect buff) {
		return activeEffects.containsKey(buff);
	}

	public boolean isSkillActive(int skillid) {
		return skillFutures.containsKey(Integer.valueOf(skillid));
	}

	public boolean isItemEffectActive(int itemid) {
		return itemEffectFutures.containsKey(Integer.valueOf(itemid));
	}

	public boolean isDebuffActive(short mobSkillId) {
		return diseaseFutures.containsKey(Short.valueOf(mobSkillId));
	}

	public boolean areEffectsActive(StatusEffectsData e) {
		switch (e.getSourceType()) {
			case PLAYER_SKILL:
				return skillFutures.containsKey(Integer.valueOf(e.getDataId()));
			case ITEM:
				return itemEffectFutures.containsKey(Integer.valueOf(e.getDataId()));
			case MOB_SKILL:
				return diseaseFutures.containsKey(Short.valueOf((short) e.getDataId()));
			default:
				return false;
		}
	}

	public Map<Integer, SkillState> activeSkillsList() {
		Map<Integer, SkillState> list = new HashMap<Integer, SkillState>();
		for (Entry<Integer, Pair<SkillState, ScheduledFuture<?>>> activeSkill : skillFutures.entrySet())
			list.put(activeSkill.getKey(), activeSkill.getValue().left);
		return list;
	}

	public Map<Integer, ItemState> activeItemsList() {
		Map<Integer, ItemState> list = new HashMap<Integer, ItemState>();
		for (Entry<Integer, Pair<ItemState, ScheduledFuture<?>>> activeItem : itemEffectFutures.entrySet())
			list.put(activeItem.getKey(), activeItem.getValue().left);
		return list;
	}

	public Map<Short, MobSkillState> activeMobSkillsList() {
		Map<Short, MobSkillState> list = new HashMap<Short, MobSkillState>();
		for (Entry<Short, Pair<MobSkillState, ScheduledFuture<?>>> activeMobSkill : diseaseFutures.entrySet())
			list.put(activeMobSkill.getKey(), activeMobSkill.getValue().left);
		return list;
	}

	public void addToEnergyCharge(int gain) {
		energyCharge = (short) Math.min(energyCharge + gain, 10000);
	}

	public void resetEnergyCharge() {
		energyCharge = 0;
	}

	public short getEnergyCharge() {
		return energyCharge;
	}

	public Collection<PlayerSkillSummon> summonsList() {
		return summons.values();
	}

	public Map<Integer, PlayerSkillSummon> getAllSummons() {
		return summons;
	}

	public void addToSummons(int skillId, PlayerSkillSummon summon) {
		summons.put(Integer.valueOf(skillId), summon);
	}

	public PlayerSkillSummon getSummonBySkill(int skillId) {
		return summons.get(Integer.valueOf(skillId));
	}

	public void removeFromSummons(int skillId) {
		summons.remove(Integer.valueOf(skillId));
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

	public void leaveMapRoutines() {
		if (miniroom != null) {
			miniroom.leaveRoom(this);
			miniroom = null;
		}
		PlayerStatusEffectValues v = getEffectValue(PlayerStatusEffect.PUPPET);
		if (v != null)
			SkillTools.cancelBuffSkill(this, v.getSource());
	}

	public boolean changeMap(int mapid, byte initialPortal) {
		leaveMapRoutines();
		GameMap goTo = GameServer.getChannel(client.getChannel()).getMapFactory().getMap(mapid);
		if (goTo != null) {
			map.removePlayer(this);
			map = goTo;
			setPosition(map.getPortalPosition(initialPortal));
			client.getSession().send(GamePackets.writeChangeMap(mapid, initialPortal, this));
			if (!isVisible())
				getClient().getSession().send(GamePackets.writeShowHide());
			map.spawnPlayer(this);
			return true;
		}
		return false;
	}

	public void prepareChannelChange() {
		leaveMapRoutines();
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

	public Miniroom getMiniRoom() {
		return miniroom;
	}

	public void setMiniRoom(Miniroom room) {
		this.miniroom = room;
	}

	public void close(boolean changingChannels) {
		//TODO: need to save debuffs in database so players cannot exploit
		//logging off and then on to get rid of debuffs...
		for (Pair<SkillState, ScheduledFuture<?>> cancelTask : skillFutures.values())
			cancelTask.right.cancel(true);
		for (Pair<ItemState, ScheduledFuture<?>> cancelTask : itemEffectFutures.values())
			cancelTask.right.cancel(true);
		for (Pair<MobSkillState, ScheduledFuture<?>> cancelTask : diseaseFutures.values())
			cancelTask.right.cancel(true);
		if (!changingChannels)
			GameServer.getChannel(client.getChannel()).getInterChannelInterface().sendBuddyOffline(this);
		leaveMapRoutines();
		if (map != null)
			map.removePlayer(this);
		saveCharacter();
		for (Cooldown cooling : cooldowns.values())
			cooling.cancel();
		client = null;
	}

	public LockableList<Mob> getControlledMobs() {
		return controllingMobs;
	}

	public void controlMonster(Mob m) {
		controllingMobs.addWhenSafe(m);
	}

	public void uncontrolMonster(Mob m) {
		controllingMobs.removeWhenSafe(m);
	}

	public void checkMonsterAggro(Mob monster) {
		if (!monster.controllerHasAggro()) {
			GameCharacter controller = monster.getController();
			if (controller == this) {
				monster.setControllerHasAggro(true);
			} else {
				if (controller != null) {
					controller.uncontrolMonster(monster);
					controller.getClient().getSession().send(GamePackets.writeStopControlMonster(monster));
				}
				monster.setController(this);
				controlMonster(monster);
				getClient().getSession().send(GamePackets.writeShowAndControlMonster(monster, true));
				monster.setControllerHasAggro(true);
				monster.setControllerKnowsAboutAggro(false);
			}
		}
	}

	private void addToWatchedList(short questId, QuestRequirementType type, Number id) {
		Map<Number, List<Short>> watched = questReqWatching.get(type);
		if (watched == null) {
			watched = new HashMap<Number, List<Short>>();
			questReqWatching.put(type, watched);
		}
		List<Short> questList = watched.get(id);
		if (questList == null) {
			questList = new ArrayList<Short>();
			watched.put(id, questList);
		}
		questList.add(Short.valueOf(questId));
	}

	private void addToWatchedList(short questId, QuestRequirementType type) {
		Map<Number, List<Short>> watched = questReqWatching.get(type);
		List<Short> questList;
		if (watched == null) {
			questList = new ArrayList<Short>();
			watched = Collections.singletonMap(null, questList);
			questReqWatching.put(type, watched);
		} else {
			questList = watched.get(null);
		}
		questList.add(Short.valueOf(questId));
	}

	private void removeFromWatchedList(short questId, QuestRequirementType type, Number id) {
		Map<Number, List<Short>> watched = questReqWatching.get(type);
		if (watched != null) {
			List<Short> questList = watched.get(id);
			if (questList != null) {
				questList.remove(Short.valueOf(questId));
				if (questList.isEmpty())
					watched.remove(id);
			}
			if (watched.isEmpty())
				questReqWatching.remove(type);
		}
	}

	private void removeFromWatchedList(short questId, QuestRequirementType type) {
		Map<Number, List<Short>> watched = questReqWatching.get(type);
		if (watched != null) {
			List<Short> questList = watched.get(null);
			questList.remove(Short.valueOf(questId));
			if (questList.isEmpty())
				questReqWatching.remove(type);
		}
	}

	public Map<Short, QuestEntry> getAllQuests() {
		return questStatuses;
	}

	/**
	 * Only recognize that the quest is started on the server without notifying the client
	 * @param questId
	 */
	public void localStartQuest(short questId) {
		Short oId = Short.valueOf(questId);
		QuestEntry status = questStatuses.get(oId);
		QuestChecks qc = QuestDataLoader.getInstance().getCompleteReqs(questId);
		Set<Integer> reqMobs;
		if (qc != null) {
			Map<Integer, Short> reqMobCounts = qc.getReqMobCounts();
			Map<Integer, Short> reqItems = qc.getReqItems();
			reqMobs = reqMobCounts.keySet();
			for (Integer mobId : reqMobCounts.keySet())
				addToWatchedList(questId, QuestRequirementType.MOB, mobId);
			for (Integer itemId : reqItems.keySet())
				addToWatchedList(questId, QuestRequirementType.ITEM, itemId);
			for (Integer petId : qc.getReqPets())
				addToWatchedList(questId, QuestRequirementType.PET, petId);
			for (Short reqQuestId : qc.getReqQuests().keySet())
				addToWatchedList(questId, QuestRequirementType.QUEST, reqQuestId);
			if (qc.requiresMesos())
				addToWatchedList(questId, QuestRequirementType.MESOS);
		} else {
			reqMobs = Collections.emptySet();
		}
		if (status == null) { //first time touching this quest
			status = new QuestEntry(QuestEntry.STATE_STARTED, reqMobs);
			questStatuses.put(oId, status);
		} else { //if we previously forfeited this quest
			status.updateState(QuestEntry.STATE_STARTED);
		}
		QuestDataLoader.getInstance().startedQuest(this, questId);

		//see if one req of another quest was starting this one...
		questStatusChanged(questId, QuestEntry.STATE_STARTED);
	}

	/**
	 * Start quest locally and send a notification to the client that we started a quest.
	 * @param questId
	 * @param npcId
	 */
	public void startQuest(short questId, int npcId) {
		localStartQuest(questId);
		getClient().getSession().send(GamePackets.writeQuestProgress(questId, ""));
		//TODO: check if quest start is not success
		getClient().getSession().send(GamePackets.writeQuestStartSuccess(questId, npcId));
	}


	/**
	 * Only recognize that the quest is completed on the server without notifying the client
	 * @param questId
	 * @param selection
	 */
	public short localCompleteQuest(short questId, int selection) {
		Short oId = Short.valueOf(questId);
		QuestEntry status = questStatuses.get(oId);
		QuestChecks qc = QuestDataLoader.getInstance().getCompleteReqs(questId);
		Set<Integer> reqMobs;
		if (qc != null) {
			Map<Integer, Short> reqMobCounts = qc.getReqMobCounts();
			Map<Integer, Short> reqItems = qc.getReqItems();
			reqMobs = reqMobCounts.keySet();
			for (Integer itemId : reqItems.keySet())
				removeFromWatchedList(questId, QuestRequirementType.ITEM, itemId);
			for (Integer mobId : reqMobCounts.keySet())
				removeFromWatchedList(questId, QuestRequirementType.MOB, mobId);
			for (Integer petId : qc.getReqPets())
				removeFromWatchedList(questId, QuestRequirementType.PET, petId);
			for (Short reqQuestId : qc.getReqQuests().keySet())
				removeFromWatchedList(questId, QuestRequirementType.QUEST, reqQuestId);
			if (qc.requiresMesos())
				removeFromWatchedList(questId, QuestRequirementType.MESOS);
		} else {
			reqMobs = Collections.emptySet();
		}
		if (status != null) {
			status.updateState(QuestEntry.STATE_COMPLETED);
		} else { //shouldn't ever happen...
			status = new QuestEntry(QuestEntry.STATE_COMPLETED, reqMobs);
			questStatuses.put(oId, status);
		}
		status.setCompletionTime(System.currentTimeMillis());
		short next = QuestDataLoader.getInstance().finishedQuest(this, questId, selection);

		//see if one req of another quest was completing this one...
		questStatusChanged(questId, QuestEntry.STATE_COMPLETED);

		return next;
	}

	/**
	 * Complete quest locally and send a notification to the client that we completed a quest.
	 * @param questId
	 * @param npcId
	 * @param selection
	 */
	public void completeQuest(short questId, int npcId, int selection) {
		short nextQuest = localCompleteQuest(questId, selection);
		getClient().getSession().send(GamePackets.writeQuestCompleted(questId, questStatuses.get(Short.valueOf(questId))));
		getClient().getSession().send(GamePackets.writeQuestStartNext(questId, npcId, nextQuest));
		getClient().getSession().send(GamePackets.writeShowSelfQuestEffect());
		getMap().sendToAll(GamePackets.writeShowQuestEffect(this));
	}

	/**
	 * Forfeit quest locally and send a notification to the client that we forfeited a quest.
	 * @param questId
	 * @param npcId
	 * @param selection
	 */
	public void forfeitQuest(short questId) {
		Short oId = Short.valueOf(questId);
		QuestEntry status = questStatuses.get(oId);
		if (status != null) {
			status.updateState(QuestEntry.STATE_NOT_STARTED);
		} else { //shouldn't ever happen
			QuestChecks qc = QuestDataLoader.getInstance().getCompleteReqs(questId);
			Set<Integer> reqMobs;
			if (qc != null) {
				Map<Integer, Short> reqMobCounts = qc.getReqMobCounts();
				Map<Integer, Short> reqItems = qc.getReqMobCounts();
				reqMobs = reqMobCounts.keySet();
				for (Integer itemId : reqItems.keySet())
					removeFromWatchedList(questId, QuestRequirementType.ITEM, itemId);
				for (Integer mobId : reqMobCounts.keySet())
					removeFromWatchedList(questId, QuestRequirementType.MOB, mobId);
				for (Integer petId : qc.getReqPets())
					removeFromWatchedList(questId, QuestRequirementType.PET, petId);
				for (Short reqQuestId : qc.getReqQuests().keySet())
					removeFromWatchedList(questId, QuestRequirementType.QUEST, reqQuestId);
				if (qc.requiresMesos())
					removeFromWatchedList(questId, QuestRequirementType.MESOS);
			} else {
				reqMobs = Collections.emptySet();
			}
			status = new QuestEntry(QuestEntry.STATE_NOT_STARTED, reqMobs);
			questStatuses.put(oId, status);
		}
		getClient().getSession().send(GamePackets.writeQuestForfeit(questId));
	}

	private void questStatusChanged(short questId, byte newStatus) {
		Short oId = Short.valueOf(questId);
		Map<Number, List<Short>> watchedQuests = questReqWatching.get(QuestRequirementType.QUEST);
		if (watchedQuests != null) {
			List<Short> watchingQuests = watchedQuests.get(Short.valueOf(questId));
			if (watchingQuests != null) {
				for (Short quest : watchingQuests) {
					questId = quest.shortValue();
					//completeReqs can never be null because in order for us to
					//add something to questReqWatching, it had to be not null
					Map<Short, Byte> questReq = QuestDataLoader.getInstance().getCompleteReqs(questId).getReqQuests();
					if (questReq.get(oId).byteValue() == newStatus)
						if (QuestDataLoader.getInstance().canCompleteQuest(this, questId))
							getClient().getSession().send(GamePackets.writeShowQuestReqsFulfilled(questId));
				}
			}
		}
	}

	public void itemCountChanged(int itemId) {
		Map<Number, List<Short>> watchedItems = questReqWatching.get(QuestRequirementType.ITEM);
		if (watchedItems != null) {
			Integer oId = Integer.valueOf(itemId);
			List<Short> watchingQuests = watchedItems.get(oId);
			if (watchingQuests != null) {
				for (Short quest : watchingQuests) {
					short questId = quest.shortValue();
					//completeReqs can never be null because in order for us to
					//add something to questReqWatching, it had to be not null
					Map<Integer, Short> itemReq = QuestDataLoader.getInstance().getCompleteReqs(questId).getReqItems();
					if (InventoryTools.hasItem(this, itemId, itemReq.get(oId).intValue()))
						if (QuestDataLoader.getInstance().canCompleteQuest(this, questId))
							getClient().getSession().send(GamePackets.writeShowQuestReqsFulfilled(questId));
				}
			}
		}
	}

	public void equippedPet(int petItemId) {
		Map<Number, List<Short>> watchedPets = questReqWatching.get(QuestRequirementType.PET);
		if (watchedPets != null) {
			List<Short> watchingQuests = watchedPets.get(Integer.valueOf(petItemId));
			if (watchingQuests != null)
				for (Short questId : watchingQuests)
					if (QuestDataLoader.getInstance().canCompleteQuest(this, questId.shortValue()))
						getClient().getSession().send(GamePackets.writeShowQuestReqsFulfilled(questId.shortValue()));
		}
	}

	public void petGainedCloseness(int petItemId) {
		Map<Number, List<Short>> watchedPetTameness = questReqWatching.get(QuestRequirementType.PET_TAMENESS);
		if (watchedPetTameness != null)
			for (Short questId : watchedPetTameness.get(null))
				if (QuestDataLoader.getInstance().canCompleteQuest(this, questId.shortValue()))
					getClient().getSession().send(GamePackets.writeShowQuestReqsFulfilled(questId.shortValue()));
	}

	private void mesosChanged() {
		Map<Number, List<Short>> watchingMesos = questReqWatching.get(QuestRequirementType.MESOS);
		if (watchingMesos != null)
			for (Short questId : watchingMesos.get(null))
				if (QuestDataLoader.getInstance().canCompleteQuest(this, questId.shortValue()))
					getClient().getSession().send(GamePackets.writeShowQuestReqsFulfilled(questId.shortValue()));
	}

	public void killedMob(short questId, int mobId) {
		QuestEntry status = questStatuses.get(Short.valueOf(questId));
		if (status != null) {
			status.killedMob(mobId);
			getClient().getSession().send(GamePackets.writeQuestProgress(questId, status.getData()));
			//completeReqs can never be null because in order for us to add
			//something to questReqWatching, it had to be not null
			Map<Integer, Short> mobReq = QuestDataLoader.getInstance().getCompleteReqs(questId).getReqMobCounts();
			Integer oId = Integer.valueOf(mobId);
			if (status.getMobCount(mobId) >= mobReq.get(oId)) {
				removeFromWatchedList(questId, QuestRequirementType.MOB, oId);
				if (QuestDataLoader.getInstance().canCompleteQuest(this, questId))
					getClient().getSession().send(GamePackets.writeShowQuestReqsFulfilled(questId));
			}
		} else { //shouldn't ever happen
			
		}
	}

	public List<MobDeathHook> getMobDeathHooks(final int mobId) {
		List<MobDeathHook> hooks = new ArrayList<MobDeathHook>();
		Map<Number, List<Short>> watchedMobs = questReqWatching.get(QuestRequirementType.MOB);
		if (watchedMobs != null) {
			List<Short> watchingQuests = watchedMobs.get(Integer.valueOf(mobId));
			if (watchingQuests != null) {
				for (Short quest : watchingQuests) {
					final short questId = quest.shortValue();
					final int mobMap = getMapId();
					//hopefully, this will prevent any memory leaks.
					final WeakReference<GameCharacter> futureSelf = new WeakReference<GameCharacter>(this);
					hooks.add(new MobDeathHook() {
						//TODO: special cases when we have party and we distribute
						//mob kill to all users.
						public void monsterKilled(GameCharacter highestDamager, GameCharacter last) {
							GameCharacter ourself = futureSelf.get();
							if (ourself != null && highestDamager == ourself
									//I think we had to be the highest damager and
									//we had to kill it to be recognized for the kill
									&& last == highestDamager
									&& ourself.getMapId() == mobMap)
								ourself.killedMob(questId, mobId);
						}
					});
				}
			}
		}
		return hooks;
	}

	/**
	 *
	 * @param questId
	 * @return false if the quest is not started (i.e. is completed, forfeited,
	 * or just plain untouched) or if it is started but meets all completion
	 * requirements and can be completed right now.
	 */
	public boolean isQuestActive(short questId) {
		QuestEntry status = questStatuses.get(Short.valueOf(questId));
		return status != null && status.getState() == QuestEntry.STATE_STARTED
				&& !QuestDataLoader.getInstance().canCompleteQuest(this, questId);
	}

	public int getMinigamePoints(MiniroomType game, MinigameResult stat) {
		Map<MinigameResult, Integer> stats = minigameStats.get(game);
		return stats != null ? stats.get(stat).intValue() : 0;
	}

	public void setMinigamePoints(MiniroomType game, MinigameResult stat, int newValue) {
		Map<MinigameResult, Integer> stats = minigameStats.get(game);
		if (stats == null) {
			stats = new EnumMap<MinigameResult, Integer>(MinigameResult.class);
			stats.put(MinigameResult.WIN, Integer.valueOf(0));
			stats.put(MinigameResult.TIE, Integer.valueOf(0));
			stats.put(MinigameResult.LOSS, Integer.valueOf(0));
			minigameStats.put(game, stats);
		}
		stats.put(stat, Integer.valueOf(newValue));
	}

	public EntityType getEntityType() {
		return EntityType.PLAYER;
	}

	public boolean isAlive() {
		return remHp > 0;
	}

	public boolean isVisible() {
		return !isEffectActive(PlayerStatusEffect.HIDE);
	}

	public byte[] getShowNewSpawnMessage() {
		return GamePackets.writeShowPlayer(this);
	}

	public byte[] getShowExistingSpawnMessage() {
		return getShowNewSpawnMessage();
	}

	public byte[] getDestructionMessage() {
		return GamePackets.writeRemovePlayer(this);
	}

	public String toString() {
		return "[Player: " + getName() + ']';
	}
}
