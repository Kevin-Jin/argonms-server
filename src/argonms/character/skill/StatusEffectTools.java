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

package argonms.character.skill;

import argonms.GlobalConstants;
import argonms.character.Player;
import argonms.character.skill.PlayerStatusEffectValues.PlayerStatusEffect;
import argonms.loading.BuffsData;
import argonms.loading.StatusEffectsData;
import argonms.loading.item.ItemDataLoader;
import argonms.loading.item.ItemEffectsData;
import argonms.loading.skill.PlayerSkillEffectsData;
import argonms.loading.skill.SkillDataLoader;
import argonms.net.external.CommonPackets;

/**
 *
 * @author GoldenKevin
 */
public class StatusEffectTools {
	public static final byte
		LEVEL_UP = 0,
		PASSIVE_BUFF = 1,
		ACTIVE_BUFF = 2, //energy charge + party buffs + beholder?
		ITEM_GAIN = 3,
		PET_LVL_UP = 3,
		DRAGON_BLOOD = 5,
		JOB_ADVANCEMENT = 8,
		QUEST = 9,
		MOB_BUFF = 11
	;

	//TODO: IMPLEMENT
	private static byte[] getThirdPersonCastEffect(Player p, BuffsData e) {
		switch (e.getSourceType()) {
			case PLAYER_SKILL:
				switch (e.getDataId()) {
					case Skills.FP_MP_EATER:
					case Skills.IL_MP_EATER:
					case Skills.CLERIC_MP_EATER:
						return CommonPackets.writeBuffEffect(p, PASSIVE_BUFF, e.getDataId(), e.getLevel(), (byte) 3);
				}
				break;
			case ITEM:
				break;
		}
		return null;
	}

	//TODO: IMPLEMENT
	private static byte[] getThirdPersonDispelEffect(Player p, BuffsData e) {
		return null;
	}

	/**
	 * Cast a buff skill of the specified skill id with the specified skill
	 * level.
	 * @param p the Player that will cast the skill
	 * @param skillId the identifier of the skill to use
	 * @param skillLevel the amount of skill points the Player has in the skill
	 */
	public static void useSkill(Player p, int skillId, byte skillLevel) {
		PlayerSkillEffectsData e = SkillDataLoader.getInstance().getSkill(skillId).getLevel(skillLevel);
		p.getClient().getSession().send(CommonPackets.writeUseSkill(p, p.applyEffect(e), skillId, e.getDuration()));
		byte[] mapEffect = getThirdPersonCastEffect(p, e);
		if (p.isVisible() && mapEffect != null) {
			p.getMap().sendToAll(mapEffect, p);
		}
	}

	/**
	 * Cast a buff skill of the specified skill id using the Player's current
	 * level of that particular skill.
	 * @param p the Player that will cast the skill
	 * @param skillId the identifier of the skill to use
	 * @return true if the Player has at least one skill point in the given
	 * skill, false if the Player has no skill points in that skill and thus
	 * could not cast it.
	 */
	public static boolean useSkill(Player p, int skillId) {
		byte skillLevel = p.getSkillLevel(skillId);
		if (skillLevel != 0) {
			useSkill(p, skillId, skillLevel);
			return true;
		}
		return false;
	}

	/**
	 * Consume a item of the specified item id that gives a buff effect.
	 * @param p the Player that will consume the item
	 * @param itemId the identifier of the item to use
	 */
	public static void useItem(Player p, int itemId) {
		ItemEffectsData e = ItemDataLoader.getInstance().getEffect(itemId);
		p.getClient().getSession().send(CommonPackets.writeUseItem(p, p.applyEffect(e), itemId, e.getDuration()));
		byte[] mapEffect = getThirdPersonCastEffect(p, e);
		if (p.isVisible() && mapEffect != null) {
			p.getMap().sendToAll(mapEffect, p);
		}
		if (e.getHpRecover() != 0)
			p.gainHp(e.getHpRecover());
		if (e.getHpRecoverPercent() != 0)
			p.gainHp(Math.round(e.getHpRecoverPercent() * p.getHp() / 100f));
		if (e.getMpRecover() != 0)
			p.gainMp(e.getMpRecover());
		if (e.getMpRecoverPercent() != 0)
			p.gainMp(Math.round(e.getMpRecoverPercent() * p.getMp() / 100f));
		if (e.getMoveTo() != 0) {
			if (e.getMoveTo() == GlobalConstants.NULL_MAP)
				p.changeMap(p.getMap().getReturnMap());
			else
				p.changeMap(e.getMoveTo());
		}
		//TODO: clear any debuff if e.curesXxx
	}

