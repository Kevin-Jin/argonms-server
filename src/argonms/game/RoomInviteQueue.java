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

package argonms.game;

import argonms.game.character.Chatroom;
import argonms.game.character.GameCharacter;
import argonms.game.field.MapEntity;
import argonms.game.field.entity.Miniroom;
import argonms.game.field.entity.Trade;
import argonms.game.net.external.GamePackets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 * @author GoldenKevin
 */
public final class RoomInviteQueue {
	private static final RoomInviteQueue INSTANCE = new RoomInviteQueue();

	public static RoomInviteQueue getInstance() {
		return INSTANCE;
	}

	private final Map<Integer, Integer> queuedTrades;
	private final Map<Integer, String> queuedChats;

	private RoomInviteQueue() {
		queuedTrades = new ConcurrentHashMap<Integer, Integer>();
		queuedChats = new ConcurrentHashMap<Integer, String>();
	}

	public void queueTradeInvite(GameCharacter sender, int recipient) {
		queuedTrades.put(Integer.valueOf(sender.getId()), Integer.valueOf(recipient));
	}

	public void inviteToTrade(GameCharacter sender, int recipient, Trade room) {
		GameCharacter invitee = (GameCharacter) sender.getMap().getEntityById(MapEntity.EntityType.PLAYER, recipient);
		if (invitee == null) {
			sender.setMiniRoom(null);
			sender.getClient().getSession().send(GamePackets.writeInviteFail(Miniroom.INVITE_ERROR_NOT_FOUND, null));
			sender.getMap().destroyEntity(room);
		} else if (invitee.getMiniRoom() != null) {
			sender.setMiniRoom(null);
			sender.getClient().getSession().send(GamePackets.writeInviteFail(Miniroom.INVITE_ERROR_BUSY, invitee.getName()));
			sender.getMap().destroyEntity(room);
		} else {
			invitee.getClient().getSession().send(GamePackets.writeTradeInvite(sender.getName(), room.getId()));
			sender.getClient().getSession().send(room.getFirstPersonJoinMessage(sender));
		}
	}

	public void processQueuedTradeInvites(GameCharacter sender, Trade room) {
		Integer recipient = queuedTrades.remove(Integer.valueOf(sender.getId()));
		if (recipient != null)
			inviteToTrade(sender, recipient.intValue(), room);
	}

	public void cancelAll(GameCharacter p) {
		Integer oId = Integer.valueOf(p.getId());
		queuedTrades.remove(oId);
		queuedChats.remove(oId);
	}

	public void queueChatInvite(GameCharacter sender, String recipient) {
		queuedChats.put(Integer.valueOf(sender.getId()), recipient);
	}

	public void inviteToChat(GameCharacter sender, String recipient, Chatroom room) {
		boolean result = GameServer.getChannel(sender.getClient().getChannel()).getCrossServerInterface().sendChatroomInvite(sender.getName(), room.getRoomId(), recipient);
		sender.getClient().getSession().send(GamePackets.writeChatroomInviteResponse(Chatroom.ACT_INVITE_RESPONSE, recipient, result));
	}

	public void processQueuedChatInvites(GameCharacter sender, Chatroom room) {
		String recipient = queuedChats.remove(Integer.valueOf(sender.getId()));
		if (recipient != null)
			inviteToChat(sender, recipient, room);
	}
}
