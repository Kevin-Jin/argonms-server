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

package argonms.loading.map;

import argonms.tools.input.LittleEndianByteArrayReader;
import argonms.tools.input.LittleEndianReader;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author GoldenKevin
 */
public class KvjMapDataLoader extends MapDataLoader {
	private static final Logger LOG = Logger.getLogger(KvjMapDataLoader.class.getName());

	private static final byte
		TOWN = 1,
		RETURN_MAP = 2,
		FORCED_RETURN = 3,
		MOB_RATE = 4,
		DEC_HP = 5,
		TIME_LIMIT = 6,
		PROTECT_ITEM = 7,
		EVERLAST = 8,
		LIFE = 9,
		AREA = 10,
		CLOCK = 11,
		BOAT = 12,
		REACTOR = 13,
		FOOTHOLD = 14,
		PORTAL = 15
	;

	private String dataPath;

	protected KvjMapDataLoader(String wzPath) {
		this.dataPath = wzPath;
	}

	protected void load(int mapid) {
		String id = String.format("%09d", mapid);

		MapStats stats = null;
		try {
			File f = new File(new StringBuilder(dataPath).append("Map.wz").append(File.separator).append("Map").append(File.separator).append("Map").append(id.substring(0, 1)).append(id).append(".img.kvj").toString());
			if (f.exists()) {
				stats = new MapStats();
				doWork(new LittleEndianByteArrayReader(f), stats);
			}
		} catch (IOException e) {
			LOG.log(Level.WARNING, "Could not read KVJ data file for map " + mapid, e);
		}
		mapStats.put(Integer.valueOf(mapid), stats);
	}

	public boolean loadAll() {
		try {
			File root = new File(dataPath + "Map.wz/Map");
			for (String cat : root.list()) {
				File prefFolder = new File(root.getAbsolutePath() + File.separatorChar + cat);
				for (String kvj : prefFolder.list()) {
					MapStats stats = new MapStats();
					doWork(new LittleEndianByteArrayReader(new File(prefFolder.getAbsolutePath() + File.separatorChar + kvj)), stats);
					//InputStream is = new BufferedInputStream(new FileInputStream(prefFolder.getAbsolutePath() + File.separatorChar + kvj));
					//doWork(new LittleEndianStreamReader(is), stats);
					//is.close();
					mapStats.put(Integer.valueOf(kvj.substring(0, kvj.lastIndexOf(".img.kvj"))), stats);
				}
			}
			return true;
		} catch (IOException ex) {
			LOG.log(Level.WARNING, "Could not load all map data from KVJ files.", ex);
			return false;
		}
	}

	public boolean canLoad(int mapid) {
		String id = String.format("%09d", mapid);
		File f = new File(new StringBuilder(dataPath).append("Map.wz").append(File.separator).append("Map").append(File.separator).append("Map").append(id.substring(0, 1)).append(id).append(".img.kvj").toString());
		return f.exists();
	}

	private void doWork(LittleEndianReader reader, MapStats stats) {
		for (byte now = reader.readByte(); now != -1; now = reader.readByte()) {
			switch (now) {
				case TOWN:
					stats.setTown();
					break;
				case RETURN_MAP:
					stats.setReturnMap(reader.readInt());
					break;
				case FORCED_RETURN:
					stats.setForcedReturn(reader.readInt());
					break;
				case MOB_RATE:
					stats.setMobRate(reader.readFloat());
					break;
				case DEC_HP:
					stats.setDecHp(reader.readInt());
					break;
				case TIME_LIMIT:
					stats.setTimeLimit(reader.readInt());
					break;
				case PROTECT_ITEM:
					stats.setProtectItem(reader.readInt());
					break;
				case EVERLAST:
					stats.setEverlast();
					break;
				case LIFE:
					processLife(reader, stats);
					break;
				case AREA:
					processArea(reader, stats);
					break;
				case CLOCK:
					stats.setClock();
					break;
				case BOAT:
					stats.setShip();
					break;
				case REACTOR:
					processReactor(reader, stats);
					break;
				case FOOTHOLD:
					processFoothold(reader, stats);
					break;
				case PORTAL:
					processPortal(reader, stats);
					break;
			}
		}
	}

	private void processLife(LittleEndianReader reader, MapStats stats) {
		int id = reader.readInt();
		Life l = new Life();
		l.setType(reader.readChar());
		l.setDataId(reader.readInt());
		l.setX(reader.readInt());
		l.setY(reader.readInt());
		l.setMobTime(reader.readInt());
		l.setF(reader.readBool());
		l.setHide(reader.readBool());
		l.setFoothold(reader.readInt());
		l.setCy(reader.readInt());
		l.setRx0(reader.readInt());
		l.setRx1(reader.readInt());
		stats.addLife(id, l);
	}

	private void processArea(LittleEndianReader reader, MapStats stats) {
		String id = reader.readNullTerminatedString();
		Area a = new Area();
		a.setX1(reader.readInt());
		a.setX2(reader.readInt());
		a.setY1(reader.readInt());
		a.setY2(reader.readInt());
		stats.addArea(id, a);
	}

	private void processReactor(LittleEndianReader reader, MapStats stats) {
		int id = reader.readInt();
		Reactor rt = new Reactor();
		rt.setDataId(reader.readInt());
		rt.setX(reader.readInt());
		rt.setY(reader.readInt());
		rt.setReactorTime(reader.readInt());
		rt.setName(reader.readNullTerminatedString());
		stats.addReactor(id, rt);
	}

	private void processFoothold(LittleEndianReader reader, MapStats stats) {
		int id = reader.readInt();
		Foothold fh = new Foothold();
		fh.setX1(reader.readInt());
		fh.setY1(reader.readInt());
		fh.setX2(reader.readInt());
		fh.setY2(reader.readInt());
		fh.setPrev(reader.readInt());
		fh.setNext(reader.readInt());
		stats.addFoothold(id, fh);
	}

	private void processPortal(LittleEndianReader reader, MapStats stats) {
		int id = reader.readInt();
		Portal p = new Portal();
		p.setPortalName(reader.readNullTerminatedString());
		p.setPortalType(reader.readInt());
		int x = reader.readInt();
		int y = reader.readInt();
		p.setPosition(x, y);
		p.setTargetMapId(reader.readInt());
		p.setTargetName(reader.readNullTerminatedString());
		p.setScript(reader.readNullTerminatedString());
		stats.addPortal(id, p);
	}
}
