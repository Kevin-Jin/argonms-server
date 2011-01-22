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

package argonms.tools;

import java.io.ByteArrayOutputStream;

/**
 * Provides a class for manipulating hexadecimal numbers.
 * 
 * Taken from an OdinMS-derived source with a few modifications.
 *
 * @author Frz, GoldenKevin
 * @since Revision 206
 * @version 1.1
 */
public class HexTool {
	static private char[] HEX = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };

	/**
	 * Static class dummy constructor.
	 */
	private HexTool() {
	}

	/**
	 * Turns a byte into a hexadecimal string.
	 * 
	 * @param byteValue The byte to convert.
	 * @return The hexadecimal representation of <code>byteValue</code>
	 */
	public static String toString(byte byteValue) {
		int tmp = byteValue << 8;
		char[] retstr = new char[] { HEX[(tmp >> 12) & 0x0F], HEX[(tmp >> 8) & 0x0F] };
		return String.valueOf(retstr);
	}

	/**
	 * Turns an integer into a hexadecimal string.
	 * 
	 * @param intValue The integer to transform.
	 * @return The hexadecimal representation of <code>intValue</code>.
	 */
	public static String toString(int intValue) {
		return Integer.toHexString(intValue);
	}

	/**
	 * Turns an array of bytes into a hexadecimal string.
	 * 
	 * @param bytes The bytes to convert.
	 * @return The hexadecimal representation of <code>bytes</code>
	 */
	public static String toString(byte[] bytes) {
		StringBuilder hexed = new StringBuilder();
		for (int i = 0; i < bytes.length; i++) {
			hexed.append(toString(bytes[i]));
			hexed.append(' ');
		}
		return hexed.substring(0, hexed.length() - 1);
	}

	/**
	 * Turns an array of bytes into a ASCII string. Any non-printable characters
	 * are replaced by a period (<code>.</code>)
	 * 
	 * @param bytes The bytes to convert.
	 * @return The ASCII hexadecimal representation of <code>bytes</code>
	 */
	public static String toStringFromAscii(byte[] bytes) {
		char[] ret = new char[bytes.length];
		for (int x = 0; x < bytes.length; x++) {
			if (bytes[x] < 32 && bytes[x] >= 0) {
				ret[x] = '.';
			} else {
				int chr = ((short) bytes[x]) & 0xFF;
				ret[x] = (char) chr;
			}
		}
		return String.valueOf(ret);
	}
	
	public static String toPaddedStringFromAscii(byte[] bytes) {
		String str = toStringFromAscii(bytes);
		StringBuilder ret = new StringBuilder(str.length() * 3);
		for (int i = 0; i < str.length(); i++) {
			ret.append(str.charAt(i));
			ret.append("  ");
		}
		return ret.toString();
	}

	/**
	 * Turns an hexadecimal string into a byte array.
	 * 
	 * @param hex The string to convert.
	 * @return The byte array representation of <code>hex</code>
	 */
	public static byte[] getByteArrayFromHexString(String hex) {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		int nexti = 0;
		int nextb = 0;
		boolean highoc = true;
		outer: for (;;) {
			int number = -1;
			while (number == -1) {
				if (nexti == hex.length()) {
					break outer;
				}
				char chr = hex.charAt(nexti);
				if (chr >= '0' && chr <= '9') {
					number = chr - '0';
				} else if (chr >= 'a' && chr <= 'f') {
					number = chr - 'a' + 10;
				} else if (chr >= 'A' && chr <= 'F') {
					number = chr - 'A' + 10;
				} else {
					number = -1;
				}
				nexti++;
			}
			if (highoc) {
				nextb = number << 4;
				highoc = false;
			} else {
				nextb |= number;
				highoc = true;
				baos.write(nextb);
			}
		}
		return baos.toByteArray();
	}
}
