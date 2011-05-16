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

package argonms.character;

import argonms.loading.skill.MobSkillEffectsData;
import argonms.loading.skill.SkillDataLoader;
import argonms.tools.Timer;

/**
 *
 * @author GoldenKevin
 */
public class DiseaseTools {
	public static void applyDebuff(final Player p, final short mobSkillId, final byte skillLevel) {
		MobSkillEffectsData e = SkillDataLoader.getInstance().getMobSkill(mobSkillId).getLevel(skillLevel);
		StatusEffectTools.applyEffectsAndShowVisuals(p, e, (byte) -1);
		p.addCancelEffectTask(e, Timer.getInstance().runAfterDelay(new Runnable() {
			public void run() {
				cancelDebuff(p, mobSkillId, skillLevel);
			}
		}, e.getDuration()));
	}

	public static void localApplyDebuff(final Player p, final short mobSkillId, final byte skillLevel) {
		MobSkillEffectsData e = SkillDataLoader.getInstance().getMobSkill(mobSkillId).getLevel(skillLevel);
		StatusEffectTools.applyEffects(p, e);
		p.addCancelEffectTask(e, Timer.getInstance().runAfterDelay(new Runnable() {
			public void run() {
				cancelDebuff(p, mobSkillId, skillLevel);
			}
		}, e.getDuration()));
	}

	public static void cancelDebuff(Player p, short mobSkillId, byte skillLevel) {
		MobSkillEffectsData e = SkillDataLoader.getInstance().getMobSkill(mobSkillId).getLevel(skillLevel);
		StatusEffectTools.dispelEffectsAndShowVisuals(p, e);
	}
}
