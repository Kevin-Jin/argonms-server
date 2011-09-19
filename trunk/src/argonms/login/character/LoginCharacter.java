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

package argonms.login.character;

import argonms.common.character.KeyBinding;
import argonms.common.character.Player.LimitedActionCharacter;
import argonms.common.character.inventory.Inventory;
import argonms.common.character.inventory.Inventory.InventoryType;
import argonms.common.character.inventory.InventoryTools;
import argonms.common.util.DatabaseManager;
import argonms.common.util.DatabaseManager.DatabaseType;
import argonms.login.net.external.LoginClient;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author GoldenKevin
 */
public class LoginCharacter extends LimitedActionCharacter {
	private static final Logger LOG = Logger.getLogger(LoginCharacter.class.getName());

	private static final Map<Byte, KeyBinding> defaultBindings;

	static {
		Map<Byte, KeyBinding> bindings = new HashMap<Byte, KeyBinding>();
		bindings.put(Byte.valueOf((byte) 2), new KeyBinding((byte) 4, 10));
		bindings.put(Byte.valueOf((byte) 3), new KeyBinding((byte) 4, 12));
		bindings.put(Byte.valueOf((byte) 4), new KeyBinding((byte) 4, 13));
		bindings.put(Byte.valueOf((byte) 5), new KeyBinding((byte) 4, 18));
		bindings.put(Byte.valueOf((byte) 6), new KeyBinding((byte) 4, 21));
		bindings.put(Byte.valueOf((byte) 16), new KeyBinding((byte) 4, 8));
		bindings.put(Byte.valueOf((byte) 17), new KeyBinding((byte) 4, 5));
		bindings.put(Byte.valueOf((byte) 18), new KeyBinding((byte) 4, 0));
		bindings.put(Byte.valueOf((byte) 19), new KeyBinding((byte) 4, 4));
		bindings.put(Byte.valueOf((byte) 23), new KeyBinding((byte) 4, 1));
		bindings.put(Byte.valueOf((byte) 25), new KeyBinding((byte) 4, 19));
		bindings.put(Byte.valueOf((byte) 26), new KeyBinding((byte) 4, 14));
		bindings.put(Byte.valueOf((byte) 27), new KeyBinding((byte) 4, 15));
		bindings.put(Byte.valueOf((byte) 29), new KeyBinding((byte) 5, 52));
		bindings.put(Byte.valueOf((byte) 31), new KeyBinding((byte) 4, 2));
		bindings.put(Byte.valueOf((byte) 34), new KeyBinding((byte) 4, 17));
		bindings.put(Byte.valueOf((byte) 35), new KeyBinding((byte) 4, 11));
		bindings.put(Byte.valueOf((byte) 37), new KeyBinding((byte) 4, 3));
		bindings.put(Byte.valueOf((byte) 38), new KeyBinding((byte) 4, 20));
		bindings.put(Byte.valueOf((byte) 40), new KeyBinding((byte) 4, 16));
		bindings.put(Byte.valueOf((byte) 43), new KeyBinding((byte) 4, 9));
		bindings.put(Byte.valueOf((byte) 44), new KeyBinding((byte) 5, 50));
		bindings.put(Byte.valueOf((byte) 45), new KeyBinding((byte) 5, 51));
		bindings.put(Byte.valueOf((byte) 46), new KeyBinding((byte) 4, 6));
		bindings.put(Byte.valueOf((byte) 50), new KeyBinding((byte) 4, 7));
		bindings.put(Byte.valueOf((byte) 56), new KeyBinding((byte) 5, 53));
		bindings.put(Byte.valueOf((byte) 59), new KeyBinding((byte) 6, 100));
		bindings.put(Byte.valueOf((byte) 60), new KeyBinding((byte) 6, 101));
		bindings.put(Byte.valueOf((byte) 61), new KeyBinding((byte) 6, 102));
		bindings.put(Byte.valueOf((byte) 62), new KeyBinding((byte) 6, 103));
		bindings.put(Byte.valueOf((byte) 63), new KeyBinding((byte) 6, 104));
		bindings.put(Byte.valueOf((byte) 64), new KeyBinding((byte) 6, 105));
		bindings.put(Byte.valueOf((byte) 65), new KeyBinding((byte) 6, 106));
		defaultBindings = Collections.unmodifiableMap(bindings);
	}

	private LoginClient client;

	private Map<Byte, KeyBinding> bindings;

	private int worldRanking, worldRankingChange;
	private int jobRanking, jobRankingChange;

	private LoginCharacter() {
		super();
	}

	@Override
	public LoginClient getClient() {
		return client;
	}

	public int getWorldRank() {
		return worldRanking;
	}

	public int getWorldRankChange() {
		return worldRankingChange;
	}

	public int getJobRank() {
		return jobRanking;
	}

	public int getJobRankChange() {
		return jobRankingChange;
	}

