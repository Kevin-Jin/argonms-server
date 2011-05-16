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

package argonms.loading.skill;

import argonms.character.skill.Skills;
import argonms.map.MobSkills;
import argonms.tools.DatabaseConnection;
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
		Connection con = DatabaseConnection.getWzConnection();
		SkillStats stats = null;
		try {
			PreparedStatement ps = con.prepareStatement("SELECT * FROM `skilldata` WHERE `skillid` = ?");
			ps.setInt(1, skillid);
			ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				stats = new SkillStats();
				doWork(rs, stats);
			}
			rs.close();
			ps.close();
		} catch (SQLException e) {
			LOG.log(Level.WARNING, "Could not read MCDB data for skill " + skillid, e);
		}
		skillStats.put(Integer.valueOf(skillid), stats);
	}

	protected void loadMobSkill(short skillid) {
		Connection con = DatabaseConnection.getWzConnection();
		MobSkillStats stats = null;
		try {
			PreparedStatement ps = con.prepareStatement("SELECT * FROM `mobskills` WHERE `skillid` = ?");
			ps.setInt(1, skillid);
			ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				stats = new MobSkillStats();
				doMobWork(rs, stats, con);
			}
			rs.close();
			ps.close();
		} catch (SQLException e) {
			LOG.log(Level.WARNING, "Could not read MCDB data for mob skill " + skillid, e);
		}
		mobSkillStats.put(Short.valueOf(skillid), stats);
	}

	public boolean loadAll() {
		Connection con = DatabaseConnection.getWzConnection();
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = con.prepareStatement("SELECT * FROM `skilldata`");
			rs = ps.executeQuery();
			while (rs.next()) {
				SkillStats stats = new SkillStats();
				skillStats.put(Integer.valueOf(doWork(rs, stats)), stats);
			}
			rs.close();
			ps.close();
			ps = con.prepareStatement("SELECT * FROM `mobskills`");
			rs = ps.executeQuery();
			while (rs.next()) {
				MobSkillStats stats = new MobSkillStats();
				mobSkillStats.put(Short.valueOf(doMobWork(rs, stats, con)), stats);
			}
			return true;
		} catch (SQLException ex) {
			LOG.log(Level.WARNING, "Could not load all skill data from MCDB.", ex);
			return false;
		} finally {
			try {
				if (rs != null)
					rs.close();
				if (ps != null)
					ps.close();
			} catch (SQLException ex) {
				//Nothing we can do
			}
		}
	}

	public boolean canLoadPlayerSkill(int skillid) {
		if (skillStats.containsKey(Integer.valueOf(skillid)))
			return true;
		Connection con = DatabaseConnection.getWzConnection();
		boolean exists = false;
		try {
			PreparedStatement ps = con.prepareStatement("SELECT * FROM `skilldata` WHERE `skillid` = ?");
			ps.setInt(1, skillid);
			ResultSet rs = ps.executeQuery();
			if (rs.next())
				exists = true;
			rs.close();
			ps.close();
		} catch (SQLException e) {
			LOG.log(Level.WARNING, "Could not use MCDB to determine whether skill " + skillid + " is valid.", e);
		}
		return exists;
	}

	public boolean canLoadMobSkill(short skillid) {
		if (mobSkillStats.containsKey(Short.valueOf(skillid)))
			return true;
		Connection con = DatabaseConnection.getWzConnection();
		boolean exists = false;
		try {
			PreparedStatement ps = con.prepareStatement("SELECT * FROM `mobskills` WHERE `skillid` = ?");
			ps.setInt(1, skillid);
			ResultSet rs = ps.executeQuery();
			if (rs.next())
				exists = true;
			rs.close();
			ps.close();
		} catch (SQLException e) {
			LOG.log(Level.WARNING, "Could not use MCDB to determine whether mob skill " + skillid + " is valid.", e);
		}
		return exists;
	}

	private int doWork(ResultSet rs, SkillStats stats) throws SQLException {
		int skillid = rs.getInt(1);
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
		} while (rs.next() && rs.getInt(1) == skillid);
		return skillid;
	}

	private short doMobWork(ResultSet rs, MobSkillStats stats, Connection con) throws SQLException {
		short skillid = rs.getShort(1);
		//there's probably another set of buffs and charged for mob skills...
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
				PreparedStatement summonsPs = con.prepareStatement("SELECT `mobindex`,`mobid` FROM `mobskillsummons` WHERE `level` = ? ORDER BY `mobindex`");
				summonsPs.setInt(1, level);
				ResultSet summons = summonsPs.executeQuery();
				while (summons.next())
					effect.addSummon(summons.getByte(1), summons.getInt(2));
				summons.close();
				summonsPs.close();
			}
			effect.setLimit(rs.getShort(15));
			effect.setSummonEffect(rs.getByte(16));
			stats.addLevel(level, effect);
		} while (rs.next() && rs.getShort(1) == skillid);
		return skillid;
	}
}
