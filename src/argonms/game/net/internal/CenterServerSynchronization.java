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

package argonms.game.net.internal;

import argonms.common.character.CenterServerSynchronizationOps;
import argonms.common.net.internal.RemoteCenterOps;
import argonms.common.util.collections.Pair;
import argonms.common.util.input.LittleEndianReader;
import argonms.common.util.output.LittleEndianByteArrayWriter;
import argonms.common.util.output.LittleEndianWriter;
import argonms.game.GameServer;
import argonms.game.character.GameCharacter;
import argonms.game.character.PartyList;
import argonms.game.net.WorldChannel;
import argonms.game.net.external.GamePackets;
import argonms.game.net.external.handler.PartyListHandler;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 *
 * @author GoldenKevin
 */
public class CenterServerSynchronization extends CrossProcessSynchronization {
	private final CrossServerSynchronization handler;
	private final WorldChannel self;
	private final ConcurrentMap<Integer, PartyList> activeLocalParties;

	public CenterServerSynchronization(CrossServerSynchronization handler, WorldChannel self) {
		this.activeLocalParties = new ConcurrentHashMap<Integer, PartyList>();
		this.handler = handler;
		this.self = self;
	}

	private void writePartySynchronizationPacketHeader(LittleEndianWriter lew, byte opcode) {
		lew.writeByte(RemoteCenterOps.CENTER_SERVER_SYNCHRONIZATION);
		lew.writeByte(opcode);
	}

	private void writePartySynchronizationPacket(byte[] packet) {
		GameServer.getInstance().getCenterInterface().getSession().send(packet);
	}

