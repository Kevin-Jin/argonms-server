package argonms.tools.input;

/**
 *
 * @author GoldenKevin
 */
public abstract class LittleEndianReader {
	protected abstract int read();
	protected abstract byte[] read(int amount);
	protected abstract void skip(int amount);
	public abstract int available();
	public abstract void dispose();
	
	public long readLong() {
		long b1, b2, b3, b4, b5, b6, b7, b8;
		
		b1 = read();
		b2 = read();
		b3 = read();
		b4 = read();
		b5 = read();
		b6 = read();
		b7 = read();
		b8 = read();
		
		return (b8 << 56) + (b7 << 48) + (b6 << 40) + (b5 << 32) + (b4 << 24) + (b3 << 16) + (b2 << 8) + b1;
	}
	
	public int readInt() {
		int b1, b2, b3, b4;
		
		b1 = read();
		b2 = read();
		b3 = read();
		b4 = read();
		
		return (b4 << 24) + (b3 << 16) + (b2 << 8) + b1;
	}
	
	public short readShort() {
		int b1, b2;
		
		b1 = read();
		b2 = read();
		
		return (short) ((b2 << 8) + b1);
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
		
		for (byte current = readByte(); current != 0; current = readByte())
			builder.append((char) current);
		
		return builder.toString();
	}
	
	public String readPaddedAsciiString(int n) {
		char ret[] = new char[n];
		for (int x = 0; x < n; x++)
			ret[x] = readChar();
		return String.valueOf(ret);
	}
	
	public String readLengthPrefixedString() {
		short length = readShort();
		char ret[] = new char[length];
		for (int x = 0; x < length; x++)
			ret[x] = readChar();
		return String.valueOf(ret);
	}
	
	public char readChar() {
		return (char) readByte();
	}
	
	public boolean readBool() {
		return (readByte() > 0);
	}
	
	public byte[] readBytes(int size) {
		byte[] bArray = new byte[size];
		for (int i = 0; i < size; i++)
			bArray[i] = readByte();
		return bArray;
	}
}
