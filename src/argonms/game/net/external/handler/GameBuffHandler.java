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

package argonms.game.net.external.handler;

import argonms.common.character.PlayerStatusEffect;
import argonms.common.character.Skills;
import argonms.common.character.inventory.Inventory.InventoryType;
import argonms.common.character.inventory.InventorySlot;
import argonms.common.character.inventory.InventoryTools;
import argonms.common.net.external.ClientSendOps;
import argonms.common.tools.input.LittleEndianReader;
import argonms.common.tools.output.LittleEndianByteArrayWriter;
import argonms.game.character.GameCharacter;
import argonms.game.character.PlayerStatusEffectValues;
import argonms.game.character.SkillTools;
import argonms.game.character.inventory.ItemTools;
import argonms.game.field.MapEntity.EntityType;
import argonms.game.field.entity.Mob;
import argonms.game.field.entity.PlayerSkillSummon;
import argonms.game.loading.skill.SkillDataLoader;
import argonms.game.loading.skill.SkillStats;
import argonms.game.net.external.GameClient;
import argonms.game.net.external.GamePackets;
import java.awt.Point;

/**
 *
 * @author GoldenKevin
 */
public class GameBuffHandler {
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
		//TODO: hacking if item's id at slot does not match itemId
		InventorySlot changed = InventoryTools.takeFromInventory(p.getInventory(InventoryType.USE), slot, (short) 1);
		if (changed != null)
			gc.getSession().send(GamePackets.writeInventorySlotUpdate(InventoryType.USE, slot, changed));
		else
			gc.getSession().send(GamePackets.writeInventoryClearSlot(InventoryType.USE, slot));
		ItemTools.useItem(p, itemId);
		p.itemCountChanged(itemId);
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
}
