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

package argonms.game.handler;

import argonms.character.Player;
import argonms.character.skill.PlayerStatusEffectValues.PlayerStatusEffect;
import argonms.character.skill.Skills;
import argonms.game.GameClient;
import argonms.loading.skill.SkillDataLoader;
import argonms.loading.skill.PlayerSkillEffectsData;
import argonms.loading.skill.SkillStats;
import argonms.map.GameMap;
import argonms.map.MapEntity;
import argonms.map.MapEntity.EntityType;
import argonms.map.entity.ItemDrop;
import argonms.map.entity.Mob;
import argonms.net.external.ClientSendOps;
import argonms.net.external.CommonPackets;
import argonms.net.external.RemoteClient;
import argonms.tools.Rng;
import argonms.tools.Timer;
import argonms.tools.collections.Pair;
import argonms.tools.input.LittleEndianReader;
import argonms.tools.output.LittleEndianByteArrayWriter;
import argonms.tools.output.LittleEndianWriter;
import java.awt.Point;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 *
 * @author GoldenKevin
 */
public class DealDamageHandler {
	public static void handleMeleeAttack(LittleEndianReader packet, RemoteClient rc) {
		AttackInfo attack = parseDamage(packet, false);
		Player p = ((GameClient) rc).getPlayer();
		p.getMap().sendToAll(writeMeleeAttack(p.getId(), attack), p.getPosition(), p);
		PlayerSkillEffectsData e = attack.getAttackEffect(p);
		int attackCount = 1;
		if (attack.skill != 0) {
			attackCount = e.getAttackCount();
			if (e.getCooltime() > 0) {
				rc.getSession().send(CommonPackets.writeCooldown(attack.skill, e.getCooltime()));
				p.addCooldown(attack.skill, e.getCooltime());
			}
		}
		applyAttack(attack, p, attackCount);
	}

	//bow/arrows, claw/stars, guns/bullets (projectiles)
	public static void handleRangedAttack(LittleEndianReader packet, RemoteClient rc) {
		AttackInfo attack = parseDamage(packet, true);
		Player p = ((GameClient) rc).getPlayer();
		int visProj = 0;
		p.getMap().sendToAll(writeRangedAttack(p.getId(), attack, visProj), p.getPosition(), p);
	}

	public static void handleMagicAttack(LittleEndianReader packet, RemoteClient rc) {
		AttackInfo attack = parseDamage(packet, false);
		Player p = ((GameClient) rc).getPlayer();
		p.getMap().sendToAll(writeMagicAttack(p.getId(), attack), p.getPosition(), p);
	}

