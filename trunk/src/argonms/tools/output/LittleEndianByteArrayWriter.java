package argonms.tools.output;

import argonms.tools.HexTool;

/**
 *
 * @author GoldenKevin
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

	private void grow() {
		byte[] copy = new byte[data.length + data.length / 2];
		System.arraycopy(data, 0, copy, 0, data.length);
		data = copy;
	}

	public void write(byte b) {
		if (index == data.length)
			grow();

		data[index++] = b;
	}

	public void write(byte[] bytes) {
		if (index + bytes.length >= data.length + 1)
			grow();

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
