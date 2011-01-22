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

package argonms.loading.mob;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author GoldenKevin
 */
public class MobStats {
	private int level;
	private int maxHp;
	private int maxMp;
	private int pad;
	private int exp;
	private boolean undead;
	private String elemAttr;
	private int removeAfter;
	private boolean hideHp;
	private boolean hideName;
	private int hpTagColor;
	private int hpTagBgColor;
	private boolean boss;
	private SelfDestruct sd;
	private List<Integer> loseItems;
	private boolean invincible;
	private List<Integer> summons;
	private boolean firstAttack;
	private Map<Integer, Attack> attacks;
	private Map<Integer, Skill> skills;
	private int buff;
	private Map<String, Integer> delays;

	public MobStats() {
		this.loseItems = new ArrayList<Integer>();
		this.summons = new ArrayList<Integer>();
		this.attacks = new HashMap<Integer, Attack>();
		this.skills = new HashMap<Integer, Skill>();
		this.delays = new HashMap<String, Integer>();
	}

	public void setLevel(int level) {
		this.level = level;
	}

	public void setMaxHp(int value) {
		this.maxHp = value;
	}

	public void setMaxMp(int value) {
		this.maxMp = value;
	}

	public void setPhysicalDamage(int pad) {
		this.pad = pad;
	}

	public void setExp(int exp) {
		this.exp = exp;
	}

	public void setUndead() {
		this.undead = true;
	}

	public void setElementAttribute(String attr) {
		this.elemAttr = attr;
	}

	public void setRemoveAfter(int time) {
		this.removeAfter = time;
	}

	public void setHideHp() {
		this.hideHp = true;
	}

	public void setHideName() {
		this.hideHp = true;
	}

	public void setHpTagColor(int color) {
		this.hpTagColor = color;
	}

	public void setHpTagBgColor(int color) {
		this.hpTagBgColor = color;
	}

	public void setBoss() {
		this.boss = true;
	}

	public void setSelfDestruct(SelfDestruct sd) {
		this.sd = sd;
	}

	public void addLoseItem(int itemid) {
		this.loseItems.add(Integer.valueOf(itemid));
	}

	public void setInvincible() {
		this.invincible = true;
	}

	public void addSummon(int mobid) {
		this.summons.add(Integer.valueOf(mobid));
	}

	public void setFirstAttack() {
		this.firstAttack = true;
	}

	public void addAttack(int attackid, Attack attack) {
		this.attacks.put(Integer.valueOf(attackid), attack);
	}

	public void addSkill(int skillid, Skill skill) {
		this.skills.put(Integer.valueOf(skillid), skill);
	}

	public void setBuffToGive(int buffid) {
		this.buff = buffid;
	}

	public void addDelay(String name, int delay) {
		this.delays.put(name, Integer.valueOf(delay));
	}
}
