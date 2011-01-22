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

import argonms.loading.StatEffects;
import java.awt.Point;

/**
 *
 * @author GoldenKevin
 */
public class SkillEffect extends StatEffects {
	private short mpCon;
	private short hpCon;
	private int x;
	private int y;
	private int z;
	private short damage;
	private Point lt;
	private Point rb;
	private byte mobCount;
	private double prop;
	private byte mastery;
	private short cooltime;
	private short range;
	private byte attackCount;
	private byte bulletCount;
	private int itemCon;
	private byte itemConCount;
	private short bulletCon;
	private short moneyCon;

	public void setMpConsume(short mpCon) {
		this.mpCon = mpCon;
	}

	public void setHpConsume(short hpCon) {
		this.hpCon = hpCon;
	}

	public void setX(int x) {
		this.x = x;
	}

	public void setY(int y) {
		this.y = y;
	}

	public void setZ(int z) {
		this.z = z;
	}

	public void setDamage(short damage) {
		this.damage = damage;
	}

	public void setLt(Point point) {
		this.lt = point;
	}

	public void setRb(Point point) {
		this.rb = point;
	}

	public void setMobCount(byte count) {
		this.mobCount = count;
	}

	public void setProp(double prop) {
		this.prop = prop;
	}

	public void setMastery(byte mastery) {
		this.mastery = mastery;
	}

	public void setCooltime(short duration) {
		this.cooltime = duration;
	}

	public void setRange(short distance) {
		this.range = distance;
	}

	public void setAttackCount(byte count) {
		this.attackCount = count;
	}

	public void setBulletCount(byte count) {
		this.bulletCount = count;
	}

	public void setItemConsume(int itemCon) {
		this.itemCon = itemCon;
	}

	public void setItemConsumeCount(byte count) {
		this.itemConCount = count;
	}

	public void setBulletConsume(short bulletCon) {
		this.bulletCon = bulletCon;
	}

	public void setMoneyConsume(short moneyCon) {
		this.moneyCon = moneyCon;
	}
}
