package argonms.tools.output;

import java.io.IOException;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author GoldenKevin
 */
public class LittleEndianStreamWriter extends LittleEndianWriter {
	private static final Logger LOG = Logger.getLogger(LittleEndianStreamWriter.class.getName());

	private OutputStream os;

	//Please please please please PLEASE input a BufferedOutputStream unless
	//you perform your own buffering by using the "void write(byte[])" method.
	//We don't need this class to use 100% of our CPU for each byte written.
	public LittleEndianStreamWriter(OutputStream os) {
		this.os = os;
	}

	public void write(byte b) {
		try {
			os.write(b);
		} catch (IOException ex) {
			LOG.log(Level.WARNING, "Could not write one byte to OutputStream.", ex);
		}
	}

	protected void write(byte[] bytes) {
		try {
			os.write(bytes);
		} catch (IOException ex) {
			LOG.log(Level.WARNING, "Could not write " + bytes.length + " bytes to OutputStream.", ex);
		}
	}

	public void dispose() {
		try {
			os.close();
			os = null;
		} catch (IOException ex) {
			LOG.log(Level.WARNING, "Could not close OutputStream.", ex);
		}
	}
}
