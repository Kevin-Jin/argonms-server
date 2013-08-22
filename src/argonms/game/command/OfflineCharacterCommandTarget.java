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

package argonms.game.command;

import argonms.common.GlobalConstants;
import argonms.common.character.Player;
import argonms.common.character.inventory.Inventory;
import argonms.common.character.inventory.InventoryTools;
import argonms.common.character.inventory.Pet;
import argonms.common.net.external.CheatTracker;
import argonms.common.util.DatabaseManager;
import argonms.common.util.TimeTool;
import argonms.game.character.ExpTables;
import argonms.game.character.MapMemoryVariable;
import argonms.game.loading.map.MapDataLoader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author GoldenKevin
 */
public class OfflineCharacterCommandTarget implements CommandTarget {
	private static final Logger LOG = Logger.getLogger(OfflineCharacterCommandTarget.class.getName());

	private final String target;
	private Connection con;
	private PreparedStatement ps;
	private ResultSet rs;

	public OfflineCharacterCommandTarget(String name) {
		target = name;
	}

	private void setValueInCharactersTable(String column, short value) throws SQLException {
		ps = con.prepareStatement("UPDATE `characters` SET `" + column + "` = ? WHERE `name` = ?");
		ps.setShort(1, value);
		ps.setString(2, target);
		ps.executeUpdate();
		ps.close();
	}

	private void addValueInCharactersTable(String column, short value, short max) throws SQLException {
		ps = con.prepareStatement("UPDATE `characters` SET `" + column + "` = LEAST(CAST(`" + column + "` AS UNSIGNED) + ?, ?) WHERE `name` = ?");
		ps.setShort(1, value);
		ps.setShort(2, max);
		ps.setString(3, target);
		ps.executeUpdate();
		ps.close();
	}

	private void setValueInCharactersTable(String column, int value) throws SQLException {
		ps = con.prepareStatement("UPDATE `characters` SET `" + column + "` = ? WHERE `name` = ?");
		ps.setInt(1, value);
		ps.setString(2, target);
		ps.executeUpdate();
		ps.close();
	}

	private void addValueInCharactersTable(String column, int value, int max) throws SQLException {
		ps = con.prepareStatement("UPDATE `characters` SET `" + column + "` = LEAST(CAST(`" + column + "` AS UNSIGNED) + ?, ?) WHERE `name` = ?");
		ps.setInt(1, value);
		ps.setInt(2, max);
		ps.setString(3, target);
		ps.executeUpdate();
		ps.close();
	}

	private byte getByteValueInCharactersTable(String column) throws SQLException {
		byte val;

		ps = con.prepareStatement("SELECT `" + column + "` FROM `characters` WHERE `name` = ?");
		ps.setString(1, target);
		rs = ps.executeQuery();
		rs.next(); //assert this is true
		val = rs.getByte(1);
		rs.close();
		ps.close();

		return val;
	}

	private int getIntValueInCharactersTable(String column) throws SQLException {
		int val;

		ps = con.prepareStatement("SELECT `" + column + "` FROM `characters` WHERE `name` = ?");
		ps.setString(1, target);
		rs = ps.executeQuery();
		rs.next(); //assert this is true
		val = rs.getInt(1);
		rs.close();
		ps.close();

		return val;
	}

	private short getShortValueInCharactersTable(String column) throws SQLException {
		short val;

		ps = con.prepareStatement("SELECT `" + column + "` FROM `characters` WHERE `name` = ?");
		ps.setString(1, target);
		rs = ps.executeQuery();
		rs.next(); //assert this is true
		val = rs.getShort(1);
		rs.close();
		ps.close();

		return val;
	}

