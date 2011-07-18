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

import argonms.common.util.HexTool;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 *
 * @author GoldenKevin
 * @version 1.1
 */
public class LittleEndianByteArrayReader extends LittleEndianReader {
	private byte[] bytes;
	private int index;

	public LittleEndianByteArrayReader(byte[] bytes) {
		this.bytes = bytes;
		this.index = 0;
	}

	public LittleEndianByteArrayReader(File f) throws IOException {
		InputStream is = new FileInputStream(f);
		long length = f.length();

		try {
			if (f.length() > Integer.MAX_VALUE)
				throw new IOException("File is too large to be stored in a byte array");
			try {
				bytes = new byte[(int)length];
			} catch (OutOfMemoryError e) { //Requested array size exceeds VM limit
				throw new IOException("File is too large to be stored in a byte array");
			}

			// Read in the bytes
			int offset = 0;
			for (int numRead = 0; (offset < bytes.length) && ((numRead = is.read(bytes, offset, bytes.length-offset)) >= 0); offset += numRead);

			// Ensure all the bytes have been read in
			if (offset < bytes.length)
				throw new IOException("Unable to completely read file " + f.getName() + ". Read " + offset + " out of " + bytes.length + " bytes.");
		} finally {
			is.close();
		}
	}

	@Override
	protected int read() {
		if (index >= bytes.length)
			return -1;
		return bytes[index++] & 0xFF;
	}

	@Override
	protected byte[] read(int amount) {
		byte[] ret = new byte[amount];
		System.arraycopy(bytes, index, ret, 0, Math.min(available(), amount));
		index += amount;
		return ret;
	}

	@Override
	public void skip(int amount) {
		index += amount;
	}

	@Override
	public int available() {
		return bytes.length - index;
	}

	@Override
	public void dispose() {
		bytes = null;
	}

	@Override
	public int readInt() {
		return (read() + (read() << 8) + (read() << 16) + (read() << 24));
	}

	@Override
	public short readShort() {
		return (short) (read() + (read() << 8));
	}

	/**
	 * Copies the remaining portion of the stream to a byte array
	 * @return the remaining bytes in the stream
	 */
	public byte[] remaining() {
		byte[] trimmed = new byte[bytes.length - index];
		System.arraycopy(bytes, index, trimmed, 0, trimmed.length);
		return trimmed;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("All Bytes: ").append(HexTool.toString(bytes));
		byte[] remaining = remaining();
		if (remaining.length != 0)
			sb.append("\nRemaining: ").append(HexTool.toString(remaining));
		return sb.toString();
	}
}
