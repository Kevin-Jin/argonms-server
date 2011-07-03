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

import argonms.common.character.Cooldown;
import argonms.common.character.Player.LimitedActionCharacter;
import argonms.common.character.Player.LoggedInPlayer;
import argonms.common.character.QuestEntry;
import argonms.common.character.SkillEntry;
import argonms.common.character.inventory.Inventory;
import argonms.common.character.inventory.Inventory.InventoryType;
import argonms.common.net.external.CommonPackets;
import argonms.common.tools.DatabaseManager;
import argonms.common.tools.DatabaseManager.DatabaseType;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author GoldenKevin
 */
public class ShopCharacter extends LimitedActionCharacter implements LoggedInPlayer {
	private static final Logger LOG = Logger.getLogger(ShopCharacter.class.getName());

	private ShopClient client;

	private short maxBuddies;
	private int mesos;
	private final Map<Integer, SkillEntry> skills;
	private final Map<Integer, Cooldown> cooldowns;
	private final Map<Short, QuestEntry> questStatuses;

	private ShopCharacter() {
		super();
		skills = new HashMap<Integer, SkillEntry>();
		cooldowns = new HashMap<Integer, Cooldown>();
		questStatuses = new HashMap<Short, QuestEntry>();
	}

	public ShopClient getClient() {
		return client;
	}

	public byte getBuddyListCapacity() {
		return (byte) maxBuddies;
	}

	public int getMesos() {
		return mesos;
	}

	public Map<Integer, SkillEntry> getSkillEntries() {
		return Collections.unmodifiableMap(skills);
	}

	public Map<Integer, Cooldown> getCooldowns() {
		return Collections.unmodifiableMap(cooldowns);
	}

	public Map<Short, QuestEntry> getAllQuests() {
		return Collections.unmodifiableMap(questStatuses);
	}

	public void close() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public void removeCooldown(int skill) {
		cooldowns.remove(Integer.valueOf(skill)).cancel();
	}

	private void addCooldown(final int skill, short time) {
		cooldowns.put(Integer.valueOf(skill), new Cooldown(time * 1000, new Runnable() {
			public void run() {
				removeCooldown(skill);
				getClient().getSession().send(CommonPackets.writeCooldown(skill, (short) 0));
			}
		}));
	}

	public static ShopCharacter loadPlayer(ShopClient c, int id) {
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
			c.setAccountId(accountid); //we aren't aware of our accountid yet
			byte world = rs.getByte(2);
			c.setWorld(world); //we aren't aware of our world yet
			ShopCharacter p = new ShopCharacter();
			p.client = c;
			loadPlayerStats(rs, id, p, InventoryType.CASH_SHOP, new Inventory((byte) 0));
			p.mesos = rs.getInt(26);
			p.maxBuddies = rs.getShort(33);
			rs.close();
			ps.close();

			ps = con.prepareStatement("SELECT * FROM `inventoryitems` WHERE `characterid` = ?"
					+ " AND `inventorytype` <= " + InventoryType.CASH.byteValue()
					+ " OR `accountid` = ? AND `inventorytype` = " + InventoryType.CASH_SHOP.byteValue());
			ps.setInt(1, id);
			ps.setInt(2, accountid);
			rs = ps.executeQuery();
			loadInventory(con, rs, p);

			ps = con.prepareStatement("SELECT `skillid`,`level`,`mastery` "
					+ "FROM `skills` WHERE `characterid` = ?");
			ps.setInt(1, id);
			rs = ps.executeQuery();
			while (rs.next())
				p.skills.put(Integer.valueOf(rs.getInt(1)),
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
				} finally {
					DatabaseManager.cleanup(DatabaseType.STATE, mrs, mps, null);
				}
			}
			return p;
		} catch (SQLException ex) {
			LOG.log(Level.WARNING, "Could not load character " + id + " from database", ex);
			return null;
		} finally {
			DatabaseManager.cleanup(DatabaseType.STATE, rs, ps, con);
		}
	}
}