	private short getTotalEquipBonus(String column) throws SQLException {
		short val;

		ps = con.prepareStatement("SELECT LEAST(SUM(`e`.`" + column + "`), " + Short.MAX_VALUE + ") FROM `inventoryequipment` `e` "
				+ "LEFT JOIN `inventoryitems` `i` ON `i`.`inventoryitemid` = `e`.`inventoryitemid` "
				+ "LEFT JOIN `characters` `c` ON `i`.`characterid` = `c`.`id` "
				+ "WHERE `c`.`name` = ? AND `i`.`inventorytype` = " + Inventory.InventoryType.EQUIPPED.byteValue());
		ps.setString(1, target);
		rs = ps.executeQuery();
		rs.next(); //assert this is true
		val = rs.getShort(1);
		rs.close();
		ps.close();

		return val;
	}

	private int getInt(CharacterManipulation update) {
		return ((Integer) update.getValue()).intValue();
	}

	private short getShort(CharacterManipulation update) {
		return ((Short) update.getValue()).shortValue();
	}

	@Override
	public void mutate(List<CharacterManipulation> updates) {
		int prevTransactionIsolation = Connection.TRANSACTION_REPEATABLE_READ;
		boolean prevAutoCommit = true;
		try {
			con = DatabaseManager.getConnection(DatabaseManager.DatabaseType.STATE);
			prevTransactionIsolation = con.getTransactionIsolation();
			prevAutoCommit = con.getAutoCommit();
			con.setTransactionIsolation(Connection.TRANSACTION_READ_UNCOMMITTED);
			con.setAutoCommit(false);

			for (CharacterManipulation update : updates) {
				switch (update.getKey()) {
					case CHANGE_MAP: {
						MapValue value = (MapValue) update.getValue();

						if (value.mapId / 100 == MapValue.FREE_MARKET_MAP_ID / 100 || value.mapId == MapValue.JAIL_MAP_ID) {
							int map = getIntValueInCharactersTable("map");
							byte spawnPoint = getByteValueInCharactersTable("spawnpoint");

							ps = con.prepareStatement("INSERT INTO `mapmemory` (`characterid`,`key`,`value`,`spawnpoint`) VALUES (?,?,?,?) ON DUPLICATE KEY UPDATE `characterid` = `characterid`");
							ps.setInt(1, Player.getIdFromName(target));
							ps.setString(2, value.mapId == MapValue.JAIL_MAP_ID ? MapMemoryVariable.JAIL.toString() : MapMemoryVariable.FREE_MARKET.toString());
							ps.setInt(3, map);
							ps.setByte(4, spawnPoint);
							ps.executeUpdate();
							ps.close();
						}

						ps = con.prepareStatement("UPDATE `characters` SET `map` = ?, `spawnpoint` = ? WHERE `name` = ?");
						ps.setInt(1, value.mapId);
						ps.setByte(2, value.spawnPoint);
						ps.setString(3, target);
						ps.executeUpdate();
						ps.close();
						break;
					}
					case CHANGE_CHANNEL:
						throw new UnsupportedOperationException("Cannot change channel of offline player");
					case ADD_LEVEL: {
						addValueInCharactersTable("level", getShort(update), GlobalConstants.MAX_LEVEL);
						short level = getShortValueInCharactersTable("level");
						//clip exp in case we subtracted levels or reached max level
						addValueInCharactersTable("exp", 0, level < GlobalConstants.MAX_LEVEL ? ExpTables.getExpForPlayerLevel(level) - 1 : 0);
						break;
					}
					case SET_LEVEL: {
						short level = getShort(update);
						setValueInCharactersTable("level", level);
						//clip exp in case we subtracted levels or reached max level
						addValueInCharactersTable("exp", 0, level < GlobalConstants.MAX_LEVEL ? ExpTables.getExpForPlayerLevel(level) - 1 : 0);
						break;
					}
					case SET_JOB:
						setValueInCharactersTable("job", getShort(update));
						break;
					case ADD_STR:
						addValueInCharactersTable("str", getShort(update), Short.MAX_VALUE);
						break;
					case SET_STR:
						setValueInCharactersTable("str", getShort(update));
						break;
					case ADD_DEX:
						addValueInCharactersTable("dex", getShort(update), Short.MAX_VALUE);
						break;
					case SET_DEX:
						setValueInCharactersTable("dex", getShort(update));
						break;
					case ADD_INT:
						addValueInCharactersTable("int", getShort(update), Short.MAX_VALUE);
						break;
					case SET_INT:
						setValueInCharactersTable("int", getShort(update));
						break;
					case ADD_LUK:
						addValueInCharactersTable("luk", getShort(update), Short.MAX_VALUE);
						break;
					case SET_LUK:
						setValueInCharactersTable("luk", getShort(update));
						break;
					case ADD_AP:
						addValueInCharactersTable("ap", getShort(update), Short.MAX_VALUE);
						break;
					case SET_AP:
						setValueInCharactersTable("ap", getShort(update));
						break;
					case ADD_SP:
						addValueInCharactersTable("sp", getShort(update), Short.MAX_VALUE);
						break;
					case SET_SP:
						setValueInCharactersTable("sp", getShort(update));
						break;
					case ADD_MAX_HP:
						addValueInCharactersTable("maxhp", getShort(update), 30000);
						break;
					case SET_MAX_HP:
						setValueInCharactersTable("maxhp", getShort(update));
						break;
					case ADD_MAX_MP:
						addValueInCharactersTable("maxmp", getShort(update), 30000);
						break;
					case SET_MAX_MP:
						setValueInCharactersTable("maxmp", getShort(update));
						break;
					case ADD_HP: {
						short maxHp = (short) Math.min(getShortValueInCharactersTable("maxhp") + getTotalEquipBonus("hp"), 30000);
						addValueInCharactersTable("hp", getShort(update), maxHp);
						break;
					}
					case SET_HP: {
						short maxHp = (short) Math.min(getShortValueInCharactersTable("maxhp") + getTotalEquipBonus("hp"), 30000);
						setValueInCharactersTable("hp", (short) Math.min(getShort(update), maxHp));
						break;
					}
					case ADD_MP: {
						short maxMp = (short) Math.min(getShortValueInCharactersTable("maxmp") + getTotalEquipBonus("mp"), 30000);
						addValueInCharactersTable("mp", getShort(update), maxMp);
						break;
					}
					case SET_MP: {
						short maxMp = (short) Math.min(getShortValueInCharactersTable("maxmp") + getTotalEquipBonus("mp"), 30000);
						setValueInCharactersTable("mp", (short) Math.min(getShort(update), maxMp));
						break;
					}
					case ADD_FAME:
						addValueInCharactersTable("fame", getShort(update), Short.MAX_VALUE);
						break;
					case SET_FAME:
						setValueInCharactersTable("fame", getShort(update));
						break;
					case ADD_EXP: {
						short level = getShortValueInCharactersTable("level");
						addValueInCharactersTable("exp", getInt(update), level < GlobalConstants.MAX_LEVEL ? ExpTables.getExpForPlayerLevel(level) - 1 : 0);
						break;
					}
					case SET_EXP: {
						setValueInCharactersTable("exp", getInt(update));
						short level = getShortValueInCharactersTable("level");
						addValueInCharactersTable("exp", 0, level < GlobalConstants.MAX_LEVEL ? ExpTables.getExpForPlayerLevel(level) - 1 : 0);
						break;
					}
					case ADD_MESO:
						addValueInCharactersTable("mesos", getInt(update), Integer.MAX_VALUE);
						break;
					case SET_MESO:
						setValueInCharactersTable("mesos", getInt(update));
						break;
					case SET_SKILL_LEVEL: {
						SkillValue value = (SkillValue) update.getValue();
						int characterId = Player.getIdFromName(target);

						ps = con.prepareStatement("DELETE FROM `skills` WHERE `characterid` = ? AND `skillid` = ?");
						ps.setInt(1, characterId);
						ps.setInt(2, value.skillId);
						ps.executeUpdate();
						ps.close();

						ps = con.prepareStatement("INSERT INTO `skills` (`characterid`,`skillid`,`level`,`mastery`) VALUES (?,?,?,?)");
						ps.setInt(1, characterId);
						ps.setInt(2, value.skillId);
						ps.setByte(3, value.skillLevel);
						ps.setByte(4, value.skillMasterLevel);
						ps.executeUpdate();
						ps.close();
						break;
					}
					case SET_QUEST_STATUS: {
						QuestStatusValue value = (QuestStatusValue) update.getValue();
						int characterId = Player.getIdFromName(target);

						ps = con.prepareStatement("DELETE FROM `queststatuses` WHERE `characterid` = ? AND `questid` = ?");
						ps.setInt(1, characterId);
						ps.setShort(2, value.questId);
						ps.executeUpdate();
						ps.close();

						ps = con.prepareStatement("INSERT INTO `queststatuses` (`characterid`,`questid`,`state`,`completed`) VALUES (?,?,?,?)");
						ps.setInt(1, characterId);
						ps.setShort(2, value.questId);
						ps.setByte(3, value.status);
						ps.setLong(4, value.completionTime);
						ps.executeUpdate();
						ps.close();
						break;
					}
					case ADD_ITEM: {
						ItemValue value = (ItemValue) update.getValue();
						Inventory.InventoryType type = InventoryTools.getCategory(value.itemId);
						Pet[] pets = new Pet[3];

						ps = con.prepareStatement("SELECT `accountid`,`id`,`" + type.toString().toLowerCase() + "slots` FROM `characters` WHERE `name` = ?");
						ps.setString(1, target);
						rs = ps.executeQuery();
						rs.next(); //assert this is true
						int accountId = rs.getInt(1);
						int characterId = rs.getInt(2);
						Map<Inventory.InventoryType, Inventory> inventories = Collections.singletonMap(type, new Inventory(rs.getShort(2)));
						rs.close();
						ps.close();

						ps = con.prepareStatement("SELECT * FROM `inventoryitems` WHERE "
								+ "`characterid` = ? AND `inventorytype` = ?");
						ps.setInt(1, characterId);
						ps.setByte(2, type.byteValue());
						rs = ps.executeQuery();
						Player.loadInventory(pets, con, rs, inventories);
						rs.close();
						ps.close();

						if (value.quantity > 0) {
							InventoryTools.addToInventory(inventories.get(type), value.itemId, value.quantity);
						} else {
							Inventory inv = inventories.get(type);
							int quantity;
							if (value.quantity == Integer.MIN_VALUE) {
								quantity = InventoryTools.getAmountOfItem(inv, value.itemId);
								if (type == Inventory.InventoryType.EQUIP)
									quantity += InventoryTools.getAmountOfItem(inv, value.itemId);
							} else {
								quantity = -value.quantity;
							}
							InventoryTools.removeFromInventory(inv, value.itemId, quantity, true);
						}

						ps = con.prepareStatement("DELETE FROM `inventoryitems` WHERE "
								+ "`characterid` = ? AND `inventorytype` = ?");
						ps.setInt(1, characterId);
						ps.setByte(2, type.byteValue());
						ps.executeUpdate();
						ps.close();

						Player.commitInventory(characterId, accountId, pets, con, inventories);
						break;
					}
					case CANCEL_DEBUFFS:
						//offline characters don't have any active status effects
						break;
					case MAX_ALL_EQUIP_STATS:
						ps = con.prepareStatement("UPDATE `inventoryequipment` `e` "
								+ "LEFT JOIN `inventoryitems` `i` ON `i`.`inventoryitemid` = `e`.`inventoryitemid` "
								+ "LEFT JOIN `characters` `c` ON `i`.`characterid` = `c`.`id` "
								+ "SET "
								+ "`e`.`str` = " + Short.MAX_VALUE + ", "
								+ "`e`.`dex` = " + Short.MAX_VALUE + ", "
								+ "`e`.`int` = " + Short.MAX_VALUE + ", "
								+ "`e`.`luk` = " + Short.MAX_VALUE + ", "
								+ "`e`.`hp` = 30000, "
								+ "`e`.`mp` = 30000, "
								+ "`e`.`watk` = " + Short.MAX_VALUE + ", "
								+ "`e`.`matk` = " + Short.MAX_VALUE + ", "
								+ "`e`.`wdef` = " + Short.MAX_VALUE + ", "
								+ "`e`.`mdef` = " + Short.MAX_VALUE + ", "
								+ "`e`.`acc` = " + Short.MAX_VALUE + ", "
								+ "`e`.`avoid` = " + Short.MAX_VALUE + ", "
								+ "`e`.`hands` = " + Short.MAX_VALUE + ", "
								+ "`e`.`speed` = 40, "
								+ "`e`.`jump` = 23 "
								+ "WHERE `c`.`name` = ? AND `i`.`inventorytype` = " + Inventory.InventoryType.EQUIPPED.byteValue());
						ps.setString(1, target);
						ps.executeUpdate();
						ps.close();
						break;
					case MAX_INVENTORY_SLOTS:
						ps = con.prepareStatement("UPDATE `characters` SET "
								+ "`equipslots` = 255, `useslots` = 255, `setupslots` = 255, `etcslots` = 255, `cashslots` = 255 "
								+ "WHERE `name` = ?");
						ps.setString(1, target);
						ps.executeUpdate();
						ps.close();

						ps = con.prepareStatement("UPDATE `accounts` SET "
								+ "`storageslots` = 255 "
								+ "WHERE `id` = (SELECT `accountid` FROM `characters` WHERE `name` = ?)");
						ps.setString(1, target);
						ps.executeUpdate();
						ps.close();
						break;
					case MAX_BUDDY_LIST_SLOTS:
						ps = con.prepareStatement("UPDATE `characters` SET "
								+ "`buddyslots` = 255 WHERE `name` = ?");
						ps.setString(1, target);
						ps.executeUpdate();
						ps.close();
						break;
					case BAN: {
						BanValue value = (BanValue) update.getValue();
						Calendar cal = TimeTool.currentDateTime();
						cal.setTimeInMillis(value.expireTimestamp);
						CheatTracker.get(target).ban(CheatTracker.Infraction.POSSIBLE_PACKET_EDITING, value.banner, value.reason, cal);
						break;
					}
					case KICK: {
						//just make sure the character's login status is reset...
						ps = con.prepareStatement("UPDATE `accounts` `a` LEFT JOIN `characters` `c` ON `c`.`accountid` = `a`.`id` SET `connected` = 0 WHERE `c`.`name` = ?");
						ps.setString(1, target);
						ps.executeUpdate();
						ps.close();
						break;
					}
					case STUN:
						break;
					case CLEAR_INVENTORY_SLOTS: {
						InventorySlotRangeValue value = (InventorySlotRangeValue) update.getValue();
						Pet[] pets = new Pet[3];

						ps = con.prepareStatement("SELECT `accountid`,`id`,`" + value.type.toString().toLowerCase() + "slots` FROM `characters` WHERE `name` = ?");
						ps.setString(1, target);
						rs = ps.executeQuery();
						rs.next(); //assert this is true
						int accountId = rs.getInt(1);
						int characterId = rs.getInt(2);
						Map<Inventory.InventoryType, Inventory> inventories = Collections.singletonMap(value.type, new Inventory(rs.getShort(2)));
						rs.close();
						ps.close();

						ps = con.prepareStatement("SELECT * FROM `inventoryitems` WHERE "
								+ "`characterid` = ? AND `inventorytype` = ?");
						ps.setInt(1, characterId);
						ps.setByte(2, value.type.byteValue());
						rs = ps.executeQuery();
						Player.loadInventory(pets, con, rs, inventories);
						rs.close();
						ps.close();

						Inventory inv = inventories.get(value.type);
						short upperBound = (short) Math.min(value.endSlot, inv.getMaxSlots());
						for (short slot = value.startSlot; slot <= upperBound; slot++)
							inv.remove(slot);

						ps = con.prepareStatement("DELETE FROM `inventoryitems` WHERE "
								+ "`characterid` = ? AND `inventorytype` = ?");
						ps.setInt(1, characterId);
						ps.setByte(2, value.type.byteValue());
						ps.executeUpdate();
						ps.close();

						Player.commitInventory(characterId, accountId, pets, con, inventories);
						break;
					}
					case RETURN_TO_REMEMBERED_MAP: {
						MapMemoryVariable value = (MapMemoryVariable) update.getValue();
						int characterId = Player.getIdFromName(target);
						int mapId = GlobalConstants.NULL_MAP;
						byte spawnPoint = -1;

						ps = con.prepareStatement("SELECT `value`,`spawnpoint` FROM `mapmemory` WHERE `characterid` = ? AND `key` = ?");
						ps.setInt(1, characterId);
						ps.setString(2, value.toString());
						rs = ps.executeQuery();
						if (rs.next()) {
							mapId = rs.getInt(1);
							spawnPoint = rs.getByte(2);
						}
						rs.close();
						ps.close();

						if (mapId != GlobalConstants.NULL_MAP && spawnPoint != -1) {
							ps = con.prepareStatement("DELETE FROM `mapmemory` WHERE `characterid` = ? AND `key` = ?");
							ps.setInt(1, characterId);
							ps.setString(2, value.toString());
							ps.executeUpdate();
							ps.close();

							ps = con.prepareStatement("UPDATE `characters` SET `map` = ?, `spawnpoint` = ? WHERE `name` = ?");
							ps.setInt(1, mapId);
							ps.setByte(2, spawnPoint);
							ps.setString(3, target);
							ps.executeUpdate();
							ps.close();
						}
						break;
					}
				}
			}

			con.commit();
		} catch (Throwable e) {
			LOG.log(Level.WARNING, "Could not manipulate stat of offline character. Rolling back all changes...", e);
			if (con != null) {
				try {
					con.rollback();
				} catch (SQLException ex2) {
					LOG.log(Level.WARNING, "Error rolling back stat manipulations of offline character.", ex2);
				}
			}
		} finally {
			if (con != null) {
				try {
					con.setAutoCommit(prevAutoCommit);
					con.setTransactionIsolation(prevTransactionIsolation);
				} catch (SQLException ex) {
					LOG.log(Level.WARNING, "Could not reset Connection config after manipulating offline character " + target, ex);
				}
			}
			DatabaseManager.cleanup(DatabaseManager.DatabaseType.STATE, rs, ps, con);
		}
	}

	@Override
	public Object access(CharacterProperty key) {
		try {
			con = DatabaseManager.getConnection(DatabaseManager.DatabaseType.STATE);
			switch (key) {
				case MAP:
					return new MapValue(getIntValueInCharactersTable("map"), getByteValueInCharactersTable("spawnpoint"), (byte) 0);
				case CHANNEL:
					return Byte.valueOf((byte) 0);
				case POSITION:
					return MapDataLoader.getInstance().getMapStats(getIntValueInCharactersTable("map")).getPortals().get(Byte.valueOf(getByteValueInCharactersTable("spawnpoint"))).getPosition();
				case PLAYER_ID:
					return Integer.valueOf(Player.getIdFromName(target));
				default:
					return null;
			}
		} catch (SQLException e) {
			LOG.log(Level.WARNING, "Could not retrieve stat of offline character", e);
			return null;
		} finally {
			DatabaseManager.cleanup(DatabaseManager.DatabaseType.STATE, rs, ps, con);
		}
	}
}
