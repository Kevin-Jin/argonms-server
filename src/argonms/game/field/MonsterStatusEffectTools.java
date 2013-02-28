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

package argonms.game.field;

import argonms.common.character.Skills;
import argonms.common.field.MonsterStatusEffect;
import argonms.common.loading.StatusEffectsData;
import argonms.common.loading.StatusEffectsData.MonsterStatusEffectsData;
import argonms.common.util.Rng;
import argonms.common.util.Scheduler;
import argonms.game.character.DiseaseTools;
import argonms.game.character.GameCharacter;
import argonms.game.field.entity.Mist;
import argonms.game.field.entity.Mob;
import argonms.game.loading.mob.MobDataLoader;
import argonms.game.loading.skill.MobSkillEffectsData;
import argonms.game.loading.skill.PlayerSkillEffectsData;
import argonms.game.loading.skill.SkillDataLoader;
import argonms.game.net.external.GamePackets;
import java.awt.Point;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicReference;

/**
 *
 * @author GoldenKevin
 */
public final class MonsterStatusEffectTools {
	private static byte[] getCastEffect(Mob m, MonsterStatusEffectsData e, Map<MonsterStatusEffect, Short> updatedStats) {
		switch (e.getSourceType()) {
			case MOB_SKILL:
				return GamePackets.writeMonsterBuff(m, updatedStats, (short) e.getDataId(), e.getLevel(), (short) 900);
			case PLAYER_SKILL:
				return GamePackets.writeMonsterDebuff(m, updatedStats, e.getDataId(), (short) 900);
		}
		return null;
	}

	private static byte[] getDispelEffect(Mob m, MonsterStatusEffectsData e) {
		if (e.getDataId() == Skills.POISON_MIST || e.getDataId() == Skills.VENOMOUS_STAR || e.getDataId() == Skills.VENOMOUS_STAB)
			return GamePackets.writeMonsterCancelStatusEffect(m, Collections.singleton(MonsterStatusEffect.POISON));
		return GamePackets.writeMonsterCancelStatusEffect(m, e.getMonsterEffects());
	}

