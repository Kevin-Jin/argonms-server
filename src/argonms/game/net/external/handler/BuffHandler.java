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

package argonms.game.net.external.handler;

import argonms.common.character.PlayerStatusEffect;
import argonms.common.character.Skills;
import argonms.common.character.inventory.Inventory;
import argonms.common.character.inventory.Inventory.InventoryType;
import argonms.common.character.inventory.InventorySlot;
import argonms.common.character.inventory.InventoryTools;
import argonms.common.net.external.CheatTracker;
import argonms.common.net.external.ClientSendOps;
import argonms.common.util.input.LittleEndianReader;
import argonms.common.util.output.LittleEndianByteArrayWriter;
import argonms.game.character.GameCharacter;
import argonms.game.character.PartyList;
import argonms.game.character.PlayerStatusEffectValues;
import argonms.game.character.SkillTools;
import argonms.game.character.inventory.ItemTools;
import argonms.game.field.MapEntity.EntityType;
import argonms.game.field.MonsterStatusEffectTools;
import argonms.game.field.entity.Mob;
import argonms.game.field.entity.PlayerSkillSummon;
import argonms.game.loading.skill.PlayerSkillEffectsData;
import argonms.game.loading.skill.SkillDataLoader;
import argonms.game.loading.skill.SkillStats;
import argonms.game.net.external.GameClient;
import argonms.game.net.external.GamePackets;
import java.awt.Point;

/**
 *
 * @author GoldenKevin
 */
public final class BuffHandler {
	private static boolean isAffected(byte bitset, byte index) {
		return ((bitset & (1 << (5 - index))) != 0);
	}

