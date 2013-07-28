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

package argonms.shop.character;

import argonms.common.character.BuddyListEntry;
import argonms.common.character.Cooldown;
import argonms.common.character.LoggedInPlayer;
import argonms.common.character.QuestEntry;
import argonms.common.character.SkillEntry;
import argonms.common.character.inventory.IInventory;
import argonms.common.character.inventory.Inventory;
import argonms.common.character.inventory.Inventory.InventoryType;
import argonms.common.net.external.CommonPackets;
import argonms.common.util.DatabaseManager;
import argonms.common.util.DatabaseManager.DatabaseType;
import argonms.shop.net.external.ShopClient;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author GoldenKevin
 */
public class ShopCharacter extends LoggedInPlayer {
	private static final Logger LOG = Logger.getLogger(ShopCharacter.class.getName());

	private ShopClient client;

	private ShopBuddyList buddies;
	private int partyId;
	private int guildId;
	private Inventory shopInventory;
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

	@Override
	public ShopClient getClient() {
		return client;
	}

	@Override
	public ShopBuddyList getBuddyList() {
		return buddies;
	}

	public int getPartyId() {
		return partyId;
	}

	public int getGuildId() {
		return guildId;
	}

	@Override
	public int getMesos() {
		return mesos;
	}

	@Override
	public Map<Integer, SkillEntry> getSkillEntries() {
		return Collections.unmodifiableMap(skills);
	}

	@Override
	public Map<Integer, Cooldown> getCooldowns() {
		return Collections.unmodifiableMap(cooldowns);
	}

	@Override
	public Map<Short, QuestEntry> getAllQuests() {
		return Collections.unmodifiableMap(questStatuses);
	}

	private void removeCooldown(int skill) {
		cooldowns.remove(Integer.valueOf(skill)).cancel();
	}

	private void addCooldown(final int skill, short time) {
		cooldowns.put(Integer.valueOf(skill), new Cooldown(time * 1000, new Runnable() {
			@Override
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
			ps = con.prepareStatement("SELECT `c`.*,`a`.`name` "
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
			c.setWorld(world); //we aren't aware of our world yet
			ShopCharacter p = new ShopCharacter();
			p.client = c;
			p.loadPlayerStats(rs, id);
			p.mesos = rs.getInt(26);
			short maxBuddies = rs.getShort(32);
			c.setAccountName(rs.getString(42));
			rs.close();
			ps.close();

			EnumMap<InventoryType, IInventory> invUnion = new EnumMap<InventoryType, IInventory>(p.getInventories());
			invUnion.put(InventoryType.CASH_SHOP, p.shopInventory);
			ps = con.prepareStatement("SELECT * FROM `inventoryitems` WHERE "
					+ "`characterid` = ? AND `inventorytype` <= " + InventoryType.CASH.byteValue()
					+ " OR `accountid` = ? AND `inventorytype` = " + InventoryType.CASH_SHOP.byteValue());
			ps.setInt(1, id);
			ps.setInt(2, accountid);
			rs = ps.executeQuery();
			p.loadInventory(con, rs, invUnion);
			rs.close();
			ps.close();

			ps = con.prepareStatement("SELECT `skillid`,`level`,`mastery` "
					+ "FROM `skills` WHERE `characterid` = ?");
			ps.setInt(1, id);
			rs = ps.executeQuery();
			while (rs.next())
				p.skills.put(Integer.valueOf(rs.getInt(1)), new SkillEntry(rs.getByte(2), rs.getByte(3)));
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

			List<BuddyListEntry> buddies = new ArrayList<BuddyListEntry>();
			ps = con.prepareStatement("SELECT `e`.`buddy` AS `id`,"
					+ "IF(ISNULL(`c`.`name`),`e`.`buddyname`,`c`.`name`) AS `name`,`e`.`status` "
					+ "FROM `buddyentries` `e` LEFT JOIN `characters` `c` ON `c`.`id` = `e`.`buddy` "
					+ "WHERE `owner` = ?");
			ps.setInt(1, id);
			rs = ps.executeQuery();
			while (rs.next()) {
				byte status = rs.getByte(3);
				if (status != BuddyListEntry.STATUS_INVITED)
					buddies.add(new BuddyListEntry(rs.getInt(1), rs.getString(2), status));
			}
			rs.close();
			ps.close();
			p.buddies = new ShopBuddyList(maxBuddies, buddies);

			ps = con.prepareStatement("SELECT `partyid` FROM `parties` WHERE `characterid` = ?");
			ps.setInt(1, id);
			rs = ps.executeQuery();
			if (rs.next())
				p.partyId = rs.getInt(1);
			rs.close();
			ps.close();

			ps = con.prepareStatement("SELECT `g`.`id` FROM `guilds` `g` LEFT JOIN `guildmembers` `m` ON `g`.`id` = `m`.`guildid` WHERE `m`.`characterid` = ?");
			ps.setInt(1, id);
			rs = ps.executeQuery();
			if (rs.next())
				p.guildId = rs.getInt(1);
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
					Map<Integer, AtomicInteger> mobProgress = new LinkedHashMap<Integer, AtomicInteger>();
					mps.setInt(1, questEntryId);
					mrs = null;
					try {
						mrs = mps.executeQuery();
						while (mrs.next())
							mobProgress.put(Integer.valueOf(mrs.getInt(1)), new AtomicInteger(mrs.getShort(2)));
					} finally {
						DatabaseManager.cleanup(DatabaseType.STATE, mrs, null, null);
					}
					QuestEntry status = new QuestEntry(rs.getByte(3), mobProgress);
					status.setCompletionTime(rs.getLong(4));
					p.questStatuses.put(Short.valueOf(questId), status);
				}
			} finally {
				DatabaseManager.cleanup(DatabaseType.STATE, null, mps, null);
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
