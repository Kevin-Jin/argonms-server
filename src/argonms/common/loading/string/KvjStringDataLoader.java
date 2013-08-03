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

package argonms.common.loading.string;

import argonms.common.util.input.LittleEndianByteArrayReader;
import argonms.common.util.input.LittleEndianReader;
import java.io.File;
import java.io.IOException;

/**
 *
 * @author GoldenKevin
 */
public class KvjStringDataLoader extends StringDataLoader {
	private final String dataPath;

	protected KvjStringDataLoader(String wzPath) {
		this.dataPath = wzPath;
	}

	@Override
	public boolean loadAll() {
		String dir = dataPath + "String.wz" + File.separatorChar;
		LittleEndianReader reader;
		Integer key;
		String str;
		try {
			reader = new LittleEndianByteArrayReader(new File(dir + "Cash.img.kvj"));
			for (int id = reader.readInt(); id != -1; id = reader.readInt()) {
				key = Integer.valueOf(id);
				str = reader.readNullTerminatedString();
				if (!str.isEmpty())
					itemNames.put(key, str);
				str = reader.readNullTerminatedString();
				if (!str.isEmpty())
					itemMsgs.put(key, str);
			}
			for (String s : new String[] { "Eqp", "Consume", "Ins", "Etc", "Pet" }) {
				reader = new LittleEndianByteArrayReader(new File(dir + s + ".img.kvj"));
				for (int id = reader.readInt(); id != -1; id = reader.readInt()) {
					key = Integer.valueOf(id);
					str = reader.readNullTerminatedString();
					if (!str.isEmpty())
						itemNames.put(key, str);
				}
			}
			reader = new LittleEndianByteArrayReader(new File(dir + "Map.img.kvj"));
			for (int id = reader.readInt(); id != -1; id = reader.readInt()) {
				key = Integer.valueOf(id);
				str = reader.readNullTerminatedString();
				if (!str.isEmpty())
					mapNames.put(key, str);
				str = reader.readNullTerminatedString();
				if (!str.isEmpty())
					streetNames.put(key, str);
			}
			reader = new LittleEndianByteArrayReader(new File(dir + "Mob.img.kvj"));
			for (int id = reader.readInt(); id != -1; id = reader.readInt()) {
				key = Integer.valueOf(id);
				str = reader.readNullTerminatedString();
				if (!str.isEmpty())
					mobNames.put(key, str);
			}
			reader = new LittleEndianByteArrayReader(new File(dir + "Npc.img.kvj"));
			for (int id = reader.readInt(); id != -1; id = reader.readInt()) {
				key = Integer.valueOf(id);
				str = reader.readNullTerminatedString();
				if (!str.isEmpty())
					npcNames.put(key, str);
			}
			reader = new LittleEndianByteArrayReader(new File(dir + "Skill.img.kvj"));
			for (int id = reader.readInt(); id != -1; id = reader.readInt()) {
				key = Integer.valueOf(id);
				str = reader.readNullTerminatedString();
				if (!str.isEmpty())
					skillNames.put(key, str);
			}
			return true;
		} catch (IOException ex) {
			return false;
		}
	}
}
