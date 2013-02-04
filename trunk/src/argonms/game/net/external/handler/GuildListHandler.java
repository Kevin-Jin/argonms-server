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

package argonms.game.net.external.handler;

import argonms.common.net.external.CheatTracker;
import argonms.common.net.external.ClientSendOps;
import argonms.common.util.input.LittleEndianReader;
import argonms.common.util.output.LittleEndianByteArrayWriter;
import argonms.game.GameServer;
import argonms.game.character.GameCharacter;
import argonms.game.character.GuildList;
import argonms.game.net.external.GameClient;
import argonms.game.net.external.GamePackets;
import argonms.game.script.binding.ScriptNpc;
import argonms.game.script.binding.ScriptObjectManipulator;

/**
 *
 * @author GoldenKevin
 */
public class GuildListHandler {
	public static final byte //guild receive op codes
		CREATE = 0x02,
		INVITE = 0x05,
		JOIN = 0x06,
		LEAVE = 0x07,
		EXPEL = 0x08,
		CHANGE_RANK_STRING = 0x0D,
		CHANGE_PLAYER_RANK = 0x0E,
		CHANGE_EMBLEM = 0x0F,
		CHANGE_NOTICE = 0x10,
		GUILD_CONTRACT_RESPONSE = 0x1E
	;

	public static final byte //guild send op codes
		ASK_NAME = 0x01,
		GENERAL_ERROR = 0x02,
		GUILD_CONTRACT = 0x03,
		INVITE_SENT = 0x05,
		ASK_EMBLEM = 0x11,
		LIST = 0x1A,
		NAME_TAKEN = 0x1C,
		LEVEL_TOO_LOW = 0x23,
		JOINED_GUILD = 0x27,
		ALREADY_IN_GUILD = 0x28,
		CANNOT_FIND = 0x2A,
		LEFT_GUILD = 0x2C,
		EXPELLED_FROM_GUILD = 0x2F,
		DISBANDED_GUILD = 0x32,
		INVITE_DENIED = 0x37,
		CAPACITY_CHANGED = 0x3A,
		LEVEL_JOB_CHANGED = 0x3C,
		CHANNEL_CHANGE = 0x3D,
		RANK_TITLES_CHANGED = 0x3E,
		RANK_CHANGED = 0x40,
		EMBLEM_CHANGED = 0x42,
		NOTICE_CHANGED = 0x44,
		GUILD_GP_CHANGED = 0x48,
		SHOW_GUILD_RANK_BOARD = 0x49
	;

