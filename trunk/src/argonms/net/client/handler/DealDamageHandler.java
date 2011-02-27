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

package argonms.net.client.handler;

import argonms.character.Player;
import argonms.loading.skill.SkillDataLoader;
import argonms.loading.skill.SkillEffect;
import argonms.loading.skill.SkillStats;
import argonms.map.GameMap;
import argonms.map.MapEntity;
import argonms.map.MapEntity.MapEntityType;
import argonms.map.entity.ItemDrop;
import argonms.map.entity.Mob;
import argonms.net.client.RemoteClient;
import argonms.tools.Pair;
import argonms.tools.Timer;
import argonms.tools.input.LittleEndianReader;
import java.awt.Point;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

//TODO: implement!
/**
 *
 * @author GoldenKevin
 */
public class DealDamageHandler {
	public static void handleMeleeAttack(LittleEndianReader packet, RemoteClient rc) {
		
	}

	//bow/arrows, claw/stars, guns/bullets (projectiles)
	public static void handleRangedAttack(LittleEndianReader packet, RemoteClient rc) {
		
	}

	public static void handleMagicAttack(LittleEndianReader packet, RemoteClient rc) {
		
	}

	private static AttackInfo parseMesoExplosion(LittleEndianReader packet, AttackInfo ret) {
		if (ret.numAttackedAndDamage == 0) {
			packet.skip(10);
			int bullets = packet.readByte();
			for (int j = 0; j < bullets; j++) {
				int mesoid = packet.readInt();
				packet.skip(1);
				ret.allDamage.add(new Pair<Integer, List<Integer>>(Integer.valueOf(mesoid), null));
			}
			return ret;
		} else {
			packet.skip(6);
		}

		for (int i = 0; i < ret.numAttacked + 1; i++) {
			int eid = packet.readInt();
			if (i < ret.numAttacked) {
				packet.skip(12);
				int bullets = packet.readByte();
				List<Integer> allDamageNumbers = new ArrayList<Integer>();
				for (int j = 0; j < bullets; j++) {
					int damage = packet.readInt();
					allDamageNumbers.add(Integer.valueOf(damage));
				}
				ret.allDamage.add(new Pair<Integer, List<Integer>>(Integer.valueOf(eid), allDamageNumbers));
				packet.skip(4);
			} else {
				int bullets = packet.readByte();
				for (int j = 0; j < bullets; j++) {
					int mesoid = packet.readInt();
					packet.skip(1);
					ret.allDamage.add(new Pair<Integer, List<Integer>>(Integer.valueOf(mesoid), null));
				}
			}
		}
		return ret;
	}

	private static AttackInfo parseDamage(LittleEndianReader packet, boolean ranged) {
		AttackInfo ret = new AttackInfo();

		packet.readByte();
		ret.numAttackedAndDamage = packet.readByte();
		ret.numAttacked = (ret.numAttackedAndDamage >>> 4) & 0xF; // guess why there are no skills damaging more than 15 monsters...
		ret.numDamage = ret.numAttackedAndDamage & 0xF; // how often each single monster was attacked o.o
		ret.allDamage = new ArrayList<Pair<Integer, List<Integer>>>();
		ret.skill = packet.readInt();
		SkillStats skillStats = SkillDataLoader.getInstance().getSkill(ret.skill);
		ret.charge = skillStats != null && skillStats.isChargedSkill() ? packet.readInt() : 0;

		if (ret.skill == 1221011)
			ret.isHH = true;
		/*ret.display = */packet.readByte();
		ret.stance = packet.readByte();

		if (ret.skill == 4211006)
			return parseMesoExplosion(packet, ret);

		if (ranged) {
			packet.readByte();
			ret.speed = packet.readByte();
			packet.readByte();
			ret.direction = packet.readByte(); // contains direction on some 4th job skills
			packet.skip(7);
			// hurricane and pierce have extra 4 bytes :/
			switch (ret.skill) {
				case 3121004:
				case 3221001:
				case 5221004:
					packet.skip(4);
					break;
				default:
					break;
			}
		} else {
			packet.readByte();
			ret.speed = packet.readByte();
			packet.skip(4);
		}

		for (int i = 0; i < ret.numAttacked; i++) {
			int eid = packet.readInt();
			// System.out.println("Unk2: " + HexTool.toString(lea.read(14)));
			packet.skip(14); // seems to contain some position info o.o

			List<Integer> allDamageNumbers = new ArrayList<Integer>();
			for (int j = 0; j < ret.numDamage; j++) {
				int damage = packet.readInt();
				if (ret.skill == 3221007)
					damage += 0x80000000; // Critical damage = 0x80000000 + damage
				allDamageNumbers.add(Integer.valueOf(damage));
			}
			if (ret.skill != 5221004)
				packet.skip(4);
			ret.allDamage.add(new Pair<Integer, List<Integer>>(Integer.valueOf(eid), allDamageNumbers));
		}

		// System.out.println("Unk3: " + HexTool.toString(lea.read(4)));
		return ret;
	}

