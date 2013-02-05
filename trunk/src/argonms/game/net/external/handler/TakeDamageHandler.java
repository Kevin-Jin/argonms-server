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

import argonms.common.character.PlayerStatusEffect;
import argonms.common.net.external.CheatTracker;
import argonms.common.net.external.ClientSendOps;
import argonms.common.util.Rng;
import argonms.common.util.input.LittleEndianReader;
import argonms.common.util.output.LittleEndianByteArrayWriter;
import argonms.game.character.DiseaseTools;
import argonms.game.character.GameCharacter;
import argonms.game.character.PlayerStatusEffectValues;
import argonms.game.character.SkillTools;
import argonms.game.field.MapEntity.EntityType;
import argonms.game.field.entity.Mob;
import argonms.game.field.entity.PlayerSkillSummon;
import argonms.game.loading.mob.Attack;
import argonms.game.loading.mob.MobDataLoader;
import argonms.game.net.external.GameClient;
import java.awt.Point;

/**
 *
 * @author GoldenKevin
 */
public final class TakeDamageHandler {
	private static final byte
		BUMP_DAMAGE = -1, //the kind of damage you take when you run into a mob
		MAP_DAMAGE = -2 //e.g. vines b/w henesys and ellinia
	;

	public static void handleTakeDamage(LittleEndianReader packet, GameClient gc) {
		GameCharacter p = gc.getPlayer();

		/*int tickCount = */packet.readInt();
		byte attack = packet.readByte();
		/*byte elem = */packet.readByte();
		int damage = packet.readInt();
		if (damage != 0)
			CheatTracker.get(gc).logTime("hpr", System.currentTimeMillis() + 5000);

		int mobid = 0;
		int ent = 0;
		byte direction = 0;
		byte diseaseLevel = 0;
		byte diseaseSkill = 0;
		short mpBurn = 0;
		boolean deadlyAttack = false;
		MobReturnDamage pgmr = new MobReturnDamage(); //power guard/mana reflection
		byte stance = 0;
		int noDamageId = 0;

		if (attack == MAP_DAMAGE) {
			diseaseLevel = packet.readByte();
			diseaseSkill = packet.readByte();
		} else {
			mobid = packet.readInt();
			ent = packet.readInt();
			direction = packet.readByte();
			Mob m = (Mob) p.getMap().getEntityById(EntityType.MONSTER, ent);
			if (m != null) { //lag...
				if (m.getDataId() != mobid) {
					CheatTracker.get(gc).suspicious(CheatTracker.Infraction.POSSIBLE_PACKET_EDITING, "Tried to spoof mob attack");
					return;
				}
				if (attack != BUMP_DAMAGE) {
					Attack a = MobDataLoader.getInstance().getMobStats(mobid).getAttacks().get(Byte.valueOf(attack));
					deadlyAttack = a.isDeadlyAttack();
					diseaseLevel = a.getDiseaseLevel();
					diseaseSkill = a.getDiseaseSkill();
					mpBurn = a.getMpBurn();
				}
				byte reduction = packet.readByte();
				packet.readByte();
				if (reduction != 0) {
					pgmr.setReduction(reduction);
					pgmr.setPhysical(packet.readBool());
					pgmr.setEntityId(packet.readInt());
					pgmr.setSkill(packet.readByte()); //powerguard = 6, mana reflection = 0
					pgmr.setPosition(packet.readPos());
					pgmr.setDamage(damage);
					int hurtDmg = (damage * reduction / 100);
					if (pgmr.isPhysical())
						damage = (damage - hurtDmg);
					switch (pgmr.getSkill()) {
						case 0:
							if (!p.isEffectActive(PlayerStatusEffect.MANA_REFLECTION)) {
								CheatTracker.get(gc).suspicious(CheatTracker.Infraction.POSSIBLE_PACKET_EDITING, "Tried to power guard without having mana reflection cast");
								return;
							}
							break;
						case 6:
							if (!p.isEffectActive(PlayerStatusEffect.POWER_GUARD)) {
								CheatTracker.get(gc).suspicious(CheatTracker.Infraction.POSSIBLE_PACKET_EDITING, "Tried to power guard without having power guard cast");
								return;
							}
							break;
					}
					m.hurt(p, hurtDmg);
					p.getMap().sendToAll(writeHurtMonster(m, hurtDmg, false));
				}
				stance = packet.readByte();
				if (stance > 0 && !p.isEffectActive(PlayerStatusEffect.POWER_STANCE) && !p.isEffectActive(PlayerStatusEffect.POWER_GUARD) && !p.isEffectActive(PlayerStatusEffect.ENERGY_CHARGE)) {
					CheatTracker.get(gc).suspicious(CheatTracker.Infraction.POSSIBLE_PACKET_EDITING, "Tried to use stance without having buff cast");
					return;
				}
			}
		}

		if (diseaseSkill > 0)
			DiseaseTools.applyDebuff(p, diseaseSkill, diseaseLevel);

		if (damage == -1) {
			//TODO: no damage player skill
		}
		//TODO: handle the rest of the good player skill stuffs (achilles, magic guard, powerguard, etc)!

		if (!deadlyAttack) {
			int hpBurn = damage;
			PlayerStatusEffectValues mg = p.getEffectValue(PlayerStatusEffect.MAGIC_GUARD);
			if (mg != null) {
				int delta = damage * mg.getModifier() / 100;
				hpBurn -= delta;
				mpBurn += delta;
			}
			int mpOverage = mpBurn - p.getMp();
			if (mpOverage > 0) {
				mpBurn -= mpOverage;
				hpBurn += mpOverage;
			}
			p.gainHp(-hpBurn);
			if (mpBurn > 0)
				p.gainMp(-mpBurn);
			//TODO: morph dispel, battleship hurt...
		} else {
			p.setHp((short) 1);
			p.setMp((short) 1);
		}
		if (p.isVisible())
			p.getMap().sendToAll(writeHurtPlayer(p, attack, damage, pgmr, mobid, direction, stance, noDamageId), p);
	}

