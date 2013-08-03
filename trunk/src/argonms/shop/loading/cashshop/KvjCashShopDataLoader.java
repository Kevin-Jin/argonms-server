/*
 * ArgonMS MapleStory server emulator written in Java
 * Copyright (C) 2011-2013  GoldenKevin
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

package argonms.shop.loading.cashshop;

import argonms.common.util.input.LittleEndianByteArrayReader;
import argonms.common.util.input.LittleEndianReader;
import java.io.File;
import java.io.IOException;

/**
 *
 * @author GoldenKevin
 */
public class KvjCashShopDataLoader extends CashShopDataLoader {
	private final String dataPath;

	protected KvjCashShopDataLoader(String wzPath) {
		this.dataPath = wzPath;
	}

	@Override
	public boolean loadAll() {
		String dir = dataPath + "Etc.wz" + File.separatorChar;
		LittleEndianReader reader;
		try {
			reader = new LittleEndianByteArrayReader(new File(dir + "Commodity.img.kvj"));
			for (int serialNumber = reader.readInt(); serialNumber != -1; serialNumber = reader.readInt())
				commodities.put(Integer.valueOf(serialNumber), new Commodity(reader.readInt(), reader.readShort(), reader.readInt(), reader.readByte(), reader.readByte(), reader.readBool()));

			reader = new LittleEndianByteArrayReader(new File(dir + "CashPackage.img.kvj"));
			for (int packageNumber = reader.readInt(); packageNumber != -1; packageNumber = reader.readInt()) {
				int[] serialNumbers = new int[reader.readByte()];
				for (int i = 0; i < serialNumbers.length; i++)
					serialNumbers[i] = reader.readInt();
				packages.put(Integer.valueOf(packageNumber), serialNumbers);
			}
			return true;
		} catch (IOException ex) {
			return false;
		}
	}
}
