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

package argonms.common.character;

//if you're adding on to this, PLEASE keep it sorted by the mask. you'll
//break a hell of a lot of routines if you don't keep it in order.
/**
 *
 * @author GoldenKevin
 */
public enum PlayerStatusEffect { //byte numbers are from left to right
	//byte 1
	SLOW			(0x0000000000000001L, (byte) 5),
	HOMING_BEACON	(0x0000000000000001L, (byte) 5),
	MORPH			(0x0000000000000002L, (byte) 5),
	RECOVERY		(0x0000000000000004L, (byte) 5),
	MAPLE_WARRIOR	(0x0000000000000008L, (byte) 5),
	STANCE			(0x0000000000000010L, (byte) 5),
	SHARP_EYES		(0x0000000000000020L, (byte) 5),
	MANA_REFLECTION	(0x0000000000000040L, (byte) 5),
	SEDUCE			(0x0000000000000080L, (byte) 5),
	DRAGON_ROAR		(0x0000000000000080L, (byte) 5),

	//byte 2
	SHADOW_STARS	(0x0000000000000100L, (byte) 6),
	INFINITY		(0x0000000000000200L, (byte) 6),
	HOLY_SHIELD		(0x0000000000000400L, (byte) 6),
	HAMSTRING		(0x0000000000000800L, (byte) 6),
	BLIND			(0x0000000000001000L, (byte) 6),
	CONCENTRATE		(0x0000000000002000L, (byte) 6),
	ECHO_OF_HERO	(0x0000000000008000L, (byte) 6),

	//byte 3
	GHOST_MORPH		(0x0000000000020000L, (byte) 7),

	//byte 4
	ENERGY_CHARGE	(0x0000000008000000L, (byte) 8),
	DASH_SPEED		(0x0000000010000000L, (byte) 8),
	DASH_JUMP		(0x0000000020000000L, (byte) 8),
	MONSTER_RIDING	(0x0000000040000000L, (byte) 8),
	FINAL_ATTACK	(0x0000000080000000L, (byte) 8),

	//byte 5
	WATK			(0x0000000100000000L, (byte) 1),
	WDEF			(0x0000000200000000L, (byte) 1),
	MATK			(0x0000000400000000L, (byte) 1),
	MDEF			(0x0000000800000000L, (byte) 1),
	ACC				(0x0000001000000000L, (byte) 1),
	AVOID			(0x0000002000000000L, (byte) 1),
	HANDS			(0x0000004000000000L, (byte) 1),
	SPEED			(0x0000008000000000L, (byte) 1),

	//byte 6
	JUMP			(0x0000010000000000L, (byte) 2),
	MAGIC_GUARD		(0x0000020000000000L, (byte) 2),
	DARKSIGHT		(0x0000040000000000L, (byte) 2),
	HIDE			(0x0000040000000000L, (byte) 2),
	BOOSTER			(0x0000080000000000L, (byte) 2),
	POWER_GUARD		(0x0000100000000000L, (byte) 2),
	HYPER_BODY_HP	(0x0000200000000000L, (byte) 2),
	HYPER_BODY_MP	(0x0000400000000000L, (byte) 2),
	INVINCIBLE		(0x0000800000000000L, (byte) 2),
	SPEED_INFUSION	(0x0000800000000000L, (byte) 2),

	//byte 7
	SOUL_ARROW		(0x0001000000000000L, (byte) 3),
	STUN			(0x0002000000000000L, (byte) 3),
	POISON			(0x0004000000000000L, (byte) 3),
	SEAL			(0x0008000000000000L, (byte) 3),
	DARKNESS		(0x0010000000000000L, (byte) 3),
	COMBO			(0x0020000000000000L, (byte) 3),
	CHARGE			(0x0040000000000000L, (byte) 3),
	DRAGON_BLOOD	(0x0080000000000000L, (byte) 3),

	//byte 8
	HOLY_SYMBOL		(0x0100000000000000L, (byte) 4),
	MESO_UP			(0x0200000000000000L, (byte) 4),
	SHADOW_PARTNER	(0x0400000000000000L, (byte) 4),
	PUPPET			(0x0800000000000000L, (byte) 4), //hacky - mask with no visible effects or stat buffs
	SUMMON			(0x0800000000000000L, (byte) 4), //hacky - mask with no visible effects or stat buffs
	PICKPOCKET		(0x0800000000000000L, (byte) 4),
	MESO_GUARD		(0x1000000000000000L, (byte) 4),
	WEAKEN			(0x4000000000000000L, (byte) 4),
	CURSE			(0x8000000000000000L, (byte) 4)
	;

	private final long mask;
	private final byte valueOrder;

	private PlayerStatusEffect(long mask, byte valueOrder) {
		this.mask = mask;
		this.valueOrder = valueOrder;
	}

	public long longValue() {
		return mask;
	}

	public byte getValueOrder() {
		return valueOrder;
	}

	public boolean isDebuff() {
		switch (this) {
			case SLOW:
			case SEDUCE:
			case STUN:
			case POISON:
			case SEAL:
			case DARKNESS:
			case WEAKEN:
			case CURSE:
				return true;
			default:
				return false;
		}
	}
}