	public static void handleListModification(LittleEndianReader packet, GameClient gc) {
		GameCharacter p = gc.getPlayer();
		GuildList currentGuild = p.getGuild();
		switch (packet.readByte()) {
			case CREATE: {
				String name = packet.readLengthPrefixedString();
				ScriptNpc npc = gc.getNpc();
				if (npc != null)
					ScriptObjectManipulator.guildNameReceived(npc, name);
				break;
			}
			case INVITE: {
				//invites only check players on current channel
				String name = packet.readLengthPrefixedString();
				GameCharacter invited = GameServer.getChannel(gc.getChannel()).getPlayerByName(name);
				if (currentGuild != null)
					if (!currentGuild.isFull())
						if (invited != null)
							if (invited.getGuild() == null)
								invited.getClient().getSession().send(writeGuildInvite(currentGuild.getId(), p.getName()));
							else
								gc.getSession().send(GamePackets.writeSimpleGuildListMessage(ALREADY_IN_GUILD));
						else
							gc.getSession().send(GamePackets.writeSimpleGuildListMessage(CANNOT_FIND));
					else
						CheatTracker.get(gc).suspicious(CheatTracker.Infraction.POSSIBLE_PACKET_EDITING, "Tried to invite player to full guild");
				else
					CheatTracker.get(gc).suspicious(CheatTracker.Infraction.POSSIBLE_PACKET_EDITING, "Tried to invite player to nonexistent guild");
				break;
			}
			case JOIN: {
				int guildId = packet.readInt();
				int characterId = packet.readInt();
				if (characterId != p.getId()) {
					CheatTracker.get(gc).suspicious(CheatTracker.Infraction.CERTAIN_PACKET_EDITING, "Tried to join guild without being invited");
					return;
				}
				//TODO: check if player was actually invited

				if (currentGuild == null)
					GameServer.getChannel(gc.getChannel()).getCrossServerInterface().sendJoinGuild(p, guildId);
				else
					gc.getSession().send(GamePackets.writeSimpleGuildListMessage(ALREADY_IN_GUILD));
				break;
			}
			case LEAVE: {
				int characterId = packet.readInt();
				String characterName = packet.readLengthPrefixedString();
				if (characterId != p.getId() || !p.getName().equals(characterName) || currentGuild == null) {
					CheatTracker.get(gc).suspicious(CheatTracker.Infraction.POSSIBLE_PACKET_EDITING, "Tried to leave guild without being in one");
					return;
				}

				GameServer.getChannel(gc.getChannel()).getCrossServerInterface().sendLeaveGuild(p, currentGuild.getId());
				break;
			}
			case EXPEL: {
				int expelled = packet.readInt();
				/*String expelledName = */packet.readLengthPrefixedString();
				if (currentGuild == null || currentGuild.getMember(p.getId()).getRank() > 2) {
					CheatTracker.get(gc).suspicious(CheatTracker.Infraction.POSSIBLE_PACKET_EDITING, "Tried to expel guild member without having privileges");
					return;
				}

				GameServer.getChannel(gc.getChannel()).getCrossServerInterface().sendExpelGuildMember(currentGuild.getMember(expelled), currentGuild.getId());
				break;
			}
			case CHANGE_RANK_STRING: {
				String[] titles = new String[5];
				for (int i = 0; i < 5; i++) {
					titles[i] = packet.readLengthPrefixedString();
					if (titles[i].length() > 12 || (i <= 2 || i > 2 && !titles[i].isEmpty()) && titles[i].length() < 4) {
						CheatTracker.get(gc).suspicious(CheatTracker.Infraction.CERTAIN_PACKET_EDITING, "Tried to set invalid guild title");
						return;
					}
				}
				if (currentGuild == null || currentGuild.getMember(p.getId()).getRank() > 2) {
					CheatTracker.get(gc).suspicious(CheatTracker.Infraction.POSSIBLE_PACKET_EDITING, "Tried to edit guild titles without having privileges");
					return;
				}

				GameServer.getChannel(gc.getChannel()).getCrossServerInterface().sendUpdateGuildTitles(currentGuild, titles);
				break;
			}
			case CHANGE_PLAYER_RANK: {
				int characterId = packet.readInt();
				byte newRank = packet.readByte();
				if (currentGuild == null || currentGuild.getMember(p.getId()).getRank() > 2 || newRank >= 2 && currentGuild.getMember(p.getId()).getRank() > 1) {
					CheatTracker.get(gc).suspicious(CheatTracker.Infraction.POSSIBLE_PACKET_EDITING, "Tried to edit guild rankings without having privileges");
					return;
				}

				GameServer.getChannel(gc.getChannel()).getCrossServerInterface().sendUpdateGuildRank(currentGuild, characterId, newRank);
				break;
			}
			case CHANGE_EMBLEM: {
				short background = packet.readShort();
				byte backgroundColor = packet.readByte();
				short design = packet.readShort();
				byte designColor = packet.readByte();
				ScriptNpc npc = gc.getNpc();
				if (npc != null)
					ScriptObjectManipulator.guildEmblemReceived(npc, background, backgroundColor, design, designColor);
				break;
			}
			case CHANGE_NOTICE: {
				String notice = packet.readLengthPrefixedString();
				if (notice.length() > 100) {
					CheatTracker.get(gc).suspicious(CheatTracker.Infraction.CERTAIN_PACKET_EDITING, "Tried to set invalid guild notice");
					return;
				}

				GameServer.getChannel(gc.getChannel()).getCrossServerInterface().sendUpdateGuildNotice(currentGuild, notice);
				break;
			}
			case GUILD_CONTRACT_RESPONSE: {
				int characterId = packet.readInt();
				boolean accept = packet.readBool();
				if (characterId != p.getId()) {
					CheatTracker.get(gc).suspicious(CheatTracker.Infraction.CERTAIN_PACKET_EDITING, "Tried to accept guild contract of another player");
					return;
				}

				GameServer.getChannel(gc.getChannel()).getCrossServerInterface().sendVoteGuildContract(currentGuild, characterId, accept);
				break;
			}
		}
	}

	public static void handleDenyRequest(LittleEndianReader packet, GameClient gc) {
		packet.readByte();
		String from = packet.readLengthPrefixedString();
		String to = packet.readLengthPrefixedString();
		GameCharacter inviter = GameServer.getChannel(gc.getChannel()).getPlayerByName(from);
		if (inviter != null) //check if inviter changed channels or logged off
			inviter.getClient().getSession().send(writeGuildInviteRejected(to));
	}

	private static byte[] writeGuildInviteRejected(String name) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(5 + name.length());

		lew.writeShort(ClientSendOps.GUILD_LIST);
		lew.writeByte(INVITE_DENIED);
		lew.writeLengthPrefixedString(name);

		return lew.getBytes();
	}

	private static byte[] writeGuildInvite(int guildId, String inviter) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(9 + inviter.length());

		lew.writeShort(ClientSendOps.GUILD_LIST);
		lew.writeByte(INVITE_SENT);
		lew.writeInt(guildId);
		lew.writeLengthPrefixedString(inviter);

		return lew.getBytes();
	}
}