	private static int applyEffects(Map<MonsterStatusEffect, Short> updatedStats, Mob m, GameCharacter p, MonsterStatusEffectsData e) {
		Element elem = null;
		if (e.getSourceType() == StatusEffectsData.EffectSource.PLAYER_SKILL) {
			elem = SkillDataLoader.getInstance().getSkill(e.getDataId()).getElement();
			switch (e.getDataId()) {
				case Skills.SWORD_ICE_CHARGE:
				case Skills.BW_BLIZZARD_CHARGE:
				case Skills.IL_ELEMENT_COMPOSITION:
					elem = Element.ICE;
					break;
				case Skills.VENOMOUS_STAR:
				case Skills.VENOMOUS_STAB:
				case Skills.FP_ELEMENT_COMPOSITION:
					elem = Element.POISON;
					break;
			}
		}
		if (!e.makeChanceResult() || elem != null && (m.getElementalResistance(elem) > Element.EFFECTIVENESS_NORMAL
				|| elem == Element.POISON && m.getHp() <= 1))
			return -1;
		for (MonsterStatusEffect buff : e.getMonsterEffects()) {
			MonsterStatusEffectValues v = m.getEffectValue(buff);
			if (v != null) {
				if (elem == Element.POISON) {
					if (e.getDataId() == Skills.VENOMOUS_STAR || e.getDataId() == Skills.VENOMOUS_STAB) {
						if (!m.canAcceptVenom(p) || m.getVenomCount() >= 3) {
							for (MonsterStatusEffect alreadyBuffed : updatedStats.keySet())
								m.removeFromActiveEffects(alreadyBuffed);
							return -1;
						}
					} else {
						for (MonsterStatusEffect alreadyBuffed : updatedStats.keySet())
							m.removeFromActiveEffects(alreadyBuffed);
						return -1;
					}
				} else {
					dispelEffect(m, buff, v);
				}
			}
			v = applyEffect(m, p, e, buff);
			updatedStats.put(buff, Short.valueOf(v.getModifier()));
			m.addToActiveEffects(buff, v);
		}
		int duration;
		switch (e.getSourceType()) {
			case MOB_SKILL: {
				//check if it is a mob attack skill
				MobSkillEffectsData skill = ((MobSkillEffectsData) e);
				switch (e.getDataId()) {
					case MobSkills.MIST:
						Mist mist = new Mist(m, skill);
						m.getMap().spawnMist(mist, skill.getX() * 10, null);
						duration = e.getDuration();
						break;
					case MobSkills.SUMMON:
						short limit = skill.getSummonLimit();
						if (limit == 5000)
							limit = (short) (30 + m.getMap().getPlayerCount() * 2);
						if (m.getSpawnedSummons() < limit) {
							Random generator = Rng.getGenerator();
							for (Integer oMobId : skill.getSummons().values()) {
								int mobId = oMobId.intValue();
								Mob summon = new Mob(MobDataLoader.getInstance().getMobStats(mobId), m.getMap());
								int ypos, xpos;
								xpos = m.getPosition().x;
								ypos = m.getPosition().y;
								switch (mobId) {
									case 8500003: // Pap bomb high
										summon.setFoothold((short)Math.ceil(generator.nextDouble() * 19.0));
										ypos = -590; //no break?
									case 8500004: // Pap bomb
										//Spawn between -500 and 500 from the monsters X position
										xpos = (int)(m.getPosition().x + Math.ceil(generator.nextDouble() * 1000.0) - 500);
										if (ypos != -590)
											ypos = m.getPosition().y;
										break;
									case 8510100: //Pianus bomb
										if (Math.ceil(generator.nextDouble() * 5) == 1) {
											ypos = 78;
											xpos = (int)(0 + Math.ceil(generator.nextDouble() * 5)) + ((Math.ceil(generator.nextDouble() * 2) == 1) ? 180 : 0);
										} else
											xpos = (int)(m.getPosition().x + Math.ceil(generator.nextDouble() * 1000.0) - 500);
										break;
								}
								// Get spawn coordinates (This fixes monster lock)
								// TODO get map left and right wall. Any suggestions?
								switch (m.getMap().getDataId()) {
									case 220080001: //Pap map
										if (xpos < -890)
											xpos = (int)(-890 + Math.ceil(generator.nextDouble() * 150));
										else if (xpos > 230)
											xpos = (int)(230 - Math.ceil(generator.nextDouble() * 150));
										break;
									case 230040420: // Pianus map
										if (xpos < -239)
											xpos = (int)(-239 + Math.ceil(generator.nextDouble() * 150));
										else if (xpos > 371)
											xpos = (int)(371 - Math.ceil(generator.nextDouble() * 150));
										break;
								}
								summon.setPosition(new Point(xpos, ypos));
								summon.setSpawnEffect(skill.getSummonEffect());
								m.getMap().spawnMonster(summon);
								m.addToSpawnedSummons();
							}
						}
						duration = 0;
						break;
					default:
						if (!e.getEffects().isEmpty())
							DiseaseTools.applyDebuff(p, (short) skill.getDataId(), skill.getLevel());
						duration = e.getDuration();
						break;
				}
				break;
			}
			case PLAYER_SKILL: {
				switch (e.getDataId()) {
					case Skills.BLIND:
					case Skills.HAMSTRING:
						duration = e.getY();
						break;
					case Skills.SWORD_ICE_CHARGE:
					case Skills.BW_BLIZZARD_CHARGE:
						//freeze skills have weird times...
						duration = e.getY() * 2;
						break;
					case Skills.VENOMOUS_STAR:
					case Skills.VENOMOUS_STAB:
					case Skills.POISON_MIST: {
						MonsterStatusEffectValues v = m.getEffectValue(MonsterStatusEffect.POISON);
						if (v != null) {
							if (elem == Element.POISON) {
								if (e.getDataId() == Skills.VENOMOUS_STAR || e.getDataId() == Skills.VENOMOUS_STAB) {
									if (!m.canAcceptVenom(p) || m.getVenomCount() >= 3) {
										for (MonsterStatusEffect alreadyBuffed : updatedStats.keySet())
											m.removeFromActiveEffects(alreadyBuffed);
										return -1;
									}
								} else {
									for (MonsterStatusEffect alreadyBuffed : updatedStats.keySet())
										m.removeFromActiveEffects(alreadyBuffed);
									return -1;
								}
							} else {
								dispelEffect(m, MonsterStatusEffect.POISON, v);
							}
						}
						v = applyEffect(m, p, e, MonsterStatusEffect.POISON);
						updatedStats.put(MonsterStatusEffect.POISON, Short.valueOf(v.getModifier()));
						m.addToActiveEffects(MonsterStatusEffect.POISON, v);
						duration = e.getDuration();
						if (e.getDataId() == Skills.VENOMOUS_STAR || e.getDataId() == Skills.VENOMOUS_STAB) {
							//TODO: not thread-safe (since (m.getVenomCount() >= 3) check was done long ago)
							m.setVenomOwner(p);
							m.incrementVenom(duration);
						}
						break;
					}
					default:
						duration = e.getDuration();
						break;
				}
				break;
			}
			default:
				duration = 0;
				break;
		}
		if (m.areEffectsActive(e))
			m.removeCancelEffectTask(e);
		return duration;
	}

