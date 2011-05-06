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
import argonms.character.inventory.Inventory.InventoryType;
import argonms.character.inventory.InventorySlot;
import argonms.character.inventory.InventoryTools;
import argonms.character.skill.PlayerStatusEffectValues.PlayerStatusEffect;
import argonms.character.skill.SkillTools;
import argonms.character.skill.Skills;
import argonms.character.inventory.InventoryTools.AmmoType;
import argonms.game.GameClient;
import argonms.loading.skill.SkillDataLoader;
import argonms.loading.skill.PlayerSkillEffectsData;
import argonms.loading.skill.SkillStats;
import argonms.map.GameMap;
import argonms.map.MapEntity.EntityType;
import argonms.map.entity.ItemDrop;
import argonms.map.entity.Mob;
import argonms.net.external.ClientSendOps;
import argonms.net.external.CommonPackets;
import argonms.net.external.RemoteClient;
import argonms.tools.Rng;
import argonms.tools.Timer;
import argonms.tools.input.LittleEndianReader;
import argonms.tools.output.LittleEndianByteArrayWriter;
import argonms.tools.output.LittleEndianWriter;
import java.awt.Point;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 *
 * @author GoldenKevin
 */
public class DealDamageHandler {
	private enum AttackType { MELEE, RANGED, MAGIC, SUMMON, CHARGE }

	public static void handleMeleeAttack(LittleEndianReader packet, RemoteClient rc) {
		Player p = ((GameClient) rc).getPlayer();
		AttackInfo attack = parseDamage(packet, AttackType.MELEE, p);
		PlayerSkillEffectsData e = attack.getAttackEffect(p);
		int attackCount = 1;
		if (e != null) {
			attackCount = e.getAttackCount();
			if (e.getCooltime() > 0) {
				rc.getSession().send(CommonPackets.writeCooldown(attack.skill, e.getCooltime()));
				p.addCooldown(attack.skill, e.getCooltime());
			}
		}
		p.getMap().sendToAll(writeMeleeAttack(p.getId(), attack), p.getPosition(), p);
		applyAttack(attack, p, attackCount);
	}

	//bow/arrows, claw/stars, guns/bullets (projectiles)
	public static void handleRangedAttack(LittleEndianReader packet, RemoteClient rc) {
		Player p = ((GameClient) rc).getPlayer();
		AttackInfo attack = parseDamage(packet, AttackType.RANGED, p);
		PlayerSkillEffectsData e = attack.getAttackEffect(p);
		int attackCount = 1;
		short useQty;
		if (e == null) { //not a skill
			useQty = 1;
		} else { //skill
			//check if it uses more than one piece of ammo
			useQty = e.getBulletConsume();
			if (useQty == 0)
				useQty = e.getBulletCount();
			if (useQty == 0)
				useQty = 1; //skill uses same amount of ammo as regular attack

			//do the usual skill stuff - cooldowns
			attackCount = e.getAttackCount();
			if (e.getCooltime() > 0) {
				rc.getSession().send(CommonPackets.writeCooldown(attack.skill, e.getCooltime()));
				p.addCooldown(attack.skill, e.getCooltime());
			}
		}
		if (p.isEffectActive(PlayerStatusEffect.SHADOW_PARTNER))
			useQty *= 2; //freakily enough, shadow partner doubles ALL ranged attacks
		AmmoType ammoType = AmmoType.getForPlayer(p);
		if (ammoType == null)
			return; //TODO: hacking
		int itemId = 0;
		boolean soulArrow = (ammoType == AmmoType.BOW_ARROW || ammoType == AmmoType.XBOW_ARROW) && p.isEffectActive(PlayerStatusEffect.SOUL_ARROW);
		if (attack.cashAmmoSlot != 0 && !soulArrow) { //soul arrow always shows special effects even if we have a cash ammo.
			InventorySlot slot = p.getInventory(InventoryType.CASH).get(attack.cashAmmoSlot);
			if (slot == null)
				return; //TODO: hacking
			itemId = slot.getDataId();
		}
		if (attack.ammoSlot != 0) {
			InventorySlot slot = p.getInventory(InventoryType.USE).get(attack.ammoSlot);
			if (slot == null || slot.getQuantity() < useQty || !ammoType.canUse(itemId = slot.getDataId())) {
				return; //TODO: hacking
			}
			slot.setQuantity((short) (slot.getQuantity() - useQty));
			if (slot.getQuantity() == 0 && !InventoryTools.isRechargeable(itemId)) {
				p.getInventory(InventoryType.USE).remove(attack.ammoSlot);
				rc.getSession().send(CommonPackets.writeInventoryClearSlot(InventoryType.USE, attack.ammoSlot));
			} else {
				rc.getSession().send(CommonPackets.writeInventorySlotUpdate(InventoryType.USE, attack.ammoSlot, slot));
			}
			switch (attack.skill) {
				case Skills.ARROW_RAIN:
				case Skills.ARROW_ERUPTION:
				case Skills.ENERGY_ORB:
					//these skills show no visible projectile apparently
					itemId = 0;
					break;
			}
		} else if (!(ammoType == AmmoType.STAR && p.isEffectActive(PlayerStatusEffect.SHADOW_STARS))
				&& !soulArrow) {
			//TODO: hacking
			return;
		}
		p.getMap().sendToAll(writeRangedAttack(p.getId(), attack, itemId), p.getPosition(), p);
		applyAttack(attack, p, attackCount);
	}

