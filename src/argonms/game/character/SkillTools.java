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

package argonms.game.character;

import argonms.common.character.inventory.Inventory;
import argonms.common.character.inventory.Inventory.InventoryType;
import argonms.common.character.inventory.InventorySlot;
import argonms.common.character.inventory.InventoryTools;
import argonms.common.character.inventory.InventoryTools.UpdatedSlots;
import argonms.common.character.inventory.InventoryTools.WeaponClass;
import argonms.common.net.external.CheatTracker;
import argonms.common.net.external.ClientSession;
import argonms.common.net.external.CommonPackets;
import argonms.common.util.Scheduler;
import argonms.game.loading.skill.PlayerSkillEffectsData;
import argonms.game.loading.skill.SkillDataLoader;
import argonms.game.net.external.GamePackets;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 *
 * @author GoldenKevin
 */
public class SkillTools {
	//TODO: IMPLEMENT HP/MP stuff for alchemist, heal, chakra
	private static Map<ClientUpdateKey, Number> skillCastCosts(GameCharacter p, PlayerSkillEffectsData e) {
		//might as well save ourself some bandwidth and don't send an individual
		//packet for each changed stat
		Map<ClientUpdateKey, Number> ret = new EnumMap<ClientUpdateKey, Number>(ClientUpdateKey.class);
		if (e.getMpConsume() > 0) {
			p.setLocalMp((short) (p.getMp() - e.getMpConsume()));
			ret.put(ClientUpdateKey.MP, Short.valueOf(p.getMp()));
		}
		if (e.getHpConsume() > 0) {
			p.setLocalHp((short) (p.getHp() - e.getHpConsume()));
			ret.put(ClientUpdateKey.HP, Short.valueOf(p.getHp()));
		}
		if (e.getMoneyConsume() > 0) {
			p.setLocalMesos(e.getMoneyConsume());
			ret.put(ClientUpdateKey.MESO, Integer.valueOf(p.getMesos()));
		}
		int itemId = e.getItemConsume();
		short quantity = e.getItemConsumeCount();
		if (itemId != 0) {
			ClientSession<?> ses = p.getClient().getSession();
			InventoryType type = InventoryTools.getCategory(itemId);
			Inventory inv = p.getInventory(InventoryTools.getCategory(itemId));
			UpdatedSlots changedSlots = InventoryTools.removeFromInventory(inv, itemId, quantity);
			short pos;
			for (Short s : changedSlots.modifiedSlots) {
				pos = s.shortValue();
				ses.send(GamePackets.writeInventoryUpdateSlotQuantity(type, pos, inv.get(pos)));
			}
			for (Short s : changedSlots.addedOrRemovedSlots) {
				pos = s.shortValue();
				ses.send(GamePackets.writeInventoryClearSlot(type, pos));
			}
			p.itemCountChanged(itemId);
		}
		if (e.getCooltime() > 0) {
			p.getClient().getSession().send(CommonPackets.writeCooldown(e.getDataId(), e.getCooltime()));
			p.addCooldown(e.getDataId(), e.getCooltime());
		}
		if (e.getDuration() > 0)
			buffSpecificCosts(p, e);
		return ret;
	}

	private static void buffSpecificCosts(GameCharacter p, PlayerSkillEffectsData e) {
		//for attack skills, ranged ammo (throwing stars, arrows, bullets)
		//are removed in DealDamageHandler.handleRangedAttack, so don't
		//worry about them here. only do buffs
		int itemId;
		short quantity = e.getBulletConsume();
		if (quantity == 0)
			quantity = e.getBulletCount();
		if (quantity != 0) { //buff skill uses bullets
			int ammoPrefix;
			switch (WeaponClass.getForPlayer(p)) {
				case BOW:
					ammoPrefix = 2060;
					break;
				case CROSSBOW:
					ammoPrefix = 2061;
					break;
				case CLAW:
					ammoPrefix = 2070;
					break;
				case GUN:
					ammoPrefix = 2330;
					break;
				default:
					CheatTracker.get(p.getClient()).suspicious(CheatTracker.Infraction.PACKET_EDITING, "Tried to use ranged buff skill without ranged weapon");
					return;
			}
			InventorySlot slot;

			//unlike attack skills, buff skills that use bullets can be cast
			//even if one slot does not have the necessary quantity. as long
			//as there is enough of one particular type of ammo (no matter
			//how many slots it is spread across), then the client allows
			//the skill. so we have to do some fancy stuff.
			Map<Integer, Short> canUse = new HashMap<Integer, Short>();
			int removeItemId = 0;
			Inventory inv = p.getInventory(InventoryType.USE);
			for (Entry<Short, InventorySlot> entry : inv.getAll().entrySet()) {
				slot = entry.getValue();
				itemId = slot.getDataId();
				if ((itemId / 1000) == ammoPrefix) {
					Short amount = canUse.get(Integer.valueOf(itemId));
					if (amount == null)
						amount = Short.valueOf(slot.getQuantity());
					else
						amount = Short.valueOf((short) (amount.shortValue() + slot.getQuantity()));
					canUse.put(Integer.valueOf(itemId), amount);
					if (amount.shortValue() >= quantity) {
						removeItemId = itemId;
						break;
					}
				}
			}
			if (removeItemId != 0) {
				short pos;
				ClientSession<?> ses = p.getClient().getSession();
				UpdatedSlots changedSlots = InventoryTools.removeFromInventory(inv, removeItemId, quantity);
				for (Short s : changedSlots.modifiedSlots) {
					pos = s.shortValue();
					ses.send(GamePackets.writeInventoryUpdateSlotQuantity(InventoryType.USE, pos, inv.get(pos)));
				}
				for (Short s : changedSlots.addedOrRemovedSlots) {
					pos = s.shortValue();
					ses.send(GamePackets.writeInventoryClearSlot(InventoryType.USE, pos));
				}
				p.itemCountChanged(removeItemId);
			} else {
				CheatTracker.get(p.getClient()).suspicious(CheatTracker.Infraction.PACKET_EDITING, "Tried to use buff skill without meeting the cast costs");
			}
		}
	}

