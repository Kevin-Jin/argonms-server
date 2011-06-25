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

import argonms.character.BuffState.ItemState;
import argonms.character.BuffState.MobSkillState;
import argonms.character.BuffState.SkillState;
import argonms.character.inventory.ItemTools;
import argonms.character.skill.SkillTools;
import argonms.character.skill.Skills;
import argonms.loading.skill.SkillDataLoader;
import argonms.map.entity.PlayerSkillSummon;
import java.awt.Point;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 *
 * @author GoldenKevin
 */
public class PlayerContinuation {
	private final Map<Integer, ItemState> activeItems;
	private final Map<Integer, SkillState> activeSkills;
	private final Map<Short, MobSkillState> activeDebuffs;
	private final Map<Integer, PlayerSkillSummon> activeSummons;

	public PlayerContinuation(Player p) {
		activeItems = p.activeItemsList();
		activeSkills = p.activeSkillsList();
		activeDebuffs = p.activeMobSkillsList();
		activeSummons = p.getAllSummons();
	}

	public PlayerContinuation() {
		activeItems = new HashMap<Integer, ItemState>();
		activeSkills = new HashMap<Integer, SkillState>();
		activeDebuffs = new HashMap<Short, MobSkillState>();
		activeSummons = new HashMap<Integer, PlayerSkillSummon>();
	}

	public Map<Integer, ItemState> getActiveItems() {
		return activeItems;
	}

	public Map<Integer, SkillState> getActiveSkills() {
		return activeSkills;
	}

	public Map<Short, MobSkillState> getActiveDebuffs() {
		return activeDebuffs;
	}

	public Map<Integer, PlayerSkillSummon> getActiveSummons() {
		return activeSummons;
	}

	public void addItemBuff(int itemId, long endTime) {
		activeItems.put(Integer.valueOf(itemId), new ItemState(endTime));
	}

	public void addSkillBuff(int skillId, byte level, long endTime) {
		activeSkills.put(Integer.valueOf(skillId), new SkillState(level, endTime));
	}

	public void addMonsterDebuff(short skillId, byte level, long endTime) {
		activeDebuffs.put(Short.valueOf(skillId), new MobSkillState(level, endTime));
	}

	public void addActiveSummon(int skillId, int pId, Point pos, byte stance) {
		activeSummons.put(Integer.valueOf(skillId), new PlayerSkillSummon(pId,
				skillId, activeSkills.get(Integer.valueOf(skillId)).level,
				pos, stance));
	}

	public void applyTo(Player p) {
		for (Entry<Integer, ItemState> item : activeItems.entrySet())
			ItemTools.localUseBuffItem(p, item.getKey().intValue(), item.getValue().endTime);
		for (Entry<Integer, SkillState> skill : activeSkills.entrySet()) {
			int skillId = skill.getKey().intValue();
			SkillState skillState = skill.getValue();
			if (SkillDataLoader.getInstance().getSkill(skillId).isSummon()) {
				if (skillId != Skills.BOW_PUPPET && skillId != Skills.XBOW_PUPPET) {
					p.addToSummons(skillId, activeSummons.get(Integer.valueOf(skillId)));
					SkillTools.localUseBuffSkill(p, skillId, skillState.level, skillState.endTime);
				} else {
					SkillTools.cancelBuffSkill(p, skillId);
				}
			} else {
				SkillTools.localUseBuffSkill(p, skillId, skillState.level, skillState.endTime);
			}
		}
		for (Entry<Short, MobSkillState> debuff : activeDebuffs.entrySet()) {
			MobSkillState debuffState = debuff.getValue();
			DiseaseTools.localApplyDebuff(p, debuff.getKey().shortValue(), debuffState.level, debuffState.endTime);
		}
	}
}
