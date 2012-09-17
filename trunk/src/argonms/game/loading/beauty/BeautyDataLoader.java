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

import argonms.common.loading.DataFileType;
import java.util.Collections;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 *
 * @author GoldenKevin
 */
public abstract class BeautyDataLoader {
	private static BeautyDataLoader instance;

	protected final SortedSet<Short> eyeStyles;
	protected final SortedSet<Short> hairStyles;

	protected BeautyDataLoader() {
		eyeStyles = new TreeSet<Short>();
		hairStyles = new TreeSet<Short>();
	}

	public abstract boolean loadAll();

	public Set<Short> getMaleFaces() {
		return Collections.unmodifiableSet(eyeStyles.headSet(Short.valueOf((short) 21000)));
	}

	public Set<Short> getFemaleFaces() {
		return Collections.unmodifiableSet(eyeStyles.tailSet(Short.valueOf((short) 21000)));
	}

	public Set<Short> getMaleHairs() {
		return Collections.unmodifiableSet(hairStyles.headSet(Short.valueOf((short) 31000)));
	}

	public Set<Short> getFemaleHairs() {
		return Collections.unmodifiableSet(hairStyles.tailSet(Short.valueOf((short) 31000)));
	}

	public static void setInstance(DataFileType wzType, String wzPath) {
		if (instance == null) {
			switch (wzType) {
				case KVJ:
					instance = new KvjBeautyDataLoader(wzPath);
					break;
				case MCDB:
					instance = new McdbBeautyDataLoader();
					break;
			}
		}
	}

	public static BeautyDataLoader getInstance() {
		return instance;
	}
}
