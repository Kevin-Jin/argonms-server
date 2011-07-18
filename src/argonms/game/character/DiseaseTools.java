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

package argonms.game.character;

import argonms.common.util.Scheduler;
import argonms.game.loading.skill.MobSkillEffectsData;
import argonms.game.loading.skill.SkillDataLoader;

/**
 *
 * @author GoldenKevin
 */
public class DiseaseTools {
	public static void applyDebuff(final GameCharacter p, final short mobSkillId, final byte skillLevel) {
		MobSkillEffectsData e = SkillDataLoader.getInstance().getMobSkill(mobSkillId).getLevel(skillLevel);
		StatusEffectTools.applyEffectsAndShowVisuals(p, e, (byte) -1);
		p.addCancelEffectTask(e, Scheduler.getInstance().runAfterDelay(new Runnable() {
			@Override
			public void run() {
				cancelDebuff(p, mobSkillId, skillLevel);
			}
		}, e.getDuration()), skillLevel, System.currentTimeMillis() + e.getDuration());
	}

	public static void localApplyDebuff(final GameCharacter p, final short mobSkillId, final byte skillLevel, long endTime) {
		MobSkillEffectsData e = SkillDataLoader.getInstance().getMobSkill(mobSkillId).getLevel(skillLevel);
		StatusEffectTools.applyEffects(p, e);
		p.addCancelEffectTask(e, Scheduler.getInstance().runAfterDelay(new Runnable() {
			@Override
			public void run() {
				cancelDebuff(p, mobSkillId, skillLevel);
			}
		}, endTime - System.currentTimeMillis()), skillLevel, endTime);
	}

	public static void cancelDebuff(GameCharacter p, short mobSkillId, byte skillLevel) {
		MobSkillEffectsData e = SkillDataLoader.getInstance().getMobSkill(mobSkillId).getLevel(skillLevel);
		StatusEffectTools.dispelEffectsAndShowVisuals(p, e);
	}
}
