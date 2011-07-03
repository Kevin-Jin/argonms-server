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

package argonms.common.loading.skill;

import argonms.game.character.skill.Skills;
import argonms.game.field.MobSkills;
import argonms.common.tools.DatabaseManager;
import argonms.common.tools.DatabaseManager.DatabaseType;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 
 * @author GoldenKevin
 */
public class McdbSkillDataLoader extends SkillDataLoader {
	private static final Logger LOG = Logger.getLogger(McdbSkillDataLoader.class.getName());

	protected McdbSkillDataLoader() {
		
	}

	protected void loadPlayerSkill(int skillid) {
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		SkillStats stats = null;
		try {
			con = DatabaseManager.getConnection(DatabaseType.WZ);
			ps = con.prepareStatement("SELECT * FROM `skilldata` WHERE `skillid` = ?");
			ps.setInt(1, skillid);
			rs = ps.executeQuery();
			if (rs.next()) {
				stats = new SkillStats();
				doWork(rs, skillid, stats);
			}
		} catch (SQLException e) {
			LOG.log(Level.WARNING, "Could not read MCDB data for skill " + skillid, e);
		} finally {
			DatabaseManager.cleanup(DatabaseType.WZ, rs, ps, con);
		}
		skillStats.put(Integer.valueOf(skillid), stats);
	}

	protected void loadMobSkill(short skillid) {
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		MobSkillStats stats = null;
		try {
			con = DatabaseManager.getConnection(DatabaseType.WZ);
			ps = con.prepareStatement("SELECT * FROM `mobskills` WHERE `skillid` = ?");
			ps.setInt(1, skillid);
			rs = ps.executeQuery();
			if (rs.next()) {
				stats = new MobSkillStats();
				doMobWork(rs, skillid, stats, con);
			}
		} catch (SQLException e) {
			LOG.log(Level.WARNING, "Could not read MCDB data for mob skill " + skillid, e);
		} finally {
			DatabaseManager.cleanup(DatabaseType.WZ, rs, ps, con);
		}
		mobSkillStats.put(Short.valueOf(skillid), stats);
	}

