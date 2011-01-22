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
}
