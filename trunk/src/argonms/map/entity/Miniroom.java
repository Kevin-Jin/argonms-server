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

		private final int id;

		private static final Map<Integer, MiniroomType> lookup = new HashMap<Integer, MiniroomType>();

		//initialize reverse lookup
		static {
			for(MiniroomType type : MiniroomType.values())
				lookup.put(Integer.valueOf(type.getValue()), type);
		}

		private MiniroomType(int clientVal) {
			id = clientVal;
		}

		public int getValue() {
			return id;
		}

		public static MiniroomType getByValue(int value) {
			return lookup.get(Integer.valueOf(value));
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