	/*private void handlePickPocket(Player p, Mob monster, Pair<Integer, List<Integer>> oned) {
		int delay = 0;
		int maxmeso = p.getBuffedValue(MapleBuffStat.PICKPOCKET).intValue();
		int reqdamage = 20000;
		Point monsterPosition = monster.getPosition();

		for (Integer eachd : oned.getRight()) {
			if (SkillDataLoader.getInstance().getSkill(4211003).getLevel(p.getSkillLevel(4211003)).makeChanceResult()) {
				double perc = (double) eachd / (double) reqdamage;

				final int todrop = Math.min((int) Math.max(perc * (double) maxmeso, (double) 1), maxmeso);
				final MapleMap tdmap = player.getMap();
				final Point tdpos = new Point((int) (monsterPosition.getX() + (Math.random() * 100) - 50), (int) (monsterPosition.getY()));
				final MapleMonster tdmob = monster;
				final MapleCharacter tdchar = player;

				Timer.getInstance().runAfterDelay(new Runnable() {
					public void run() {
						tdmap.spawnMesoDrop(todrop, todrop, tdpos, tdmob, tdchar, false);
					}
				}, delay);

				delay += 100;
			}
		}
	}

	protected synchronized void applyAttack(AttackInfo attack, Player player, int maxDamagePerMonster, int attackCount) {
		SkillEffect attackEffect = null;
		if (attack.skill != 0) {
			attackEffect = attack.getAttackEffect(player);
			if (attackEffect == null) {
				player.getClient().getSession().write(MaplePacketCreator.enableActions());
				return;
			}
			if (attack.skill != 2301002) {
				// heal is both an attack and a special move (healing)
				// so we'll let the whole applying magic live in the special move part
				if (player.isAlive()) {
					attackEffect.applyTo(player);
				} else {
					player.getClient().getSession().write(MaplePacketCreator.enableActions());
				}
			}
		}
		int totDamage = 0;
		final GameMap map = player.getMap();
		if (attack.skill == 4211006) { // meso explosion
			int delay = 0;
			for (Pair<Integer, List<Integer>> oned : attack.allDamage) {
				MapEntity ent = map.getEntityById(oned.getLeft().intValue());
				if (ent != null && ent.getEntityType() == MapEntityType.ITEM) {
					final ItemDrop mapitem = (ItemDrop) ent;
					if (mapitem.getMesoValue() >= 10) {
						synchronized (mapitem) {
							if (!mapitem.isAlive())
								return;
							Timer.getInstance().runAfterDelay(new Runnable() {

								public void run() {
									map.removeMapObject(mapitem);
									map.broadcastMessage(MaplePacketCreator.removeItemFromMap(mapitem.getObjectId(), 4, 0), mapitem.getPosition());
									mapitem.setPickedUp(true);
								}
							}, delay);
							delay += 100;
						}
					} else if (mapitem.getMeso() == 0) {
						player.getCheatTracker().registerOffense(CheatingOffense.ETC_EXPLOSION);
						return;
					}
				} else if (mapobject != null && mapobject.getType() != MapleMapObjectType.MONSTER) {
					player.getCheatTracker().registerOffense(CheatingOffense.EXPLODING_NONEXISTANT);
					return; // etc explosion, exploding nonexistant things, etc.
				}
			}
		}

		for (Pair<Integer, List<Integer>> oned : attack.allDamage) {
			MapleMonster monster = map.getMonsterByOid(oned.getLeft().intValue());

			if (monster != null) {
				int totDamageToOneMonster = 0;
				for (Integer eachd : oned.getRight()) {
					totDamageToOneMonster += eachd.intValue();
				}
				totDamage += totDamageToOneMonster;

				player.checkMonsterAggro(monster);

				// anti-hack
				if (totDamageToOneMonster > attack.numDamage + 1) {
					int dmgCheck = player.getCheatTracker().checkDamage(totDamageToOneMonster);
					if (dmgCheck > 5 && totDamageToOneMonster < 99999 && monster.getId() < 9500317 && monster.getId() > 9500319) {
					}
				}

				double distance = player.getPosition().distanceSq(monster.getPosition());
				if (distance > 400000.0) { // 600^2, 550 is approximatly the range of ultis
					player.getCheatTracker().registerOffense(CheatingOffense.ATTACK_FARAWAY_MONSTER, Double.toString(Math.sqrt(distance)));
				}

				if (attack.skill == 2301002 && !monster.getUndead()) {
					player.getCheatTracker().registerOffense(CheatingOffense.HEAL_ATTACKING_UNDEAD);
					return;
				}

				if (attack.skill == 5111004) { // Energy Drain
					ISkill edrain = SkillFactory.getSkill(5111004);
					int gainhp;
					gainhp = (int) ((double) totDamage * (double) edrain.getEffect(player.getSkillLevel(edrain)).getX() / 100.0);
					gainhp = Math.min(monster.getMaxHp(), Math.min(gainhp, player.getMaxHp() / 2));
					player.addHP(gainhp);
				}

				// pickpocket
				if (player.getBuffedValue(MapleBuffStat.PICKPOCKET) != null) {
					switch (attack.skill) {
						case 0:
						case 4001334:
						case 4201005:
						case 4211002:
						case 4211004:
						case 4211001:
						case 4221003:
						case 4221007:
							handlePickPocket(player, monster, oned);
							break;
					}
				}

				// effects
				switch (attack.skill) {
					case 1221011: //sanctuary
						if (attack.isHH) {
							// TODO min damage still needs calculated.. using -20% as mindamage in the meantime.. seems to work
							int HHDmg = (int) (player.calculateMaxBaseDamage(player.getTotalWatk()) * (theSkill.getEffect(player.getSkillLevel(theSkill)).getDamage() / 100));
							HHDmg = (int) (Math.floor(Math.random() * (HHDmg - HHDmg * .80) + HHDmg * .80));
							map.damageMonster(player, monster, HHDmg);
						}
						break;
					case 3221007: //snipe
						totDamageToOneMonster = 95000 + rand.nextInt(5000);
						break;
					case 4101005: //drain
						int gainhp = (int) ((double) totDamageToOneMonster * (double) SkillFactory.getSkill(4101005).getEffect(player.getSkillLevel(SkillFactory.getSkill(4101005))).getX() / 100.0);
						gainhp = Math.min(monster.getMaxHp(), Math.min(gainhp, player.getMaxHp() / 2));
						player.addHP(gainhp);
						break;
					default:
						//passives attack bonuses
						if (totDamageToOneMonster > 0 && monster.isAlive()) {
							if (player.getBuffedValue(MapleBuffStat.BLIND) != null) {
								if (SkillFactory.getSkill(3221006).getEffect(player.getSkillLevel(SkillFactory.getSkill(3221006))).makeChanceResult()) {
									MonsterStatusEffect monsterStatusEffect = new MonsterStatusEffect(Collections.singletonMap(MonsterStatus.ACC, SkillFactory.getSkill(3221006).getEffect(player.getSkillLevel(SkillFactory.getSkill(3221006))).getX()), SkillFactory.getSkill(3221006), false);
									monster.applyStatus(player, monsterStatusEffect, false, SkillFactory.getSkill(3221006).getEffect(player.getSkillLevel(SkillFactory.getSkill(3221006))).getY() * 1000);

								}
							}
							if (player.getBuffedValue(MapleBuffStat.HAMSTRING) != null) {
								if (SkillFactory.getSkill(3121007).getEffect(player.getSkillLevel(SkillFactory.getSkill(3121007))).makeChanceResult()) {
									MonsterStatusEffect monsterStatusEffect = new MonsterStatusEffect(Collections.singletonMap(MonsterStatus.SPEED, SkillFactory.getSkill(3121007).getEffect(player.getSkillLevel(SkillFactory.getSkill(3121007))).getX()), SkillFactory.getSkill(3121007), false);
									monster.applyStatus(player, monsterStatusEffect, false, SkillFactory.getSkill(3121007).getEffect(player.getSkillLevel(SkillFactory.getSkill(3121007))).getY() * 1000);
								}
							}
							if (player.getJob().isA(MapleJob.WHITEKNIGHT)) {
								int[] charges = {1211005, 1211006};
								for (int charge : charges) {
									if (player.isBuffFrom(MapleBuffStat.WK_CHARGE, SkillFactory.getSkill(charge))) {
										final ElementalEffectiveness iceEffectiveness = monster.getEffectiveness(Element.ICE);
										if (iceEffectiveness == ElementalEffectiveness.NORMAL || iceEffectiveness == ElementalEffectiveness.WEAK) {
											MonsterStatusEffect monsterStatusEffect = new MonsterStatusEffect(Collections.singletonMap(MonsterStatus.FREEZE, 1), SkillFactory.getSkill(charge), false);
											monster.applyStatus(player, monsterStatusEffect, false, SkillFactory.getSkill(charge).getEffect(player.getSkillLevel(SkillFactory.getSkill(charge))).getY() * 2000);
										}
										break;
									}
								}
							}
						}
						break;
				}

				//venom
				if (player.getSkillLevel(SkillFactory.getSkill(4120005)) > 0) {
					MapleStatEffect venomEffect = SkillFactory.getSkill(4120005).getEffect(player.getSkillLevel(SkillFactory.getSkill(4120005)));
					for (int i = 0; i < attackCount; i++) {
						if (venomEffect.makeChanceResult()) {
							if (monster.getVenomMulti() < 3) {
								monster.setVenomMulti((monster.getVenomMulti() + 1));
								MonsterStatusEffect monsterStatusEffect = new MonsterStatusEffect(Collections.singletonMap(MonsterStatus.POISON, 1), SkillFactory.getSkill(4120005), false);
								monster.applyStatus(player, monsterStatusEffect, false, venomEffect.getDuration(), true);
							}
						}
					}
				} else if (player.getSkillLevel(SkillFactory.getSkill(4220005)) > 0) {
					MapleStatEffect venomEffect = SkillFactory.getSkill(4220005).getEffect(player.getSkillLevel(SkillFactory.getSkill(4220005)));
					for (int i = 0; i < attackCount; i++) {
						if (venomEffect.makeChanceResult()) {
							if (monster.getVenomMulti() < 3) {
								monster.setVenomMulti((monster.getVenomMulti() + 1));
								MonsterStatusEffect monsterStatusEffect = new MonsterStatusEffect(Collections.singletonMap(MonsterStatus.POISON, 1), SkillFactory.getSkill(4220005), false);
								monster.applyStatus(player, monsterStatusEffect, false, venomEffect.getDuration(), true);
							}
						}
					}
				}
				if (totDamageToOneMonster > 0 && attackEffect != null && attackEffect.getMonsterStati().size() > 0) {
					if (attackEffect.makeChanceResult()) {
						MonsterStatusEffect monsterStatusEffect = new MonsterStatusEffect(attackEffect.getMonsterStati(), theSkill, false);
						monster.applyStatus(player, monsterStatusEffect, attackEffect.isPoison(), attackEffect.getDuration());
					}
				}

				//apply attack
				if (!attack.isHH) {
					map.damageMonster(player, monster, totDamageToOneMonster);
				}
			}
		}
		if (totDamage > 1) {
			player.getCheatTracker().setAttacksWithoutHit(player.getCheatTracker().getAttacksWithoutHit() + 1);
			final int offenseLimit;
			switch (attack.skill) {
				case 3121004:
				case 5221004:
					offenseLimit = 100;
					break;
				default:
					offenseLimit = 500;
					break;
			}
			if (player.getCheatTracker().getAttacksWithoutHit() > offenseLimit) {
				player.getCheatTracker().registerOffense(CheatingOffense.ATTACK_WITHOUT_GETTING_HIT,
					Integer.toString(player.getCheatTracker().getAttacksWithoutHit()));
			}
		}
	}*/

	private static class AttackInfo {

		public int numAttacked, numDamage, numAttackedAndDamage;
		public int skill, charge;
		public byte stance, direction;
		public List<Pair<Integer, List<Integer>>> allDamage;
		public boolean isHH = false;
		public byte speed = 4;

		private SkillEffect getAttackEffect(Player p) {
			SkillStats skillStats = SkillDataLoader.getInstance().getSkill(skill);
			byte skillLevel = p.getSkillLevel(skill);
			if (skillLevel == 0)
				return null;
			return skillStats.getLevel(skillLevel);
		}
	}
}
