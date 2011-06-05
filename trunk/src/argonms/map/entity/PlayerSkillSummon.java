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

package argonms.map.entity;

import argonms.character.Player;
import argonms.loading.skill.PlayerSkillEffectsData;
import argonms.loading.skill.SkillDataLoader;
import argonms.map.MapEntity;
import argonms.net.external.CommonPackets;
import java.awt.Point;

/**
 *
 * @author GoldenKevin
 */
public class PlayerSkillSummon extends MapEntity {
	private byte summonType; //0 = stationary, 1 = follow, 2/4 = only tele follow, 3 = bird follow
	private int ownerEid;
	private int skillId;
	private byte skillLevel;
	private short remHp;

	public PlayerSkillSummon(Player p, PlayerSkillEffectsData skill, Point initialPos, byte stance) {
		setPosition(initialPos);
		setStance(stance);
		ownerEid = p.getId();
		skillId = skill.getDataId();
		skillLevel = skill.getLevel();
		remHp = -1;
		summonType = SkillDataLoader.getInstance().getSkill(skillId).getSummonType();
	}

	public byte getSummonType() {
		return summonType;
	}

	public int getOwner() {
		return ownerEid;
	}

	public int getSkillId() {
		return skillId;
	}

	public byte getSkillLevel() {
		return skillLevel;
	}

	public void setHp(short hp) {
		remHp = hp;
	}

	public boolean hurt(int loss) {
		remHp -= Math.min(loss, remHp);
		return remHp == 0;
	}

	public boolean isPuppet() {
		return remHp >= 0;
	}

	public EntityType getEntityType() {
		return EntityType.SUMMON;
	}

	public boolean isAlive() {
		return remHp != 0;
	}

	public boolean isVisible() {
		return true;
	}

	public byte[] getCreationMessage() {
		return CommonPackets.writeShowSummon(this, (byte) 0);
	}

	public byte[] getShowEntityMessage() {
		return CommonPackets.writeShowSummon(this, (byte) 1);
	}

	public byte[] getOutOfViewMessage() {
		return null;
	}

	public byte[] getDestructionMessage() {
		return CommonPackets.writeRemoveSummon(this, (byte) 1);
	}

	public boolean isNonRangedType() {
		return true;
	}
}