	public static void handleUseSkill(LittleEndianReader packet, GameClient gc) {
		GameCharacter p = gc.getPlayer();
		/*int tickCount = */packet.readInt();
		int skillId = packet.readInt();
		byte skillLevel = packet.readByte();
		byte stance = -1;
		switch (skillId) {
			case Skills.HERO_MONSTER_MAGNET:
			case Skills.PALADIN_MONSTER_MAGNET:
			case Skills.DARK_KNIGHT_MONSTER_MAGNET: {
				int amount = packet.readInt();
				for (int i = 0; i < amount; i++) {
					int mobEntId = packet.readInt();
					byte success = packet.readByte();
					p.getMap().sendToAll(writeMonsterMagnetSuccess(mobEntId, success), p);
					Mob m = (Mob) p.getMap().getEntityById(EntityType.MONSTER, mobEntId);
					if (m != null) {
						GameCharacter controller = m.getController();
						if (controller != null) {
							controller.uncontrolMonster(m);
							controller.getClient().getSession().send(GamePackets.writeStopControlMonster(m));
						}
						m.setController(p);
						p.controlMonster(m);
						p.getClient().getSession().send(GamePackets.writeShowAndControlMonster(m, m.controllerHasAggro()));
						m.setControllerKnowsAboutAggro(false);
					}
				}
				stance = packet.readByte();
				break;
			}
			case Skills.HEAL:
				//TODO: implement cleric heal
				break;
			case Skills.RAGE:
			case Skills.IRON_WILL:
			case Skills.SPEARMAN_HYPER_BODY:
			case Skills.FP_MEDITATION:
			case Skills.IL_MEDITATION:
			case Skills.CLERIC_BLESS:
			case Skills.CLERIC_HOLY_SYMBOL:
			case Skills.HOLY_SHIELD:
			case Skills.XBOW_MASTER_SHARP_EYES:
			case Skills.BOW_MASTER_SHARP_EYES:
			case Skills.SIN_HASTE:
			case Skills.MESO_UP:
			case Skills.DIT_HASTE:
			case Skills.SPEED_INFUSION:
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
			case Skills.CORSAIR_MAPLE_WARRIOR: {
				PartyList party = p.getParty();
				byte affected = packet.readByte();
				if (party != null) {
					PlayerSkillEffectsData e = SkillDataLoader.getInstance().getSkill(skillId).getLevel(skillLevel);
					party.lockRead();
					try {
						PartyList.Member[] members = party.getAllMembers();
						byte partySize = party.getMembersCount();
						for (byte i = 0; i < partySize; i++) {
							if (isAffected(affected, i)) {
								GameCharacter memberPlayer = ((PartyList.LocalMember) members[i]).getPlayer();
								if (p != memberPlayer && e.makeChanceResult())
									SkillTools.applyAoeBuff(memberPlayer, e);
							}
						}
					} finally {
						party.unlockRead();
					}
				} //else assert (affected & 0xFF) == 0b10000000;
				break;
			}
			case Skills.TIME_LEAP: {
				PlayerSkillEffectsData e = SkillDataLoader.getInstance().getSkill(skillId).getLevel(skillLevel);
				PartyList party = p.getParty();
				byte affected = packet.readByte();
				if (party != null) {
					party.lockRead();
					try {
						PartyList.Member[] members = party.getAllMembers();
						byte partySize = party.getMembersCount();
						for (byte i = 0; i < partySize; i++) {
							if (isAffected(affected, i)) {
								GameCharacter memberPlayer = ((PartyList.LocalMember) members[i]).getPlayer();
								if (p != memberPlayer && e.makeChanceResult())
									SkillTools.applyTimeLeap(memberPlayer, false, e);
							}
						}
					} finally {
						party.unlockRead();
					}
				} //else assert (affected & 0xFF) == 0b10000000;
				SkillTools.applyTimeLeap(p, true, e);
				break;
			}
			case Skills.BISHOP_RESURRECTION: {
				PartyList party = p.getParty();
				byte affected = packet.readByte();
				if (party != null) {
					PlayerSkillEffectsData e = SkillDataLoader.getInstance().getSkill(skillId).getLevel(skillLevel);
					party.lockRead();
					try {
						PartyList.Member[] members = party.getAllMembers();
						byte partySize = party.getMembersCount();
						for (byte i = 0; i < partySize; i++) {
							if (isAffected(affected, i)) {
								GameCharacter memberPlayer = ((PartyList.LocalMember) members[i]).getPlayer();
								if (p != memberPlayer && e.makeChanceResult())
									SkillTools.applyResurrection(memberPlayer, e);
							}
						}
					} finally {
						party.unlockRead();
					}
				} //else assert (affected & 0xFF) == 0b10000000;
				//can't apply resurrect to self, so don't bother
				break;
			}
			case Skills.DISPEL: {
				PlayerSkillEffectsData e = SkillDataLoader.getInstance().getSkill(skillId).getLevel(skillLevel);
				PartyList party = p.getParty();
				byte affected = packet.readByte();
				if (party != null) {
					party.lockRead();
					try {
						PartyList.Member[] members = party.getAllMembers();
						byte partySize = party.getMembersCount();
						for (byte i = 0; i < partySize; i++) {
							if (isAffected(affected, i)) {
								GameCharacter memberPlayer = ((PartyList.LocalMember) members[i]).getPlayer();
								if (p != memberPlayer && e.makeChanceResult())
									SkillTools.applyDispel(memberPlayer, false, e);
							}
						}
					} finally {
						party.unlockRead();
					}
				} //else assert (affected & 0xFF) == 0b10000000;
				SkillTools.applyDispel(p, true, e);

				packet.readShort();
				byte count = packet.readByte();
				for (int i = 0; i < count; i++) {
					int mobId = packet.readInt();
					Mob m = (Mob) p.getMap().getEntityById(EntityType.MONSTER, mobId);
					if (m != null && e.makeChanceResult())
						MonsterStatusEffectTools.applyDispel(m, e);
				}
				break;
			}
			case Skills.ECHO_OF_HERO:
			case Skills.GM_HASTE:
			case Skills.GM_HOLY_SYMBOL:
			case Skills.GM_BLESS:
			case Skills.GM_HYPER_BODY: {
				PlayerSkillEffectsData e = SkillDataLoader.getInstance().getSkill(skillId).getLevel(skillLevel);
				byte count = packet.readByte();
				for (int i = 0; i < count; i++) {
					int playerId = packet.readInt();
					GameCharacter target = (GameCharacter) p.getMap().getEntityById(EntityType.PLAYER, playerId);
					if (p != target && target != null)
						SkillTools.applyAoeBuff(target, e);
				}
				break;
			}
			case Skills.HEAL_AND_DISPEL: {
				PlayerSkillEffectsData e = SkillDataLoader.getInstance().getSkill(skillId).getLevel(skillLevel);
				byte count = packet.readByte();
				for (int i = 0; i < count; i++) {
					int playerId = packet.readInt();
					GameCharacter target = (GameCharacter) p.getMap().getEntityById(EntityType.PLAYER, playerId);
					if (target != null)
						SkillTools.applyHealAndDispel(target, p == target, e);
				}
				break;
			}
			case Skills.GM_RESURRECTION: {
				PlayerSkillEffectsData e = SkillDataLoader.getInstance().getSkill(skillId).getLevel(skillLevel);
				byte count = packet.readByte();
				for (int i = 0; i < count; i++) {
					int playerId = packet.readInt();
					GameCharacter target = (GameCharacter) p.getMap().getEntityById(EntityType.PLAYER, playerId);
					//assert p != target;
					if (target != null)
						SkillTools.applyGmResurrection(target, e);
				}
				break;
			}
			default:
				SkillStats skill = SkillDataLoader.getInstance().getSkill(skillId);
				if (skill.isSummon()) {
					Point summonPos = packet.readPos();
					stance = packet.readByte();
					PlayerStatusEffectValues v;
					switch (skillId) {
						case Skills.BOW_PUPPET:
						case Skills.XBOW_PUPPET:
							v = p.getEffectValue(PlayerStatusEffect.PUPPET);
							break;
						default:
							v = p.getEffectValue(PlayerStatusEffect.SUMMON);
							break;
					}
					if (v != null) //we can only keep track of one PUPPET and one SUMMON at once =/
						SkillTools.cancelBuffSkill(p, v.getSource());
					PlayerSkillSummon summon = new PlayerSkillSummon(p, skill.getLevel(skillLevel), summonPos, stance);
					p.addToSummons(skillId, summon);
					p.getMap().spawnEntity(summon);
				}
				break;
		}
		SkillTools.useCastSkill(p, skillId, skillLevel, stance);
	}

