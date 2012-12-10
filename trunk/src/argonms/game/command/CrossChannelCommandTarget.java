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

import argonms.common.character.PlayerStatusEffect;
import argonms.common.character.inventory.Inventory;
import argonms.common.util.input.LittleEndianReader;
import argonms.common.util.output.LittleEndianWriter;
import argonms.game.GameServer;
import java.awt.Point;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 *
 * @author GoldenKevin
 */
public class CrossChannelCommandTarget implements CommandTarget {
	private final byte localChannel, targetChannel;
	private final String targetName;

	public CrossChannelCommandTarget(byte localChannel, byte targetChannel, String targetName) {
		this.localChannel = localChannel;
		this.targetChannel = targetChannel;
		this.targetName = targetName;
	}

	private static byte getByte(CharacterManipulation update) {
		return ((Byte) update.getValue()).byteValue();
	}

	private static int getInt(CharacterManipulation update) {
		return ((Integer) update.getValue()).intValue();
	}

	private static short getShort(CharacterManipulation update) {
		return ((Short) update.getValue()).shortValue();
	}

	@Override
	public void mutate(List<CharacterManipulation> updates) {
		GameServer.getChannel(localChannel).getCrossServerInterface().sendCrossChannelCommandCharacterManipulation(targetChannel, targetName, updates);
	}

	public static void serialize(List<CharacterManipulation> updates, LittleEndianWriter lew) {
		lew.writeShort((short) updates.size());
		for (CharacterManipulation update : updates) {
			lew.writeByte(update.getKey().byteValue());
			switch (update.getKey()) {
				case CHANGE_MAP: {
					MapValue value = (MapValue) update.getValue();
					lew.writeInt(value.mapId);
					lew.writeByte(value.spawnPoint);
					lew.writeByte(value.channel);
					break;
				}
				case CHANGE_CHANNEL:
					lew.writeByte(getByte(update));
					break;
				case ADD_LEVEL:
				case SET_LEVEL:
				case SET_JOB:
				case ADD_STR:
				case SET_STR:
				case ADD_DEX:
				case SET_DEX:
				case ADD_INT:
				case SET_INT:
				case ADD_LUK:
				case SET_LUK:
				case ADD_AP:
				case SET_AP:
				case ADD_SP:
				case SET_SP:
				case ADD_MAX_HP:
				case SET_MAX_HP:
				case ADD_MAX_MP:
				case SET_MAX_MP:
				case ADD_HP:
				case SET_HP:
				case ADD_MP:
				case SET_MP:
				case ADD_FAME:
				case SET_FAME:
					lew.writeShort(getShort(update));
					break;
				case ADD_EXP:
				case SET_EXP:
				case ADD_MESO:
				case SET_MESO:
					lew.writeInt(getInt(update));
					break;
				case SET_SKILL_LEVEL: {
					SkillValue value = (SkillValue) update.getValue();
					lew.writeInt(value.skillId);
					lew.writeByte(value.skillLevel);
					lew.writeByte(value.skillMasterLevel);
					break;
				}
				case ADD_ITEM: {
					ItemValue value = (ItemValue) update.getValue();
					lew.writeInt(value.itemId);
					lew.writeInt(value.quantity);
					break;
				}
				case CANCEL_DEBUFFS: {
					long mask = 0;
					PlayerStatusEffect[] effects = (PlayerStatusEffect[]) update.getValue();
					for (PlayerStatusEffect e : effects)
						mask |= e.longValue();
					lew.writeLong(mask);
					break;
				}
				case MAX_ALL_EQUIP_STATS:
				case MAX_INVENTORY_SLOTS:
				case MAX_BUDDY_LIST_SLOTS:
					break;
				case BAN: {
					BanValue value = (BanValue) update.getValue();
					lew.writeLengthPrefixedString(value.banner);
					lew.writeLengthPrefixedString(value.reason);
					lew.writeLong(value.expireTimestamp);
					break;
				}
				case STUN:
					lew.writeBool(((Boolean) update.getValue()).booleanValue());
					break;
				case CLEAR_INVENTORY_SLOTS: {
					InventorySlotRangeValue value = (InventorySlotRangeValue) update.getValue();
					lew.writeByte(value.type.byteValue());
					lew.writeShort(value.startSlot);
					lew.writeShort(value.endSlot);
					break;
				}
			}
		}
	}

