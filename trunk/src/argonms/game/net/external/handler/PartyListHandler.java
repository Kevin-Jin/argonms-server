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

import argonms.common.net.external.ClientSendOps;
import argonms.common.util.input.LittleEndianReader;
import argonms.common.util.output.LittleEndianByteArrayWriter;
import argonms.game.GameServer;
import argonms.game.character.GameCharacter;
import argonms.game.character.PartyList;
import argonms.game.net.external.GameClient;
import argonms.game.net.external.GamePackets;

/**
 *
 * @author GoldenKevin
 */
public class PartyListHandler {
	public static final byte //party receive op codes
		CREATE = 0x01,
		LEAVE = 0x02,
		JOIN = 0x03,
		INVITE = 0x04,
		EXPEL = 0x05,
		CHANGE_LEADER = 0x06
	;

	public static final byte //party send op codes
		INVITE_SENT = 0x04,
		SILENT_LIST_UPDATE = 0x07,
		PARTY_CREATED = 0x08,
		IS_BEGINNER = 0x0A,
		LEFT_PARTY = 0x0C,
		NOT_IN_PARTY = 0x0D,
		JOINED_PARTY = 0x0F,
		ALREADY_IN_PARTY = 0x10,
		PARTY_FULL = 0x11,
		CANNOT_FIND = 0x13,
		BUSY = 0x16,
		INVITE_DENIED = 0x17,
		LEADER_CHANGED = 0x1A,
		NOT_IN_VICINITY = 0x1B,
		NO_MEMBERS_IN_VICINITY = 0x1C,
		NOT_IN_CHANNEL = 0x1D,
		IS_GM = 0x1F
	;

	public static void handleListModification(LittleEndianReader packet, GameClient gc) {
		GameCharacter p = gc.getPlayer();
		PartyList currentParty = p.getParty();
		switch (packet.readByte()) {
			case CREATE: {
				if (currentParty == null)
					GameServer.getChannel(gc.getChannel()).getInterChannelInterface().sendMakeParty(p);
				else
					gc.getSession().send(GamePackets.writeSimplePartyListMessage(ALREADY_IN_PARTY));
				break;
			}
			case LEAVE: {
				if (currentParty != null)
					if (currentParty.getLeader() == p.getId())
						GameServer.getChannel(gc.getChannel()).getInterChannelInterface().sendDisbandParty(currentParty.getId());
					else
						GameServer.getChannel(gc.getChannel()).getInterChannelInterface().sendLeaveParty(p, currentParty.getId());
				else
					gc.getSession().send(GamePackets.writeSimplePartyListMessage(NOT_IN_PARTY));
				break;
			}
			case JOIN: {
				int partyId = packet.readInt();
				if (currentParty == null)
					GameServer.getChannel(gc.getChannel()).getInterChannelInterface().sendJoinParty(p, partyId);
				else
					gc.getSession().send(GamePackets.writeSimplePartyListMessage(ALREADY_IN_PARTY));
				break;
			}
			case INVITE: {
				//invites only check players on current channel
				//TODO: writeSimplePartyMessage(BUSY) if other invited has
				//another pending party invitation
				String name = packet.readLengthPrefixedString();
				GameCharacter invited = GameServer.getChannel(gc.getChannel()).getPlayerByName(name);
				if (currentParty != null)
					if (!currentParty.isFull())
						if (invited != null)
							if (invited.getParty() == null)
								invited.getClient().getSession().send(writePartyInvite(currentParty.getId(), p.getName()));
							else
								gc.getSession().send(GamePackets.writeSimplePartyListMessage(ALREADY_IN_PARTY));
						else
							gc.getSession().send(GamePackets.writeSimplePartyListMessage(CANNOT_FIND));
					else
						gc.getSession().send(GamePackets.writeSimplePartyListMessage(PARTY_FULL));
				else
					gc.getSession().send(GamePackets.writeSimplePartyListMessage(NOT_IN_PARTY));
				break;
			}
			case EXPEL: {
				int expelled = packet.readInt();
				if (currentParty != null && currentParty.getLeader() == p.getId()) {
					currentParty.lockRead();
					try {
						GameServer.getChannel(gc.getChannel()).getInterChannelInterface().sendExpelPartyMember(currentParty.getMember(expelled), currentParty.getId());
					} finally {
						currentParty.unlockRead();
					}
				} else {
					gc.getSession().send(GamePackets.writeSimplePartyListMessage(NOT_IN_PARTY));
				}
				break;
			}
			case CHANGE_LEADER: {
				int newLeader = packet.readInt();
				if (currentParty != null && currentParty.getLeader() == p.getId())
					GameServer.getChannel(gc.getChannel()).getInterChannelInterface().sendChangePartyLeader(currentParty.getId(), newLeader);	
				else
					gc.getSession().send(GamePackets.writeSimplePartyListMessage(NOT_IN_PARTY));
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
			inviter.getClient().getSession().send(writePartyInviteRejected(to));
	}

	private static byte[] writePartyInviteRejected(String name) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(5 + name.length());

		lew.writeShort(ClientSendOps.PARTY_LIST);
		lew.writeByte(INVITE_DENIED);
		lew.writeLengthPrefixedString(name);

		return lew.getBytes();
	}

	private static byte[] writePartyInvite(int partyId, String inviter) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(10 + inviter.length());

		lew.writeShort(ClientSendOps.PARTY_LIST);
		lew.writeByte(INVITE_SENT);
		lew.writeInt(partyId);
		lew.writeLengthPrefixedString(inviter);
		lew.writeByte((byte) 0);

		return lew.getBytes();
	}
}
