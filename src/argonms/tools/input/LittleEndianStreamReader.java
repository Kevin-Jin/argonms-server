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
			return is.read();
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

	public void skip(int amount) {
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
