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

package argonms.game.character.inventory;

import argonms.common.GlobalConstants;
import argonms.common.character.PlayerStatusEffect;
import argonms.common.character.Skills;
import argonms.common.character.inventory.InventoryTools;
import argonms.common.loading.item.ItemDataLoader;
import argonms.common.loading.item.ItemEffectsData;
import argonms.common.util.Scheduler;
import argonms.game.character.ClientUpdateKey;
import argonms.game.character.DiseaseTools;
import argonms.game.character.GameCharacter;
import argonms.game.character.PlayerStatusEffectValues;
import argonms.game.character.StatusEffectTools;
import argonms.game.loading.skill.SkillDataLoader;
import argonms.game.net.external.GamePackets;
import java.util.EnumMap;
import java.util.Map;

/**
 *
 * @author GoldenKevin
 */
public final class ItemTools {
	public static short getPersonalSlotMax(GameCharacter p, int itemid) {
		short max = ItemDataLoader.getInstance().getSlotMax(Integer.valueOf(itemid));
		int skillId;
		byte level;
		if (InventoryTools.isThrowingStar(itemid) && (level = p.getSkillLevel(skillId = Skills.CLAW_MASTERY)) > 0
				|| InventoryTools.isBullet(itemid) && (level = p.getSkillLevel(skillId = Skills.GUN_MASTERY)) > 0)
			max += SkillDataLoader.getInstance().getSkill(skillId).getLevel(level).getY();
		return max;
	}

	private static Map<ClientUpdateKey, Number> itemRecovers(GameCharacter p, ItemEffectsData e) {
		//might as well save ourself some bandwidth and don't send an individual
		//packet for each changed stat
		Map<ClientUpdateKey, Number> ret = new EnumMap<ClientUpdateKey, Number>(ClientUpdateKey.class);
		if (e.getHpRecover() != 0) {
			p.setLocalHp((short) Math.min(p.getHp() + e.getHpRecover(), p.getCurrentMaxHp()));
			ret.put(ClientUpdateKey.HP, Short.valueOf(p.getHp()));
		}
		if (e.getHpRecoverPercent() != 0) {
			short maxHp = p.getCurrentMaxHp();
			int hpGain = Math.round(e.getHpRecoverPercent() * maxHp / 100f);
			p.setLocalHp((short) Math.min(p.getHp() + hpGain, maxHp));
			ret.put(ClientUpdateKey.HP, Short.valueOf(p.getHp()));
		}
		if (e.getMpRecover() != 0) {
			p.setLocalMp((short) Math.min(p.getMp() + e.getMpRecover(), p.getCurrentMaxMp()));
			ret.put(ClientUpdateKey.MP, Short.valueOf(p.getMp()));
		}
		if (e.getMpRecoverPercent() != 0) {
			short maxMp = p.getCurrentMaxMp();
			int mpGain = Math.round(e.getMpRecoverPercent() * maxMp / 100f);
			p.setLocalMp((short) Math.min(p.getMp() + mpGain, maxMp));
			ret.put(ClientUpdateKey.MP, Short.valueOf(p.getMp()));
		}
		if (e.curesCurse()) {
			PlayerStatusEffectValues v = p.getEffectValue(PlayerStatusEffect.CURSE);
			if (v != null)
				DiseaseTools.cancelDebuff(p, (short) v.getSource(), v.getLevelWhenCast());
		}
		if (e.curesDarkness()) {
			PlayerStatusEffectValues v = p.getEffectValue(PlayerStatusEffect.DARKNESS);
			if (v != null)
				DiseaseTools.cancelDebuff(p, (short) v.getSource(), v.getLevelWhenCast());
		}
		if (e.curesPoison()) {
			PlayerStatusEffectValues v = p.getEffectValue(PlayerStatusEffect.POISON);
			if (v != null)
				DiseaseTools.cancelDebuff(p, (short) v.getSource(), v.getLevelWhenCast());
		}
		if (e.curesSeal()) {
			PlayerStatusEffectValues v = p.getEffectValue(PlayerStatusEffect.SEAL);
			if (v != null)
				DiseaseTools.cancelDebuff(p, (short) v.getSource(), v.getLevelWhenCast());
		}
		if (e.curesWeakness()) {
			PlayerStatusEffectValues v = p.getEffectValue(PlayerStatusEffect.WEAKEN);
			if (v != null)
				DiseaseTools.cancelDebuff(p, (short) v.getSource(), v.getLevelWhenCast());
		}
		if (e.getMoveTo() != 0) {
			if (e.getMoveTo() == GlobalConstants.NULL_MAP)
				p.changeMap(p.getMap().getReturnMap());
			else
				p.changeMap(e.getMoveTo());
		}
		return ret;
	}

	/**
	 * Consume a item of the specified item id.
	 * @param p the Player that will consume the item
	 * @param itemId the identifier of the item to use
	 */
	public static void useItem(final GameCharacter p, final int itemId) {
		ItemEffectsData e = ItemDataLoader.getInstance().getEffect(itemId);
		Map<ClientUpdateKey, Number> statChanges = itemRecovers(p, e);
		if (!statChanges.isEmpty())
			p.getClient().getSession().send(GamePackets.writeUpdatePlayerStats(statChanges, false));
		if (e.getDuration() > 0) { //buff item
			StatusEffectTools.applyEffectsAndShowVisuals(p, e, (byte) -1);
			p.addCancelEffectTask(e, Scheduler.getInstance().runAfterDelay(new Runnable() {
				@Override
				public void run() {
					cancelBuffItem(p, itemId);
				}
			}, e.getDuration()), (byte) 0, System.currentTimeMillis() + e.getDuration());
		}
	}

	/**
	 * Only recognize the stat increases of a buff item on the server. Does not
	 * send any notifications to the client or any other players in the same map
	 * upon the use of a item. Useful for resuming a buff item when a player has
	 * changed channels because we have to recognize stat increases locally.
	 * @param p the Player that consumed the item
	 * @param itemId the identifier of the item that was used
	 * @param remainingTime the amount of time left before the item expires
	 */
	public static void localUseBuffItem(final GameCharacter p, final int itemId, long endTime) {
		ItemEffectsData e = ItemDataLoader.getInstance().getEffect(itemId);
		StatusEffectTools.applyEffects(p, e);
		p.addCancelEffectTask(e, Scheduler.getInstance().runAfterDelay(new Runnable() {
			@Override
			public void run() {
				cancelBuffItem(p, itemId);
			}
		}, endTime - System.currentTimeMillis()), (byte) 0, endTime);
	}

	public static void cancelBuffItem(GameCharacter p, int itemId) {
		ItemEffectsData e = ItemDataLoader.getInstance().getEffect(itemId);
		StatusEffectTools.dispelEffectsAndShowVisuals(p, e);
	}

	private ItemTools() {
		//uninstantiable...
	}
}
