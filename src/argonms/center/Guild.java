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

/**
 *
 * @author GoldenKevin
 */
public class Guild extends IntraworldGroup<Guild.Member> {
	public static class Member extends IntraworldGroup.Member {
		private final byte rank;

		public Member(int playerId, String name, short job, short level, byte channel, byte rank) {
			super(playerId, name, job, level, channel);
			this.rank = rank;
		}

		public Member(IntraworldGroup.Member m, boolean leader) {
			this(m.getPlayerId(), m.getName(), m.getJob(), m.getLevel(), m.getChannel(), (byte) (leader ? 1 : 0));
		}

		public byte getRank() {
			return rank;
		}
	}

	private final String name;

	public Guild(String name, Party p) {
		super();
		this.name = name;

		for (IntraworldGroup.Member m : p.getAllMembers())
			addPlayer(new Member(m, p.getLeader() == m.getPlayerId()));
	}
}
