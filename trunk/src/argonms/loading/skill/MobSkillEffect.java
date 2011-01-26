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

import java.awt.Point;

/**
 *
 * @author GoldenKevin
 */
public class MobSkillEffect {
	private short mpCon;
	private int duration;
	private int x;
	private int y;
	private Point lt;
	private Point rb;
	private double prop;
	private short cooltime;
	private short hp;

	protected MobSkillEffect() {

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

	protected void setLt(Point point) {
		this.lt = point;
	}

	protected void setRb(Point point) {
		this.rb = point;
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
}
