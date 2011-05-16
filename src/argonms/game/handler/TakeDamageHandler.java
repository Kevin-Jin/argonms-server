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

import argonms.character.DiseaseTools;
import argonms.character.Player;
import argonms.character.skill.PlayerStatusEffectValues;
import argonms.character.skill.PlayerStatusEffectValues.PlayerStatusEffect;
import argonms.character.skill.SkillTools;
import argonms.game.GameClient;
import argonms.loading.mob.Attack;
import argonms.loading.mob.MobDataLoader;
import argonms.map.MapEntity.EntityType;
import argonms.map.entity.Mob;
import argonms.map.entity.PlayerSkillSummon;
import argonms.net.external.ClientSendOps;
import argonms.net.external.RemoteClient;
import argonms.tools.input.LittleEndianReader;
import argonms.tools.output.LittleEndianByteArrayWriter;
import java.awt.Point;

/**
 *
 * @author GoldenKevin
 */
public class TakeDamageHandler {
	private static final byte
		BUMP_DAMAGE = -1, //the kind of damage you take when you run into a mob
		MAP_DAMAGE = -2 //e.g. vines b/w henesys and ellinia
	;

	public static void handleTakeDamage(LittleEndianReader packet, RemoteClient rc) {
		Player p = ((GameClient) rc).getPlayer();

		/*int tickCount = */packet.readInt();
		byte attack = packet.readByte();
		/*byte elem = */packet.readByte();
		int damage = packet.readInt();
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
					//TODO: hacking
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
					m.hurt(p, hurtDmg);
				}
				stance = packet.readByte();
				if (stance > 0 && !p.isEffectActive(PlayerStatusEffect.STANCE)) {
					//TODO: hacking
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
			PlayerStatusEffectValues mg = p.getEffectValue(PlayerStatusEffect.MAGIC_GUARD);
			if (mg != null) {
				int delta = damage * mg.getModifier() / 100;
				damage -= delta;
				mpBurn += delta;
			}
			p.gainHp(-damage);
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

	public static void handlePuppetTakeDamage(LittleEndianReader packet, RemoteClient rc) {
		Player p = ((GameClient) rc).getPlayer();
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
		p.getMap().sendToAll(writeHurtPuppet(p, puppet, misc, damage, mobEid), puppet.getPosition(), null);
	}

	private static byte[] writeHurtPuppet(Player p, PlayerSkillSummon puppet, byte misc, int damage, int mobEid) {
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

	private static byte[] writeHurtPlayer(Player p, byte mobAttack, int damage, MobReturnDamage pgmr, int mobId, byte direction, byte stance, int noDamageSkill) {
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
}
