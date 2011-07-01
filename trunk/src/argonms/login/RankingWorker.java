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

package argonms.login;

import argonms.tools.DatabaseManager;
import argonms.tools.DatabaseManager.DatabaseType;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Offloads all calculation to the MySQL server so less data (or even none
 * entirely) will have to be transferred between the MySQL server and the login
 * server. The reduction in bandwidth usage and network processing will offset
 * Java's better processing performance over MySQL script execution.
 *
 * Inspired by Vana's RankingCalculator.
 * @author GoldenKevin
 */
public class RankingWorker implements Runnable {
	private static final Logger LOG = Logger.getLogger(RankingWorker.class.getName());

	private void updateLevelBasedRank(Connection con, String additionalConstraints, String updatedRankColumn, String oldRankColumn, String exceptionMessage) throws SQLException {
		PreparedStatement ps = null;
		try {
			ps = con.prepareStatement("SET @rank := @real_rank := 0, @level := @exp := -1;");
			ps.executeUpdate();
			ps = con.prepareStatement(
				"UPDATE `characters` `target`"
				+ "	INNER JOIN ("
				+ "		SELECT `c`.`id`,"
							//actual rank calculating
				+ "			GREATEST("
				+ "				@rank := IF(`level` <> 200 AND @level = `level` AND @exp = `exp`, @rank, @real_rank + 1),"
				+ "				LEAST(0, @real_rank := @real_rank + 1),"
				+ "				LEAST(0, @level := `level`),"
				+ "				LEAST(0, @exp := `exp`)"
				+ "			) AS `rank`"
							//`source`.`rank` = Max((`level` != 200 && @level == `level` && @exp == `exp`) ? @rank : (@real_rank + 1), Min(0, ++@real_rank), Min(0, @level = `level`), Min(0, @exp = `exp`))
				+ "		FROM `characters` `c`"
				+ "		LEFT JOIN `accounts` `a` ON `a`.`id` = `c`.`accountid`"
				+ "		WHERE" //constraints/exclusions
				+ "			(`a`.`banexpire` DIV 1000) < UNIX_TIMESTAMP()" //do not count banned players
				+ "			AND `a`.`gm` = 0" //do not count GMs
				+ "			AND ((`c`.`job` = 0 AND `c`.`level` >= 10) OR (`c`.`job` <> 0))" //only count non-beginners or beginners above level 9
				+ additionalConstraints
				+ "		ORDER BY"
				+ "			`c`.`level` DESC," //highest levels go on top
				+ "			`c`.`exp` DESC" //followed by higher exp if two players have same level
				+ "	) AS `source` ON `source`.`id` = `target`.`id`"
				+ "	SET"
				+ "		`target`." + oldRankColumn + " = `target`." + updatedRankColumn + ","
				+ "		`target`." + updatedRankColumn + " = `source`.rank"
			);
			ps.executeUpdate();
		} catch (SQLException e) {
			throw new SQLException(exceptionMessage, e);
		} finally {
			DatabaseManager.cleanup(DatabaseType.STATE, null, ps, null);
		}
	}

	private void updateOverall(Connection con) throws SQLException {
		updateLevelBasedRank(con, "", "overallrankcurrentpos", "overallrankoldpos", "Error updating character overall rankings.");
	}

	private void updateWorld(Connection con, byte worldId) throws SQLException {
		updateLevelBasedRank(con, "AND c.`world`", "worldrankcurrentpos", "worldrankoldpos", "Error updating character world " + worldId + " rankings.");
	}

	private void updateJob(Connection con, byte prefix) throws SQLException {
		updateLevelBasedRank(con, "AND (c.`job` DIV 100) = " + prefix, "jobrankcurrentpos", "jobrankoldpos", "Error updating character job class " + prefix + " rankings.");
	}

	private void updateFame(Connection con) throws SQLException {
		PreparedStatement ps = null;
		try {
			ps = con.prepareStatement("SET @rank := @real_rank := 0, @level := @exp := -1;");
			ps.executeUpdate();
			ps = con.prepareStatement(
				"UPDATE `characters` `target`"
				+ "	INNER JOIN ("
				+ "		SELECT `c`.`id`,"
							//actual rank calculating
				+ "			(@rank := @rank + 1) AS `rank`"
							//`source`.`rank` = ++@rank
				+ "		FROM `characters` `c`"
				+ "		LEFT JOIN `accounts` `a` ON `a`.`id` = `c`.`accountid`"
				+ "		WHERE" //constraints/exclusions
				+ "			(`a`.`banexpire` DIV 1000) < UNIX_TIMESTAMP()" //do not count banned players
				+ "			AND `a`.`gm` = 0" //do not count GMs
				+ "			AND ((`c`.`job` = 0 AND `c`.`level` >= 10) OR (`c`.`job` <> 0))" //only count non-beginners or beginners above level 9
				+ "			AND fame > 0" //only count players who have some fame
				+ "		ORDER BY"
				+ "			`c`.`fame` DESC," //highest famed players go on top
				+ "			`c`.`level` DESC," //followed by higher level if two players have same fame
				+ "			`c`.`exp` DESC" //followed by higher exp if two players have same level
				+ "	) AS `source` ON `source`.`id` = `target`.`id`"
				+ "	SET"
				+ "		`target`.`famerankoldpos` = `target`.`famerankcurrentpos`,"
				+ "		`target`.`famerankcurrentpos` = `source`.`rank`"
			);
			ps.executeUpdate();
		} catch (SQLException e) {
			throw new SQLException("Error updating character fame rankings.", e);
		} finally {
			DatabaseManager.cleanup(DatabaseType.STATE, null, ps, null);
		}
	}

	public void run() {
		Connection con = null;
		try {
			long start, end;
			start = System.nanoTime();
			con = DatabaseManager.getConnection(DatabaseType.STATE);
			updateOverall(con);
			for (Byte world : LoginServer.getInstance().getAllWorlds().keySet())
				updateWorld(con, world.byteValue());
			for (byte jobClass = 0; jobClass <= 5; jobClass++) //beginner, warrior, magician, bowman, thief, pirate respectively
				updateJob(con, jobClass);
			updateFame(con);
			end = System.nanoTime();
			LOG.log(Level.FINE, "Sucessfully updated rankings in {0} milliseconds.", (end - start) / 1000000.0);
		} catch (SQLException ex) {
			LOG.log(Level.WARNING, "Error updating the rankings.", ex);
		} finally {
			DatabaseManager.cleanup(DatabaseType.STATE, null, null, con);
		}
	}
}
