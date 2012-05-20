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
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
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
				processPartyMemberLeft(packet);
				break;
			case InterServerPartyOps.ADD_PLAYER:
				processPartyMemberJoined(packet);
				break;
			case InterServerPartyOps.CHANGE_LEADER:
				processPartyLeaderChange(packet);
				break;
			case InterServerPartyOps.FETCH_LIST:
				processPartyFetchList(packet);
				break;
			case InterServerPartyOps.MEMBER_CONNECTED:
				processPartyMemberEnteredChannel(packet);
				break;
			case InterServerPartyOps.MEMBER_DISCONNECTED:
				processPartyMemberExitedChannel(packet);
				break;
			case InterServerPartyOps.MEMBER_STAT_UPDATED:
				processPartyMemberStatChanged(packet);
				break;
		}
	}

	private void processPartyCreation(LittleEndianReader packet) {
		int creatorId = packet.readInt();
		byte creatorCh = packet.readByte();
		String creatorName = packet.readLengthPrefixedString();
		short creatorJob = packet.readShort();
		short creatorLevel = packet.readShort();
		int partyId = CenterServer.getInstance().getPartyDb(r.getWorld()).makeNewParty(new Party.Member(creatorId, creatorName, creatorJob, creatorLevel, creatorCh));
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(11);
		lew.writeByte(CenterRemoteOps.PARTY_SYNCHRONIZATION);
		lew.writeByte(creatorCh);
		lew.writeByte(InterServerPartyOps.CREATE);
		lew.writeInt(partyId);
		lew.writeInt(creatorId);
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
							LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(7);
							lew.writeByte(CenterRemoteOps.PARTY_SYNCHRONIZATION);
							lew.writeByte(channel.byteValue());
							lew.writeByte(InterServerPartyOps.DISBAND);
							lew.writeInt(partyId);
							cgi.getSession().send(lew.getBytes());
						}
					}
				}

				//TODO: not safe if player is being concurrently loaded
				Collection<Party.Member> offlineMembers = party.getMembersOfChannel(Party.OFFLINE_CH);
				if (!offlineMembers.isEmpty()) {
					Connection con = null;
					PreparedStatement ps = null;
					try {
						con = DatabaseManager.getConnection(DatabaseType.STATE);
						ps = con.prepareStatement("DELETE FROM `parties` WHERE `characterid` = ?");
						for (Party.Member mem : offlineMembers) {
							ps.setInt(1, mem.getPlayerId());
							ps.executeUpdate();
						}
					} catch (SQLException ex) {
						LOG.log(Level.WARNING, "Could not expel offline members from disbanded party " + partyId, ex);
					} finally {
						DatabaseManager.cleanup(DatabaseManager.DatabaseType.STATE, null, ps, con);
					}
				}
			} finally {
				party.unlockRead();
			}
		}
	}

	private void processPartyMemberLeft(LittleEndianReader packet) {
		int partyId = packet.readInt();
		int leaverId = packet.readInt();
		byte leaverChannel = packet.readByte();
		String leaverName = packet.readLengthPrefixedString();
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
					if (partyChannels.size() == 1 && partyChannels.contains(Byte.valueOf(Party.OFFLINE_CH)))
						//make sure the leaving player saves himself to the
						//database. otherwise if another player in the party
						//logs on before he changes channels or logs off, he
						//will still be loaded into the party list
						CenterServer.getInstance().getPartyDb(r.getWorld()).remove(partyId);
					partyChannels = new HashSet<Byte>(partyChannels);
					partyChannels.add(Byte.valueOf(leaverChannel));
					for (Byte channel : partyChannels) {
						for (CenterGameInterface cgi : CenterServer.getInstance().getAllServersOfWorld(r.getWorld(), ServerType.UNDEFINED)) {
							if (cgi.isOnline() && cgi.getChannels().contains(channel)) {
								LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(leaverChannel == channel.byteValue() ? 13 : (15 + leaverName.length()));
								lew.writeByte(CenterRemoteOps.PARTY_SYNCHRONIZATION);
								lew.writeByte(channel.byteValue());
								lew.writeByte(InterServerPartyOps.REMOVE_PLAYER);
								lew.writeInt(partyId);
								lew.writeInt(leaverId);
								lew.writeByte(leaverChannel);
								lew.writeBool(leaverExpelled);
								if (leaverChannel != channel.byteValue())
									lew.writeLengthPrefixedString(leaverName);
								cgi.getSession().send(lew.getBytes());
							}
						}
					}
				} finally {
					party.unlockRead();
				}

				//TODO: not safe if player is being concurrently loaded
				if (leaverChannel == Party.OFFLINE_CH) {
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
				}
			}
		}
	}

	private void processPartyMemberJoined(LittleEndianReader packet) {
		int partyId = packet.readInt();
		int joinerId = packet.readInt();
		byte joinerCh = packet.readByte();
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
								//notify other party members
								LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(joinerCh == channel.byteValue() ? 12 : (18 + joinerName.length()));
								lew.writeByte(CenterRemoteOps.PARTY_SYNCHRONIZATION);
								lew.writeByte(channel.byteValue());
								lew.writeByte(InterServerPartyOps.ADD_PLAYER);
								lew.writeInt(partyId);
								lew.writeInt(joinerId);
								lew.writeByte(joinerCh);
								//we don't need to send this information if the
								//two are on the same channel
								if (joinerCh != channel.byteValue()) {
									lew.writeLengthPrefixedString(joinerName);
									lew.writeShort(joinerJob);
									lew.writeShort(joinerLevel);
								}
								cgi.getSession().send(lew.getBytes());
							}
						}
					}
				} finally {
					party.unlockRead();
				}
			} else {
				LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(7);
				lew.writeByte(CenterRemoteOps.PARTY_SYNCHRONIZATION);
				lew.writeByte(joinerCh);
				lew.writeByte(InterServerPartyOps.JOIN_ERROR);
				lew.writeInt(joinerId);
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
							LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(11);
							lew.writeByte(CenterRemoteOps.PARTY_SYNCHRONIZATION);
							lew.writeByte(channel.byteValue());
							lew.writeByte(InterServerPartyOps.CHANGE_LEADER);
							lew.writeInt(partyId);
							lew.writeInt(newLeader);
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
		int partyId = packet.readInt();
		int excludePlayerId = packet.readInt();
		byte responseCh = packet.readByte();
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
					party.addPlayer(new Party.Member(cid, rs.getString(2), rs.getShort(3), rs.getShort(4), Party.OFFLINE_CH));
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
				if (cgi.isOnline() && cgi.getChannels().contains(Byte.valueOf(responseCh))) {
					LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter();
					lew.writeByte(CenterRemoteOps.PARTY_SYNCHRONIZATION);
					lew.writeByte(responseCh);
					lew.writeByte(InterServerPartyOps.FETCH_LIST);
					lew.writeInt(responseId);
					lew.writeInt(party.getLeader());
					Collection<Party.Member> members = party.getAllMembers();
					lew.writeByte((byte) (members.size() - 1));
					for (Party.Member member : members) {
						if (member.getPlayerId() != excludePlayerId) {
							lew.writeInt(member.getPlayerId());
							lew.writeByte(member.getChannel());
							if (member.getChannel() != responseCh) {
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

	private void processPartyMemberEnteredChannel(LittleEndianReader packet) {
		int partyId = packet.readInt();
		int entererId = packet.readInt();
		byte targetCh = packet.readByte();
		Party party = CenterServer.getInstance().getPartyDb(r.getWorld()).get(partyId);
		if (party != null) {
			party.lockWrite();
			try {
				party.setMemberChannel(entererId, targetCh);
			} finally {
				party.unlockWrite();
			}
			party.lockRead();
			try {
				for (Byte channel : party.allChannels()) {
					for (CenterGameInterface cgi : CenterServer.getInstance().getAllServersOfWorld(r.getWorld(), ServerType.UNDEFINED)) {
						if (cgi.isOnline() && cgi.getChannels().contains(channel)) {
							//notify other party members
							LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(12);
							lew.writeByte(CenterRemoteOps.PARTY_SYNCHRONIZATION);
							lew.writeByte(channel.byteValue());
							lew.writeByte(InterServerPartyOps.MEMBER_CONNECTED);
							lew.writeInt(partyId);
							lew.writeInt(entererId);
							lew.writeByte(targetCh);
							cgi.getSession().send(lew.getBytes());
						}
					}
				}
			} finally {
				party.unlockRead();
			}
		}
	}

	private void processPartyMemberExitedChannel(LittleEndianReader packet) {
		int partyId = packet.readInt();
		int exiterId = packet.readInt();
		byte lastCh = packet.readByte();
		boolean loggingOff = packet.readBool();
		Party party = CenterServer.getInstance().getPartyDb(r.getWorld()).get(partyId);
		if (party != null) {
			party.lockWrite();
			try {
				party.setMemberChannel(exiterId, Party.OFFLINE_CH);
			} finally {
				party.unlockWrite();
			}
			party.lockRead();
			try {
				Set<Byte> partyChannels = party.allChannels();
				if (loggingOff && partyChannels.size() == 1 && partyChannels.contains(Byte.valueOf(Party.OFFLINE_CH)))
					CenterServer.getInstance().getPartyDb(r.getWorld()).remove(partyId);
				partyChannels = new HashSet<Byte>(partyChannels);
				partyChannels.add(Byte.valueOf(lastCh));
				for (Byte channel : partyChannels) {
					for (CenterGameInterface cgi : CenterServer.getInstance().getAllServersOfWorld(r.getWorld(), ServerType.UNDEFINED)) {
						if (cgi.isOnline() && cgi.getChannels().contains(channel)) {
							LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(13);
							lew.writeByte(CenterRemoteOps.PARTY_SYNCHRONIZATION);
							lew.writeByte(channel.byteValue());
							lew.writeByte(InterServerPartyOps.MEMBER_DISCONNECTED);
							lew.writeInt(partyId);
							lew.writeInt(exiterId);
							lew.writeByte(lastCh);
							lew.writeBool(loggingOff);
							cgi.getSession().send(lew.getBytes());
						}
					}
				}
			} finally {
				party.unlockRead();
			}
		}
	}

	private void processPartyMemberStatChanged(LittleEndianReader packet) {
		int partyId = packet.readInt();
		int updatedPlayerId = packet.readInt();
		byte updatedPlayerCh = packet.readByte();
		boolean updateLevel = packet.readBool();
		short newValue = packet.readShort();

		Party party = CenterServer.getInstance().getPartyDb(r.getWorld()).get(partyId);
		if (party != null) {
			party.lockRead();
			try {
				if (updateLevel)
					party.getMember(updatedPlayerCh, updatedPlayerId).setLevel(newValue);
				else
					party.getMember(updatedPlayerCh, updatedPlayerId).setJob(newValue);
				for (Byte channel : party.allChannels()) {
					for (CenterGameInterface cgi : CenterServer.getInstance().getAllServersOfWorld(r.getWorld(), ServerType.UNDEFINED)) {
						if (cgi.isOnline() && cgi.getChannels().contains(channel)) {
							LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(updatedPlayerCh == channel.byteValue() ? 12 : 15);
							lew.writeByte(CenterRemoteOps.PARTY_SYNCHRONIZATION);
							lew.writeByte(channel.byteValue());
							lew.writeByte(InterServerPartyOps.MEMBER_STAT_UPDATED);
							lew.writeInt(partyId);
							lew.writeInt(updatedPlayerId);
							lew.writeByte(updatedPlayerCh);
							if (updatedPlayerCh != channel.byteValue()) {
								lew.writeBool(updateLevel);
								lew.writeShort(newValue);
							}
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