	private static AttackInfo parseMesoExplosion(LittleEndianReader packet, AttackInfo ret) {
		if (ret.numAttacked == 0 && ret.numDamage == 0) {
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
		ret.setNumAttackedAndDamage(packet.readByte());
		ret.allDamage = new ArrayList<Pair<Integer, List<Integer>>>();
		ret.skill = packet.readInt();
		SkillStats skillStats = SkillDataLoader.getInstance().getSkill(ret.skill);
		ret.charge = skillStats != null && skillStats.isChargedSkill() ? packet.readInt() : 0;

		/*ret.display = */packet.readByte();
		ret.stance = packet.readByte();

		if (ret.skill == Skills.MESO_EXPLOSION)
			return parseMesoExplosion(packet, ret);

		if (ranged) {
			packet.readByte();
			ret.speed = packet.readByte();
			packet.readByte();
			byte direction = packet.readByte();
			packet.skip(7);
			switch (ret.skill) {
				case Skills.HURRICANE:
				case Skills.PIERCING_ARROW:
				case Skills.RAPID_FIRE:
					packet.skip(4);
					ret.stance = direction;
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
			packet.skip(14); // seems to contain some position info

			List<Integer> allDamageNumbers = new ArrayList<Integer>();
			for (int j = 0; j < ret.numDamage; j++) {
				int damage = packet.readInt();
				if (ret.skill == Skills.SNIPE) //setting the most significant bit signifies critical damage...
					damage += 0x80000000;
				allDamageNumbers.add(Integer.valueOf(damage));
			}
			if (ret.skill != Skills.RAPID_FIRE)
				packet.skip(4);
			ret.allDamage.add(new Pair<Integer, List<Integer>>(Integer.valueOf(eid), allDamageNumbers));
		}
		return ret;
	}

	private static void handlePickPocket(Player p, Mob monster, Pair<Integer, List<Integer>> oned) {
		int delay = 0;
		int maxmeso = SkillDataLoader.getInstance().getSkill(Skills.PICK_POCKET).getLevel(p.getEffectValue(PlayerStatusEffect.PICKPOCKET).getLevelWhenCast()).getX();
		double reqdamage = 20000;
		final Point mobPos = monster.getPosition();
		final int mobEntId = monster.getId();
		final int pEntId = p.getId();
		final GameMap tdmap = p.getMap();

		for (Integer eachd : oned.right) {
			if (SkillDataLoader.getInstance().getSkill(Skills.PICK_POCKET).getLevel(p.getSkillLevel(4211003)).shouldPerform()) {
				double perc = eachd.doubleValue() / reqdamage;

				int dropAmt = Math.min(Math.max((int) (perc * maxmeso), 1), maxmeso);
				final Point tdpos = new Point(mobPos.x + Rng.getGenerator().nextInt(100) - 50, mobPos.y);
				final ItemDrop d = new ItemDrop(dropAmt);

				Timer.getInstance().runAfterDelay(new Runnable() {
					public void run() {
						tdmap.drop(d, mobEntId, mobPos, tdpos, ItemDrop.PICKUP_ALLOW_OWNER, pEntId);
					}
				}, delay);

				delay += 100;
			}
		}
	}

	//TODO: handle skills
	private static void applyAttack(AttackInfo attack, final Player player, int attackCount) {
		PlayerSkillEffectsData attackEffect = null;
		if (attack.skill != 0) {
			attackEffect = attack.getAttackEffect(player);
			if (attackEffect == null) {
				player.getClient().getSession().send(CommonPackets.writeEnableActions());
				return;
			}
			if (attack.skill != Skills.HEAL) {
				// heal is both an attack and a special move (healing)
				// so we'll let the whole applying magic live in the special move part
				if (player.isAlive())
					player.applyEffect(attackEffect);
				else
					player.getClient().getSession().send(CommonPackets.writeEnableActions());
			}
		}
		int totDamage = 0;
		final GameMap map = player.getMap();
		if (attack.skill == Skills.MESO_EXPLOSION) {
			int delay = 0;
			for (Pair<Integer, List<Integer>> oned : attack.allDamage) {
				final ItemDrop drop = (ItemDrop) map.getEntityById(EntityType.DROP, oned.left.intValue());
				if (drop != null && drop.getDataId() >= 10) {
					Timer.getInstance().runAfterDelay(new Runnable() {
						public void run() {
							synchronized (drop) {
								if (drop.isAlive())
									map.mesoExplosion(drop, player);
							}
						}
					}, delay);
					delay += 100;
				}
			}
		}

		for (Pair<Integer, List<Integer>> oned : attack.allDamage) {
			//TODO: Synchronize on the monster for aggro and hp stuffs
			Mob monster = (Mob) map.getEntityById(EntityType.MONSTER, oned.left.intValue());

			if (monster != null) {
				int totDamageToOneMonster = 0;
				for (Integer eachd : oned.right)
					totDamageToOneMonster += eachd.intValue();
				totDamage += totDamageToOneMonster;

				player.checkMonsterAggro(monster);

				if (attack.skill == Skills.ENERGY_DRAIN) { // Energy Drain
					int addHp;
					addHp = (int) ((double) totDamage * (double) SkillDataLoader.getInstance().getSkill(Skills.ENERGY_DRAIN).getLevel(player.getSkillLevel(Skills.ENERGY_DRAIN)).getX() / 100.0);
					addHp = Math.min(monster.getMaxHp(), Math.min(addHp, player.getCurrentMaxHp() / 2));
					player.gainHp(addHp);
				}

				if (player.isEffectActive(PlayerStatusEffect.PICKPOCKET)) {
					switch (attack.skill) {
						case 0:
						case Skills.DOUBLE_STAB:
						case Skills.SAVAGE_BLOW:
						case Skills.ASSAULTER:
						case Skills.BAND_OF_THIEVES:
						case Skills.CHAKRA:
						case Skills.SHADOWER_TAUNT:
						case Skills.BOOMERANG_STEP:
							handlePickPocket(player, monster, oned);
							break;
					}
				}
				// effects
				/*switch (attack.skill) {
					case Skills.HEAVENS_HAMMER:
						// TODO min damage still needs calculated.. using -20% as mindamage in the meantime.. seems to work
						int HHDmg = (int) (player.calculateMaxBaseDamage(player.getTotalWatk()) * (SkillDataLoader.getInstance().getSkill(Skills.HEAVENS_HAMMER).getLevel(player.getSkillLevel(Skills.HEAVENS_HAMMER)).getDamage() / 100));
						HHDmg = (int) (Math.floor(Rng.getGenerator().nextDouble() * (HHDmg - HHDmg * .80) + HHDmg * .80));
						monster.damage(player, HHDmg);
						break;
					case Skills.SNIPE:
						totDamageToOneMonster = 95000 + Rng.getGenerator().nextInt(5000);
						break;
					case Skills.DRAIN:
						int gainhp = (int) ((double) totDamageToOneMonster * (double) SkillDataLoader.getInstance().getSkill(Skills.DRAIN).getLevel(player.getSkillLevel(Skills.DRAIN)).getX() / 100.0);
						gainhp = Math.min(monster.getMaxHp(), Math.min(gainhp, player.getMaxHp() / 2));
						player.gainHp(gainhp);
						break;
					default: //passives attack bonuses
						if (totDamageToOneMonster > 0 && monster.isAlive()) {
							if (player.isEffectActive(BuffKey.BLIND)) {
								SkillEffectsData e = SkillDataLoader.getInstance().getSkill(Skills.BLIND).getLevel(player.getSkillLevel(Skills.BLIND));
								if (e.shouldPerform()) {
									MonsterStatusEffect monsterStatusEffect = new MonsterStatusEffect(Collections.singletonMap(MonsterStatus.ACC, player.getBuff(BuffKey.BLIND)), SkillFactory.getSkill(Skills.BLIND), false);
									monster.applyStatus(player, monsterStatusEffect, false, e.getY() * 1000);
								}
							}
							if (player.isEffectActive(BuffKey.HAMSTRING)) {
								SkillEffectsData e = SkillDataLoader.getInstance().getSkill(Skills.HAMSTRING).getLevel(player.getSkillLevel(Skills.HAMSTRING));
								if (e.shouldPerform()) {
									MonsterStatusEffect monsterStatusEffect = new MonsterStatusEffect(Collections.singletonMap(MonsterStatus.SPEED, SkillFactory.getSkill(Skills.HAMSTRING).getEffect(player.getSkillLevel(SkillFactory.getSkill(3121007))).getX()), SkillFactory.getSkill(3121007), false);
									monster.applyStatus(player, monsterStatusEffect, false, e.getY() * 1000);
								}
							}
							if (player.getJob().isA(MapleJob.WHITEKNIGHT)) {
								int[] charges = { Skills.SWORD_ICE_CHARGE, Skills.BW_BLIZZARD_CHARGE };
								for (int charge : charges) {
									BuffState bs = player.getBuff(BuffKey.WK_CHARGE);
									if (bs != null && bs.getSource() == charge) {
										SkillEffectsData e = SkillDataLoader.getInstance().getSkill(charge).getLevel(player.getSkillLevel(charge));
										final ElementalEffectiveness iceEffectiveness = monster.getEffectiveness(Element.ICE);
										if (iceEffectiveness == ElementalEffectiveness.NORMAL || iceEffectiveness == ElementalEffectiveness.WEAK) {
											MonsterStatusEffect monsterStatusEffect = new MonsterStatusEffect(Collections.singletonMap(MonsterStatus.FREEZE, 1), SkillFactory.getSkill(charge), false);
											monster.applyStatus(player, monsterStatusEffect, false, e.getY() * 2000);
										}
										break;
									}
								}
							}
						}
						break;
				}

				//venom
				if (player.getSkillLevel(Skills.VENOMOUS_STAR) > 0) {
					SkillEffectsData e = SkillDataLoader.getInstance().getSkill(Skills.VENOMOUS_STAR).getLevel(player.getSkillLevel(Skills.VENOMOUS_STAR));
					for (int i = 0; i < attackCount; i++) {
						if (e.shouldPerform()) {
							if (monster.getVenomMulti() < 3) {
								monster.setVenomMulti((monster.getVenomMulti() + 1));
								MonsterStatusEffect monsterStatusEffect = new MonsterStatusEffect(Collections.singletonMap(MonsterStatus.POISON, 1), SkillFactory.getSkill(Skills.VENOMOUS_STAR), false);
								monster.applyStatus(player, monsterStatusEffect, false, e.getDuration(), true);
							}
						}
					}
				} else if (player.getSkillLevel(Skills.VENOMOUS_STAB) > 0) {
					SkillEffectsData e = SkillDataLoader.getInstance().getSkill(Skills.VENOMOUS_STAB).getLevel(player.getSkillLevel(Skills.VENOMOUS_STAB));
					for (int i = 0; i < attackCount; i++) {
						if (e.shouldPerform()) {
							if (monster.getVenomMulti() < 3) {
								monster.setVenomMulti((monster.getVenomMulti() + 1));
								MonsterStatusEffect monsterStatusEffect = new MonsterStatusEffect(Collections.singletonMap(MonsterStatus.POISON, 1), SkillFactory.getSkill(Skills.VENOMOUS_STAB), false);
								monster.applyStatus(player, monsterStatusEffect, false, e.getDuration(), true);
							}
						}
					}
				}
				if (totDamageToOneMonster > 0 && attackEffect != null && attackEffect.getMonsterStati().size() > 0) {
					if (attackEffect.shouldPerform()) {
						MonsterStatusEffect monsterStatusEffect = new MonsterStatusEffect(attackEffect.getMonsterStati(), theSkill, false);
						monster.applyStatus(player, monsterStatusEffect, attackEffect.isPoison(), attackEffect.getDuration());
					}
				}*/

				if (attack.skill != Skills.HEAVENS_HAMMER)
					player.getMap().damageMonster(player, monster, totDamageToOneMonster);
			}
		}
	}

	private static void writeMesoExplosion(LittleEndianWriter lew, int cid, AttackInfo info, int projectile) {
		lew.writeInt(cid);
		lew.writeByte(info.getNumAttackedAndDamage());
		lew.writeByte((byte) 0x1E);
		lew.writeInt(info.skill);
		lew.writeByte((byte) 0);
		lew.writeByte(info.stance);
		lew.writeByte(info.speed);
		lew.writeByte((byte) 0x0A);
		lew.writeInt(projectile);

		for (Pair<Integer, List<Integer>> oned : info.allDamage) {
			if (oned.right != null) {
				lew.writeInt(oned.left.intValue());
				lew.writeByte((byte) 0xFF);
				lew.writeByte((byte) oned.right.size());
				for (Integer eachd : oned.right)
					// highest bit set = crit
					lew.writeInt(eachd.intValue());
			}
		}
	}

	private static void writeAttackData(LittleEndianWriter lew, int cid, AttackInfo info, int projectile) {
		lew.writeInt(cid);
		lew.writeByte(info.getNumAttackedAndDamage());
		if (info.skill > 0) {
			lew.writeByte((byte) 0xFF); // too low and some skills don't work (?)
			lew.writeInt(info.skill);
		} else
			lew.writeByte((byte) 0);

		lew.writeByte((byte) 0);
		lew.writeByte(info.stance);
		lew.writeByte(info.speed);
		lew.writeByte((byte) 0x0A);
		lew.writeInt(projectile);

		for (Pair<Integer, List<Integer>> oned : info.allDamage) {
			if (oned.right != null) {
				lew.writeInt(oned.left.intValue());
				lew.writeByte((byte) 0xFF);
				for (Integer eachd : oned.right)
					lew.writeInt(eachd.intValue());
			}
		}
	}

	private static byte[] writeMeleeAttack(int cid, AttackInfo info) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter();
		lew.writeShort(ClientSendOps.MELEE_ATTACK);
		if (info.skill == Skills.MESO_EXPLOSION)
			writeMesoExplosion(lew, cid, info, 0);
		else
			writeAttackData(lew, cid, info, 0);
		return lew.getBytes();
	}

	private static byte[] writeRangedAttack(int cid, AttackInfo info, int projectile) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter();
		lew.writeShort(ClientSendOps.RANGED_ATTACK);
		writeAttackData(lew, cid, info, projectile);
		return lew.getBytes();
	}

