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

package argonms.game.field;

/**
 *
 * @author GoldenKevin
 */
public enum Element {
	NEUTRAL, FIRE, ICE, LIGHTING, POISON, HOLY, PHYSICAL;

	public static final byte
		EFFECTIVENESS_WEAK = 0,
		EFFECTIVENESS_NORMAL = 1,
		EFFECTIVENESS_STRONG = 2,
		EFFECTIVENESS_IMMUNE = 3
	;

	public static Element valueOf(char c) {
		switch (Character.toUpperCase(c)) {
			case 'F':
				return FIRE;
			case 'I':
				return ICE;
			case 'L':
				return LIGHTING;
			case 'S':
				return POISON;
			case 'H':
				return HOLY;
			case 'P':
				return PHYSICAL;
			default:
				throw new IllegalArgumentException("Unknown element char " + c);
		}
	}
}