	public static void cancelSkill(Player p, int skillId, byte skillLevel) {
		PlayerSkillEffectsData e = SkillDataLoader.getInstance().getSkill(skillId).getLevel(skillLevel);
		p.dispelEffect(e);
		p.getClient().getSession().send(CommonPackets.writeCancelSkillOrItem(p, e.getEffects()));
		byte[] mapEffect = getThirdPersonDispelEffect(p, e);
		if (p.isVisible() && mapEffect != null) {
			p.getMap().sendToAll(mapEffect, p);
		}
	}

	public static void cancelSkill(Player p, int skillId) {
		//even if we cast the skill at an earlier level, the individual effects
		//for each level should be the same. That's all we need to call
		//Player.dispelEffect...
		byte skillLevel = p.getSkillLevel(skillId);
		if (skillLevel <= 0) //casted a skill if we don't have any levels in it
			skillLevel = 1; //it happens!
		cancelSkill(p, skillId, skillLevel);
	}

	public static void cancelItem(Player p, int itemId) {
		ItemEffectsData e = ItemDataLoader.getInstance().getEffect(itemId);
		p.dispelEffect(e);
		p.getClient().getSession().send(CommonPackets.writeCancelSkillOrItem(p, e.getEffects()));
		byte[] mapEffect = getThirdPersonDispelEffect(p, e);
		if (p.isVisible() && mapEffect != null) {
			p.getMap().sendToAll(mapEffect, p);
		}
	}

	//Helper method for Player. DO NOT CALL THIS FROM ANYWHERE ELSE UNLESS YOU
	//HAVE A FIRM UNDERSTANDING OF WHAT IT DOES.
	public static PlayerStatusEffectValues applyEffect(Player p, StatusEffectsData e, PlayerStatusEffect buff) {
		short mod = -1;
		switch (buff) {
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
			case STANCE:
				break;
			case SHARP_EYES:
				break;
			case MANA_REFLECTION:
				break;
			case SEDUCE:
				break;
			case DRAGON_ROAR:
				break;
			case SHADOW_CLAW:
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
			case BERSERK_FURY:
				break;
			case DIVINE_BODY:
				break;
			case MONSTER_RIDING:
				break;
			case FINALATTACK:
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
			case DASH:
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
				p.getClient().getSession().send(CommonPackets.writeShowHide());
				p.getMap().hidePlayer(p);
				break;
			case BOOSTER:
				break;
			case POWERGUARD:
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
			case SOULARROW:
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
			case SUMMON:
				break;
			case WK_CHARGE:
				break;
			case DRAGONBLOOD:
				break;
			case HOLY_SYMBOL:
				mod = (short) ((PlayerSkillEffectsData) e).getX();
				break;
			case MESOUP:
				break;
			case SHADOWPARTNER:
				break;
			case PICKPOCKET:
				break;
			case PUPPET:
				break;
			case MESOGUARD:
				break;
			case WEAKEN:
				break;
			case CURSE:
				break;
		}
		return new PlayerStatusEffectValues(e, mod);
	}

	//Helper method for Player. DO NOT CALL THIS FROM ANYWHERE ELSE UNLESS YOU
	//HAVE A FIRM UNDERSTANDING OF WHAT IT DOES.
	public static void dispelEffect(Player p, PlayerStatusEffect key, PlayerStatusEffectValues value) {
		switch (key) {
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
			case STANCE:
				break;
			case SHARP_EYES:
				break;
			case MANA_REFLECTION:
				break;
			case SEDUCE:
				break;
			case DRAGON_ROAR:
				break;
			case SHADOW_CLAW:
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
			case BERSERK_FURY:
				break;
			case DIVINE_BODY:
				break;
			case MONSTER_RIDING:
				break;
			case FINALATTACK:
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
			case DASH:
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
				p.getClient().getSession().send(CommonPackets.writeStopHide());
				p.getMap().unhidePlayer(p);
				break;
			case BOOSTER:
				break;
			case POWERGUARD:
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
			case SOULARROW:
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
			case SUMMON:
				break;
			case WK_CHARGE:
				break;
			case DRAGONBLOOD:
				break;
			case HOLY_SYMBOL:
				break;
			case MESOUP:
				break;
			case SHADOWPARTNER:
				break;
			case PICKPOCKET:
				break;
			case PUPPET:
				break;
			case MESOGUARD:
				break;
			case WEAKEN:
				break;
			case CURSE:
				break;
		}
	}
}
