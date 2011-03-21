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

package argonms.loading.skill;

import argonms.character.Disease;
import argonms.map.MonsterStatusEffect;
import java.awt.Point;
import java.util.Map;
import java.util.TreeMap;

/**
 *
 * @author GoldenKevin
 */
public class MobSkillEffectsData {
	private int skillid;
	private byte level;
	private short mpCon;
	private int duration;
	private int x;
	private int y;
	private Point lt;
	private Point rb;
	private double prop;
	private short cooltime;
	private short hp;
	private MonsterStatusEffect buff;
	private Disease disease;
	private short summonLimit;
	private byte summonEffect;
	private final Map<Byte, Integer> summons;

	protected MobSkillEffectsData(int skillid, byte level) {
		this.summons = new TreeMap<Byte, Integer>();
		this.skillid = skillid;
		this.level = level;
		switch (skillid) {
			case 100:
			case 110:
				buff = MonsterStatusEffect.WEAPON_ATTACK_UP;
				break;
			case 101:
			case 111:
				buff = MonsterStatusEffect.MAGIC_ATTACK_UP;
				break;
			case 102:
			case 112:
				buff = MonsterStatusEffect.WEAPON_DEFENSE_UP;
				break;
			case 103:
			case 113:
				buff = MonsterStatusEffect.MAGIC_DEFENSE_UP;
				break;

			case 120:
				disease = Disease.SEAL;
				break;
			case 121:
				disease = Disease.DARKNESS;
				break;
			case 122:
				disease = Disease.WEAKEN;
				break;
			case 123:
				disease = Disease.STUN;
				break;
			case 124:
				disease = Disease.CURSE;
				break;
			case 125:
				disease = Disease.POISON;
				break;
			case 126:
				disease = Disease.SLOW;
				break;
		}
	}

	protected void setMpConsume(short mpCon) {
		this.mpCon = mpCon;
	}

	protected void setDuration(int time) {
		this.duration = time;
	}

	protected void setX(int x) {
		this.x = x;
	}

	protected void setY(int y) {
		this.y = y;
	}

	protected void setLt(short x, short y) {
		this.lt = new Point(x, y);
	}

	protected void setRb(short x, short y) {
		this.rb = new Point(x, y);
	}

	protected void setProp(double prop) {
		this.prop = prop;
	}

	protected void setCooltime(short duration) {
		this.cooltime = duration;
	}

	protected void setHp(short hp) {
		this.hp = hp;
	}

	protected void setLimit(short limit) {
		this.summonLimit = limit;
	}

	protected void setSummonEffect(byte effect) {
		this.summonEffect = effect;
	}

	protected void addSummon(byte index, int mobid) {
		summons.put(Byte.valueOf(index), Integer.valueOf(mobid));
	}

	public short getMpConsume() {
		return mpCon;
	}

	public int getDuration() {
		return duration;
	}

	public int getX() {
		return x;
	}

	public int getY() {
		return y;
	}

	public Point getLt() {
		return lt;
	}

	public Point getRb() {
		return rb;
	}

	public double getProp() {
		return prop;
	}

	public boolean shouldPerform() {
		return Math.random() < prop;
	}

	public short getCooltime() {
		return cooltime;
	}

	public short getHp() {
		return hp;
	}

	public short getSummonLimit() {
		return summonLimit;
	}

	public byte getSummonEffect() {
		return summonEffect;
	}

	public Map<Byte, Integer> getSummons() {
		return summons;
	}

	public int getDataId() {
		return skillid;
	}

	public byte getLevel() {
		return level;
	}

	public MonsterStatusEffect getBuff() {
		return buff;
	}

	public Disease getDisease() {
		return disease;
	}
}
