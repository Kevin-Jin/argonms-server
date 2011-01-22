package argonms.tools.input;

import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author GoldenKevin
 */
public class LittleEndianStreamReader extends LittleEndianReader {
	private static final Logger LOG = Logger.getLogger(LittleEndianStreamReader.class.getName());
	
	private InputStream is;

	//Please please please please PLEASE input a BufferedInputStream unless
	//you perform your own buffering by using the "byte[] read(int)" method.
	//We don't need this class to use 100% of our CPU for each byte read.
	public LittleEndianStreamReader(InputStream is) {
		this.is = is;
	}

	protected int read() {
		try {
			return is.read() & 0xFF;
		} catch (IOException ex) {
			LOG.log(Level.WARNING, "Could not read one byte from InputStream.", ex);
			return -1;
		}
	}

	protected byte[] read(int amount) {
		byte[] ret = new byte[amount];
		try {
			is.read(ret);
		} catch (IOException ex) {
			LOG.log(Level.WARNING, "Could not read " + amount + " bytes from InputStream.", ex);
		}
		return ret;
	}

	protected void skip(int amount) {
		try {
			is.skip(amount);
		} catch (IOException ex) {
			LOG.log(Level.WARNING, "Could not skip " + amount + " bytes from InputStream.", ex);
		}
	}

	public int available() {
		try {
			return is.available();
		} catch (IOException ex) {
			LOG.log(Level.WARNING, "Could not determine amount of remaining bytes in InputStream.", ex);
			return 0;
		}
	}

	public void dispose() {
		try {
			is.close();
			is = null;
		} catch (IOException ex) {
			LOG.log(Level.WARNING, "Could not close InputStream.", ex);
		}
	}
}
