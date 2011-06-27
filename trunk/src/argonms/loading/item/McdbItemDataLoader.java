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

package argonms.loading.item;

import argonms.character.inventory.InventoryTools;
import argonms.StatEffect;
import argonms.tools.DatabaseManager;
import argonms.tools.DatabaseManager.DatabaseType;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author GoldenKevin
 */
public class McdbItemDataLoader extends ItemDataLoader {
	private static final Logger LOG = Logger.getLogger(McdbItemDataLoader.class.getName());

	protected McdbItemDataLoader() {
		
	}

	protected void load(int itemid) {
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		String query;
		if (InventoryTools.isEquip(itemid))
			query = "SELECT * FROM `equipdata` WHERE `equipid` = ?";
		else
			query = "SELECT * FROM `itemdata` WHERE `itemid` = ?";
		try {
			con = DatabaseManager.getConnection(DatabaseType.WZ);
			ps = con.prepareStatement(query);
			ps.setInt(1, itemid);
			rs = ps.executeQuery();
			if (rs.next())
				doWork(itemid, rs, con);
		} catch (SQLException e) {
			LOG.log(Level.WARNING, "Could not read MCDB data for item " + itemid, e);
		} finally {
			DatabaseManager.cleanup(DatabaseType.WZ, rs, ps, con);
		}
		loaded.add(Integer.valueOf(itemid));
	}

