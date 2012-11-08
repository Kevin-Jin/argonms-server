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

package argonms.game.command;

import argonms.common.GlobalConstants;
import argonms.common.character.inventory.Inventory;
import argonms.common.character.inventory.InventoryTools;
import argonms.common.net.external.ClientSession;
import argonms.game.GameServer;
import argonms.game.character.ExpTables;
import argonms.game.character.GameCharacter;
import argonms.game.net.external.GamePackets;
import java.util.Map;

/**
 *
 * @author GoldenKevin
 */
public class LocalChannelCommandTarget implements CommandTarget {
	private final GameCharacter target;

	public LocalChannelCommandTarget(GameCharacter backing) {
		target = backing;
	}

	private int getInt(Map.Entry<CharacterManipulation, ?> update) {
		return ((Integer) update.getValue()).intValue();
	}

	private short getShort(Map.Entry<CharacterManipulation, ?> update) {
		return ((Short) update.getValue()).shortValue();
	}

	@Override
	public void mutate(Map<CharacterManipulation, ?> updates) {
		for (Map.Entry<CharacterManipulation, ?> update : updates.entrySet()) {
			switch (update.getKey()) {
				case CHANGE_MAP: {
					MapValue value = (MapValue) update.getValue();
					target.changeMap(value.mapId, value.spawnPoint);
					break;
				}
				case CHANGE_CHANNEL:
					GameServer.getChannel(target.getClient().getChannel()).performChannelChange(target.getDataId(), ((Byte) update.getValue()).byteValue());
					break;
				case ADD_LEVEL:
					target.setLevel((short) Math.min(target.getLevel() + getShort(update), GlobalConstants.MAX_LEVEL));
					if (target.getLevel() < GlobalConstants.MAX_LEVEL)
						target.setExp(Math.min(target.getExp(), ExpTables.getForLevel(target.getLevel()) - 1));
					else
						target.setExp(0);
					break;
				case SET_LEVEL:
					target.setLevel(getShort(update));
					if (target.getLevel() < GlobalConstants.MAX_LEVEL)
						target.setExp(Math.min(target.getExp(), ExpTables.getForLevel(target.getLevel()) - 1));
					else
						target.setExp(0);
					break;
				case SET_JOB:
					target.setJob(getShort(update));
					break;
				case ADD_STR:
					target.setStr((short) Math.min(target.getStr() + getShort(update), Short.MAX_VALUE));
					break;
				case SET_STR:
					target.setStr(getShort(update));
					break;
				case ADD_DEX:
					target.setDex((short) Math.min(target.getDex() + getShort(update), Short.MAX_VALUE));
					break;
				case SET_DEX:
					target.setDex(getShort(update));
					break;
				case ADD_INT:
					target.setInt((short) Math.min(target.getInt() + getShort(update), Short.MAX_VALUE));
					break;
				case SET_INT:
					target.setInt(getShort(update));
					break;
				case ADD_LUK:
					target.setLuk((short) Math.min(target.getLuk() + getShort(update), Short.MAX_VALUE));
					break;
				case SET_LUK:
					target.setLuk(getShort(update));
					break;
				case ADD_AP:
					target.setAp((short) Math.min(target.getAp() + getShort(update), Short.MAX_VALUE));
					break;
				case SET_AP:
					target.setAp(getShort(update));
					break;
				case ADD_SP:
					target.setSp((short) Math.min(target.getSp() + getShort(update), Short.MAX_VALUE));
					break;
				case SET_SP:
					target.setSp(getShort(update));
					break;
				case ADD_MAX_HP:
					target.setMaxHp((short) Math.min(target.getMaxHp() + getShort(update), 30000));
					break;
				case SET_MAX_HP:
					target.setMaxHp(getShort(update));
					break;
				case ADD_MAX_MP:
					target.setMaxMp((short) Math.min(target.getMaxMp() + getShort(update), 30000));
					break;
				case SET_MAX_MP:
					target.setMaxMp(getShort(update));
					break;
				case ADD_HP:
					target.setHp((short) Math.min(target.getHp() + getShort(update), target.getCurrentMaxHp()));
					break;
				case SET_HP:
					target.setHp((short) Math.min(getShort(update), target.getCurrentMaxHp()));
					break;
				case ADD_MP:
					target.setMp((short) Math.min(target.getMp() + getShort(update), target.getCurrentMaxMp()));
					break;
				case SET_MP:
					target.setMp((short) Math.min(getShort(update), target.getCurrentMaxMp()));
					break;
				case ADD_FAME:
					target.setFame((short) Math.min(target.getFame() + getShort(update), Short.MAX_VALUE));
					break;
				case SET_FAME:
					target.setFame(getShort(update));
					break;
				case ADD_EXP:
					if (target.getLevel() < GlobalConstants.MAX_LEVEL)
						target.setExp(Math.min((int) Math.min((long) target.getExp() + getInt(update), Integer.MAX_VALUE), ExpTables.getForLevel(target.getLevel()) - 1));
					else if (target.getExp() + getInt(update) == 0)
						target.setExp(0);
					break;
				case SET_EXP:
					if (target.getLevel() < GlobalConstants.MAX_LEVEL)
						target.setExp(Math.min(getInt(update), ExpTables.getForLevel(target.getLevel()) - 1));
					else if (getInt(update) == 0)
						target.setExp(0);
					break;
				case ADD_MESO:
					target.setMesos((int) Math.min((long) target.getMesos() + getInt(update), Integer.MAX_VALUE));
					break;
				case SET_MESO:
					target.setMesos(getInt(update));
					break;
				case SET_SKILL_LEVEL: {
					SkillValue value = (SkillValue) update.getValue();
					target.setSkillLevel(value.skillId, value.skillLevel, value.skillMasterLevel);
					break;
				}
				case ADD_ITEM: {
					ItemValue value = (ItemValue) update.getValue();
					Inventory.InventoryType type = InventoryTools.getCategory(value.itemId);

					Inventory inv = target.getInventory(type);
					InventoryTools.UpdatedSlots changedSlots = InventoryTools.addToInventory(inv, value.itemId, value.quantity);
					ClientSession<?> ses = target.getClient().getSession();
					short pos;
					for (Short s : changedSlots.modifiedSlots) {
						pos = s.shortValue();
						ses.send(GamePackets.writeInventoryUpdateSlotQuantity(type, pos, inv.get(pos)));
					}
					for (Short s : changedSlots.addedOrRemovedSlots) {
						pos = s.shortValue();
						ses.send(GamePackets.writeInventoryAddSlot(type, pos, inv.get(pos)));
					}
					target.itemCountChanged(value.itemId);
					break;
				}
			}
		}
	}

	@Override
	public Object access(CharacterProperty key) {
		switch (key) {
			case MAP:
				return new MapValue(target.getMapId(), target.getMap().nearestSpawnPoint(target.getPosition()));
			case CHANNEL:
				return Byte.valueOf(target.getClient().getChannel());
			case POSITION:
				return target.getPosition();
			case PLAYER_ID:
				return Integer.valueOf(target.getId());
			default:
				return null;
		}
	}
}
