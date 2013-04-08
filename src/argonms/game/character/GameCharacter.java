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

package argonms.game.character;

import argonms.common.GlobalConstants;
import argonms.common.character.BuddyList;
import argonms.common.character.BuddyListEntry;
import argonms.common.character.Cooldown;
import argonms.common.character.KeyBinding;
import argonms.common.character.LoggedInPlayer;
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
import argonms.game.field.entity.Mob.MobDeathListener;
import argonms.game.field.entity.MysticDoor;
import argonms.game.field.entity.PlayerSkillSummon;
import argonms.game.loading.quest.QuestChecks;
import argonms.game.loading.quest.QuestChecks.QuestRequirementType;
import argonms.game.loading.quest.QuestDataLoader;
import argonms.game.loading.quest.QuestItemStats;
import argonms.game.loading.skill.SkillDataLoader;
import argonms.game.net.external.GameClient;
import argonms.game.net.external.GamePackets;
import argonms.game.net.external.handler.BuddyListHandler;
import argonms.game.script.EventManipulator;
import java.awt.Point;
import java.lang.ref.WeakReference;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author GoldenKevin
 */
public class GameCharacter extends LoggedInPlayer implements MapEntity {
	private static final Logger LOG = Logger.getLogger(GameCharacter.class.getName());

	private Point pos;
	/**
	 * 1-byte bit field, with the flags (from most significant to least significant bits):
	 * (?)(?)(?)(?)(has owner)(can fly)(?)(facing left)
	 */
	private byte stance;
	private short foothold;

	private GameClient client;

	private volatile short maxHp, maxMp;
	private volatile byte baseSpeed;
	private volatile int addStr, addDex, addInt, addLuk, addMaxHp, addMaxMp,
			addWatk, addWdef, addMatk, addMdef, addAcc, addAvo, addHands, addSpeed, addJump;

	private final LockableList<Mob> controllingMobs;
	private GameMap map;
	private final ConcurrentMap<MapMemoryVariable, Pair<Integer, Byte>> rememberedMaps;

	private BuddyList buddies;
	private GuildList guild;
	private PartyList party;

	private volatile int mesos;
	private StorageInventory storage;
	private int itemChair;
	private short mapChair;

	private final ConcurrentMap<Byte, KeyBinding> bindings;
	private volatile SkillMacro[] skillMacros;
	private final ConcurrentMap<Integer, SkillEntry> skillEntries;
	private final ConcurrentMap<Integer, Cooldown> cooldowns;
	private final ConcurrentMap<PlayerStatusEffect, PlayerStatusEffectValues> activeEffects;
	private final ConcurrentMap<Integer, Pair<SkillState, ScheduledFuture<?>>> skillFutures;
	private final ConcurrentMap<Integer, Pair<ItemState, ScheduledFuture<?>>> itemEffectFutures;
	private final ConcurrentMap<Short, Pair<MobSkillState, ScheduledFuture<?>>> diseaseFutures;
	private final ConcurrentMap<Integer, PlayerSkillSummon> summons;
	private volatile short energyCharge;
	private volatile MysticDoor door;

	private final Map<Short, QuestEntry> questStatuses;
	private final Map<QuestRequirementType, Map<Number, List<Short>>> questSubscriptions;
	private final Set<Short> completableQuests;

	private Miniroom miniroom;
	private final Map<MiniroomType, Map<MinigameResult, AtomicInteger>> minigameStats;
	private Chatroom chatroom;

	private volatile long lastFameGiven;
	private final Map<Integer, Long> famesThisMonth;

	private final List<Integer> wishList;

	private EventManipulator event;