	public void receivedPartySynchronziationPacket(LittleEndianReader packet) {
		switch (packet.readByte()) {
			case CenterServerSynchronizationOps.PARTY_CREATE: {
				int partyId = packet.readInt();
				int leaderId = packet.readInt();
				GameCharacter leaderPlayer = self.getPlayerById(leaderId);
				if (leaderPlayer == null)
					return;

				PartyList party = new PartyList(partyId, leaderPlayer, true, true);
				activeLocalParties.put(Integer.valueOf(partyId), party);
				leaderPlayer.setParty(party);
				leaderPlayer.getClient().getSession().send(GamePackets.writePartyCreated(partyId));
				break;
			}
			case CenterServerSynchronizationOps.PARTY_DISBAND: {
				int partyId = packet.readInt();
				PartyList party = activeLocalParties.remove(Integer.valueOf(partyId));
				if (party == null)
					return;

				party.lockRead();
				try {
					for (PartyList.LocalMember mem : party.getMembersInLocalChannel()) {
						mem.getPlayer().setParty(null);
						mem.getPlayer().getClient().getSession().send(GamePackets.writePartyDisbanded(party.getId(), party.getLeader()));
					}
				} finally {
					party.unlockRead();
				}
				break;
			}
			case CenterServerSynchronizationOps.PARTY_REMOVE_PLAYER: {
				int partyId = packet.readInt();
				int leaverId = packet.readInt();
				byte leaverCh = packet.readByte();
				boolean leaverExpelled = packet.readBool();
				//TODO: not safe for activeLocalParties when members
				//concurrently join or leave party, or enter or exit channel
				PartyList party = activeLocalParties.get(Integer.valueOf(partyId));
				if (party == null)
					return;

				boolean removeParty = false;
				String leaverName;
				party.lockWrite();
				try {
					if (self.getChannelId() == leaverCh) {
						GameCharacter leavingPlayer = self.getPlayerById(leaverId);
						leaverName = leavingPlayer.getName();
						party.removePlayer(leavingPlayer);
						removeParty = party.getMembersInLocalChannel().isEmpty();

						leavingPlayer.setParty(null);
						leavingPlayer.getClient().getSession().send(GamePackets.writePartyMemberLeft(party, leaverId, leaverName, leaverExpelled));

						//GameCenterPacketProcessor.processPartyMemberLeft
						//has an explanation for this
						if (removeParty) {
							boolean save;
							party.lockRead();
							try {
								save = party.allOffline();
							} finally {
								party.unlockRead();
							}
							if (save)
								leavingPlayer.saveCharacter();
						}
					} else {
						leaverName = packet.readLengthPrefixedString();
						party.removePlayer(leaverCh, leaverId);
					}
				} finally {
					party.unlockWrite();
				}
				if (removeParty) {
					activeLocalParties.remove(Integer.valueOf(partyId));
				} else {
					party.lockRead();
					try {
						for (PartyList.LocalMember mem : party.getMembersInLocalChannel())
							mem.getPlayer().getClient().getSession().send(GamePackets.writePartyMemberLeft(party, leaverId, leaverName, leaverExpelled));
					} finally {
						party.unlockRead();
					}
				}
				break;
			}
			case CenterServerSynchronizationOps.PARTY_ADD_PLAYER: {
				int partyId = packet.readInt();
				int joinerId = packet.readInt();
				byte joinerCh = packet.readByte();
				if (joinerCh == self.getChannelId()) {
					GameCharacter joiningPlayer = self.getPlayerById(joinerId);
					PartyList newParty = new PartyList(partyId, joiningPlayer, true, false);
					PartyList party = activeLocalParties.putIfAbsent(Integer.valueOf(partyId), newParty);
					if (party == null) {
						//only can happen if the leader changed channels or
						//logged off. is that even a valid case?
						party = newParty;
						party.lockWrite();
						try {
							handler.fillPartyList(joiningPlayer, party);
						} finally {
							party.unlockWrite();
						}
					} else {
						party.lockWrite();
						try {
							party.addPlayer(new PartyList.LocalMember(joiningPlayer));
						} finally {
							party.unlockWrite();
						}
						party.lockRead();
						try {
							for (PartyList.LocalMember mem : party.getMembersInLocalChannel()) {
								mem.getPlayer().getClient().getSession().send(GamePackets.writePartyMemberJoined(party, joiningPlayer.getName()));
								mem.getPlayer().pullPartyHp();
								mem.getPlayer().pushHpToParty();
							}
						} finally {
							party.unlockRead();
						}
					}
					joiningPlayer.setParty(party);
				} else {
					String joinerName = packet.readLengthPrefixedString();
					short joinerJob = packet.readShort();
					short joinerLevel = packet.readShort();
					PartyList party = activeLocalParties.get(Integer.valueOf(partyId));
					if (party != null) {
						party.lockWrite();
						try {
							party.addPlayer(new PartyList.RemoteMember(joinerId, joinerName, joinerJob, joinerLevel, joinerCh));
						} finally {
							party.unlockWrite();
						}
						party.lockRead();
						try {
							for (PartyList.LocalMember mem : party.getMembersInLocalChannel())
								mem.getPlayer().getClient().getSession().send(GamePackets.writePartyMemberJoined(party, joinerName));
						} finally {
							party.unlockRead();
						}
					}
				}
				break;
			}
			case CenterServerSynchronizationOps.PARTY_JOIN_ERROR: {
				int joinFailedPlayerId = packet.readInt();
				GameCharacter joinFailedPlayer = self.getPlayerById(joinFailedPlayerId);
				if (joinFailedPlayer != null)
					joinFailedPlayer.getClient().getSession().send(GamePackets.writeSimplePartyListMessage(PartyListHandler.PARTY_FULL));
				break;
			}
			case CenterServerSynchronizationOps.PARTY_CHANGE_LEADER: {
				int partyId = packet.readInt();
				int newLeader = packet.readInt();
				PartyList party = activeLocalParties.get(Integer.valueOf(partyId));
				if (party == null)
					return;

				party.setLeader(newLeader);
				party.lockRead();
				try {
					for (PartyList.LocalMember mem : party.getMembersInLocalChannel())
						mem.getPlayer().getClient().getSession().send(GamePackets.writePartyChangeLeader(newLeader, false));
				} finally {
					party.unlockRead();
				}
				break;
			}
			case CenterServerSynchronizationOps.PARTY_FETCH_LIST: {
				int responseId = packet.readInt();
				int leader = packet.readInt();
				byte count = packet.readByte();
				PartyList.Member[] members = new PartyList.Member[count];
				for (int i = 0; i < count; i++) {
					int memberId = packet.readInt();
					byte memberCh = packet.readByte();
					if (memberCh == self.getChannelId()) {
						members[i] = new PartyList.LocalMember(self.getPlayerById(memberId));
					} else {
						String memberName = packet.readLengthPrefixedString();
						short memberJob = packet.readShort();
						short memberLevel = packet.readShort();
						members[i] = new PartyList.RemoteMember(memberId, memberName, memberJob, memberLevel, memberCh);
					}
				}

				BlockingQueue<Pair<Byte, Object>> consumer = blockingCalls.remove(Integer.valueOf(responseId));
				if (consumer == null)
					//timed out and garbage collected
					return;

				consumer.add(new Pair<Byte, Object>(Byte.valueOf((byte) -1), Integer.valueOf(leader)));
				consumer.add(new Pair<Byte, Object>(Byte.valueOf((byte) -1), members));
				break;
			}
			case CenterServerSynchronizationOps.PARTY_MEMBER_CONNECTED: {
				int partyId = packet.readInt();
				int entererId = packet.readInt();
				byte targetCh = packet.readByte();

				if (targetCh == self.getChannelId()) {
					GameCharacter enteringPlayer = self.getPlayerById(entererId);
					PartyList newParty = new PartyList(partyId, enteringPlayer, true, false);
					PartyList party = activeLocalParties.putIfAbsent(Integer.valueOf(partyId), newParty);
					if (party == null) {
						//this should never happen, since we always call
						//sendFetchPartyList when we load the character, which
						//happens before we call sendPartyMemberEnteredChannel
						party = newParty;
						party.lockWrite();
						try {
							handler.fillPartyList(enteringPlayer, party);
						} finally {
							party.unlockWrite();
						}
					} else {
						party.lockWrite();
						try {
							party.memberConnected(self.getPlayerById(entererId));
						} finally {
							party.unlockWrite();
						}
						party.lockRead();
						try {
							for (PartyList.LocalMember mem : party.getMembersInLocalChannel()) {
								mem.getPlayer().getClient().getSession().send(GamePackets.writePartyList(party));
								mem.getPlayer().pullPartyHp();
								mem.getPlayer().pushHpToParty();
							}
						} finally {
							party.unlockRead();
						}
					}
				} else {
					PartyList party = activeLocalParties.get(Integer.valueOf(partyId));
					if (party != null) {
						party.lockWrite();
						try {
							party.memberConnected(new PartyList.RemoteMember(party.getOfflineMember(entererId), targetCh));
						} finally {
							party.unlockWrite();
						}
						party.lockRead();
						try {
							for (PartyList.LocalMember mem : party.getMembersInLocalChannel())
								mem.getPlayer().getClient().getSession().send(GamePackets.writePartyList(party));
						} finally {
							party.unlockRead();
						}
					}
				}
				break;
			}
			case CenterServerSynchronizationOps.PARTY_MEMBER_DISCONNECTED: {
				int partyId = packet.readInt();
				int exiterId = packet.readInt();
				byte lastCh = packet.readByte();
				boolean loggingOff = packet.readBool();
				//TODO: not safe for activeLocalParties when members
				//concurrently join or leave party, or enter or exit channel
				PartyList party = activeLocalParties.get(Integer.valueOf(partyId));
				if (party == null)
					return;

				boolean removeParty = false;
				party.lockWrite();
				try {
					if (lastCh == self.getChannelId()) {
						party.memberDisconnected(self.getPlayerById(exiterId));
						removeParty = party.getMembersInLocalChannel().isEmpty();
					} else {
						party.memberDisconnected(lastCh, exiterId);
					}
				} finally {
					party.unlockWrite();
				}
				if (removeParty) {
					activeLocalParties.remove(Integer.valueOf(partyId));
				} else if (loggingOff) {
					party.lockRead();
					try {
						for (PartyList.LocalMember mem : party.getMembersInLocalChannel())
							mem.getPlayer().getClient().getSession().send(GamePackets.writePartyList(party));
					} finally {
						party.unlockRead();
					}
				}
				break;
			}
			case CenterServerSynchronizationOps.PARTY_MEMBER_STAT_UPDATED: {
				int partyId = packet.readInt();
				int updatedPlayerId = packet.readInt();
				byte updatedPlayerCh = packet.readByte();
				PartyList party = activeLocalParties.get(Integer.valueOf(partyId));
				if (party == null)
					return;

				party.lockRead();
				try {
					if (updatedPlayerCh != self.getChannelId()) {
						boolean levelUpdated = packet.readBool();
						short newValue = packet.readShort();
						PartyList.RemoteMember member = party.getMember(updatedPlayerCh, updatedPlayerId);
						if (levelUpdated)
							member.setLevel(newValue);
						else
							member.setJob(newValue);
					}
					for (PartyList.LocalMember mem : party.getMembersInLocalChannel())
						mem.getPlayer().getClient().getSession().send(GamePackets.writePartyList(party));
				} finally {
					party.unlockRead();
				}
				break;
			}
		}
	}

