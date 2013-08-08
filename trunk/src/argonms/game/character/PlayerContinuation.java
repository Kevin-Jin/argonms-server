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

package argonms.game.character;

import argonms.common.character.AbstractPlayerContinuation;
import argonms.common.character.PlayerStatusEffect;
import argonms.common.character.Skills;
import argonms.common.util.Scheduler;
import argonms.game.GameServer;
import argonms.common.character.BuffState.ItemState;
import argonms.common.character.BuffState.MobSkillState;
import argonms.common.character.BuffState.SkillState;
import argonms.game.character.inventory.ItemTools;
import argonms.game.field.entity.PlayerSkillSummon;
import argonms.game.loading.skill.PlayerSkillEffectsData;
import argonms.game.loading.skill.SkillDataLoader;
import argonms.game.net.external.GamePackets;
import java.awt.Point;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 *
 * @author GoldenKevin
 */
public class PlayerContinuation extends AbstractPlayerContinuation {
	private final Map<Integer, PlayerSkillSummon> activeSummons;

	public PlayerContinuation(GameCharacter p) {
		super(p.activeItemsList(), p.activeSkillsList(), p.activeMobSkillsList(), p.getClient().getChannel(), p.getEnergyCharge());
		if (p.getChatRoom() != null)
			setChatroomId(p.getChatRoom().getRoomId());
		activeSummons = p.getAllSummons();
	}

	public PlayerContinuation() {
		super();
		activeSummons = new HashMap<Integer, PlayerSkillSummon>();
	}

	public Map<Integer, PlayerSkillSummon> getActiveSummons() {
		return activeSummons;
	}

	public void addActiveSummon(int skillId, int pId, Point pos, byte stance) {
		activeSummons.put(Integer.valueOf(skillId), new PlayerSkillSummon(pId,
				skillId, getActiveSkills().get(Integer.valueOf(skillId)).level, pos, stance));
	}

	public void applyTo(final GameCharacter p) {
		for (Entry<Integer, ItemState> item : getActiveItems().entrySet())
			ItemTools.localUseBuffItem(p, item.getKey().intValue(), item.getValue().endTime);
		for (Entry<Integer, SkillState> skill : getActiveSkills().entrySet()) {
			int skillId = skill.getKey().intValue();
			SkillState skillState = skill.getValue();
			if (SkillDataLoader.getInstance().getSkill(skillId).isSummon()) {
				if (skillId != Skills.BOW_PUPPET && skillId != Skills.XBOW_PUPPET) {
					p.addToSummons(skillId, activeSummons.get(Integer.valueOf(skillId)));
					SkillTools.localUseBuffSkill(p, skillId, skillState.level, skillState.endTime);
				} else {
					SkillTools.cancelBuffSkill(p, skillId);
				}
			} else if (skillId == Skills.ENERGY_CHARGE) {
				final PlayerSkillEffectsData e = SkillDataLoader.getInstance().getSkill(Skills.ENERGY_CHARGE).getLevel(skillState.level);
				p.addToActiveEffects(PlayerStatusEffect.ENERGY_CHARGE, new PlayerStatusEffectValues(e, (short) 10000));
				p.addCancelEffectTask(e, Scheduler.getInstance().runAfterDelay(new Runnable() {
					@Override
					public void run() {
						p.resetEnergyCharge();
						p.removeCancelEffectTask(e);
						p.removeFromActiveEffects(PlayerStatusEffect.ENERGY_CHARGE);
						Map<PlayerStatusEffect, Short> updatedStats = Collections.singletonMap(PlayerStatusEffect.ENERGY_CHARGE, Short.valueOf((short) 0));
						p.getClient().getSession().send(GamePackets.writeUsePirateSkill(updatedStats, 0, 0, (short) 0));
						p.getMap().sendToAll(GamePackets.writeBuffMapPirateEffect(p, updatedStats, 0, 0), p);
					}
				}, skillState.endTime - System.currentTimeMillis()), skillState.level, skillState.endTime);
			} else {
				SkillTools.localUseBuffSkill(p, skillId, skillState.level, skillState.endTime);
			}
		}
		for (Entry<Short, MobSkillState> debuff : getActiveDebuffs().entrySet()) {
			MobSkillState debuffState = debuff.getValue();
			DiseaseTools.localApplyDebuff(p, debuff.getKey().shortValue(), debuffState.level, debuffState.endTime);
		}
		if (getEnergyCharge() != 0) {
			p.resetEnergyCharge();
			p.addToEnergyCharge(getEnergyCharge());
		}
		if (getChatroomId() != 0)
			GameServer.getChannel(p.getClient().getChannel()).getCrossServerInterface().sendChatroomPlayerChangedChannels(p, getChatroomId());
	}
}
