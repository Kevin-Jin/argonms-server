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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 *
 * @author GoldenKevin
 */
public class GuildList extends IntraworldGroupList<
		GuildList.Member,
		GuildList.RemoteMember,
		GuildList.LocalMember> {
	public interface Member extends IntraworldGroupList.Member {
		public byte getRank();
		public byte getSignature();
		public byte getAllianceRank();
	}

	public static class RemoteMember extends IntraworldGroupList.RemoteMember implements Member {
		private final byte rank;
		private final byte signature;
		private final byte allianceRank;

		public RemoteMember(int playerId, String name, short job, short level, byte channel, byte rank, byte signature, byte allianceRank) {
			super(playerId, name, job, level, channel);
			this.rank = rank;
			this.signature = signature;
			this.allianceRank = allianceRank;
		}

		public RemoteMember(Member copy, byte channel) {
			super(copy, channel);
			this.rank = copy.getRank();
			this.signature = copy.getSignature();
			this.allianceRank = copy.getAllianceRank();
		}

		@Override
		public byte getRank() {
			return rank;
		}

		@Override
		public byte getSignature() {
			return signature;
		}

		@Override
		public byte getAllianceRank() {
			return allianceRank;
		}
	}

	public static class LocalMember extends IntraworldGroupList.LocalMember implements Member {
		private final byte rank;
		private final byte signature;
		private final byte allianceRank;

		public LocalMember(GameCharacter player, byte rank, byte signature, byte allianceRank) {
			super(player);
			this.rank = rank;
			this.signature = signature;
			this.allianceRank = allianceRank;
		}

		public LocalMember(IntraworldGroupList.LocalMember m, boolean leader) {
			this(m.getPlayer(), (byte) (leader ? 1 : 0), (byte) 0, (byte) 0);
		}

		@Override
		public byte getRank() {
			return rank;
		}

		@Override
		public byte getSignature() {
			return signature;
		}

		@Override
		public byte getAllianceRank() {
			return allianceRank;
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
		public byte getRank() {
			return 0;
		}

		public static EmptyMember getInstance() {
			return instance;
		}

		@Override
		public byte getSignature() {
			return 0;
		}

		@Override
		public byte getAllianceRank() {
			return 0;
		}
	}

	private final String name;
	private short emblemBg, emblemFg;
	private byte emblemBgC, emblemFgC;
	private String[] titles;
	private byte capacity;
	private String notice;
	private int gp;
	private int allianceId;

	public GuildList(int guildId, String name, PartyList p) {
		super(guildId);
		this.name = name;
		titles = new String[5];
		capacity = 10;
		notice = "";

		for (IntraworldGroupList.LocalMember m : p.getMembersInLocalChannel())
			addPlayer(new LocalMember(m, p.getLeader() == m.getPlayerId()));
	}

	@Override
	protected LocalMember createLocalMember(GameCharacter p) {
		return new LocalMember(p, (byte) 0, (byte) 0, (byte) 0);
	}

	@Override
	protected RemoteMember createRemoteMember(IntraworldGroupList.Member member, byte channel) {
		return new RemoteMember((Member) member, channel);
	}

	@Override
	public Member[] getAllMembers() {
		List<Member> list = new ArrayList<Member>();
		list.addAll(localMembers.values());
		for (Map<Integer, RemoteMember> channel : remoteMembers.values())
			list.addAll(channel.values());
		return list.toArray(new Member[list.size()]);
	}

	@Override
	public Member getMember(int playerId) {
		Member member = localMembers.get(Integer.valueOf(playerId));
		if (member != null)
			return member;
		for (Map<Integer, RemoteMember> channel : remoteMembers.values()) {
			member = channel.get(Integer.valueOf(playerId));
			if (member != null)
				return member;
		}
		return null;
	}

	public String getName() {
		return name;
	}

	public short getEmblemBackground() {
		return emblemBg;
	}

	public byte getEmblemBackgroundColor() {
		return emblemBgC;
	}

	public short getEmblemDesign() {
		return emblemFg;
	}

	public byte getEmblemDesignColor() {
		return emblemFgC;
	}

	public String getTitle(byte index) {
		return titles[index];
	}

	public byte getCapacity() {
		return capacity;
	}

	public String getNotice() {
		return notice;
	}

	public int getGp() {
		return gp;
	}

	public int getAllianceId() {
		return allianceId;
	}
}
