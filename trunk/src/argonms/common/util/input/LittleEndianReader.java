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

package argonms.common.util.input;

import java.awt.Point;

/**
 * Reads a stream that uses little endian byte ordering for multiple-byte
 * integer types. Characters (and strings) are assumed to be encoded in ASCII.
 * @author GoldenKevin
 * @version 1.1
 */
public abstract class LittleEndianReader {
	protected abstract int read();
	protected abstract byte[] read(int amount);
	public abstract void skip(int amount);
	public abstract int available();
	public abstract void dispose();

	public long readLong() {
		byte[] bytes = read(8);
		return (
				((long) (bytes[0] & 0xFF)) +
				((long) (bytes[1] & 0xFF) << 8) +
				((long) (bytes[2] & 0xFF) << 16) +
				((long) (bytes[3] & 0xFF) << 24) +
				((long) (bytes[4] & 0xFF) << 32) +
				((long) (bytes[5] & 0xFF) << 40) +
				((long) (bytes[6] & 0xFF) << 48) +
				((long) (bytes[7] & 0xFF) << 56)
		);
	}

	public int readInt() {
		byte[] bytes = read(4);
		return (
				((bytes[0] & 0xFF)) +
				((bytes[1] & 0xFF) << 8) +
				((bytes[2] & 0xFF) << 16) +
				((bytes[3] & 0xFF) << 24)
		);
	}

	public short readShort() {
		byte[] bytes = read(2);
		return (short) (
				((bytes[0] & 0xFF)) +
				((bytes[1] & 0xFF) << 8)
		);
	}

	public byte readByte() {
		return (byte) read();
	}

	public float readFloat() {
		return Float.intBitsToFloat(readInt());
	}

	public double readDouble() {
		return Double.longBitsToDouble(readLong());
	}

	public String readNullTerminatedString() {
		StringBuilder builder = new StringBuilder();

		for (char current = readChar(); current != '\0'; current = readChar())
			builder.append(current);

		return builder.toString();
	}

	//this way, we don't have to call the public method readPaddedAsciiString
	//in readLengthPrefixedString... improves performance somewhat (inlining)
	//and if somebody overrides readPaddedAsciiString, it won't screw it up.
	private String readKnownLengthAsciiString(int n) {
		//uses O(2n) memory, but takes half as long than alternatives
		byte[] bytes = read(n);
		char[] ret = new char[n];
		for (int x = 0; x < n; x++)
			ret[x] = (char) bytes[x];
		return String.valueOf(ret);
	}

	public String readPaddedAsciiString(int n) {
		return readKnownLengthAsciiString(n);
	}

	public String readLengthPrefixedString() {
		return readKnownLengthAsciiString(readShort());
	}

	/**
	 * Read a one-byte large character that is encoded in ASCII.
	 * @return
	 */
	public char readChar() {
		return (char) readByte(); //just cast since we're using 1-byte ascii chars
	}

	public boolean readBool() {
		return (readByte() > 0);
	}

	public byte[] readBytes(int size) {
		return read(size);
	}

	/**
	 * Read a 4-byte long Point from the stream (2-bytes for x, 2-bytes for y)
	 * @return
	 */
	public Point readPos() {
		return new Point(readShort(), readShort());
	}
}
