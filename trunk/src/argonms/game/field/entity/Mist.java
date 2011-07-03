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

package argonms.game.field.entity;

import argonms.common.net.external.CommonPackets;
import argonms.common.tools.Rng;
import argonms.game.character.GameCharacter;
import argonms.game.character.Skills;
import argonms.game.field.MapEntity;
import argonms.game.field.MapEntity.EntityType;
import argonms.game.loading.skill.MobSkillEffectsData;
import argonms.game.loading.skill.PlayerSkillEffectsData;
import java.awt.Rectangle;

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
	private short prop;

	public Mist(Rectangle mistPosition, Mob mob, MobSkillEffectsData skill) {
		this.mistType = MOB_MIST;
		this.ownerEid = mob.getId();
		this.skillId = skill.getDataId();
		this.skillLevel = skill.getLevel();
		this.skillDelay = 0;
		this.box = mistPosition;
		this.prop = skill.getProp();
	}

	public Mist(Rectangle mistPosition, GameCharacter p, PlayerSkillEffectsData skill) {
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
			case Skills.POISON_MIST:
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
		return Rng.getGenerator().nextInt(100) < prop;
	}

	public EntityType getEntityType() {
		return EntityType.MIST;
	}

	public boolean isAlive() {
		return true;
	}

	public boolean isVisible() {
		return true;
	}

	public byte[] getShowNewSpawnMessage() {
		return CommonPackets.writeShowMist(this);
	}

	public byte[] getShowExistingSpawnMessage() {
		return getShowNewSpawnMessage();
	}

	public byte[] getDestructionMessage() {
		return CommonPackets.writeRemoveMist(this);
	}
}
