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

import argonms.tools.HexTool;

/**
 *
 * @author GoldenKevin
 * @version 1.1
 */
public class LittleEndianByteArrayWriter extends LittleEndianWriter {
	private byte[] data;
	private int index;

	public LittleEndianByteArrayWriter(int size) {
		this.data = new byte[size];
		this.index = 0;
	}

	public LittleEndianByteArrayWriter() {
		this(32);
	}

	private void grow(int min) {
		int increase = Math.max(data.length / 2, min);
		byte[] copy = new byte[data.length + increase];
		System.arraycopy(data, 0, copy, 0, index);
		data = copy;
	}

	public void write(byte b) {
		if (index == data.length)
			grow(1);

		data[index++] = b;
	}

	public void write(byte... bytes) {
		if (index + bytes.length >= data.length + 1)
			grow(bytes.length);

		System.arraycopy(bytes, 0, data, index, bytes.length);
		index += bytes.length;
	}

	public byte[] getBytes() {
		if (index == data.length)
			return data;

		byte[] trimmed = new byte[index];
		System.arraycopy(data, 0, trimmed, 0, index);
		return trimmed;
	}

	public void dispose() {
		data = null;
	}

	public String toString() {
		return HexTool.toString(getBytes());
	}
}
