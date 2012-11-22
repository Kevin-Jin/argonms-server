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

package argonms.game.loading.skill;

import argonms.common.character.PlayerStatusEffect;
import argonms.common.field.MonsterStatusEffect;
import argonms.common.loading.StatusEffectsData.MonsterStatusEffectsData;
import argonms.common.util.Rng;
import argonms.game.field.MobSkills;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 *
 * @author GoldenKevin
 */
public class MobSkillEffectsData implements MonsterStatusEffectsData {
	private final short skillid;
	private final byte level;
	private final Map<Byte, Integer> summons;
	private short mpCon;
	private int duration;
	private int x;
	private int y;
	private Point lt;
	private Point rb;
	private short prop;
	private short cooltime;
	private short maxHpPerc;
	private MonsterStatusEffect monsterBuff;
	private PlayerStatusEffect playerDisease;
	private short summonLimit;
	private byte summonEffect;
	private boolean aoe;

	protected MobSkillEffectsData(short skillid, byte level) {
		this.maxHpPerc = 100;
		this.summons = new TreeMap<Byte, Integer>();
		this.skillid = skillid;
		this.level = level;
		this.prop = 100;
		switch (skillid) {
			case MobSkills.WATK_UP_AOE:
				aoe = true;
			case MobSkills.WATK_UP:
				monsterBuff = MonsterStatusEffect.WATK;
				break;
			case MobSkills.MATK_UP_AOE:
				aoe = true;
			case MobSkills.MATK_UP:
				monsterBuff = MonsterStatusEffect.MATK;
				break;
			case MobSkills.WDEF_UP_AOE:
				aoe = true;
			case MobSkills.WDEF_UP:
				monsterBuff = MonsterStatusEffect.WDEF;
				break;
			case MobSkills.MDEF_UP_AOE:
				aoe = true;
			case MobSkills.MDEF_UP:
				monsterBuff = MonsterStatusEffect.MDEF;
				break;

			case MobSkills.SEAL:
				playerDisease = PlayerStatusEffect.SEAL;
				break;
			case MobSkills.DARKEN:
				playerDisease = PlayerStatusEffect.DARKNESS;
				break;
			case MobSkills.WEAKEN:
				playerDisease = PlayerStatusEffect.WEAKNESS;
				break;
			case MobSkills.STUN:
				playerDisease = PlayerStatusEffect.STUN;
				break;
			case MobSkills.CURSE:
				playerDisease = PlayerStatusEffect.CURSE;
				break;
			case MobSkills.POISON:
				playerDisease = PlayerStatusEffect.POISON;
				break;
			case MobSkills.SLOW:
				playerDisease = PlayerStatusEffect.SLOW;
				break;

			case MobSkills.PHYSICAL_IMMUNITY:
				monsterBuff = MonsterStatusEffect.WEAPON_IMMUNITY;
				break;
			case MobSkills.MAGIC_IMMUNITY:
				monsterBuff = MonsterStatusEffect.MAGIC_IMMUNITY;
				break;
		}
	}

	protected void setMpConsume(short mpCon) {
		this.mpCon = mpCon;
	}

	protected void setDuration(int time) {
		this.duration = time;
	}

	protected void setX(int x) {
		this.x = x;
	}

	protected void setY(int y) {
		this.y = y;
	}

	protected void setLt(short x, short y) {
		this.lt = new Point(x, y);
	}

	protected void setRb(short x, short y) {
		this.rb = new Point(x, y);
	}

	protected void setProp(short prop) {
		this.prop = prop;
	}

	protected void setCooltime(short duration) {
		this.cooltime = duration;
	}

	protected void setMaxHpPercent(short hp) {
		this.maxHpPerc = hp;
	}

	protected void setLimit(short limit) {
		this.summonLimit = limit;
	}

	protected void setSummonEffect(byte effect) {
		this.summonEffect = effect;
	}

	protected void addSummon(byte index, int mobid) {
		summons.put(Byte.valueOf(index), Integer.valueOf(mobid));
	}

	public short getMpConsume() {
		return mpCon;
	}

	@Override
	public int getDuration() {
		return duration;
	}

	@Override
	public int getX() {
		return x;
	}

	@Override
	public int getY() {
		return y;
	}

	@Override
	public Rectangle getBoundingBox(Point posFrom, boolean facingLeft) {
		int ltx, lty, rbx, rby;
		if (facingLeft) {
			ltx = lt.x + posFrom.x;
			rbx = rb.x + posFrom.x;
		} else {
			ltx = -rb.x + posFrom.x;
			rbx = -lt.x + posFrom.x;
		}
		lty = lt.y + posFrom.y;
		rby = rb.y + posFrom.y;
		Rectangle bounds = new Rectangle(ltx, lty, rbx - ltx, rby - lty);
		return bounds;
	}

	public short getProp() {
		return prop;
	}

	public boolean makeChanceResult() {
		return Rng.getGenerator().nextInt(100) < prop;
	}

	public short getCooltime() {
		return cooltime;
	}

	public short getMaxPercentHp() {
		return maxHpPerc;
	}

	public short getSummonLimit() {
		return summonLimit;
	}

	public byte getSummonEffect() {
		return summonEffect;
	}

	public Map<Byte, Integer> getSummons() {
		return summons;
	}

	public boolean isAoe() {
		return aoe;
	}

	@Override
	public int getDataId() {
		return skillid;
	}

	@Override
	public byte getLevel() {
		return level;
	}

	@Override
	public Set<MonsterStatusEffect> getMonsterEffects() {
		if (monsterBuff == null)
			return Collections.emptySet();
		else
			return Collections.singleton(monsterBuff);
	}

	@Override
	public EffectSource getSourceType() {
		return EffectSource.MOB_SKILL;
	}

	@Override
	public Set<PlayerStatusEffect> getEffects() {
		if (playerDisease != null)
			return EnumSet.of(playerDisease);
		return EnumSet.noneOf(PlayerStatusEffect.class);
	}
}