	public static void handlePuppetTakeDamage(LittleEndianReader packet, GameClient gc) {
		GameCharacter p = gc.getPlayer();
		int summonEntId = packet.readInt();
		byte misc = packet.readByte();
		int damage = packet.readInt();
		int mobEid = packet.readInt();
		packet.readByte();

		/*int skillId = p.getEffectValue(PlayerStatusEffect.PUPPET).getSource();
		PlayerSkillSummon puppet = p.getSummonBySkill(skillId);*/
		PlayerSkillSummon puppet = (PlayerSkillSummon) p.getMap().getEntityById(EntityType.SUMMON, summonEntId);
		int skillId = puppet.getSkillId();
		if (puppet.hurt(damage)) //died
			SkillTools.cancelBuffSkill(p, skillId);
		p.getMap().sendToAll(writeHurtPuppet(p, puppet, misc, damage, mobEid));
	}

	public static void handleMobDamageMob(LittleEndianReader packet, GameClient gc) {
		GameCharacter p = gc.getPlayer();
		int attackerEid = packet.readInt();
		/*int playerId = */packet.readInt();
		int attackedEid = packet.readInt();

		Mob attacker = (Mob) p.getMap().getEntityById(EntityType.MONSTER, attackerEid);
		Mob attacked = (Mob) p.getMap().getEntityById(EntityType.MONSTER, attackedEid);

		if (attacker != null && attacked != null) {
			//TODO: Fix formula
			int damage = attacker.getLevel() * Rng.getGenerator().nextInt(100) / 10;
			p.getMap().damageMonster(null, attacked, damage);
			if (p.getEvent() != null)
				p.getEvent().friendlyMobHurt(attacked, p.getMapId());
			p.getMap().sendToAll(writeHurtMonster(attacked, damage, true));
		}
	}

	private static byte[] writeHurtPuppet(GameCharacter p, PlayerSkillSummon puppet, byte misc, int damage, int mobEid) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter();
		lew.writeShort(ClientSendOps.DAMAGE_SUMMON);
		lew.writeInt(p.getId());
		lew.writeInt(puppet.getSkillId());
		lew.writeByte(misc);
		lew.writeInt(damage);
		lew.writeInt(mobEid);
		lew.writeByte((byte) 0);
		return lew.getBytes();
	}

	private static byte[] writeHurtPlayer(GameCharacter p, byte mobAttack, int damage, MobReturnDamage pgmr, int mobId, byte direction, byte stance, int noDamageSkill) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter();
		lew.writeShort(ClientSendOps.DAMAGE_PLAYER);
		lew.writeInt(p.getId());
		lew.writeByte(mobAttack);
		if (mobAttack != MAP_DAMAGE) {
			lew.writeInt(pgmr.getReduction() > 0 ? pgmr.getDamage() : damage);
			lew.writeInt(mobId);
			lew.writeByte(direction);
			lew.writeByte(pgmr.getReduction());
			if (pgmr.getReduction() > 0) {
				lew.writeBool(pgmr.isPhysical());
				lew.writeInt(pgmr.getEntityId());
				lew.writeByte(pgmr.getSkill());
				lew.writePos(pgmr.getPosition());
			}
			lew.writeByte(stance);
			lew.writeInt(damage);
			if (noDamageSkill > 0)
				lew.writeInt(noDamageSkill);
		} else { //repetitive much?
			lew.writeInt(damage);
			lew.writeInt(damage);
		}
		return lew.getBytes();
	}

	private static byte[] writeHurtMonster(Mob monster, int damage, boolean byMob) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(!byMob ? 11 : 19);
		lew.writeShort(ClientSendOps.DAMAGE_MONSTER);
		lew.writeInt(monster.getId());
		lew.writeBool(byMob);
		lew.writeInt(damage);
		if (byMob) {
			lew.writeInt(monster.getHp());
			lew.writeInt(monster.getMaxHp());
		}
		return lew.getBytes();
	}

	private static class MobReturnDamage {
		private byte reduction;
		private boolean physical;
		private int entity;
		private byte skill;
		private Point position;
		private int damage;

		public void setReduction(byte reduc) {
			reduction = reduc;
		}

		public void setPhysical(boolean isPhys) {
			physical = isPhys;
		}

		public void setEntityId(int eId) {
			entity = eId;
		}

		public void setSkill(byte s) {
			skill = s;
		}

		public void setPosition(Point pos) {
			position = pos;
		}

		public void setDamage(int dmg) {
			damage = dmg;
		}

		public byte getReduction() {
			return reduction;
		}

		public boolean isPhysical() {
			return physical;
		}

		public int getEntityId() {
			return entity;
		}

		public byte getSkill() {
			return skill;
		}

		public Point getPosition() {
			return position;
		}

		public int getDamage() {
			return damage;
		}
	}

	private TakeDamageHandler() {
		//uninstantiable...
	}
}