	private static boolean applyEffectsAndShowVisualsInternal(final Mob m, final GameCharacter p, final MonsterStatusEffectsData e) {
		Map<MonsterStatusEffect, Short> updatedStats = new EnumMap<MonsterStatusEffect, Short>(MonsterStatusEffect.class);
		int duration = applyEffects(updatedStats, m, p, e);
		if (duration == -1)
			return false;
		byte[] effect = getCastEffect(m, e, updatedStats);
		if (m.isVisible() && effect != null)
			m.getMap().sendToAll(effect);
		m.addCancelEffectTask(e, Scheduler.getInstance().runAfterDelay(new Runnable() {
			@Override
			public void run() {
				dispelEffectsAndShowVisuals(m, e);
			}
		}, duration));
		return true;
	}

	public static boolean applyEffectsAndShowVisuals(final Mob m, final GameCharacter p, final MonsterStatusEffectsData e) {
		if (e.getSourceType() == StatusEffectsData.EffectSource.MOB_SKILL && ((MobSkillEffectsData) e).isAoe()) {
			for (MapEntity neighbor : m.getMap().getMapEntitiesInRect(e.getBoundingBox(m.getPosition(), m.getStance() % 2 != 0), EnumSet.of(MapEntity.EntityType.MONSTER)))
				if (!applyEffectsAndShowVisualsInternal((Mob) neighbor, p, e))
					return false;
			return true;
		} else {
			return applyEffectsAndShowVisualsInternal(m, p, e);
		}
	}

	public static void dispelEffectsAndShowVisuals(Mob m, MonsterStatusEffectsData e) {
		m.removeCancelEffectTask(e);
		switch (e.getSourceType()) {
			case MOB_SKILL: {
				for (MonsterStatusEffect buff : e.getMonsterEffects()) {
					MonsterStatusEffectValues v = m.removeFromActiveEffects(buff);
					if (v != null)
						dispelEffect(m, buff, v);
				}
				break;
			}
			case PLAYER_SKILL: {
				for (MonsterStatusEffect buff : e.getMonsterEffects()) {
					MonsterStatusEffectValues v = m.removeFromActiveEffects(buff);
					if (v != null)
						dispelEffect(m, buff, v);
				}
				if (e.getDataId() == Skills.POISON_MIST || e.getDataId() == Skills.VENOMOUS_STAR || e.getDataId() == Skills.VENOMOUS_STAB) {
					MonsterStatusEffectValues v = m.removeFromActiveEffects(MonsterStatusEffect.POISON);
					if (v != null)
						dispelEffect(m, MonsterStatusEffect.POISON, v);
				}
				break;
			}
		}
		byte[] effect = getDispelEffect(m, e);
		if (m.isVisible() && effect != null)
			m.getMap().sendToAll(effect);
	}

	private static void schedulePoisonDamage(final Mob m, final GameCharacter p, final MonsterStatusEffectsData e, final short damage) {
		m.setPoisonTask(Scheduler.getInstance().runRepeatedly(new Runnable() {
			@Override
			public void run() {
				if (m.getHp() > damage)
					//TODO: not thread-safe
					m.hurt(p, damage);
				else
					dispelEffectsAndShowVisuals(m, e);
			}
		}, 0, 1000));
	}

