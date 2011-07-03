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

package argonms.common.loading.string;

import argonms.common.tools.input.LittleEndianByteArrayReader;
import argonms.common.tools.input.LittleEndianReader;
import java.io.File;
import java.io.IOException;

/**
 *
 * @author GoldenKevin
 */
public class KvjStringDataLoader extends StringDataLoader {
	private static final byte
		NEXT = 1,
		NAME = 2,
		MAP_NAME = 3,
		STREET_NAME = 4,
		MSG = 5
	;

	private String dataPath;

	protected KvjStringDataLoader(String wzPath) {
		this.dataPath = wzPath;
	}

	public boolean loadAll() {
		String dir = dataPath + "String.wz" + File.separatorChar;
		LittleEndianReader reader;
		Integer key;
		try {
			for (String s : new String[] { "Eqp", "Consume", "Ins", "Etc", "Cash", "Pet" }) {
				reader = new LittleEndianByteArrayReader(new File(dir + s + ".img.kvj"));
				key = null;
				for (byte now = reader.readByte(); now != -1; now = reader.readByte()) {
					switch (now) {
						case NEXT:
							key = Integer.valueOf(reader.readInt());
							break;
						case NAME:
							itemNames.put(key, reader.readNullTerminatedString());
							break;
						case MSG:
							itemMsgs.put(key, reader.readNullTerminatedString());
							break;
					}
				}
			}
			reader = new LittleEndianByteArrayReader(new File(dir + "Map.img.kvj"));
			key = null;
			for (byte now = reader.readByte(); now != -1; now = reader.readByte()) {
				switch (now) {
					case NEXT:
						key = Integer.valueOf(reader.readInt());
						break;
					case MAP_NAME:
						mapNames.put(key, reader.readNullTerminatedString());
						break;
					case STREET_NAME:
						streetNames.put(key, reader.readNullTerminatedString());
						break;
				}
			}
			reader = new LittleEndianByteArrayReader(new File(dir + "Mob.img.kvj"));
			key = null;
			for (byte now = reader.readByte(); now != -1; now = reader.readByte()) {
				switch (now) {
					case NEXT:
						key = Integer.valueOf(reader.readInt());
						break;
					case NAME:
						mobNames.put(key, reader.readNullTerminatedString());
						break;
				}
			}
			reader = new LittleEndianByteArrayReader(new File(dir + "Npc.img.kvj"));
			key = null;
			for (byte now = reader.readByte(); now != -1; now = reader.readByte()) {
				switch (now) {
					case NEXT:
						key = Integer.valueOf(reader.readInt());
						break;
					case NAME:
						npcNames.put(key, reader.readNullTerminatedString());
						break;
				}
			}
			reader = new LittleEndianByteArrayReader(new File(dir + "Skill.img.kvj"));
			key = null;
			for (byte now = reader.readByte(); now != -1; now = reader.readByte()) {
				switch (now) {
					case NEXT:
						key = Integer.valueOf(reader.readInt());
						break;
					case NAME:
						skillNames.put(key, reader.readNullTerminatedString());
						break;
				}
			}
			return true;
		} catch (IOException ex) {
			return false;
		}
	}
}
