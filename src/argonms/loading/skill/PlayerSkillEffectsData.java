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

package argonms.loading.skill;

import argonms.character.skill.PlayerStatusEffectValues.PlayerStatusEffect;
import argonms.character.skill.Skills;
import argonms.loading.BuffsData;
import argonms.map.MonsterStatusEffect;
import argonms.tools.Rng;

import java.awt.Point;
import java.util.EnumSet;
import java.util.Set;

/**
 *
 * @author GoldenKevin
 */
public class PlayerSkillEffectsData extends BuffsData {
	private short mpCon;
	private short hpCon;
	private int x;
	private int y;
	private int z;
	private short damage;
	private Point lt;
	private Point rb;
	private byte mobCount;
	private double prop;
	private byte mastery;
	private short cooltime;
	private short range;
	private byte attackCount;
	private byte bulletCount;
	private int itemCon;
	private byte itemConCount;
	private short bulletCon;
	private short moneyCon;
	private Set<MonsterStatusEffect> monsterDiseases;
	private boolean isFreeze;
	private byte level;

	protected PlayerSkillEffectsData(int skillid, byte level) {
		super(skillid);
		this.level = level;
		this.monsterDiseases = EnumSet.noneOf(MonsterStatusEffect.class);
		switch (skillid) { //for them skills that don't have x or y
			case Skills.HIDE:
				effects.add(PlayerStatusEffect.HIDE);
				//kinda hacky, but hide doesn't have a duration
				setDuration(60 * 120 * 1000);
				break;
		}
	}

	public EffectSource getSourceType() {
		return EffectSource.PLAYER_SKILL;
	}

	protected void setMpConsume(short mpCon) {
		this.mpCon = mpCon;
	}

	protected void setHpConsume(short hpCon) {
		this.hpCon = hpCon;
	}