	public static void handleMagicAttack(LittleEndianReader packet, RemoteClient rc) {
		Player p = ((GameClient) rc).getPlayer();
		AttackInfo attack = parseDamage(packet, AttackType.MAGIC, p);
		p.getMap().sendToAll(writeMagicAttack(p.getId(), attack), p.getPosition(), p);
	}

	private static AttackInfo parseDamage(LittleEndianReader packet, AttackType type, Player p) {
		AttackInfo ret = new AttackInfo();

		if (type != AttackType.SUMMON) {
			/*portals = */packet.readByte();
			ret.setNumAttackedAndDamage(packet.readByte() & 0xFF);
			ret.skill = packet.readInt();
			SkillStats skillStats = SkillDataLoader.getInstance().getSkill(ret.skill);
			ret.charge = skillStats != null && skillStats.isChargedSkill() ? packet.readInt() : 0;
			/*display = */packet.readByte();
			ret.stance = packet.readByte();
			/*weaponClass = */packet.readByte();
			ret.speed = packet.readByte();
			/*int tickCount = */packet.readInt();
		} else {
			/*summonId = */packet.readInt();
			/*tickCount = */packet.readInt();
			ret.stance = packet.readByte();
			ret.numAttacked = packet.readByte();
			ret.numDamage = 1;
		}
		if (type == AttackType.RANGED) {
			ret.ammoSlot = packet.readShort();
			ret.cashAmmoSlot = packet.readShort();
			packet.skip(1); //0x00 = AoE?

			if (p.isEffectActive(PlayerStatusEffect.SHADOW_STARS))
				ret.ammoItemId = packet.readInt();
		}

		byte numDamaged = ret.numDamage;
		for (int i = 0; i < ret.numAttacked; i++) {
			int mobEid = packet.readInt();
			packet.skip(4);
			/*mobPos = */packet.readPos();
			/*damagePos = */packet.readPos();
			if (ret.skill != Skills.MESO_EXPLOSION)
				/*distance = */packet.readShort();
			else
				numDamaged = packet.readByte();

			int[] allDamageNumbers = new int[numDamaged];
			for (int j = 0; j < numDamaged; j++)
				allDamageNumbers[j] = packet.readInt();
			if (type != AttackType.SUMMON)
				packet.skip(4);
			ret.allDamage.put(Integer.valueOf(mobEid), allDamageNumbers);
		}
		/*playerPos = */packet.readPos();
		if (ret.skill == Skills.MESO_EXPLOSION) {
			byte mesoExplodeCount = packet.readByte();
			ret.mesoExplosion = new int[mesoExplodeCount];
			for (int i = 0; i < mesoExplodeCount; i++) {
				int mesoEid = packet.readInt();
				/*monstersKilled = */packet.readByte();
				ret.mesoExplosion[i] = mesoEid;
			}
			packet.readShort();
		}

		return ret;
	}