	private GameCharacter () {
		//doesn't need to be synchronized because we only add/remove entries
		//before we can possibly get them
		bindings = new ConcurrentSkipListMap<Byte, KeyBinding>();
		skillEntries = new ConcurrentHashMap<Integer, SkillEntry>();
		cooldowns = new ConcurrentHashMap<Integer, Cooldown>();
		activeEffects = new ConcurrentHashMap<PlayerStatusEffect, PlayerStatusEffectValues>();
		skillFutures = new ConcurrentHashMap<Integer, Pair<SkillState, ScheduledFuture<?>>>();
		itemEffectFutures = new ConcurrentHashMap<Integer, Pair<ItemState, ScheduledFuture<?>>>();
		diseaseFutures = new ConcurrentHashMap<Short, Pair<MobSkillState, ScheduledFuture<?>>>();
		summons = new ConcurrentHashMap<Integer, PlayerSkillSummon>();
		controllingMobs = new LockableList<Mob>(new ArrayList<Mob>());
		rememberedMaps = new ConcurrentHashMap<MapMemoryVariable, Pair<Integer, Byte>>();

		questStatuses = new HashMap<Short, QuestEntry>();
		questSubscriptions = new EnumMap<QuestRequirementType, Map<Number, List<Short>>>(QuestRequirementType.class);
		completableQuests = new HashSet<Short>();

		minigameStats = Collections.synchronizedMap(new EnumMap<MiniroomType, Map<MinigameResult, AtomicInteger>>(MiniroomType.class));
		famesThisMonth = Collections.synchronizedMap(new HashMap<Integer, Long>());
		//doesn't need to be synchronized because we only add/remove entries
		//before we can possibly get them
		wishList = new ArrayList<Integer>(10);
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
			updateDbMapMemory(con);
			updateDbInventory(con);
			updateDbSkills(con);
			updateDbCooldowns(con);
			updateDbBindings(con);
			updateDbBuddies(con);
			updateDbParty(con);
			updateDbGuilds(con);
			updateDbQuests(con);
			updateDbMinigameStats(con);
			updateDbFameLog(con);
			//wishlists can't change in game server, so don't bother with them
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
			ps = con.prepareStatement("UPDATE `accounts` SET `storageslots` = ?, `storagemesos` = ? WHERE `id` = ?");
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
			ps = con.prepareStatement("UPDATE `characters` SET "
					+ "`accountid` = ?, `world` = ?, `name` = ?, `gender` = ?, `skin` = ?, `eyes` = ?, `hair` = ?, "
					+ "`level` = ?, `job` = ?, `str` = ?, `dex` = ?, `int` = ?, `luk` = ?, "
					+ "`hp` = ?, `maxhp` = ?, `mp` = ?, `maxmp` = ?, `ap` = ?, `sp` = ?, `exp` = ?, `fame` = ?, "
					+ "`spouse` = ?, `map` = ?, `spawnpoint` = ?, `mesos` = ?, "
					+ "`equipslots` = ?, `useslots` = ?, `setupslots` = ?, `etcslots` = ?, `cashslots` = ?, "
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
			ps.setShort(26, getInventory(InventoryType.EQUIP).getMaxSlots());
			ps.setShort(27, getInventory(InventoryType.USE).getMaxSlots());
			ps.setShort(28, getInventory(InventoryType.SETUP).getMaxSlots());
			ps.setShort(29, getInventory(InventoryType.ETC).getMaxSlots());
			ps.setShort(30, getInventory(InventoryType.CASH).getMaxSlots());
			ps.setShort(31, buddies.getCapacity());
			ps.setByte(32, getPrivilegeLevel());
			ps.setInt(33, getDataId());
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

	private void updateDbMapMemory(Connection con) throws SQLException {
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = con.prepareStatement("DELETE FROM `mapmemory` WHERE `characterid` = ?");
			ps.setInt(1, getDataId());
			ps.executeUpdate();
			ps.close();

			ps = con.prepareStatement("INSERT INTO `mapmemory` (`characterid`,`key`,`value`,`spawnpoint`) VALUES (?,?,?,?)");
			ps.setInt(1, getDataId());
			for (Entry<MapMemoryVariable, Pair<Integer, Byte>> entry : rememberedMaps.entrySet()) {
				ps.setString(2, entry.getKey().toString());
				ps.setInt(3, entry.getValue().left.intValue());
				ps.setByte(4, entry.getValue().right.byteValue());
				ps.addBatch();
			}
			ps.executeBatch();
		} finally {
			DatabaseManager.cleanup(DatabaseType.STATE, rs, ps, null);
		}
	}

	private void updateDbInventory(Connection con) throws SQLException {
		String invUpdate = "DELETE FROM `inventoryitems` WHERE "
				+ "`characterid` = ? AND `inventorytype` <= " + InventoryType.CASH.byteValue()
				+ " OR `accountid` = ? AND `inventorytype` = " + InventoryType.STORAGE.byteValue();
		PreparedStatement ps = null, ips = null;
		ResultSet rs = null;
		try {
			ps = con.prepareStatement(invUpdate);
			ps.setInt(1, getDataId());
			ps.setInt(2, client.getAccountId());
			ps.executeUpdate();
			ps.close();

			EnumMap<InventoryType, IInventory> union = new EnumMap<InventoryType, IInventory>(getInventories());
			union.put(InventoryType.STORAGE, storage);
			commitInventory(con, union);
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
			ps = con.prepareStatement("DELETE FROM `skills` WHERE `characterid` = ?");
			ps.setInt(1, getDataId());
			ps.executeUpdate();
			ps.close();

			ps = con.prepareStatement("INSERT INTO `skills` (`characterid`,`skillid`,`level`,`mastery`) VALUES (?,?,?,?)");
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
			ps = con.prepareStatement("DELETE FROM `cooldowns` WHERE `characterid` = ?");
			ps.setInt(1, getDataId());
			ps.executeUpdate();
			ps.close();

			ps = con.prepareStatement("INSERT INTO `cooldowns` (`characterid`,`skillid`,`remaining`) VALUES (?,?,?)");
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
			ps = con.prepareStatement("DELETE FROM `keymaps` WHERE `characterid` = ?");
			ps.setInt(1, getDataId());
			ps.executeUpdate();
			ps.close();

			ps = con.prepareStatement("INSERT INTO `keymaps` (`characterid`,`key`,`type`,`action`) VALUES (?,?,?,?)");
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

			ps = con.prepareStatement("DELETE FROM `skillmacros` WHERE `characterid` = ?");
			ps.setInt(1, getDataId());
			ps.executeUpdate();
			ps.close();

			ps = con.prepareStatement("INSERT INTO `skillmacros` "
					+ "(`characterid`,`position`,`name`,`silent`,`skill1`,`skill2`,`skill3`) "
					+ "VALUES (?,?,?,?,?,?,?)");
			ps.setInt(1, getDataId());
			for (byte pos = 0; pos < skillMacros.length; pos++) {
				SkillMacro macro = skillMacros[pos];
				if (macro.getName().isEmpty() && !macro.isSilent() && macro.getFirstSkill() == 0 && macro.getSecondSkill() == 0 && macro.getThirdSkill() == 0)
					continue; //placeholder macro

				ps.setByte(2, pos);
				ps.setString(3, macro.getName());
				ps.setBoolean(4, macro.isSilent());
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
			ps = con.prepareStatement("DELETE FROM `buddyentries` WHERE `owner` = ?");
			ps.setInt(1, getDataId());
			ps.executeUpdate();
			ps.close();

			ps = con.prepareStatement("INSERT INTO `buddyentries` "
					+ "(`owner`,`buddy`,`buddyname`,`status`) VALUES (?,?,?,?)");
			ps.setInt(1, getDataId());
			for (BuddyListEntry buddy : buddies.getBuddies()) {
				ps.setInt(2, buddy.getId());
				ps.setString(3, buddy.getName());
				ps.setByte(4, buddy.getStatus());
				ps.addBatch();
			}
			ps.setByte(4, BuddyListEntry.STATUS_INVITED);
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

	private void updateDbParty(Connection con) throws SQLException {
		PreparedStatement ps = null;
		try {
			ps = con.prepareStatement("DELETE FROM `parties` WHERE `characterid` = ?");
			ps.setInt(1, getDataId());
			ps.executeUpdate();

			if (party != null) {
				ps.close();
				ps = con.prepareStatement("INSERT INTO `parties` "
						+ "(`world`,`partyid`,`characterid`,`leader`) VALUES (?,?,?,?)");
				ps.setByte(1, getClient().getWorld());
				ps.setInt(2, party.getId());
				ps.setInt(3, getDataId());
				ps.setBoolean(4, party.getLeader() == getDataId());
				ps.executeUpdate();
			}
		} finally {
			DatabaseManager.cleanup(DatabaseType.STATE, null, ps, null);
		}
	}

	private void updateDbGuilds(Connection con) throws SQLException {
		PreparedStatement ps = null;
		try {
			ps = con.prepareStatement("DELETE FROM `guildmembers` WHERE `characterid` = ?");
			ps.setInt(1, getDataId());
			ps.executeUpdate();

			if (guild != null) {
				ps.close();
				ps = con.prepareStatement("INSERT INTO `guildmembers` "
						+ "(`guildid`,`characterid`,`rank`,`signature`,`alliancerank`) VALUES (?,?,?,?,?)");
				ps.setInt(1, guild.getId());
				ps.setInt(2, getDataId());
				GuildList.Member member = guild.getMember(getId());
				ps.setByte(3, member.getRank());
				ps.setByte(4, member.getSignature());
				ps.setByte(5, member.getAllianceRank());
				ps.executeUpdate();
			}
		} finally {
			DatabaseManager.cleanup(DatabaseType.STATE, null, ps, null);
		}
	}

	private void updateDbQuests(Connection con) throws SQLException {
		PreparedStatement ps = null, mps = null;
		ResultSet rs = null;
		try {
			ps = con.prepareStatement("DELETE FROM `queststatuses` WHERE `characterid` = ?");
			ps.setInt(1, getDataId());
			ps.executeUpdate();
			ps.close();

			ps = con.prepareStatement("INSERT INTO `queststatuses` "
					+ "(`characterid`,`questid`,`state`,`completed`) VALUES (?,?,?,?)",
					Statement.RETURN_GENERATED_KEYS);
			ps.setInt(1, getDataId());
			mps = con.prepareStatement("INSERT INTO `questmobprogress` "
					+ "(`queststatusid`,`mobid`,`count`) VALUES (?,?,?)");
			for (Entry<Short, QuestEntry> entry : questStatuses.entrySet()) {
				QuestEntry status = entry.getValue();
				ps.setShort(2, entry.getKey().shortValue());
				ps.setByte(3, status.getState());
				ps.setLong(4, status.getCompletionTime());
				if (status.getState() == QuestEntry.STATE_STARTED) {
					ps.executeUpdate();
					rs = ps.getGeneratedKeys();
					int questEntryId = rs.next() ? rs.getInt(1) : -1;
					rs.close();

					mps.setInt(1, questEntryId);
					for (Entry<Integer, ? extends Number> mobProgress : status.getAllMobCounts().entrySet()) {
						mps.setInt(2, mobProgress.getKey().intValue());
						mps.setShort(3, mobProgress.getValue().shortValue());
						mps.addBatch();
					}
				} else {
					ps.addBatch();
				}
			}
			ps.executeBatch();
			mps.executeBatch();
		} catch (SQLException e) {
			throw new SQLException("Failed to save quest states of character " + name, e);
		} finally {
			DatabaseManager.cleanup(DatabaseType.STATE, null, mps, null);
			DatabaseManager.cleanup(DatabaseType.STATE, rs, ps, null);
		}
	}

	private void updateDbMinigameStats(Connection con) throws SQLException {
		PreparedStatement ps = null;
		try {
			ps = con.prepareStatement("DELETE FROM `minigamescores` WHERE `characterid` = ?");
			ps.setInt(1, getDataId());
			ps.executeUpdate();
			ps.close();

			ps = con.prepareStatement("INSERT INTO `minigamescores` "
					+ "(`characterid`,`gametype`,`wins`,`ties`,`losses`) "
					+ "VALUES (?, ?, ?, ?, ?)");
			ps.setInt(1, getDataId());
			synchronized(minigameStats) {
				for (Entry<MiniroomType, Map<MinigameResult, AtomicInteger>> stats : minigameStats.entrySet()) {
					ps.setByte(2, stats.getKey().byteValue());
					ps.setInt(3, stats.getValue().get(MinigameResult.WIN).get());
					ps.setInt(4, stats.getValue().get(MinigameResult.TIE).get());
					ps.setInt(5, stats.getValue().get(MinigameResult.LOSS).get());
					ps.addBatch();
				}
			}
			ps.executeBatch();
		} catch (SQLException e) {
			throw new SQLException("Failed to save minigame stats of character " + name, e);
		} finally {
			DatabaseManager.cleanup(DatabaseType.STATE, null, ps, null);
		}
	}

	private void updateDbFameLog(Connection con) throws SQLException {
		PreparedStatement ps = null;
		try {
			ps = con.prepareStatement("DELETE FROM `famelog` WHERE `from` = ?");
			ps.setInt(1, getDataId());
			ps.executeUpdate();
			ps.close();

			ps = con.prepareStatement("INSERT INTO `famelog` (`from`,`to`,`millis`) VALUES (?,?,?)");
			ps.setInt(1, getDataId());
			long threshold = System.currentTimeMillis() - 1000L * 60 * 60 * 24 * 30;
			synchronized(famesThisMonth) {
				for (Entry<Integer, Long> fameEntry : famesThisMonth.entrySet()) {
					long time = fameEntry.getValue().longValue();
					//given time >= now - 30 days
					if (time >= threshold) {
						ps.setInt(2, fameEntry.getKey().intValue());
						ps.setLong(3, fameEntry.getValue().longValue());
						ps.addBatch();
					}
				}
			}
			ps.executeBatch();
		} catch (SQLException e) {
			throw new SQLException("Failed to save fame log of character " + name, e);
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
			ps = con.prepareStatement("SELECT `c`.*,`a`.`name`,`a`.`storageslots`,`a`.`storagemesos` "
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
			if (world != c.getWorld()) { //we are aware of our world
				LOG.log(Level.WARNING, "Client account {0} is trying to load character {1} on world {2} but exists on world {3}",
						new Object[] { accountid, id, c.getWorld(), world });
				return null;
			}
			GameCharacter p = new GameCharacter();
			p.client = c;
			p.loadPlayerStats(rs, id);
			p.map = GameServer.getChannel(c.getChannel()).getMapFactory().getMap(p.savedMapId);
			int forcedReturn = p.map.getForcedReturnMap();
			if (forcedReturn != GlobalConstants.NULL_MAP) {
				p.map = GameServer.getChannel(p.getClient().getChannel()).getMapFactory().getMap(forcedReturn);
				p.savedSpawnPoint = 0;
			}
			p.setPosition(p.map.getPortalPosition(p.savedSpawnPoint));

			p.maxHp = p.baseMaxHp;
			p.maxMp = p.baseMaxMp;

			p.mesos = rs.getInt(26);
			p.buddies = new BuddyList(rs.getShort(32));
			c.setAccountName(rs.getString(42));
			p.storage = new StorageInventory(rs.getShort(43), rs.getInt(44));
			rs.close();
			ps.close();

			ps = con.prepareStatement("SELECT `key`,`value`,`spawnpoint` FROM `mapmemory` WHERE `characterid` = ?");
			ps.setInt(1, id);
			rs = ps.executeQuery();
			while (rs.next())
				p.rememberedMaps.put(MapMemoryVariable.valueOf(rs.getString(1)), new Pair<Integer, Byte>(Integer.valueOf(rs.getInt(2)), Byte.valueOf(rs.getByte(3))));
			rs.close();
			ps.close();

			EnumMap<InventoryType, IInventory> invUnion = new EnumMap<InventoryType, IInventory>(p.getInventories());
			invUnion.put(InventoryType.STORAGE, p.storage);
			ps = con.prepareStatement("SELECT * FROM `inventoryitems` WHERE "
					+ "`characterid` = ? AND `inventorytype` <= " + InventoryType.CASH.byteValue()
					+ " OR `accountid` = ? AND `inventorytype` = " + InventoryType.STORAGE.byteValue());
			ps.setInt(1, id);
			ps.setInt(2, accountid);
			rs = ps.executeQuery();
			p.loadInventory(con, rs, invUnion);
			rs.close();
			ps.close();
			//inventories should still be safe right now, so no need for synchronization...
			for (InventorySlot equip : p.getInventory(InventoryType.EQUIPPED).getAll().values())
				p.equipChanged((Equip) equip, true, true);

			p.remHp = (short) Math.min(p.remHp, p.maxHp);
			p.remMp = (short) Math.min(p.remMp, p.maxMp);

			ps = con.prepareStatement("SELECT `skillid`,`level`,`mastery` "
					+ "FROM `skills` WHERE `characterid` = ?");
			ps.setInt(1, id);
			rs = ps.executeQuery();
			while (rs.next())
				p.skillEntries.put(Integer.valueOf(rs.getInt(1)), new SkillEntry(rs.getByte(2), rs.getByte(3)));
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

			ps = con.prepareStatement("SELECT `position`,`name`,`silent`,`skill1`,`skill2`,`skill3` "
					+ "FROM `skillmacros` WHERE `characterid` = ? ORDER BY `position` DESC");
			ps.setInt(1, id);
			rs = ps.executeQuery();
			byte macroPos = 0;
			for (boolean first = true; rs.next(); first = false) {
				macroPos = rs.getByte(1);
				if (first)
					p.skillMacros = new SkillMacro[macroPos + 1];
				p.skillMacros[macroPos] = new SkillMacro(rs.getString(2),
						rs.getBoolean(3), rs.getInt(4), rs.getInt(5),
						rs.getInt(6));
			}
			if (p.skillMacros == null)
				p.skillMacros = new SkillMacro[0]; //no macros
			for (macroPos--; macroPos >= 0; macroPos--)
				p.skillMacros[macroPos] = new SkillMacro("", false, 0, 0, 0); //placeholder macro
			rs.close();
			ps.close();

			ps = con.prepareStatement("SELECT `e`.`buddy` AS `id`,"
					+ "IF(ISNULL(`c`.`name`),`e`.`buddyname`,`c`.`name`) AS `name`,`e`.`status` "
					+ "FROM `buddyentries` `e` LEFT JOIN `characters` `c` ON `c`.`id` = `e`.`buddy` "
					+ "WHERE `owner` = ?");
			ps.setInt(1, id);
			rs = ps.executeQuery();
			while (rs.next()) {
				byte status = rs.getByte(3);
				if (status != BuddyListEntry.STATUS_INVITED)
					p.buddies.addBuddy(new BuddyListEntry(rs.getInt(1), rs.getString(2), status));
				else
					p.buddies.addInvite(rs.getInt(1), rs.getString(2));
			}
			rs.close();
			ps.close();

			ps = con.prepareStatement("SELECT `partyid` FROM `parties` WHERE `characterid` = ?");
			ps.setInt(1, id);
			rs = ps.executeQuery();
			if (rs.next())
				p.party = GameServer.getChannel(c.getChannel()).getCrossServerInterface().sendFetchPartyList(rs.getInt(1));
			rs.close();
			ps.close();

			ps = con.prepareStatement("SELECT `g`.`id` FROM `guilds` `g` LEFT JOIN `guildmembers` `m` ON `g`.`id` = `m`.`guildid` WHERE `m`.`characterid` = ?");
			ps.setInt(1, id);
			rs = ps.executeQuery();
			if (rs.next())
				p.guild = GameServer.getChannel(c.getChannel()).getCrossServerInterface().sendFetchGuildList(rs.getInt(1));
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
					byte state = rs.getByte(3);
					Map<Integer, AtomicInteger> mobProgress = new LinkedHashMap<Integer, AtomicInteger>();
					if (state == QuestEntry.STATE_STARTED) {
						mps.setInt(1, questEntryId);
						mrs = null;
						try {
							mrs = mps.executeQuery();
							while (mrs.next())
								mobProgress.put(Integer.valueOf(mrs.getInt(1)), new AtomicInteger(mrs.getShort(2)));
						} finally {
							DatabaseManager.cleanup(DatabaseType.STATE, mrs, null, null);
						}
					}
					QuestEntry status = new QuestEntry(state, mobProgress);
					status.setCompletionTime(rs.getLong(4));
					p.questStatuses.put(Short.valueOf(questId), status);
					if (status.getState() == QuestEntry.STATE_STARTED) {
						QuestChecks qc = QuestDataLoader.getInstance().getCompleteReqs(questId);
						if (qc != null) {
							for (Entry<Integer, Short> mob : qc.getReqMobCounts().entrySet())
								//mob progress cannot be undone, so it's safe to do this
								if (status.getMobCount(mob.getKey().intValue()) < mob.getValue().shortValue())
									p.addToWatchedList(questId, QuestRequirementType.MOB, mob.getKey());
							for (QuestItemStats item : qc.getReqItems())
								p.addToWatchedList(questId, QuestRequirementType.ITEM, item.getItemId());
							for (Integer petId : qc.getReqPets())
								p.addToWatchedList(questId, QuestRequirementType.PET, petId);
							for (Short reqQuestId : qc.getReqQuests().keySet())
								p.addToWatchedList(questId, QuestRequirementType.QUEST, reqQuestId);
							if (qc.requiresMesos())
								p.addToWatchedList(questId, QuestRequirementType.MESOS);
						}
					}
				}
			} finally {
				DatabaseManager.cleanup(DatabaseType.STATE, null, mps, null);
			}
			rs.close();
			ps.close();

			ps = con.prepareStatement("SELECT * FROM `minigamescores` WHERE `characterid` = ?");
			ps.setInt(1, id);
			rs = ps.executeQuery();
			while (rs.next()) {
				Map<MinigameResult, AtomicInteger> stats = new EnumMap<MinigameResult, AtomicInteger>(MinigameResult.class);
				stats.put(MinigameResult.WIN, new AtomicInteger(rs.getInt(4)));
				stats.put(MinigameResult.TIE, new AtomicInteger(rs.getInt(5)));
				stats.put(MinigameResult.LOSS, new AtomicInteger(rs.getInt(6)));
				p.minigameStats.put(MiniroomType.valueOf(rs.getByte(3)), Collections.unmodifiableMap(stats));
			}
			rs.close();
			ps.close();

			ps = con.prepareStatement("SELECT * FROM `famelog` WHERE `from` = ?");
			ps.setInt(1, id);
			rs = ps.executeQuery();
			long threshold = System.currentTimeMillis() - 1000L * 60 * 60 * 24 * 30;
			while (rs.next()) {
				long time = rs.getLong(4);
				//given time >= now - 30 days
				if (time >= threshold) {
					p.famesThisMonth.put(Integer.valueOf(rs.getInt(3)), Long.valueOf(time));
					if (time > p.lastFameGiven)
						p.lastFameGiven = time;
				}
			}
			rs.close();
			ps.close();

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
			DatabaseManager.cleanup(DatabaseType.STATE, irs, ips, null);
			DatabaseManager.cleanup(DatabaseType.STATE, rs, ps, con);
		}
	}

	@Override
	public GameClient getClient() {
		return client;
	}

	public void setExp(int newExp) {
		this.exp = newExp;
		getClient().getSession().send(GamePackets.writeUpdatePlayerStats(Collections.singletonMap(ClientUpdateKey.EXP, Integer.valueOf(exp)), false));
	}

	public void gainExp(int gain, boolean isKiller, boolean fromQuest) {
		if (gain != 0 && level < GlobalConstants.MAX_LEVEL) {
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
				hpInc += SkillDataLoader.getInstance().getSkill(Skills.IMPROVED_MAXHP_INCREASE).getLevel(skillLevel).getX();
			if ((skillLevel = getSkillLevel(Skills.IMPROVE_MAXHP)) != 0)
				hpInc += SkillDataLoader.getInstance().getSkill(Skills.IMPROVE_MAXHP).getLevel(skillLevel).getX();
			if ((skillLevel = getSkillLevel(Skills.IMPROVED_MAXMP_INCREASE)) != 0)
				mpInc += SkillDataLoader.getInstance().getSkill(Skills.IMPROVED_MAXMP_INCREASE).getLevel(skillLevel).getX();
			apInc += 5;
			exp -= ExpTables.getForLevel(level++);
			if (singleLevelOnly && exp >= ExpTables.getForLevel(level))
				exp = ExpTables.getForLevel(level) - 1;
		} while (level < GlobalConstants.MAX_LEVEL && exp >= ExpTables.getForLevel(level));

		updateMaxHp((short) Math.min(baseMaxHp + hpInc, 30000));
		updateMaxMp((short) Math.min(baseMaxMp + mpInc, 30000));
		remAp = (short) Math.min(remAp + apInc, Short.MAX_VALUE);
		remSp = (short) Math.min(remSp + spInc, Short.MAX_VALUE);

		//don't revive a leech who leveled up
		if (isAlive()) {
			remHp = maxHp;
			remMp = maxMp;
		}

		stats.put(ClientUpdateKey.LEVEL, Short.valueOf(level));
		stats.put(ClientUpdateKey.MAXHP, Short.valueOf(baseMaxHp));
		stats.put(ClientUpdateKey.MAXMP, Short.valueOf(baseMaxMp));
		stats.put(ClientUpdateKey.HP, Short.valueOf(remHp));
		stats.put(ClientUpdateKey.MP, Short.valueOf(remMp));
		stats.put(ClientUpdateKey.AVAILABLEAP, Short.valueOf(remAp));
		stats.put(ClientUpdateKey.AVAILABLESP, Short.valueOf(remSp));

		getMap().sendToAll(GamePackets.writeShowLevelUp(this), this);
		if (party != null)
			GameServer.getChannel(getClient().getChannel()).getCrossServerInterface().sendPartyLevelOrJobUpdate(this, true);
		if (guild != null)
			GameServer.getChannel(getClient().getChannel()).getCrossServerInterface().sendGuildLevelOrJobUpdate(this, true);
		pushHpToParty();

		return level < GlobalConstants.MAX_LEVEL ? exp : 0;
	}

	@Override
	public void setLevel(short newLevel) {
		boolean levelUp = newLevel > getLevel();
		super.setLevel(newLevel);
		if (levelUp)
			getMap().sendToAll(GamePackets.writeShowLevelUp(this), this);
		getClient().getSession().send(GamePackets.writeUpdatePlayerStats(Collections.singletonMap(ClientUpdateKey.LEVEL, Short.valueOf(level)), false));
		if (party != null)
			GameServer.getChannel(getClient().getChannel()).getCrossServerInterface().sendPartyLevelOrJobUpdate(this, true);
		if (guild != null)
			GameServer.getChannel(getClient().getChannel()).getCrossServerInterface().sendGuildLevelOrJobUpdate(this, true);
	}

	@Override
	public void setJob(short newJob) {
		super.setJob(newJob);
		getMap().sendToAll(GamePackets.writeShowJobChange(this), this);
		getClient().getSession().send(GamePackets.writeUpdatePlayerStats(Collections.singletonMap(ClientUpdateKey.JOB, Short.valueOf(job)), false));
		if (party != null)
			GameServer.getChannel(getClient().getChannel()).getCrossServerInterface().sendPartyLevelOrJobUpdate(this, false);
		if (guild != null)
			GameServer.getChannel(getClient().getChannel()).getCrossServerInterface().sendGuildLevelOrJobUpdate(this, false);
	}

	public int getCurrentStr() {
		return baseStr + addStr;
	}

	public short incrementLocalStr() {
		return ++this.baseStr;
	}

	@Override
	public void setHair(short newHair) {
		super.setHair(newHair);
		getMap().sendToAll(GamePackets.writeUpdateAvatar(this), this);
		getClient().getSession().send(GamePackets.writeUpdatePlayerStats(Collections.singletonMap(ClientUpdateKey.HAIR, Short.valueOf(hair)), false));
		if (chatroom != null)
			GameServer.getChannel(getClient().getChannel()).getCrossServerInterface().sendChatroomPlayerLookUpdate(this, chatroom.getRoomId());
	}

	@Override
	public void setSkin(byte newSkin) {
		super.setSkin(newSkin);
		getMap().sendToAll(GamePackets.writeUpdateAvatar(this), this);
		getClient().getSession().send(GamePackets.writeUpdatePlayerStats(Collections.singletonMap(ClientUpdateKey.SKIN, Byte.valueOf(skin)), false));
		if (chatroom != null)
			GameServer.getChannel(getClient().getChannel()).getCrossServerInterface().sendChatroomPlayerLookUpdate(this, chatroom.getRoomId());
	}

	@Override
	public void setEyes(short newEyes) {
		super.setEyes(newEyes);
		getMap().sendToAll(GamePackets.writeUpdateAvatar(this), this);
		getClient().getSession().send(GamePackets.writeUpdatePlayerStats(Collections.singletonMap(ClientUpdateKey.FACE, Short.valueOf(eyes)), false));
		if (chatroom != null)
			GameServer.getChannel(getClient().getChannel()).getCrossServerInterface().sendChatroomPlayerLookUpdate(this, chatroom.getRoomId());
	}

	@Override
	public void setStr(short newStr) {
		super.setStr(newStr);
		getClient().getSession().send(GamePackets.writeUpdatePlayerStats(Collections.singletonMap(ClientUpdateKey.STR, Short.valueOf(baseStr)), false));
	}

	public int getCurrentDex() {
		return baseDex + addDex;
	}

	public short incrementLocalDex() {
		return ++this.baseDex;
	}

	@Override
	public void setDex(short newDex) {
		super.setDex(newDex);
		getClient().getSession().send(GamePackets.writeUpdatePlayerStats(Collections.singletonMap(ClientUpdateKey.DEX, Short.valueOf(baseDex)), false));
	}

	public int getCurrentInt() {
		return baseInt + addInt;
	}

	public short incrementLocalInt() {
		return ++this.baseInt;
	}

	@Override
	public void setInt(short newInt) {
		super.setInt(newInt);
		getClient().getSession().send(GamePackets.writeUpdatePlayerStats(Collections.singletonMap(ClientUpdateKey.INT, Short.valueOf(baseInt)), false));
	}

	public int getCurrentLuk() {
		return baseLuk + addLuk;
	}

	public short incrementLocalLuk() {
		return ++this.baseLuk;
	}

	@Override
	public void setLuk(short newLuk) {
		super.setLuk(newLuk);
		getClient().getSession().send(GamePackets.writeUpdatePlayerStats(Collections.singletonMap(ClientUpdateKey.LUK, Short.valueOf(baseLuk)), false));
	}

	public void setLocalHp(short newHp) {
		if (newHp < 0)
			newHp = 0;
		else if (newHp > maxHp)
			newHp = maxHp;
		this.remHp = newHp;
		pushHpToParty();
	}

	public void setHp(short newHp) {
		boolean notAlreadyDead = isAlive();
		setLocalHp(newHp);
		getClient().getSession().send(GamePackets.writeUpdatePlayerStats(Collections.singletonMap(ClientUpdateKey.HP, Short.valueOf(remHp)), false));
		if (notAlreadyDead && remHp == 0)
			died();
	}

	public void gainHp(int gain) {
		setHp((short) Math.min(remHp + gain, getCurrentMaxHp()));
	}

	public short getCurrentMaxHp() {
		return maxHp;
	}

	public void died() {
		getClient().getSession().send(GamePackets.writeEnableActions());
		if (event != null)
			event.playerDied(this);

		byte lossPercent;
		Inventory inv = getInventory(InventoryType.CASH);
		if (PlayerJob.isBeginner(getJob()) || getLevel() == 200) {
			lossPercent = 0;
		} else if (inv.hasItem(5130000, 1)) { //safety charm
			InventoryTools.UpdatedSlots changedSlots = InventoryTools.removeFromInventory(inv, 5130000, 1, false);
			for (Short s : changedSlots.modifiedSlots)
				getClient().getSession().send(GamePackets.writeInventoryUpdateSlotQuantity(InventoryType.CASH, s.shortValue(), inv.get(s.shortValue())));
			for (Short s : changedSlots.addedOrRemovedSlots)
				getClient().getSession().send(GamePackets.writeInventoryClearSlot(InventoryType.CASH, s.shortValue()));
			itemCountChanged(5130000);
			getClient().getSession().send(GamePackets.writeSelfCharmEffect((short) Math.min(0xFF, InventoryTools.getAmountOfItem(inv, 5130000)), (short) 99));
			lossPercent = 0;
		} else if (map.getStaticData().reducedExpLoss()) {
			lossPercent = 1;
		} else if (PlayerJob.isThief(getJob())) {
			lossPercent = 5;
		} else if (PlayerJob.isMage(getJob())) {
			lossPercent = 7;
		} else {
			lossPercent = 10;
		}
		if (lossPercent != 0)
			setExp(Math.max(0, exp - (int) ((long) ExpTables.getForLevel(getLevel()) * lossPercent / 100)));
	}

	private void updateMaxHp(short newMax) {
		this.baseMaxHp = newMax;
		recalculateMaxHp();
	}

	public void setMaxHp(short newMax) {
		updateMaxHp(newMax);
		if (remHp > maxHp) {
			remHp = maxHp;
			getClient().getSession().send(GamePackets.writeUpdatePlayerStats(Collections.singletonMap(ClientUpdateKey.HP, Short.valueOf(remHp)), false));
		}
		getClient().getSession().send(GamePackets.writeUpdatePlayerStats(Collections.singletonMap(ClientUpdateKey.MAXHP, Short.valueOf(baseMaxHp)), false));
		pushHpToParty();
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
		pushHpToParty();
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
		setMp((short) Math.min(remMp + gain, getCurrentMaxMp()));
	}

	public short getCurrentMaxMp() {
		return maxMp;
	}

	private void updateMaxMp(short newMax) {
		this.baseMaxMp = newMax;
		recalculateMaxMp();
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

	public short decrementLocalAp() {
		return --this.remAp;
	}

	public void setAp(short newAp) {
		this.remAp = newAp;
		getClient().getSession().send(GamePackets.writeUpdatePlayerStats(Collections.singletonMap(ClientUpdateKey.AVAILABLEAP, Short.valueOf(remAp)), false));
	}

	public void setSp(short newSp) {
		this.remSp = newSp;
		getClient().getSession().send(GamePackets.writeUpdatePlayerStats(Collections.singletonMap(ClientUpdateKey.AVAILABLESP, Short.valueOf(remSp)), false));
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

	public StorageInventory getStorageInventory() {
		return storage;
	}

	@Override
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

	public int getWatk() {
		return addWatk;
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

	public void setBaseSpeed(byte newVal) {
		baseSpeed = newVal;
	}

	public short getSpeed() {
		return (short) Math.min(baseSpeed == 100 ? (100 + addSpeed) : baseSpeed, 140);
	}

	public GameMap getMap() {
		return map;
	}

	public GameMap getReturnMap() {
		return GameServer.getChannel(client.getChannel()).getMapFactory().getMap(map.getReturnMap());
	}

	public GameMap getForcedReturnMap() {
		return GameServer.getChannel(client.getChannel()).getMapFactory().getMap(map.getForcedReturnMap());
	}

	@Override
	public int getMapId() {
		return (map != null ? map.getDataId() : super.getMapId());
	}

	public void rememberMap(MapMemoryVariable key, byte spawnPoint) {
		rememberedMaps.put(key, new Pair<Integer, Byte>(Integer.valueOf(getMapId()), Byte.valueOf(spawnPoint)));
	}

	public void rememberMap(MapMemoryVariable key) {
		rememberMap(key, map.nearestSpawnPoint(getPosition()));
	}

	public Pair<Integer, Byte> getRememberedMap(MapMemoryVariable key) {
		Pair<Integer, Byte> location = rememberedMaps.get(key);
		if (location == null)
			location = new Pair<Integer, Byte>(Integer.valueOf(GlobalConstants.NULL_MAP), Byte.valueOf((byte) -1));
		return location;
	}

	public Pair<Integer, Byte> resetRememberedMap(MapMemoryVariable key) {
		Pair<Integer, Byte> location = rememberedMaps.remove(key);
		if (location == null)
			location = new Pair<Integer, Byte>(Integer.valueOf(GlobalConstants.NULL_MAP), Byte.valueOf((byte) -1));
		return location;
	}

	public BuddyList getBuddyList() {
		return buddies;
	}

	@Override
	public byte getBuddyListCapacity() {
		return (byte) buddies.getCapacity();
	}

	public void equipChanged(Equip e, boolean putOn, boolean permanent) {
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
				if (permanent && remHp > maxHp) {
					remHp = maxHp;
					getClient().getSession().send(GamePackets.writeUpdatePlayerStats(Collections.singletonMap(ClientUpdateKey.HP, Short.valueOf(remHp)), false));
				}
			}
			stat = e.getMp();
			if (stat > 0) {
				addMaxMp -= stat;
				recalculateMaxMp();
				if (permanent && remMp > maxMp) {
					remMp = maxMp;
					getClient().getSession().send(GamePackets.writeUpdatePlayerStats(Collections.singletonMap(ClientUpdateKey.MP, Short.valueOf(remMp)), false));
				}
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

			pushHpToParty();
		}
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

	public SkillMacro[] getMacros() {
		return skillMacros;
	}

	public void setMacros(SkillMacro[] newMacros) {
		skillMacros = newMacros;
	}

	@Override
	public Map<Integer, SkillEntry> getSkillEntries() {
		return skillEntries;
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
	 * @param onlyMasterLevel if true, only set the current skill level to the
	 * level parameter if the skill has not been leveled up yet.
	 */
	public void setSkillLevel(int skill, byte level, byte masterLevel, boolean onlyMasterLevel) {
		byte defaultMasterLevel;
		if (masterLevel == -1)
			if (!Skills.isFourthJob(skill))
				defaultMasterLevel = SkillDataLoader.getInstance().getSkill(skill).maxLevel();
			else
				defaultMasterLevel = 0;
		else
			defaultMasterLevel = masterLevel;

		SkillEntry skillLevel;
		SkillEntry newSkillLevel = new SkillEntry(level, defaultMasterLevel);
		if (level != 0 || masterLevel != -1)
			skillLevel = skillEntries.putIfAbsent(Integer.valueOf(skill), newSkillLevel);
		else
			skillLevel = skillEntries.remove(Integer.valueOf(skill));

		if (skillLevel == null) {
			skillLevel = newSkillLevel;
		} else {
			if (!onlyMasterLevel)
				skillLevel.changeCurrentLevel(level);
			if (masterLevel != -1)
				skillLevel.changeMasterLevel(masterLevel);
		}
		getClient().getSession().send(GamePackets.writeUpdateSkillLevel(skill, skillLevel.getLevel(), skillLevel.getMasterLevel()));
	}

	public void addCooldown(final int skill, short time) {
		cooldowns.put(Integer.valueOf(skill), new Cooldown(time * 1000, new Runnable() {
			@Override
			public void run() {
				removeCooldown(skill);
				getClient().getSession().send(CommonPackets.writeCooldown(skill, (short) 0));
			}
		}));
	}

	public void removeCooldown(int skill) {
		cooldowns.remove(Integer.valueOf(skill)).cancel();
	}

	public void cancelCooldowns() {
		for (Iterator<Map.Entry<Integer, Cooldown>> iter = cooldowns.entrySet().iterator(); iter.hasNext(); ) {
			Map.Entry<Integer, Cooldown> cooldown = iter.next();
			iter.remove();
			cooldown.getValue().cancel();
			getClient().getSession().send(CommonPackets.writeCooldown(cooldown.getKey().intValue(), (short) 0));
		}
	}

	@Override
	public Map<Integer, Cooldown> getCooldowns() {
		return cooldowns;
	}

	public void addToActiveEffects(PlayerStatusEffect buff, PlayerStatusEffectValues value) {
		activeEffects.put(buff, value);
	}

	public void addCancelEffectTask(StatusEffectsData e, ScheduledFuture<?> cancelTask, byte level, long endTime) {
		switch (e.getSourceType()) {
			case ITEM:
				itemEffectFutures.put(Integer.valueOf(e.getDataId()), new Pair<ItemState, ScheduledFuture<?>>(new ItemState(endTime), cancelTask));
				break;
			case PLAYER_SKILL:
				skillFutures.put(Integer.valueOf(e.getDataId()), new Pair<SkillState, ScheduledFuture<?>>(new SkillState(level, endTime), cancelTask));
				break;
			case MOB_SKILL:
				diseaseFutures.put(Short.valueOf((short) e.getDataId()), new Pair<MobSkillState, ScheduledFuture<?>>(new MobSkillState(level, endTime), cancelTask));
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
		return activeEffects.remove(e);
	}

	public void removeCancelEffectTask(StatusEffectsData e) {
		Pair<? extends BuffState, ScheduledFuture<?>> cancelTask;
		switch (e.getSourceType()) {
			case ITEM:
				cancelTask = itemEffectFutures.remove(Integer.valueOf(e.getDataId()));
				break;
			case PLAYER_SKILL:
				cancelTask = skillFutures.remove(Integer.valueOf(e.getDataId()));
				break;
			case MOB_SKILL:
				cancelTask = diseaseFutures.remove(Short.valueOf((short) e.getDataId()));
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

	public long getSkillExpireTime(int skillId) {
		Pair<SkillState, ScheduledFuture<?>> activeSkill = skillFutures.get(Integer.valueOf(skillId));
		if (activeSkill == null)
			return 0;

		return activeSkill.left.endTime;
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

	public MysticDoor getDoor() {
		return door;
	}

	public void setDoor(MysticDoor door) {
		this.door = door;
	}

	public int getItemEffect() {
		return 0;
	}

	public int getItemChair() {
		return itemChair;
	}

	public void setItemChair(int itemId) {
		itemChair = itemId;
	}

	public void setMapChair(short chairId) {
		if (chairId == 0)
			map.leaveChair(mapChair);
		else
			map.occupyChair(chairId);
		mapChair = chairId;
	}

	public void leaveMapRoutines() {
		if (miniroom != null) {
			miniroom.leaveRoom(this);
			miniroom = null;
		}
		if (getItemChair() != 0)
			setItemChair(0);
		else if (mapChair != 0)
			setMapChair((short) 0);
	}

	private void leaveMapAndSetTo(GameMap goTo, byte initialPortal) {
		mapChangeCancelSkills();
		leaveMapRoutines();
		map.removePlayer(this);
		map = goTo;
		if (initialPortal != MysticDoor.OUT_OF_TOWN_PORTAL_ID)
			setPosition(map.getPortalPosition(initialPortal));
		setFoothold((short) 0);
	}

	public void changeMap(GameMap goTo, byte initialPortal) {
		leaveMapAndSetTo(goTo, initialPortal);
		client.getSession().send(GamePackets.writeChangeMap(goTo.getDataId(), initialPortal, this));
		if (!isVisible())
			getClient().getSession().send(GamePackets.writeShowHide());
		map.spawnPlayer(this);
		if (party != null) {
			party.lockRead();
			try {
				for (PartyList.LocalMember member : party.getMembersInLocalChannel())
					member.getPlayer().getClient().getSession().send(GamePackets.writePartyList(member.getPlayer().getParty()));
			} finally {
				party.unlockRead();
			}
		}
		if (event != null)
			event.playerChangedMap(this);
	}

	public boolean changeMap(int mapid, byte initialPortal) {
		GameMap goTo = GameServer.getChannel(client.getChannel()).getMapFactory().getMap(mapid);
		if (goTo != null) {
			changeMap(goTo, initialPortal);
			return true;
		}
		return false;
	}

	public boolean changeMap(int mapid) {
		return changeMap(mapid, (byte) 0);
	}

	public void changeMapAndChannel(int mapid, byte initialPortal, byte channel) {
		GameMap goTo = GameServer.getChannel(client.getChannel()).getMapFactory().getMap(mapid);
		if (goTo != null) {
			leaveMapAndSetTo(goTo, initialPortal);
			//party members will get new map when we connect to new channel
			if (event != null)
				event.playerChangedMap(this);
			GameServer.getChannel(client.getChannel()).requestChannelChange(this, channel);
		}
	}

	private void mapChangeCancelSkills() {
		PlayerStatusEffectValues v = getEffectValue(PlayerStatusEffect.PUPPET);
		if (v != null)
			StatusEffectTools.dispelEffectsAndShowVisuals(this, v.getEffectsData());
	}

	public void channelChangeCancelSkills() {
		PlayerStatusEffectValues v = getEffectValue(PlayerStatusEffect.PUPPET);
		if (v != null)
			StatusEffectTools.dispelEffectsAndShowVisuals(this, v.getEffectsData());
		v = getEffectValue(PlayerStatusEffect.MYSTIC_DOOR);
		if (v != null) {
			door.setNoDestroyAnimation();
			StatusEffectTools.dispelEffectsAndShowVisuals(this, v.getEffectsData());
		}
	}

	public void logOffCancelSkills() {
		PlayerStatusEffectValues v = getEffectValue(PlayerStatusEffect.PUPPET);
		if (v != null)
			StatusEffectTools.dispelEffectsAndShowVisuals(this, v.getEffectsData());
		v = getEffectValue(PlayerStatusEffect.MYSTIC_DOOR);
		if (v != null) {
			door.setNoDestroyAnimation();
			StatusEffectTools.dispelEffectsAndShowVisuals(this, v.getEffectsData());
		}
	}

	private void prepareExitChannel(boolean quickCleanup) {
		//TODO: need to save debuffs in database so players cannot exploit
		//logging off and then on to get rid of debuffs...
		for (Pair<SkillState, ScheduledFuture<?>> cancelTask : skillFutures.values())
			cancelTask.right.cancel(false);
		for (Pair<ItemState, ScheduledFuture<?>> cancelTask : itemEffectFutures.values())
			cancelTask.right.cancel(false);
		for (Pair<MobSkillState, ScheduledFuture<?>> cancelTask : diseaseFutures.values())
			cancelTask.right.cancel(false);

		for (Cooldown cooling : cooldowns.values())
			cooling.cancel();

		if (!quickCleanup) {
			leaveMapRoutines();
			if (map != null)
				map.removePlayer(this);
			saveCharacter();
		}
	}

	public void prepareChannelChange() {
		if (event != null)
			event.playerDisconnected(this);
		if (party != null)
			GameServer.getChannel(client.getChannel()).getCrossServerInterface().sendPartyMemberLogOffNotifications(this, false);
		if (guild != null)
			GameServer.getChannel(client.getChannel()).getCrossServerInterface().sendGuildMemberLogOffNotifications(this, false);
		if (chatroom != null)
			GameServer.getChannel(getClient().getChannel()).getCrossServerInterface().chatroomPlayerChangingChannels(getId(), chatroom);
		prepareExitChannel(false);
	}

	public void prepareLogOff(boolean quickCleanup) {
		if (event != null)
			event.playerDisconnected(this);
		GameServer.getChannel(client.getChannel()).getCrossServerInterface().sendBuddyLogOffNotifications(this);
		if (party != null)
			GameServer.getChannel(client.getChannel()).getCrossServerInterface().sendPartyMemberLogOffNotifications(this, true);
		if (guild != null)
			GameServer.getChannel(client.getChannel()).getCrossServerInterface().sendGuildMemberLogOffNotifications(this, true);
		if (chatroom != null)
			GameServer.getChannel(getClient().getChannel()).getCrossServerInterface().sendLeaveChatroom(this);
		prepareExitChannel(quickCleanup);
	}

	public GuildList getGuild() {
		return guild;
	}

	public void setGuild(GuildList guild) {
		this.guild = guild;
	}

	public PartyList getParty() {
		return party;
	}

	public void setParty(PartyList party) {
		if (party == null && event != null)
			event.partyMemberDischarged(this);
		this.party = party;
	}

	public void pushHpToParty() {
		if (party != null) {
			party.lockRead();
			try {
				for (GameCharacter member : party.getLocalMembersInMap(getMapId()))
					member.getClient().getSession().send(GamePackets.writePartyMemberHpUpdate(getId(), getHp(), getCurrentMaxHp()));
			} finally {
				party.unlockRead();
			}
		}
	}

	public void pullPartyHp() {
		if (party != null) {
			party.lockRead();
			try {
				for (GameCharacter member : party.getLocalMembersInMap(getMapId()))
					client.getSession().send(GamePackets.writePartyMemberHpUpdate(member.getId(), member.getHp(), member.getCurrentMaxHp()));
			} finally {
				party.unlockRead();
			}
		}
	}

	public Miniroom getMiniRoom() {
		return miniroom;
	}

	public void setMiniRoom(Miniroom room) {
		this.miniroom = room;
	}

	public Chatroom getChatRoom() {
		return chatroom;
	}

	public void setChatRoom(Chatroom room) {
		this.chatroom = room;
	}

	public void disconnect() {
		client = null;
	}

	public boolean isClosed() {
		return client == null;
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

	public boolean canCompleteQuest(short questId) {
		return QuestDataLoader.getInstance().completeRequirementError(this, questId) == 0;
	}

	/**
	 * Quests must be write locked when this method is called.
	 * @param questId
	 * @param type
	 * @param id 
	 */
	private void addToWatchedList(short questId, QuestRequirementType type, Number id) {
		Map<Number, List<Short>> watched;
		watched = questSubscriptions.get(type);
		if (watched == null) {
			watched = Collections.synchronizedMap(new HashMap<Number, List<Short>>());
			questSubscriptions.put(type, watched);
		}
		if (canCompleteQuest(questId))
			completableQuests.add(Short.valueOf(questId));
		List<Short> questList = watched.get(id);
		if (questList == null) {
			questList = Collections.synchronizedList(new ArrayList<Short>());
			watched.put(id, questList);
		}
		questList.add(Short.valueOf(questId));
	}

	/**
	 * Quests must be write locked when this method is called.
	 * @param questId
	 * @param type 
	 */
	private void addToWatchedList(short questId, QuestRequirementType type) {
		List<Short> questList;
		Map<Number, List<Short>> watched = questSubscriptions.get(type);
		if (watched == null) {
			questList = Collections.synchronizedList(new ArrayList<Short>());
			watched = Collections.singletonMap(null, questList);
			questSubscriptions.put(type, watched);
		} else {
			questList = watched.get(null);
		}
		if (canCompleteQuest(questId))
			completableQuests.add(Short.valueOf(questId));
		questList.add(Short.valueOf(questId));
	}

	/**
	 * Quests must be write locked when this method is called.
	 * @param questId
	 * @param type
	 * @param id 
	 */
	private void removeFromWatchedList(short questId, QuestRequirementType type, Number id) {
		Map<Number, List<Short>> watched = questSubscriptions.get(type);
		if (watched != null) {
			List<Short> questList = watched.get(id);
			if (questList != null) {
				questList.remove(Short.valueOf(questId));
				if (questList.isEmpty())
					watched.remove(id);
			}
			if (watched.isEmpty())
				questSubscriptions.remove(type);
		}
		completableQuests.remove(Short.valueOf(questId));
	}

	/**
	 * Quests must be write locked when this method is called.
	 * @param questId
	 * @param type 
	 */
	private void removeFromWatchedList(short questId, QuestRequirementType type) {
		Map<Number, List<Short>> watched = questSubscriptions.get(type);
		if (watched != null) {
			List<Short> questList = watched.get(null);
			questList.remove(Short.valueOf(questId));
			if (questList.isEmpty())
				questSubscriptions.remove(type);
		}
		completableQuests.remove(Short.valueOf(questId));
	}

	/**
	 * Quests must be at least read locked while the returned Map is in scope.
	 * @return 
	 */
	@Override
	public Map<Short, QuestEntry> getAllQuests() {
		return questStatuses;
	}

	public void updateStatusOfStartedQuest(short questId) {
		QuestChecks qc = QuestDataLoader.getInstance().getCompleteReqs(questId);
		writeLockQuests();
		try {
			Set<Integer> reqMobs;
			if (qc != null) {
				Map<Integer, Short> reqMobCounts = qc.getReqMobCounts();
				reqMobs = reqMobCounts.keySet();
				for (Integer mobId : reqMobs)
					addToWatchedList(questId, QuestRequirementType.MOB, mobId);
				for (QuestItemStats item : qc.getReqItems())
					addToWatchedList(questId, QuestRequirementType.ITEM, Integer.valueOf(item.getItemId()));
				for (Integer petId : qc.getReqPets())
					addToWatchedList(questId, QuestRequirementType.PET, petId);
				for (Short reqQuestId : qc.getReqQuests().keySet())
					addToWatchedList(questId, QuestRequirementType.QUEST, reqQuestId);
				if (qc.requiresMesos())
					addToWatchedList(questId, QuestRequirementType.MESOS);
			} else {
				reqMobs = Collections.emptySet();
			}
			Short oId = Short.valueOf(questId);
			QuestEntry status = questStatuses.get(oId);
			if (status == null) {
				status = new QuestEntry(QuestEntry.STATE_STARTED, reqMobs);
				questStatuses.put(oId, status);
			} else {
				status.updateState(QuestEntry.STATE_STARTED);
			}
		} finally {
			writeUnlockQuests();
		}

		//see if one req of another quest was starting this one...
		questStatusChanged(questId, QuestEntry.STATE_STARTED);
	}

	/**
	 * Only recognize that the quest is started on the server without notifying the client
	 * @param questId
	 */
	public byte localStartQuest(short questId) {
		byte error = (byte) -QuestDataLoader.getInstance().startedQuest(this, questId);
		if (error != 0)
			return error;

		updateStatusOfStartedQuest(questId);

		return 0;
	}

	/**
	 * Start quest locally and send a notification to the client that we started a quest.
	 * @param questId
	 * @param npcId
	 */
	public void startQuest(short questId, int npcId) {
		byte error = localStartQuest(questId);
		if (error != 0) {
			//most likely an inventory full error - so no infraction
			getClient().getSession().send(GamePackets.writeQuestActionError(questId, error));
			return;
		}
		getClient().getSession().send(GamePackets.writeQuestProgress(questId, ""));
		getClient().getSession().send(GamePackets.writeQuestStartSuccess(questId, npcId));
	}

	public void updateStatusOfCompletedQuest(short questId, long completionTime) {
		QuestChecks qc = QuestDataLoader.getInstance().getCompleteReqs(questId);
		writeLockQuests();
		try {
			Set<Integer> reqMobs;
			if (qc != null) {
				Map<Integer, Short> reqMobCounts = qc.getReqMobCounts();
				reqMobs = reqMobCounts.keySet();
				for (Integer mobId : reqMobs)
					removeFromWatchedList(questId, QuestRequirementType.MOB, mobId);
				for (QuestItemStats item : qc.getReqItems())
					removeFromWatchedList(questId, QuestRequirementType.ITEM, Integer.valueOf(item.getItemId()));
				for (Integer petId : qc.getReqPets())
					removeFromWatchedList(questId, QuestRequirementType.PET, petId);
				for (Short reqQuestId : qc.getReqQuests().keySet())
					removeFromWatchedList(questId, QuestRequirementType.QUEST, reqQuestId);
				if (qc.requiresMesos())
					removeFromWatchedList(questId, QuestRequirementType.MESOS);
			} else {
				reqMobs = Collections.emptySet();
			}
			Short oId = Short.valueOf(questId);
			QuestEntry status = questStatuses.get(oId);
			if (status != null) {
				status.updateState(QuestEntry.STATE_COMPLETED);
			} else {
				status = new QuestEntry(QuestEntry.STATE_COMPLETED, reqMobs);
				questStatuses.put(oId, status);
			}
			status.setCompletionTime(completionTime);
		} finally {
			writeUnlockQuests();
		}

		//see if one req of another quest was completing this one...
		questStatusChanged(questId, QuestEntry.STATE_COMPLETED);
	}

	/**
	 * Only recognize that the quest is completed on the server without notifying the client
	 * @param questId
	 * @param selection
	 */
	public short localCompleteQuest(short questId, int selection) {
		short next = QuestDataLoader.getInstance().finishedQuest(this, questId, selection);
		if (next < 0)
			return next;

		updateStatusOfCompletedQuest(questId, System.currentTimeMillis());

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
		if (nextQuest < 0) {
			//most likely an inventory full error - so no infraction
			getClient().getSession().send(GamePackets.writeQuestActionError(questId, (byte) -nextQuest));
			return;
		}
		getClient().getSession().send(GamePackets.writeQuestComplete(questId, questStatuses.get(Short.valueOf(questId)).getCompletionTime()));
		getClient().getSession().send(GamePackets.writeQuestCompleteSuccess(questId, npcId, nextQuest));
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
		QuestChecks qc = QuestDataLoader.getInstance().getCompleteReqs(questId);
		writeLockQuests();
		try {
			Set<Integer> reqMobs;
			if (qc != null) {
				Map<Integer, Short> reqMobCounts = qc.getReqMobCounts();
				reqMobs = reqMobCounts.keySet();
				for (Integer mobId : reqMobs)
					removeFromWatchedList(questId, QuestRequirementType.MOB, mobId);
				for (QuestItemStats item : qc.getReqItems())
					removeFromWatchedList(questId, QuestRequirementType.ITEM, Integer.valueOf(item.getItemId()));
				for (Integer petId : qc.getReqPets())
					removeFromWatchedList(questId, QuestRequirementType.PET, petId);
				for (Short reqQuestId : qc.getReqQuests().keySet())
					removeFromWatchedList(questId, QuestRequirementType.QUEST, reqQuestId);
				if (qc.requiresMesos())
					removeFromWatchedList(questId, QuestRequirementType.MESOS);
			} else {
				reqMobs = Collections.emptySet();
			}
			//TODO: is there really a reason why we keep track of forfeited quests?
			//can't we just delete the quest status entirely?
			Short oId = Short.valueOf(questId);
			QuestEntry status = questStatuses.get(oId);
			if (status != null) {
				status.updateState(QuestEntry.STATE_NOT_STARTED);
			} else {
				status = new QuestEntry(QuestEntry.STATE_NOT_STARTED, reqMobs);
				questStatuses.put(oId, status);
			}
		} finally {
			writeUnlockQuests();
		}
		getClient().getSession().send(GamePackets.writeQuestForfeit(questId));
	}

	private void questStatusChanged(short questId, byte newStatus) {
		writeLockQuests();
		try {
			Short oId = Short.valueOf(questId);
			Map<Number, List<Short>> watchedQuests = questSubscriptions.get(QuestRequirementType.QUEST);
			if (watchedQuests == null)
				return;

			List<Short> watchingQuests = watchedQuests.get(Short.valueOf(questId));
			if (watchingQuests == null)
				return;

			for (Short quest : watchingQuests) {
				questId = quest.shortValue();
				//completeReqs can never be null because in order for us to
				//add something to questSubscriptions, it had to be not null
				Map<Short, Byte> questReq = QuestDataLoader.getInstance().getCompleteReqs(questId).getReqQuests();
				if (questReq.get(oId).byteValue() != newStatus || !canCompleteQuest(questId)) {
					completableQuests.remove(quest);
					continue;
				}

				if (!completableQuests.contains(quest)) {
					completableQuests.add(quest);
					getClient().getSession().send(GamePackets.writeShowQuestReqsFulfilled(questId));
				}
			}
		} finally {
			writeUnlockQuests();
		}
	}

	public void itemCountChanged(int itemId) {
		writeLockQuests();
		try {
			Map<Number, List<Short>> watchedItems = questSubscriptions.get(QuestRequirementType.ITEM);
			if (watchedItems == null)
				return;

			Integer oId = Integer.valueOf(itemId);
			List<Short> watchingQuests = watchedItems.get(oId);
			if (watchingQuests == null)
				return;

			for (Short quest : watchingQuests) {
				short questId = quest.shortValue();
				if (!canCompleteQuest(questId)) {
					completableQuests.remove(quest);
					continue;
				}

				if (!completableQuests.contains(quest)) {
					completableQuests.add(quest);
					getClient().getSession().send(GamePackets.writeShowQuestReqsFulfilled(questId));
				}
			}
		} finally {
			writeUnlockQuests();
		}
	}

	public void equippedPet(int petItemId) {
		writeLockQuests();
		try {
			Map<Number, List<Short>> watchedPets = questSubscriptions.get(QuestRequirementType.PET);
			if (watchedPets == null)
				return;

			List<Short> watchingQuests = watchedPets.get(Integer.valueOf(petItemId));
			if (watchingQuests == null)
				return;

			for (Short questId : watchingQuests) {
				if (!canCompleteQuest(questId.shortValue())) {
					completableQuests.remove(questId);
					continue;
				}

				if (!completableQuests.contains(questId)) {
					completableQuests.add(questId);
					getClient().getSession().send(GamePackets.writeShowQuestReqsFulfilled(questId.shortValue()));
				}
			}
		} finally {
			writeUnlockQuests();
		}
	}

	public void petGainedCloseness(int petItemId) {
		writeLockQuests();
		try {
			Map<Number, List<Short>> watchedPetTameness = questSubscriptions.get(QuestRequirementType.PET_TAMENESS);
			if (watchedPetTameness == null)
				return;

			List<Short> watchingQuests = watchedPetTameness.get(null);
			for (Short questId : watchingQuests) {
				if (!canCompleteQuest(questId.shortValue())) {
					completableQuests.remove(questId);
					continue;
				}

				if (!completableQuests.contains(questId)) {
					completableQuests.add(questId);
					getClient().getSession().send(GamePackets.writeShowQuestReqsFulfilled(questId.shortValue()));
				}
			}
		} finally {
			writeUnlockQuests();
		}
	}

	private void mesosChanged() {
		writeLockQuests();
		try {
			Map<Number, List<Short>> watchingMesos = questSubscriptions.get(QuestRequirementType.MESOS);
			if (watchingMesos == null)
				return;

			List<Short> watchingQuests = watchingMesos.get(null);
			for (Short questId : watchingQuests) {
				if (!canCompleteQuest(questId.shortValue())) {
					completableQuests.remove(questId);
					continue;
				}

				if (!completableQuests.contains(questId)) {
					completableQuests.add(questId);
					getClient().getSession().send(GamePackets.writeShowQuestReqsFulfilled(questId.shortValue()));
				}
			}
		} finally {
			writeUnlockQuests();
		}
	}

	public void mobKilled(int mobId) {
		writeLockQuests();
		try {
			Map<Number, List<Short>> watchedMobs = questSubscriptions.get(QuestRequirementType.MOB);
			if (watchedMobs == null)
				return;

			Integer oId = Integer.valueOf(mobId);
			List<Short> watchingQuests = watchedMobs.get(oId);
			if (watchingQuests == null)
				return;

			List<Short> mobReqCompleteQuests = new ArrayList<Short>();
			for (Short questId : watchingQuests) {
				QuestEntry status = questStatuses.get(questId);
				short mobReq = QuestDataLoader.getInstance().getCompleteReqs(questId.shortValue()).getReqMobCounts().get(oId);
				int progress = status.mobKilled(oId, mobReq);
				if (progress == mobReq)
					mobReqCompleteQuests.add(questId);
				getClient().getSession().send(GamePackets.writeQuestProgress(questId.shortValue(), status.getData()));
				if (progress != mobReq || !canCompleteQuest(questId.shortValue())) {
					//completableQuests should not contain questId (mob progress
					//cannot be undone), but remove it here anyway just for symmetry
					completableQuests.remove(questId);
					continue;
				}

				//completableQuests should not contain questId (mob progress
				//cannot be undone), but check it here anyway just for symmetry
				if (!completableQuests.contains(questId)) {
					completableQuests.add(questId);
					getClient().getSession().send(GamePackets.writeShowQuestReqsFulfilled(questId.shortValue()));
				}
			}
			for (Short questId : mobReqCompleteQuests)
				//mob progress cannot be undone, so it's safe to do this
				removeFromWatchedList(questId.shortValue(), QuestRequirementType.MOB, oId);
		} finally {
			writeUnlockQuests();
		}
	}

	public MobDeathListener getMobDeathListener(final int mobId) {
		final WeakReference<GameCharacter> futureSelf = new WeakReference<GameCharacter>(this);
		final int mobMap = getMapId();
		return new MobDeathListener() {
			@Override
			public void monsterKilled(GameCharacter highestDamager, GameCharacter last) {
				GameCharacter ourself = futureSelf.get();
				if (ourself == null || ourself.isClosed()
						|| highestDamager != ourself
						//I think we had to be the highest damager and
						//we had to kill it to be recognized for the kill
						|| last != highestDamager
						|| ourself.getMapId() != mobMap)
					return;

				if (party == null)
					ourself.mobKilled(mobId);
				else
					for (GameCharacter mem : party.getLocalMembersInMap(mobMap))
						mem.mobKilled(mobId);
			}
		};
	}

	/**
	 * Quests must be at least read locked while the returned Map is in scope.
	 * @return 
	 */
	public Map<Number, List<Short>> getQuestItems() {
		return questSubscriptions.get(QuestRequirementType.ITEM);
	}

	/**
	 * 
	 * @param questId
	 * @return false if quest is completed, fofeited, or untouched.
	 */
	public boolean isQuestStarted(short questId) {
		readLockQuests();
		try {
			QuestEntry status = questStatuses.get(Short.valueOf(questId));
			return status != null && status.getState() == QuestEntry.STATE_STARTED;
		} finally {
			readUnlockQuests();
		}
	}

	/**
	 *
	 * @param questId
	 * @return false if the quest is not started (i.e. is completed, forfeited,
	 * or just plain untouched) or if it is started but meets all completion
	 * requirements and can be completed right now.
	 */
	public boolean isQuestActive(short questId) {
		return isQuestStarted(questId) && !canCompleteQuest(questId);
	}

	public boolean isQuestCompleted(short questId) {
		readLockQuests();
		try {
			QuestEntry status = questStatuses.get(Short.valueOf(questId));
			return status != null && status.getState() == QuestEntry.STATE_COMPLETED;
		} finally {
			readUnlockQuests();
		}
	}

	public boolean isQuestInactive(short questId) {
		readLockQuests();
		try {
			QuestEntry status = questStatuses.get(Short.valueOf(questId));
			return status == null || status.getState() == QuestEntry.STATE_NOT_STARTED;
		} finally {
			readUnlockQuests();
		}
	}

	public int getMinigamePoints(MiniroomType game, MinigameResult stat) {
		Map<MinigameResult, AtomicInteger> stats = minigameStats.get(game);
		return stats != null ? stats.get(stat).get() : 0;
	}

	public void incrementMinigamePoints(MiniroomType game, MinigameResult stat) {
		Map<MinigameResult, AtomicInteger> stats;
		synchronized(minigameStats) {
			stats = minigameStats.get(game);
			if (stats == null) {
				stats = new EnumMap<MinigameResult, AtomicInteger>(MinigameResult.class);
				stats.put(MinigameResult.WIN, new AtomicInteger(0));
				stats.put(MinigameResult.TIE, new AtomicInteger(0));
				stats.put(MinigameResult.LOSS, new AtomicInteger(0));
				minigameStats.put(game, Collections.unmodifiableMap(stats));
			}
		}
		stats.get(stat).incrementAndGet();
	}

	public long getLastFameGivenTime() {
		return lastFameGiven;
	}

	public boolean canGiveFameToPlayer(int receiver) {
		synchronized(famesThisMonth) {
			Long lastTime = famesThisMonth.get(Integer.valueOf(receiver));
			if (lastTime == null)
				return true;
			//given time >= now - 30 days
			if (lastTime.longValue() >= System.currentTimeMillis() - 1000L * 60 * 60 * 24 * 30)
				return false;
			famesThisMonth.remove(Integer.valueOf(receiver));
			return true;
		}
	}

	public void gaveFame(int receiver) {
		long now = System.currentTimeMillis();
		famesThisMonth.put(Integer.valueOf(receiver), Long.valueOf(now));
		lastFameGiven = now;
	}

	public List<Integer> getWishListSerialNumbers() {
		return Collections.unmodifiableList(wishList);
	}

	public void setEvent(EventManipulator event) {
		this.event = event;
	}

	public EventManipulator getEvent() {
		return event;
	}

	@Override
	public EntityType getEntityType() {
		return EntityType.PLAYER;
	}

	@Override
	public Point getPosition() {
		return pos;
	}

	@Override
	public void setPosition(Point newPos) {
		pos = newPos;
	}

	@Override
	public byte getStance() {
		return stance;
	}

	@Override
	public void setStance(byte newStance) {
		stance = newStance;
	}

	@Override
	public short getFoothold() {
		return foothold;
	}

	@Override
	public void setFoothold(short newFh) {
		foothold = newFh;
	}

	@Override
	public boolean isAlive() {
		return remHp > 0;
	}

	@Override
	public boolean isVisible() {
		return !isEffectActive(PlayerStatusEffect.HIDE);
	}

	@Override
	public byte[] getShowNewSpawnMessage() {
		return GamePackets.writeShowPlayer(this);
	}

	@Override
	public byte[] getShowExistingSpawnMessage() {
		return getShowNewSpawnMessage();
	}

	@Override
	public byte[] getDestructionMessage() {
		return GamePackets.writeRemovePlayer(this);
	}

	@Override
	public String toString() {
		return "[Player: " + getName() + ']';
	}
}
