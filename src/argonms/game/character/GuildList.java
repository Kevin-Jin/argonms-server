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
public class GuildList extends IntraworldGroupList<
		GuildList.Member,
		GuildList.RemoteMember,
		GuildList.LocalMember> {
	public interface Member extends IntraworldGroupList.Member {
		public int getRank();
	}

	public static class RemoteMember extends IntraworldGroupList.RemoteMember implements Member {
		private int rank;

		public RemoteMember(int playerId, String name, short job, short level, byte channel, int rank) {
			super(playerId, name, job, level, channel);
			this.rank = rank;
		}

		public RemoteMember(Member copy, byte channel) {
			super(copy, channel);
		}

		@Override
		public int getRank() {
			return rank;
		}
	}

	public static class LocalMember extends IntraworldGroupList.LocalMember implements Member {
		private int rank;

		public LocalMember(GameCharacter player, int rank) {
			super(player);
			this.rank = rank;
		}

		@Override
		public int getRank() {
			return rank;
		}
	}

	public static class EmptyMember implements Member {
		private static EmptyMember instance = new EmptyMember();

		private EmptyMember() {
			
		}

		@Override
		public int getPlayerId() {
			return 0;
		}

		@Override
		public String getName() {
			return "";
		}

		@Override
		public short getJob() {
			return 0;
		}

		@Override
		public short getLevel() {
			return 0;
		}

		@Override
		public byte getChannel() {
			return OFFLINE_CH;
		}

		@Override
		public int getMapId() {
			return 0;
		}

		@Override
		public int getRank() {
			return 0;
		}

		public static EmptyMember getInstance() {
			return instance;
		}
	}

	@Override
	protected Member getEmptyMember() {
		return EmptyMember.getInstance();
	}

	public GuildList(int guildId) {
		super(guildId);
	}
}
