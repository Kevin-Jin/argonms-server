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
	private final Member[] allMembers;
	private volatile int leader;

	public PartyList(int partyId) {
		super(partyId);
		this.allMembers = new Member[6];
		for (int i = 0; i < 6; i++)
			allMembers[i] = EmptyMember.getInstance();
	}

	public PartyList(int partyId, GameCharacter creator) {
		this(partyId);
		this.leader = creator.getId();
		addPlayer(new LocalMember(creator), false);
	}

	@Override
	protected LocalMember createLocalMember(GameCharacter p) {
		return new LocalMember(p);
	}

	@Override
	protected RemoteMember createRemoteMember(Member member, byte channel) {
		return new RemoteMember(member, channel);
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

	private void removeFromAllMembersAndCollapse(int playerId) {
		for (int i = 0; i < 6; i++) {
			if (allMembers[i].getPlayerId() == playerId) {
				for (; i < 5 && allMembers[i + 1].getPlayerId() != 0; i++)
					allMembers[i] = allMembers[i + 1];
				allMembers[i] = EmptyMember.getInstance();
				break;
			}
		}
	}

	private void addToAllMembers(Member member) {
		for (int i = 0; i < 6; i++) {
			if (allMembers[i].getPlayerId() == 0) {
				allMembers[i] = member;
				break;
			}
		}
	}

	private void syncWithAllMembers(Member member) {
		for (int i = 0; i < 6; i++) {
			if (allMembers[i].getPlayerId() == member.getPlayerId()) {
				allMembers[i] = member;
				break;
			}
		}
	}

	@Override
	public Member[] getAllMembers() {
		return allMembers;
	}

	@Override
	public void memberConnected(RemoteMember member) {
		super.memberConnected(member);
		syncWithAllMembers(member);
	}

	@Override
	public LocalMember memberConnected(GameCharacter p) {
		LocalMember member = super.memberConnected(p);
		syncWithAllMembers(member);
		return member;
	}

	@Override
	public RemoteMember memberDisconnected(byte ch, int pId) {
		RemoteMember member = super.memberDisconnected(ch, pId);
		syncWithAllMembers(member);
		return member;
	}

	@Override
	public RemoteMember memberDisconnected(GameCharacter p) {
		RemoteMember member = super.memberDisconnected(p);
		syncWithAllMembers(member);
		return member;
	}

	@Override
	protected void addPlayer(RemoteMember member, boolean transition) {
		super.addPlayer(member, transition);
		if (!transition)
			addToAllMembers(member);
	}

	@Override
	protected void addPlayer(LocalMember member, boolean transition) {
		super.addPlayer(member, transition);
		if (!transition)
			addToAllMembers(member);
	}

	@Override
	protected RemoteMember removePlayer(byte ch, int playerId, boolean transition) {
		if (!transition)
			removeFromAllMembersAndCollapse(playerId);
		return super.removePlayer(ch, playerId, transition);
	}

	@Override
	protected LocalMember removePlayer(GameCharacter p, boolean transition) {
		if (!transition)
			removeFromAllMembersAndCollapse(p.getId());
		return super.removePlayer(p, transition);
	}

	@Override
	public Member getMember(int playerId) {
		for (int i = 0; i < 6; i++)
			if (allMembers[i].getPlayerId() == playerId)
				return allMembers[i];
		return null;
	}

	/**
	 * This PartyList must be at least read locked when this method is called.
	 * @param position
	 * @return 
	 */
	public Member getMember(byte position) {
		return allMembers[position];
	}
}