	public static LoginCharacter loadPlayer(LoginClient c, int id) {
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
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
			if (accountid != c.getAccountId()) { //we are aware of our accountid
				LOG.log(Level.WARNING, "Client account {0} is trying to load "
						+ "character {1} which belongs to account {2}",
						new Object[] { c.getAccountId(), id, accountid });
				return null;
			}
			byte world = rs.getByte(2);
			if (world != c.getWorld()) { //we are aware of our world
				LOG.log(Level.WARNING, "Client account {0} is trying to load "
						+ "character {1} on world {2} but exists on world {3}",
						new Object[] { accountid, id, c.getWorld(), world });
				return null;
			}
			LoginCharacter p = new LoginCharacter();
			p.client = c;
			loadPlayerStats(rs, id, p);
			p.worldRanking = rs.getInt(36);
			p.worldRankingChange = rs.getInt(37) - p.worldRanking;
			p.jobRanking = rs.getInt(38);
			p.jobRankingChange = rs.getInt(39) - p.jobRanking;
			rs.close();
			ps.close();

			ps = con.prepareStatement("SELECT * FROM `inventoryitems` WHERE `characterid` = ? "
					+ "AND `inventorytype` <= " + InventoryType.CASH.byteValue());
			ps.setInt(1, id);
			rs = ps.executeQuery();
			CharacterTools.loadInventory(con, rs, p.getPets(), p.getInventories());
			return p;
		} catch (SQLException ex) {
			LOG.log(Level.WARNING, "Could not load character " + id + " from database", ex);
			return null;
		} finally {
			DatabaseManager.cleanup(DatabaseType.STATE, rs, ps, con);
		}
	}

	private void updateDbInventory(Connection con) throws SQLException {
		String invUpdate = "DELETE FROM `inventoryitems` WHERE "
				+ "`characterid` = ? AND `inventorytype` <= "
				+ InventoryType.CASH.byteValue();
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = con.prepareStatement(invUpdate);
			ps.setInt(1, getDataId());
			ps.executeUpdate();
			ps.close();

			CharacterTools.commitInventory(con, getId(), client.getAccountId(), getPets(), getInventories());
		} catch (SQLException e) {
			throw new SQLException("Failed to save inventory of character " + getName(), e);
		} finally {
			DatabaseManager.cleanup(DatabaseType.STATE, rs, ps, null);
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
		} catch (SQLException e) {
			throw new SQLException("Failed to save keymap of character " + getName(), e);
		} finally {
			DatabaseManager.cleanup(DatabaseType.STATE, null, ps, null);
		}
	}

	public static LoginCharacter saveNewPlayer(LoginClient account, String name,
			int eyes, int hair, int skin, byte gender, byte str, byte dex,
			byte _int, byte luk, int top, int bottom, int shoes, int weapon) {
		LoginCharacter p = new LoginCharacter();
		p.client = account;
		p.setName(name);
		p.setEyes((short) eyes);
		p.setHair((short) hair);
		p.setSkin((byte) skin);
		p.setGender(gender);
		p.setStr(str);
		p.setDex(dex);
		p.setInt(_int);
		p.setLuk(luk);
		p.setLevel((byte) 1);
		p.setGm(account.getGm());

		Map<InventoryType, Inventory> inventories = new EnumMap<InventoryType, Inventory>(InventoryType.class);
		Inventory equipment = new Inventory((short) 24);
		Inventory equipped = new Inventory((short) 0);
		Inventory etc = new Inventory((short) 24);
		inventories.put(InventoryType.EQUIP, equipment);
		inventories.put(InventoryType.USE, new Inventory((short) 24));
		inventories.put(InventoryType.SETUP, new Inventory((short) 24));
		inventories.put(InventoryType.ETC, etc);
		inventories.put(InventoryType.CASH, new Inventory((short) 24));
		//TODO: get real equipped inventory size?
		inventories.put(InventoryType.EQUIPPED, equipped);
		InventoryTools.equip(equipment, equipped,
				InventoryTools.addToInventory(equipment, top, 1)
				.addedOrRemovedSlots.get(0).shortValue(), (short) -5);
		InventoryTools.equip(equipment, equipped,
				InventoryTools.addToInventory(equipment, bottom, 1)
				.addedOrRemovedSlots.get(0).shortValue(), (short) -6);
		InventoryTools.equip(equipment, equipped,
				InventoryTools.addToInventory(equipment, shoes, 1)
				.addedOrRemovedSlots.get(0).shortValue(), (short) -7);
		InventoryTools.equip(equipment, equipped,
				InventoryTools.addToInventory(equipment, weapon, 1)
				.addedOrRemovedSlots.get(0).shortValue(), (short) -11);
		InventoryTools.addToInventory(etc, 4161001, 1);
		p.addInventories(inventories);

		p.bindings = defaultBindings;

		int prevTransactionIsolation = Connection.TRANSACTION_REPEATABLE_READ;
		boolean prevAutoCommit = true;
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			con = DatabaseManager.getConnection(DatabaseType.STATE);
			prevTransactionIsolation = con.getTransactionIsolation();
			prevAutoCommit = con.getAutoCommit();
			con.setTransactionIsolation(Connection.TRANSACTION_READ_UNCOMMITTED);
			con.setAutoCommit(false);
			ps = con.prepareStatement("INSERT INTO"
					+ "`characters`(`accountid`,`world`,`name`,`gender`,`skin`,"
					+ "`eyes`,`hair`,`str`,`dex`,`int`,`luk`,`gm`) VALUES"
					+ " (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
					Statement.RETURN_GENERATED_KEYS);
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
			rs = ps.getGeneratedKeys();
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
						+ "after creating character " + p.getDataId(), ex);
			}
			DatabaseManager.cleanup(DatabaseType.STATE, rs, ps, con);
		}

		return p;
	}
}
