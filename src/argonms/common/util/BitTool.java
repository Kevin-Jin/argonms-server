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

package argonms.common.util;

/**
 * Provides static methods for working with raw byte sequences.
 *
 * Taken from an OdinMS-derived source with a few modifications.
 *
 * @author Frz
 * @since Revision 206
 * @version 1.1
 */
public final class BitTool {
	private BitTool() {
		//uninstantiable...
	}

	/**
	 * Rotates the bits of <code>in</code> <code>count</code> places to the
	 * left.
	 * 
	 * @param in The byte to rotate the bits
	 * @param count Number of times to rotate.
	 * @return The rotated byte.
	 */
	public static byte rollLeft(byte in, int count) {
		int tmp = in & 0xFF;
		tmp = tmp << (count % 8);
		return (byte) ((tmp & 0xFF) | (tmp >> 8));
	}

	/**
	 * Rotates the bits of <code>in</code> <code>count</code> places to the
	 * right.
	 * 
	 * @param in The byte to rotate the bits
	 * @param count Number of times to rotate.
	 * @return The rotated byte.
	 */
	public static byte rollRight(byte in, int count) {
		int tmp = in & 0xFF;
		tmp = (tmp << 8) >>> (count % 8);
		return (byte) ((tmp & 0xFF) | (tmp >>> 8));
	}

	/**
	 * Repeats <code>count</code> bytes of <code>in</code> <code>mul</code> times.
	 * 
	 * @param in The array of bytes containing the bytes to multiply.
	 * @param count The number of bytes to repeat.
	 * @param mul The number of times to repeat.
	 * @return The repeated bytes.
	 */
	public static byte[] multiplyBytes(byte[] in, int count, int mul) {
		byte[] ret = new byte[count * mul];
		for (int i = 0; i < mul; i++)
			System.arraycopy(in, 0, ret, i * count, count);
		return ret;
	}
}
