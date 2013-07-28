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

package argonms.game.character;

import argonms.common.character.BuddyListEntry;
import argonms.common.character.ReadableBuddyList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 *
 * @author GoldenKevin
 */
public class BuddyList implements ReadableBuddyList {
	private short capacity;
	private final Map<Integer, BuddyListEntry> buddies;
	private final Map<Integer, String> pendingInvites;

	public BuddyList(short capacity) {
		this.capacity = capacity;
		this.buddies = new LinkedHashMap<Integer, BuddyListEntry>();
		this.pendingInvites = new LinkedHashMap<Integer, String>();
	}

	public BuddyListEntry getBuddy(int id) {
		return buddies.get(Integer.valueOf(id));
	}

	@Override
	public Collection<BuddyListEntry> getBuddies() {
		return Collections.unmodifiableCollection(buddies.values());
	}

	public void addBuddy(BuddyListEntry entry) {
		buddies.put(Integer.valueOf(entry.getId()), entry);
	}

	public BuddyListEntry removeBuddy(int id) {
		return buddies.remove(Integer.valueOf(id));
	}

	public void addInvite(int id, String name) {
		pendingInvites.put(Integer.valueOf(id), name);
	}

	public String removeInvite(int id) {
		return pendingInvites.remove(Integer.valueOf(id));
	}

	public Set<Entry<Integer, String>> getInvites() {
		return Collections.unmodifiableSet(pendingInvites.entrySet());
	}

	public boolean isInInvites(int id) {
		return pendingInvites.containsKey(Integer.valueOf(id));
	}

	public boolean isFull() {
		return buddies.size() >= capacity;
	}

	@Override
	public short getCapacity() {
		return capacity;
	}

	public void increaseCapacity(short delta) {
		capacity = (short) Math.min(capacity + delta, 0xFF);
	}
}
