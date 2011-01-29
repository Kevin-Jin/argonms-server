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

import java.nio.charset.Charset;

/**
 *
 * @author GoldenKevin
 */
public abstract class LittleEndianWriter {
	private static final Charset asciiEncoder = Charset.forName("US-ASCII");

	protected abstract void write(byte b);
	protected abstract void write(byte[] bytes);
	public abstract void dispose();

	public LittleEndianWriter writeLong(long l) {
		write((byte) (l & 0xFF));
		write((byte) ((l >>> 8) & 0xFF));
		write((byte) ((l >>> 16) & 0xFF));
		write((byte) ((l >>> 24) & 0xFF));
		write((byte) ((l >>> 32) & 0xFF));
		write((byte) ((l >>> 40) & 0xFF));
		write((byte) ((l >>> 48) & 0xFF));
		write((byte) ((l >>> 56) & 0xFF));
		return this;
	}

	public LittleEndianWriter writeInt(int i) {
		write((byte) (i & 0xFF));
		write((byte) ((i >>> 8) & 0xFF));
		write((byte) ((i >>> 16) & 0xFF));
		write((byte) ((i >>> 24) & 0xFF));
		return this;
	}

	public LittleEndianWriter writeShort(short s) {
		write((byte) (s & 0xFF));
		write((byte) ((s >>> 8) & 0xFF));
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
		writeByte((byte) 0);
		return this;
	}

	public LittleEndianWriter writePaddedAsciiString(String str, int n) {
		int length = Math.min(str.length(), n);
		for (int x = 0; x < length; x++)
			writeChar(str.charAt(x));
		for (int i = n - length; i > 0; i--)
			writeByte((byte) 0);
		return this;
	}

	public LittleEndianWriter writeLengthPrefixedString(String str) {
		writeShort((short) str.length());
		writeBytes(str.getBytes(asciiEncoder));
		return this;
	}

	public LittleEndianWriter writeChar(char c) {
		writeByte((byte) c);
		return this;
	}

	public LittleEndianWriter writeBool(boolean b) {
		writeByte((byte) (b ? 1 : 0));
		return this;
	}

	public LittleEndianWriter writeBytes(byte[] b) {
		for (int i = 0; i < b.length; i++)
			writeByte(b[i]);
		return this;
	}
}
