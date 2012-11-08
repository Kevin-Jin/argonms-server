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
import java.util.Map;

/**
 *
 * @author GoldenKevin
 */
public interface CommandTarget {
	public static class MapValue {
		public int mapId;
		public byte spawnPoint;

		public MapValue(int mapId, byte portal) {
			this.mapId = mapId;
			this.spawnPoint = portal;
		}
	}

	public static class SkillValue {
		public int skillId;
		public byte skillLevel;
		public byte skillMasterLevel;

		public SkillValue(int skillId, byte skillLevel, byte skillMasterLevel) {
			this.skillId = skillId;
			this.skillLevel = skillLevel;
			this.skillMasterLevel = skillMasterLevel;
		}
	}

	public static class ItemValue {
		public int itemId;
		public int quantity;

		public ItemValue(int itemId, int quantity) {
			this.itemId = itemId;
			this.quantity = quantity;
		}
	}

	public enum CharacterManipulation {
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
		ADD_ITEM((byte) 33);

		private static final Map<Byte, CharacterManipulation> lookup;

		static {
			lookup = new HashMap<Byte, CharacterManipulation>();
			for (CharacterManipulation key : values())
				lookup.put(Byte.valueOf(key.byteValue()), key);
		}

		private final byte serial;

		private CharacterManipulation(byte serial) {
			this.serial = serial;
		}

		public byte byteValue() {
			return serial;
		}
	}

	public enum CharacterProperty {
		MAP((byte) 1),
		CHANNEL((byte) 2),
		POSITION((byte) 3),
		PLAYER_ID((byte) 4);

		private static final Map<Byte, CharacterProperty> lookup;

		static {
			lookup = new HashMap<Byte, CharacterProperty>();
			for (CharacterProperty key : values())
				lookup.put(Byte.valueOf(key.byteValue()), key);
		}

		private final byte serial;

		private CharacterProperty(byte serial) {
			this.serial = serial;
		}

		public byte byteValue() {
			return serial;
		}
	}

	public void mutate(Map<CharacterManipulation, ?> updates);

	public Object access(CharacterProperty key);
}
