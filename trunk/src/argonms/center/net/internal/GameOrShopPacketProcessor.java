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
import argonms.common.util.input.LittleEndianReader;
import argonms.common.util.output.LittleEndianByteArrayWriter;
import argonms.common.util.output.LittleEndianWriter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 *
 * @author GoldenKevin
 */
public abstract class GameOrShopPacketProcessor extends RemoteCenterPacketProcessor {
	protected void writeCenterGameSynchronizationPacketHeader(LittleEndianWriter lew, byte destCh, byte opcode) {
		lew.writeByte(CenterRemoteOps.CENTER_SERVER_SYNCHRONIZATION);
		lew.writeByte(destCh);
		lew.writeByte(opcode);
	}

	protected void writeCenterShopSynchronizationPacketHeader(LittleEndianWriter lew, byte destWorld, byte destCh, byte opcode) {
		lew.writeByte(CenterRemoteOps.CENTER_SERVER_SYNCHRONIZATION);
		lew.writeByte(destWorld);
		lew.writeByte(destCh);
		lew.writeByte(opcode);
	}

	protected void processPartyMemberEnteredChannel(LittleEndianReader packet, byte world) {
		int partyId = packet.readInt();
		int entererId = packet.readInt();
		byte targetCh = packet.readByte();
		Party party = CenterServer.getInstance().getGroupsDb(world).getParty(partyId);
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
			for (CenterGameInterface cgi : CenterServer.getInstance().getAllServersOfWorld(world, ServerType.UNDEFINED)) {
				for (Byte channel : party.allChannels()) {
					if (!cgi.isOnline() || !cgi.getChannels().contains(channel))
						continue;

					//notify other party members
					LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(12);
					writeCenterGameSynchronizationPacketHeader(lew, channel.byteValue(), CenterServerSynchronizationOps.PARTY_MEMBER_CONNECTED);
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

	protected void processPartyMemberExitedChannel(LittleEndianReader packet, byte world) {
		int partyId = packet.readInt();
		int exiterId = packet.readInt();
		byte lastCh = packet.readByte();
		boolean loggingOff = packet.readBool();
		Party party = CenterServer.getInstance().getGroupsDb(world).getParty(partyId);
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
				CenterServer.getInstance().getGroupsDb(world).flushParty(partyId);
			partyChannels = new HashSet<Byte>(partyChannels);
			partyChannels.add(Byte.valueOf(lastCh));
			for (CenterGameInterface cgi : CenterServer.getInstance().getAllServersOfWorld(world, ServerType.UNDEFINED)) {
				for (Byte channel : partyChannels) {
					if (!cgi.isOnline() || !cgi.getChannels().contains(channel))
						continue;

					LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(13);
					writeCenterGameSynchronizationPacketHeader(lew, channel.byteValue(), CenterServerSynchronizationOps.PARTY_MEMBER_DISCONNECTED);
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

	protected void processGuildMemberEnteredChannel(LittleEndianReader packet, byte world) {
		int guildId = packet.readInt();
		int entererId = packet.readInt();
		byte targetCh = packet.readByte();
		boolean firstLogIn = packet.readBool();
		Guild guild = CenterServer.getInstance().getGroupsDb(world).getGuild(guildId);
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
			for (CenterGameInterface cgi : CenterServer.getInstance().getAllServersOfWorld(world, ServerType.UNDEFINED)) {
				for (Byte channel : guild.allChannels()) {
					if (!cgi.isOnline() || !cgi.getChannels().contains(channel))
						continue;

					//notify other party members
					LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(13);
					writeCenterGameSynchronizationPacketHeader(lew, channel.byteValue(), CenterServerSynchronizationOps.GUILD_MEMBER_CONNECTED);
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

	protected void processGuildMemberExitedChannel(LittleEndianReader packet, byte world) {
		int guildId = packet.readInt();
		int exiterId = packet.readInt();
		byte lastCh = packet.readByte();
		boolean loggingOff = packet.readBool();
		Guild guild = CenterServer.getInstance().getGroupsDb(world).getGuild(guildId);
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
				CenterServer.getInstance().getGroupsDb(world).flushGuild(guildId);
			guildChannels = new HashSet<Byte>(guildChannels);
			guildChannels.add(Byte.valueOf(lastCh));
			for (CenterGameInterface cgi : CenterServer.getInstance().getAllServersOfWorld(world, ServerType.UNDEFINED)) {
				for (Byte channel : guildChannels) {
					if (!cgi.isOnline() || !cgi.getChannels().contains(channel))
						continue;

					LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(13);
					writeCenterGameSynchronizationPacketHeader(lew, channel.byteValue(), CenterServerSynchronizationOps.GUILD_MEMBER_DISCONNECTED);
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

	protected void processCloseChatroom(LittleEndianReader packet, byte world, CenterRemoteSession respondTo) {
		int roomId = packet.readInt();
		int playerId = packet.readInt();
		Chatroom room = CenterServer.getInstance().getGroupsDb(world).getRoom(roomId);
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
		LittleEndianByteArrayWriter lew;
		if (respondTo != null) {
			lew = new LittleEndianByteArrayWriter(16);
			writeCenterGameSynchronizationPacketHeader(lew, leaver.getChannel(), CenterServerSynchronizationOps.CHATROOM_ROOM_CHANGED);
			lew.writeInt(0);
			lew.writeInt(leaver.getPlayerId());
			lew.writeByte(pos);
			lew.writeInt(roomId);
			respondTo.send(lew.getBytes());
		}

		boolean empty;
		room.lockRead();
		try {
			List<CenterGameInterface> gameServers = CenterServer.getInstance().getAllServersOfWorld(world, ServerType.UNDEFINED);
			for (Byte channel : room.allChannels()) {
				if (channel.byteValue() == leaver.getChannel() && respondTo != null)
					continue;

				for (CenterGameInterface cgi : gameServers) {
					if (!cgi.isOnline() || !cgi.getChannels().contains(channel))
						continue;

					//notify other chatroom members
					lew = new LittleEndianByteArrayWriter(12);
					writeCenterGameSynchronizationPacketHeader(lew, channel.byteValue(), CenterServerSynchronizationOps.CHATROOM_SLOT_CHANGED);
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
			CenterServer.getInstance().getGroupsDb(world).flushRoom(roomId);
	}
}