	public void sendMakeParty(GameCharacter creator) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(13 + creator.getName().length());
		writePartySynchronizationPacketHeader(lew, CenterServerSynchronizationOps.PARTY_CREATE);
		lew.writeInt(creator.getId());
		lew.writeByte(self.getChannelId());
		lew.writeLengthPrefixedString(creator.getName());
		lew.writeShort(creator.getJob());
		lew.writeShort(creator.getLevel());

		writePartySynchronizationPacket(lew.getBytes());
	}

	public void sendDisbandParty(int partyId) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(6);
		writePartySynchronizationPacketHeader(lew, CenterServerSynchronizationOps.PARTY_DISBAND);
		lew.writeInt(partyId);

		writePartySynchronizationPacket(lew.getBytes());
	}

	public void sendLeaveParty(GameCharacter p, int partyId) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(14 + p.getName().length());
		writePartySynchronizationPacketHeader(lew, CenterServerSynchronizationOps.PARTY_REMOVE_PLAYER);
		lew.writeInt(partyId);
		lew.writeInt(p.getId());
		lew.writeByte(p.getClient().getChannel());
		lew.writeLengthPrefixedString(p.getName());
		lew.writeBool(false);

		writePartySynchronizationPacket(lew.getBytes());
	}

	public void sendJoinParty(GameCharacter p, int partyId) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(17 + p.getName().length());
		writePartySynchronizationPacketHeader(lew, CenterServerSynchronizationOps.PARTY_ADD_PLAYER);
		lew.writeInt(partyId);
		lew.writeInt(p.getId());
		lew.writeByte(p.getClient().getChannel());
		lew.writeLengthPrefixedString(p.getName());
		lew.writeShort(p.getJob());
		lew.writeShort(p.getLevel());

		writePartySynchronizationPacket(lew.getBytes());
	}

	public void sendExpelPartyMember(PartyList.Member member, int partyId) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(14 + member.getName().length());
		writePartySynchronizationPacketHeader(lew, CenterServerSynchronizationOps.PARTY_REMOVE_PLAYER);
		lew.writeInt(partyId);
		lew.writeInt(member.getPlayerId());
		lew.writeByte(member.getChannel());
		lew.writeLengthPrefixedString(member.getName());
		lew.writeBool(true);

		writePartySynchronizationPacket(lew.getBytes());
	}

	public void sendChangePartyLeader(int partyId, int newLeader) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(10);
		writePartySynchronizationPacketHeader(lew, CenterServerSynchronizationOps.PARTY_CHANGE_LEADER);
		lew.writeInt(partyId);
		lew.writeInt(newLeader);

		writePartySynchronizationPacket(lew.getBytes());
	}

	private void fillPartyList(BlockingQueue<Pair<Byte, Object>> resultConsumer, GameCharacter excludeMember, PartyList party) {
		int responseId = nextResponseId.incrementAndGet();
		blockingCalls.put(Integer.valueOf(responseId), resultConsumer);

		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(15);
		writePartySynchronizationPacketHeader(lew, CenterServerSynchronizationOps.PARTY_FETCH_LIST);
		lew.writeInt(party.getId());
		lew.writeInt(excludeMember.getId());
		lew.writeByte(self.getChannelId());
		lew.writeInt(responseId);

		writePartySynchronizationPacket(lew.getBytes());
	}

	public PartyList sendFetchPartyList(GameCharacter p, int partyId) {
		PartyList newParty = new PartyList(partyId, p, false, false);
		PartyList party;
		//prevent any concurrent accessors from using an uninitialized PartyList
		newParty.lockWrite();
		try {
			party = activeLocalParties.putIfAbsent(Integer.valueOf(partyId), newParty);
			if (party == null) {
				party = newParty;
				handler.fillPartyList(p, party);
			}
		} finally {
			newParty.unlockWrite();
		}
		return party;
	}

	public void sendPartyMemberOnline(GameCharacter p, PartyList party) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(11);
		writePartySynchronizationPacketHeader(lew, CenterServerSynchronizationOps.PARTY_MEMBER_CONNECTED);
		lew.writeInt(p.getParty().getId());
		lew.writeInt(p.getId());
		lew.writeByte(self.getChannelId());

		writePartySynchronizationPacket(lew.getBytes());
	}

	public void sendPartyMemberOffline(GameCharacter exiter, boolean loggingOff) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(12);
		writePartySynchronizationPacketHeader(lew, CenterServerSynchronizationOps.PARTY_MEMBER_DISCONNECTED);
		lew.writeInt(exiter.getParty().getId());
		lew.writeInt(exiter.getId());
		lew.writeByte(self.getChannelId());
		lew.writeBool(loggingOff);

		writePartySynchronizationPacket(lew.getBytes());
	}

	public void sendPartyLevelOrJobUpdate(GameCharacter p, boolean level) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(14);
		writePartySynchronizationPacketHeader(lew, CenterServerSynchronizationOps.PARTY_MEMBER_STAT_UPDATED);
		lew.writeInt(p.getParty().getId());
		lew.writeInt(p.getId());
		lew.writeByte(self.getChannelId());
		lew.writeBool(level);
		lew.writeShort(level ? p.getLevel() : p.getJob());

		writePartySynchronizationPacket(lew.getBytes());
	}
}
