/*
 * ArgonMS MapleStory server emulator written in Java
 * Copyright (C) 2011-2012  GoldenKevin
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

import argonms.common.character.PlayerStatusEffect;
import argonms.common.character.Skills;
import argonms.common.loading.StatusEffectsData;
import argonms.common.loading.StatusEffectsData.BuffsData;
import argonms.game.field.entity.PlayerSkillSummon;
import argonms.game.loading.skill.PlayerSkillEffectsData;
import argonms.game.net.external.GamePackets;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author GoldenKevin
 */
public final class StatusEffectTools {
	public static final byte
		LEVEL_UP = 0,
		PASSIVE_BUFF = 1,
		ACTIVE_BUFF = 2, //energy charge + party buffs + beholder?
		ITEM_GAIN = 3,
		PET_LVL_UP = 3,
		DRAGON_BLOOD = 5,
		SAFETY_CHARM = 6,
		JOB_ADVANCEMENT = 8,
		QUEST = 9,
		MOB_BUFF = 11
	;

	private static byte[] getFirstPersonCastVisualEffect(GameCharacter p, StatusEffectsData e, byte stance) {
		return null;
	}

	private static byte[] getFirstPersonCastEffect(GameCharacter p, StatusEffectsData e, Map<PlayerStatusEffect, Short> updatedStats) {
		switch (e.getSourceType()) {
			case PLAYER_SKILL:
				switch (e.getDataId()) {
					case Skills.CHAKRA:
						break;
					case Skills.DASH:
						return GamePackets.writeUsePirateSkill(updatedStats, e.getDataId(), e.getDuration());
					default:
						return GamePackets.writeUseSkill(updatedStats, e.getDataId(), e.getDuration());
				}
			case ITEM:
				return GamePackets.writeUseItem(updatedStats, e.getDataId(), e.getDuration());
			case MOB_SKILL:
				return GamePackets.writeGiveDebuff(updatedStats, (short) e.getDataId(), e.getLevel(), e.getDuration(), (short) 900);
		}
		return null;
	}

	private static byte[] getThirdPersonCastVisualEffect(GameCharacter p, StatusEffectsData e, byte stance) {
		switch (e.getSourceType()) {
			case PLAYER_SKILL:
				return GamePackets.writeBuffMapVisualEffect(p, PASSIVE_BUFF, e.getDataId(), e.getLevel(), stance);
		}
		return null;
	}

	private static byte[] getThirdPersonCastEffect(GameCharacter p, StatusEffectsData e, Map<PlayerStatusEffect, Short> updatedStats) {
		switch (e.getSourceType()) {
			case PLAYER_SKILL:
				switch (e.getDataId()) {
					case Skills.CHAKRA:
						break;
					case Skills.DASH:
						return GamePackets.writeBuffMapPirateEffect(p, updatedStats, e.getDataId(), e.getDuration());
					default:
						if (e.getDuration() > 0)
							return GamePackets.writeBuffMapEffect(p, updatedStats);
				}
			case MOB_SKILL:
				return GamePackets.writeDebuffMapEffect(p, updatedStats, (short) e.getDataId(), e.getLevel(), (short) 900);
		}
		return null;
	}

	private static byte[] getFirstPersonDispelVisualEffect(GameCharacter p) {
		return null;
	}

	private static byte[] getFirstPersonDispelEffect(GameCharacter p, StatusEffectsData e) {
		return GamePackets.writeCancelStatusEffect(e.getEffects());
	}

	private static byte[] getThirdPersonDispelVisualEffect(GameCharacter p) {
		return null;
	}

	private static byte[] getThirdPersonDispelEffect(GameCharacter p, StatusEffectsData e, Set<PlayerStatusEffect> updatedStats) {
		return GamePackets.writeCancelStatusEffectMapEffect(p, updatedStats);
	}

	public static Map<PlayerStatusEffect, Short> applyEffects(GameCharacter p, StatusEffectsData e) {
		Map<PlayerStatusEffect, Short> updatedStats = new EnumMap<PlayerStatusEffect, Short>(PlayerStatusEffect.class);
		if (p.areEffectsActive(e))
			p.removeCancelEffectTask(e);
		for (PlayerStatusEffect buff : e.getEffects()) {
			PlayerStatusEffectValues v = p.getEffectValue(buff);
			if (v != null)
				dispelEffect(p, buff, v);
			PlayerStatusEffectValues value = applyEffect(p, e, buff);
			updatedStats.put(buff, Short.valueOf(value.getModifier()));
			p.addToActiveEffects(buff, value);
		}
		return updatedStats;
	}

