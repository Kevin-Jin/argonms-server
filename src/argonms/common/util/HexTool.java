/*
 * ArgonMS MapleStory server emulator written in Java
 * Copyright (C) 2011-2013  GoldenKevin
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

package argonms.common.util;

/**
 * Provides a class for manipulating hexadecimal numbers.
 * 
 * Taken from an OdinMS-derived source. Optimized and modified by GoldenKevin.
 *
 * @author Frz, GoldenKevin
 * @since Revision 206
 * @version 1.2
 */
public final class HexTool {
	private static final char[] HEX = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };

	private static char leftHexDigit(byte byteValue) {
		return HEX[(byteValue & 0xF0) >>> 4];
	}

	private static char rightHexDigit(byte byteValue) {
		return HEX[byteValue & 0x0F];
	}

	/**
	 * Turns a byte into a hexadecimal string.
	 * 
	 * @param byteValue The byte to convert.
	 * @return The hexadecimal representation of <code>byteValue</code>
	 */
	public static String toString(byte byteValue) {
		return String.valueOf(new char[] { leftHexDigit(byteValue), rightHexDigit(byteValue) });
	}

	/**
	 * Turns an integer into a hexadecimal string.
	 * 
	 * @param intValue The integer to transform.
	 * @return The hexadecimal representation of <code>intValue</code>.
	 */
	public static String toString(int intValue) {
		return Integer.toHexString(intValue).toUpperCase();
	}

	/**
	 * Turns an array of bytes into a hexadecimal string. If delimiter is the
	 * null character, the hexadecimal string will have no delimiters between
	 * bytes. Otherwise, bytes will be delimited by the specified char.
	 * 
	 * @param bytes The bytes to convert.
	 * @param delimiter The delimiter character.
	 * @return The hexadecimal representation of <code>bytes</code>
	 */
	private static String toString(byte[] input, char delimiter) {
		char[] output;
		if (delimiter != '\0') {
			if (input.length == 0)
				return "";

			output = new char[input.length * 3 - 1];
			for (int i = 0; i < input.length - 1; i++) {
				output[i * 3] = leftHexDigit(input[i]);
				output[i * 3 + 1] = rightHexDigit(input[i]);
				output[i * 3 + 2] = delimiter;
			}
			int i = input.length - 1;
			output[i * 3] = leftHexDigit(input[i]);
			output[i * 3 + 1] = rightHexDigit(input[i]);
		} else {
			output = new char[input.length * 2];
			for (int i = 0; i < input.length; i++) {
				output[i * 2] = leftHexDigit(input[i]);
				output[i * 2 + 1] = rightHexDigit(input[i]);
			}
		}
		return String.valueOf(output);
	}

	/**
	 * Turns an array of bytes into a hexadecimal string with no delimiters.
	 *
	 * @param bytes The bytes to convert.
	 * @return The hexadecimal representation of <code>bytes</code>
	 */
	public static String toStringNoSpaces(byte[] bytes) {
		return toString(bytes, '\0');
	}

	/**
	 * Turns an array of bytes into a hexadecimal string with each byte
	 * delimited by a space.
	 * 
	 * @param bytes The bytes to convert.
	 * @return The hexadecimal representation of <code>bytes</code>
	 */
	public static String toString(byte[] bytes) {
		return toString(bytes, ' ');
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
		if (delimited) {
			byte[] data = new byte[hex.length() / 3 + 1];
			for (int i = 0; i < data.length; i++)
				data[i] = (byte) ((Character.digit(hex.charAt(i * 3), 16) << 4) + Character.digit(hex.charAt(i * 3 + 1), 16));
			return data;
		} else {
			byte[] data = new byte[hex.length() / 2];
			for (int i = 0; i < data.length; i++)
				data[i] = (byte) ((Character.digit(hex.charAt(i * 2), 16) << 4) + Character.digit(hex.charAt(i * 2 + 1), 16));
			return data;
		}
	}

	/**
	 * Turns an hexadecimal string into a byte array.
	 *
	 * Use of this method for packet writing is discouraged, as text is parsed
	 * at runtime and there are no guarantees the process is optimized when this
	 * method is frequently called. Instead, a byte array with integer literals
	 * in hexadecimal should be used.
	 *
	 * @param hex The string to convert.
	 * @return The byte array representation of <code>hex</code>
	 */
	public static byte[] getByteArrayFromHexString(String hex) {
		return getByteArrayFromHexString(hex, true);
	}

	private HexTool() {
		//uninstantiable...
	}
}