	private static MonsterStatusEffectValues applyEffect(final Mob m, final GameCharacter p, final MonsterStatusEffectsData e, MonsterStatusEffect buff) {
		short mod = 0;
		switch (buff) {
			case WATK:
				mod = (short) (e.getX() - 100);
				break;
			case WDEF:
				mod = (short) e.getX();
				break;
			case MATK:
				mod = (short) e.getX();
				break;
			case MDEF:
				mod = (short) e.getX();
				break;
			case ACC:
				mod = (short) e.getX();
				break;
			case AVOID:
				mod = (short) e.getX();
				break;
			case SPEED:
				mod = (short) e.getX();
				break;
			case STUN:
				mod = 1;
				break;
			case FREEZE:
				mod = 1;
				break;
			case POISON: {
				final short damage = mod = (short) Math.min((double) m.getMaxHp() / (70 - e.getLevel()), Short.MAX_VALUE);
				if (e.getDataId() == Skills.VENOMOUS_STAR || e.getDataId() == Skills.VENOMOUS_STAB) {
					MonsterStatusEffectValues v = m.getEffectValue(MonsterStatusEffect.POISON);
					if (v != null) {
						assert (v.getSource() == Skills.VENOMOUS_STAR || v.getSource() == Skills.VENOMOUS_STAB);
						mod += v.getModifier();
						final AtomicReference<Runnable> decrementVenomTask = new AtomicReference<Runnable>();
						decrementVenomTask.set(new Runnable() {
							@Override
							public void run() {
								MonsterStatusEffectValues value = m.getEffectValue(MonsterStatusEffect.POISON);
								if (value == null)
									return;

								ScheduledFuture<?> f = m.removePoisonTask();
								if (f != null)
									f.cancel(false);
								m.decrementVenom();
								short newMod = value.getModifier();
								newMod -= damage;
								if (newMod <= 0)
									return;

								schedulePoisonDamage(m, p, value.getEffectsData(), newMod);
								m.addToActiveEffects(MonsterStatusEffect.POISON, new MonsterStatusEffectValues(value.getEffectsData(), newMod));
								byte[] effect = getDispelEffect(m, e);
								if (m.isVisible() && effect != null)
									m.getMap().sendToAll(effect);
								effect = getCastEffect(m, e, Collections.singletonMap(MonsterStatusEffect.POISON, Short.valueOf(newMod)));
								if (m.isVisible() && effect != null)
									m.getMap().sendToAll(effect);

								m.setVenomDecrementTask(Scheduler.getInstance().runAfterDelay(decrementVenomTask.get(), m.nextVenomExpire()));
							}
						});
						m.setVenomDecrementTask(Scheduler.getInstance().runAfterDelay(decrementVenomTask.get(), m.nextVenomExpire()));
					}
				}

				schedulePoisonDamage(m, p, e, mod);
				break;
			}
			case SEAL:
				mod = 1;
				break;
			case TAUNT_1:
				mod = (short) (100 - e.getX());
				break;
			case TAUNT_2:
				mod = (short) (100 - e.getX());
				break;
			case DOOM:
				mod = 1;
				break;
			case SHADOW_WEB:
				mod = 1;
				break;
			case WEAPON_IMMUNITY:
				mod = (short) e.getX();
				break;
			case MAGIC_IMMUNITY:
				mod = (short) e.getX();
				break;
			case NINJA_AMBUSH:
				mod = 1;
				break;
			case INERTMOB:
				mod = 1;
				break;
		}
		return new MonsterStatusEffectValues(e, mod);
	}

	public static void dispelEffect(Mob m, MonsterStatusEffect key, MonsterStatusEffectValues value) {
		switch (key) {
			case WATK:
				break;
			case WDEF:
				break;
			case MATK:
				break;
			case MDEF:
				break;
			case ACC:
				break;
			case AVOID:
				break;
			case SPEED:
				break;
			case STUN:
				break;
			case FREEZE:
				break;
			case POISON: {
				ScheduledFuture<?> f = m.removePoisonTask();
				if (f != null)
					f.cancel(false);
				if (value.getSource() == Skills.VENOMOUS_STAR || value.getSource() == Skills.VENOMOUS_STAB) {
					m.resetVenom();
					m.removeVenomOwner();
					f = m.removeVenomDecrementTask();
					if (f != null)
						f.cancel(false);
				}
				break;
			}
			case SEAL:
				break;
			case TAUNT_1:
				break;
			case TAUNT_2:
				break;
			case DOOM:
				break;
			case SHADOW_WEB:
				break;
			case WEAPON_IMMUNITY:
				break;
			case MAGIC_IMMUNITY:
				break;
			case NINJA_AMBUSH:
				break;
			case INERTMOB:
				break;
		}
	}

	public static void applyDispel(Mob m, PlayerSkillEffectsData e) {
		//"Dispel can override: W.Attack+, M.Att+, W.Defence+, M.Defence+, Accuracy+, Avoidability+, Speed+ and Super Avoidability
		//Dispel can NOT override: Damage Reflection, Cancel W.Attack and Cancel M.Attack"
		for (MonsterStatusEffect buff : new MonsterStatusEffect[] { MonsterStatusEffect.WATK, MonsterStatusEffect.WDEF, MonsterStatusEffect.MATK, MonsterStatusEffect.MDEF, MonsterStatusEffect.ACC, MonsterStatusEffect.AVOID, MonsterStatusEffect.SPEED }) {
			MonsterStatusEffectValues v = m.getEffectValue(buff);
			if (v != null)
				MonsterStatusEffectTools.dispelEffectsAndShowVisuals(m, v.getEffectsData());
		}
	}

	private MonsterStatusEffectTools() {
		//uninstantiable...
	}
}