	/**
	 * Cast a buff skill of the specified skill id with the specified skill
	 * level.
	 * @param p the Player that will cast the skill
	 * @param skillId the identifier of the skill to use
	 * @param skillLevel the amount of skill points the Player has in the skill
	 */
	public static void useCastSkill(final GameCharacter p, final int skillId, final byte skillLevel, byte stance) {
		PlayerSkillEffectsData e = SkillDataLoader.getInstance().getSkill(skillId).getLevel(skillLevel);
		p.getClient().getSession().send(GamePackets.writeUpdatePlayerStats(skillCastCosts(p, e), true));
		StatusEffectTools.applyEffectsAndShowVisuals(p, e, stance);
		if (e.getDuration() > 0) {
			p.addCancelEffectTask(e, Scheduler.getInstance().runAfterDelay(new Runnable() {
				@Override
				public void run() {
					cancelBuffSkill(p, skillId, skillLevel);
				}
			}, e.getDuration()), skillLevel, System.currentTimeMillis() + e.getDuration());
		}
	}

	/**
	 * Only recognize the stat increases of a buff skill on the server. Does not
	 * send any notifications to the client or any other players in the same map
	 * upon the use of a skill. Useful for resuming a skill when a player has
	 * changed channels because we have to recognize stat increases locally.
	 * @param p the Player that casted the skill
	 * @param skillId the identifier of the skill that was used
	 * @param skillLevel the amount of skill points the Player had in the skill
	 * @param remainingTime the amount of time left before the skill expires
	 */
	public static void localUseBuffSkill(final GameCharacter p, final int skillId, final byte skillLevel, long endTime) {
		PlayerSkillEffectsData e = SkillDataLoader.getInstance().getSkill(skillId).getLevel(skillLevel);
		StatusEffectTools.applyEffects(p, e);
		p.addCancelEffectTask(e, Scheduler.getInstance().runAfterDelay(new Runnable() {
			@Override
			public void run() {
				cancelBuffSkill(p, skillId, skillLevel);
			}
		}, endTime - System.currentTimeMillis()), skillLevel, endTime);
	}

	/**
	 * Cast a buff skill of the specified skill id using the Player's current
	 * level of that particular skill.
	 * @param p the Player that will cast the skill
	 * @param skillId the identifier of the skill to use
	 * @return true if the Player has at least one skill point in the given
	 * skill, false if the Player has no skill points in that skill and thus
	 * could not cast it.
	 */
	public static boolean useCastSkill(GameCharacter p, int skillId) {
		byte skillLevel = p.getSkillLevel(skillId);
		if (skillLevel != 0) {
			useCastSkill(p, skillId, skillLevel, (byte) -1);
			return true;
		}
		return false;
	}

	private static void cancelBuffSkill(GameCharacter p, int skillId, byte skillLevel) {
		PlayerSkillEffectsData e = SkillDataLoader.getInstance().getSkill(skillId).getLevel(skillLevel);
		StatusEffectTools.dispelEffectsAndShowVisuals(p, e);
	}

	public static void cancelBuffSkill(GameCharacter p, int skillId) {
		//even if we cast the skill at an earlier level, the individual effects
		//for each level should be the same. That's all we need to call
		//Player.dispelEffect...
		byte skillLevel = p.getSkillLevel(skillId);
		if (skillLevel <= 0) //casted a skill if we don't have any levels in it
			skillLevel = 1; //it happens!
		cancelBuffSkill(p, skillId, skillLevel);
	}

	public static void useAttackSkill(GameCharacter p, int skillId, byte skillLevel) {
		PlayerSkillEffectsData e = SkillDataLoader.getInstance().getSkill(skillId).getLevel(skillLevel);
		p.getClient().getSession().send(GamePackets.writeUpdatePlayerStats(skillCastCosts(p, e), false));
	}
}