	public static List<CharacterManipulation> deserialize(LittleEndianReader packet) {
		short count = packet.readShort();
		List<CharacterManipulation> updates = new ArrayList<CharacterManipulation>(count);
		for (int i = 0; i < count; i++) {
			CharacterManipulationKey key = CharacterManipulationKey.valueOf(packet.readByte());
			Object value = null;
			switch (key) {
				case CHANGE_MAP: {
					int mapId = packet.readInt();
					byte spawnPoint = packet.readByte();
					byte channel = packet.readByte();
					value = new MapValue(mapId, spawnPoint, channel);
					break;
				}
				case CHANGE_CHANNEL:
					value = Byte.valueOf(packet.readByte());
					break;
				case ADD_LEVEL:
				case SET_LEVEL:
				case SET_JOB:
				case ADD_STR:
				case SET_STR:
				case ADD_DEX:
				case SET_DEX:
				case ADD_INT:
				case SET_INT:
				case ADD_LUK:
				case SET_LUK:
				case ADD_AP:
				case SET_AP:
				case ADD_SP:
				case SET_SP:
				case ADD_MAX_HP:
				case SET_MAX_HP:
				case ADD_MAX_MP:
				case SET_MAX_MP:
				case ADD_HP:
				case SET_HP:
				case ADD_MP:
				case SET_MP:
				case ADD_FAME:
				case SET_FAME:
					value = Short.valueOf(packet.readShort());
					break;
				case ADD_EXP:
				case SET_EXP:
				case ADD_MESO:
				case SET_MESO:
					value = Integer.valueOf(packet.readInt());
					break;
				case SET_SKILL_LEVEL: {
					int skillId = packet.readInt();
					byte skillLevel = packet.readByte();
					byte skillMasterLevel = packet.readByte();
					value = new SkillValue(skillId, skillLevel, skillMasterLevel);
					break;
				}
				case ADD_ITEM: {
					int itemId = packet.readInt();
					int quantity = packet.readInt();
					value = new ItemValue(itemId, quantity);
					break;
				}
				case CANCEL_DEBUFFS: {
					long mask = packet.readLong();
					Set<PlayerStatusEffect> values = EnumSet.noneOf(PlayerStatusEffect.class);
					for (PlayerStatusEffect e : PlayerStatusEffect.values())
						if ((mask & e.longValue()) != 0)
							values.add(e);
					value = values.toArray(new PlayerStatusEffect[values.size()]);
					break;
				}
				case MAX_ALL_EQUIP_STATS:
				case MAX_INVENTORY_SLOTS:
				case MAX_BUDDY_LIST_SLOTS:
					break;
				case BAN: {
					String banner = packet.readLengthPrefixedString();
					String reason = packet.readLengthPrefixedString();
					long expireTimestamp = packet.readLong();
					value = new BanValue(banner, reason, expireTimestamp);
					break;
				}
				case STUN:
					value = Boolean.valueOf(packet.readBool());
					break;
				case CLEAR_INVENTORY_SLOTS: {
					Inventory.InventoryType type = Inventory.InventoryType.valueOf(packet.readByte());
					short startSlot = packet.readShort();
					short endSlot = packet.readShort();
					value = new InventorySlotRangeValue(type, startSlot, endSlot);
					break;
				}
			}
			updates.add(new CharacterManipulation(key, value));
		}
		return updates;
	}

	public static void serialize(CharacterProperty key, Object val, LittleEndianWriter lew) {
		switch (key) {
			case MAP: {
				MapValue value = (MapValue) val;
				lew.writeInt(value.mapId);
				lew.writeByte(value.spawnPoint);
				lew.writeByte(value.channel);
				break;
			}
			case POSITION:
				lew.writePos((Point) val);
				break;
			case PLAYER_ID:
				lew.writeInt(((Integer) val).intValue());
				break;
		}
	}

	public static Object deserialize(CharacterProperty key, LittleEndianReader packet) {
		switch (key) {
			case MAP: {
				int mapId = packet.readInt();
				byte spawnPoint = packet.readByte();
				byte channel = packet.readByte();
				return new MapValue(mapId, spawnPoint, channel);
			}
			case POSITION:
				return packet.readPos();
			case PLAYER_ID:
				return Integer.valueOf(packet.readInt());
			default:
				return null;
		}
	}

	@Override
	public Object access(CharacterProperty key) {
		switch (key) {
			case CHANNEL:
				return Byte.valueOf(targetChannel);
			case MAP:
			case POSITION:
			case PLAYER_ID:
				return GameServer.getChannel(localChannel).getCrossServerInterface().sendCrossChannelCommandCharacterAccess(targetChannel, targetName, key);
			default:
				return null;
		}
	}
}
