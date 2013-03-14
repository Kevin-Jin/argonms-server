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

package argonms.game.net.external.handler;

import argonms.common.StatEffect;
import argonms.common.character.PlayerJob;
import argonms.common.character.PlayerStatusEffect;
import argonms.common.character.Skills;
import argonms.common.character.inventory.Equip;
import argonms.common.character.inventory.Inventory.InventoryType;
import argonms.common.character.inventory.InventorySlot;
import argonms.common.character.inventory.InventoryTools;
import argonms.common.character.inventory.InventoryTools.WeaponClass;
import argonms.common.loading.item.ItemDataLoader;
import argonms.common.net.external.CheatTracker;
import argonms.common.net.external.ClientSendOps;
import argonms.common.util.Rng;
import argonms.common.util.Scheduler;
import argonms.common.util.input.LittleEndianReader;
import argonms.common.util.output.LittleEndianByteArrayWriter;
import argonms.common.util.output.LittleEndianWriter;
import argonms.game.character.GameCharacter;
import argonms.game.character.PlayerStatusEffectValues;
import argonms.game.character.SkillTools;
import argonms.game.character.StatusEffectTools;
import argonms.game.field.GameMap;
import argonms.game.field.MapEntity;
import argonms.game.field.MapEntity.EntityType;
import argonms.game.field.MonsterStatusEffectTools;
import argonms.game.field.entity.ItemDrop;
import argonms.game.field.entity.Mist;
import argonms.game.field.entity.Mob;
import argonms.game.field.entity.PlayerSkillSummon;
import argonms.game.loading.skill.PlayerSkillEffectsData;
import argonms.game.loading.skill.SkillDataLoader;
import argonms.game.loading.skill.SkillStats;
import argonms.game.net.external.GameClient;
import argonms.game.net.external.GamePackets;
import java.awt.Point;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ScheduledFuture;

//TODO: log any suspicious damages (must calculate max damage first)
/**
 *
 * @author GoldenKevin
 */
public final class DealDamageHandler {
	private enum AttackType { MELEE, RANGED, MAGIC, SUMMON, CHARGE }

	private static int getMaxBaseDamage(GameCharacter p, int additionalWatk, PlayerSkillEffectsData attackSkill) {
		if (p.getWatk() == 0)
			return 1;

		Equip weapon = (Equip) p.getInventory(InventoryType.EQUIPPED).get((short) -11);
		if (weapon == null)
			return 0;

		Equip.WeaponType type = InventoryTools.getWeaponType(weapon.getDataId());
		int mainStat;
		int secondaryStat;
		switch (InventoryTools.getWeaponType(weapon.getDataId())) {
			case BOW:
			case CROSSBOW:
			case GUN:
				mainStat = p.getCurrentDex();
				secondaryStat = p.getCurrentStr();
				break;
			case CLAW:
			case DAGGER:
				if (PlayerJob.isThief(p.getJob())) {
					mainStat = p.getCurrentLuk();
					secondaryStat = p.getCurrentDex() + p.getCurrentStr();
				} else {
					mainStat = p.getCurrentStr();
					secondaryStat = p.getCurrentDex();
				}
				break;
			case NOT_A_WEAPON: //not sure what this means - it's from celino
				if (PlayerJob.isPirate(p.getJob())) {
					mainStat = p.getCurrentStr();
					secondaryStat = p.getCurrentDex();
				} else {
					mainStat = 0;
					secondaryStat = 0;
				}
				break;
			case KNUCKLE:
			default:
				mainStat = p.getCurrentStr();
				secondaryStat = p.getCurrentDex();
				break;
		}
		int damage = (int) ((type.getMaxDamageMultiplier() * mainStat + secondaryStat) * (p.getWatk() + additionalWatk) / 100);
		if (attackSkill != null)
			damage = damage * attackSkill.getDamage() / 100;
		return damage;
	}

	public static void handleMeleeAttack(LittleEndianReader packet, GameClient gc) {
		CheatTracker.get(gc).logTime("hpr", System.currentTimeMillis() + 5000);
		GameCharacter p = gc.getPlayer();
		if (p.getInventory(InventoryType.EQUIPPED).get((short) -11) == null) {
			CheatTracker.get(gc).suspicious(CheatTracker.Infraction.POSSIBLE_PACKET_EDITING, "Tried to attack without a weapon");
			return;
		}
		AttackInfo attack = parseDamage(packet, AttackType.MELEE, p);
		p.getMap().sendToAll(writeMeleeAttack(p.getId(), attack, getMasteryLevel(p, AttackType.MELEE, attack.skill)), p);
		getMaxBaseDamage(p, 0, attack.getAttackEffect(p));
		applyAttack(attack, AttackType.MELEE, p);
	}

