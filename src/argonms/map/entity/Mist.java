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

import java.awt.Rectangle;

import argonms.character.Player;
import argonms.character.skill.Skills;
import argonms.loading.skill.MobSkillEffectsData;
import argonms.map.MapEntity;
import argonms.net.client.CommonPackets;
import argonms.tools.Rng;

/**
 *
 * @author GoldenKevin
 */
public class Mist extends MapEntity {
	public static final int
		MOB_MIST = 0,
		POISON_MIST = 1,
		SMOKE_SCREEN = 2
	;

	private int mistType;
	private int ownerEid;
	private int skillId;
	private byte skillLevel;
	private short skillDelay;
	private Rectangle box;
	private double prop;

	public Mist(Rectangle mistPosition, Mob mob, MobSkillEffectsData skill) {
		this.mistType = MOB_MIST;
		this.ownerEid = mob.getId();
		this.skillId = skill.getDataId();
		this.skillLevel = skill.getLevel();
		this.skillDelay = 0;
		this.box = mistPosition;
		this.prop = skill.getProp();
	}

	public Mist(Rectangle mistPosition, Player p, MobSkillEffectsData skill) {
		this.ownerEid = p.getId();
		this.skillId = skill.getDataId();
		this.skillLevel = skill.getLevel();
		this.box = mistPosition;
		this.prop = skill.getProp();
		switch (skillId) {
			case Skills.SMOKESCREEN:
				skillDelay = 8;
				mistType = SMOKE_SCREEN;
				break;
			case Skills.POISON_MIST: // FP mist
				skillDelay = 8;
				mistType = POISON_MIST;
				break;
		}
	}

	public int getMistType() {
		return mistType;
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

	public short getSkillDelay() {
		return skillDelay;
	}

	public Rectangle getBox() {
		return box;
	}

	public boolean shouldHurt() {
		return Rng.getGenerator().nextDouble() < prop;
	}

	public MapEntityType getEntityType() {
		return MapEntityType.MIST;
	}

	public boolean isAlive() {
		return true;
	}

	public boolean isVisible() {
		return false;
	}

	public byte[] getCreationMessage() {
		return CommonPackets.writeShowMist(this);
	}

	public byte[] getShowEntityMessage() {
		return getCreationMessage();
	}

	public byte[] getOutOfViewMessage() {
		return null;
	}

	public byte[] getDestructionMessage() {
		return CommonPackets.writeRemoveMist(this);
	}

	public boolean isNonRangedType() {
		return true;
	}
}
