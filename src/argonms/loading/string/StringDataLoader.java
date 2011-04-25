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

package argonms.loading.string;

import argonms.loading.DataFileType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 *
 * @author GoldenKevin
 */
public abstract class StringDataLoader {
	private static StringDataLoader instance;

	protected final Map<Integer, String> itemNames;
	protected final Map<Integer, String> skillNames;
	protected final Map<Integer, String> streetNames;
	protected final Map<Integer, String> mapNames;
	protected final Map<Integer, String> mobNames;
	protected final Map<Integer, String> npcNames;
	protected final Map<Integer, String> itemMsgs;

	protected StringDataLoader() {
		itemNames = new HashMap<Integer, String>();
		skillNames = new HashMap<Integer, String>();
		streetNames = new HashMap<Integer, String>();
		mapNames = new HashMap<Integer, String>();
		mobNames = new HashMap<Integer, String>();
		npcNames = new HashMap<Integer, String>();
		itemMsgs = new HashMap<Integer, String>();
	}

	public abstract boolean loadAll();

	public String getItemNameFromId(int itemid) {
		return itemNames.get(Integer.valueOf(itemid));
	}

	public String getSkillNameFromId(int skillid) {
		return skillNames.get(Integer.valueOf(skillid));
	}

	public String getStreetNameFromMapId(int mapid) {
		return streetNames.get(Integer.valueOf(mapid));
	}

	public String getMapNameFromId(int mapid) {
		return mapNames.get(Integer.valueOf(mapid));
	}

	public String getMobNameFromId(int mobid) {
		return mobNames.get(Integer.valueOf(mobid));
	}

	public String getNpcNameFromId(int npcid) {
		return npcNames.get(Integer.valueOf(npcid));
	}

	public List<String> getSimilarNamedItems(String reference) {
		List<String> retItems = new ArrayList<String>();
		for (Entry<Integer, String> name : itemNames.entrySet())
			if (name.getValue().toLowerCase().contains(reference.toLowerCase()))
				retItems.add(name.getKey() + " - " + name.getValue());
		return retItems;
	}

	public List<String> getSimilarNamedSkills(String reference) {
		List<String> retSkills = new ArrayList<String>();
		for (Entry<Integer, String> name : skillNames.entrySet())
			if (name.getValue().toLowerCase().contains(reference.toLowerCase()))
				retSkills.add(name.getKey() + " - " + name.getValue());
		return retSkills;
	}

	public List<String> getSimilarNamedMaps(String reference) {
		List<String> retMaps = new ArrayList<String>();
		for (Entry<Integer, String> name : mapNames.entrySet()) {
			String streetAndMap = streetNames != null ? streetNames.get(name.getKey()) : null;
			if (streetAndMap != null)
				streetAndMap += ": " + name.getValue();
			else
				streetAndMap = name.getValue();
			if (streetAndMap.toLowerCase().contains(reference.toLowerCase()))
				retMaps.add(name.getKey() + " - " + streetAndMap);
		}
		return retMaps;
	}

	public List<String> getSimilarNamedMobs(String reference) {
		List<String> retMobs = new ArrayList<String>();
		for (Entry<Integer, String> name : mobNames.entrySet())
			if (name.getValue().toLowerCase().contains(reference.toLowerCase()))
				retMobs.add(name.getKey() + " - " + name.getValue());
		return retMobs;
	}

	public List<String> getSimilarNamedNpcs(String reference) {
		List<String> retNpcs = new ArrayList<String>();
		for (Entry<Integer, String> name : npcNames.entrySet())
			if (name.getValue().toLowerCase().contains(reference.toLowerCase()))
				retNpcs.add(name.getKey() + " - " + name.getValue());
		return retNpcs;
	}

	public static void setInstance(DataFileType wzType, String wzPath) {
		if (instance == null) {
			switch (wzType) {
				case KVJ:
					instance = new KvjStringDataLoader(wzPath);
					break;
				case MCDB:
					instance = new McdbStringDataLoader();
					break;
			}
		}
	}

	public static StringDataLoader getInstance() {
		return instance;
	}
}
