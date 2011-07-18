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

package argonms.common.util.output;

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

	@Override
	public void write(byte b) {
		try {
			os.write(b);
		} catch (IOException ex) {
			LOG.log(Level.WARNING, "Could not write one byte to OutputStream.", ex);
		}
	}

	@Override
	protected void write(byte... bytes) {
		try {
			os.write(bytes);
		} catch (IOException ex) {
			LOG.log(Level.WARNING, "Could not write " + bytes.length + " bytes to OutputStream.", ex);
		}
	}

	@Override
	public void dispose() {
		try {
			os.close();
			os = null;
		} catch (IOException ex) {
			LOG.log(Level.WARNING, "Could not close OutputStream.", ex);
		}
	}
}
