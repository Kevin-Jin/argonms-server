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

package argonms.game.net.external.handler;

import argonms.common.net.external.CheatTracker;
import argonms.common.util.input.LittleEndianReader;
import argonms.game.GameServer;
import argonms.game.RoomInviteQueue;
import argonms.game.character.Chatroom;
import argonms.game.character.GameCharacter;
import argonms.game.net.external.GameClient;

/**
 *
 * @author GoldenKevin
 */
public class MessengerHandler {
	public static void handleAction(LittleEndianReader packet, GameClient gc) {
		GameCharacter p = gc.getPlayer();
		Chatroom room = p.getChatRoom();
		switch (packet.readByte()) {
			case Chatroom.ACT_OPEN: {
				if (room != null) {
					CheatTracker.get(gc).suspicious(CheatTracker.Infraction.POSSIBLE_PACKET_EDITING, "Tried to open new chatroom while one is open.");
					return;
				}
				int roomId = packet.readInt();
				if (roomId == 0) //create
					GameServer.getChannel(gc.getChannel()).getCrossServerInterface().sendMakeChatroom(p);
				else //join
					GameServer.getChannel(gc.getChannel()).getCrossServerInterface().sendJoinChatroom(p, roomId);
				break;
			}
			case Chatroom.ACT_EXIT:
				if (room == null) {
					CheatTracker.get(gc).suspicious(CheatTracker.Infraction.POSSIBLE_PACKET_EDITING, "Tried to leave null chatroom.");
					return;
				}
				GameServer.getChannel(gc.getChannel()).getCrossServerInterface().sendLeaveChatroom(p);
				break;
			case Chatroom.ACT_INVITE: {
				String recipient = packet.readLengthPrefixedString();
				if (room == null) {
					RoomInviteQueue.getInstance().queueChatInvite(p, recipient);
					return;
				}

				room.lockRead();
				try {
					if (room.isFull())
						//TODO: send response that room is full
						return;
				} finally {
					room.unlockRead();
				}

				RoomInviteQueue.getInstance().inviteToChat(p, recipient, room);
				break;
			}
			case Chatroom.ACT_DECLINE: {
				String recipient = packet.readLengthPrefixedString();
				GameServer.getChannel(gc.getChannel()).getCrossServerInterface().sendChatroomDecline(p.getName(), recipient);
				break;
			}
			case Chatroom.ACT_CHAT:
				if (room == null) {
					CheatTracker.get(gc).suspicious(CheatTracker.Infraction.POSSIBLE_PACKET_EDITING, "Tried to send text to null chatroom.");
					return;
				}
				String text = packet.readLengthPrefixedString();
				GameServer.getChannel(gc.getChannel()).getCrossServerInterface().sendChatroomText(text, room, p.getId());
				break;
		}
	}
}
