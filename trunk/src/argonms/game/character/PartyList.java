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

package argonms.game.character;

/**
 *
 * @author GoldenKevin
 */
public class PartyList extends IntraworldGroupList<
		IntraworldGroupList.Member,
		IntraworldGroupList.RemoteMember,
		IntraworldGroupList.LocalMember> {
	private volatile int leader;

	public PartyList(int partyId) {
		super(partyId);
	}

	public PartyList(int partyId, GameCharacter creator) {
		this(partyId);
		this.leader = creator.getId();
		addPlayer(new LocalMember(creator), false);
	}

	@Override
	protected Member getEmptyMember() {
		return EmptyMember.getInstance();
	}

	/**
	 * This PartyList does NOT need to be locked when this method is called.
	 */
	public int getLeader() {
		return leader;
	}

	/**
	 * This PartyList does NOT need to be locked when this method is called.
	 */
	public void setLeader(int leader) {
		this.leader = leader;
	}
}
