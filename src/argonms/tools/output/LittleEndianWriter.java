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

package argonms.tools.output;

import java.awt.Point;
import java.nio.charset.Charset;

/**
 * Write data to a stream using little endian byte ordering for
 * multiple-byte integer types.
 * Characters (and character strings) are encoded in ASCII.
 * @author GoldenKevin
 * @version 1.1
 */
public abstract class LittleEndianWriter {
	private static final Charset asciiEncoder = Charset.forName("US-ASCII");

	protected abstract void write(byte b);
	protected abstract void write(byte... bytes);
	public abstract void dispose();

	public LittleEndianWriter writeLong(long l) {
		write(
				(byte) ((l & 0xFF)),
				(byte) ((l >>> 8) & 0xFF),
				(byte) ((l >>> 16) & 0xFF),
				(byte) ((l >>> 24) & 0xFF),
				(byte) ((l >>> 32) & 0xFF),
				(byte) ((l >>> 40) & 0xFF),
				(byte) ((l >>> 48) & 0xFF),
				(byte) ((l >>> 56) & 0xFF)
		);
		return this;
	}

	public LittleEndianWriter writeInt(int i) {
		write(
				(byte) ((i & 0xFF)),
				(byte) ((i >>> 8) & 0xFF),
				(byte) ((i >>> 16) & 0xFF),
				(byte) ((i >>> 24) & 0xFF)
		);
		return this;
	}

	public LittleEndianWriter writeShort(short s) {
		write(
				(byte) ((s & 0xFF)),
				(byte) ((s >>> 8) & 0xFF)
		);
		return this;
	}

	public LittleEndianWriter writeByte(byte b) {
		write(b);
		return this;
	}

	public LittleEndianWriter writeFloat(float f) {
		writeInt(Float.floatToRawIntBits(f));
		return this;
	}

	public LittleEndianWriter writeDouble(double d) {
		writeLong(Double.doubleToRawLongBits(d));
		return this;
	}

	public LittleEndianWriter writeNullTerminatedString(String str) {
		writeBytes(str.getBytes(asciiEncoder));
		writeChar('\0');
		return this;
	}

	public LittleEndianWriter writePaddedAsciiString(String str, int n) {
		int length = Math.min(str.length(), n);
		byte[] space = new byte[n];
		byte[] ascii = str.getBytes(asciiEncoder);
		System.arraycopy(ascii, 0, space, 0, length);
		write(space);
		return this;
	}

	public LittleEndianWriter writeLengthPrefixedString(String str) {
		if (str != null) {
			writeShort((short) str.length());
			writeBytes(str.getBytes(asciiEncoder));
		} else {
			writeShort((short) 0);
		}
		return this;
	}

	public LittleEndianWriter writeChar(char c) {
		writeByte((byte) c); //just cast since we're using 1-byte ascii chars
		return this;
	}

	public LittleEndianWriter writeBool(boolean b) {
		writeByte((byte) (b ? 1 : 0));
		return this;
	}

	public LittleEndianWriter writeBytes(byte... b) {
		write(b);
		return this;
	}

	/**
	 * Writes a 4-byte long Point to the stream (2-bytes for x, 2-bytes for y)
	 * @param p the Point that contains the x and y coordinates to write. If the
	 * x or y values are out of the range of the short data type, they will be
	 * cut off.
	 * @return
	 */
	public LittleEndianWriter writePos(Point p) {
		writeShort((short) p.x);
		writeShort((short) p.y);
		return this;
	}
}
