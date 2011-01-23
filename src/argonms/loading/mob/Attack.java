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

/**
 *
 * @author GoldenKevin
 */
public class Attack {
	private boolean deadlyAttack;
	private int mpBurn;
	private int diseaseSkill;
	private int diseaseLevel;
	private int conMp;

	protected Attack() {
		
	}

	protected void setDeadlyAttack(boolean value) {
		this.deadlyAttack = value;
	}

	protected void setMpBurn(int burn) {
		this.mpBurn = burn;
	}

	protected void setDiseaseSkill(int skill) {
		this.diseaseSkill = skill;
	}

	protected void setDiseaseLevel(int level) {
		this.diseaseLevel = level;
	}

	protected void setMpConsume(int con) {
		this.conMp = con;
	}

	public boolean isDeadlyAttack() {
		return deadlyAttack;
	}

	public int getMpBurn() {
		return mpBurn;
	}

	public int getDiseaseSkill() {
		return diseaseSkill;
	}

	public int getDiseaseLevel() {
		return diseaseLevel;
	}

	public int getMpConsume() {
		return conMp;
	}

	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("DeadlyAttack=").append(deadlyAttack);
		builder.append(", MpBurn=").append(mpBurn);
		builder.append(", Disease (Skill=").append(diseaseSkill);
		builder.append(", Level=").append(diseaseLevel).append(')');
		builder.append(", MpConsume=").append(conMp);
		return builder.toString();
	}
}
