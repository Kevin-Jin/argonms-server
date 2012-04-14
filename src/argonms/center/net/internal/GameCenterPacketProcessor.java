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

package argonms.center.net.internal;

import argonms.center.CenterServer;
import argonms.center.Party;
import argonms.common.ServerType;
import argonms.common.character.InterServerPartyOps;
import argonms.common.net.internal.CenterRemoteOps;
import argonms.common.net.internal.RemoteCenterOps;
import argonms.common.util.DatabaseManager;
import argonms.common.util.DatabaseManager.DatabaseType;
import argonms.common.util.input.LittleEndianReader;
import argonms.common.util.output.LittleEndianByteArrayWriter;
import argonms.game.character.PartyList;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Processes packet sent from the game server and received at the center
 * server.
 * @author GoldenKevin
 */
public class GameCenterPacketProcessor extends RemoteCenterPacketProcessor {
	private static final Logger LOG = Logger.getLogger(GameCenterPacketProcessor.class.getName());

	private CenterGameInterface r;

	public GameCenterPacketProcessor(CenterGameInterface r) {
		this.r = r;
	}

	@Override
	public void process(LittleEndianReader packet) {
		switch (packet.readByte()) {
			case RemoteCenterOps.PING:
				r.getSession().send(pongMessage());
				break;
			case RemoteCenterOps.PONG:
				r.getSession().receivedPong();
				break;
			case RemoteCenterOps.ONLINE:
				serverOnline(packet);
				break;
			case RemoteCenterOps.POPULATION_CHANGED:
				processPopulationChange(packet);
				break;
			case RemoteCenterOps.MODIFY_CHANNEL_PORT:
				processChannelPortChange(packet);
				break;
			case RemoteCenterOps.INTER_CHANNEL:
				processInterChannel(packet);
				break;
			case RemoteCenterOps.INTER_CHANNEL_ALL:
				processInterChannelAll(packet);
				break;
			case RemoteCenterOps.PARTY_SYNCHRONIZATION:
				processPartySynchronization(packet);
				break;
		}
	}

	private void serverOnline(LittleEndianReader packet)  {
		r.setHost(packet.readLengthPrefixedString());
		byte world = packet.readByte();
		byte size = packet.readByte();
		Map<Byte, Integer> clientPorts = new HashMap<Byte, Integer>(size);
		for (int i = 0; i < size; i++)
			clientPorts.put(Byte.valueOf(packet.readByte()), packet.readInt());
		r.setWorld(world);
		r.setClientPorts(clientPorts);
		CenterServer.getInstance().registerGame(r);
	}

	private void processPopulationChange(LittleEndianReader packet) {
		byte channel = packet.readByte();
		short now = packet.readShort();
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(4);
		lew.writeByte(CenterRemoteOps.CHANGE_POPULATION);
		lew.writeByte(r.getWorld());
		lew.writeByte(channel);
		lew.writeShort(now);
		CenterServer.getInstance().sendToLogin(lew.getBytes());
	}

	private void processChannelPortChange(LittleEndianReader packet) {
		byte world = packet.readByte();
		byte channel = packet.readByte();
		int newPort = packet.readInt();
		CenterServer.getInstance().channelPortChanged(world, r.getServerId(), channel, newPort);
	}

	private void processInterChannel(LittleEndianReader packet) {
		byte channel = packet.readByte();
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(packet.available() + 2);
		lew.writeByte(CenterRemoteOps.INTER_CHANNEL_RELAY);
		lew.writeByte(channel);
		lew.writeBytes(packet.readBytes(packet.available()));
		byte[] message = lew.getBytes();
		for (CenterGameInterface cgi : CenterServer.getInstance().getAllServersOfWorld(r.getWorld(), ServerType.UNDEFINED))
			if (cgi.isOnline() && cgi.getChannels().contains(Byte.valueOf(channel)))
				cgi.getSession().send(message);
	}