	public static void applyEffectsAndShowVisuals(GameCharacter p, StatusEffectsData e, byte stance) {
		Map<PlayerStatusEffect, Short> updatedStats = applyEffects(p, e);
		byte[] effect = getFirstPersonCastVisualEffect(p, e, stance);
		if (effect != null)
			p.getClient().getSession().send(effect);
		effect = getFirstPersonCastEffect(p, e, updatedStats);
		if (effect != null)
			p.getClient().getSession().send(effect);
		effect = getThirdPersonCastVisualEffect(p, e, stance);
		if (p.isVisible() && effect != null)
			p.getMap().sendToAll(effect, p);
		effect = getThirdPersonCastEffect(p, e, updatedStats);
		if (p.isVisible() && effect != null)
			p.getMap().sendToAll(effect, p);
	}

	public static void dispelEffects(GameCharacter p, StatusEffectsData e) {
		p.removeCancelEffectTask(e);
		for (PlayerStatusEffect buff : e.getEffects()) {
			PlayerStatusEffectValues v = p.removeFromActiveEffects(buff);
			if (v != null)
				dispelEffect(p, buff, v);
		}
	}

	public static void dispelEffectsAndShowVisuals(GameCharacter p, StatusEffectsData e) {
		dispelEffects(p, e);
		byte[] effect = getFirstPersonDispelVisualEffect(p);
		if (effect != null)
			p.getClient().getSession().send(effect);
		effect = getFirstPersonDispelEffect(p, e);
		if (effect != null)
			p.getClient().getSession().send(effect);
		effect = getThirdPersonDispelVisualEffect(p);
		if (p.isVisible() && effect != null)
			p.getMap().sendToAll(effect, p);
		effect = getThirdPersonDispelEffect(p, e, e.getEffects());
		if (p.isVisible() && effect != null)
			p.getMap().sendToAll(effect, p);
	}

	private static PlayerStatusEffectValues applyEffect(GameCharacter p, StatusEffectsData e, PlayerStatusEffect buff) {
		short mod = -1;
		switch (buff) {
			case SUMMON:
				break;
			case PUPPET:
				PlayerSkillSummon summon = p.getSummonBySkill(e.getDataId());
				summon.setHp((short) ((PlayerSkillEffectsData) e).getX());
				break;
			case SLOW:
				break;
			case HOMING_BEACON:
				break;
			case MORPH:
				break;
			case RECOVERY:
				break;
			case MAPLE_WARRIOR:
				break;
			case POWER_STANCE:
				break;
			case SHARP_EYES:
				break;
			case MANA_REFLECTION:
				break;
			case SEDUCE:
				break;
			case DRAGON_ROAR:
				break;
			case SHADOW_STARS:
				break;
			case INFINITY:
				break;
			case HOLY_SHIELD:
				break;
			case HAMSTRING:
				break;
			case BLIND:
				break;
			case CONCENTRATE:
				break;
			case ECHO_OF_HERO:
				break;
			case GHOST_MORPH:
				break;
			case ENERGY_CHARGE:
				break;
			case DASH_SPEED:
				mod = (short) ((PlayerSkillEffectsData) e).getX();
				p.addSpeed(mod);
				break;
			case DASH_JUMP:
				mod = (short) ((PlayerSkillEffectsData) e).getY();
				p.addJump(mod);
				break;
			case MONSTER_RIDING:
				break;
			case FINAL_ATTACK:
				break;
			case WATK:
				mod = ((BuffsData) e).getWatk();
				p.addWatk(mod);
				break;
			case WDEF:
				mod = ((BuffsData) e).getWdef();
				p.addWdef(mod);
				break;
			case MATK:
				mod = ((BuffsData) e).getMatk();
				p.addMatk(mod);
				break;
			case MDEF:
				mod = ((BuffsData) e).getMdef();
				p.addMdef(mod);
				break;
			case ACC:
				mod = ((BuffsData) e).getAcc();
				p.addAcc(mod);
				break;
			case AVOID:
				mod = ((BuffsData) e).getAvoid();
				p.addAvoid(mod);
				break;
			case HANDS:
				mod = ((BuffsData) e).getHands();
				p.addHands(mod);
				break;
			case SPEED:
				mod = ((PlayerSkillEffectsData) e).getSpeed();
				p.addSpeed(mod);
				break;
			case JUMP:
				mod = ((PlayerSkillEffectsData) e).getJump();
				p.addJump(mod);
				break;
			case MAGIC_GUARD:
				mod = (short) ((PlayerSkillEffectsData) e).getX();
				break;
			case DARKSIGHT:
				break;
			case HIDE:
				mod = 0;
				p.getClient().getSession().send(GamePackets.writeShowHide());
				p.getMap().hidePlayer(p);
				break;
			case BOOSTER:
				break;
			case POWER_GUARD:
				break;
			case HYPER_BODY_HP:
				mod = (short) ((PlayerSkillEffectsData) e).getX();
				p.recalculateMaxHp(mod);
				break;
			case HYPER_BODY_MP:
				mod = (short) ((PlayerSkillEffectsData) e).getY();
				p.recalculateMaxMp(mod);
				break;
			case INVINCIBLE:
				break;
			case SPEED_INFUSION:
				break;
			case SOUL_ARROW:
				break;
			case STUN:
				break;
			case POISON:
				break;
			case SEAL:
				break;
			case DARKNESS:
				break;
			case COMBO:
				break;
			case CHARGE:
				break;
			case DRAGON_BLOOD:
				break;
			case HOLY_SYMBOL:
				mod = (short) ((PlayerSkillEffectsData) e).getX();
				break;
			case MESO_UP:
				break;
			case SHADOW_PARTNER:
				break;
			case PICKPOCKET:
				break;
			case MESO_GUARD:
				break;
			case WEAKEN:
				break;
			case CURSE:
				break;
		}
		return new PlayerStatusEffectValues(e, mod);
	}

