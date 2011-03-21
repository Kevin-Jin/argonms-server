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

package argonms.character.skill;

import argonms.loading.StatEffectsData;

/**
 *
 * @author GoldenKevin
 */
public class BuffState {
	public enum BuffKey {
		//byte 8
		HOMING_BEACON   (0x0000000000000001L),
		MORPH			(0x0000000000000002L),
		RECOVERY		(0x0000000000000004L),
		MAPLE_WARRIOR	(0x0000000000000008L),
		STANCE			(0x0000000000000010L),
		SHARP_EYES		(0x0000000000000020L),
		MANA_REFLECTION	(0x0000000000000040L),
		DRAGON_ROAR     (0x0000000000000080L),

		//byte 7
		SHADOW_CLAW		(0x0000000000000100L),
		INFINITY		(0x0000000000000200L),
		HOLY_SHIELD		(0x0000000000000400L),
		HAMSTRING		(0x0000000000000800L),
		BLIND			(0x0000000000001000L),
		CONCENTRATE		(0x0000000000002000L),
		ECHO_OF_HERO	(0x0000000000008000L),

		//byte 6
		GHOST_MORPH		(0x0000000000020000L),

		//byte 5
		BERSERK_FURY    (0x0000000008000000L),
		DIVINE_BODY     (0x0000000010000000L),
		//DASH			(0x0000000030000000L),
		MONSTER_RIDING	(0x0000000040000000L),
		FINALATTACK     (0x0000000080000000L),

		//byte 4
		WATK			(0x0000000100000000L),
		WDEF			(0x0000000200000000L),
		MATK			(0x0000000400000000L),
		MDEF			(0x0000000800000000L),
		ACC				(0x0000001000000000L),
		AVOID			(0x0000002000000000L),
		HANDS			(0x0000004000000000L),
		DASH            (0x0000006000000000L),
		SPEED			(0x0000008000000000L),

		//byte 3
		JUMP			(0x0000010000000000L),
		MAGIC_GUARD		(0x0000020000000000L),
		DARKSIGHT		(0x0000040000000000L),
		HIDE			(0x0000040000000000L),
		BOOSTER			(0x0000080000000000L),
		POWERGUARD		(0x0000100000000000L),
		MAXHP			(0x0000200000000000L),
		MAXMP			(0x0000400000000000L),
		INVINCIBLE		(0x0000800000000000L),
		SPEED_INFUSION	(0x0000800000000000L),

		//byte 2
		SOULARROW		(0x0001000000000000L),
		STUN			(0x0002000000000000L),
		POISON			(0x0004000000000000L),
		SEAL			(0x0008000000000000L),
		DARKNESS		(0x0010000000000000L),
		COMBO			(0x0020000000000000L),
		SUMMON			(0x0020000000000000L),
		WK_CHARGE		(0x0040000000000000L),
		DRAGONBLOOD		(0x0080000000000000L),

		//byte 1
		HOLY_SYMBOL		(0x0100000000000000L),
		MESOUP			(0x0200000000000000L),
		SHADOWPARTNER	(0x0400000000000000L),
		PICKPOCKET		(0x0800000000000000L),
		PUPPET			(0x0800000000000000L),
		MESOGUARD		(0x1000000000000000L),
		WEAKEN			(0x4000000000000000L),
		;

		private final long mask;

		private BuffKey(long mask) {
			this.mask = mask;
		}

		public long getMask() {
			return mask;
		}
	}

	private int dataid;
	private byte skillLevel;

	public BuffState(StatEffectsData e) {
		this.dataid = e.getDataId();
		this.skillLevel = e.getLevel();
	}

	public int getSource() {
		return dataid;
	}

	public byte getLevelWhenCast() {
		return skillLevel;
	}
}
