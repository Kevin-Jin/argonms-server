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
public abstract class StatEffects {
	public enum Effect { ITEM, SKILL }

	public static final byte //indicies for a stats array (bonus or req)
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

	public abstract Effect getType();

	public void setDuration(int time) {
		this.duration = time;
	}

	public void setWatk(short pad) {
		this.watk = pad;
	}

	public void setWdef(short pdd) {
		this.wdef = pdd;
	}

	public void setMatk(short mad) {
		this.matk = mad;
	}

	public void setMdef(short mdd) {
		this.mdef = mdd;
	}

	public void setAcc(short acc) {
		this.acc = acc;
	}

	public void setAvoid(short eva) {
		this.avoid = eva;
	}

	public void setHp(short hp) {
		this.hp = hp;
	}

	public void setMp(short mp) {
		this.mp = mp;
	}

	public void setSpeed(short speed) {
		this.speed = speed;
	}

	public void setJump(short jump) {
		this.jump = jump;
	}

	public void setMorph(int id) {
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
}