	private static void doPickPocketDrops(Player p, Mob monster, Entry<Integer, int[]> oned) {
		int delay = 0;
		int maxmeso = SkillDataLoader.getInstance().getSkill(Skills.PICK_POCKET).getLevel(p.getEffectValue(PlayerStatusEffect.PICKPOCKET).getLevelWhenCast()).getX();
		double reqdamage = 20000;
		final Point mobPos = monster.getPosition();
		final int pEntId = p.getId();
		final GameMap tdmap = p.getMap();

		for (int eachd : oned.getValue()) {
			if (SkillDataLoader.getInstance().getSkill(Skills.PICK_POCKET).getLevel(p.getSkillLevel(4211003)).shouldPerform()) {
				double perc = (double) eachd / reqdamage;

				int dropAmt = Math.min(Math.max((int) (perc * maxmeso), 1), maxmeso);
				final Point tdpos = new Point(mobPos.x + Rng.getGenerator().nextInt(100) - 50, mobPos.y);
				final ItemDrop d = new ItemDrop(dropAmt);

				Timer.getInstance().runAfterDelay(new Runnable() {
					public void run() {
						tdmap.drop(d, mobPos, tdpos, ItemDrop.PICKUP_ALLOW_OWNER, pEntId);
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
					SkillTools.useAttackSkill(player, attackEffect.getDataId(), attackEffect.getLevel());
				else
					player.getClient().getSession().send(CommonPackets.writeEnableActions());
			}
		}
		int totDamage = 0;
		final GameMap map = player.getMap();
		if (attack.skill == Skills.MESO_EXPLOSION) {
			int delay = 0;
			for (int meso : attack.mesoExplosion) {
				final ItemDrop drop = (ItemDrop) map.getEntityById(EntityType.DROP, meso);
				if (drop != null) {
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

		for (Entry<Integer, int[]> oned : attack.allDamage.entrySet()) {
			//TODO: Synchronize on the monster for aggro and hp stuffs
			Mob monster = (Mob) map.getEntityById(EntityType.MONSTER, oned.getKey().intValue());

			if (monster != null) {
				int totDamageToOneMonster = 0;
				for (int eachd : oned.getValue())
					totDamageToOneMonster += eachd;
				totDamage += totDamageToOneMonster;

				player.checkMonsterAggro(monster);

				if (attack.skill == Skills.ENERGY_DRAIN) {
					int addHp = (int) ((double) totDamage * (double) SkillDataLoader.getInstance().getSkill(Skills.ENERGY_DRAIN).getLevel(player.getSkillLevel(Skills.ENERGY_DRAIN)).getX() / 100.0);
					addHp = Math.min(monster.getMaxHp(), Math.min(addHp, player.getCurrentMaxHp() / 2));
					player.gainHp(addHp);
				}

				if (player.isEffectActive(PlayerStatusEffect.PICKPOCKET)) {
					switch (attack.skill) { //TODO: this is probably not an exhaustive list...
						case 0:
						case Skills.DOUBLE_STAB:
						case Skills.SAVAGE_BLOW:
						case Skills.ASSAULTER:
						case Skills.BAND_OF_THIEVES:
						case Skills.CHAKRA:
						case Skills.SHADOWER_TAUNT:
						case Skills.BOOMERANG_STEP:
							doPickPocketDrops(player, monster, oned);
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

	private static void writeMesoExplosion(LittleEndianWriter lew, int cid, AttackInfo info) {
		lew.writeInt(cid);
		lew.writeByte(info.getNumAttackedAndDamage());
		lew.writeByte((byte) 0xFF);
		lew.writeInt(info.skill);
		lew.writeByte((byte) 0);
		lew.writeByte(info.stance);
		lew.writeByte(info.speed);
		lew.writeByte((byte) 0x0A);
		lew.writeInt(0);

		for (Entry<Integer, int[]> oned : info.allDamage.entrySet()) {
			lew.writeInt(oned.getKey().intValue());
			lew.writeByte((byte) 0xFF);
			lew.writeByte((byte) oned.getValue().length);
			for (int eachd : oned.getValue())
				lew.writeInt(eachd);
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

		for (Entry<Integer, int[]> oned : info.allDamage.entrySet()) {
			lew.writeInt(oned.getKey().intValue());
			lew.writeByte((byte) 0xFF);
			for (int eachd : oned.getValue())
				lew.writeInt(eachd);
		}
	}

	private static byte[] writeMeleeAttack(int cid, AttackInfo info) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter();
		lew.writeShort(ClientSendOps.MELEE_ATTACK);
		if (info.skill == Skills.MESO_EXPLOSION)
			writeMesoExplosion(lew, cid, info);
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
		public short ammoSlot, cashAmmoSlot;
		public int skill, charge, ammoItemId;
		public byte numAttacked, numDamage;
		public byte stance;
		public Map<Integer, int[]> allDamage;
		public int[] mesoExplosion;
		public byte speed;

		public AttackInfo() {
			this.speed = 4;
			this.allDamage = new HashMap<Integer, int[]>();
		}

		public PlayerSkillEffectsData getAttackEffect(Player p) {
			if (skill == 0)
				return null;
			SkillStats skillStats = SkillDataLoader.getInstance().getSkill(skill);
			byte skillLevel = p.getSkillLevel(skill);
			if (skillLevel == 0)
				return null;
			return skillStats.getLevel(skillLevel);
		}

		public void setNumAttackedAndDamage(int combined) {
			numAttacked = (byte) (combined / 0x10); //4 bits that are most significant
			numDamage = (byte) (combined % 0x10); //4 bits in that are least significant
		}

		public byte getNumAttackedAndDamage() {
			return (byte) (numAttacked * 0x10 | numDamage);
		}
	}
}
