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

import argonms.character.skill.PlayerStatusEffectValues.PlayerStatusEffect;
import java.util.EnumSet;
import java.util.Set;

/**
 *
 * @author GoldenKevin
 */
public abstract class BuffsData implements StatusEffectsData {
	private int duration;
	private short watk;
	private short wdef;
	private short matk;
	private short mdef;
	private short acc;
	private short avoid;
	private short hp;
	private short mp;
	private short speed;
	private short jump;
	private int morph;
	private int sourceid;
	protected Set<PlayerStatusEffect> effects;

	public BuffsData(int sourceid) {
		this.sourceid = sourceid;
		this.effects = EnumSet.noneOf(PlayerStatusEffect.class);
	}

	public void setDuration(int time) {
		this.duration = time;
	}

	public void setWatk(short pad) {
		effects.add(PlayerStatusEffect.WATK);
		this.watk = pad;
	}

	public void setWdef(short pdd) {
		effects.add(PlayerStatusEffect.WDEF);
		this.wdef = pdd;
	}

	public void setMatk(short mad) {
		effects.add(PlayerStatusEffect.MATK);
		this.matk = mad;
	}

	public void setMdef(short mdd) {
		effects.add(PlayerStatusEffect.MDEF);
		this.mdef = mdd;
	}

	public void setAcc(short acc) {
		effects.add(PlayerStatusEffect.ACC);
		this.acc = acc;
	}

	public void setAvoid(short eva) {
		effects.add(PlayerStatusEffect.AVOID);
		this.avoid = eva;
	}

	public void setHp(short hp) {
		effects.add(PlayerStatusEffect.MAXHP);
		this.hp = hp;
	}

	public void setMp(short mp) {
		effects.add(PlayerStatusEffect.MAXMP);
		this.mp = mp;
	}

	public void setSpeed(short speed) {
		effects.add(PlayerStatusEffect.SPEED);
		this.speed = speed;
	}

	public void setJump(short jump) {
		effects.add(PlayerStatusEffect.JUMP);
		this.jump = jump;
	}

	public void setMorph(int id) {
		effects.add(PlayerStatusEffect.MORPH);
		this.morph = id;
	}

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

	public short getHp() {
		return hp;
	}

	public short getMp() {
		return mp;
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

	public int getDataId() {
		return sourceid;
	}

	public Set<PlayerStatusEffect> getEffects() {
		return effects;
	}
}