	//bow/arrows, claw/stars, guns/bullets (projectiles)
	public static void handleRangedAttack(LittleEndianReader packet, GameClient gc) {
		CheatTracker.get(gc).logTime("hpr", System.currentTimeMillis() + 5000);
		GameCharacter p = gc.getPlayer();
		if (p.getInventory(InventoryType.EQUIPPED).get((short) -11) == null) {
			CheatTracker.get(gc).suspicious(CheatTracker.Infraction.POSSIBLE_PACKET_EDITING, "Tried to attack without a weapon");
			return;
		}
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
				if (slot == null || slot.getQuantity() < useQty) {
					CheatTracker.get(gc).suspicious(CheatTracker.Infraction.POSSIBLE_PACKET_EDITING, "Tried to use nonexistent ranged weapon ammunition");
					return;
				}
				attack.ammoItemId = slot.getDataId();
				slot.setQuantity((short) (slot.getQuantity() - useQty));
				if (slot.getQuantity() == 0 && !InventoryTools.isRechargeable(attack.ammoItemId)) {
					p.getInventory(InventoryType.USE).remove(attack.ammoSlot);
					gc.getSession().send(GamePackets.writeInventoryClearSlot(InventoryType.USE, attack.ammoSlot));
				} else {
					gc.getSession().send(GamePackets.writeInventoryUpdateSlotQuantity(InventoryType.USE, attack.ammoSlot, slot));
				}
			}
			if (attack.cashAmmoSlot != 0) { //NX throwing stars
				InventorySlot slot = p.getInventory(InventoryType.CASH).get(attack.cashAmmoSlot);
				if (slot == null) {
					CheatTracker.get(gc).suspicious(CheatTracker.Infraction.POSSIBLE_PACKET_EDITING, "Tried to use nonexistent cash shop stars");
					return;
				}
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

		p.getMap().sendToAll(writeRangedAttack(p.getId(), attack, getMasteryLevel(p, AttackType.RANGED, attack.skill)), p);
		int additionalWatk = 0;
		if (attack.ammoItemId != 0) {
			short[] bonusStats = ItemDataLoader.getInstance().getBonusStats(attack.ammoItemId);
			if (bonusStats != null)
				additionalWatk = bonusStats[StatEffect.PAD];
		}
		getMaxBaseDamage(p, additionalWatk, attack.getAttackEffect(p));
		applyAttack(attack, AttackType.RANGED, p);
	}

	public static void handleMagicAttack(LittleEndianReader packet, GameClient gc) {
		CheatTracker.get(gc).logTime("hpr", System.currentTimeMillis() + 5000);
		final GameCharacter p = gc.getPlayer();
		if (p.getInventory(InventoryType.EQUIPPED).get((short) -11) == null) {
			CheatTracker.get(gc).suspicious(CheatTracker.Infraction.POSSIBLE_PACKET_EDITING, "Tried to attack without a weapon");
			return;
		}
		AttackInfo attack = parseDamage(packet, AttackType.MAGIC, p);
		p.getMap().sendToAll(writeMagicAttack(p.getId(), attack, getMasteryLevel(p, AttackType.MAGIC, attack.skill)), p);
		applyAttack(attack, AttackType.MAGIC, p);

		final PlayerSkillEffectsData e = attack.getAttackEffect(p);
		if (e != null) {
			switch (attack.skill) {
				case Skills.POISON_MIST:
					final Mist mist = new Mist(p, e);
					ScheduledFuture<?> poisonSchedule = Scheduler.getInstance().runRepeatedly(new Runnable() {
						@Override
						public void run() {
							for (MapEntity mo : p.getMap().getMapEntitiesInRect(mist.getBox(), EnumSet.of(EntityType.MONSTER)))
								MonsterStatusEffectTools.applyEffectsAndShowVisuals((Mob) mo, p, e);
						}
					}, 2000, 2500);
					p.getMap().spawnMist(mist, e.getDuration(), poisonSchedule);
					break;
			}
		}
	}

