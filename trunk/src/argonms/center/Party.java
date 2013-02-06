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

package argonms.center;

/**
 *
 * @author GoldenKevin
 */
public class Party extends IntraworldGroup<IntraworldGroup.Member> {
	public static final byte OFFLINE_CH = -1;

	private volatile int leader;

	public Party() {
		super();
	}

	public Party(Member creator) {
		this();

		this.leader = creator.getPlayerId();
		//no locking necessary as long as we don't leak this reference
		addPlayer(creator);
	}

	/**
	 * This Party does NOT need to be locked when this method is called.
	 * @param leader 
	 */
	public void setLeader(int leader) {
		this.leader = leader;
	}

	/**
	 * This Party does NOT need to be locked when this method is called.
	 */
	public int getLeader() {
		return leader;
	}

	@Override
	public byte getCapacity() {
		return 6;
	}
}