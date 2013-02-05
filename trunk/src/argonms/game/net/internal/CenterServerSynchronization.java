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

package argonms.game.net.internal;

import argonms.common.character.CenterServerSynchronizationOps;
import argonms.common.character.inventory.Inventory;
import argonms.common.net.internal.RemoteCenterOps;
import argonms.common.util.collections.Pair;
import argonms.common.util.input.LittleEndianReader;
import argonms.common.util.output.LittleEndianByteArrayWriter;
import argonms.common.util.output.LittleEndianWriter;
import argonms.game.GameServer;
import argonms.game.character.Chatroom;
import argonms.game.character.GameCharacter;
import argonms.game.character.GuildList;
import argonms.game.character.PartyList;
import argonms.game.net.WorldChannel;
import argonms.game.net.external.GamePackets;
import argonms.game.net.external.handler.ChatHandler;
import argonms.game.net.external.handler.GuildListHandler;
import argonms.game.net.external.handler.PartyListHandler;
import argonms.game.script.binding.ScriptObjectManipulator;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
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
	private final ConcurrentMap<Integer, GuildList> activeLocalGuilds;
	private final ConcurrentMap<Integer, Chatroom> localChatRooms;

	public CenterServerSynchronization(CrossServerSynchronization handler, WorldChannel self) {
		this.activeLocalParties = new ConcurrentHashMap<Integer, PartyList>();
		this.activeLocalGuilds = new ConcurrentHashMap<Integer, GuildList>();
		this.localChatRooms = new ConcurrentHashMap<Integer, Chatroom>();
		this.handler = handler;
		this.self = self;
	}

	private void writeCenterServerSynchronizationPacketHeader(LittleEndianWriter lew, byte opcode) {
		lew.writeByte(RemoteCenterOps.CENTER_SERVER_SYNCHRONIZATION);
		lew.writeByte(opcode);
	}

	private void writeCenterServerSynchronizationPacket(byte[] packet) {
		GameServer.getInstance().getCenterInterface().getSession().send(packet);
	}

	public void receivedCenterServerSynchronizationPacket(LittleEndianReader packet) {
		switch (packet.readByte()) {
			case CenterServerSynchronizationOps.PARTY_CREATE:
				receivedPartyCreated(packet);
				break;
			case CenterServerSynchronizationOps.PARTY_DISBAND:
				receivedPartyDisbanded(packet);
				break;
			case CenterServerSynchronizationOps.PARTY_REMOVE_PLAYER:
				receivedPartyPlayerRemoved(packet);
				break;
			case CenterServerSynchronizationOps.PARTY_ADD_PLAYER:
				receivedPartyPlayerAdded(packet);
				break;
			case CenterServerSynchronizationOps.PARTY_JOIN_ERROR:
				receivedPartyJoinError(packet);
				break;
			case CenterServerSynchronizationOps.PARTY_CHANGE_LEADER:
				receivedPartyLeaderChanged(packet);
				break;
			case CenterServerSynchronizationOps.PARTY_FETCH_LIST:
				receivedPartyListFetched(packet);
				break;
			case CenterServerSynchronizationOps.PARTY_MEMBER_CONNECTED:
				receivedPartyMemberConnected(packet);
				break;
			case CenterServerSynchronizationOps.PARTY_MEMBER_DISCONNECTED:
				receivedPartyMemberDisconnected(packet);
				break;
			case CenterServerSynchronizationOps.PARTY_MEMBER_STAT_UPDATED:
				receivedPartyMemberStatUpdated(packet);
				break;
			case CenterServerSynchronizationOps.GUILD_CONTRACT:
				receivedGuildContract(packet);
				break;
			case CenterServerSynchronizationOps.GUILD_FETCH_LIST:
				receivedGuildListFetched(packet);
				break;
			case CenterServerSynchronizationOps.GUILD_MEMBER_CONNECTED:
				receivedGuildMemberConnected(packet);
				break;
			case CenterServerSynchronizationOps.GUILD_MEMBER_DISCONNECTED:
				receivedGuildMemberDisconnected(packet);
				break;
			case CenterServerSynchronizationOps.GUILD_MEMBER_STAT_UPDATED:
				receivedGuildMemberStatUpdated(packet);
				break;
			case CenterServerSynchronizationOps.GUILD_ADD_PLAYER:
				receivedGuildPlayerAdded(packet);
				break;
			case CenterServerSynchronizationOps.GUILD_JOIN_ERROR:
				receivedGuildJoinError(packet);
				break;
			case CenterServerSynchronizationOps.GUILD_REMOVE_PLAYER:
				receivedGuildPlayerRemoved(packet);
				break;
			case CenterServerSynchronizationOps.GUILD_EXPAND:
				receivedGuildExpanded(packet);
				break;
			case CenterServerSynchronizationOps.GUILD_EMBLEM_UPDATE:
				receivedGuildEmblemUpdate(packet);
				break;
			case CenterServerSynchronizationOps.GUILD_TITLES_UPDATE:
				receivedGuildTitlesUpdate(packet);
				break;
			case CenterServerSynchronizationOps.GUILD_MEMBER_RANK_UPDATE:
				receivedGuildMemberRankUpdate(packet);
				break;
			case CenterServerSynchronizationOps.GUILD_NOTICE_UPDATE:
				receivedGuildNoticeUpdate(packet);
				break;
			case CenterServerSynchronizationOps.GUILD_CREATED:
				receivedGuildCreated(packet);
				break;
			case CenterServerSynchronizationOps.GUILD_DISBAND:
				receivedGuildDisbanded(packet);
				break;
			case CenterServerSynchronizationOps.CHATROOM_CREATED:
				receivedChatroomCreated(packet);
				break;
			case CenterServerSynchronizationOps.CHATROOM_ROOM_CHANGED: 
				receivedChatroomRoomChanged(packet);
				break;
			case CenterServerSynchronizationOps.CHATROOM_SLOT_CHANGED:
				receivedChatroomSlotChanged(packet);
				break;
		}
	}

	public void sendMakeParty(GameCharacter creator) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(13 + creator.getName().length());
		writeCenterServerSynchronizationPacketHeader(lew, CenterServerSynchronizationOps.PARTY_CREATE);
		lew.writeInt(creator.getId());
		lew.writeByte(self.getChannelId());
		lew.writeLengthPrefixedString(creator.getName());
		lew.writeShort(creator.getJob());
		lew.writeShort(creator.getLevel());

		writeCenterServerSynchronizationPacket(lew.getBytes());
	}

	public void sendDisbandParty(int partyId) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(6);
		writeCenterServerSynchronizationPacketHeader(lew, CenterServerSynchronizationOps.PARTY_DISBAND);
		lew.writeInt(partyId);

		writeCenterServerSynchronizationPacket(lew.getBytes());
	}

	public void sendLeaveParty(GameCharacter p, int partyId) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(14 + p.getName().length());
		writeCenterServerSynchronizationPacketHeader(lew, CenterServerSynchronizationOps.PARTY_REMOVE_PLAYER);
		lew.writeInt(partyId);
		lew.writeInt(p.getId());
		lew.writeByte(p.getClient().getChannel());
		lew.writeLengthPrefixedString(p.getName());
		lew.writeBool(false);

		writeCenterServerSynchronizationPacket(lew.getBytes());
	}

	public void sendJoinParty(GameCharacter p, int partyId) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(17 + p.getName().length());
		writeCenterServerSynchronizationPacketHeader(lew, CenterServerSynchronizationOps.PARTY_ADD_PLAYER);
		lew.writeInt(partyId);
		lew.writeInt(p.getId());
		lew.writeByte(p.getClient().getChannel());
		lew.writeLengthPrefixedString(p.getName());
		lew.writeShort(p.getJob());
		lew.writeShort(p.getLevel());

		writeCenterServerSynchronizationPacket(lew.getBytes());
	}

	public void sendExpelPartyMember(PartyList.Member member, int partyId) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(14 + member.getName().length());
		writeCenterServerSynchronizationPacketHeader(lew, CenterServerSynchronizationOps.PARTY_REMOVE_PLAYER);
		lew.writeInt(partyId);
		lew.writeInt(member.getPlayerId());
		lew.writeByte(member.getChannel());
		lew.writeLengthPrefixedString(member.getName());
		lew.writeBool(true);

		writeCenterServerSynchronizationPacket(lew.getBytes());
	}

	public void sendChangePartyLeader(int partyId, int newLeader) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(10);
		writeCenterServerSynchronizationPacketHeader(lew, CenterServerSynchronizationOps.PARTY_CHANGE_LEADER);
		lew.writeInt(partyId);
		lew.writeInt(newLeader);

		writeCenterServerSynchronizationPacket(lew.getBytes());
	}

	/* package-private */ void sendFillPartyList(BlockingQueue<Pair<Byte, Object>> resultConsumer, PartyList party) {
		int responseId = nextResponseId.incrementAndGet();
		blockingCalls.put(Integer.valueOf(responseId), resultConsumer);

		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(11);
		writeCenterServerSynchronizationPacketHeader(lew, CenterServerSynchronizationOps.PARTY_FETCH_LIST);
		lew.writeInt(party.getId());
		lew.writeByte(self.getChannelId());
		lew.writeInt(responseId);

		writeCenterServerSynchronizationPacket(lew.getBytes());
	}

	public PartyList sendFetchPartyList(int partyId) {
		PartyList newParty = new PartyList(partyId);
		PartyList party;
		//prevent any concurrent accessors from using an uninitialized PartyList
		newParty.lockWrite();
		try {
			party = activeLocalParties.putIfAbsent(Integer.valueOf(partyId), newParty);
			if (party == null) {
				party = newParty;
				handler.fillPartyList(party);
			}
		} finally {
			newParty.unlockWrite();
		}
		return party;
	}

	public void sendPartyMemberOnline(GameCharacter p) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(11);
		writeCenterServerSynchronizationPacketHeader(lew, CenterServerSynchronizationOps.PARTY_MEMBER_CONNECTED);
		lew.writeInt(p.getParty().getId());
		lew.writeInt(p.getId());
		lew.writeByte(self.getChannelId());

		writeCenterServerSynchronizationPacket(lew.getBytes());
	}

	public void sendPartyMemberOffline(GameCharacter exiter, boolean loggingOff) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(12);
		writeCenterServerSynchronizationPacketHeader(lew, CenterServerSynchronizationOps.PARTY_MEMBER_DISCONNECTED);
		lew.writeInt(exiter.getParty().getId());
		lew.writeInt(exiter.getId());
		lew.writeByte(self.getChannelId());
		lew.writeBool(loggingOff);

		writeCenterServerSynchronizationPacket(lew.getBytes());
	}

	public void sendPartyLevelOrJobUpdate(GameCharacter p, boolean level) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(14);
		writeCenterServerSynchronizationPacketHeader(lew, CenterServerSynchronizationOps.PARTY_MEMBER_STAT_UPDATED);
		lew.writeInt(p.getParty().getId());
		lew.writeInt(p.getId());
		lew.writeByte(self.getChannelId());
		lew.writeBool(level);
		lew.writeShort(level ? p.getLevel() : p.getJob());

		writeCenterServerSynchronizationPacket(lew.getBytes());
	}

	public void sendMakeGuild(String name, PartyList party) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(9 + name.length());
		writeCenterServerSynchronizationPacketHeader(lew, CenterServerSynchronizationOps.GUILD_CREATE);
		lew.writeByte(self.getChannelId());
		lew.writeLengthPrefixedString(name);
		lew.writeInt(party.getId());

		writeCenterServerSynchronizationPacket(lew.getBytes());
	}

	/* package-private */ void sendFillGuildList(BlockingQueue<Pair<Byte, Object>> resultConsumer, GuildList guild) {
		int responseId = nextResponseId.incrementAndGet();
		blockingCalls.put(Integer.valueOf(responseId), resultConsumer);

		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(11);
		writeCenterServerSynchronizationPacketHeader(lew, CenterServerSynchronizationOps.GUILD_FETCH_LIST);
		lew.writeInt(guild.getId());
		lew.writeByte(self.getChannelId());
		lew.writeInt(responseId);

		writeCenterServerSynchronizationPacket(lew.getBytes());
	}

	public GuildList sendFetchGuildList(int guildId) {
		GuildList newGuild = new GuildList(guildId);
		GuildList guild;
		//prevent any concurrent accessors from using an uninitialized GuildList
		newGuild.lockWrite();
		try {
			guild = activeLocalGuilds.putIfAbsent(Integer.valueOf(guildId), newGuild);
			if (guild == null) {
				guild = newGuild;
				handler.fillGuildList(guild);
			}
		} finally {
			newGuild.unlockWrite();
		}
		return guild;
	}

	public void sendGuildMemberOnline(GameCharacter p, boolean firstLogIn) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(12);
		writeCenterServerSynchronizationPacketHeader(lew, CenterServerSynchronizationOps.GUILD_MEMBER_CONNECTED);
		lew.writeInt(p.getGuild().getId());
		lew.writeInt(p.getId());
		lew.writeByte(self.getChannelId());
		lew.writeBool(firstLogIn);

		writeCenterServerSynchronizationPacket(lew.getBytes());
	}

	public void sendGuildMemberOffline(GameCharacter exiter, boolean loggingOff) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(12);
		writeCenterServerSynchronizationPacketHeader(lew, CenterServerSynchronizationOps.GUILD_MEMBER_DISCONNECTED);
		lew.writeInt(exiter.getGuild().getId());
		lew.writeInt(exiter.getId());
		lew.writeByte(self.getChannelId());
		lew.writeBool(loggingOff);

		writeCenterServerSynchronizationPacket(lew.getBytes());
	}

	public void sendGuildLevelOrJobUpdate(GameCharacter p, boolean level) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(14);
		writeCenterServerSynchronizationPacketHeader(lew, CenterServerSynchronizationOps.GUILD_MEMBER_STAT_UPDATED);
		lew.writeInt(p.getGuild().getId());
		lew.writeInt(p.getId());
		lew.writeByte(self.getChannelId());
		lew.writeBool(level);
		lew.writeShort(level ? p.getLevel() : p.getJob());

		writeCenterServerSynchronizationPacket(lew.getBytes());
	}

	public void sendJoinGuild(GameCharacter p, int guildId) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(17 + p.getName().length());
		writeCenterServerSynchronizationPacketHeader(lew, CenterServerSynchronizationOps.GUILD_ADD_PLAYER);
		lew.writeInt(guildId);
		lew.writeInt(p.getId());
		lew.writeByte(p.getClient().getChannel());
		lew.writeLengthPrefixedString(p.getName());
		lew.writeShort(p.getJob());
		lew.writeShort(p.getLevel());

		writeCenterServerSynchronizationPacket(lew.getBytes());
	}

	public void sendLeaveGuild(GameCharacter p, int guildId) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(14 + p.getName().length());
		writeCenterServerSynchronizationPacketHeader(lew, CenterServerSynchronizationOps.GUILD_REMOVE_PLAYER);
		lew.writeInt(guildId);
		lew.writeInt(p.getId());
		lew.writeByte(p.getClient().getChannel());
		lew.writeLengthPrefixedString(p.getName());
		lew.writeBool(false);

		writeCenterServerSynchronizationPacket(lew.getBytes());
	}

	public void sendExpelGuildMember(GuildList.Member member, int partyId) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(14 + member.getName().length());
		writeCenterServerSynchronizationPacketHeader(lew, CenterServerSynchronizationOps.GUILD_REMOVE_PLAYER);
		lew.writeInt(partyId);
		lew.writeInt(member.getPlayerId());
		lew.writeByte(member.getChannel());
		lew.writeLengthPrefixedString(member.getName());
		lew.writeBool(true);

		writeCenterServerSynchronizationPacket(lew.getBytes());
	}

	public void sendExpandGuild(GuildList guild, byte amount) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(7);
		writeCenterServerSynchronizationPacketHeader(lew, CenterServerSynchronizationOps.GUILD_EXPAND);
		lew.writeInt(guild.getId());
		lew.writeByte(amount);

		writeCenterServerSynchronizationPacket(lew.getBytes());
	}

	public void sendUpdateGuildEmblem(GuildList guild, short background, byte backgroundColor, short design, byte designColor) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(12);
		writeCenterServerSynchronizationPacketHeader(lew, CenterServerSynchronizationOps.GUILD_EMBLEM_UPDATE);
		lew.writeInt(guild.getId());
		lew.writeShort(background);
		lew.writeByte(backgroundColor);
		lew.writeShort(design);
		lew.writeByte(designColor);

		writeCenterServerSynchronizationPacket(lew.getBytes());
	}

	public void sendUpdateGuildTitles(GuildList guild, String[] titles) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter();
		writeCenterServerSynchronizationPacketHeader(lew, CenterServerSynchronizationOps.GUILD_TITLES_UPDATE);
		lew.writeInt(guild.getId());
		for (int i = 0; i < 5; i++)
			lew.writeLengthPrefixedString(titles[i]);

		writeCenterServerSynchronizationPacket(lew.getBytes());
	}

	public void sendUpdateGuildMemberRank(GuildList guild, int characterId, byte newRank) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(11);
		writeCenterServerSynchronizationPacketHeader(lew, CenterServerSynchronizationOps.GUILD_MEMBER_RANK_UPDATE);
		lew.writeInt(guild.getId());
		lew.writeInt(characterId);
		lew.writeByte(newRank);

		writeCenterServerSynchronizationPacket(lew.getBytes());
	}

	public void sendUpdateGuildNotice(GuildList guild, String notice) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(8 + notice.length());
		writeCenterServerSynchronizationPacketHeader(lew, CenterServerSynchronizationOps.GUILD_NOTICE_UPDATE);
		lew.writeInt(guild.getId());
		lew.writeLengthPrefixedString(notice);

		writeCenterServerSynchronizationPacket(lew.getBytes());
	}

	public void sendVoteGuildContract(GuildList guild, int characterId, boolean result) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(11);
		writeCenterServerSynchronizationPacketHeader(lew, CenterServerSynchronizationOps.GUILD_CONTRACT_VOTE);
		lew.writeInt(guild.getId());
		lew.writeInt(characterId);
		lew.writeBool(result);

		writeCenterServerSynchronizationPacket(lew.getBytes());
	}

	public void sendDisbandGuild(int guildId) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(6);
		writeCenterServerSynchronizationPacketHeader(lew, CenterServerSynchronizationOps.GUILD_DISBAND);
		lew.writeInt(guildId);

		writeCenterServerSynchronizationPacket(lew.getBytes());
	}

	private static void writeChatroomAvatar(LittleEndianWriter lew, GameCharacter p, Map<Short, Integer> equips) {
		lew.writeInt(p.getId());
		lew.writeByte((byte) equips.size());
		for (Map.Entry<Short, Integer> entry : equips.entrySet()) {
			lew.writeShort(entry.getKey().shortValue());
			lew.writeInt(entry.getValue().intValue());
		}
		lew.writeByte(p.getGender());
		lew.writeByte(p.getSkinColor());
		lew.writeInt(p.getEyes());
		lew.writeInt(p.getHair());
		lew.writeLengthPrefixedString(p.getName());
		lew.writeByte(p.getClient().getChannel());
	}

	public void sendMakeChatroom(GameCharacter creator) {
		Map<Short, Integer> equips = creator.getInventory(Inventory.InventoryType.EQUIPPED).getItemIds();
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(20 + creator.getName().length() + equips.size() * 6);
		writeCenterServerSynchronizationPacketHeader(lew, CenterServerSynchronizationOps.CHATROOM_CREATE);
		writeChatroomAvatar(lew, creator, equips);

		writeCenterServerSynchronizationPacket(lew.getBytes());
	}

	public void sendJoinChatroom(GameCharacter joiner, int roomId) {
		Map<Short, Integer> equips = joiner.getInventory(Inventory.InventoryType.EQUIPPED).getItemIds();
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(24 + joiner.getName().length() + equips.size() * 6);
		writeCenterServerSynchronizationPacketHeader(lew, CenterServerSynchronizationOps.CHATROOM_ADD_PLAYER);
		lew.writeInt(roomId);
		writeChatroomAvatar(lew, joiner, equips);

		writeCenterServerSynchronizationPacket(lew.getBytes());
	}

	public void sendLeaveChatroom(int roomId, int leaver) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(10);
		writeCenterServerSynchronizationPacketHeader(lew, CenterServerSynchronizationOps.CHATROOM_REMOVE_PLAYER);
		lew.writeInt(roomId);
		lew.writeInt(leaver);

		writeCenterServerSynchronizationPacket(lew.getBytes());
	}

	public Chatroom getChatRoom(int roomId) {
		return localChatRooms.get(Integer.valueOf(roomId));
	}

	public void chatroomPlayerChangingChannels(int playerId, Chatroom room) {
		Set<Byte> others;
		room.lockRead();
		try {
			others = room.localChannelSlots();
			others.remove(Byte.valueOf(room.positionOf(playerId)));
		} finally {
			room.unlockRead();
		}
		//if there are no other players in the chatroom on the
		//channel, delete the chatroom from our cache
		if (others.isEmpty())
			localChatRooms.remove(Integer.valueOf(room.getRoomId()));
	}

	public void sendChatroomPlayerChangedChannels(int playerId, int roomId) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(11);
		writeCenterServerSynchronizationPacketHeader(lew, CenterServerSynchronizationOps.CHATROOM_UPDATE_AVATAR_CHANNEL);
		lew.writeInt(playerId);
		lew.writeInt(roomId);
		lew.writeByte(self.getChannelId());

		writeCenterServerSynchronizationPacket(lew.getBytes());
	}

	public void sendChatroomPlayerLookUpdate(GameCharacter p, int roomId) {
		Map<Short, Integer> equips = p.getInventory(Inventory.InventoryType.EQUIPPED).getItemIds();
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(20 + 6 * equips.size());
		writeCenterServerSynchronizationPacketHeader(lew, CenterServerSynchronizationOps.CHATROOM_UPDATE_AVATAR_LOOK);
		lew.writeInt(p.getId());
		lew.writeInt(roomId);
		lew.writeByte((byte) equips.size());
		for (Map.Entry<Short, Integer> entry : equips.entrySet()) {
			lew.writeShort(entry.getKey().shortValue());
			lew.writeInt(entry.getValue().intValue());
		}
		lew.writeByte(p.getSkinColor());
		lew.writeInt(p.getEyes());
		lew.writeInt(p.getHair());

		writeCenterServerSynchronizationPacket(lew.getBytes());
	}

	private void receivedPartyCreated(LittleEndianReader packet) {
		int partyId = packet.readInt();
		int leaderId = packet.readInt();
		GameCharacter leaderPlayer = self.getPlayerById(leaderId);
		if (leaderPlayer == null)
			return;

		PartyList party = new PartyList(partyId, leaderPlayer);
		activeLocalParties.put(Integer.valueOf(partyId), party);
		leaderPlayer.setParty(party);
		leaderPlayer.getClient().getSession().send(GamePackets.writePartyCreated(partyId));
	}

	private void receivedPartyDisbanded(LittleEndianReader packet) {
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
	}

	private void receivedPartyPlayerRemoved(LittleEndianReader packet) {
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
	}

	private void receivedPartyPlayerAdded(LittleEndianReader packet) {
		int partyId = packet.readInt();
		int joinerId = packet.readInt();
		byte joinerCh = packet.readByte();
		if (joinerCh == self.getChannelId()) {
			PartyList.LocalMember joiningPlayer = new PartyList.LocalMember(self.getPlayerById(joinerId));
			PartyList newParty = new PartyList(partyId);
			PartyList party = activeLocalParties.putIfAbsent(Integer.valueOf(partyId), newParty);
			if (party == null) {
				//only can happen if the leader changed channels or
				//logged off. is that even a valid case?
				party = newParty;
				party.lockWrite();
				try {
					handler.fillPartyList(party);
				} finally {
					party.unlockWrite();
				}
			} else {
				party.lockWrite();
				try {
					party.addPlayer(joiningPlayer);
				} finally {
					party.unlockWrite();
				}
				party.lockRead();
				try {
					for (PartyList.LocalMember mem : party.getMembersInLocalChannel()) {
						if (mem == joiningPlayer)
							continue;

						mem.getPlayer().getClient().getSession().send(GamePackets.writePartyMemberJoined(party, joiningPlayer.getName()));
						mem.getPlayer().pullPartyHp();
						mem.getPlayer().pushHpToParty();
					}
				} finally {
					party.unlockRead();
				}
			}
			joiningPlayer.getPlayer().setParty(party);
			joiningPlayer.getPlayer().getClient().getSession().send(GamePackets.writePartyList(party));
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
	}

	private void receivedPartyJoinError(LittleEndianReader packet) {
		int joinFailedPlayerId = packet.readInt();
		GameCharacter joinFailedPlayer = self.getPlayerById(joinFailedPlayerId);
		if (joinFailedPlayer != null)
			joinFailedPlayer.getClient().getSession().send(GamePackets.writeSimplePartyListMessage(PartyListHandler.PARTY_FULL));
	}

	private void receivedPartyLeaderChanged(LittleEndianReader packet) {
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
	}

	private void receivedPartyListFetched(LittleEndianReader packet) {
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
	}

	private void receivedPartyMemberConnected(LittleEndianReader packet) {
		int partyId = packet.readInt();
		int entererId = packet.readInt();
		byte targetCh = packet.readByte();

		if (targetCh == self.getChannelId()) {
			PartyList newParty = new PartyList(partyId);
			PartyList party = activeLocalParties.putIfAbsent(Integer.valueOf(partyId), newParty);
			if (party == null) {
				//this should never happen, since we always call
				//sendFetchPartyList when we load the character, which
				//happens before we call sendPartyMemberEnteredChannel
				party = newParty;
				party.lockWrite();
				try {
					handler.fillPartyList(party);
				} finally {
					party.unlockWrite();
				}
				self.getPlayerById(entererId).getClient().getSession().send(GamePackets.writePartyList(party));
			} else {
				PartyList.LocalMember member;
				party.lockWrite();
				try {
					member = party.memberConnected(self.getPlayerById(entererId));
				} finally {
					party.unlockWrite();
				}
				party.lockRead();
				try {
					for (PartyList.LocalMember mem : party.getMembersInLocalChannel()) {
						if (mem == member)
							continue;

						mem.getPlayer().getClient().getSession().send(GamePackets.writePartyList(party));
						mem.getPlayer().pullPartyHp();
						mem.getPlayer().pushHpToParty();
					}
				} finally {
					party.unlockRead();
				}
				member.getPlayer().getClient().getSession().send(GamePackets.writePartyList(party));
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
	}

	private void receivedPartyMemberDisconnected(LittleEndianReader packet) {
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
	}

	private void receivedPartyMemberStatUpdated(LittleEndianReader packet) {
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
	}

	private void receivedGuildContract(LittleEndianReader packet) {
		int guildId = packet.readInt();
		int partyId = packet.readInt();
		String name = packet.readLengthPrefixedString();
		PartyList party = activeLocalParties.get(Integer.valueOf(partyId));
		if (party == null)
			return;

		GameCharacter leader = GameServer.getChannel(self.getChannelId()).getPlayerById(party.getLeader());
		switch (guildId) {
			case -2:
				leader.getClient().getSession().send(GamePackets.writeSimpleGuildListMessage(GuildListHandler.NAME_TAKEN));
				return;
			case -1:
				leader.getClient().getSession().send(GamePackets.writeSimpleGuildListMessage(GuildListHandler.GENERAL_ERROR));
				ScriptObjectManipulator.guildNameReceived(leader.getClient().getNpc(), null);
				return;
			default:
				ScriptObjectManipulator.guildNameReceived(leader.getClient().getNpc(), null);
				break;
		}

		GuildList guild = new GuildList(guildId, name, party);
		activeLocalGuilds.put(Integer.valueOf(guildId), guild);
		for (PartyList.LocalMember m : party.getMembersInLocalChannel()) {
			m.getPlayer().setGuild(guild); //TODO: this makes player look like he's already in guild to players who walk into guilds HQ; refactor this.
			m.getPlayer().getClient().getSession().send(GamePackets.writeGuildContract(party, name, leader.getName(), m.getPlayer() == leader));
		}
	}

	private void receivedGuildListFetched(LittleEndianReader packet) {
		int responseId = packet.readInt();
		String name = packet.readLengthPrefixedString();
		short emblemBackground = packet.readShort();
		byte emblemBackgroundColor = packet.readByte();
		short emblemDesign = packet.readShort();
		byte emblemDesignColor = packet.readByte();
		String[] titles = new String[5];
		for (int i = 0; i < 5; i++)
			titles[i] = packet.readLengthPrefixedString();
		byte capacity = packet.readByte();
		String notice = packet.readLengthPrefixedString();
		int gp = packet.readInt();
		int allianceId = packet.readInt();
		byte count = packet.readByte();
		GuildList.Member[] members = new GuildList.Member[count];
		for (int i = 0; i < count; i++) {
			int memberId = packet.readInt();
			byte memberCh = packet.readByte();
			byte rank = packet.readByte();
			byte signature = packet.readByte();
			byte allianceRank = packet.readByte();
			if (memberCh == self.getChannelId()) {
				members[i] = new GuildList.LocalMember(self.getPlayerById(memberId), rank, signature, allianceRank);
			} else {
				String memberName = packet.readLengthPrefixedString();
				short memberJob = packet.readShort();
				short memberLevel = packet.readShort();
				members[i] = new GuildList.RemoteMember(memberId, memberName, memberJob, memberLevel, memberCh, rank, signature, allianceRank);
			}
		}

		BlockingQueue<Pair<Byte, Object>> consumer = blockingCalls.remove(Integer.valueOf(responseId));
		if (consumer == null)
			//timed out and garbage collected
			return;

		consumer.add(new Pair<Byte, Object>(Byte.valueOf((byte) -1), new Object[] {
			name,
			Short.valueOf(emblemBackground), Byte.valueOf(emblemBackgroundColor), Short.valueOf(emblemDesign), Byte.valueOf(emblemDesignColor),
			titles, Byte.valueOf(capacity), notice, Integer.valueOf(gp), Integer.valueOf(allianceId)
		}));
		consumer.add(new Pair<Byte, Object>(Byte.valueOf((byte) -1), members));
	}

	private void receivedGuildMemberConnected(LittleEndianReader packet) {
		int guildId = packet.readInt();
		int entererId = packet.readInt();
		byte targetCh = packet.readByte();
		boolean firstLogIn = packet.readBool();

		if (targetCh == self.getChannelId()) {
			GuildList newGuild = new GuildList(guildId);
			GuildList guild = activeLocalGuilds.putIfAbsent(Integer.valueOf(guildId), newGuild);
			if (guild == null) {
				//this should never happen, since we always call
				//sendFetchPartyList when we load the character, which
				//happens before we call sendPartyMemberEnteredChannel
				guild = newGuild;
				guild.lockWrite();
				try {
					handler.fillGuildList(guild);
				} finally {
					guild.unlockWrite();
				}
				self.getPlayerById(entererId).getClient().getSession().send(GamePackets.writeGuildList(guild));
			} else {
				GuildList.LocalMember member;
				guild.lockWrite();
				try {
					member = guild.memberConnected(self.getPlayerById(entererId));
				} finally {
					guild.unlockWrite();
				}
				if (firstLogIn) {
					guild.lockRead();
					try {
						for (GuildList.LocalMember mem : guild.getMembersInLocalChannel())
							if (mem != member)
								mem.getPlayer().getClient().getSession().send(GamePackets.writeGuildMemberLoggedIn(guild, member));
					} finally {
						guild.unlockRead();
					}
					member.getPlayer().getClient().getSession().send(GamePackets.writeGuildList(guild));
				}
			}
		} else {
			GuildList guild = activeLocalGuilds.get(Integer.valueOf(guildId));
			if (guild != null) {
				GuildList.RemoteMember member;
				guild.lockWrite();
				try {
					guild.memberConnected(member = new GuildList.RemoteMember(guild.getOfflineMember(entererId), targetCh));
				} finally {
					guild.unlockWrite();
				}
				if (firstLogIn) {
					guild.lockRead();
					try {
						for (GuildList.LocalMember mem : guild.getMembersInLocalChannel())
							mem.getPlayer().getClient().getSession().send(GamePackets.writeGuildMemberLoggedIn(guild, member));
					} finally {
						guild.unlockRead();
					}
				}
			}
		}
	}

	private void receivedGuildMemberDisconnected(LittleEndianReader packet) {
		int guildId = packet.readInt();
		int exiterId = packet.readInt();
		byte lastCh = packet.readByte();
		boolean loggingOff = packet.readBool();
		//TODO: not safe for activeLocalParties when members
		//concurrently join or leave party, or enter or exit channel
		GuildList guild = activeLocalGuilds.get(Integer.valueOf(guildId));
		if (guild == null)
			return;

		boolean removeGuild = false;
		GuildList.RemoteMember member;
		guild.lockWrite();
		try {
			if (lastCh == self.getChannelId()) {
				member = guild.memberDisconnected(self.getPlayerById(exiterId));
				removeGuild = guild.getMembersInLocalChannel().isEmpty();
			} else {
				member = guild.memberDisconnected(lastCh, exiterId);
			}
		} finally {
			guild.unlockWrite();
		}
		if (removeGuild) {
			activeLocalGuilds.remove(Integer.valueOf(guildId));
		} else if (loggingOff) {
			guild.lockRead();
			try {
				for (GuildList.LocalMember mem : guild.getMembersInLocalChannel())
					mem.getPlayer().getClient().getSession().send(GamePackets.writeGuildMemberLoggedIn(guild, member));
			} finally {
				guild.unlockRead();
			}
		}
	}

	private void receivedGuildMemberStatUpdated(LittleEndianReader packet) {
		int guildId = packet.readInt();
		int updatedPlayerId = packet.readInt();
		byte updatedPlayerCh = packet.readByte();
		GuildList guild = activeLocalGuilds.get(Integer.valueOf(guildId));
		if (guild == null)
			return;

		guild.lockRead();
		try {
			if (updatedPlayerCh != self.getChannelId()) {
				boolean levelUpdated = packet.readBool();
				short newValue = packet.readShort();
				GuildList.RemoteMember member = guild.getMember(updatedPlayerCh, updatedPlayerId);
				if (levelUpdated)
					member.setLevel(newValue);
				else
					member.setJob(newValue);
			}
			GuildList.Member member = guild.getMember(updatedPlayerId);
			for (GuildList.LocalMember mem : guild.getMembersInLocalChannel())
				mem.getPlayer().getClient().getSession().send(GamePackets.writeGuildMemberLevelJobUpdate(guild, member));
		} finally {
			guild.unlockRead();
		}
	}

	private void receivedGuildPlayerAdded(LittleEndianReader packet) {
		int guildId = packet.readInt();
		int joinerId = packet.readInt();
		byte joinerCh = packet.readByte();
		if (joinerCh == self.getChannelId()) {
			GameCharacter joiningPlayer = self.getPlayerById(joinerId);
			GuildList.LocalMember joiningMember = new GuildList.LocalMember(joiningPlayer);
			GuildList newGuild = new GuildList(guildId);
			GuildList guild = activeLocalGuilds.putIfAbsent(Integer.valueOf(guildId), newGuild);
			if (guild == null) {
				//only can happen if the inviter changed channels or
				//logged off. is that even a valid case?
				guild = newGuild;
				guild.lockWrite();
				try {
					handler.fillGuildList(guild);
				} finally {
					guild.unlockWrite();
				}
			} else {
				guild.lockWrite();
				try {
					guild.addPlayer(joiningMember);
				} finally {
					guild.unlockWrite();
				}
				guild.lockRead();
				try {
					for (GuildList.LocalMember mem : guild.getMembersInLocalChannel())
						if (mem != joiningMember)
							mem.getPlayer().getClient().getSession().send(GamePackets.writeGuildMemberJoined(guild, joiningMember));
				} finally {
					guild.unlockRead();
				}
			}
			joiningPlayer.setGuild(guild);
			joiningPlayer.getClient().getSession().send(GamePackets.writeGuildList(guild));
			joiningPlayer.getMap().sendToAll(GamePackets.writeUpdateGuildName(joiningPlayer, guild.getName()), joiningPlayer);
		} else {
			String joinerName = packet.readLengthPrefixedString();
			short joinerJob = packet.readShort();
			short joinerLevel = packet.readShort();
			GuildList guild = activeLocalGuilds.get(Integer.valueOf(guildId));
			if (guild != null) {
				GuildList.RemoteMember joiningPlayer = new GuildList.RemoteMember(joinerId, joinerName, joinerJob, joinerLevel, joinerCh);
				guild.lockWrite();
				try {
					guild.addPlayer(joiningPlayer);
				} finally {
					guild.unlockWrite();
				}
				guild.lockRead();
				try {
					for (GuildList.LocalMember mem : guild.getMembersInLocalChannel())
						mem.getPlayer().getClient().getSession().send(GamePackets.writeGuildMemberJoined(guild, joiningPlayer));
				} finally {
					guild.unlockRead();
				}
			}
		}
	}

	private void receivedGuildJoinError(LittleEndianReader packet) {
		int joinFailedPlayerId = packet.readInt();
		GameCharacter joinFailedPlayer = self.getPlayerById(joinFailedPlayerId);
		if (joinFailedPlayer != null)
			joinFailedPlayer.getClient().getSession().send(GamePackets.writeSimpleGuildListMessage(GuildListHandler.GENERAL_ERROR));
	}

	private void receivedGuildPlayerRemoved(LittleEndianReader packet) {
		int guildId = packet.readInt();
		int leaverId = packet.readInt();
		byte leaverCh = packet.readByte();
		boolean leaverExpelled = packet.readBool();
		//TODO: not safe for activeLocalGuilds when members
		//concurrently join or leave guild, or enter or exit channel
		GuildList guild = activeLocalGuilds.get(Integer.valueOf(guildId));
		if (guild == null)
			return;

		boolean removeGuild = false;
		String leaverName;
		guild.lockWrite();
		try {
			if (self.getChannelId() == leaverCh) {
				GameCharacter leavingPlayer = self.getPlayerById(leaverId);
				leaverName = leavingPlayer.getName();
				guild.removePlayer(leavingPlayer);
				removeGuild = guild.getMembersInLocalChannel().isEmpty();

				leavingPlayer.setGuild(null);
				if (leaverExpelled)
					leavingPlayer.getClient().getSession().send(GamePackets.writeGuildMemberLeft(guild, leaverId, leaverName, true));
				leavingPlayer.getClient().getSession().send(GamePackets.writeGuildClear());

				//GameCenterPacketProcessor.processGuildMemberLeft
				//has an explanation for this
				if (removeGuild) {
					boolean save;
					guild.lockRead();
					try {
						save = guild.allOffline();
					} finally {
						guild.unlockRead();
					}
					if (save)
						leavingPlayer.saveCharacter();
				}
				leavingPlayer.getMap().sendToAll(GamePackets.writeUpdateGuildName(leavingPlayer, ""), leavingPlayer);
			} else {
				leaverName = packet.readLengthPrefixedString();
				guild.removePlayer(leaverCh, leaverId);
			}
		} finally {
			guild.unlockWrite();
		}
		if (removeGuild) {
			activeLocalGuilds.remove(Integer.valueOf(guildId));
		} else {
			guild.lockRead();
			try {
				for (GuildList.LocalMember mem : guild.getMembersInLocalChannel())
					mem.getPlayer().getClient().getSession().send(GamePackets.writeGuildMemberLeft(guild, leaverId, leaverName, leaverExpelled));
			} finally {
				guild.unlockRead();
			}
		}
	}

	private void receivedGuildExpanded(LittleEndianReader packet) {
		int guildId = packet.readInt();
		byte newCap = packet.readByte();
		GuildList guild = activeLocalGuilds.get(Integer.valueOf(guildId));
		if (guild == null)
			return;

		guild.lockWrite();
		try {
			guild.setCapacity(newCap);
		} finally {
			guild.unlockWrite();
		}
		guild.lockRead();
		try {
			for (GuildList.LocalMember mem : guild.getMembersInLocalChannel())
				mem.getPlayer().getClient().getSession().send(GamePackets.writeGuildCapacityChanged(guild));
		} finally {
			guild.unlockRead();
		}
	}

	private void receivedGuildEmblemUpdate(LittleEndianReader packet) {
		int guildId = packet.readInt();
		short background = packet.readShort();
		byte backgroundColor = packet.readByte();
		short design = packet.readShort();
		byte designColor = packet.readByte();
		GuildList guild = activeLocalGuilds.get(Integer.valueOf(guildId));
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
			for (GuildList.LocalMember mem : guild.getMembersInLocalChannel()) {
				mem.getPlayer().getClient().getSession().send(GamePackets.writeGuildChangeEmblem(guild));
				mem.getPlayer().getMap().sendToAll(GamePackets.writeUpdateGuildEmblem(mem.getPlayer(), guild), mem.getPlayer());
			}
		} finally {
			guild.unlockRead();
		}
	}

	private void receivedGuildTitlesUpdate(LittleEndianReader packet) {
		int guildId = packet.readInt();
		String[] titles = new String[5];
		for (int i = 0; i < 5; i++)
			titles[i] = packet.readLengthPrefixedString();
		GuildList guild = activeLocalGuilds.get(Integer.valueOf(guildId));
		if (guild == null)
			return;

		guild.lockWrite();
		try {
			guild.setTitles(titles);
		} finally {
			guild.unlockWrite();
		}
		guild.lockRead();
		try {
			for (GuildList.LocalMember mem : guild.getMembersInLocalChannel())
				mem.getPlayer().getClient().getSession().send(GamePackets.writeGuildChangeRankTitles(guild));
		} finally {
			guild.unlockRead();
		}
	}

	private void receivedGuildMemberRankUpdate(LittleEndianReader packet) {
		int guildId = packet.readInt();
		int characterId = packet.readInt();
		byte newRank = packet.readByte();
		GuildList guild = activeLocalGuilds.get(Integer.valueOf(guildId));
		if (guild == null)
			return;

		GuildList.Member member;
		guild.lockWrite();
		try {
			(member = guild.getMember(characterId)).setRank(newRank);
		} finally {
			guild.unlockWrite();
		}
		guild.lockRead();
		try {
			for (GuildList.LocalMember mem : guild.getMembersInLocalChannel())
				mem.getPlayer().getClient().getSession().send(GamePackets.writeGuildMemberChangeRank(guild, member));
		} finally {
			guild.unlockRead();
		}
	}

	private void receivedGuildNoticeUpdate(LittleEndianReader packet) {
		int guildId = packet.readInt();
		String notice = packet.readLengthPrefixedString();
		GuildList guild = activeLocalGuilds.get(Integer.valueOf(guildId));
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
			for (GuildList.LocalMember mem : guild.getMembersInLocalChannel())
				mem.getPlayer().getClient().getSession().send(GamePackets.writeGuildChangeNotice(guild));
		} finally {
			guild.unlockRead();
		}
	}

	private void receivedGuildCreated(LittleEndianReader packet) {
		int guildId = packet.readInt();
		boolean create = packet.readBool();
		GuildList guild;
		if (create)
			guild = activeLocalGuilds.get(Integer.valueOf(guildId));
		else
			guild = activeLocalGuilds.remove(Integer.valueOf(guildId));
		if (guild == null)
			return;

		if (create) {
			for (GuildList.LocalMember m : guild.getMembersInLocalChannel()) {
				m.getPlayer().getClient().getSession().send(GamePackets.writeGuildList(guild));
				m.getPlayer().getMap().sendToAll(GamePackets.writeUpdateGuildName(m.getPlayer(), guild.getName()), m.getPlayer());
			}
		} else {
			//TODO: get packet for guild contract rejection
			for (GuildList.LocalMember m : guild.getMembersInLocalChannel()) {
				m.getPlayer().setGuild(null);
				m.getPlayer().getClient().getSession().send(GamePackets.writeServerMessage(ChatHandler.TextStyle.OK_BOX.byteValue(), "Guild contract was not unanimous.", (byte) -1, true));
			}
		}
	}

	private void receivedGuildDisbanded(LittleEndianReader packet) {
		int guildId = packet.readInt();
		GuildList guild = activeLocalGuilds.remove(Integer.valueOf(guildId));
		if (guild == null)
			return;

		guild.lockRead();
		try {
			for (GuildList.LocalMember mem : guild.getMembersInLocalChannel()) {
				mem.getPlayer().setGuild(null);
				mem.getPlayer().getClient().getSession().send(GamePackets.writeGuildDisbanded(guild));
				mem.getPlayer().getMap().sendToAll(GamePackets.writeUpdateGuildName(mem.getPlayer(), ""), mem.getPlayer());
			}
		} finally {
			guild.unlockRead();
		}
	}

	private void receivedChatroomCreated(LittleEndianReader packet) {
		int roomId = packet.readInt();
		int creatorId = packet.readInt();
		GameCharacter p = self.getPlayerById(creatorId);
		if (p == null)
			return;

		Chatroom room = new Chatroom(roomId, p);
		localChatRooms.put(Integer.valueOf(roomId), room);
		p.setChatRoom(room);
	}

	private void receivedChatroomRoomChanged(LittleEndianReader packet) {
		int roomId = packet.readInt();
		int playerId = packet.readInt();
		byte position = packet.readByte();
		GameCharacter p = self.getPlayerById(playerId);
		if (roomId != 0) {
			//this will be false if joiner was in the room before and just changed channels.
			boolean firstTime = packet.readBool();
			if (position == -1)
				return; //room is full

			Set<Byte> existingLocalSlots;
			Chatroom room = localChatRooms.get(Integer.valueOf(roomId));
			if (room == null) { //first on this channel to join the chat room
				room = new Chatroom(roomId);
				for (byte pos = 0; pos < 3; pos++) {
					byte channel = packet.readByte();
					if (channel == 0)
						continue;

					int avatarPlayerId = packet.readInt();
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
					String name = packet.readLengthPrefixedString();
					room.setAvatar(pos, new Chatroom.Avatar(avatarPlayerId, gender, skin, eyes, hair, equips, name, channel), false);
				}
				//defer adding Chatroom to cache until after initialization to
				//avoid write locking for room.setAvatar above
				localChatRooms.put(Integer.valueOf(roomId), room);
				existingLocalSlots = Collections.emptySet();
			} else {
				room.lockRead();
				try {
					existingLocalSlots = room.localChannelSlots();
				} finally {
					room.unlockRead();
				}
			}
			Chatroom.Avatar a = new Chatroom.Avatar(p);
			room.lockWrite();
			try {
				room.setAvatar(position, a, true);
			} finally {
				room.unlockWrite();
			}
			p.setChatRoom(room);

			//Center server will not send us a CHATROOM_SLOT_CHANGED if the
			//joiner is on the same channel as us, to save bandwidth.
			//so send joiner's avatar to any other player in the chatroom on
			//the channel
			byte[] message = GamePackets.writeChatroomAvatar(firstTime ? Chatroom.ACT_OPEN : Chatroom.ACT_REFRESH_AVATAR, position, a, firstTime);
			room.lockRead();
			try {
				for (Byte pos : existingLocalSlots) {
					GameCharacter other = self.getPlayerById(room.getAvatar(pos.byteValue()).getPlayerId());
					if (other != null)
						other.getClient().getSession().send(message);
				}
			} finally {
				room.unlockRead();
			}

			if (firstTime) {
				//send other avatars to joiner
				p.getClient().getSession().send(GamePackets.writeChatroomJoin(position));
				room.lockRead();
				try {
					for (byte pos = 0; pos < 3; pos++) {
						a = room.getAvatar(pos);
						if (position != pos && a != null)
							p.getClient().getSession().send(GamePackets.writeChatroomAvatar(Chatroom.ACT_OPEN, pos, a, false));
					}
				} finally {
					room.unlockRead();
				}
			} else {
				//client doesn't update avatar tooltip for its own player
				//after it changes channels...
				p.getClient().getSession().send(GamePackets.writeChatroomAvatar(Chatroom.ACT_REFRESH_AVATAR, position, a, false));
			}
		} else { //left room
			if (p == null)
				return;

			Chatroom room = p.getChatRoom();
			p.setChatRoom(null);
			if (room == null)
				return;

			room.lockWrite();
			try {
				room.setAvatar(position, null, true);
			} finally {
				room.unlockWrite();
			}

			//Center server will not send us a CHATROOM_SLOT_CHANGED if the
			//leaver is on the same channel as us, to save bandwidth.
			//so notify any other player in the chatroom on the channel
			//that the slot is now unoccupied.
			byte[] message = GamePackets.writeChatroomClear(position);
			room.lockRead();
			try {
				for (Byte pos : room.localChannelSlots()) {
					GameCharacter other = self.getPlayerById(room.getAvatar(pos.byteValue()).getPlayerId());
					if (other != null)
						other.getClient().getSession().send(message);
				}
			} finally {
				room.unlockRead();
			}
			//if there are no other players in the chatroom on the
			//channel, delete the chatroom from our cache
			room.lockRead();
			try {
				if (room.localChannelSlots().isEmpty())
					localChatRooms.remove(Integer.valueOf(room.getRoomId()));
			} finally {
				room.unlockRead();
			}
		}
	}

	private void receivedChatroomSlotChanged(LittleEndianReader packet) {
		int roomId = packet.readInt();
		byte position = packet.readByte();
		int playerId = packet.readInt();
		Chatroom room = localChatRooms.get(Integer.valueOf(roomId));
		if (room == null)
			return;
		if (playerId != 0) {
			byte channel = packet.readByte();
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
			String name = packet.readLengthPrefixedString();
			byte[] message;
			Set<Byte> slotsToNotify;

			room.lockWrite();
			try {
				Chatroom.Avatar a = room.getAvatar(position);
				boolean firstTime = (a == null);
				assert firstTime || a.getPlayerId() == playerId;
				a = new Chatroom.Avatar(playerId, gender, skin, eyes, hair, equips, name, channel);
				message = GamePackets.writeChatroomAvatar(firstTime ? Chatroom.ACT_OPEN : Chatroom.ACT_REFRESH_AVATAR, position, a, firstTime);
				//Chatroom.Avatar is immutable...
				room.setAvatar(position, a, channel == self.getChannelId());
				slotsToNotify = room.localChannelSlots();
			} finally {
				room.unlockWrite();
			}

			slotsToNotify.remove(Byte.valueOf(position));
			room.lockRead();
			try {
				for (Byte pos : slotsToNotify) {
					GameCharacter other = self.getPlayerById(room.getAvatar(pos.byteValue()).getPlayerId());
					if (other != null)
						other.getClient().getSession().send(message);
				}
			} finally {
				room.unlockRead();
			}
		} else { //remove slot
			room.lockWrite();
			try {
				room.setAvatar(position, null, false);
			} finally {
				room.unlockWrite();
			}

			byte[] message = GamePackets.writeChatroomClear(position);
			room.lockRead();
			try {
				for (Byte pos : room.localChannelSlots()) {
					GameCharacter other = self.getPlayerById(room.getAvatar(pos.byteValue()).getPlayerId());
					if (other != null)
						other.getClient().getSession().send(message);
				}
				assert !room.localChannelSlots().isEmpty();
			} finally {
				room.unlockRead();
			}
		}
	}
}
