/*
 * ArgonMS MapleStory server emulator written in Java
 * Copyright (C) 2011-2012  GoldenKevin
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

package argonms.game.loading.beauty;

import argonms.common.util.input.LittleEndianByteArrayReader;
import argonms.common.util.input.LittleEndianReader;
import java.io.File;
import java.io.IOException;

/**
 *
 * @author GoldenKevin
 */
public class KvjBeautyDataLoader extends BeautyDataLoader {
	private final String dataPath;

	protected KvjBeautyDataLoader(String wzPath) {
		this.dataPath = wzPath;
	}

	@Override
	public boolean loadAll() {
		String dir = dataPath + "Character.wz" + File.separatorChar;
		try {
			LittleEndianReader reader = new LittleEndianByteArrayReader(new File(dir + "Face.kvj"));
			while (reader.available() != 0)
				eyeStyles.add(Short.valueOf(reader.readShort()));
			reader = new LittleEndianByteArrayReader(new File(dir + "Hair.kvj"));
			while (reader.available() != 0)
				hairStyles.add(Short.valueOf(reader.readShort()));
			return true;
		} catch (IOException ex) {
			return false;
		}
	}
}