	public boolean loadAll() {
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			con = DatabaseManager.getConnection(DatabaseType.WZ);
			ps = con.prepareStatement("SELECT * FROM `itemdata`");
			rs = ps.executeQuery();
			while (rs.next())
				doWork(rs.getInt("itemid"), rs, con);
			rs.close();
			ps.close();

			ps = con.prepareStatement("SELECT * FROM `equipdata`");
			rs = ps.executeQuery();
			while (rs.next())
				doWork(rs.getInt("equipid"), rs, con);
			return true;
		} catch (SQLException ex) {
			LOG.log(Level.WARNING, "Could not load all item data from MCDB.", ex);
			return false;
		} finally {
			DatabaseManager.cleanup(DatabaseType.WZ, rs, ps, con);
		}
	}

	public boolean canLoad(int itemid) {
		if (loaded.contains(Integer.valueOf(itemid)))
			return true;
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		boolean exists = false;
		String query;
		if (InventoryTools.isEquip(itemid))
			query = "SELECT * FROM `equipdata` WHERE `equipid` = ?";
		else
			query = "SELECT * FROM `itemdata` WHERE `itemid` = ?";
		try {
			con = DatabaseManager.getConnection(DatabaseType.WZ);
			ps = con.prepareStatement(query);
			ps.setInt(1, itemid);
			rs = ps.executeQuery();
			if (rs.next())
				exists = true;
		} catch (SQLException e) {
			LOG.log(Level.WARNING, "Could not use MCDB to determine whether item " + itemid + " is valid.", e);
		} finally {
			DatabaseManager.cleanup(DatabaseType.WZ, rs, ps, con);
		}
		return exists;
	}

	private void doWork(int itemid, ResultSet rs, Connection con) throws SQLException {
		String cat = InventoryTools.getCategoryName(itemid);
		Integer oId = itemid;
		wholePrice.put(oId, Integer.valueOf(rs.getInt("price")));
		short[] incStats = new short[16];
		if (cat.equals("Equip")) {
			incStats[StatEffect.STR] = rs.getShort("str");
			incStats[StatEffect.DEX] = rs.getShort("dex");
			incStats[StatEffect.INT] = rs.getShort("int");
			incStats[StatEffect.LUK] = rs.getShort("luk");
			incStats[StatEffect.PAD] = rs.getShort("watk");
			incStats[StatEffect.PDD] = rs.getShort("wdef");
			incStats[StatEffect.MAD] = rs.getShort("matk");
			incStats[StatEffect.MDD] = rs.getShort("mdef");
			incStats[StatEffect.ACC] = rs.getShort("acc");
			incStats[StatEffect.EVA] = rs.getShort("avo");
			incStats[StatEffect.MHP] = rs.getShort("hp");
			incStats[StatEffect.MMP] = rs.getShort("mp");
			incStats[StatEffect.Speed] = rs.getShort("speed");
			incStats[StatEffect.Jump] = rs.getShort("jump");

			tuc.put(oId, Byte.valueOf(rs.getByte("slots")));

			if (InventoryTools.getCharCat(itemid).equals("TamingMob")) {
				byte tMobId = rs.getByte("tmob");
				if (tMobId != 0)
					tamingMobIds.put(oId, tMobId);
			}
		} else {
			slotMax.put(oId, Short.valueOf(rs.getShort("maxslot")));

			if (cat.equals("Pet")) {
				PreparedStatement ps = null;
				ResultSet prs = null;
				try {
					ps = con.prepareStatement("SELECT `hunger` FROM `petdata` WHERE `id` = ?");
					ps.setInt(1, itemid);
					prs = ps.executeQuery();
					if (prs.next())
						petHunger.put(oId, Integer.valueOf(prs.getInt(1)));
					prs.close();
					ps.close();

					ps = con.prepareStatement("SELECT `command`,`increase`,`prob` FROM `petinteractdata` WHERE `id` = ?");
					ps.setInt(1, itemid);
					prs = ps.executeQuery();
					if (prs.next()) {
						if (!petCommands.containsKey(oId))
							petCommands.put(oId, new HashMap<Byte, int[]>());
						petCommands.get(oId).put(Byte.valueOf(prs.getByte(1)), new int[] { prs.getInt(3), prs.getInt(2) });
					}
				} catch (SQLException e) {
					throw new SQLException("Failed to load pet specific data of item " + itemid, e);
				} finally {
					DatabaseManager.cleanup(DatabaseType.WZ, prs, ps, null);
				}
			}
			if (cat.equals("Consume")) {
				incStats[StatEffect.STR] = rs.getShort("istr");
				incStats[StatEffect.DEX] = rs.getShort("idex");
				incStats[StatEffect.INT] = rs.getShort("iint");
				incStats[StatEffect.LUK] = rs.getShort("iluk");
				incStats[StatEffect.PAD] = rs.getShort("iwatk");
				incStats[StatEffect.PDD] = rs.getShort("iwdef");
				incStats[StatEffect.MAD] = rs.getShort("imatk");
				incStats[StatEffect.MDD] = rs.getShort("imdef");
				incStats[StatEffect.ACC] = rs.getShort("iacc");
				incStats[StatEffect.EVA] = rs.getShort("iavo");
				incStats[StatEffect.MHP] = rs.getShort("ihp");
				incStats[StatEffect.MMP] = rs.getShort("imp");
				incStats[StatEffect.Speed] = rs.getShort("ispeed");
				incStats[StatEffect.Jump] = rs.getShort("ijump");

				ArrayList<int[]> mobsToSpawn = new ArrayList<int[]>();
				PreparedStatement ps = null;
				ResultSet urs = null;
				try {
					ps = con.prepareStatement("SELECT `mobid`,`chance` FROM `itemsummondata` WHERE `itemid` = ?");
					ps.setInt(1, oId.intValue());
					urs = ps.executeQuery();
					while (urs.next())
						mobsToSpawn.add(new int[] { urs.getInt(1), urs.getInt(2) } );
				} catch (SQLException e) {
					throw new SQLException("Failed to load summoning bag specific data of item " + itemid, e);
				} finally {
					DatabaseManager.cleanup(DatabaseType.WZ, urs, ps, null);
				}
				if (!mobsToSpawn.isEmpty())
					summons.put(oId, mobsToSpawn);

				int chance = rs.getInt("success");
				if (chance != 0)
					success.put(oId, Integer.valueOf(chance));
				chance = rs.getInt("cursed");
				if (chance != 0)
					cursed.put(oId, Integer.valueOf(chance));

				List<Integer> skillIds = new ArrayList<Integer>();
				try {
					ps = con.prepareStatement("SELECT `skillid` FROM `itemskilldata` WHERE `itemid` = ?");
					ps.setInt(1, itemid);
					urs = ps.executeQuery();
					while (urs.next())
						skillIds.add(Integer.valueOf(urs.getInt(1)));
				} catch (SQLException e) {
					throw new SQLException("Failed to load skill book specific data of item " + itemid, e);
				} finally {
					DatabaseManager.cleanup(DatabaseType.WZ, urs, ps, null);
				}
				if (!skillIds.isEmpty())
					skills.put(oId, skillIds);

				try {
					ps = con.prepareStatement("SELECT `price` FROM `rechargedata` WHERE `itemid` = ?");
					ps.setInt(1, itemid);
					urs = ps.executeQuery();
					if (urs.next())
						unitPrice.put(oId, Double.valueOf(urs.getDouble(1)));
				} catch (SQLException e) {
					throw new SQLException("Failed to load projectile specific data of item " + itemid, e);
				} finally {
					DatabaseManager.cleanup(DatabaseType.WZ, urs, ps, null);
				}

				//it would be a waste of memory if all these values were 0, hm...
				ItemEffectsData effect = new ItemEffectsData(itemid);
				effect.setDuration(rs.getInt("time") * 1000);
				effect.setHpRecover(rs.getShort("hp"));
				effect.setMpRecover(rs.getShort("mp"));
				effect.setHpRecoverPercent(rs.getShort("hpr"));
				effect.setMpRecoverPercent(rs.getShort("mpr"));
				effect.setMorph(rs.getInt("morph"));
				effect.setWatk(rs.getShort("watk"));
				effect.setWdef(rs.getShort("wdef"));
				effect.setMatk(rs.getShort("matk"));
				effect.setMdef(rs.getShort("mdef"));
				effect.setAcc(rs.getShort("acc"));
				effect.setAvoid(rs.getShort("avo"));
				effect.setSpeed(rs.getShort("speed"));
				effect.setJump(rs.getShort("jump"));
				effect.setMoveTo(rs.getInt("moveto"));
				statEffects.put(oId, effect);
			}
			switch (itemid) {
				case 5121006:
					triggerItem.put(oId, 2022112);
					break;
				case 5122000:
					triggerItem.put(oId, 2022302);
					break;
				case 5121000:
					triggerItem.put(oId, 2022071);
					break;
				case 5121001:
					triggerItem.put(oId, 2022072);
					break;
				case 5121002:
					triggerItem.put(oId, 2022073);
					break;
				case 5121003:
					triggerItem.put(oId, 2022094);
					break;
				case 5121004:
					triggerItem.put(oId, 2022100);
					break;
				case 5121005:
					triggerItem.put(oId, 2022101);
					break;
				case 5121007:
					triggerItem.put(oId, 2022119);
					break;
				case 5121008:
					triggerItem.put(oId, 2022153);
					break;
				case 5121009:
					triggerItem.put(oId, 2022154);
					break;
				case 5121010:
					triggerItem.put(oId, 2022183);
					break;
				case 5121011:
					triggerItem.put(oId, 2022196);
					break;
				case 5121012:
					triggerItem.put(oId, 2022197);
					break;
				case 5121013:
					triggerItem.put(oId, 2022200);
					break;
				case 5121014:
					triggerItem.put(oId, 2022265);
					break;
				case 5121015:
					triggerItem.put(oId, 2022280);
					break;
				case 5121016:
					triggerItem.put(oId, 2022285);
					break;
			}

			switch (itemid) { //hack, since mcdb doesn't have this
				//these are from the v62 xmls.
				case 5200000:
					mesoValue.put(oId, Integer.valueOf(1000000));
					break;
				case 5200001:
					mesoValue.put(oId, Integer.valueOf(130000));
					break;
				case 5200002:
					mesoValue.put(oId, Integer.valueOf(350000));
					break;
			}
		}
		for (int i = 0; i < 16; i++) {
			if (incStats[i] != 0) {
				bonusStats.put(oId, incStats);
				break;
			}
		}
		if (rs.getInt("notrade") != 0)
			tradeBlocked.add(oId);
		if (rs.getInt("quest") != 0)
			questItem.add(oId);
	}
}