	public static void handleEnergyChargeAttack(LittleEndianReader packet, GameClient gc) {
		GameCharacter p = gc.getPlayer();
		if (!p.isEffectActive(PlayerStatusEffect.ENERGY_CHARGE)) {
			CheatTracker.get(gc).suspicious(CheatTracker.Infraction.POSSIBLE_PACKET_EDITING, "Tried to power guard without having energy charge cast");
			return;
		}
		AttackInfo attack = parseDamage(packet, AttackType.CHARGE, p);
		p.getMap().sendToAll(writeEnergyChargeAttack(p.getId(), attack, getMasteryLevel(p, AttackType.CHARGE, attack.skill)), p);
		CheatTracker.get(gc).logTime("hpr", System.currentTimeMillis() + 5000);
		applyAttack(attack, AttackType.CHARGE, p);
	}

	public static void handleSummonAttack(LittleEndianReader packet, GameClient gc) {
		GameCharacter p = gc.getPlayer();
		AttackInfo attack = parseDamage(packet, AttackType.SUMMON, p);
		p.getMap().sendToAll(writeSummonAttack(p.getId(), attack), p);
		applyAttack(attack, AttackType.SUMMON, p);
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
			CheatTracker.get(gc).suspicious(CheatTracker.Infraction.CERTAIN_PACKET_EDITING, "Tried to charge non-prepared skill");
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
		final int mobId = monster.getId();
		final Point mobPos = monster.getPosition();
		final int pEntId = p.getId();
		final GameMap tdmap = p.getMap();

		for (int eachd : oned.getValue()) {
			if (SkillDataLoader.getInstance().getSkill(Skills.PICK_POCKET).getLevel(p.getSkillLevel(Skills.PICK_POCKET)).makeChanceResult()) {
				double perc = eachd / reqdamage;

				int dropAmt = Math.min(Math.max((int) (perc * maxmeso), 1), maxmeso);
				final Point tdpos = new Point(mobPos.x + Rng.getGenerator().nextInt(100) - 50, mobPos.y);
				final ItemDrop d = new ItemDrop(dropAmt);

				Scheduler.getInstance().runAfterDelay(new Runnable() {
					@Override
					public void run() {
						tdmap.drop(d, mobId, mobPos, tdpos, ItemDrop.PICKUP_ALLOW_OWNER, pEntId);
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
			e = SkillDataLoader.getInstance().getSkill(v.getSource()).getLevel(v.getLevelWhenCast());
			MonsterStatusEffectTools.applyEffectsAndShowVisuals(monster, player, e);
		}
		if ((v = player.getEffectValue(PlayerStatusEffect.HAMSTRING)) != null) {
			e = SkillDataLoader.getInstance().getSkill(v.getSource()).getLevel(v.getLevelWhenCast());
			MonsterStatusEffectTools.applyEffectsAndShowVisuals(monster, player, e);
		}
		if ((v = player.getEffectValue(PlayerStatusEffect.CHARGE)) != null) {
			switch (v.getSource()) {
				case Skills.SWORD_ICE_CHARGE:
				case Skills.BW_BLIZZARD_CHARGE:
					e = SkillDataLoader.getInstance().getSkill(v.getSource()).getLevel(v.getLevelWhenCast());
					MonsterStatusEffectTools.applyEffectsAndShowVisuals(monster, player, e);
					break;
			}
		}
	}

	private static void giveMonsterDiseasesFromPassiveSkills(final GameCharacter player, Mob monster, WeaponClass weaponClass, int attackCount, AttackType type) {
		byte level;
		PlayerSkillEffectsData e;
		//(when the client says "Usable up to 3 times against each monster", do
		//they mean 3 per player or 3 max for the mob? I'll assume that it's 3
		//max per mob. I guess this is why post-BB venom is not stackable...)
		if (type == AttackType.RANGED && weaponClass == WeaponClass.CLAW && (level = player.getSkillLevel(Skills.VENOMOUS_STAR)) > 0) {
			e = SkillDataLoader.getInstance().getSkill(Skills.VENOMOUS_STAR).getLevel(level);
			for (int i = 0; i < attackCount; i++)
				MonsterStatusEffectTools.applyEffectsAndShowVisuals(monster, player, e);
		}
		if (type == AttackType.MELEE && weaponClass == WeaponClass.ONE_HANDED_MELEE && (level = player.getSkillLevel(Skills.VENOMOUS_STAB)) > 0) {
			e = SkillDataLoader.getInstance().getSkill(Skills.VENOMOUS_STAB).getLevel(level);
			for (int i = 0; i < attackCount; i++)
				MonsterStatusEffectTools.applyEffectsAndShowVisuals(monster, player, e);
		}

		//MP Eater - just stack them until the monster has no MP if we leveled more than one of them!
		if ((level = player.getSkillLevel(Skills.FP_MP_EATER)) > 0) {
			e = SkillDataLoader.getInstance().getSkill(Skills.FP_MP_EATER).getLevel(level);
			if (e.makeChanceResult()) {
				int absorbMp = Math.min(monster.getMaxMp() * e.getX() / 100, monster.getMp());
				if (absorbMp > 0) {
					monster.loseMp(absorbMp);
					player.gainMp(absorbMp);
					player.getClient().getSession().send(GamePackets.writeSelfVisualEffect(StatusEffectTools.ACTIVE_BUFF, Skills.FP_MP_EATER, level, (byte) -1));
					player.getMap().sendToAll(GamePackets.writeBuffMapVisualEffect(player, StatusEffectTools.ACTIVE_BUFF, Skills.FP_MP_EATER, level, (byte) -1), player);
				}
			}
		}
		if ((level = player.getSkillLevel(Skills.IL_MP_EATER)) > 0) {
			e = SkillDataLoader.getInstance().getSkill(Skills.IL_MP_EATER).getLevel(level);
			if (e.makeChanceResult()) {
				int absorbMp = Math.min(monster.getMaxMp() * e.getX() / 100, monster.getMp());
				if (absorbMp > 0) {
					monster.loseMp(absorbMp);
					player.gainMp(absorbMp);
					player.getClient().getSession().send(GamePackets.writeSelfVisualEffect(StatusEffectTools.ACTIVE_BUFF, Skills.IL_MP_EATER, level, (byte) -1));
					player.getMap().sendToAll(GamePackets.writeBuffMapVisualEffect(player, StatusEffectTools.ACTIVE_BUFF, Skills.IL_MP_EATER, level, (byte) -1), player);
				}
			}
		}
		if ((level = player.getSkillLevel(Skills.CLERIC_MP_EATER)) > 0) {
			e = SkillDataLoader.getInstance().getSkill(Skills.CLERIC_MP_EATER).getLevel(level);
			if (e.makeChanceResult()) {
				int absorbMp = Math.min(monster.getMaxMp() * e.getX() / 100, monster.getMp());
				if (absorbMp > 0) {
					monster.loseMp(absorbMp);
					player.gainMp(absorbMp);
					player.getClient().getSession().send(GamePackets.writeSelfVisualEffect(StatusEffectTools.ACTIVE_BUFF, Skills.CLERIC_MP_EATER, level, (byte) -1));
					player.getMap().sendToAll(GamePackets.writeBuffMapVisualEffect(player, StatusEffectTools.ACTIVE_BUFF, Skills.CLERIC_MP_EATER, level, (byte) -1), player);
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
						@Override
						public void run() {
							player.resetEnergyCharge();
							player.removeCancelEffectTask(effects);
							player.removeFromActiveEffects(PlayerStatusEffect.ENERGY_CHARGE);
							Map<PlayerStatusEffect, Short> updatedStats = Collections.singletonMap(PlayerStatusEffect.ENERGY_CHARGE, Short.valueOf((short) 0));
							player.getClient().getSession().send(GamePackets.writeUsePirateSkill(updatedStats, 0, 0, (short) 0));
							player.getMap().sendToAll(GamePackets.writeBuffMapPirateEffect(player, updatedStats, 0, 0), player);
						}
					}, e.getDuration()), level, System.currentTimeMillis() + e.getDuration());
				}
				Map<PlayerStatusEffect, Short> updatedStats = Collections.singletonMap(PlayerStatusEffect.ENERGY_CHARGE, Short.valueOf(player.getEnergyCharge()));
				player.getClient().getSession().send(GamePackets.writeUsePirateSkill(updatedStats, 0, 0, (short) 0));
				player.getMap().sendToAll(GamePackets.writeBuffMapPirateEffect(player, updatedStats, 0, 0), player);
				player.getClient().getSession().send(GamePackets.writeSelfVisualEffect(StatusEffectTools.PASSIVE_BUFF, Skills.ENERGY_CHARGE, level, (byte) -1));
				player.getMap().sendToAll(GamePackets.writeBuffMapVisualEffect(player, StatusEffectTools.PASSIVE_BUFF, Skills.ENERGY_CHARGE, level, (byte) -1), player);
			}
		}
	}

	private static void applyAttack(AttackInfo attack, AttackType type, final GameCharacter player) {
		PlayerSkillEffectsData attackEffect = attack.getAttackEffect(player);
		final GameMap map = player.getMap();
		PlayerStatusEffectValues combo = player.getEffectValue(PlayerStatusEffect.COMBO);
		if (attackEffect != null && attack.skill != 0) { //attack skills
			//apply skill costs
			if (attack.skill != Skills.HEAL) {
				//heal is both an attack (against undead) and a cast skill (healing)
				//so just apply skill costs in the cast skill part
				if (player.isAlive())
					SkillTools.useAttackSkill(player, attack.skill, attackEffect.getLevel());
				else
					player.getClient().getSession().send(GamePackets.writeEnableActions());
			}
			switch (attack.skill) {
				case Skills.MESO_EXPLOSION: {
					int delay = 0;
					for (int meso : attack.mesoExplosion) {
						final ItemDrop drop = (ItemDrop) map.getEntityById(EntityType.DROP, meso);
						if (drop != null) {
							Scheduler.getInstance().runAfterDelay(new Runnable() {
								@Override
								public void run() {
									if (drop.isAlive())
										map.mesoExplosion(drop, player);
								}
							}, delay);
							delay += 100;
						}
					}
					break;
				}
				case Skills.CHARGED_BLOW: {
					PlayerStatusEffectValues chargeBuff = player.getEffectValue(PlayerStatusEffect.CHARGE);
					if (chargeBuff == null) {
						CheatTracker.get(player.getClient()).suspicious(CheatTracker.Infraction.POSSIBLE_PACKET_EDITING, "Tried to use Charged Blow without Charge buff");
						return;
					}
					byte advancedBlowLvl = player.getSkillLevel(Skills.ADVANCED_CHARGE_BLOW);
					if (advancedBlowLvl == 0 || Rng.getGenerator().nextInt(100) >= SkillDataLoader.getInstance().getSkill(Skills.ADVANCED_CHARGE_BLOW).getLevel(advancedBlowLvl).getX())
						StatusEffectTools.dispelEffectsAndShowVisuals(player, chargeBuff.getEffectsData());
					break;
				}
				case Skills.SWORD_PANIC:
				case Skills.AXE_PANIC:
				case Skills.SWORD_COMA:
				case Skills.AXE_COMA:
					if (combo == null) {
						CheatTracker.get(player.getClient()).suspicious(CheatTracker.Infraction.POSSIBLE_PACKET_EDITING, "Tried to use Panic or Coma without Combo Attack buff");
						return;
					}

					StatusEffectTools.updateComboCounter(player, combo, (short) 1);
					combo = null; //so that combo is not incremented below...
					break;
			}
		}
		if (combo != null && !attack.allDamage.isEmpty()) { //TODO: make sure to increment counter for final attack as well
			short counter = combo.getModifier();
			if ((counter - 1) < ((PlayerSkillEffectsData) combo.getEffectsData()).getX())
				StatusEffectTools.updateComboCounter(player, combo, (short) (counter + 1));
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
					case Skills.SACRIFICE:
						player.setHp((short) Math.max(player.getHp() - totDamageToOneMonster * attackEffect.getX() / 100, 1));
						break;
					case Skills.POISON_MIST:
						//TODO: could Poison Mist poison a monster as a basic attack?
						//if (totDamageToOneMonster > 0 && monster.isAlive() && attackEffect != null)
							//MonsterStatusEffectTools.applyEffectsAndShowVisuals(monster, player, attackEffect);
						break;
					case Skills.FP_ELEMENT_COMPOSITION:
					case Skills.IL_ELEMENT_COMPOSITION:
					default:
						//see if the attack skill can give the monster a disease
						if (totDamageToOneMonster > 0 && monster.isAlive() && attackEffect != null)
							if (!attackEffect.getMonsterEffects().isEmpty())
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
				giveMonsterDiseasesFromPassiveSkills(player, monster, attack.weaponClass, attack.numDamage, type);

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

	private static byte getMasteryLevel(GameCharacter p, AttackType type, int skill) {
		List<Integer> skills = new ArrayList<Integer>();
		switch (type) {
			case MAGIC:
				skills.add(Integer.valueOf(skill));
				break;
			case MELEE:
				switch (InventoryTools.getWeaponType(p.getInventory(InventoryType.EQUIPPED).get((short) -11).getDataId())) {
					case SWORD1H:
					case SWORD2H:
						switch (p.getJob()) {
							case PlayerJob.JOB_FIGHTER:
							case PlayerJob.JOB_CRUSADER:
							case PlayerJob.JOB_HERO:
								skills.add(Integer.valueOf(Skills.CRUSADER_SWORD_MASTERY));
								break;
							case PlayerJob.JOB_PAGE:
							case PlayerJob.JOB_WHITE_KNIGHT:
							case PlayerJob.JOB_PALADIN:
								skills.add(Integer.valueOf(Skills.PAGE_SWORD_MASTERY));
								break;
							default:
								if (p.getSkillLevel(Skills.CRUSADER_SWORD_MASTERY) > p.getSkillLevel(Skills.PAGE_SWORD_MASTERY))
									skills.add(Integer.valueOf(Skills.CRUSADER_SWORD_MASTERY));
								else
									skills.add(Integer.valueOf(Skills.PAGE_SWORD_MASTERY));
								break;
						}
						break;
					case AXE1H:
					case AXE2H:
						skills.add(Integer.valueOf(Skills.AXE_MASTERY));
						break;
					case BLUNT1H:
					case BLUNT2H:
						skills.add(Integer.valueOf(Skills.BW_MASTERY));
						break;
					case DAGGER:
						skills.add(Integer.valueOf(Skills.DAGGER_MASTERY));
						break;
					case SPEAR:
						skills.add(Integer.valueOf(Skills.SPEAR_MASTERY));
						skills.add(Integer.valueOf(Skills.BEHOLDER));
						break;
					case POLE_ARM:
						skills.add(Integer.valueOf(Skills.POLE_ARM_MASTERY));
						skills.add(Integer.valueOf(Skills.BEHOLDER));
						break;
					case KNUCKLE:
						skills.add(Integer.valueOf(Skills.KNUCKLER_MASTERY));
						break;
				}
				break;
			case RANGED:
				switch (InventoryTools.getWeaponType(p.getInventory(InventoryType.EQUIPPED).get((short) -11).getDataId())) {
					case BOW:
						skills.add(Integer.valueOf(Skills.BOW_MASTERY));
						skills.add(Integer.valueOf(Skills.BOW_EXPERT));
						break;
					case CROSSBOW:
						skills.add(Integer.valueOf(Skills.XBOW_MASTERY));
						skills.add(Integer.valueOf(Skills.MARKSMAN_BOOST));
						break;
					case CLAW:
						skills.add(Integer.valueOf(Skills.CLAW_MASTERY));
						break;
					case GUN:
						skills.add(Integer.valueOf(Skills.GUN_MASTERY));
						break;
				}
				break;
		}
		byte sum = 0;
		for (Integer skillId : skills) {
			PlayerSkillEffectsData skillData = SkillDataLoader.getInstance().getSkill(skillId.intValue()).getLevel(p.getSkillLevel(skillId.intValue()));
			if (skillData != null)
				sum += skillData.getMastery();
		}
		return sum;
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

	private static byte[] writeMagicAttack(int cid, AttackInfo info, byte mastery) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter();
		lew.writeShort(ClientSendOps.MAGIC_ATTACK);
		writeAttackData(lew, cid, info, mastery);
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
			int skillId;
			if (skill != 0) {
				skillId = skill;
			} else if (summonId != 0) {
				PlayerSkillSummon summon = (PlayerSkillSummon) p.getMap().getEntityById(EntityType.SUMMON, summonId);
				if (summon == null)
					return null;
				skillId = summon.getSkillId();
			} else {
				return null;
			}
			SkillStats skillStats = SkillDataLoader.getInstance().getSkill(skillId);
			byte skillLevel = p.getSkillLevel(skillId);
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

	private DealDamageHandler() {
		//uninstantiable...
	}
}