	private void processInterChannelAll(LittleEndianReader packet) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(packet.available() + 2);
		lew.writeByte(CenterRemoteOps.INTER_CHANNEL_RELAY);
		lew.writeByte((byte) -1);
		lew.writeBytes(packet.readBytes(packet.available()));
		byte[] message = lew.getBytes();
		for (CenterGameInterface cgi : CenterServer.getInstance().getAllServersOfWorld(r.getWorld(), r.getServerId()))
			if (cgi.isOnline())
				cgi.getSession().send(message);
	}

	private void processPartySynchronization(LittleEndianReader packet) {
		switch (packet.readByte()) {
			case InterServerPartyOps.CREATE:
				processPartyCreation(packet);
				break;
			case InterServerPartyOps.DISBAND:
				processPartyDisbandment(packet);
				break;
			case InterServerPartyOps.REMOVE_PLAYER:
				processPartyMemberRemoval(packet);
				break;
			case InterServerPartyOps.ADD_PLAYER:
				processPartyMemberAddition(packet);
				break;
			case InterServerPartyOps.CHANGE_LEADER:
				processPartyLeaderChange(packet);
				break;
			case InterServerPartyOps.FETCH_LIST:
				processPartyFetchList(packet);
				break;
			case InterServerPartyOps.MEMBER_CHANGED_CHANNEL:
				processPartyMemberChangedChannel(packet);
				break;
			case InterServerPartyOps.MEMBER_LOGGED_OFF:
				processPartyMemberLoggedOff(packet);
				break;
			case InterServerPartyOps.MEMBER_STAT_UPDATED:
				processPartyMemberStatChanged(packet);
				break;
		}
	}

	private void processPartyCreation(LittleEndianReader packet) {
		byte creatorChannel = packet.readByte();
		int creatorId = packet.readInt();
		String creatorName = packet.readLengthPrefixedString();
		short creatorJob = packet.readShort();
		short creatorLevel = packet.readShort();
		int partyId = CenterServer.getInstance().getPartyDb(r.getWorld()).makeNewParty(new Party.Member(creatorId, creatorName, creatorJob, creatorLevel, creatorChannel));
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(11);
		lew.writeByte(CenterRemoteOps.PARTY_SYNCHRONIZATION);
		lew.writeByte(creatorChannel);
		lew.writeByte(InterServerPartyOps.CREATE);
		lew.writeInt(creatorId);
		lew.writeInt(partyId);
		r.getSession().send(lew.getBytes());
	}

	private void processPartyDisbandment(LittleEndianReader packet) {
		int partyId = packet.readInt();
		Party party = CenterServer.getInstance().getPartyDb(r.getWorld()).remove(partyId);
		if (party != null) { //if there was lag, leader may have spammed leave button
			List<CenterGameInterface> gameServers = CenterServer.getInstance().getAllServersOfWorld(r.getWorld(), ServerType.UNDEFINED);
			party.lockRead();
			try {
				for (Byte channel : party.allChannels()) {
					for (CenterGameInterface cgi : gameServers) {
						if (cgi.isOnline() && cgi.getChannels().contains(channel)) {
							Collection<Party.Member> members = party.getMembersOfChannel(channel.byteValue());
							LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(4 + 4 * members.size());
							lew.writeByte(CenterRemoteOps.PARTY_SYNCHRONIZATION);
							lew.writeByte(channel.byteValue());
							lew.writeByte(InterServerPartyOps.DISBAND);
							lew.writeByte((byte) members.size());
							for (Party.Member member : members)
								lew.writeInt(member.getPlayerId());
							cgi.getSession().send(lew.getBytes());
						}
					}
				}
			} finally {
				party.unlockRead();
			}
		}
	}

	private void processPartyMemberRemoval(LittleEndianReader packet) {
		int partyId = packet.readInt();
		int leaverId = packet.readInt();
		String leaverName = packet.readLengthPrefixedString();
		byte leaverChannel = packet.readByte();
		boolean leaverExpelled = packet.readBool();
		Party party = CenterServer.getInstance().getPartyDb(r.getWorld()).get(partyId);
		if (party != null) { //if there was lag, party may have been disbanded before member clicked leave
			boolean success;
			party.lockWrite();
			try {
				success = party.removePlayer(leaverId);
			} finally {
				party.unlockWrite();
			}
			//if there was lag, member may have spammed leave button
			if (success) {
				party.lockRead();
				try {
					Set<Byte> partyChannels = party.allChannels();
					if (partyChannels.size() == 1 && partyChannels.contains(Byte.valueOf(PartyList.OFFLINE_CH))) {
						CenterServer.getInstance().getPartyDb(r.getWorld()).remove(partyId);
					} else {
						for (Byte channel : partyChannels) {
							for (CenterGameInterface cgi : CenterServer.getInstance().getAllServersOfWorld(r.getWorld(), ServerType.UNDEFINED)) {
								if (cgi.isOnline() && cgi.getChannels().contains(channel)) {
									Collection<Party.Member> members = party.getMembersOfChannel(channel.byteValue());
									LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(12 + leaverName.length() + 4 * members.size());
									lew.writeByte(CenterRemoteOps.PARTY_SYNCHRONIZATION);
									lew.writeByte(channel.byteValue());
									lew.writeByte(InterServerPartyOps.REMOVE_PLAYER);
									lew.writeInt(leaverId);
									lew.writeLengthPrefixedString(leaverName);
									lew.writeByte(leaverChannel);
									lew.writeBool(leaverExpelled);
									lew.writeByte((byte) members.size());
									for (Party.Member member : members)
										lew.writeInt(member.getPlayerId());
									cgi.getSession().send(lew.getBytes());
								}
							}
						}
					}
				} finally {
					party.unlockRead();
				}

				if (leaverChannel == PartyList.OFFLINE_CH) {
					Connection con = null;
					PreparedStatement ps = null;
					try {
						con = DatabaseManager.getConnection(DatabaseType.STATE);
						ps = con.prepareStatement("DELETE FROM `parties` WHERE `characterid` = ?");
						ps.setInt(1, leaverId);
						ps.executeUpdate();
					} catch (SQLException ex) {
						LOG.log(Level.WARNING, "Could not expel offline member " + leaverName + " from party " + partyId, ex);
					} finally {
						DatabaseManager.cleanup(DatabaseManager.DatabaseType.STATE, null, ps, con);
					}
				} else {
					//leaver could've been only member in his channel, which means he
					//would be skipped in the above loop after he was removed
					//we still need to send him a message
					for (CenterGameInterface cgi : CenterServer.getInstance().getAllServersOfWorld(r.getWorld(), ServerType.UNDEFINED)) {
						if (cgi.isOnline() && cgi.getChannels().contains(Byte.valueOf(leaverChannel))) {
							LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(8);
							lew.writeByte(CenterRemoteOps.PARTY_SYNCHRONIZATION);
							lew.writeByte(leaverChannel);
							lew.writeByte(InterServerPartyOps.LEAVE);
							lew.writeInt(leaverId);
							lew.writeBool(leaverExpelled);
							cgi.getSession().send(lew.getBytes());
						}
					}
				}
			}
		}
	}

	private void processPartyMemberAddition(LittleEndianReader packet) {
		byte joinerCh = packet.readByte();
		int partyId = packet.readInt();
		int joinerId = packet.readInt();
		String joinerName = packet.readLengthPrefixedString();
		short joinerJob = packet.readShort();
		short joinerLevel = packet.readShort();
		Party party = CenterServer.getInstance().getPartyDb(r.getWorld()).get(partyId);
		if (party != null) { //if there was lag, party may have been disbanded before player clicked accept
			boolean full;
			party.lockWrite();
			try {
				if (!party.isFull()) {
					full = false;
					party.addPlayer(new Party.Member(joinerId, joinerName, joinerJob, joinerLevel, joinerCh));
				} else {
					full = true;
				}
			} finally {
				party.unlockWrite();
			}
			if (!full) {
				party.lockRead();
				try {
					for (Byte channel : party.allChannels()) {
						for (CenterGameInterface cgi : CenterServer.getInstance().getAllServersOfWorld(r.getWorld(), ServerType.UNDEFINED)) {
							if (cgi.isOnline() && cgi.getChannels().contains(channel)) {
								Collection<Party.Member> members = party.getMembersOfChannel(channel.byteValue());
								//notify other party members
								LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(joinerCh == channel.byteValue() ? (9 + 4 * (members.size() - 1)) : (15 + joinerName.length() + 4 * members.size()));
								lew.writeByte(CenterRemoteOps.PARTY_SYNCHRONIZATION);
								lew.writeByte(channel.byteValue());
								lew.writeByte(InterServerPartyOps.ADD_PLAYER);
								lew.writeByte(joinerCh);
								lew.writeInt(joinerId);
								//we don't need to send this information if the
								//two are on the same channel
								if (joinerCh != channel.byteValue()) {
									lew.writeLengthPrefixedString(joinerName);
									lew.writeShort(joinerJob);
									lew.writeShort(joinerLevel);
								}
								lew.writeByte((byte) (joinerCh == channel.byteValue() ? (members.size() - 1) : members.size()));
								for (Party.Member member : members)
									//exclude the joining player from getting
									//his own info
									if (member.getPlayerId() != joinerId)
										lew.writeInt(member.getPlayerId());
								cgi.getSession().send(lew.getBytes());

								//we need to send the party member list to the
								//joining player
								if (channel.byteValue() == joinerCh) {
									lew = new LittleEndianByteArrayWriter();
									lew.writeByte(CenterRemoteOps.PARTY_SYNCHRONIZATION);
									lew.writeByte(channel.byteValue());
									lew.writeByte(InterServerPartyOps.JOIN);
									lew.writeInt(joinerId);
									lew.writeBool(true);
									lew.writeInt(partyId);
									lew.writeInt(party.getLeader());
									members = party.getAllMembers();
									lew.writeByte((byte) (members.size() - 1));
									for (Party.Member member : members) {
										//exclude the joining player from
										//getting his own info
										if (member.getPlayerId() != joinerId) {
											lew.writeByte(member.getChannel());
											lew.writeInt(member.getPlayerId());
											if (member.getChannel() != joinerCh) {
												lew.writeLengthPrefixedString(member.getName());
												lew.writeShort(member.getJob());
												lew.writeShort(member.getLevel());
											}
										}
									}
									cgi.getSession().send(lew.getBytes());
								}
							}
						}
					}
				} finally {
					party.unlockRead();
				}
			} else {
				LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(8);
				lew.writeByte(CenterRemoteOps.PARTY_SYNCHRONIZATION);
				lew.writeByte(joinerCh);
				lew.writeByte(InterServerPartyOps.JOIN);
				lew.writeInt(joinerId);
				lew.writeBool(false);
				for (CenterGameInterface cgi : CenterServer.getInstance().getAllServersOfWorld(r.getWorld(), ServerType.UNDEFINED))
					if (cgi.isOnline() && cgi.getChannels().contains(Byte.valueOf(joinerCh)))
						cgi.getSession().send(lew.getBytes());
			}
		}
	}

	private void processPartyLeaderChange(LittleEndianReader packet) {
		int partyId = packet.readInt();
		int newLeader = packet.readInt();
		Party party = CenterServer.getInstance().getPartyDb(r.getWorld()).get(partyId);
		if (party != null) { //if there was lag, party may have been disbanded before leader clicked Party Leader
			party.setLeader(newLeader);

			List<CenterGameInterface> gameServers = CenterServer.getInstance().getAllServersOfWorld(r.getWorld(), ServerType.UNDEFINED);
			party.lockRead();
			try {
				for (Byte channel : party.allChannels()) {
					for (CenterGameInterface cgi : gameServers) {
						if (cgi.isOnline() && cgi.getChannels().contains(channel)) {
							Collection<Party.Member> members = party.getMembersOfChannel(channel.byteValue());
							LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(8 + 4 * members.size());
							lew.writeByte(CenterRemoteOps.PARTY_SYNCHRONIZATION);
							lew.writeByte(channel.byteValue());
							lew.writeByte(InterServerPartyOps.CHANGE_LEADER);
							lew.writeInt(newLeader);
							lew.writeByte((byte) members.size());
							for (Party.Member member : members)
								lew.writeInt(member.getPlayerId());
							cgi.getSession().send(lew.getBytes());
						}
					}
				}
			} finally {
				party.unlockRead();
			}
		}
	}

	private void processPartyFetchList(LittleEndianReader packet) {
		byte channel = packet.readByte();
		int playerId = packet.readInt();
		int partyId = packet.readInt();
		int responseId = packet.readInt();
		Party party = CenterServer.getInstance().getPartyDb(r.getWorld()).get(partyId);
		if (party == null) {
			//TODO: not safe when two members of the same party log in at the
			//same time or when there was only one member logged in and he
			//logged off at the same time another member logs in
			party = new Party();
			Connection con = null;
			PreparedStatement ps = null;
			ResultSet rs = null;
			try {
				con = DatabaseManager.getConnection(DatabaseType.STATE);
				ps = con.prepareStatement("SELECT `c`.`id`,`c`.`name`,"
						+ "`c`.`job`,`c`.`level`,`p`.`leader` FROM "
						+ "`parties` `p` LEFT JOIN `characters` `c` ON "
						+ "`c`.`id` = `p`.`characterid` WHERE `p`.`world` = ? "
						+ "AND `p`.`partyid` = ?");
				ps.setInt(1, r.getWorld());
				ps.setInt(2, partyId);
				rs = ps.executeQuery();
				//no write locking necessary because scope is still limited
				while (rs.next()) {
					int cid = rs.getInt(1);
					party.addPlayer(new Party.Member(cid, rs.getString(2), rs.getShort(3), rs.getShort(4), PartyList.OFFLINE_CH));
					if (rs.getBoolean(5))
						party.setLeader(cid);
				}
			} catch (SQLException ex) {
				LOG.log(Level.WARNING, "Could not load party " + partyId + " of world " + r.getWorld(), ex);
			} finally {
				DatabaseManager.cleanup(DatabaseManager.DatabaseType.STATE, rs, ps, con);
			}
			CenterServer.getInstance().getPartyDb(r.getWorld()).set(partyId, party);
		}
		party.lockRead();
		try {
			for (CenterGameInterface cgi : CenterServer.getInstance().getAllServersOfWorld(r.getWorld(), ServerType.UNDEFINED)) {
				if (cgi.isOnline() && cgi.getChannels().contains(Byte.valueOf(channel))) {
					LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter();
					lew.writeByte(CenterRemoteOps.PARTY_SYNCHRONIZATION);
					lew.writeByte(channel);
					lew.writeByte(InterServerPartyOps.FETCH_LIST);
					lew.writeInt(responseId);
					lew.writeInt(party.getLeader());
					Collection<Party.Member> members = party.getAllMembers();
					lew.writeByte((byte) (members.size() - 1));
					for (Party.Member member : members) {
						//exclude the joining player from getting
						//his own info
						if (member.getPlayerId() != playerId) {
							lew.writeByte(member.getChannel());
							lew.writeInt(member.getPlayerId());
							if (member.getChannel() != channel) {
								lew.writeLengthPrefixedString(member.getName());
								lew.writeShort(member.getJob());
								lew.writeShort(member.getLevel());
							}
						}
					}
					cgi.getSession().send(lew.getBytes());
				}
			}
		} finally {
			party.unlockRead();
		}
	}

	private void processPartyMemberChangedChannel(LittleEndianReader packet) {
		byte destCh = packet.readByte();
		int playerId = packet.readInt();
		int partyId = packet.readInt();
		Party party = CenterServer.getInstance().getPartyDb(r.getWorld()).get(partyId);
		if (party != null) {
			party.lockWrite();
			try {
				party.setMemberChannel(playerId, destCh);
			} finally {
				party.unlockWrite();
			}
			party.lockRead();
			try {
				for (Byte channel : party.allChannels()) {
					for (CenterGameInterface cgi : CenterServer.getInstance().getAllServersOfWorld(r.getWorld(), ServerType.UNDEFINED)) {
						if (cgi.isOnline() && cgi.getChannels().contains(channel)) {
							Collection<Party.Member> members = party.getMembersOfChannel(channel.byteValue());
							//notify other party members
							LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(9 + 4 * (destCh == channel.byteValue() ? members.size() - 1 : members.size()));
							lew.writeByte(CenterRemoteOps.PARTY_SYNCHRONIZATION);
							lew.writeByte(channel.byteValue());
							lew.writeByte(InterServerPartyOps.MEMBER_CHANGED_CHANNEL);
							lew.writeByte(destCh);
							lew.writeInt(playerId);
							lew.writeByte((byte) (destCh == channel.byteValue() ? (members.size() - 1) : members.size()));
							for (Party.Member member : members)
								//exclude the joining player from getting his
								//own info
								if (member.getPlayerId() != playerId)
									lew.writeInt(member.getPlayerId());
							cgi.getSession().send(lew.getBytes());
						}
					}
				}
			} finally {
				party.unlockRead();
			}
		}
	}

	private void processPartyMemberLoggedOff(LittleEndianReader packet) {
		byte lastCh = packet.readByte();
		int playerId = packet.readInt();
		int partyId = packet.readInt();
		boolean silent = packet.readBool();
		Party party = CenterServer.getInstance().getPartyDb(r.getWorld()).get(partyId);
		if (party != null) {
			party.lockWrite();
			try {
				party.setMemberChannel(playerId, PartyList.OFFLINE_CH);
				Set<Byte> partyChannels = party.allChannels();
				if (partyChannels.size() == 1 && partyChannels.contains(Byte.valueOf(PartyList.OFFLINE_CH))) {
					CenterServer.getInstance().getPartyDb(r.getWorld()).remove(partyId);
				} else {
					for (Byte channel : partyChannels) {
						for (CenterGameInterface cgi : CenterServer.getInstance().getAllServersOfWorld(r.getWorld(), ServerType.UNDEFINED)) {
							if (cgi.isOnline() && cgi.getChannels().contains(channel)) {
								Collection<Party.Member> members = party.getMembersOfChannel(channel.byteValue());
								LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(10 + 4 * members.size());
								lew.writeByte(CenterRemoteOps.PARTY_SYNCHRONIZATION);
								lew.writeByte(channel.byteValue());
								lew.writeByte(InterServerPartyOps.MEMBER_LOGGED_OFF);
								lew.writeByte(lastCh);
								lew.writeInt(playerId);
								lew.writeBool(silent);
								lew.writeByte((byte) (members.size()));
								for (Party.Member member : members)
									lew.writeInt(member.getPlayerId());
								cgi.getSession().send(lew.getBytes());
							}
						}
					}
				}
			} finally {
				party.unlockWrite();
			}
		}
	}

	private void processPartyMemberStatChanged(LittleEndianReader packet) {
		byte ch = packet.readByte();
		int playerId = packet.readInt();
		int partyId = packet.readInt();
		boolean updateLevel = packet.readBool();
		short newValue = packet.readShort();

		Party party = CenterServer.getInstance().getPartyDb(r.getWorld()).get(partyId);
		if (party != null) {
			party.lockRead();
			try {
				if (updateLevel)
					party.getMember(ch, playerId).setLevel(newValue);
				else
					party.getMember(ch, playerId).setJob(newValue);
				for (Byte channel : party.allChannels()) {
					for (CenterGameInterface cgi : CenterServer.getInstance().getAllServersOfWorld(r.getWorld(), ServerType.UNDEFINED)) {
						if (cgi.isOnline() && cgi.getChannels().contains(channel)) {
							Collection<Party.Member> members = party.getMembersOfChannel(channel.byteValue());
							LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(9 + (ch != channel.byteValue() ? 3 : 0) + members.size() * 4);
							lew.writeByte(CenterRemoteOps.PARTY_SYNCHRONIZATION);
							lew.writeByte(channel.byteValue());
							lew.writeByte(InterServerPartyOps.MEMBER_STAT_UPDATED);
							lew.writeByte(ch);
							lew.writeInt(playerId);
							if (ch != channel.byteValue()) {
								lew.writeBool(updateLevel);
								lew.writeShort(newValue);
							}
							lew.writeByte((byte) members.size());
							for (Party.Member member : members)
								lew.writeInt(member.getPlayerId());
							cgi.getSession().send(lew.getBytes());
						}
					}
				}
			} finally {
				party.unlockRead();
			}
		}
	}
}
