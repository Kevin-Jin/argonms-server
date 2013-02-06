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

package argonms.center.net.internal;

import argonms.center.CenterServer;
import argonms.center.Chatroom;
import argonms.center.Guild;
import argonms.center.Party;
import argonms.common.ServerType;
import argonms.common.character.CenterServerSynchronizationOps;
import argonms.common.net.internal.CenterRemoteOps;
import argonms.common.net.internal.RemoteCenterOps;
import argonms.common.util.DatabaseManager;
import argonms.common.util.DatabaseManager.DatabaseType;
import argonms.common.util.input.LittleEndianReader;
import argonms.common.util.output.LittleEndianByteArrayWriter;
import argonms.common.util.output.LittleEndianWriter;
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

	private final CenterGameInterface r;

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
			case RemoteCenterOps.CROSS_CHANNEL_SYNCHRONIZATION:
				processCrossChannelSynchronization(packet);
				break;
			case RemoteCenterOps.CENTER_SERVER_SYNCHRONIZATION:
				processCenterServerSynchronization(packet);
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

	private void processCrossChannelSynchronization(LittleEndianReader packet) {
		byte channel = packet.readByte();
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(packet.available() + 2);
		lew.writeByte(CenterRemoteOps.CROSS_CHANNEL_SYNCHRONIZATION);
		lew.writeByte(channel);
		lew.writeBytes(packet.readBytes(packet.available()));
		byte[] message = lew.getBytes();
		for (CenterGameInterface cgi : CenterServer.getInstance().getAllServersOfWorld(r.getWorld(), ServerType.UNDEFINED))
			if (cgi.isOnline() && cgi.getChannels().contains(Byte.valueOf(channel)))
				cgi.getSession().send(message);
	}

	private void processCenterServerSynchronization(LittleEndianReader packet) {
		switch (packet.readByte()) {
			case CenterServerSynchronizationOps.PARTY_CREATE:
				processPartyCreation(packet);
				break;
			case CenterServerSynchronizationOps.PARTY_DISBAND:
				processPartyDisbandment(packet);
				break;
			case CenterServerSynchronizationOps.PARTY_REMOVE_PLAYER:
				processPartyMemberLeft(packet);
				break;
			case CenterServerSynchronizationOps.PARTY_ADD_PLAYER:
				processPartyMemberJoined(packet);
				break;
			case CenterServerSynchronizationOps.PARTY_CHANGE_LEADER:
				processPartyLeaderChange(packet);
				break;
			case CenterServerSynchronizationOps.PARTY_FETCH_LIST:
				processPartyFetchList(packet);
				break;
			case CenterServerSynchronizationOps.PARTY_MEMBER_CONNECTED:
				processPartyMemberEnteredChannel(packet);
				break;
			case CenterServerSynchronizationOps.PARTY_MEMBER_DISCONNECTED:
				processPartyMemberExitedChannel(packet);
				break;
			case CenterServerSynchronizationOps.PARTY_MEMBER_STAT_UPDATED:
				processPartyMemberStatChanged(packet);
				break;
			case CenterServerSynchronizationOps.GUILD_CREATE:
				processCreateGuild(packet);
				break;
			case CenterServerSynchronizationOps.GUILD_FETCH_LIST:
				processGuildFetchList(packet);
				break;
			case CenterServerSynchronizationOps.GUILD_MEMBER_CONNECTED:
				processGuildMemberEnteredChannel(packet);
				break;
			case CenterServerSynchronizationOps.GUILD_MEMBER_DISCONNECTED:
				processGuildMemberExitedChannel(packet);
				break;
			case CenterServerSynchronizationOps.GUILD_MEMBER_STAT_UPDATED:
				processGuildMemberStatChanged(packet);
				break;
			case CenterServerSynchronizationOps.GUILD_ADD_PLAYER:
				processGuildMemberJoined(packet);
				break;
			case CenterServerSynchronizationOps.GUILD_REMOVE_PLAYER:
				processGuildMemberLeft(packet);
				break;
			case CenterServerSynchronizationOps.GUILD_EXPAND:
				processGuildExpanded(packet);
				break;
			case CenterServerSynchronizationOps.GUILD_EMBLEM_UPDATE:
				processGuildEmblemUpdate(packet);
				break;
			case CenterServerSynchronizationOps.GUILD_TITLES_UPDATE:
				processGuildTitlesUpdate(packet);
				break;
			case CenterServerSynchronizationOps.GUILD_MEMBER_RANK_UPDATE:
				processGuildMemberRankUpdate(packet);
				break;
			case CenterServerSynchronizationOps.GUILD_NOTICE_UPDATE:
				processGuildNoticeUpdate(packet);
				break;
			case CenterServerSynchronizationOps.GUILD_CONTRACT_VOTE:
				processGuildContractVote(packet);
				break;
			case CenterServerSynchronizationOps.GUILD_DISBAND:
				processGuildDisbandment(packet);
				break;
			case CenterServerSynchronizationOps.CHATROOM_CREATE:
				processCreateChatroom(packet);
				break;
			case CenterServerSynchronizationOps.CHATROOM_ADD_PLAYER:
				processJoinChatroom(packet);
				break;
			case CenterServerSynchronizationOps.CHATROOM_REMOVE_PLAYER:
				processCloseChatroom(packet);
				break;
			case CenterServerSynchronizationOps.CHATROOM_UPDATE_AVATAR_CHANNEL:
				processUpdateChatroomPlayerChannel(packet);
				break;
			case CenterServerSynchronizationOps.CHATROOM_UPDATE_AVATAR_LOOK:
				processUpdateChatroomPlayerLook(packet);
				break;
		}
	}

	private void writeCenterServerSynchronizationPacketHeader(LittleEndianWriter lew, byte destCh, byte opcode) {
		lew.writeByte(CenterRemoteOps.CENTER_SERVER_SYNCHRONIZATION);
		lew.writeByte(destCh);
		lew.writeByte(opcode);
	}

	private void processPartyCreation(LittleEndianReader packet) {
		int creatorId = packet.readInt();
		byte creatorCh = packet.readByte();
		String creatorName = packet.readLengthPrefixedString();
		short creatorJob = packet.readShort();
		short creatorLevel = packet.readShort();
		int partyId = CenterServer.getInstance().getGroupsDb(r.getWorld()).makeParty(new Party.Member(creatorId, creatorName, creatorJob, creatorLevel, creatorCh));
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(11);
		writeCenterServerSynchronizationPacketHeader(lew, creatorCh, CenterServerSynchronizationOps.PARTY_CREATE);
		lew.writeInt(partyId);
		lew.writeInt(creatorId);
		r.getSession().send(lew.getBytes());
	}

	private void processPartyDisbandment(LittleEndianReader packet) {
		int partyId = packet.readInt();
		Party party = CenterServer.getInstance().getGroupsDb(r.getWorld()).flushParty(partyId);
		if (party == null) //if there was lag, leader may have spammed leave button
			return;

		party.lockRead();
		try {
			for (CenterGameInterface cgi : CenterServer.getInstance().getAllServersOfWorld(r.getWorld(), ServerType.UNDEFINED)) {
				for (Byte channel : party.allChannels()) {
					if (!cgi.isOnline() || !cgi.getChannels().contains(channel))
						continue;

					LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(7);
					writeCenterServerSynchronizationPacketHeader(lew, channel.byteValue(), CenterServerSynchronizationOps.PARTY_DISBAND);
					lew.writeInt(partyId);
					cgi.getSession().send(lew.getBytes());
				}
			}

			//TODO: not safe if player is being concurrently loaded
			Collection<Party.Member> offlineMembers = party.getMembersOfChannel(Party.OFFLINE_CH);
			if (offlineMembers.isEmpty())
				return;

			Connection con = null;
			PreparedStatement ps = null;
			try {
				con = DatabaseManager.getConnection(DatabaseType.STATE);
				ps = con.prepareStatement("DELETE FROM `parties` WHERE `characterid` = ?");
				for (Party.Member mem : offlineMembers) {
					ps.setInt(1, mem.getPlayerId());
					ps.addBatch();
				}
				ps.executeBatch();
			} catch (SQLException ex) {
				LOG.log(Level.WARNING, "Could not expel offline members from disbanded party " + partyId, ex);
			} finally {
				DatabaseManager.cleanup(DatabaseManager.DatabaseType.STATE, null, ps, con);
			}
		} finally {
			party.unlockRead();
		}
	}

	private void processPartyMemberLeft(LittleEndianReader packet) {
		int partyId = packet.readInt();
		int leaverId = packet.readInt();
		byte leaverChannel = packet.readByte();
		String leaverName = packet.readLengthPrefixedString();
		boolean leaverExpelled = packet.readBool();
		Party party = CenterServer.getInstance().getGroupsDb(r.getWorld()).getParty(partyId);
		if (party == null) //if there was lag, party may have been disbanded before member clicked leave
			return;

		party.lockWrite();
		try {
			//if there was lag, member may have spammed leave button
			if (!party.removePlayer(leaverId))
				return;
		} finally {
			party.unlockWrite();
		}

		party.lockRead();
		try {
			Set<Byte> partyChannels = party.allChannels();
			if (partyChannels.size() == 1 && partyChannels.contains(Byte.valueOf(Party.OFFLINE_CH)))
				//make sure the leaving player saves himself to the
				//database. otherwise if another player in the party
				//logs on before he changes channels or logs off, he
				//will still be loaded into the party list
				CenterServer.getInstance().getGroupsDb(r.getWorld()).flushParty(partyId);
			partyChannels = new HashSet<Byte>(partyChannels);
			partyChannels.add(Byte.valueOf(leaverChannel));
			for (CenterGameInterface cgi : CenterServer.getInstance().getAllServersOfWorld(r.getWorld(), ServerType.UNDEFINED)) {
				for (Byte channel : partyChannels) {
					if (!cgi.isOnline() || !cgi.getChannels().contains(channel))
						continue;

					LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(leaverChannel == channel.byteValue() ? 13 : (15 + leaverName.length()));
					writeCenterServerSynchronizationPacketHeader(lew, channel.byteValue(), CenterServerSynchronizationOps.PARTY_REMOVE_PLAYER);
					lew.writeInt(partyId);
					lew.writeInt(leaverId);
					lew.writeByte(leaverChannel);
					lew.writeBool(leaverExpelled);
					if (leaverChannel != channel.byteValue())
						lew.writeLengthPrefixedString(leaverName);
					cgi.getSession().send(lew.getBytes());
				}
			}
		} finally {
			party.unlockRead();
		}

		//TODO: not safe if player is being concurrently loaded
		if (leaverChannel != Party.OFFLINE_CH)
			return;

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

	private void processPartyMemberJoined(LittleEndianReader packet) {
		int partyId = packet.readInt();
		int joinerId = packet.readInt();
		byte joinerCh = packet.readByte();
		String joinerName = packet.readLengthPrefixedString();
		short joinerJob = packet.readShort();
		short joinerLevel = packet.readShort();
		Party party = CenterServer.getInstance().getGroupsDb(r.getWorld()).getParty(partyId);
		if (party == null) //if there was lag, party may have been disbanded before member clicked leave
			return;

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
				for (CenterGameInterface cgi : CenterServer.getInstance().getAllServersOfWorld(r.getWorld(), ServerType.UNDEFINED)) {
					for (Byte channel : party.allChannels()) {
						if (!cgi.isOnline() || !cgi.getChannels().contains(channel))
							continue;

						//notify other party members
						LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(joinerCh == channel.byteValue() ? 12 : (18 + joinerName.length()));
						writeCenterServerSynchronizationPacketHeader(lew, channel.byteValue(), CenterServerSynchronizationOps.PARTY_ADD_PLAYER);
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
			} finally {
				party.unlockRead();
			}
		} else {
			LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(7);
			writeCenterServerSynchronizationPacketHeader(lew, joinerCh, CenterServerSynchronizationOps.PARTY_JOIN_ERROR);
			lew.writeInt(joinerId);
			for (CenterGameInterface cgi : CenterServer.getInstance().getAllServersOfWorld(r.getWorld(), ServerType.UNDEFINED))
				if (cgi.isOnline() && cgi.getChannels().contains(Byte.valueOf(joinerCh)))
					cgi.getSession().send(lew.getBytes());
		}
	}

	private void processPartyLeaderChange(LittleEndianReader packet) {
		int partyId = packet.readInt();
		int newLeader = packet.readInt();
		Party party = CenterServer.getInstance().getGroupsDb(r.getWorld()).getParty(partyId);
		if (party == null) //if there was lag, party may have been disbanded before member clicked leave
			return;

		party.setLeader(newLeader);

		party.lockRead();
		try {
			for (CenterGameInterface cgi : CenterServer.getInstance().getAllServersOfWorld(r.getWorld(), ServerType.UNDEFINED)) {
				for (Byte channel : party.allChannels()) {
					if (!cgi.isOnline() || !cgi.getChannels().contains(channel))
						continue;

					LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(11);
					writeCenterServerSynchronizationPacketHeader(lew, channel.byteValue(), CenterServerSynchronizationOps.PARTY_CHANGE_LEADER);
					lew.writeInt(partyId);
					lew.writeInt(newLeader);
					cgi.getSession().send(lew.getBytes());
				}
			}
		} finally {
			party.unlockRead();
		}
	}

	private void processPartyFetchList(LittleEndianReader packet) {
		int partyId = packet.readInt();
		byte responseCh = packet.readByte();
		int responseId = packet.readInt();
		Party party = CenterServer.getInstance().getGroupsDb(r.getWorld()).getParty(partyId);
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
				ps = con.prepareStatement("SELECT `c`.`id`,`c`.`name`,`c`.`job`,`c`.`level`,`p`.`leader` "
						+ "FROM `parties` `p` LEFT JOIN `characters` `c` ON `c`.`id` = `p`.`characterid` "
						+ "WHERE `p`.`world` = ? AND `p`.`partyid` = ?");
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
			CenterServer.getInstance().getGroupsDb(r.getWorld()).setParty(partyId, party);
		}
		party.lockRead();
		try {
			LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter();
			writeCenterServerSynchronizationPacketHeader(lew, responseCh, CenterServerSynchronizationOps.PARTY_FETCH_LIST);
			lew.writeInt(responseId);
			lew.writeInt(party.getLeader());
			Collection<Party.Member> members = party.getAllMembers();
			lew.writeByte((byte) members.size());
			for (Party.Member member : members) {
				lew.writeInt(member.getPlayerId());
				lew.writeByte(member.getChannel());
				if (member.getChannel() == responseCh)
					continue;

				lew.writeLengthPrefixedString(member.getName());
				lew.writeShort(member.getJob());
				lew.writeShort(member.getLevel());
			}
			r.getSession().send(lew.getBytes());
		} finally {
			party.unlockRead();
		}
	}

	private void processPartyMemberEnteredChannel(LittleEndianReader packet) {
		int partyId = packet.readInt();
		int entererId = packet.readInt();
		byte targetCh = packet.readByte();
		Party party = CenterServer.getInstance().getGroupsDb(r.getWorld()).getParty(partyId);
		if (party == null) //if there was lag, party may have been disbanded before member clicked leave
			return;

		party.lockWrite();
		try {
			party.setMemberChannel(entererId, targetCh);
		} finally {
			party.unlockWrite();
		}
		party.lockRead();
		try {
			for (CenterGameInterface cgi : CenterServer.getInstance().getAllServersOfWorld(r.getWorld(), ServerType.UNDEFINED)) {
				for (Byte channel : party.allChannels()) {
					if (!cgi.isOnline() || !cgi.getChannels().contains(channel))
						continue;

					//notify other party members
					LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(12);
					writeCenterServerSynchronizationPacketHeader(lew, channel.byteValue(), CenterServerSynchronizationOps.PARTY_MEMBER_CONNECTED);
					lew.writeInt(partyId);
					lew.writeInt(entererId);
					lew.writeByte(targetCh);
					cgi.getSession().send(lew.getBytes());
				}
			}
		} finally {
			party.unlockRead();
		}
	}

	private void processPartyMemberExitedChannel(LittleEndianReader packet) {
		int partyId = packet.readInt();
		int exiterId = packet.readInt();
		byte lastCh = packet.readByte();
		boolean loggingOff = packet.readBool();
		Party party = CenterServer.getInstance().getGroupsDb(r.getWorld()).getParty(partyId);
		if (party == null) //if there was lag, party may have been disbanded before member clicked leave
			return;

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
				CenterServer.getInstance().getGroupsDb(r.getWorld()).flushParty(partyId);
			partyChannels = new HashSet<Byte>(partyChannels);
			partyChannels.add(Byte.valueOf(lastCh));
			for (CenterGameInterface cgi : CenterServer.getInstance().getAllServersOfWorld(r.getWorld(), ServerType.UNDEFINED)) {
				for (Byte channel : partyChannels) {
					if (!cgi.isOnline() || !cgi.getChannels().contains(channel))
						continue;

					LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(13);
					writeCenterServerSynchronizationPacketHeader(lew, channel.byteValue(), CenterServerSynchronizationOps.PARTY_MEMBER_DISCONNECTED);
					lew.writeInt(partyId);
					lew.writeInt(exiterId);
					lew.writeByte(lastCh);
					lew.writeBool(loggingOff);
					cgi.getSession().send(lew.getBytes());
				}
			}
		} finally {
			party.unlockRead();
		}
	}

	private void processPartyMemberStatChanged(LittleEndianReader packet) {
		int partyId = packet.readInt();
		int updatedPlayerId = packet.readInt();
		byte updatedPlayerCh = packet.readByte();
		boolean updateLevel = packet.readBool();
		short newValue = packet.readShort();

		Party party = CenterServer.getInstance().getGroupsDb(r.getWorld()).getParty(partyId);
		if (party == null) //if there was lag, party may have been disbanded before member clicked leave
			return;

		party.lockRead();
		try {
			if (updateLevel)
				party.getMember(updatedPlayerId).setLevel(newValue);
			else
				party.getMember(updatedPlayerId).setJob(newValue);
			for (CenterGameInterface cgi : CenterServer.getInstance().getAllServersOfWorld(r.getWorld(), ServerType.UNDEFINED)) {
				for (Byte channel : party.allChannels()) {
					if (!cgi.isOnline() || !cgi.getChannels().contains(channel))
						continue;

					LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(updatedPlayerCh == channel.byteValue() ? 12 : 15);
					writeCenterServerSynchronizationPacketHeader(lew, channel.byteValue(), CenterServerSynchronizationOps.PARTY_MEMBER_STAT_UPDATED);
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
		} finally {
			party.unlockRead();
		}
	}

	private void processCreateGuild(LittleEndianReader packet) {
		byte channel = packet.readByte();
		String name = packet.readLengthPrefixedString();
		int partyId = packet.readInt();
		int guildId;
		if (!CenterServer.getInstance().getGroupsDb(r.getWorld()).guildExists(name)) {
			guildId = CenterServer.getInstance().getGroupsDb(r.getWorld()).makeGuild(name, partyId);
			CenterServer.getInstance().getGroupsDb(r.getWorld()).makePendingGuildContractVotes(guildId, partyId);
		} else {
			guildId = -2;
		}
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(13 + name.length());
		writeCenterServerSynchronizationPacketHeader(lew, channel, CenterServerSynchronizationOps.GUILD_CONTRACT);
		lew.writeInt(guildId);
		lew.writeInt(partyId);
		lew.writeLengthPrefixedString(name);
		r.getSession().send(lew.getBytes());
	}

	private void processGuildFetchList(LittleEndianReader packet) {
		int guildId = packet.readInt();
		byte responseCh = packet.readByte();
		int responseId = packet.readInt();
		Guild guild = CenterServer.getInstance().getGroupsDb(r.getWorld()).getGuild(guildId);
		if (guild == null) {
			//TODO: not safe when two members of the same guild log in at the
			//same time or when there was only one member logged in and he
			//logged off at the same time another member logs in
			guild = new Guild();
			Connection con = null;
			PreparedStatement ps = null;
			ResultSet rs = null;
			try {
				con = DatabaseManager.getConnection(DatabaseType.STATE);
				ps = con.prepareStatement("SELECT `name`,`titles`,`capacity`,`emblemBackground`,`emblemBackgroundColor`,`emblemDesign`,`emblemDesignColor`,"
						+ "`notice`,`gp`,`alliance` FROM `guilds` WHERE `world` = ? AND `id` = ?");
				ps.setByte(1, r.getWorld());
				ps.setInt(2, guildId);
				rs = ps.executeQuery();
				if (rs.next()) {
					guild.setName(rs.getString(1));
					guild.setTitles(rs.getString(2));
					guild.setCapacity(rs.getByte(3));
					guild.setEmblem(rs.getShort(4), rs.getByte(5), rs.getShort(6), rs.getByte(7));
					guild.setNotice(rs.getString(8));
					guild.setGp(rs.getInt(9));
					guild.setAlliance(rs.getInt(10));
				}
				rs.close();
				ps.close();

				ps = con.prepareStatement("SELECT `c`.`id`,`c`.`name`,`c`.`job`,`c`.`level`,`g`.`rank`,`g`.`signature`,`g`.`alliancerank` "
						+ "FROM `guildmembers` `g` LEFT JOIN `characters` `c` ON `c`.`id` = `g`.`characterid` "
						+ "WHERE `g`.`guildid` = ?");
				ps.setInt(1, guildId);
				rs = ps.executeQuery();
				//no write locking necessary because scope is still limited
				while (rs.next()) {
					int cid = rs.getInt(1);
					guild.addPlayer(new Guild.Member(cid, rs.getString(2), rs.getShort(3), rs.getShort(4), Guild.OFFLINE_CH, rs.getByte(5), rs.getByte(6), rs.getByte(7)));
				}
			} catch (SQLException ex) {
				LOG.log(Level.WARNING, "Could not load guild " + guildId, ex);
			} finally {
				DatabaseManager.cleanup(DatabaseManager.DatabaseType.STATE, rs, ps, con);
			}
			CenterServer.getInstance().getGroupsDb(r.getWorld()).setGuild(guildId, guild);
		}
		guild.lockRead();
		try {
			LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter();
			writeCenterServerSynchronizationPacketHeader(lew, responseCh, CenterServerSynchronizationOps.GUILD_FETCH_LIST);
			lew.writeInt(responseId);
			lew.writeLengthPrefixedString(guild.getName());
			lew.writeShort(guild.getEmblemBackground());
			lew.writeByte(guild.getEmblemBackgroundColor());
			lew.writeShort(guild.getEmblemDesign());
			lew.writeByte(guild.getEmblemDesignColor());
			for (byte i = 0; i < 5; i++)
				lew.writeLengthPrefixedString(guild.getTitle(i));
			lew.writeByte(guild.getCapacity());
			lew.writeLengthPrefixedString(guild.getNotice());
			lew.writeInt(guild.getGp());
			lew.writeInt(guild.getAlliance());
			Collection<Guild.Member> members = guild.getAllMembers();
			lew.writeByte((byte) members.size());
			for (Guild.Member member : members) {
				lew.writeInt(member.getPlayerId());
				lew.writeByte(member.getChannel());
				lew.writeByte(member.getRank());
				lew.writeByte(member.getSignature());
				lew.writeByte(member.getAllianceRank());
				if (member.getChannel() == responseCh)
					continue;

				lew.writeLengthPrefixedString(member.getName());
				lew.writeShort(member.getJob());
				lew.writeShort(member.getLevel());
			}
			r.getSession().send(lew.getBytes());
		} finally {
			guild.unlockRead();
		}
	}

	private void processGuildMemberEnteredChannel(LittleEndianReader packet) {
		int guildId = packet.readInt();
		int entererId = packet.readInt();
		byte targetCh = packet.readByte();
		boolean firstLogIn = packet.readBool();
		Guild guild = CenterServer.getInstance().getGroupsDb(r.getWorld()).getGuild(guildId);
		if (guild == null) //if there was lag, guild may have been disbanded before member clicked leave
			return;

		guild.lockWrite();
		try {
			guild.setMemberChannel(entererId, targetCh);
		} finally {
			guild.unlockWrite();
		}
		guild.lockRead();
		try {
			for (CenterGameInterface cgi : CenterServer.getInstance().getAllServersOfWorld(r.getWorld(), ServerType.UNDEFINED)) {
				for (Byte channel : guild.allChannels()) {
					if (!cgi.isOnline() || !cgi.getChannels().contains(channel))
						continue;

					//notify other party members
					LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(13);
					writeCenterServerSynchronizationPacketHeader(lew, channel.byteValue(), CenterServerSynchronizationOps.GUILD_MEMBER_CONNECTED);
					lew.writeInt(guildId);
					lew.writeInt(entererId);
					lew.writeByte(targetCh);
					lew.writeBool(firstLogIn);
					cgi.getSession().send(lew.getBytes());
				}
			}
		} finally {
			guild.unlockRead();
		}
	}

	private void processGuildMemberExitedChannel(LittleEndianReader packet) {
		int guildId = packet.readInt();
		int exiterId = packet.readInt();
		byte lastCh = packet.readByte();
		boolean loggingOff = packet.readBool();
		Guild guild = CenterServer.getInstance().getGroupsDb(r.getWorld()).getGuild(guildId);
		if (guild == null) //if there was lag, guild may have been disbanded before member clicked leave
			return;

		guild.lockWrite();
		try {
			guild.setMemberChannel(exiterId, Guild.OFFLINE_CH);
		} finally {
			guild.unlockWrite();
		}
		guild.lockRead();
		try {
			Set<Byte> guildChannels = guild.allChannels();
			if (loggingOff && guildChannels.size() == 1 && guildChannels.contains(Byte.valueOf(Guild.OFFLINE_CH)))
				CenterServer.getInstance().getGroupsDb(r.getWorld()).flushGuild(guildId);
			guildChannels = new HashSet<Byte>(guildChannels);
			guildChannels.add(Byte.valueOf(lastCh));
			for (CenterGameInterface cgi : CenterServer.getInstance().getAllServersOfWorld(r.getWorld(), ServerType.UNDEFINED)) {
				for (Byte channel : guildChannels) {
					if (!cgi.isOnline() || !cgi.getChannels().contains(channel))
						continue;

					LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(13);
					writeCenterServerSynchronizationPacketHeader(lew, channel.byteValue(), CenterServerSynchronizationOps.GUILD_MEMBER_DISCONNECTED);
					lew.writeInt(guildId);
					lew.writeInt(exiterId);
					lew.writeByte(lastCh);
					lew.writeBool(loggingOff);
					cgi.getSession().send(lew.getBytes());
				}
			}
		} finally {
			guild.unlockRead();
		}
	}

	private void processGuildMemberStatChanged(LittleEndianReader packet) {
		int guildId = packet.readInt();
		int updatedPlayerId = packet.readInt();
		byte updatedPlayerCh = packet.readByte();
		boolean updateLevel = packet.readBool();
		short newValue = packet.readShort();

		Guild guild = CenterServer.getInstance().getGroupsDb(r.getWorld()).getGuild(guildId);
		if (guild == null)
			return;

		guild.lockRead();
		try {
			if (updateLevel)
				guild.getMember(updatedPlayerId).setLevel(newValue);
			else
				guild.getMember(updatedPlayerId).setJob(newValue);
			for (CenterGameInterface cgi : CenterServer.getInstance().getAllServersOfWorld(r.getWorld(), ServerType.UNDEFINED)) {
				for (Byte channel : guild.allChannels()) {
					if (!cgi.isOnline() || !cgi.getChannels().contains(channel))
						continue;

					LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(updatedPlayerCh == channel.byteValue() ? 12 : 15);
					writeCenterServerSynchronizationPacketHeader(lew, channel.byteValue(), CenterServerSynchronizationOps.GUILD_MEMBER_STAT_UPDATED);
					lew.writeInt(guildId);
					lew.writeInt(updatedPlayerId);
					lew.writeByte(updatedPlayerCh);
					if (updatedPlayerCh != channel.byteValue()) {
						lew.writeBool(updateLevel);
						lew.writeShort(newValue);
					}
					cgi.getSession().send(lew.getBytes());
				}
			}
		} finally {
			guild.unlockRead();
		}
	}

	private void processGuildMemberJoined(LittleEndianReader packet) {
		int guildId = packet.readInt();
		int joinerId = packet.readInt();
		byte joinerCh = packet.readByte();
		String joinerName = packet.readLengthPrefixedString();
		short joinerJob = packet.readShort();
		short joinerLevel = packet.readShort();
		Guild guild = CenterServer.getInstance().getGroupsDb(r.getWorld()).getGuild(guildId);
		if (guild == null) //if there was lag, guild may have been disbanded before member clicked leave
			return;

		boolean full;
		guild.lockWrite();
		try {
			if (!guild.isFull()) {
				full = false;
				guild.addPlayer(new Guild.Member(joinerId, joinerName, joinerJob, joinerLevel, joinerCh, guild.getLowestRank()));
			} else {
				full = true;
			}
		} finally {
			guild.unlockWrite();
		}
		if (!full) {
			guild.lockRead();
			try {
				for (CenterGameInterface cgi : CenterServer.getInstance().getAllServersOfWorld(r.getWorld(), ServerType.UNDEFINED)) {
					for (Byte channel : guild.allChannels()) {
						if (!cgi.isOnline() || !cgi.getChannels().contains(channel))
							continue;

						//notify other guild members
						LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(joinerCh == channel.byteValue() ? 12 : (18 + joinerName.length()));
						writeCenterServerSynchronizationPacketHeader(lew, channel.byteValue(), CenterServerSynchronizationOps.GUILD_ADD_PLAYER);
						lew.writeInt(guildId);
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
			} finally {
				guild.unlockRead();
			}
		} else {
			LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(7);
			writeCenterServerSynchronizationPacketHeader(lew, joinerCh, CenterServerSynchronizationOps.GUILD_JOIN_ERROR);
			lew.writeInt(joinerId);
			for (CenterGameInterface cgi : CenterServer.getInstance().getAllServersOfWorld(r.getWorld(), ServerType.UNDEFINED))
				if (cgi.isOnline() && cgi.getChannels().contains(Byte.valueOf(joinerCh)))
					cgi.getSession().send(lew.getBytes());
		}
	}

	private void processGuildMemberLeft(LittleEndianReader packet) {
		int guildId = packet.readInt();
		int leaverId = packet.readInt();
		byte leaverChannel = packet.readByte();
		String leaverName = packet.readLengthPrefixedString();
		boolean leaverExpelled = packet.readBool();
		Guild guild = CenterServer.getInstance().getGroupsDb(r.getWorld()).getGuild(guildId);
		if (guild == null)
			return;

		guild.lockWrite();
		try {
			//if there was lag, member may have spammed leave button
			if (!guild.removePlayer(leaverId))
				return;
		} finally {
			guild.unlockWrite();
		}

		guild.lockRead();
		try {
			Set<Byte> guildChannels = guild.allChannels();
			if (guildChannels.size() == 1 && guildChannels.contains(Byte.valueOf(Guild.OFFLINE_CH)))
				//make sure the leaving player saves himself to the
				//database. otherwise if another player in the guild
				//logs on before he changes channels or logs off, he
				//will still be loaded into the guild list
				CenterServer.getInstance().getGroupsDb(r.getWorld()).flushGuild(guildId);
			guildChannels = new HashSet<Byte>(guildChannels);
			guildChannels.add(Byte.valueOf(leaverChannel));
			for (CenterGameInterface cgi : CenterServer.getInstance().getAllServersOfWorld(r.getWorld(), ServerType.UNDEFINED)) {
				for (Byte channel : guildChannels) {
					if (!cgi.isOnline() || !cgi.getChannels().contains(channel))
						continue;

					LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(leaverChannel == channel.byteValue() ? 13 : (15 + leaverName.length()));
					writeCenterServerSynchronizationPacketHeader(lew, channel.byteValue(), CenterServerSynchronizationOps.GUILD_REMOVE_PLAYER);
					lew.writeInt(guildId);
					lew.writeInt(leaverId);
					lew.writeByte(leaverChannel);
					lew.writeBool(leaverExpelled);
					if (leaverChannel != channel.byteValue())
						lew.writeLengthPrefixedString(leaverName);
					cgi.getSession().send(lew.getBytes());
				}
			}
		} finally {
			guild.unlockRead();
		}

		//TODO: not safe if player is being concurrently loaded
		if (leaverChannel != Guild.OFFLINE_CH)
			return;

		Connection con = null;
		PreparedStatement ps = null;
		try {
			con = DatabaseManager.getConnection(DatabaseType.STATE);
			ps = con.prepareStatement("DELETE FROM `guildmembers` WHERE `characterid` = ?");
			ps.setInt(1, leaverId);
			ps.executeUpdate();

			//TODO: add note
		} catch (SQLException ex) {
			LOG.log(Level.WARNING, "Could not expel offline member " + leaverName + " from guild " + guildId, ex);
		} finally {
			DatabaseManager.cleanup(DatabaseManager.DatabaseType.STATE, null, ps, con);
		}
	}

	private void processGuildExpanded(LittleEndianReader packet) {
		int guildId = packet.readInt();
		byte amount = packet.readByte();
		Guild guild = CenterServer.getInstance().getGroupsDb(r.getWorld()).getGuild(guildId);
		if (guild == null)
			return;

		guild.lockWrite();
		try {
			guild.setCapacity((byte) (guild.getCapacity() + amount));
		} finally {
			guild.unlockWrite();
		}
		guild.lockRead();
		try {
			Connection con = null;
			PreparedStatement ps = null;
			try {
				con = DatabaseManager.getConnection(DatabaseType.STATE);
				ps = con.prepareStatement("UPDATE `guilds` SET `capacity` = ? WHERE `id` = ?");
				ps.setByte(1, guild.getCapacity());
				ps.setInt(2, guildId);
				ps.executeUpdate();
			} catch (SQLException ex) {
				LOG.log(Level.WARNING, "Could not update capacity of guild " + guildId, ex);
			} finally {
				DatabaseManager.cleanup(DatabaseManager.DatabaseType.STATE, null, ps, con);
			}
			for (CenterGameInterface cgi : CenterServer.getInstance().getAllServersOfWorld(r.getWorld(), ServerType.UNDEFINED)) {
				for (Byte channel : guild.allChannels()) {
					if (!cgi.isOnline() || !cgi.getChannels().contains(channel))
						continue;

					LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(8);
					writeCenterServerSynchronizationPacketHeader(lew, channel.byteValue(), CenterServerSynchronizationOps.GUILD_EXPAND);
					lew.writeInt(guildId);
					lew.writeByte(guild.getCapacity());
					cgi.getSession().send(lew.getBytes());
				}
			}
		} finally {
			guild.unlockRead();
		}
	}

	private void processGuildEmblemUpdate(LittleEndianReader packet) {
		int guildId = packet.readInt();
		short background = packet.readShort();
		byte backgroundColor = packet.readByte();
		short design = packet.readShort();
		byte designColor = packet.readByte();
		Guild guild = CenterServer.getInstance().getGroupsDb(r.getWorld()).getGuild(guildId);
		if (guild == null)
			return;

		guild.lockWrite();
		try {
			guild.setEmblem(background, backgroundColor, design, designColor);
		} finally {
			guild.unlockWrite();
		}
		guild.lockRead();
		try {
			Connection con = null;
			PreparedStatement ps = null;
			try {
				con = DatabaseManager.getConnection(DatabaseType.STATE);
				ps = con.prepareStatement("UPDATE `guilds` SET `emblemBackground` = ?, `emblemBackgroundColor` = ?, `emblemDesign` = ?, `emblemDesignColor` = ? WHERE `id` = ?");
				ps.setShort(1, background);
				ps.setByte(2, backgroundColor);
				ps.setShort(3, design);
				ps.setByte(4, designColor);
				ps.setInt(5, guildId);
				ps.executeUpdate();
			} catch (SQLException ex) {
				LOG.log(Level.WARNING, "Could not update emblem of guild " + guildId, ex);
			} finally {
				DatabaseManager.cleanup(DatabaseManager.DatabaseType.STATE, null, ps, con);
			}
			for (CenterGameInterface cgi : CenterServer.getInstance().getAllServersOfWorld(r.getWorld(), ServerType.UNDEFINED)) {
				for (Byte channel : guild.allChannels()) {
					if (!cgi.isOnline() || !cgi.getChannels().contains(channel))
						continue;

					LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(9);
					writeCenterServerSynchronizationPacketHeader(lew, channel.byteValue(), CenterServerSynchronizationOps.GUILD_EMBLEM_UPDATE);
					lew.writeInt(guildId);
					lew.writeShort(background);
					lew.writeByte(backgroundColor);
					lew.writeShort(design);
					lew.writeByte(designColor);
					cgi.getSession().send(lew.getBytes());
				}
			}
		} finally {
			guild.unlockRead();
		}
	}

	private void processGuildTitlesUpdate(LittleEndianReader packet) {
		int guildId = packet.readInt();
		String[] titles = new String[5];
		for (int i = 0; i < 5; i++)
			titles[i] = packet.readLengthPrefixedString();
		Guild guild = CenterServer.getInstance().getGroupsDb(r.getWorld()).getGuild(guildId);
		if (guild == null)
			return;

		guild.lockWrite();
		try {
			guild.setTitles(titles);
		} finally {
			guild.unlockWrite();
		}
		StringBuilder joined = new StringBuilder(titles[0]);
		for (int i = 1; i < 5; i++)
			joined.append(',').append(titles[i]);
		guild.lockRead();
		try {
			Connection con = null;
			PreparedStatement ps = null;
			try {
				con = DatabaseManager.getConnection(DatabaseType.STATE);
				ps = con.prepareStatement("UPDATE `guilds` SET `titles` = ? WHERE `id` = ?");
				ps.setString(1, joined.toString());
				ps.setInt(2, guildId);
				ps.executeUpdate();
			} catch (SQLException ex) {
				LOG.log(Level.WARNING, "Could not update titles of guild " + guildId, ex);
			} finally {
				DatabaseManager.cleanup(DatabaseManager.DatabaseType.STATE, null, ps, con);
			}
			for (CenterGameInterface cgi : CenterServer.getInstance().getAllServersOfWorld(r.getWorld(), ServerType.UNDEFINED)) {
				for (Byte channel : guild.allChannels()) {
					if (!cgi.isOnline() || !cgi.getChannels().contains(channel))
						continue;

					LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter();
					writeCenterServerSynchronizationPacketHeader(lew, channel.byteValue(), CenterServerSynchronizationOps.GUILD_TITLES_UPDATE);
					lew.writeInt(guildId);
					for (int i = 0; i < 5; i++)
						lew.writeLengthPrefixedString(titles[i]);
					cgi.getSession().send(lew.getBytes());
				}
			}
		} finally {
			guild.unlockRead();
		}
	}

	private void processGuildMemberRankUpdate(LittleEndianReader packet) {
		int guildId = packet.readInt();
		int characterId = packet.readInt();
		byte newRank = packet.readByte();
		Guild guild = CenterServer.getInstance().getGroupsDb(r.getWorld()).getGuild(guildId);
		if (guild == null)
			return;

		Guild.Member member = guild.getMember(characterId);
		guild.lockWrite();
		try {
			member = guild.getMember(characterId);
			member.setRank(newRank);
		} finally {
			guild.unlockWrite();
		}
		guild.lockRead();
		try {
			if (member.getChannel() == Guild.OFFLINE_CH) {
				Connection con = null;
				PreparedStatement ps = null;
				try {
					con = DatabaseManager.getConnection(DatabaseType.STATE);
					ps = con.prepareStatement("UPDATE `guildmembers` SET `rank` = ? WHERE `guildid` = ? AND `characterid` = ?");
					ps.setByte(1, newRank);
					ps.setInt(2, guildId);
					ps.setInt(3, member.getPlayerId());
					ps.executeUpdate();
				} catch (SQLException ex) {
					LOG.log(Level.WARNING, "Could not update rank of offline guild member " + member.getName(), ex);
				} finally {
					DatabaseManager.cleanup(DatabaseManager.DatabaseType.STATE, null, ps, con);
				}
			}
			for (CenterGameInterface cgi : CenterServer.getInstance().getAllServersOfWorld(r.getWorld(), ServerType.UNDEFINED)) {
				for (Byte channel : guild.allChannels()) {
					if (!cgi.isOnline() || !cgi.getChannels().contains(channel))
						continue;

					LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(12);
					writeCenterServerSynchronizationPacketHeader(lew, channel.byteValue(), CenterServerSynchronizationOps.GUILD_MEMBER_RANK_UPDATE);
					lew.writeInt(guildId);
					lew.writeInt(characterId);
					lew.writeByte(newRank);
					cgi.getSession().send(lew.getBytes());
				}
			}
		} finally {
			guild.unlockRead();
		}
	}

	private void processGuildNoticeUpdate(LittleEndianReader packet) {
		int guildId = packet.readInt();
		String notice = packet.readLengthPrefixedString();
		Guild guild = CenterServer.getInstance().getGroupsDb(r.getWorld()).getGuild(guildId);
		if (guild == null)
			return;

		guild.lockWrite();
		try {
			guild.setNotice(notice);
		} finally {
			guild.unlockWrite();
		}
		guild.lockRead();
		try {
			Connection con = null;
			PreparedStatement ps = null;
			try {
				con = DatabaseManager.getConnection(DatabaseType.STATE);
				ps = con.prepareStatement("UPDATE `guilds` SET `notice` = ? WHERE `id` = ?");
				ps.setString(1, notice);
				ps.setInt(2, guildId);
				ps.executeUpdate();
			} catch (SQLException ex) {
				LOG.log(Level.WARNING, "Could not update notice of guild " + guildId, ex);
			} finally {
				DatabaseManager.cleanup(DatabaseManager.DatabaseType.STATE, null, ps, con);
			}

			for (CenterGameInterface cgi : CenterServer.getInstance().getAllServersOfWorld(r.getWorld(), ServerType.UNDEFINED)) {
				for (Byte channel : guild.allChannels()) {
					if (!cgi.isOnline() || !cgi.getChannels().contains(channel))
						continue;

					LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(9 + notice.length());
					writeCenterServerSynchronizationPacketHeader(lew, channel.byteValue(), CenterServerSynchronizationOps.GUILD_NOTICE_UPDATE);
					lew.writeInt(guildId);
					lew.writeLengthPrefixedString(notice);
					cgi.getSession().send(lew.getBytes());
				}
			}
		} finally {
			guild.unlockRead();
		}
	}

	private void processGuildContractVote(LittleEndianReader packet) {
		int guildId = packet.readInt();
		int characterId = packet.readInt();
		boolean accept = packet.readBool();
		Guild guild;
		Set<Integer> pendingContractVotes;
		if (accept)
			guild = CenterServer.getInstance().getGroupsDb(r.getWorld()).getGuild(guildId);
		else
			guild = CenterServer.getInstance().getGroupsDb(r.getWorld()).flushGuild(guildId);
		if (accept)
			pendingContractVotes = CenterServer.getInstance().getGroupsDb(r.getWorld()).getPendingGuildContractVotes(guildId);
		else
			pendingContractVotes = CenterServer.getInstance().getGroupsDb(r.getWorld()).removePendingGuildContractVotes(guildId);
		if (pendingContractVotes == null || guild == null)
			return;

		if (accept) {
			pendingContractVotes.remove(Integer.valueOf(characterId));
			if (pendingContractVotes.isEmpty()) {
				CenterServer.getInstance().getGroupsDb(r.getWorld()).removePendingGuildContractVotes(guildId);
				for (CenterGameInterface cgi : CenterServer.getInstance().getAllServersOfWorld(r.getWorld(), ServerType.UNDEFINED)) {
					for (Byte channel : guild.allChannels()) {
						if (!cgi.isOnline() || !cgi.getChannels().contains(channel))
							continue;

						LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(7);
						writeCenterServerSynchronizationPacketHeader(lew, channel, CenterServerSynchronizationOps.GUILD_CREATED);
						lew.writeInt(guildId);
						lew.writeBool(true);
						r.getSession().send(lew.getBytes());
					}
				}
			}
		} else {
			Connection con = null;
			PreparedStatement ps = null;
			try {
				con = DatabaseManager.getConnection(DatabaseType.STATE);
				ps = con.prepareStatement("DELETE FROM `guilds` WHERE `id` = ?");
				ps.setInt(1, guildId);
				ps.executeUpdate();
			} catch (SQLException ex) {
				LOG.log(Level.WARNING, "Could not remove guild " + guildId + " after contract rejection", ex);
			} finally {
				DatabaseManager.cleanup(DatabaseManager.DatabaseType.STATE, null, ps, con);
			}

			for (CenterGameInterface cgi : CenterServer.getInstance().getAllServersOfWorld(r.getWorld(), ServerType.UNDEFINED)) {
				for (Byte channel : guild.allChannels()) {
					if (!cgi.isOnline() || !cgi.getChannels().contains(channel))
						continue;

					LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(7);
					writeCenterServerSynchronizationPacketHeader(lew, channel, CenterServerSynchronizationOps.GUILD_CREATED);
					lew.writeInt(guildId);
					lew.writeBool(false);
					r.getSession().send(lew.getBytes());
				}
			}
		}
	}

	private void processGuildDisbandment(LittleEndianReader packet) {
		int guildId = packet.readInt();
		Guild guild = CenterServer.getInstance().getGroupsDb(r.getWorld()).flushGuild(guildId);
		if (guild == null)
			return;

		guild.lockRead();
		try {
			for (CenterGameInterface cgi : CenterServer.getInstance().getAllServersOfWorld(r.getWorld(), ServerType.UNDEFINED)) {
				for (Byte channel : guild.allChannels()) {
					if (!cgi.isOnline() || !cgi.getChannels().contains(channel))
						continue;

					LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(7);
					writeCenterServerSynchronizationPacketHeader(lew, channel.byteValue(), CenterServerSynchronizationOps.GUILD_DISBAND);
					lew.writeInt(guildId);
					cgi.getSession().send(lew.getBytes());
				}
			}

			Connection con = null;
			PreparedStatement ps = null;
			try {
				con = DatabaseManager.getConnection(DatabaseType.STATE);
				ps = con.prepareStatement("DELETE FROM `guilds` WHERE `id` = ?");
				ps.setInt(1, guildId);
				ps.executeUpdate();
				//TODO: add note to offline?
			} catch (SQLException ex) {
				LOG.log(Level.WARNING, "Could not disband guild " + guildId, ex);
			} finally {
				DatabaseManager.cleanup(DatabaseManager.DatabaseType.STATE, null, ps, con);
			}
		} finally {
			guild.unlockRead();
		}
	}

	private void processCreateChatroom(LittleEndianReader packet) {
		int creatorId = packet.readInt();
		Map<Short, Integer> equips = new HashMap<Short, Integer>();
		for (byte i = packet.readByte(); i > 0; i--) {
			short slot = packet.readShort();
			int itemId = packet.readInt();
			equips.put(Short.valueOf(slot), Integer.valueOf(itemId));
		}
		byte gender = packet.readByte();
		byte skin = packet.readByte();
		int eyes = packet.readInt();
		int hair = packet.readInt();
		String creatorName = packet.readLengthPrefixedString();
		byte creatorCh = packet.readByte();
		int roomId = CenterServer.getInstance().getGroupsDb(r.getWorld()).makeRoom(new Chatroom.Avatar(creatorId, gender, skin, eyes, hair, equips, creatorName, creatorCh));
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(11);
		writeCenterServerSynchronizationPacketHeader(lew, creatorCh, CenterServerSynchronizationOps.CHATROOM_CREATED);
		lew.writeInt(roomId);
		lew.writeInt(creatorId);
		r.getSession().send(lew.getBytes());
	}

	private void processJoinChatroom(LittleEndianReader packet) {
		int roomId = packet.readInt();
		int joinerId = packet.readInt();
		Map<Short, Integer> equips = new HashMap<Short, Integer>();
		for (byte i = packet.readByte(); i > 0; i--) {
			short slot = packet.readShort();
			int itemId = packet.readInt();
			equips.put(Short.valueOf(slot), Integer.valueOf(itemId));
		}
		byte gender = packet.readByte();
		byte skin = packet.readByte();
		int eyes = packet.readInt();
		int hair = packet.readInt();
		String joinerName = packet.readLengthPrefixedString();
		byte joinerCh = packet.readByte();
		Chatroom room = CenterServer.getInstance().getGroupsDb(r.getWorld()).getRoom(roomId);
		if (room == null)
			return;

		boolean sendAvatars;
		byte position;
		room.lockWrite();
		try {
			sendAvatars = !room.allChannels().contains(Byte.valueOf(joinerCh));
			position = room.addPlayer(new Chatroom.Avatar(joinerId, gender, skin, eyes, hair, equips, joinerName, joinerCh));
		} finally {
			room.unlockWrite();
		}
		sendAvatars &= position != -1;

		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter();
		writeCenterServerSynchronizationPacketHeader(lew, joinerCh, CenterServerSynchronizationOps.CHATROOM_ROOM_CHANGED);
		lew.writeInt(roomId);
		lew.writeInt(joinerId);
		lew.writeByte(position);
		lew.writeBool(true);
		//TODO: race condition - game server side could be in the process of
		//removing an avatar in joinerCh while center server side still has
		//joinerCh in room.allChannels(), and when game server receives this
		//packet, it's missing the avatars from other channels because this
		//portion wasn't sent.
		if (sendAvatars) {
			room.lockRead();
			try {
				for (byte i = 0; i < 3; i++) {
					Chatroom.Avatar a = room.getPlayer(i);
					if (position == i || a == null) {
						lew.writeByte((byte) 0);
						continue;
					}

					lew.writeByte(a.getChannel());
					lew.writeInt(a.getPlayerId());
					lew.writeByte((byte) a.getEquips().size());
					for (Map.Entry<Short, Integer> entry : a.getEquips().entrySet()) {
						lew.writeShort(entry.getKey().shortValue());
						lew.writeInt(entry.getValue().intValue());
					}
					lew.writeByte(a.getGender());
					lew.writeByte(a.getSkin());
					lew.writeInt(a.getEyes());
					lew.writeInt(a.getHair());
					lew.writeLengthPrefixedString(a.getName());
				}
			} finally {
				room.unlockRead();
			}
		}
		r.getSession().send(lew.getBytes());

		if (position == -1)
			return;

		room.lockRead();
		try {
			List<CenterGameInterface> gameServers = CenterServer.getInstance().getAllServersOfWorld(r.getWorld(), ServerType.UNDEFINED);
			for (Byte channel : room.allChannels()) {
				if (channel.byteValue() == joinerCh)
					continue;

				for (CenterGameInterface cgi : gameServers) {
					if (!cgi.isOnline() || !cgi.getChannels().contains(channel))
						continue;

					//notify other party members
					lew = new LittleEndianByteArrayWriter(26 + joinerName.length() + 6 * equips.size());
					writeCenterServerSynchronizationPacketHeader(lew, channel.byteValue(), CenterServerSynchronizationOps.CHATROOM_SLOT_CHANGED);
					lew.writeInt(roomId);
					lew.writeByte(position);
					lew.writeInt(joinerId);
					lew.writeByte(joinerCh);
					lew.writeByte((byte) equips.size());
					for (Map.Entry<Short, Integer> entry : equips.entrySet()) {
						lew.writeShort(entry.getKey().shortValue());
						lew.writeInt(entry.getValue().intValue());
					}
					lew.writeByte(gender);
					lew.writeByte(skin);
					lew.writeInt(eyes);
					lew.writeInt(hair);
					lew.writeLengthPrefixedString(joinerName);

					cgi.getSession().send(lew.getBytes());
				}
			}
		} finally {
			room.unlockRead();
		}
	}

	private void processCloseChatroom(LittleEndianReader packet) {
		int roomId = packet.readInt();
		int playerId = packet.readInt();
		Chatroom room = CenterServer.getInstance().getGroupsDb(r.getWorld()).getRoom(roomId);
		if (room == null)
			return;

		byte pos;
		room.lockRead();
		try {
			pos = room.positionOf(playerId);
		} finally {
			room.unlockRead();
		}
		if (pos == -1)
			return;

		Chatroom.Avatar leaver;
		room.lockWrite();
		try {
			leaver = room.removePlayer(pos);
		} finally {
			room.unlockWrite();
		}
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(11);
		writeCenterServerSynchronizationPacketHeader(lew, leaver.getChannel(), CenterServerSynchronizationOps.CHATROOM_ROOM_CHANGED);
		lew.writeInt(0);
		lew.writeInt(leaver.getPlayerId());
		lew.writeByte(pos);
		r.getSession().send(lew.getBytes());

		boolean empty;
		room.lockRead();
		try {
			List<CenterGameInterface> gameServers = CenterServer.getInstance().getAllServersOfWorld(r.getWorld(), ServerType.UNDEFINED);
			for (Byte channel : room.allChannels()) {
				if (channel.byteValue() == leaver.getChannel())
					continue;

				for (CenterGameInterface cgi : gameServers) {
					if (!cgi.isOnline() || !cgi.getChannels().contains(channel))
						continue;

					//notify other chatroom members
					lew = new LittleEndianByteArrayWriter(12);
					writeCenterServerSynchronizationPacketHeader(lew, channel.byteValue(), CenterServerSynchronizationOps.CHATROOM_SLOT_CHANGED);
					lew.writeInt(roomId);
					lew.writeByte(pos);
					lew.writeInt(0);

					cgi.getSession().send(lew.getBytes());
				}
			}
			empty = room.isEmpty();
		} finally {
			room.unlockRead();
		}
		if (empty)
			CenterServer.getInstance().getGroupsDb(r.getWorld()).flushRoom(roomId);
	}

	private void processUpdateChatroomPlayerChannel(LittleEndianReader packet) {
		int playerId = packet.readInt();
		int roomId = packet.readInt();
		byte newChannel = packet.readByte();
		Chatroom room = CenterServer.getInstance().getGroupsDb(r.getWorld()).getRoom(roomId);
		if (room == null)
			return;

		byte pos;
		room.lockRead();
		try {
			pos = room.positionOf(playerId);
		} finally {
			room.unlockRead();
		}
		if (pos == -1)
			return;

		boolean sendAvatars;
		Chatroom.Avatar a;
		room.lockWrite();
		try {
			sendAvatars = !room.allChannels().contains(Byte.valueOf(newChannel));
			a = room.getPlayer(pos);
			a = new Chatroom.Avatar(a, newChannel);
			room.setPlayer(pos, a);
		} finally {
			room.unlockWrite();
		}

		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(11);
		writeCenterServerSynchronizationPacketHeader(lew, newChannel, CenterServerSynchronizationOps.CHATROOM_ROOM_CHANGED);
		lew.writeInt(roomId);
		lew.writeInt(playerId);
		lew.writeByte(pos);
		lew.writeBool(false);
		if (sendAvatars) {
			room.lockRead();
			try {
				for (byte i = 0; i < 3; i++) {
					Chatroom.Avatar b = room.getPlayer(i);
					if (pos == i || b == null) {
						lew.writeByte((byte) 0);
						continue;
					}

					lew.writeByte(b.getChannel());
					lew.writeInt(b.getPlayerId());
					lew.writeByte((byte) b.getEquips().size());
					for (Map.Entry<Short, Integer> entry : b.getEquips().entrySet()) {
						lew.writeShort(entry.getKey().shortValue());
						lew.writeInt(entry.getValue().intValue());
					}
					lew.writeByte(b.getGender());
					lew.writeByte(b.getSkin());
					lew.writeInt(b.getEyes());
					lew.writeInt(b.getHair());
					lew.writeLengthPrefixedString(b.getName());
				}
			} finally {
				room.unlockRead();
			}
		}
		r.getSession().send(lew.getBytes());

		room.lockRead();
		try {
			List<CenterGameInterface> gameServers = CenterServer.getInstance().getAllServersOfWorld(r.getWorld(), ServerType.UNDEFINED);
			for (Byte channel : room.allChannels()) {
				if (channel.byteValue() == newChannel)
					continue;

				for (CenterGameInterface cgi : gameServers) {
					if (!cgi.isOnline() || !cgi.getChannels().contains(channel))
						continue;

					//notify other chatroom members
					lew = new LittleEndianByteArrayWriter(26 + a.getName().length() + 6 * a.getEquips().size());
					writeCenterServerSynchronizationPacketHeader(lew, channel.byteValue(), CenterServerSynchronizationOps.CHATROOM_SLOT_CHANGED);
					lew.writeInt(roomId);
					lew.writeByte(pos);
					lew.writeInt(playerId);
					lew.writeByte(newChannel);
					lew.writeByte((byte) a.getEquips().size());
					for (Map.Entry<Short, Integer> entry : a.getEquips().entrySet()) {
						lew.writeShort(entry.getKey().shortValue());
						lew.writeInt(entry.getValue().intValue());
					}
					lew.writeByte(a.getGender());
					lew.writeByte(a.getSkin());
					lew.writeInt(a.getEyes());
					lew.writeInt(a.getHair());
					lew.writeLengthPrefixedString(a.getName());

					cgi.getSession().send(lew.getBytes());
				}
			}
		} finally {
			room.unlockRead();
		}
	}

	private void processUpdateChatroomPlayerLook(LittleEndianReader packet) {
		int playerId = packet.readInt();
		int roomId = packet.readInt();
		Map<Short, Integer> equips = new HashMap<Short, Integer>();
		for (byte i = packet.readByte(); i > 0; i--) {
			short slot = packet.readShort();
			int itemId = packet.readInt();
			equips.put(Short.valueOf(slot), Integer.valueOf(itemId));
		}
		byte skin = packet.readByte();
		int eyes = packet.readInt();
		int hair = packet.readInt();
		Chatroom room = CenterServer.getInstance().getGroupsDb(r.getWorld()).getRoom(roomId);
		if (room == null)
			return;

		byte pos;
		room.lockRead();
		try {
			pos = room.positionOf(playerId);
		} finally {
			room.unlockRead();
		}
		if (pos == -1)
			return;

		Chatroom.Avatar a;
		room.lockWrite();
		try {
			a = room.getPlayer(pos);
			a = new Chatroom.Avatar(a, equips, skin, eyes, hair);
			room.setPlayer(pos, a);
		} finally {
			room.unlockWrite();
		}

		room.lockRead();
		try {
			for (CenterGameInterface cgi : CenterServer.getInstance().getAllServersOfWorld(r.getWorld(), ServerType.UNDEFINED)) {
				for (Byte channel : room.allChannels()) {
					if (!cgi.isOnline() || !cgi.getChannels().contains(channel))
						continue;

					LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(26 + a.getName().length() + 6 * a.getEquips().size());
					writeCenterServerSynchronizationPacketHeader(lew, channel.byteValue(), CenterServerSynchronizationOps.CHATROOM_SLOT_CHANGED);
					lew.writeInt(roomId);
					lew.writeByte(pos);
					lew.writeInt(playerId);
					lew.writeByte(a.getChannel());
					lew.writeByte((byte) a.getEquips().size());
					for (Map.Entry<Short, Integer> entry : a.getEquips().entrySet()) {
						lew.writeShort(entry.getKey().shortValue());
						lew.writeInt(entry.getValue().intValue());
					}
					lew.writeByte(a.getGender());
					lew.writeByte(a.getSkin());
					lew.writeInt(a.getEyes());
					lew.writeInt(a.getHair());
					lew.writeLengthPrefixedString(a.getName());

					cgi.getSession().send(lew.getBytes());
				}
			}
		} finally {
			room.unlockRead();
		}
	}
}
