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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author GoldenKevin
 */
public interface CommandTarget {
	public static class MapValue {
		public static byte NO_CHANNEL_CHANGE = -1;

		public final int mapId;
		public final byte spawnPoint;
		public final byte channel;

		public MapValue(int mapId, byte portal, byte channel) {
			this.mapId = mapId;
			this.spawnPoint = portal;
			this.channel = channel;
		}

		public MapValue(MapValue toCopy, byte channel) {
			this(toCopy.mapId, toCopy.spawnPoint, channel);
		}

		public MapValue(int mapId, byte portal) {
			this(mapId, portal, NO_CHANNEL_CHANGE);
		}

		public MapValue(int mapId) {
			this(mapId, (byte) 0);
		}
	}

	public static class SkillValue {
		public final int skillId;
		public final byte skillLevel;
		public final byte skillMasterLevel;

		public SkillValue(int skillId, byte skillLevel, byte skillMasterLevel) {
			this.skillId = skillId;
			this.skillLevel = skillLevel;
			this.skillMasterLevel = skillMasterLevel;
		}
	}

	public static class ItemValue {
		public final int itemId;
		public final int quantity;

		public ItemValue(int itemId, int quantity) {
			this.itemId = itemId;
			this.quantity = quantity;
		}
	}

	public static class BanValue {
		public final String banner;
		public final String reason;
		public final long expireTimestamp;

		public BanValue(String banner, String reason, long expireTimestamp) {
			this.banner = banner;
			this.reason = reason;
			this.expireTimestamp = expireTimestamp;
		}
	}

	public static class CharacterManipulation {
		private final CharacterManipulationKey key;
		private final Object value;

		public CharacterManipulation(CharacterManipulationKey key, Object value) {
			this.key = key;
			this.value = value;
		}

		public CharacterManipulationKey getKey() {
			return key;
		}

		public Object getValue() {
			return value;
		}
	}

	public enum CharacterManipulationKey {
		CHANGE_MAP((byte) 1),
		CHANGE_CHANNEL((byte) 2),
		ADD_LEVEL((byte) 3),
		SET_LEVEL((byte) 4),
		SET_JOB((byte) 5),
		ADD_STR((byte) 6),
		SET_STR((byte) 7),
		ADD_DEX((byte) 8),
		SET_DEX((byte) 9),
		ADD_INT((byte) 10),
		SET_INT((byte) 11),
		ADD_LUK((byte) 12),
		SET_LUK((byte) 13),
		ADD_AP((byte) 14),
		SET_AP((byte) 15),
		ADD_SP((byte) 16),
		SET_SP((byte) 17),
		ADD_MAX_HP((byte) 18),
		SET_MAX_HP((byte) 19),
		ADD_MAX_MP((byte) 20),
		SET_MAX_MP((byte) 21),
		ADD_HP((byte) 22),
		SET_HP((byte) 23),
		ADD_MP((byte) 24),
		SET_MP((byte) 25),
		ADD_FAME((byte) 26),
		SET_FAME((byte) 27),
		ADD_EXP((byte) 28),
		SET_EXP((byte) 29),
		ADD_MESO((byte) 30),
		SET_MESO((byte) 31),
		SET_SKILL_LEVEL((byte) 32),
		ADD_ITEM((byte) 33),
		CANCEL_DEBUFFS((byte) 34),
		MAX_ALL_EQUIP_STATS((byte) 35),
		MAX_INVENTORY_SLOTS((byte) 36),
		MAX_BUDDY_LIST_SLOTS((byte) 37),
		BAN((byte) 38);

		private static final Map<Byte, CharacterManipulationKey> lookup;

		static {
			lookup = new HashMap<Byte, CharacterManipulationKey>();
			for (CharacterManipulationKey key : values())
				lookup.put(Byte.valueOf(key.byteValue()), key);
		}

		private final byte serial;

		private CharacterManipulationKey(byte serial) {
			this.serial = serial;
		}

		public byte byteValue() {
			return serial;
		}

		public static CharacterManipulationKey valueOf(byte b) {
			return lookup.get(Byte.valueOf(b));
		}
	}

	public enum CharacterProperty {
		MAP((byte) 1, 6),
		CHANNEL((byte) 2, 1),
		POSITION((byte) 3, 4),
		PLAYER_ID((byte) 4, 4);

		private static final Map<Byte, CharacterProperty> lookup;

		static {
			lookup = new HashMap<Byte, CharacterProperty>();
			for (CharacterProperty key : values())
				lookup.put(Byte.valueOf(key.byteValue()), key);
		}

		private final byte serial;
		private final int valueSize;

		private CharacterProperty(byte serial, int valueSize) {
			this.serial = serial;
			this.valueSize = valueSize;
		}

		public byte byteValue() {
			return serial;
		}

		public int getSizeOfValue() {
			return valueSize;
		}

		public static CharacterProperty valueOf(byte b) {
			return lookup.get(Byte.valueOf(b));
		}
	}

	public void mutate(List<CharacterManipulation> updates);

	public Object access(CharacterProperty key);
}
