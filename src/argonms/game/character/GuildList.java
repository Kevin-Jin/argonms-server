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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
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
	private static final byte OFFLINE_CH = 0;

	public interface Member extends IntraworldGroupList.Member {
		public byte getRank();
		public byte getSignature();
		public byte getAllianceRank();
		public void setRank(byte rank);
	}

	public static class RemoteMember extends IntraworldGroupList.RemoteMember implements Member {
		private byte rank;
		private final byte signature;
		private final byte allianceRank;

		public RemoteMember(int playerId, String name, short job, short level, byte channel, byte rank, byte signature, byte allianceRank) {
			super(playerId, name, job, level, channel);
			this.rank = rank;
			this.signature = signature;
			this.allianceRank = allianceRank;
		}

		public RemoteMember(int playerId, String name, short job, short level, byte channel) {
			this(playerId, name, job, level, channel, (byte) 3, (byte) 0, (byte) 0);
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

		@Override
		public void setRank(byte rank) {
			this.rank = rank;
		}
	}

	public static class LocalMember extends IntraworldGroupList.LocalMember implements Member {
		private byte rank;
		private final byte signature;
		private final byte allianceRank;

		public LocalMember(GameCharacter player, byte rank, byte signature, byte allianceRank) {
			super(player);
			this.rank = rank;
			this.signature = signature;
			this.allianceRank = allianceRank;
		}

		public LocalMember(GameCharacter player) {
			this(player, (byte) 3, (byte) 0, (byte) 0);
		}

		public LocalMember(IntraworldGroupList.LocalMember m, boolean leader) {
			this(m.getPlayer(), (byte) (leader ? 1 : 3), (byte) 0, (byte) 0);
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

		@Override
		public void setRank(byte rank) {
			this.rank = rank;
		}
	}

	private String name;
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
		titles = new String[] { "Master", "Jr.Master", "Member", "", "" };
		capacity = 10;
		notice = "";

		for (IntraworldGroupList.LocalMember m : p.getMembersInLocalChannel())
			addPlayer(new LocalMember(m, p.getLeader() == m.getPlayerId()));
	}

	public GuildList(int guildId) {
		super(guildId);
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setEmblem(short background, byte backgroundColor, short foreground, byte foregroundColor) {
		this.emblemBg = background;
		this.emblemBgC = backgroundColor;
		this.emblemFg = foreground;
		this.emblemFgC = foregroundColor;
	}

	public void setTitles(String[] titles) {
		this.titles = titles;
	}

	public void setCapacity(byte capacity) {
		this.capacity = capacity;
	}

	public void setNotice(String notice) {
		this.notice = notice;
	}

	public void setGp(int gp) {
		this.gp = gp;
	}

	public void setAlliance(int allianceId) {
		this.allianceId = allianceId;
	}

	private byte getLowestRank() {
		byte i;
		for (i = 5; i >= 4 && titles[i - 1].isEmpty(); --i);
		return i;
	}

	@Override
	protected LocalMember createLocalMember(GameCharacter p) {
		LocalMember member = new LocalMember(p);
		Member existing = getMember(member.getPlayerId());
		if (existing != null)
			member.setRank(existing.getRank());
		else
			member.setRank(getLowestRank());
		return member;
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
		Member[] array = list.toArray(new Member[list.size()]);
		Arrays.sort(array, new Comparator<Member>() {
			@Override
			public int compare(Member o1, Member o2) {
				int delta = o1.getRank() - o2.getRank();
				if (delta == 0)
					delta = o1.getName().compareTo(o2.getName());
				return delta;
			}
		});
		return array;
	}

	@Override
	public boolean allOffline() {
		return localMembers.isEmpty() && remoteMembers.size() == 1 && remoteMembers.containsKey(Byte.valueOf(OFFLINE_CH));
	}

	@Override
	protected void removeFromOffline(IntraworldGroupList.Member member) {
		Map<Integer, RemoteMember> others = remoteMembers.get(Byte.valueOf(OFFLINE_CH));
		others.remove(Integer.valueOf(member.getPlayerId()));
		if (others.isEmpty())
			remoteMembers.remove(Byte.valueOf(OFFLINE_CH));
	}

	@Override
	protected RemoteMember addToOffline(IntraworldGroupList.Member member) {
		Map<Integer, RemoteMember> others = remoteMembers.get(Byte.valueOf(OFFLINE_CH));
		if (others == null) {
			others = new HashMap<Integer, RemoteMember>();
			remoteMembers.put(Byte.valueOf(OFFLINE_CH), others);
		}
		RemoteMember offlineMember = createRemoteMember(member, OFFLINE_CH);
		others.put(Integer.valueOf(member.getPlayerId()), offlineMember);
		return offlineMember;
	}

	@Override
	public RemoteMember getOfflineMember(int playerId) {
		return remoteMembers.get(Byte.valueOf(OFFLINE_CH)).get(Integer.valueOf(playerId));
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

	@Override
	public boolean isFull() {
		int count = localMembers.size();
		for (Map<Integer, RemoteMember> channel : remoteMembers.values())
			count += channel.size();
		return count >= capacity;
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
