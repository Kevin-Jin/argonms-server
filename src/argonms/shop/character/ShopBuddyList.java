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

package argonms.shop.character;

import argonms.common.character.BuddyListEntry;
import argonms.common.character.ReadableBuddyList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 *
 * @author GoldenKevin
 */
public class ShopBuddyList implements ReadableBuddyList {
	private final short capacity;
	private final List<BuddyListEntry> buddies;

	public ShopBuddyList(short capacity, List<BuddyListEntry> buddies) {
		this.capacity = capacity;
		this.buddies = Collections.unmodifiableList(buddies);
	}

	@Override
	public Collection<BuddyListEntry> getBuddies() {
		return buddies;
	}

	@Override
	public short getCapacity() {
		return capacity;
	}
}
