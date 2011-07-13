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

package argonms.game.loading.npc;

import argonms.common.tools.input.LittleEndianByteArrayReader;
import argonms.common.tools.input.LittleEndianReader;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author GoldenKevin
 */
public class KvjNpcDataLoader extends NpcDataLoader {
	private static final Logger LOG = Logger.getLogger(KvjNpcDataLoader.class.getName());

	private static final byte
		SCRIPT_NAME = 1,
		TRUNK_PUT = 2,
		TRUNK_GET = 3
	;

	private String dataPath;

	protected KvjNpcDataLoader(String wzPath) {
		this.dataPath = wzPath;
	}

	protected void load(int npcId) {
		String id = String.format("%07d", npcId);

		try {
			File f = new File(new StringBuilder(dataPath).append("Npc.wz").append(File.separator).append(id).append(".img.kvj").toString());
			if (f.exists())
				doWork(new LittleEndianByteArrayReader(f), npcId);
		} catch (IOException e) {
			LOG.log(Level.WARNING, "Could not read KVJ data file for NPC " + npcId, e);
		}
		loaded.add(Integer.valueOf(npcId));
	}

	public boolean loadAll() {
		try {
			File root = new File(dataPath + "Npc.wz");
			for (String kvj : root.list()) {
				int npcId = Integer.parseInt(kvj.substring(0, kvj.lastIndexOf(".img.kvj")));
				doWork(new LittleEndianByteArrayReader(new File(root.getAbsolutePath() + File.separatorChar + kvj)), npcId);
				loaded.add(Integer.valueOf(npcId));
				//InputStream is = new BufferedInputStream(new FileInputStream(root.getAbsolutePath() + File.separatorChar + kvj));
				//storageCosts.put(Integer.valueOf(npcId), doWork(new LittleEndianStreamReader(is)));
				//is.close();
			}
			return true;
		} catch (IOException ex) {
			LOG.log(Level.WARNING, "Could not load all NPC data from KVJ files.", ex);
			return false;
		}
	}

	private void doWork(LittleEndianReader reader, int npcId) {
		int withdrawCost = 0, depositCost = 0;
		for (byte now = reader.readByte(); now != -1; now = reader.readByte()) {
			switch (now) {
				case SCRIPT_NAME:
					reader.readNullTerminatedString();
					break;
				case TRUNK_PUT:
					depositCost = reader.readInt();
					break;
				case TRUNK_GET:
					withdrawCost = reader.readInt();
					break;
			}
		}
		if (withdrawCost != 0 || depositCost != 0)
			storageCosts.put(Integer.valueOf(npcId), new NpcStorageKeeper(depositCost, withdrawCost));
	}
}
