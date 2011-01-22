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

package argonms.loading;

/**
 *
 * @author GoldenKevin
 */
public final class KvjEffects {
	public static final byte //stats
		STR = 0,
		DEX = 1,
		INT = 2,
		LUK = 3,
		PAD = 4,
		PDD = 5,
		MAD = 6,
		MDD = 7,
		ACC = 8,
		EVA = 9,
		MHP = 10,
		MMP = 11,
		Speed = 12,
		Jump = 13,
		Level = 14,
		MaxLevel = 15
	;

	public static final byte //effects
		END_EFFECT = 0,
		MP_CONSUME = 1,
		HP_CONSUME = 2,
		DURATION = 3,
		X = 4,
		Y = 5,
		Z = 6,
		DAMAGE = 7,
		LT = 8,
		RB = 9,
		MOB_COUNT = 10,
		PROP = 11,
		MASTERY = 12,
		COOLTIME = 13,
		RANGE = 14,
		WATK = 15,
		WDEF = 16,
		MATK = 17,
		MDEF = 18,
		ACCY = 19,
		AVOID = 20,
		HP_BONUS = 21,
		MP_BONUS = 22,
		SPEED = 23,
		JUMP = 24,
		ATTACK_COUNT = 25,
		BULLET_COUNT = 26,
		ITEM_CONSUME = 27,
		ITEM_CONSUME_COUNT = 28,
		BULLET_CONSUME = 29,
		MONEY_CONSUME = 30,
		MORPH = 31,
		HP_RECOVER = 32,
		MP_RECOVER = 33,
		MOVE_TO = 34,
		POISON = 35,
		SEAL = 36,
		DARKNESS = 37,
		WEAKNESS = 38,
		CURSE = 39,
		CONSUME_ON_PICKUP = 40,
		PET_CONSUMABLE_BY = 41,
		SUMMON_EFFECT = 42,
		LIMIT = 43
	;

	private KvjEffects() {
	}
}