	/* Here are the mappings between BuffValue and the data fields for
	 * when copying code from OdinMS.
			case Skills.MAGIC_GUARD:
				effects.put(BuffKey.MAGIC_GUARD, Integer.valueOf(x));
				break;
			case Skills.INVINCIBLE:
				effects.put(BuffKey.INVINCIBLE, Integer.valueOf(x));
				break;
			case Skills.HIDE:
				effects.put(BuffKey.HIDE, null);
				duration = 60 * 120 * 1000;
				break;
			case Skills.DARK_SIGHT:
				effects.put(BuffKey.DARKSIGHT, Integer.valueOf(x));
				break;
			case Skills.PICK_POCKET:
				effects.put(BuffKey.PICKPOCKET, Integer.valueOf(x));
				break;
			case Skills.MESO_GUARD:
				effects.put(BuffKey.MESOGUARD, Integer.valueOf(x));
				break;
			case Skills.MESO_UP:
				effects.put(BuffKey.MESOUP, Integer.valueOf(x));
				break;
			case Skills.SHADOW_PARTNER:
				effects.put(BuffKey.SHADOWPARTNER, Integer.valueOf(x));
				break;
			case Skills.BOW_SOUL_ARROW:
			case Skills.XBOW_SOUL_ARROW:
			case Skills.MYSTIC_DOOR: // hacked buff icon
				effects.put(BuffKey.SOULARROW, Integer.valueOf(x));
				break;
			case Skills.SWORD_FIRE_CHARGE:
			case Skills.BW_FLAME_CHARGE:
			case Skills.SWORD_ICE_CHARGE:
			case Skills.BW_BLIZZARD_CHARGE:
			case Skills.SWORD_THUNDER_CHARGE:
			case Skills.BW_LIGHTNING_CHARGE:
			case Skills.SWORD_HOLY_CHARGE:
			case Skills.BW_DIVINE_CHARGE:
				effects.put(BuffKey.WK_CHARGE, Integer.valueOf(x));
				break;
			case Skills.CRUSADER_SWORD_BOOSTER:
			case Skills.AXE_BOOSTER:
			case Skills.PAGE_SWORD_BOOSTER:
			case Skills.BW_BOOSTER:
			case Skills.SPEAR_BOOSTER:
			case Skills.POLE_ARM_BOOSTER:
			case Skills.FP_SPELL_BOOSTER:
			case Skills.IL_SPELL_BOOSTER:
			case Skills.BOW_BOOSTER:
			case Skills.XBOW_BOOSTER:
			case Skills.CLAW_BOOSTER:
			case Skills.DAGGER_BOOSTER:
			case Skills.KNUCKLER_BOOSTER:
			case Skills.GUN_BOOSTER:
				effects.put(BuffKey.BOOSTER, Integer.valueOf(x));
				break;
			case Skills.SPEED_INFUSION:
				effects.put(BuffKey.SPEED_INFUSION, Integer.valueOf(x));
				break;
			case Skills.RAGE:
				effects.put(BuffKey.WDEF, Short.valueOf(d.getWdef()));
			case Skills.ENRAGE:
				effects.put(BuffKey.WATK, Short.valueOf(d.getWatk()));
				break;
			case Skills.IRON_WILL:
				effects.put(BuffKey.MDEF, Short.valueOf(d.getMdef()));
			case Skills.IRON_BODY:
				effects.put(BuffKey.WDEF, Short.valueOf(d.getWdef()));
				break;
			case Skills.MAGIC_ARMOR:
				effects.put(BuffKey.WDEF, Short.valueOf(d.getWdef()));
				break;
			case Skills.FP_MEDITATION:
			case Skills.IL_MEDITATION:
				effects.put(BuffKey.MATK, Short.valueOf(d.getMatk()));
				break;
			case Skills.SIN_HASTE:
			case Skills.DIT_HASTE:
			case Skills.GM_HASTE:
				effects.put(BuffKey.SPEED, Short.valueOf(d.getSpeed()));
				effects.put(BuffKey.JUMP, Short.valueOf(d.getJump()));
				break;
			case Skills.CLERIC_BLESS:
				effects.put(BuffKey.WDEF, Short.valueOf(d.getWdef()));
				effects.put(BuffKey.MDEF, Short.valueOf(d.getMdef()));
			case Skills.FOCUS:
				effects.put(BuffKey.ACC, Short.valueOf(d.getAcc()));
				effects.put(BuffKey.AVOID, Short.valueOf(d.getAvoid()));
				break;
			case Skills.GM_BLESS:
				effects.put(BuffKey.MATK, Short.valueOf(d.getMatk()));
			case Skills.CONCENTRATE:
				effects.put(BuffKey.CONCENTRATE, Integer.valueOf(x));
				effects.put(BuffKey.WATK, Short.valueOf(d.getWatk()));
				break;
			case Skills.DASH:
				effects.put(BuffKey.DASH, null);
				break;
			case Skills.FIGHTER_POWER_GUARD:
			case Skills.PAGE_POWER_GUARD:
				effects.put(BuffKey.POWERGUARD, Integer.valueOf(x));
				break;
			case Skills.SPEARMAN_HYPER_BODY:
			case Skills.GM_HYPER_BODY:
				effects.put(BuffKey.MAXHP, Integer.valueOf(x));
				effects.put(BuffKey.MAXMP, Integer.valueOf(y));
				break;
			case Skills.RECOVERY:
				effects.put(BuffKey.RECOVERY, Integer.valueOf(x));
				break;
			case Skills.COMBO:
				effects.put(BuffKey.COMBO, Integer.valueOf(1));
				break;
			case Skills.MONSTER_RIDING:
			case Skills.BATTLE_SHIP:
				effects.put(BuffKey.MONSTER_RIDING, null);
				break;
			case Skills.DRAGON_ROAR:
				this.hpR = -x / 100.0;
				break;
			case Skills.DRAGON_BLOOD:
				effects.put(BuffKey.DRAGONBLOOD, Integer.valueOf(x));
				break;
			case Skills.HERO_MAPLE_WARRIOR:
			case Skills.PALADIN_MAPLE_WARRIOR:
			case Skills.DARK_KNIGHT_MAPLE_WARRIOR:
			case Skills.FP_MAPLE_WARRIOR:
			case Skills.IL_MAPLE_WARRIOR:
			case Skills.BISHOP_MAPLE_WARRIOR:
			case Skills.BOW_MASTER_MAPLE_WARRIOR:
			case Skills.XBOW_MASTER_MAPLE_WARRIOR:
			case Skills.NL_MAPLE_WARRIOR:
			case Skills.SHADOWER_MAPLE_WARRIOR:
			case Skills.BUCCANEER_MAPLE_WARRIOR:
			case Skills.CORSAIR_MAPLE_WARRIOR:
				effects.put(BuffKey.MAPLE_WARRIOR, Integer.valueOf(x));
				break;
			case Skills.BOW_MASTER_SHARP_EYES:
			case Skills.XBOW_MASTER_SHARP_EYES:
				// hack much (TODO is the order correct?)
				effects.put(BuffKey.SHARP_EYES, Integer.valueOf(x << 8 | y));
				break;
			case Skills.BEHOLDER:
			case Skills.IFRIT:
			case Skills.SUMMON_DRAGON:
			case Skills.BAHAMUT:
			case Skills.PHOENIX:
			case Skills.OCTOPUS:
			case Skills.GAVIOTA:
			case Skills.WRATH_OF_THE_OCTOPI:
				effects.put(BuffKey.SUMMON, null);
				break;
			case Skills.CLERIC_HOLY_SYMBOL:
			case Skills.GM_HOLY_SYMBOL:
				effects.put(BuffKey.HOLY_SYMBOL, Integer.valueOf(x));
				break;
			case Skills.SHADOW_STARS:
				effects.put(BuffKey.SHADOW_CLAW, null);
				break;
			case Skills.FP_INFINITY:
			case Skills.IL_INFINITY:
			case Skills.BISHOP_INFINITY:
				effects.put(BuffKey.INFINITY, Integer.valueOf(x));
				break;
			case Skills.HERO_POWER_STANCE:
			case Skills.PAGE_POWER_STANCE:
			case Skills.DARK_KNIGHT_POWER_STANCE:
				effects.put(BuffKey.STANCE, Double.valueOf(d.getProp()));
				break;
			case Skills.ECHO_OF_HERO:
				effects.put(BuffKey.ECHO_OF_HERO, Integer.valueOf(x));
				break;
			case Skills.FP_MANA_REFLECTION:
			case Skills.IL_MANA_REFLECTION:
			case Skills.BISHOP_MANA_REFLECTION:
				effects.put(BuffKey.MANA_REFLECTION, null);
				break;
			case Skills.HOLY_SHIELD:
				effects.put(BuffKey.HOLY_SHIELD, Integer.valueOf(x));
				break;
			case Skills.BOW_PUPPET:
			case Skills.XBOW_PUPPET:
				effects.put(BuffKey.PUPPET, null);
				break;

			case Skills.DISORDER:
				monsterStatus.put(MonsterStatus.WATK, Integer.valueOf(x));
				monsterStatus.put(MonsterStatus.WDEF, Integer.valueOf(y));
				break;
			case Skills.THREATEN:
				monsterStatus.put(MonsterStatus.WATK, Integer.valueOf(x));
				monsterStatus.put(MonsterStatus.WDEF, Integer.valueOf(y));
				break;
			case Skills.SWORD_COMA:
			case Skills.AXE_COMA:
			case Skills.SHOUT:
			case Skills.CHARGED_BLOW:
			case Skills.ARROW_BOMB:
			case Skills.ASSAULTER:
			case Skills.BOOMERANG_STEP:
			case Skills.BACKSPIN_BLOW:
			case Skills.DOUBLE_UPPERCUT:
			case Skills.DEMOLITION:
			case Skills.SNATCH:
			case Skills.BARRAGE:
			case Skills.BLANK_SHOT:
				monsterStatus.put(MonsterStatus.STUN, null);
				break;
			case Skills.NL_TAUNT:
			case Skills.SHADOWER_TAUNT:
				monsterStatus.put(MonsterStatus.TAUNT, null);
				break;
			case Skills.COLD_BEAM:
			case Skills.ICE_STRIKE:
			case Skills.IL_ELEMENT_COMPOSITION:
			case Skills.IL_BLIZZARD:
			case Skills.XBOW_BLIZZARD:
			case Skills.ICE_SPLITTER:
				monsterStatus.put(MonsterStatus.FREEZE, null);
				duration *= 2; // freezing skills are a little strange
				break;
			case Skills.PARALYZE:
			case Skills.FP_SLOW:
			case Skills.IL_SLOW:
				monsterStatus.put(MonsterStatus.SPEED, Integer.valueOf(x));
				break;
			case Skills.POISON_BREATH:
			case Skills.FP_ELEMENT_COMPOSITION:
				monsterStatus.put(MonsterStatus.POISON, null);
				break;
			case Skills.DOOM:
				monsterStatus.put(MonsterStatus.DOOM, null);
				break;
			case Skills.SILVER_HAWK:
			case Skills.GOLDEN_EAGLE:
				effects.put(BuffKey.SUMMON, null);
				monsterStatus.put(MonsterStatus.STUN, null);
				break;
			case Skills.ELQUINES:
			case Skills.FROSTPREY:
				effects.put(BuffKey.SUMMON, null);
				monsterStatus.put(MonsterStatus.FREEZE, null);
				break;
			case Skills.FP_SEAL:
			case Skills.IL_SEAL:
				monsterStatus.put(MonsterStatus.SEAL, null);
				break;
			case Skills.SHADOW_WEB:
				monsterStatus.put(MonsterStatus.SHADOW_WEB, null);
				break;
			case Skills.HAMSTRING:
				effects.put(BuffKey.HAMSTRING, Integer.valueOf(x));
				monsterStatus.put(MonsterStatus.SPEED, x);
				break;
			case Skills.BLIND:
				effects.put(BuffKey.BLIND, Integer.valueOf(x));
				monsterStatus.put(MonsterStatus.ACC, x);
				break;
			case Skills.NL_NINJA_AMBUSH:
			case Skills.SHADOWER_NINJA_AMBUSH:
				monsterStatus.put(MonsterStatus.NINJA_AMBUSH, null);
				break;
			case Skills.HYPNOTIZE:
				monsterStatus.put(MonsterStatus.INERTMOB, null);
				break;
	 */
	protected void setX(int x) {
		this.x = x;
		switch (getDataId()) {
			case Skills.MAGIC_GUARD:
				effects.add(PlayerStatusEffect.MAGIC_GUARD);
				break;
			case Skills.INVINCIBLE:
				effects.add(PlayerStatusEffect.INVINCIBLE);
				break;
			case Skills.DARK_SIGHT:
				effects.add(PlayerStatusEffect.DARKSIGHT);
				break;
			case Skills.PICK_POCKET:
				effects.add(PlayerStatusEffect.PICKPOCKET);
				break;
			case Skills.MESO_GUARD:
				effects.add(PlayerStatusEffect.MESOGUARD);
				break;
			case Skills.MESO_UP:
				effects.add(PlayerStatusEffect.MESOUP);
				break;
			case Skills.SHADOW_PARTNER:
				effects.add(PlayerStatusEffect.SHADOWPARTNER);
				break;
			case Skills.BOW_SOUL_ARROW:
			case Skills.XBOW_SOUL_ARROW:
			case Skills.MYSTIC_DOOR: // hacked buff icon
				effects.add(PlayerStatusEffect.SOULARROW);
				break;
			case Skills.SWORD_FIRE_CHARGE:
			case Skills.BW_FLAME_CHARGE:
			case Skills.SWORD_ICE_CHARGE:
			case Skills.BW_BLIZZARD_CHARGE:
			case Skills.SWORD_THUNDER_CHARGE:
			case Skills.BW_LIGHTNING_CHARGE:
			case Skills.SWORD_HOLY_CHARGE:
			case Skills.BW_DIVINE_CHARGE:
				effects.add(PlayerStatusEffect.WK_CHARGE);
				break;
			case Skills.CRUSADER_SWORD_BOOSTER:
			case Skills.AXE_BOOSTER:
			case Skills.PAGE_SWORD_BOOSTER:
			case Skills.BW_BOOSTER:
			case Skills.SPEAR_BOOSTER:
			case Skills.POLE_ARM_BOOSTER:
			case Skills.FP_SPELL_BOOSTER:
			case Skills.IL_SPELL_BOOSTER:
			case Skills.BOW_BOOSTER:
			case Skills.XBOW_BOOSTER:
			case Skills.CLAW_BOOSTER:
			case Skills.DAGGER_BOOSTER:
			case Skills.KNUCKLER_BOOSTER:
			case Skills.GUN_BOOSTER:
				effects.add(PlayerStatusEffect.BOOSTER);
				break;
			case Skills.SPEED_INFUSION:
				effects.add(PlayerStatusEffect.SPEED_INFUSION);
				break;
			case Skills.CONCENTRATE:
				effects.add(PlayerStatusEffect.CONCENTRATE);
				break;
			case Skills.DASH:
				effects.add(PlayerStatusEffect.DASH);
				break;
			case Skills.FIGHTER_POWER_GUARD:
			case Skills.PAGE_POWER_GUARD:
				effects.add(PlayerStatusEffect.POWERGUARD);
				break;
			case Skills.SPEARMAN_HYPER_BODY:
			case Skills.GM_HYPER_BODY:
				effects.add(PlayerStatusEffect.MAXHP);
				break;
			case Skills.RECOVERY:
				effects.add(PlayerStatusEffect.RECOVERY);
				break;
			case Skills.COMBO:
				effects.add(PlayerStatusEffect.COMBO);
				break;
			case Skills.MONSTER_RIDING:
			case Skills.BATTLE_SHIP:
				effects.add(PlayerStatusEffect.MONSTER_RIDING);
				break;
			case Skills.DRAGON_BLOOD:
				effects.add(PlayerStatusEffect.DRAGONBLOOD);
				break;
			case Skills.HERO_MAPLE_WARRIOR:
			case Skills.PALADIN_MAPLE_WARRIOR:
			case Skills.DARK_KNIGHT_MAPLE_WARRIOR:
			case Skills.FP_MAPLE_WARRIOR:
			case Skills.IL_MAPLE_WARRIOR:
			case Skills.BISHOP_MAPLE_WARRIOR:
			case Skills.BOW_MASTER_MAPLE_WARRIOR:
			case Skills.XBOW_MASTER_MAPLE_WARRIOR:
			case Skills.NL_MAPLE_WARRIOR:
			case Skills.SHADOWER_MAPLE_WARRIOR:
			case Skills.BUCCANEER_MAPLE_WARRIOR:
			case Skills.CORSAIR_MAPLE_WARRIOR:
				effects.add(PlayerStatusEffect.MAPLE_WARRIOR);
				break;
			case Skills.BOW_MASTER_SHARP_EYES:
			case Skills.XBOW_MASTER_SHARP_EYES:
				effects.add(PlayerStatusEffect.SHARP_EYES);
				break;
			case Skills.BEHOLDER:
			case Skills.IFRIT:
			case Skills.SUMMON_DRAGON:
			case Skills.BAHAMUT:
			case Skills.PHOENIX:
			case Skills.OCTOPUS:
			case Skills.GAVIOTA:
			case Skills.WRATH_OF_THE_OCTOPI:
				effects.add(PlayerStatusEffect.SUMMON);
				break;
			case Skills.CLERIC_HOLY_SYMBOL:
			case Skills.GM_HOLY_SYMBOL:
				effects.add(PlayerStatusEffect.HOLY_SYMBOL);
				break;
			case Skills.SHADOW_STARS:
				effects.add(PlayerStatusEffect.SHADOW_CLAW);
				break;
			case Skills.FP_INFINITY:
			case Skills.IL_INFINITY:
			case Skills.BISHOP_INFINITY:
				effects.add(PlayerStatusEffect.INFINITY);
				break;
			case Skills.HERO_POWER_STANCE:
			case Skills.PAGE_POWER_STANCE:
			case Skills.DARK_KNIGHT_POWER_STANCE:
				effects.add(PlayerStatusEffect.STANCE);
				break;
			case Skills.ECHO_OF_HERO:
				effects.add(PlayerStatusEffect.ECHO_OF_HERO);
				break;
			case Skills.FP_MANA_REFLECTION:
			case Skills.IL_MANA_REFLECTION:
			case Skills.BISHOP_MANA_REFLECTION:
				effects.add(PlayerStatusEffect.MANA_REFLECTION);
				break;
			case Skills.HOLY_SHIELD:
				effects.add(PlayerStatusEffect.HOLY_SHIELD);
				break;
			case Skills.BOW_PUPPET:
			case Skills.XBOW_PUPPET:
				effects.add(PlayerStatusEffect.PUPPET);
				break;

			case Skills.DISORDER:
				monsterDiseases.add(MonsterStatusEffect.WATK);
				break;
			case Skills.THREATEN:
				monsterDiseases.add(MonsterStatusEffect.WATK);
				break;
			case Skills.SWORD_COMA:
			case Skills.AXE_COMA:
			case Skills.SHOUT:
			case Skills.CHARGED_BLOW:
			case Skills.ARROW_BOMB:
			case Skills.ASSAULTER:
			case Skills.BOOMERANG_STEP:
			case Skills.BACKSPIN_BLOW:
			case Skills.DOUBLE_UPPERCUT:
			case Skills.DEMOLITION:
			case Skills.SNATCH:
			case Skills.BARRAGE:
			case Skills.BLANK_SHOT:
				monsterDiseases.add(MonsterStatusEffect.STUN);
				break;
			case Skills.NL_TAUNT:
			case Skills.SHADOWER_TAUNT:
				monsterDiseases.add(MonsterStatusEffect.TAUNT);
				break;
			case Skills.COLD_BEAM:
			case Skills.ICE_STRIKE:
			case Skills.IL_ELEMENT_COMPOSITION:
			case Skills.IL_BLIZZARD:
			case Skills.XBOW_BLIZZARD:
			case Skills.ICE_SPLITTER:
				monsterDiseases.add(MonsterStatusEffect.FREEZE);
				isFreeze = true;
				break;
			case Skills.PARALYZE:
			case Skills.FP_SLOW:
			case Skills.IL_SLOW:
				monsterDiseases.add(MonsterStatusEffect.SPEED);
				break;
			case Skills.POISON_BREATH:
			case Skills.FP_ELEMENT_COMPOSITION:
				monsterDiseases.add(MonsterStatusEffect.POISON);
				break;
			case Skills.DOOM:
				monsterDiseases.add(MonsterStatusEffect.DOOM);
				break;
			case Skills.SILVER_HAWK:
			case Skills.GOLDEN_EAGLE:
				effects.add(PlayerStatusEffect.SUMMON);
				monsterDiseases.add(MonsterStatusEffect.STUN);
				break;
			case Skills.ELQUINES:
			case Skills.FROSTPREY:
				effects.add(PlayerStatusEffect.SUMMON);
				monsterDiseases.add(MonsterStatusEffect.FREEZE);
				break;
			case Skills.FP_SEAL:
			case Skills.IL_SEAL:
				monsterDiseases.add(MonsterStatusEffect.SEAL);
				break;
			case Skills.SHADOW_WEB:
				monsterDiseases.add(MonsterStatusEffect.SHADOW_WEB);
				break;
			case Skills.HAMSTRING:
				effects.add(PlayerStatusEffect.HAMSTRING);
				monsterDiseases.add(MonsterStatusEffect.SPEED);
				break;
			case Skills.BLIND:
				effects.add(PlayerStatusEffect.BLIND);
				monsterDiseases.add(MonsterStatusEffect.ACC);
				break;
			case Skills.NL_NINJA_AMBUSH:
			case Skills.SHADOWER_NINJA_AMBUSH:
				monsterDiseases.add(MonsterStatusEffect.NINJA_AMBUSH);
				break;
			case Skills.HYPNOTIZE:
				monsterDiseases.add(MonsterStatusEffect.INERTMOB);
				break;
		}
	}

