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

package argonms.map.entity;

import argonms.map.MapEntity;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author GoldenKevin
 */
public abstract class Miniroom extends MapEntity {
	public enum MiniroomType {
		OMOK (1),
		MATCH_CARDS (2),
		TRADE (3),
		PLAYER_SHOP (4),
		HIRED_MERCHANT (5);

		private static final Map<Byte, MiniroomType> lookup;

		//initialize reverse lookup
		static {
			lookup = new HashMap<Byte, MiniroomType>(values().length);
			for(MiniroomType type : values())
				lookup.put(Byte.valueOf(type.byteValue()), type);
		}

		private final byte id;

		private MiniroomType(int clientVal) {
			id = (byte) clientVal;
		}

		public byte byteValue() {
			return id;
		}

		public static MiniroomType valueOf(byte value) {
			return lookup.get(Byte.valueOf(value));
		}
	}

	public abstract MiniroomType getMiniroomType();

	public EntityType getEntityType() {
		return EntityType.MINI_ROOM;
	}

	public boolean isAlive() {
		return true;
	}

	public byte[] getShowEntityMessage() {
		return getCreationMessage();
	}

	public byte[] getOutOfViewMessage() {
		return null;
	}

	public boolean isNonRangedType() {
		return true;
	}
}
