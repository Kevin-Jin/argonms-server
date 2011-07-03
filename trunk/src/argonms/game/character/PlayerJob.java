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

/**
 * A helper class with static methods that accept job id parameters and check
 * various conditions.
 * @author GoldenKevin
 */
public final class PlayerJob {
	public static final int //job paths
		CLASS_BEGINNER = 0,
		CLASS_WARRIOR = 1,
		CLASS_MAGICIAN = 2,
		CLASS_BOWMAN = 3,
		CLASS_THIEF = 4,
		CLASS_PIRATE = 5,
		CLASS_GAMEMASTER = 9
	;

	public static final short //adventurer jobs
		JOB_SWORDMAN = 100,
		JOB_FIGHTER = 110,
		JOB_CRUSADER = 111,
		JOB_HERO = 112,
		JOB_PAGE = 120,
		JOB_WHITE_KNIGHT = 121,
		JOB_PALADIN = 122,
		JOB_SPEARMAN = 130,
		JOB_DRAGON_KNIGHT = 131,
		JOB_DARK_KNIGHT = 132,
		JOB_MAGICIAN = 200,
		JOB_FP_WIZARD = 210,
		JOB_FP_MAGE = 211,
		JOB_FP_ARCH_MAGE = 212,
		JOB_IL_WIZARD = 220,
		JOB_IL_MAGE = 221,
		JOB_IL_ARCH_MAGE = 222,
		JOB_CLERIC = 230,
		JOB_PRIEST = 231,
		JOB_BISHOP = 232,
		JOB_ARCHER = 300,
		JOB_HUNTER = 310,
		JOB_RANGER = 311,
		JOB_BOWMASTER = 312,
		JOB_CROSSBOWMAN = 320,
		JOB_SNIPER = 321,
		JOB_MARKSMAN = 322,
		JOB_ROUGE = 400,
		JOB_ASSASSIN = 410,
		JOB_HERMIT = 411,
		JOB_NIGHT_LORD = 412,
		JOB_BANDIT = 420,
		JOB_CHIEF_BANDIT = 421,
		JOB_SHADOWER = 422,
		JOB_PIRATE = 500,
		JOB_BRAWLER = 510,
		JOB_MARAUDER = 511,
		JOB_BUCCANEER = 512,
		JOB_GUNSLINGER = 520,
		JOB_OUTLAW = 521,
		JOB_CORSAIR = 522,
		JOB_GM = 900,
		JOB_SUPER_GM = 910
	;

	private PlayerJob() {
		
	}

	public static int getJobPath(short jobid) {
		return (jobid / 100);
	}

	public static boolean isBeginner(short jobid) {
		return (getJobPath(jobid) == CLASS_BEGINNER);
	}

	public static boolean isWarrior(short jobid) {
		return (getJobPath(jobid) == CLASS_WARRIOR);
	}

	public static boolean isMage(short jobid) {
		return (getJobPath(jobid) == CLASS_MAGICIAN);
	}

	public static boolean isArcher(short jobid) {
		return (getJobPath(jobid) == CLASS_BOWMAN);
	}

	public static boolean isThief(short jobid) {
		return (getJobPath(jobid) == CLASS_THIEF);
	}

	public static boolean isPirate(short jobid) {
		return (getJobPath(jobid) == CLASS_PIRATE);
	}

	public static boolean isModerator(short jobid) {
		return (getJobPath(jobid) == CLASS_GAMEMASTER);
	}
}
