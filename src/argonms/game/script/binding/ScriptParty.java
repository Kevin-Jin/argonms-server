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

package argonms.game.script.binding;

import argonms.game.character.GameCharacter;
import argonms.game.character.PartyList;

/**
 *
 * @author GoldenKevin
 */
public class ScriptParty {
	private final PartyList party;

	public ScriptParty(PartyList party) {
		this.party = party;
	}

	public int getLeader() {
		return party.getLeader();
	}

	public byte getMapPartyMembersCount(int mapId, short minLevel, short maxLevel) {
		byte count = 0;
		party.lockRead();
		try {
			for (GameCharacter c : party.getLocalMembersInMap(mapId))
				if (c.getLevel() >= minLevel && c.getLevel() <= maxLevel)
					count++;
		} finally {
			party.unlockRead();
		}
		return count;
	}
}
