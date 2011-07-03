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

import argonms.game.character.PlayerJob;
import argonms.game.character.StatusEffectTools;
import argonms.game.character.inventory.Inventory.InventoryType;
import argonms.game.character.inventory.InventorySlot;
import argonms.game.character.inventory.InventoryTools;
import argonms.game.character.inventory.InventoryTools.WeaponClass;
import argonms.game.character.skill.PlayerStatusEffectValues.PlayerStatusEffect;
import argonms.game.character.skill.SkillTools;
import argonms.game.character.skill.Skills;
import argonms.game.character.skill.PlayerStatusEffectValues;
import argonms.game.GameClient;
import argonms.game.character.GameCharacter;
import argonms.common.loading.skill.SkillDataLoader;
import argonms.common.loading.skill.PlayerSkillEffectsData;
import argonms.common.loading.skill.SkillStats;
import argonms.game.field.Element;
import argonms.game.field.GameMap;
import argonms.game.field.MapEntity.EntityType;
import argonms.game.field.MonsterStatusEffectTools;
import argonms.game.field.entity.ItemDrop;
import argonms.game.field.entity.Mob;
import argonms.common.net.external.ClientSendOps;
import argonms.common.net.external.CommonPackets;
import argonms.common.tools.Rng;
import argonms.common.tools.Scheduler;
import argonms.common.tools.input.LittleEndianReader;
import argonms.common.tools.output.LittleEndianByteArrayWriter;
import argonms.common.tools.output.LittleEndianWriter;
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

	public static void handleMeleeAttack(LittleEndianReader packet, GameClient gc) {
		GameCharacter p = gc.getPlayer();
		AttackInfo attack = parseDamage(packet, AttackType.MELEE, p);
		p.getMap().sendToAll(writeMeleeAttack(p.getId(), attack, getMasteryLevel(p)), p);
		applyAttack(attack, p);
	}

	//bow/arrows, claw/stars, guns/bullets (projectiles)
	public static void handleRangedAttack(LittleEndianReader packet, GameClient gc) {
		GameCharacter p = gc.getPlayer();
		AttackInfo attack = parseDamage(packet, AttackType.RANGED, p);
		PlayerSkillEffectsData e = attack.getAttackEffect(p);
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
		}
		if (p.isEffectActive(PlayerStatusEffect.SHADOW_PARTNER))
			useQty *= 2; //freakily enough, shadow partner doubles ALL ranged attacks

		boolean soulArrow = (attack.weaponClass == WeaponClass.BOW || attack.weaponClass == WeaponClass.CROSSBOW) && p.isEffectActive(PlayerStatusEffect.SOUL_ARROW);
		if (!soulArrow) {
			boolean shadowStars = attack.weaponClass == WeaponClass.CLAW && p.isEffectActive(PlayerStatusEffect.SHADOW_STARS);
			if (!shadowStars) { //consume ammo if shadow claw is not active
				InventorySlot slot = p.getInventory(InventoryType.USE).get(attack.ammoSlot);
				if (slot == null || slot.getQuantity() < useQty)
					return; //TODO: hacking
				attack.ammoItemId = slot.getDataId();
				slot.setQuantity((short) (slot.getQuantity() - useQty));
				if (slot.getQuantity() == 0 && !InventoryTools.isRechargeable(attack.ammoItemId)) {
					p.getInventory(InventoryType.USE).remove(attack.ammoSlot);
					gc.getSession().send(CommonPackets.writeInventoryClearSlot(InventoryType.USE, attack.ammoSlot));
				} else {
					gc.getSession().send(CommonPackets.writeInventorySlotUpdate(InventoryType.USE, attack.ammoSlot, slot));
				}
			}
			if (attack.cashAmmoSlot != 0) { //NX throwing stars
				InventorySlot slot = p.getInventory(InventoryType.CASH).get(attack.cashAmmoSlot);
				if (slot == null)
					return; //TODO: hacking
				attack.ammoItemId = slot.getDataId();
			}
			switch (attack.skill) { //skills that do not show visible projectiles
				case Skills.ARROW_RAIN:
				case Skills.ARROW_ERUPTION:
				case Skills.ENERGY_ORB:
					attack.ammoItemId = 0;
					break;
			}
		} else { //soul arrow sends no visible projectile either.
			attack.ammoItemId = 0; //should be 0 already, but just make sure.
		}
		p.getMap().sendToAll(writeRangedAttack(p.getId(), attack, getMasteryLevel(p)), p);
		applyAttack(attack, p);
	}

	public static void handleMagicAttack(LittleEndianReader packet, GameClient gc) {
		GameCharacter p = gc.getPlayer();
		AttackInfo attack = parseDamage(packet, AttackType.MAGIC, p);
		p.getMap().sendToAll(writeMagicAttack(p.getId(), attack), p);
		applyAttack(attack, p);
	}

	public static void handleEnergyChargeAttack(LittleEndianReader packet, GameClient gc) {
		GameCharacter p = gc.getPlayer();
		AttackInfo attack = parseDamage(packet, AttackType.CHARGE, p);
		p.getMap().sendToAll(writeEnergyChargeAttack(p.getId(), attack, getMasteryLevel(p)), p);
		applyAttack(attack, p);
	}

	public static void handleSummonAttack(LittleEndianReader packet, GameClient gc) {
		GameCharacter p = gc.getPlayer();
		AttackInfo attack = parseDamage(packet, AttackType.SUMMON, p);
		p.getMap().sendToAll(writeSummonAttack(p.getId(), attack), p);
		applyAttack(attack, p);
	}

	public static void handlePreparedSkill(LittleEndianReader packet, GameClient gc) {
		GameCharacter p = gc.getPlayer();
		int skillId = packet.readInt();
		byte skillLevel = packet.readByte();
		byte flags = packet.readByte();
		byte speed = packet.readByte();

		if (SkillDataLoader.getInstance().getSkill(skillId).isPrepared()) {
			if (skillId != Skills.CHAKRA) {
				p.getMap().sendToAll(writePreparedSkillEffect(p.getId(), skillId, skillLevel, flags, speed), p);
			} else {
				int dex = p.getCurrentDex();
				int luk = p.getCurrentLuk();
				int recovery = SkillDataLoader.getInstance().getSkill(skillId).getLevel(skillLevel).getY();
				int maximum = (int) ((luk * 6.6 + dex) * 0.2 * (recovery / 100.0 + 1));
				int minimum = (int) ((luk * 3.3 + dex) * 0.2 * (recovery / 100.0 + 1));
				p.gainHp(Rng.getGenerator().nextInt(maximum - minimum + 1) + minimum);
				//apparently chakra does not show a prepared skill effect to the map.
			}
		} else {
			//TODO: hacking
		}
	}

	private static AttackInfo parseDamage(LittleEndianReader packet, AttackType type, GameCharacter p) {
		AttackInfo ret = new AttackInfo();

		if (type != AttackType.SUMMON) {
			/*portals = */packet.readByte();
			ret.setNumAttackedAndDamage(packet.readByte() & 0xFF);
			ret.skill = packet.readInt();
			SkillStats skillStats = SkillDataLoader.getInstance().getSkill(ret.skill);
			ret.charge = skillStats != null && skillStats.isKeydown() ? packet.readInt() : 0;
			/*display = */packet.readByte();
			ret.stance = packet.readByte();
			ret.setWeaponClass(packet.readByte());
			ret.speed = packet.readByte();
			/*int tickCount = */packet.readInt();
		} else {
			ret.summonId = packet.readInt();
			/*tickCount = */packet.readInt();
			ret.stance = packet.readByte();
			ret.numAttacked = packet.readByte();
			ret.numDamage = 1;
		}
		if (type == AttackType.RANGED) {
			ret.ammoSlot = packet.readShort();
			ret.cashAmmoSlot = packet.readShort();
			/*aoe = */packet.readBool();

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

	private static void doPickPocketDrops(GameCharacter p, Mob monster, Entry<Integer, int[]> oned) {
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

				Scheduler.getInstance().runAfterDelay(new Runnable() {
					public void run() {
						tdmap.drop(d, mobPos, tdpos, ItemDrop.PICKUP_ALLOW_OWNER, pEntId);
					}
				}, delay);

				delay += 100;
			}
		}
	}

	private static void giveMonsterDiseasesFromActiveBuffs(GameCharacter player, Mob monster) {
		PlayerStatusEffectValues v;
		PlayerSkillEffectsData e;
		if ((v = player.getEffectValue(PlayerStatusEffect.BLIND)) != null) {
			e = SkillDataLoader.getInstance().getSkill(v.getSource()).getLevel(player.getSkillLevel(v.getLevelWhenCast()));
			if (e.shouldPerform())
				MonsterStatusEffectTools.applyEffectsAndShowVisuals(monster, player, e);
		}
		if ((v = player.getEffectValue(PlayerStatusEffect.HAMSTRING)) != null) {
			e = SkillDataLoader.getInstance().getSkill(v.getSource()).getLevel(player.getSkillLevel(v.getLevelWhenCast()));
			if (e.shouldPerform())
				MonsterStatusEffectTools.applyEffectsAndShowVisuals(monster, player, e);
		}
		if ((v = player.getEffectValue(PlayerStatusEffect.CHARGE)) != null) {
			switch (v.getSource()) {
				case Skills.SWORD_ICE_CHARGE:
				case Skills.BW_BLIZZARD_CHARGE:
					e = SkillDataLoader.getInstance().getSkill(v.getSource()).getLevel(player.getSkillLevel(v.getLevelWhenCast()));
					if (monster.getElementalResistance(Element.ICE) <= Element.EFFECTIVENESS_NORMAL)
						MonsterStatusEffectTools.applyEffectsAndShowVisuals(monster, player, e);
					break;
			}
		}
	}

	private static void giveMonsterDiseasesFromPassiveSkills(final GameCharacter player, Mob monster, WeaponClass weaponClass, int attackCount) {
		byte level;
		PlayerSkillEffectsData e;
		//(when the client says "Usable up to 3 times against each monster", do
		//they mean 3 per player or 3 max for the mob? I'll assume that it's 3
		//max per mob. I guess this is why post-BB venom is not stackable...)
		if (weaponClass == WeaponClass.CLAW && (level = player.getSkillLevel(Skills.VENOMOUS_STAR)) > 0) {
			e = SkillDataLoader.getInstance().getSkill(Skills.VENOMOUS_STAR).getLevel(level);
			for (int i = 0; i < attackCount; i++) {
				if (monster.getVenomCount() < 3 && e.shouldPerform()) {
					monster.addToVenomCount();
					MonsterStatusEffectTools.applyEffectsAndShowVisuals(monster, player, e);
				}
			}
		}
		if (weaponClass == WeaponClass.ONE_HANDED_MELEE && (level = player.getSkillLevel(Skills.VENOMOUS_STAB)) > 0) {
			e = SkillDataLoader.getInstance().getSkill(Skills.VENOMOUS_STAB).getLevel(level);
			for (int i = 0; i < attackCount; i++) {
				if (monster.getVenomCount() < 3 && e.shouldPerform()) {
					monster.addToVenomCount();
					MonsterStatusEffectTools.applyEffectsAndShowVisuals(monster, player, e);
				}
			}
		}

		//MP Eater - just stack them until the monster has no MP if we leveled more than one of them!
		if ((level = player.getSkillLevel(Skills.FP_MP_EATER)) > 0) {
			e = SkillDataLoader.getInstance().getSkill(Skills.FP_MP_EATER).getLevel(level);
			if (e.shouldPerform()) {
				int absorbMp = Math.min(monster.getMaxMp() * e.getX() / 100, monster.getMp());
				if (absorbMp > 0) {
					monster.loseMp(absorbMp);
					player.gainMp(absorbMp);
					player.getClient().getSession().send(CommonPackets.writeSelfVisualEffect(StatusEffectTools.PASSIVE_BUFF, Skills.FP_MP_EATER, level, (byte) -1));
					player.getMap().sendToAll(CommonPackets.writeBuffMapVisualEffect(player, StatusEffectTools.PASSIVE_BUFF, Skills.FP_MP_EATER, level, (byte) -1), player);
				}
			}
		}
		if ((level = player.getSkillLevel(Skills.IL_MP_EATER)) > 0) {
			e = SkillDataLoader.getInstance().getSkill(Skills.IL_MP_EATER).getLevel(level);
			if (e.shouldPerform()) {
				int absorbMp = Math.min(monster.getMaxMp() * e.getX() / 100, monster.getMp());
				if (absorbMp > 0) {
					monster.loseMp(absorbMp);
					player.gainMp(absorbMp);
					player.getClient().getSession().send(CommonPackets.writeSelfVisualEffect(StatusEffectTools.PASSIVE_BUFF, Skills.IL_MP_EATER, level, (byte) -1));
					player.getMap().sendToAll(CommonPackets.writeBuffMapVisualEffect(player, StatusEffectTools.PASSIVE_BUFF, Skills.IL_MP_EATER, level, (byte) -1), player);
				}
			}
		}
		if ((level = player.getSkillLevel(Skills.CLERIC_MP_EATER)) > 0) {
			e = SkillDataLoader.getInstance().getSkill(Skills.CLERIC_MP_EATER).getLevel(level);
			if (e.shouldPerform()) {
				int absorbMp = Math.min(monster.getMaxMp() * e.getX() / 100, monster.getMp());
				if (absorbMp > 0) {
					monster.loseMp(absorbMp);
					player.gainMp(absorbMp);
					player.getClient().getSession().send(CommonPackets.writeSelfVisualEffect(StatusEffectTools.PASSIVE_BUFF, Skills.CLERIC_MP_EATER, level, (byte) -1));
					player.getMap().sendToAll(CommonPackets.writeBuffMapVisualEffect(player, StatusEffectTools.PASSIVE_BUFF, Skills.CLERIC_MP_EATER, level, (byte) -1), player);
				}
			}
		}

		//energy charge
		if ((level = player.getSkillLevel(Skills.ENERGY_CHARGE)) > 0) {
			if (player.getEnergyCharge() != 10000) {
				e = SkillDataLoader.getInstance().getSkill(Skills.ENERGY_CHARGE).getLevel(level);
				player.addToEnergyCharge(e.getX());
				if (player.getEnergyCharge() == 10000) {
					player.addToActiveEffects(PlayerStatusEffect.ENERGY_CHARGE, new PlayerStatusEffectValues(e, (short) 10000));
					final PlayerSkillEffectsData effects = e;
					player.addCancelEffectTask(e, Scheduler.getInstance().runAfterDelay(new Runnable() {
						public void run() {
							player.resetEnergyCharge();
							player.removeCancelEffectTask(effects);
							player.removeFromActiveEffects(PlayerStatusEffect.ENERGY_CHARGE);
							Map<PlayerStatusEffect, Short> updatedStats = Collections.singletonMap(PlayerStatusEffect.ENERGY_CHARGE, Short.valueOf((short) 0));
							player.getClient().getSession().send(CommonPackets.writeUsePirateSkill(updatedStats, 0, 0));
							player.getMap().sendToAll(CommonPackets.writeBuffMapPirateEffect(player, updatedStats, 0, 0), player);
						}
					}, e.getDuration()), level, System.currentTimeMillis() + e.getDuration());
				}
				Map<PlayerStatusEffect, Short> updatedStats = Collections.singletonMap(PlayerStatusEffect.ENERGY_CHARGE, Short.valueOf(player.getEnergyCharge()));
				player.getClient().getSession().send(CommonPackets.writeUsePirateSkill(updatedStats, 0, 0));
				player.getMap().sendToAll(CommonPackets.writeBuffMapPirateEffect(player, updatedStats, 0, 0), player);
				player.getClient().getSession().send(CommonPackets.writeSelfVisualEffect(StatusEffectTools.ACTIVE_BUFF, Skills.ENERGY_CHARGE, level, (byte) -1));
				player.getMap().sendToAll(CommonPackets.writeBuffMapVisualEffect(player, StatusEffectTools.ACTIVE_BUFF, Skills.ENERGY_CHARGE, level, (byte) -1), player);
			}
		}
	}

	//TODO: handle skills
	private static void applyAttack(AttackInfo attack, final GameCharacter player) {
		PlayerSkillEffectsData attackEffect = attack.getAttackEffect(player);
		final GameMap map = player.getMap();
		if (attackEffect != null) { //attack skills
			//apply skill costs
			if (attack.skill != Skills.HEAL) {
				//heal is both an attack (against undead) and a cast skill (healing)
				//so just apply skill costs in the cast skill part
				if (player.isAlive())
					SkillTools.useAttackSkill(player, attackEffect.getDataId(), attackEffect.getLevel());
				else
					player.getClient().getSession().send(CommonPackets.writeEnableActions());
			}
			//perform meso explosion
			if (attack.skill == Skills.MESO_EXPLOSION) {
				int delay = 0;
				for (int meso : attack.mesoExplosion) {
					final ItemDrop drop = (ItemDrop) map.getEntityById(EntityType.DROP, meso);
					if (drop != null) {
						Scheduler.getInstance().runAfterDelay(new Runnable() {
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
		}

		for (Entry<Integer, int[]> oned : attack.allDamage.entrySet()) {
			//TODO: Synchronize on the monster for aggro and hp stuffs
			Mob monster = (Mob) map.getEntityById(EntityType.MONSTER, oned.getKey().intValue());

			if (monster != null) {
				int totDamageToOneMonster = 0;
				for (int eachd : oned.getValue())
					totDamageToOneMonster += eachd;

				player.checkMonsterAggro(monster);

				//specially handled attack skills
				switch (attack.skill) {
					case Skills.DRAIN:
					case Skills.ENERGY_DRAIN:
						int addHp = (int) ((double) totDamageToOneMonster * (double) SkillDataLoader.getInstance().getSkill(attack.skill).getLevel(player.getSkillLevel(attack.skill)).getX() / 100.0);
						addHp = Math.min(monster.getMaxHp(), Math.min(addHp, player.getCurrentMaxHp() / 2));
						player.gainHp(addHp);
						break;
					case Skills.HEAVENS_HAMMER:
						//TODO: min damage still needs to be calculated. Using -20% as mindamage in the meantime seems to work
						//totDamageToOneMonster = (int) (player.calculateMaxBaseDamage(player.getTotalWatk()) * (SkillDataLoader.getInstance().getSkill(Skills.HEAVENS_HAMMER).getLevel(player.getSkillLevel(Skills.HEAVENS_HAMMER)).getDamage() / 100));
						//totDamageToOneMonster = (int) (Math.floor(Rng.getGenerator().nextDouble() * (totDamageToOneMonster * .2) + totDamageToOneMonster * .8));
						break;
					default:
						//see if the attack skill can give the monster a disease
						if (totDamageToOneMonster > 0 && monster.isAlive() && attackEffect != null)
							if (attackEffect.getMonsterEffect() != null && attackEffect.shouldPerform())
								MonsterStatusEffectTools.applyEffectsAndShowVisuals(monster, player, attackEffect);
						break;
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
				//see if any active player buffs can give the monster a disease
				giveMonsterDiseasesFromActiveBuffs(player, monster);
				//see if any passive player skills can give the monster a disease
				giveMonsterDiseasesFromPassiveSkills(player, monster, attack.weaponClass, attack.numDamage);

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

	private static byte getMasteryLevel(GameCharacter p) {
		switch (InventoryTools.getWeaponType(p.getInventory(InventoryType.EQUIPPED).get((short) -11).getDataId())) {
			case SWORD1H:
			case SWORD2H:
				switch (p.getJob()) {
					case PlayerJob.JOB_FIGHTER:
					case PlayerJob.JOB_CRUSADER:
					case PlayerJob.JOB_HERO:
						return p.getSkillLevel(Skills.CRUSADER_SWORD_MASTERY);
					case PlayerJob.JOB_PAGE:
					case PlayerJob.JOB_WHITE_KNIGHT:
					case PlayerJob.JOB_PALADIN:
						return p.getSkillLevel(Skills.PAGE_SWORD_MASTERY);
					default:
						return (byte) Math.max(p.getSkillLevel(Skills.CRUSADER_SWORD_MASTERY), p.getSkillLevel(Skills.PAGE_SWORD_MASTERY));
				}
			case AXE1H:
			case AXE2H:
				return p.getSkillLevel(Skills.AXE_MASTERY);
			case BLUNT1H:
			case BLUNT2H:
				return p.getSkillLevel(Skills.BW_MASTERY);
			case DAGGER:
				return p.getSkillLevel(Skills.DAGGER_MASTERY);
			case SPEAR:
				return p.getSkillLevel(Skills.SPEAR_MASTERY);
			case POLE_ARM:
				return p.getSkillLevel(Skills.POLE_ARM_MASTERY);
			case BOW:
				return p.getSkillLevel(Skills.BOW_MASTERY);
			case CROSSBOW:
				return p.getSkillLevel(Skills.XBOW_MASTERY);
			case CLAW:
				return p.getSkillLevel(Skills.CLAW_MASTERY);
			case KNUCKLE:
				return p.getSkillLevel(Skills.KNUCKLER_MASTERY);
			case GUN:
				return p.getSkillLevel(Skills.GUN_MASTERY);
			case WAND:
			case STAFF:
			default:
				return 0;
		}
	}

	private static void writeAttackData(LittleEndianWriter lew, int cid, AttackInfo info, byte mastery) {
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
		lew.writeByte(mastery);
		lew.writeInt(info.ammoItemId);

		for (Entry<Integer, int[]> oned : info.allDamage.entrySet()) {
			lew.writeInt(oned.getKey().intValue());
			lew.writeByte((byte) 0xFF);
			for (int eachd : oned.getValue())
				lew.writeInt(eachd);
		}
	}

	private static byte[] writeMeleeAttack(int cid, AttackInfo info, byte mastery) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter();
		lew.writeShort(ClientSendOps.MELEE_ATTACK);
		if (info.skill == Skills.MESO_EXPLOSION)
			writeMesoExplosion(lew, cid, info);
		else
			writeAttackData(lew, cid, info, mastery);
		return lew.getBytes();
	}

	private static byte[] writeRangedAttack(int cid, AttackInfo info, byte mastery) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter();
		lew.writeShort(ClientSendOps.RANGED_ATTACK);
		writeAttackData(lew, cid, info, mastery);
		return lew.getBytes();
	}

	private static byte[] writeMagicAttack(int cid, AttackInfo info) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter();
		lew.writeShort(ClientSendOps.MAGIC_ATTACK);
		writeAttackData(lew, cid, info, (byte) 0);
		if (info.charge != 0)
			lew.writeInt(info.charge);
		return lew.getBytes();
	}

	private static byte[] writeEnergyChargeAttack(int cid, AttackInfo info, byte mastery) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter();
		lew.writeShort(ClientSendOps.ENERGY_CHARGE_ATTACK);
		writeAttackData(lew, cid, info, mastery);
		return lew.getBytes();
	}

	private static byte[] writeSummonAttack(int cid, AttackInfo info) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter();
		lew.writeShort(ClientSendOps.SUMMON_ATTACK);
		lew.writeInt(cid);
		lew.writeInt(info.summonId);
		lew.writeByte(info.stance);
		lew.writeByte(info.numAttacked);
		for (Entry<Integer, int[]> oned : info.allDamage.entrySet()) {
			lew.writeInt(oned.getKey().intValue());
			lew.writeByte((byte) 0xFF);
			for (int eachd : oned.getValue())
				lew.writeInt(eachd);
		}
		return lew.getBytes();
	}

	private static byte[] writePreparedSkillEffect(int cid, int skillId, byte level, byte flags, byte speed) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(13);
		lew.writeShort(ClientSendOps.PREPARED_SKILL);
		lew.writeInt(cid);
		lew.writeInt(skillId);
		lew.writeByte(level);
		lew.writeByte(flags);
		lew.writeByte(speed);
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
		public WeaponClass weaponClass;
		public int summonId;

		public AttackInfo() {
			this.speed = 4;
			this.allDamage = new HashMap<Integer, int[]>();
		}

		public PlayerSkillEffectsData getAttackEffect(GameCharacter p) {
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

		public void setWeaponClass(byte value) {
			weaponClass = WeaponClass.valueOf(value);
		}
	}
}
