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

package argonms.center;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 *
 * @author GoldenKevin
 */
public class Parties {
	private final AtomicInteger nextPartyId;
	private final Map<Integer, Party> parties;

	public Parties() {
		nextPartyId = new AtomicInteger(0);
		parties = new ConcurrentHashMap<Integer, Party>();
	}

	/**
	 * 
	 * @param creator
	 * @return the unique partyId of the newly created party
	 */
	public int makeNewParty(Party.Member creator) {
		//make sure we never return 0, because the client treats 0 specially
		int partyId = nextPartyId.incrementAndGet();
		parties.put(Integer.valueOf(partyId), new Party(creator));
		return partyId;
	}

	public Party remove(int partyId) {
		return parties.remove(Integer.valueOf(partyId));
	}

	public Party get(int partyId) {
		return parties.get(Integer.valueOf(partyId));
	}
}
