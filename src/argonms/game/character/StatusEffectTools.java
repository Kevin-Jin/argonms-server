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

import argonms.common.character.PlayerStatusEffect;
import argonms.common.character.Skills;
import argonms.common.loading.StatusEffectsData;
import argonms.common.loading.StatusEffectsData.BuffsData;
import argonms.common.util.Scheduler;
import argonms.game.field.entity.PlayerSkillSummon;
import argonms.game.loading.skill.MobSkillEffectsData;
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
		ACTIVE_BUFF = 1, //player activated buff himself - such as all buffs that apply to the caster only
		PASSIVE_BUFF = 2, //something else activated buff for player - energy charge, party buffs, beholder, etc.
		ITEM_GAIN = 3,
		PET_LVL_UP = 3,
		DRAGON_BLOOD = 5,
		SAFETY_CHARM = 6,
		JOB_ADVANCEMENT = 8,
		QUEST = 9,
		MOB_BUFF = 11
	;

	private static byte[] getFirstPersonCastEffect(GameCharacter p, byte effectType, StatusEffectsData e, byte stance) {
		switch (e.getSourceType()) {
			case PLAYER_SKILL:
				if (effectType == PASSIVE_BUFF)
					return GamePackets.writeSelfVisualEffect(effectType, e.getDataId(), e.getLevel(), stance);
				break;
		}
		return null;
	}

	private static byte[] getFirstPersonApplyEffect(GameCharacter p, StatusEffectsData e, Map<PlayerStatusEffect, Short> updatedStats, int duration) {
		switch (e.getSourceType()) {
			case PLAYER_SKILL:
				switch (e.getDataId()) {
					case Skills.CHAKRA:
						break;
					case Skills.DASH:
						return GamePackets.writeUsePirateSkill(updatedStats, e.getDataId(), duration, (short) 0);
					default:
						return GamePackets.writeUseSkill(updatedStats, e.getDataId(), duration, (short) 0);
				}
				break;
			case ITEM:
				return GamePackets.writeUseItem(updatedStats, e.getDataId(), duration);
			case MOB_SKILL:
				return GamePackets.writeGiveDebuff(updatedStats, (short) e.getDataId(), e.getLevel(), duration, (short) 900);
		}
		return null;
	}

	private static byte[] getThirdPersonCastEffect(GameCharacter p, byte effectType, StatusEffectsData e, byte stance) {
		switch (e.getSourceType()) {
			case PLAYER_SKILL:
				return GamePackets.writeBuffMapVisualEffect(p, effectType, e.getDataId(), e.getLevel(), stance);
		}
		return null;
	}

	private static byte[] getThirdPersonApplyEffect(GameCharacter p, StatusEffectsData e, Map<PlayerStatusEffect, Short> updatedStats, int duration) {
		switch (e.getSourceType()) {
			case PLAYER_SKILL:
				switch (e.getDataId()) {
					case Skills.CHAKRA:
					case Skills.NL_NINJA_AMBUSH:
					case Skills.SHADOWER_NINJA_AMBUSH:
					case Skills.THREATEN:
					case Skills.FP_SLOW:
					case Skills.IL_SLOW:
					case Skills.FP_SEAL:
					case Skills.IL_SEAL:
					case Skills.DOOM:
					case Skills.SHADOW_WEB:
					case Skills.WINGS:
						break;
					case Skills.DASH:
						return GamePackets.writeBuffMapPirateEffect(p, updatedStats, e.getDataId(), duration);
					case Skills.SWORD_FIRE_CHARGE:
					case Skills.BW_FLAME_CHARGE:
					case Skills.SWORD_ICE_CHARGE:
					case Skills.BW_BLIZZARD_CHARGE:
					case Skills.SWORD_THUNDER_CHARGE:
					case Skills.BW_LIGHTNING_CHARGE:
						return GamePackets.writeBuffMapChargeEffect(p, updatedStats, e.getDataId(), (short) 600);
					case Skills.SHADOW_STARS:
						return GamePackets.writeBuffMapShadowStarsEffect(p, updatedStats, -1, (short) 0);
					default:
						if (duration > 0)
							return GamePackets.writeBuffMapEffect(p, updatedStats);
						break;
				}
				break;
			case ITEM:
				if (duration > 0)
					return GamePackets.writeBuffMapEffect(p, updatedStats);
				break;
			case MOB_SKILL:
				return GamePackets.writeDebuffMapEffect(p, updatedStats, (short) e.getDataId(), e.getLevel(), (short) 900);
		}
		return null;
	}

	private static byte[] getFirstPersonCancelEffect(GameCharacter p) {
		return null;
	}

	private static byte[] getFirstPersonDispelEffect(GameCharacter p, StatusEffectsData e) {
		return GamePackets.writeCancelStatusEffect(e.getEffects());
	}

	private static byte[] getThirdPersonCancelEffect(GameCharacter p) {
		return null;
	}

	private static byte[] getThirdPersonDispelEffect(GameCharacter p, StatusEffectsData e, Set<PlayerStatusEffect> updatedStats) {
		return GamePackets.writeCancelStatusEffectMapEffect(p, updatedStats);
	}

	public static Map<PlayerStatusEffect, Short> applyEffects(GameCharacter p, StatusEffectsData e) {
		Map<PlayerStatusEffect, Short> updatedStats = new EnumMap<PlayerStatusEffect, Short>(PlayerStatusEffect.class);
		for (PlayerStatusEffect buff : e.getEffects()) {
			PlayerStatusEffectValues v = p.getEffectValue(buff);
			if (v != null)
				dispelEffectsAndShowVisuals(p, v.getEffectsData());
			v = applyEffect(p, e, buff);
			updatedStats.put(buff, Short.valueOf(v.getModifier()));
			p.addToActiveEffects(buff, v);
		}
		return updatedStats;
	}

	public static void applyEffectsAndShowVisuals(GameCharacter p, byte effectType, StatusEffectsData e, byte stance, int duration) {
		Map<PlayerStatusEffect, Short> updatedStats = applyEffects(p, e);
		byte[] effect = getFirstPersonCastEffect(p, effectType, e, stance);
		if (effect != null)
			p.getClient().getSession().send(effect);
		effect = getFirstPersonApplyEffect(p, e, updatedStats, duration);
		if (effect != null)
			p.getClient().getSession().send(effect);
		effect = getThirdPersonCastEffect(p, effectType, e, stance);
		if (p.isVisible() && effect != null)
			p.getMap().sendToAll(effect, p);
		effect = getThirdPersonApplyEffect(p, e, updatedStats, duration);
		if (p.isVisible() && effect != null)
			p.getMap().sendToAll(effect, p);
	}

	public static void applyEffectsAndShowVisuals(GameCharacter p, byte effectType, StatusEffectsData e, byte stance) {
		applyEffectsAndShowVisuals(p, effectType, e, stance, e.getDuration());
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
		byte[] effect = getFirstPersonCancelEffect(p);
		if (effect != null)
			p.getClient().getSession().send(effect);
		effect = getFirstPersonDispelEffect(p, e);
		if (effect != null)
			p.getClient().getSession().send(effect);
		effect = getThirdPersonCancelEffect(p);
		if (p.isVisible() && effect != null)
			p.getMap().sendToAll(effect, p);
		effect = getThirdPersonDispelEffect(p, e, e.getEffects());
		if (p.isVisible() && effect != null)
			p.getMap().sendToAll(effect, p);
	}

	private static PlayerStatusEffectValues applyEffect(final GameCharacter p, StatusEffectsData e, PlayerStatusEffect buff) {
		short mod = 0;
		switch (buff) {
			case SUMMON:
				mod = 1;
				break;
			case PUPPET:
				PlayerSkillSummon summon = p.getSummonBySkill(e.getDataId());
				summon.setHp((short) ((PlayerSkillEffectsData) e).getX());
				break;
			case SLOW:
				mod = (short) ((MobSkillEffectsData) e).getX();
				p.setBaseSpeed((byte) mod);
				break;
			case HOMING_BEACON:
				mod = 1;
				break;
			case MORPH:
				mod = (short) (((BuffsData) e).getMorph() + p.getGender() * 100);
				break;
			case RECOVERY: {
				//TODO: get packet for showing HP recovered (blue numbers above
				//player's head when standing still or sitting on a chair)
				final int period = e.getDuration() / 6;
				final int gain = ((PlayerSkillEffectsData) e).getX();

				//essentially scheduleWithFixedDelay with a repeat limit of 5.
				//okay to send p in here because the GameCharacter is held for
				//no longer than 30 seconds.
				Scheduler.getInstance().runAfterDelay(new Runnable() {
					private volatile int iteration = 0;

					@Override
					public void run() {
						if (p.isEffectActive(PlayerStatusEffect.RECOVERY) && iteration < 6) {
							iteration++;
							p.gainHp(gain);
							Scheduler.getInstance().runAfterDelay(this, period);
						}
					}
				}, period);
				break;
			}
			case MAPLE_WARRIOR:
				//TODO: server side buff all stats with %
				mod = (short) ((PlayerSkillEffectsData) e).getX();
				break;
			case POWER_STANCE:
				mod = (short) ((PlayerSkillEffectsData) e).getProp();
				break;
			case SHARP_EYES:
				mod = (short) (((PlayerSkillEffectsData) e).getX() << 8 | ((PlayerSkillEffectsData) e).getY());
				break;
			case MANA_REFLECTION:
				mod = (short) ((PlayerSkillEffectsData) e).getX();
				break;
			case SEDUCE:
				mod = 1;
				break;
			case SHADOW_STARS:
				mod = -1;
				break;
			case INFINITY:
				mod = (short) ((PlayerSkillEffectsData) e).getX();
				break;
			case HOLY_SHIELD:
				mod = (short) ((PlayerSkillEffectsData) e).getX();
				break;
			case HAMSTRING:
				mod = (short) ((PlayerSkillEffectsData) e).getX();
				break;
			case BLIND:
				mod = (short) ((PlayerSkillEffectsData) e).getX();
				break;
			case CONCENTRATE:
				mod = (short) ((PlayerSkillEffectsData) e).getX();
				p.addWatk(mod);
				break;
			case ECHO_OF_HERO:
				mod = (short) ((PlayerSkillEffectsData) e).getX();
				p.addWatk(mod);
				p.addMatk(mod);
				break;
			case ENERGY_CHARGE:
				assert false;
				mod = p.getEnergyCharge();
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
				mod = (short) ((PlayerSkillEffectsData) e).getX();
				break;
			case WATK:
				if (!e.getEffects().contains(PlayerStatusEffect.SUMMON)) {
					mod = ((BuffsData) e).getWatk();
					p.addWatk(mod);
				}
				break;
			case WDEF:
				mod = ((BuffsData) e).getWdef();
				p.addWdef(mod);
				break;
			case MATK:
				if (!e.getEffects().contains(PlayerStatusEffect.SUMMON)) {
					mod = ((BuffsData) e).getMatk();
					p.addMatk(mod);
				}
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
				mod = ((BuffsData) e).getSpeed();
				p.addSpeed(mod);
				break;
			case JUMP:
				mod = ((BuffsData) e).getJump();
				p.addJump(mod);
				break;
			case MAGIC_GUARD:
				mod = (short) ((PlayerSkillEffectsData) e).getX();
				break;
			case DARKSIGHT:
				mod = 1;
				break;
			case HIDE:
				mod = 0;
				p.getClient().getSession().send(GamePackets.writeShowHide());
				p.getMap().hidePlayer(p);
				break;
			case BOOSTER:
				mod = (short) ((PlayerSkillEffectsData) e).getX();
				break;
			case POWER_GUARD:
				mod = (short) ((PlayerSkillEffectsData) e).getX();
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
				mod = (short) ((PlayerSkillEffectsData) e).getX();
				break;
			case SPEED_INFUSION:
				mod = (short) ((PlayerSkillEffectsData) e).getX();
				break;
			case SOUL_ARROW:
				mod = (short) ((PlayerSkillEffectsData) e).getX();
				break;
			case STUN:
				mod = 1;
				break;
			case POISON:
				mod = (short) ((MobSkillEffectsData) e).getX();
				break;
			case SEAL:
				mod = 1;
				break;
			case DARKNESS:
				mod = 1;
				break;
			case COMBO:
				mod = (short) ((PlayerSkillEffectsData) e).getX();
				break;
			case CHARGE:
				mod = (short) ((PlayerSkillEffectsData) e).getX();
				break;
			case DRAGON_BLOOD:
				mod = (short) ((PlayerSkillEffectsData) e).getX();
				break;
			case HOLY_SYMBOL:
				mod = (short) ((PlayerSkillEffectsData) e).getX();
				break;
			case MESO_UP:
				mod = (short) ((PlayerSkillEffectsData) e).getX();
				break;
			case SHADOW_PARTNER:
				mod = (short) ((PlayerSkillEffectsData) e).getX();
				break;
			case PICKPOCKET:
				mod = (short) ((PlayerSkillEffectsData) e).getX();
				break;
			case MESO_GUARD:
				mod = (short) ((PlayerSkillEffectsData) e).getX();
				break;
			case WEAKNESS:
				mod = 1;
				break;
			case CURSE:
				mod = 1;
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
				p.setBaseSpeed((byte) 100);
				break;
			case HOMING_BEACON:
				break;
			case MORPH:
				break;
			case RECOVERY:
				//next iteration of recovery will detect if the skill is no
				//longer active, so no need to manually cancel the task here.
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
				p.addWatk(-value.getModifier());
				break;
			case ECHO_OF_HERO:
				p.addWatk(-value.getModifier());
				p.addMatk(-value.getModifier());
				break;
			case ENERGY_CHARGE:
				assert false;
				break;
			case DASH_SPEED:
				p.addSpeed(-value.getModifier());
				break;
			case DASH_JUMP:
				p.addJump(-value.getModifier());
				break;
			case MONSTER_RIDING:
				break;
			case WATK:
				if (!value.getEffectsData().getEffects().contains(PlayerStatusEffect.SUMMON))
					p.addWatk(-value.getModifier());
				break;
			case WDEF:
				p.addWdef(-value.getModifier());
				break;
			case MATK:
				if (!value.getEffectsData().getEffects().contains(PlayerStatusEffect.SUMMON))
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
			case WEAKNESS:
				break;
			case CURSE:
				break;
		}
	}

	private StatusEffectTools() {
		//uninstantiable...
	}
}