	public static void handleUseItem(LittleEndianReader packet, GameClient gc) {
		GameCharacter p = gc.getPlayer();
		/*int tickCount = */packet.readInt();
		short slot = packet.readShort();
		int itemId = packet.readInt();
		Inventory inv = p.getInventory(InventoryType.USE);
		InventorySlot changed = inv.get(slot);
		if (changed == null || changed.getDataId() != itemId || changed.getQuantity() < 1) {
			CheatTracker.get(gc).suspicious(CheatTracker.Infraction.PACKET_EDITING, "Tried to use nonexistant consume item");
			return;
		}
		changed = InventoryTools.takeFromInventory(inv, slot, (short) 1);
		if (changed != null)
			gc.getSession().send(GamePackets.writeInventoryUpdateSlotQuantity(InventoryType.USE, slot, changed));
		else
			gc.getSession().send(GamePackets.writeInventoryClearSlot(InventoryType.USE, slot));
		p.itemCountChanged(itemId);
		ItemTools.useItem(p, itemId);
	}

	public static void handleCancelSkill(LittleEndianReader packet, GameClient gc) {
		GameCharacter p = gc.getPlayer();
		int skillId = packet.readInt();
		//method name is kind of a misnomer. this handles buff cancels and
		//skills with a keydownend (only Hurricane and Rapid Fire at the moment)
		if (!SkillDataLoader.getInstance().getSkill(skillId).isKeydownEnd())
			SkillTools.cancelBuffSkill(p, skillId);
		else
			p.getMap().sendToAll(writeEndKeydown(p, skillId), p);
	}

	public static void handleCancelItem(LittleEndianReader packet, GameClient gc) {
		GameCharacter p = gc.getPlayer();
		int itemId = -packet.readInt();
		ItemTools.cancelBuffItem(p, itemId);
	}

	private static byte[] writeEndKeydown(GameCharacter p, int skillId) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(10);
		lew.writeShort(ClientSendOps.END_KEY_DOWN);
		lew.writeInt(p.getId());
		lew.writeInt(skillId);
		return lew.getBytes();
	}

	private static byte[] writeMonsterMagnetSuccess(int mobId, byte success) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(7);
		lew.writeShort(ClientSendOps.MONSTER_DRAGGED);
		lew.writeInt(mobId);
		lew.writeByte(success);
		return lew.getBytes();
	}

	private BuffHandler() {
		//uninstantiable...
	}
}
