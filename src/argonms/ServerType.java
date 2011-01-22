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

package argonms;

/**
 *
 * @author GoldenKevin
 */
public final class ServerType {
	public static final byte
		UNDEFINED = -4,
		CENTER = -3,
		SHOP = -2,
		LOGIN = -1,
		GAME = 0
	;

	private ServerType() {
		//uninstantiable...
	}

	public static boolean isCenter(byte type) {
		return type == CENTER;
	}

	public static boolean isShop(byte type) {
		return type == SHOP;
	}

	public static boolean isLogin(byte type) {
		return type == LOGIN;
	}

	public static boolean isGame(byte type) {
		return type >= 0;
	}

	public static String getName(byte type) {
		switch (type) {
			case CENTER:
				return "Center";
			case SHOP:
				return "Shop";
			case LOGIN:
				return "Login";
			default:
				if (type >= 0)
					return "Game" + type;
				return null;
		}
	}
}
