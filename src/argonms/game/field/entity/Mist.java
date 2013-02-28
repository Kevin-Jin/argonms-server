/*
 * ArgonMS MapleStory server emulator written in Java
 * Copyright (C) 2011-2013  GoldenKevin
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

import argonms.common.character.Skills;
import argonms.common.util.Rng;
import argonms.game.character.GameCharacter;
import argonms.game.field.AbstractEntity;
import argonms.game.loading.skill.MobSkillEffectsData;
import argonms.game.loading.skill.PlayerSkillEffectsData;
import argonms.game.net.external.GamePackets;
import java.awt.Rectangle;

/**
 *
 * @author GoldenKevin
 */
public class Mist extends AbstractEntity {
	public static final int
		MOB_MIST = 0,
		POISON_MIST = 1,
		SMOKE_SCREEN = 2
	;

	private final int mistType;
	private final int ownerEid;
	private final int skillId;
	private final byte skillLevel;
	private final short skillDelay;
	private final Rectangle box;

	public Mist(Mob mob, MobSkillEffectsData skill) {
		this.mistType = MOB_MIST;
		this.ownerEid = mob.getId();
		this.skillId = skill.getDataId();
		this.skillLevel = skill.getLevel();
		this.skillDelay = 0;
		this.box = skill.getBoundingBox(mob.getPosition(), mob.getStance() % 2 != 0);
	}

	public Mist(GameCharacter p, PlayerSkillEffectsData skill) {
		this.ownerEid = p.getId();
		this.skillId = skill.getDataId();
		this.skillLevel = skill.getLevel();
		this.box = skill.getBoundingBox(p.getPosition(), p.getStance() % 2 != 0);
		switch (skillId) {
			case Skills.SMOKESCREEN:
				skillDelay = 8;
				mistType = SMOKE_SCREEN;
				break;
			case Skills.POISON_MIST:
				skillDelay = 8;
				mistType = POISON_MIST;
				break;
			default:
				skillDelay = -1;
				mistType = -1;
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

	@Override
	public EntityType getEntityType() {
		return EntityType.MIST;
	}

	@Override
	public boolean isAlive() {
		return true;
	}

	@Override
	public boolean isVisible() {
		return true;
	}

	@Override
	public byte[] getShowNewSpawnMessage() {
		return GamePackets.writeShowMist(this);
	}

	@Override
	public byte[] getShowExistingSpawnMessage() {
		return getShowNewSpawnMessage();
	}

	@Override
	public byte[] getDestructionMessage() {
		return GamePackets.writeRemoveMist(this);
	}
}
