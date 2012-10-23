/*
 * ArgonMS MapleStory server emulator written in Java
 * Copyright (C) 2011-2012  GoldenKevin
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

package argonms.common.loading;

import argonms.common.character.PlayerStatusEffect;
import argonms.common.field.MonsterStatusEffect;
import java.util.EnumSet;
import java.util.Set;

/**
 *
 * @author GoldenKevin
 */
public interface StatusEffectsData {
	public enum EffectSource { ITEM, PLAYER_SKILL, MOB_SKILL }

	public EffectSource getSourceType();

	public int getDataId();

	public Set<PlayerStatusEffect> getEffects();

	@Override
	public int hashCode();

	public byte getLevel();

	public int getDuration();

	public abstract class BuffsData implements StatusEffectsData {
		private final int sourceid;
		protected final Set<PlayerStatusEffect> effects;
		private int duration;
		private short watk;
		private short wdef;
		private short matk;
		private short mdef;
		private short acc;
		private short avoid;
		private short hands;
		private short speed;
		private short jump;
		private int morph;

		public BuffsData(int sourceid) {
			this.sourceid = sourceid;
			this.effects = EnumSet.noneOf(PlayerStatusEffect.class);
		}

		public void setDuration(int time) {
			if (time != 0) //the only buff with time of 0 is HIDE as far as I know.
				this.duration = time;
		}

		public void setWatk(short pad) {
			if (pad != 0) {
				effects.add(PlayerStatusEffect.WATK);
				this.watk = pad;
			}
		}

		public void setWdef(short pdd) {
			if (pdd != 0) {
				effects.add(PlayerStatusEffect.WDEF);
				this.wdef = pdd;
			}
		}

		public void setMatk(short mad) {
			if (mad != 0) {
				effects.add(PlayerStatusEffect.MATK);
				this.matk = mad;
			}
		}

		public void setMdef(short mdd) {
			if (mdd != 0) {
				effects.add(PlayerStatusEffect.MDEF);
				this.mdef = mdd;
			}
		}

		public void setAcc(short acc) {
			if (acc != 0) {
				effects.add(PlayerStatusEffect.ACC);
				this.acc = acc;
			}
		}

		public void setAvoid(short eva) {
			if (eva != 0) {
				effects.add(PlayerStatusEffect.AVOID);
				this.avoid = eva;
			}
		}

		public void setHands(short hands) {
			if (hands != 0) {
				effects.add(PlayerStatusEffect.HANDS);
				this.hands = hands;
			}
		}

		public void setSpeed(short speed) {
			if (speed != 0) {
				effects.add(PlayerStatusEffect.SPEED);
				this.speed = speed;
			}
		}

		public void setJump(short jump) {
			if (jump != 0) {
				effects.add(PlayerStatusEffect.JUMP);
				this.jump = jump;
			}
		}

		public void setMorph(int id) {
			if (id != 0) {
				effects.add(PlayerStatusEffect.MORPH);
				this.morph = id;
			}
		}

		@Override
		public int getDuration() {
			return duration;
		}

		public short getWatk() {
			return watk;
		}

		public short getWdef() {
			return wdef;
		}

		public short getMatk() {
			return matk;
		}

		public short getMdef() {
			return mdef;
		}

		public short getAcc() {
			return acc;
		}

		public short getAvoid() {
			return avoid;
		}

		public short getHands() {
			return hands;
		}

		public short getSpeed() {
			return speed;
		}

		public short getJump() {
			return jump;
		}

		public int getMorph() {
			return morph;
		}

		@Override
		public int getDataId() {
			return sourceid;
		}

		@Override
		public Set<PlayerStatusEffect> getEffects() {
			return effects;
		}
	}

	public interface MonsterStatusEffectsData extends StatusEffectsData {
		public Set<MonsterStatusEffect> getMonsterEffects();
		public int getX();
		public int getY();
	}
}