	public static void dispelEffect(GameCharacter p, PlayerStatusEffect key, PlayerStatusEffectValues value) {
		switch (key) {
			case SUMMON:
			case PUPPET:
				p.getMap().destroyEntity(p.getSummonBySkill(value.getSource()));
				p.removeFromSummons(value.getSource());
				break;
			case SLOW:
				break;
			case HOMING_BEACON:
				break;
			case MORPH:
				break;
			case RECOVERY:
				break;
			case MAPLE_WARRIOR:
				break;
			case POWER_STANCE:
				break;
			case SHARP_EYES:
				break;
			case MANA_REFLECTION:
				break;
			case SEDUCE:
				break;
			case DRAGON_ROAR:
				break;
			case SHADOW_STARS:
				break;
			case INFINITY:
				break;
			case HOLY_SHIELD:
				break;
			case HAMSTRING:
				break;
			case BLIND:
				break;
			case CONCENTRATE:
				break;
			case ECHO_OF_HERO:
				break;
			case GHOST_MORPH:
				break;
			case ENERGY_CHARGE:
				break;
			case DASH_SPEED:
				p.addSpeed(-value.getModifier());
				break;
			case DASH_JUMP:
				p.addJump(-value.getModifier());
				break;
			case MONSTER_RIDING:
				break;
			case FINAL_ATTACK:
				break;
			case WATK:
				p.addWatk(-value.getModifier());
				break;
			case WDEF:
				p.addWdef(-value.getModifier());
				break;
			case MATK:
				p.addMatk(-value.getModifier());
				break;
			case MDEF:
				p.addMdef(-value.getModifier());
				break;
			case ACC:
				p.addAcc(-value.getModifier());
				break;
			case AVOID:
				p.addAvoid(-value.getModifier());
				break;
			case HANDS:
				p.addHands(-value.getModifier());
				break;
			case SPEED:
				p.addSpeed(-value.getModifier());
				break;
			case JUMP:
				p.addJump(-value.getModifier());
				break;
			case MAGIC_GUARD:
				break;
			case DARKSIGHT:
				break;
			case HIDE:
				p.getClient().getSession().send(GamePackets.writeStopHide());
				p.getMap().unhidePlayer(p);
				break;
			case BOOSTER:
				break;
			case POWER_GUARD:
				break;
			case HYPER_BODY_HP:
				p.recalculateMaxHp((short) 0);
				if (p.getHp() > p.getCurrentMaxHp())
					p.setHp(p.getCurrentMaxHp());
				break;
			case HYPER_BODY_MP:
				p.recalculateMaxMp((short) 0);
				break;
			case INVINCIBLE:
				break;
			case SPEED_INFUSION:
				break;
			case SOUL_ARROW:
				break;
			case STUN:
				break;
			case POISON:
				break;
			case SEAL:
				break;
			case DARKNESS:
				break;
			case COMBO:
				break;
			case CHARGE:
				break;
			case DRAGON_BLOOD:
				break;
			case HOLY_SYMBOL:
				break;
			case MESO_UP:
				break;
			case SHADOW_PARTNER:
				break;
			case PICKPOCKET:
				break;
			case MESO_GUARD:
				break;
			case WEAKEN:
				break;
			case CURSE:
				break;
		}
	}

	private StatusEffectTools() {
		//uninstantiable...
	}
}
