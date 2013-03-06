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
public class Guild extends IntraworldGroup<Guild.Member> {
	public static final byte OFFLINE_CH = 0;

	public static class Member extends IntraworldGroup.Member {
		private byte rank;
		private final byte signature;
		private final byte allianceRank;

		public Member(int playerId, String name, short job, short level, byte channel, byte rank, byte signature, byte allianceRank) {
			super(playerId, name, job, level, channel);
			this.rank = rank;
			this.signature = signature;
			this.allianceRank = allianceRank;
		}

		public Member(int playerId, String name, short job, short level, byte channel, byte rank) {
			this(playerId, name, job, level, channel, rank, (byte) 0, (byte) 0);
		}

		public Member(IntraworldGroup.Member m, boolean leader) {
			this(m.getPlayerId(), m.getName(), m.getJob(), m.getLevel(), m.getChannel(), (byte) (leader ? 1 : 3), (byte) 0, (byte) 0);
		}

		public byte getRank() {
			return rank;
		}

		public byte getSignature() {
			return signature;
		}

		public byte getAllianceRank() {
			return allianceRank;
		}

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
	private int alliance;

	public Guild(String name, Party p) {
		super();
		this.name = name;
		titles = new String[] { "Master", "Jr.Master", "Member", "", "" };
		capacity = 10;
		notice = "";

		for (IntraworldGroup.Member m : p.getAllMembers())
			addPlayer(new Member(m, p.getLeader() == m.getPlayerId()));
	}

	public Guild() {
		super();
	}

	public byte getLowestRank() {
		byte i;
		for (i = 5; i >= 4 && titles[i - 1].isEmpty(); --i);
		return i;
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

	public void setTitles(String csv) {
		this.titles = csv.split(",", -1);
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
		this.alliance = allianceId;
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

	@Override
	public byte getCapacity() {
		return capacity;
	}

	public String getNotice() {
		return notice;
	}

	public int getGp() {
		return gp;
	}

	public int getAlliance() {
		return alliance;
	}
}