	private static byte[] writeMagicAttack(int cid, AttackInfo info) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter();
		lew.writeShort(ClientSendOps.MAGIC_ATTACK);
		writeAttackData(lew, cid, info, 0);
		switch (info.skill) {
			case Skills.FP_BIG_BANG:
			case Skills.IL_BIG_BANG:
			case Skills.BISHOP_BIG_BANG:
				lew.writeInt(info.charge);
				break;
			default:
				lew.writeInt(-1);
				break;
		}
		return lew.getBytes();
	}

	private static class AttackInfo {
		public int skill, charge;
		public byte numAttacked, numDamage;
		public byte stance;
		public List<Pair<Integer, List<Integer>>> allDamage;
		public byte speed;

		public AttackInfo() {
			this.speed = 4;
		}

		public PlayerSkillEffectsData getAttackEffect(Player p) {
			SkillStats skillStats = SkillDataLoader.getInstance().getSkill(skill);
			byte skillLevel = p.getSkillLevel(skill);
			if (skillLevel == 0)
				return null;
			return skillStats.getLevel(skillLevel);
		}

		//numAttacked are the 4 bits that are most significant and
		//numDamage are the 4 bits in that are least significant
		public void setNumAttackedAndDamage(byte combined) {
			numAttacked = (byte) ((combined >>> 4) & 0xF);
			numDamage = (byte) (combined & 0xF);
		}

		public byte getNumAttackedAndDamage() {
			return (byte) (numAttacked << 4 | numDamage);
		}
	}
}
