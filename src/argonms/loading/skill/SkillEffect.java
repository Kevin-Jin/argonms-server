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

	protected SkillEffect() {
		
	}

	protected void setMpConsume(short mpCon) {
		this.mpCon = mpCon;
	}

	protected void setHpConsume(short hpCon) {
		this.hpCon = hpCon;
	}

	protected void setX(int x) {
		this.x = x;
	}

	protected void setY(int y) {
		this.y = y;
	}

	protected void setZ(int z) {
		this.z = z;
	}

	protected void setDamage(short damage) {
		this.damage = damage;
	}

	protected void setLt(short x, short y) {
		this.lt = new Point(x, y);
	}

	protected void setRb(short x, short y) {
		this.rb = new Point(x, y);
	}

	protected void setMobCount(byte count) {
		this.mobCount = count;
	}

	protected void setProp(double prop) {
		this.prop = prop;
	}

	protected void setMastery(byte mastery) {
		this.mastery = mastery;
	}

	protected void setCooltime(short duration) {
		this.cooltime = duration;
	}

	protected void setRange(short distance) {
		this.range = distance;
	}

	protected void setAttackCount(byte count) {
		this.attackCount = count;
	}

	protected void setBulletCount(byte count) {
		this.bulletCount = count;
	}

	protected void setItemConsume(int itemCon) {
		this.itemCon = itemCon;
	}

	protected void setItemConsumeCount(byte count) {
		this.itemConCount = count;
	}

	protected void setBulletConsume(short bulletCon) {
		this.bulletCon = bulletCon;
	}

	protected void setMoneyConsume(short moneyCon) {
		this.moneyCon = moneyCon;
	}

	public short getMpConsume() {
		return mpCon;
	}

	public short getHpConsume() {
		return hpCon;
	}

	public int getX() {
		return x;
	}

	public int getY() {
		return y;
	}

	public int getZ() {
		return z;
	}

	public short getDamage() {
		return damage;
	}

	public Point getLt() {
		return lt;
	}

	public Point getRb() {
		return rb;
	}

	public byte getMobCount() {
		return mobCount;
	}

	public double getProp() {
		return prop;
	}

	public byte getMastery() {
		return mastery;
	}

	public short getCooltime() {
		return cooltime;
	}

	public short getRange() {
		return range;
	}

	public byte getAttackCount() {
		return attackCount;
	}

	public byte setBulletCount() {
		return bulletCount;
	}

	public int getItemConsume() {
		return itemCon;
	}

	public byte getItemConsumeCount() {
		return itemConCount;
	}

	public short getBulletConsume() {
		return bulletCon;
	}

	public short getMoneyConsume() {
		return moneyCon;
	}
}