	protected void setY(int y) {
		this.y = y;
		switch (getDataId()) {
			case Skills.SPEARMAN_HYPER_BODY:
			case Skills.GM_HYPER_BODY:
				effects.add(PlayerStatusEffect.MAXMP);
				break;
			case Skills.BOW_MASTER_SHARP_EYES:
			case Skills.XBOW_MASTER_SHARP_EYES:
				effects.add(PlayerStatusEffect.SHARP_EYES);
				break;

			case Skills.DISORDER:
				monsterDiseases.add(MonsterStatusEffect.WDEF);
				break;
			case Skills.THREATEN:
				monsterDiseases.add(MonsterStatusEffect.WDEF);
				break;
		}
	}

	protected void setZ(int z) {
		this.z = z;
	}

	protected void setDamage(short damage) {
		this.damage = damage;
	}

	protected void setLt(short x, short y) {
		this.lt = new Point(x, y);
	}

	protected void setRb(short x, short y) {
		this.rb = new Point(x, y);
	}

	protected void setMobCount(byte count) {
		this.mobCount = count;
	}

	protected void setProp(double prop) {
		this.prop = prop;
	}

	protected void setMastery(byte mastery) {
		this.mastery = mastery;
	}

	protected void setCooltime(short duration) {
		this.cooltime = duration;
	}