	public boolean loadAll() {
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			con = DatabaseManager.getConnection(DatabaseType.WZ);
			ps = con.prepareStatement("SELECT * FROM `skilldata` ORDER BY `skillid`");
			rs = ps.executeQuery();
			boolean more = false;
			while (more || rs.next()) {
				int skillid = rs.getInt(1);
				SkillStats stats = new SkillStats();
				more = doWork(rs, skillid, stats);
				skillStats.put(Integer.valueOf(skillid), stats);
			}
			rs.close();
			ps.close();
			ps = con.prepareStatement("SELECT * FROM `mobskills` ORDER BY `skillid`");
			rs = ps.executeQuery();
			while (rs.next()) {
				short skillid = rs.getShort(1);
				MobSkillStats stats = new MobSkillStats();
				more = doMobWork(rs, skillid, stats, con);
				mobSkillStats.put(Short.valueOf(skillid), stats);
			}
			return true;
		} catch (SQLException ex) {
			LOG.log(Level.WARNING, "Could not load all skill data from MCDB.", ex);
			return false;
		} finally {
			DatabaseManager.cleanup(DatabaseType.WZ, rs, ps, con);
		}
	}

	public boolean canLoadPlayerSkill(int skillid) {
		if (skillStats.containsKey(Integer.valueOf(skillid)))
			return true;
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		boolean exists = false;
		try {
			con = DatabaseManager.getConnection(DatabaseType.WZ);
			ps = con.prepareStatement("SELECT * FROM `skilldata` WHERE `skillid` = ?");
			ps.setInt(1, skillid);
			rs = ps.executeQuery();
			if (rs.next())
				exists = true;
		} catch (SQLException e) {
			LOG.log(Level.WARNING, "Could not use MCDB to determine whether skill " + skillid + " is valid.", e);
		} finally {
			DatabaseManager.cleanup(DatabaseType.WZ, rs, ps, con);
		}
		return exists;
	}

	public boolean canLoadMobSkill(short skillid) {
		if (mobSkillStats.containsKey(Short.valueOf(skillid)))
			return true;
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		boolean exists = false;
		try {
			con = DatabaseManager.getConnection(DatabaseType.WZ);
			ps = con.prepareStatement("SELECT * FROM `mobskills` WHERE `skillid` = ?");
			ps.setInt(1, skillid);
			rs = ps.executeQuery();
			if (rs.next())
				exists = true;
		} catch (SQLException e) {
			LOG.log(Level.WARNING, "Could not use MCDB to determine whether mob skill " + skillid + " is valid.", e);
		} finally {
			DatabaseManager.cleanup(DatabaseType.WZ, rs, ps, con);
		}
		return exists;
	}

	private boolean doWork(ResultSet rs, int skillid, SkillStats stats) throws SQLException {
		switch (skillid) {
			case Skills.HURRICANE:
			case Skills.RAPID_FIRE:
				stats.setKeydownEnd();
				//intentional fallthrough to stats.setKeydown() and stats.setPrepared()
			case Skills.FP_BIG_BANG:
			case Skills.IL_BIG_BANG:
			case Skills.BISHOP_BIG_BANG:
			case Skills.PIERCING_ARROW:
			case Skills.CORKSCREW_BLOW:
			case Skills.GRENADE:
				stats.setKeydown();
				//intentional fallthrough to stats.setPrepared()
			case Skills.HERO_MONSTER_MAGNET:
			case Skills.PALADIN_MONSTER_MAGNET:
			case Skills.DARK_KNIGHT_MONSTER_MAGNET:
			case Skills.EXPLOSION:
			case Skills.CHAKRA:
				stats.setPrepared();
				break;
		}
		switch (skillid) {
			case Skills.XBOW_PUPPET:
			case Skills.BOW_PUPPET:
			case Skills.OCTOPUS:
			case Skills.WRATH_OF_THE_OCTOPI:
				stats.setSummonType((byte) 0);
				break;
			case Skills.BEHOLDER:
			case Skills.ELQUINES:
			case Skills.IFRIT:
			case Skills.BAHAMUT:
				stats.setSummonType((byte) 1);
				break;
			case Skills.GOLDEN_EAGLE:
			case Skills.SILVER_HAWK:
			case Skills.SUMMON_DRAGON:
			case Skills.FROSTPREY:
			case Skills.PHOENIX:
			case Skills.GAVIOTA:
				stats.setSummonType((byte) 3);
				break;
		}
		boolean more;
		do {
			byte level = rs.getByte(2);
			PlayerSkillEffectsData effect = new PlayerSkillEffectsData(skillid, level);
			effect.setMpConsume(rs.getShort(6));
			effect.setHpConsume(rs.getShort(7));
			effect.setDuration(rs.getInt(5) * 1000);
			effect.setX(rs.getInt(13));
			effect.setY(rs.getInt(14));
			effect.setDamage(rs.getShort(8));
			effect.setLt(rs.getShort(27), rs.getShort(28));
			effect.setRb(rs.getShort(29), rs.getShort(30));
			effect.setMobCount(rs.getByte(3));
			effect.setProp(rs.getShort(25));
			effect.setCooltime(rs.getShort(31));
			effect.setWatk(rs.getShort(17));
			effect.setWdef(rs.getShort(18));
			effect.setMatk(rs.getShort(19));
			effect.setMdef(rs.getShort(20));
			effect.setAcc(rs.getShort(21));
			effect.setAvoid(rs.getShort(22));
			effect.setHpRecoverRate(rs.getShort(23));
			effect.setMpRecoverRate(rs.getShort(24));
			effect.setSpeed(rs.getShort(15));
			effect.setJump(rs.getShort(16));
			effect.setAttackCount(rs.getByte(4));
			short bulletcon = rs.getShort(11);
			if (skillid == 4121006 || skillid == 4111005 || skillid == 5201001)
				effect.setBulletConsume(bulletcon);
			else
				effect.setBulletCount(bulletcon == 0 ? 1 : (byte) bulletcon);
			effect.setItemConsume(rs.getInt(9));
			effect.setItemConsumeCount(rs.getByte(10));
			effect.setMoneyConsume(rs.getShort(12));
			effect.setMorph(rs.getInt(26));
			stats.addLevel(level, effect);
		} while ((more = rs.next()) && rs.getInt(1) == skillid);
		return more;
	}

	private boolean doMobWork(ResultSet rs, short skillid, MobSkillStats stats, Connection con) throws SQLException {
		boolean more;
		do {
			byte level = rs.getByte(2);
			MobSkillEffectsData effect = new MobSkillEffectsData(skillid, level);
			effect.setMpConsume(rs.getShort(4));
			effect.setDuration(rs.getInt(3));
			effect.setX(rs.getInt(5));
			effect.setY(rs.getInt(6));
			effect.setLt(rs.getShort(10), rs.getShort(11));
			effect.setRb(rs.getShort(12), rs.getShort(13));
			effect.setProp(rs.getShort(7));
			effect.setCooltime(rs.getShort(9));
			effect.setMaxHpPercent(rs.getShort(14));
			if (skillid == MobSkills.SUMMON) {
				PreparedStatement summonsPs = null;
				ResultSet summons = null;
				try {
					summonsPs = con.prepareStatement("SELECT `mobindex`,`mobid` FROM `mobskillsummons` WHERE `level` = ? ORDER BY `mobindex`");
					summonsPs.setInt(1, level);
					summons = summonsPs.executeQuery();
					while (summons.next())
						effect.addSummon(summons.getByte(1), summons.getInt(2));
				} catch (SQLException e) {
					throw new SQLException("Failed to load summon data of mob skill " + skillid + " (level " + level + ")", e);
				} finally {
					DatabaseManager.cleanup(DatabaseType.WZ, summons, summonsPs, null);
				}
			}
			effect.setLimit(rs.getShort(15));
			effect.setSummonEffect(rs.getByte(16));
			stats.addLevel(level, effect);
		} while ((more = rs.next()) && rs.getShort(1) == skillid);
		return more;
	}
}
