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

package argonms.character;

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

	public static final short //jobs
		JOB_SWORDMAN = 100,
		JOB_MAGICIAN = 200,
		JOB_ARCHER = 300,
		JOB_ROUGE = 400,
		JOB_PIRATE = 500,
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
