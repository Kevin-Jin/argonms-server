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
import argonms.common.character.BuddyList;
import argonms.common.character.PlayerStatusEffect;
import argonms.common.character.inventory.Equip;
import argonms.common.character.inventory.Inventory;
import argonms.common.character.inventory.InventorySlot;
import argonms.common.character.inventory.InventoryTools;
import argonms.common.net.external.CheatTracker;
import argonms.common.net.external.ClientSession;
import argonms.common.util.TimeTool;
import argonms.common.util.collections.Pair;
import argonms.game.GameServer;
import argonms.game.character.DiseaseTools;
import argonms.game.character.ExpTables;
import argonms.game.character.GameCharacter;
import argonms.game.character.MapMemoryVariable;
import argonms.game.character.PlayerStatusEffectValues;
import argonms.game.character.StatusEffectTools;
import argonms.game.character.inventory.StorageInventory;
import argonms.game.field.MobSkills;
import argonms.game.loading.skill.MobSkillEffectsData;
import argonms.game.loading.skill.SkillDataLoader;
import argonms.game.net.external.GamePackets;
import java.util.Calendar;
import java.util.List;
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

	private byte getByte(CharacterManipulation update) {
		return ((Byte) update.getValue()).byteValue();
	}

	private int getInt(CharacterManipulation update) {
		return ((Integer) update.getValue()).intValue();
	}

	private short getShort(CharacterManipulation update) {
		return ((Short) update.getValue()).shortValue();
	}

	@Override
	public void mutate(List<CharacterManipulation> updates) {
		for (CharacterManipulation update : updates) {
			switch (update.getKey()) {
				case CHANGE_MAP: {
					MapValue value = (MapValue) update.getValue();
					if (value.mapId / 100 == MapValue.FREE_MARKET_MAP_ID / 100 && target.getRememberedMap(MapMemoryVariable.FREE_MARKET).left.intValue() == GlobalConstants.NULL_MAP)
						target.rememberMap(MapMemoryVariable.FREE_MARKET);
					if (value.mapId == MapValue.JAIL_MAP_ID && target.getRememberedMap(MapMemoryVariable.JAIL).left.intValue() == GlobalConstants.NULL_MAP)
						target.rememberMap(MapMemoryVariable.JAIL);
					if (value.channel == MapValue.NO_CHANNEL_CHANGE || value.channel == target.getClient().getChannel())
						target.changeMap(value.mapId, value.spawnPoint);
					else
						target.changeMapAndChannel(value.mapId, value.spawnPoint, value.channel);
					break;
				}
				case CHANGE_CHANNEL:
					GameServer.getChannel(target.getClient().getChannel()).requestChannelChange(target, getByte(update));
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
					target.setSkillLevel(value.skillId, value.skillLevel, value.skillMasterLevel, false);
					break;
				}
				case ADD_ITEM: {
					ItemValue value = (ItemValue) update.getValue();
					Inventory.InventoryType type = InventoryTools.getCategory(value.itemId);

					Inventory inv = target.getInventory(type);
					InventoryTools.UpdatedSlots changedSlots;
					ClientSession<?> ses = target.getClient().getSession();
					short pos;
					if (value.quantity > 0) {
						changedSlots = InventoryTools.addToInventory(inv, value.itemId, value.quantity);
						for (Short s : changedSlots.addedOrRemovedSlots) {
							pos = s.shortValue();
							ses.send(GamePackets.writeInventoryAddSlot(type, pos, inv.get(pos)));
						}
					} else {
						int quantity;
						if (value.quantity == Integer.MIN_VALUE) {
							quantity = InventoryTools.getAmountOfItem(inv, value.itemId);
							if (type == Inventory.InventoryType.EQUIP)
								quantity += InventoryTools.getAmountOfItem(inv, value.itemId);
						} else {
							quantity = -value.quantity;
						}
						changedSlots = InventoryTools.removeFromInventory(inv, value.itemId, quantity);
						for (Short s : changedSlots.addedOrRemovedSlots) {
							pos = s.shortValue();
							ses.send(GamePackets.writeInventoryClearSlot(type, pos));
						}
					}
					for (Short s : changedSlots.modifiedSlots) {
						pos = s.shortValue();
						ses.send(GamePackets.writeInventoryUpdateSlotQuantity(type, pos, inv.get(pos)));
					}
					target.itemCountChanged(value.itemId);
					break;
				}
				case CANCEL_DEBUFFS: {
					PlayerStatusEffect[] effects = (PlayerStatusEffect[]) update.getValue();
					for (PlayerStatusEffect e : effects) {
						PlayerStatusEffectValues v = target.getEffectValue(e);
						if (v != null)
							DiseaseTools.cancelDebuff(target, (short) v.getSource(), v.getLevelWhenCast());
					}
					break;
				}
				case MAX_ALL_EQUIP_STATS: {
					Map<Short, InventorySlot> iv = target.getInventory(Inventory.InventoryType.EQUIPPED).getAll();
					synchronized(iv) {
						for (Map.Entry<Short, InventorySlot> item : iv.entrySet()) {
							Equip e = (Equip) item.getValue();
							target.equipChanged(e, false, false);
							e.setStr(Short.MAX_VALUE);
							e.setDex(Short.MAX_VALUE);
							e.setInt(Short.MAX_VALUE);
							e.setLuk(Short.MAX_VALUE);
							e.setHp((short) 30000);
							e.setMp((short) 30000);
							e.setWatk(Short.MAX_VALUE);
							e.setMatk(Short.MAX_VALUE);
							e.setWdef(Short.MAX_VALUE);
							e.setMdef(Short.MAX_VALUE);
							e.setAcc(Short.MAX_VALUE);
							e.setAvoid(Short.MAX_VALUE);
							e.setHands(Short.MAX_VALUE);
							e.setSpeed((short) 40);
							e.setJump((short) 23);
							target.equipChanged(e, true, true);
							target.getClient().getSession().send(GamePackets.writeInventoryUpdateEquipStats(item.getKey().shortValue(), e));
						}
					}
				}
				case MAX_INVENTORY_SLOTS: {
					for (Inventory.InventoryType type : new Inventory.InventoryType[] { Inventory.InventoryType.EQUIP, Inventory.InventoryType.USE, Inventory.InventoryType.SETUP, Inventory.InventoryType.ETC, Inventory.InventoryType.CASH }) {
						Inventory inv = target.getInventory(type);
						inv.increaseCapacity((short) 0xFF);
						target.getClient().getSession().send(GamePackets.writeInventoryUpdateCapacity(type, (short) 0xFF));
					}
					StorageInventory inv = target.getStorageInventory();
					inv.increaseCapacity((short) 0xFF);
					break;
				}
				case MAX_BUDDY_LIST_SLOTS: {
					BuddyList bList = target.getBuddyList();
					bList.increaseCapacity((short) 0xFF);
					target.getClient().getSession().send(GamePackets.writeBuddyCapacityUpdate(bList.getCapacity()));
					break;
				}
				case BAN: {
					BanValue value = (BanValue) update.getValue();
					Calendar cal = TimeTool.currentDateTime();
					cal.setTimeInMillis(value.expireTimestamp);
					CheatTracker.get(target.getClient()).ban(CheatTracker.Infraction.POSSIBLE_PACKET_EDITING, value.banner, value.reason, cal);
					break;
				}
				case STUN: {
					boolean start = ((Boolean) update.getValue()).booleanValue();
					MobSkillEffectsData e = SkillDataLoader.getInstance().getMobSkill(MobSkills.STUN).getLevel((byte) 1);
					if (start)
						StatusEffectTools.applyEffectsAndShowVisuals(target, StatusEffectTools.ACTIVE_BUFF, e, (byte) -1, Integer.MAX_VALUE);
					else //stop
						StatusEffectTools.dispelEffectsAndShowVisuals(target, e);
					break;
				}
				case CLEAR_INVENTORY_SLOTS: {
					InventorySlotRangeValue value = (InventorySlotRangeValue) update.getValue();
					Inventory inv = target.getInventory(value.type);
					short upperBound = (short) Math.min(value.endSlot, inv.getMaxSlots());
					for (short slot = value.startSlot; slot <= upperBound; slot++) {
						InventorySlot removed = inv.remove(slot);
						if (removed != null) {
							target.getClient().getSession().send(GamePackets.writeInventoryClearSlot(value.type, slot));
							target.itemCountChanged(removed.getDataId());
						}
					}
					break;
				}
				case RETURN_TO_REMEMBERED_MAP: {
					MapMemoryVariable value = (MapMemoryVariable) update.getValue();
					Pair<Integer, Byte> location = target.resetRememberedMap(value);
					if (location.left.intValue() != GlobalConstants.NULL_MAP && location.right.byteValue() != -1)
						target.changeMap(location.left.intValue(), location.right.byteValue());
					break;
				}
			}
		}
	}

	@Override
	public Object access(CharacterProperty key) {
		switch (key) {
			case MAP:
				return new MapValue(target.getMapId(), target.getMap().nearestSpawnPoint(target.getPosition()), target.getClient().getChannel());
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
