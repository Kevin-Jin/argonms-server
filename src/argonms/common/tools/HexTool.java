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

package argonms.common.tools;

/**
 * Provides a class for manipulating hexadecimal numbers.
 * 
 * Taken from an OdinMS-derived source. Optimized and modified by GoldenKevin.
 *
 * @author Frz, GoldenKevin
 * @since Revision 206
 * @version 1.2
 */
public class HexTool {
	private static final char[] HEX = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };

	/**
	 * Static class dummy constructor.
	 */
	private HexTool() {
	}

	private static char[] toCharArray(byte byteValue) {
		return new char[] { HEX[(byteValue & 0xF0) >>> 4], HEX[byteValue & 0x0F] };
	}

	private static char[] toCharArrayWithSpace(byte byteValue) {
		return new char[] { HEX[(byteValue & 0xF0) >>> 4], HEX[byteValue & 0x0F], ' ' };
	}

	/**
	 * Turns a byte into a hexadecimal string.
	 * 
	 * @param byteValue The byte to convert.
	 * @return The hexadecimal representation of <code>byteValue</code>
	 */
	public static String toString(byte byteValue) {
		return String.valueOf(toCharArray(byteValue));
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
	 * Turns an array of bytes into a hexadecimal string with no delimiters.
	 *
	 * @param bytes The bytes to convert.
	 * @return The hexadecimal representation of <code>bytes</code>
	 */
	public static String toStringNoSpaces(byte[] bytes) {
		char[] array = new char[bytes.length * 2];
		for (int i = 0; i < bytes.length; i++)
			System.arraycopy(toCharArray(bytes[i]), 0, array, i * 2, 2);
		return String.valueOf(array);
	}

	/**
	 * Turns an array of bytes into a hexadecimal string with each byte
	 * delimited by a space.
	 * 
	 * @param bytes The bytes to convert.
	 * @return The hexadecimal representation of <code>bytes</code>
	 */
	public static String toString(byte[] bytes) {
		char[] array = new char[bytes.length * 3];
		for (int i = 0; i < bytes.length; i++)
			System.arraycopy(toCharArrayWithSpace(bytes[i]), 0, array, i * 3, 3);
		//somehow faster than StringBuilder.substring...
		char[] trimmed = new char[array.length - 1];
		System.arraycopy(array, 0, trimmed, 0, trimmed.length);
		return String.valueOf(trimmed);
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
			byte b = bytes[x];
			ret[x] = ((b >= 0x20 && b <= 0x7E) ? ((char) (b & 0xFF)) : '.');
		}
		return String.valueOf(ret);
	}

	private static byte[] getByteArrayFromHexString(String hex, boolean delimited) {
		int len = hex.length();
		if (delimited) {
			byte[] data = new byte[len / 3 + 1];
			for (int i = 0; i < len; i += 3)
				data[i / 3] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
									 + Character.digit(hex.charAt(i + 1), 16));
			return data;
		} else {
			byte[] data = new byte[len / 2];
			for (int i = 0; i < len; i += 2)
				data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
									 + Character.digit(hex.charAt(i + 1), 16));
			return data;
		}
	}

	/**
	 * Turns an hexadecimal string into a byte array.
	 *
	 * Try not to use this method when writing to a packet.
	 *
	 * @param hex The string to convert.
	 * @return The byte array representation of <code>hex</code>
	 */
	public static byte[] getByteArrayFromHexString(String hex) {
		//return getByteArrayFromHexString(hex, hex.contains(" "));
		//just perform the same behavior as before...
		return getByteArrayFromHexString(hex, true);
	}
}