	protected void setRange(short distance) {
		this.range = distance;
	}

	protected void setAttackCount(byte count) {
		this.attackCount = count;
	}

	protected void setBulletCount(byte count) {
		this.bulletCount = count;
	}

	protected void setItemConsume(int itemCon) {
		this.itemCon = itemCon;
	}

	protected void setItemConsumeCount(byte count) {
		this.itemConCount = count;
	}

	protected void setBulletConsume(short bulletCon) {
		this.bulletCon = bulletCon;
	}

	protected void setMoneyConsume(short moneyCon) {
		this.moneyCon = moneyCon;
	}

	public int getDuration() {
		if (isFreeze)
			return super.getDuration() * 2;
		return super.getDuration();
	}

	public short getMpConsume() {
		return mpCon;
	}

	public short getHpConsume() {
		return hpCon;
	}

	public int getX() {
		return x;
	}

	public int getY() {
		return y;
	}

	public int getZ() {
		return z;
	}

	public short getDamage() {
		return damage;
	}

	public Point getLt() {
		return lt;
	}

	public Point getRb() {
		return rb;
	}

	public byte getMobCount() {
		return mobCount;
	}

	public double getProp() {
		return prop;
	}

	public boolean shouldPerform() {
		return Rng.getGenerator().nextDouble() < prop;
	}

	public byte getMastery() {
		return mastery;
	}

	public short getCooltime() {
		return cooltime;
	}

	public short getRange() {
		return range;
	}

	public byte getAttackCount() {
		return attackCount;
	}

	public byte setBulletCount() {
		return bulletCount;
	}

	public int getItemConsume() {
		return itemCon;
	}

	public byte getItemConsumeCount() {
		return itemConCount;
	}

	public short getBulletConsume() {
		return bulletCon;
	}

	public short getMoneyConsume() {
		return moneyCon;
	}

	public int hashCode() {
		//set the high bit to differentiate between skill and item effects that
		//have the same data id.
		//e.g. for use in hash maps that combine any kind of StatEffectsData.
		return getDataId() | 0x80000000;
	}

	public Set<MonsterStatusEffect> getMonsterDiseasesToGive() {
		return monsterDiseases;
	}

	public byte getLevel() {
		return level;
	}
}
