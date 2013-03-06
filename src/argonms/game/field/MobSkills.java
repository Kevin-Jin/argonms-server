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

package argonms.game.field;

/**
 *
 * @author GoldenKevin
 */
public final class MobSkills {
	public static final short
		WATK_UP = 100,
		MATK_UP = 101,
		WDEF_UP = 102,
		MDEF_UP = 103,
		WATK_UP_AOE = 110,
		MATK_UP_AOE = 111,
		WDEF_UP_AOE = 112,
		MDEF_UP_AOE = 113,
		HEAL_AOE = 114,
		SPEED_UP_AOE = 115,
		SEAL = 120,
		DARKEN = 121,
		WEAKEN = 122,
		STUN = 123,
		CURSE = 124,
		POISON = 125,
		SLOW = 126,
		DISPEL = 127,
		SEDUCE = 128,
		BANISH = 129,
		MIST = 131,
		CRAZY_SKULL = 132, //Ariant Coliseum
		PHYSICAL_IMMUNITY = 140,
		MAGIC_IMMUNITY = 141,
		ARMOR = 142,
		PHYSICAL_REFLECT = 143,
		MAGIC_REFLECT = 144, 
		ALL_REFLECT = 145,
		MONSTER_CARNIVAL_WATK_UP = 150,
		MONSTER_CARNIVAL_MATK_UP = 151,
		MONSTER_CARNIVAL_WDEF_UP = 152,
		MONSTER_CARNIVAL_MDEF_UP = 153,
		MONSTER_CARNIVAL_ACC_UP = 154,
		MONSTER_CARNIVAL_EVA_UP = 155,
		MONSTER_CARNIVAL_SPEED_UP = 156,
		MONSTER_CARNIVAL_SEAL = 157,
		SUMMON = 200
	;

	private MobSkills() {
		//uninstantiable...
	}
}
