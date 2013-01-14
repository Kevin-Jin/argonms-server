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

import argonms.common.character.inventory.Inventory;
import argonms.common.character.inventory.Inventory.InventoryType;
import argonms.common.character.inventory.InventorySlot;
import argonms.common.net.external.CheatTracker;
import argonms.common.net.external.ClientSendOps;
import argonms.common.util.input.LittleEndianReader;
import argonms.common.util.output.LittleEndianByteArrayWriter;
import argonms.game.character.GameCharacter;
import argonms.game.field.MapEntity.EntityType;
import argonms.game.field.entity.FreeMarketShop.HiredMerchant;
import argonms.game.field.entity.FreeMarketShop.PlayerStore;
import argonms.game.field.entity.Minigame;
import argonms.game.field.entity.Minigame.MatchCards;
import argonms.game.field.entity.Minigame.MinigameResult;
import argonms.game.field.entity.Minigame.Omok;
import argonms.game.field.entity.Miniroom;
import argonms.game.field.entity.Miniroom.MiniroomType;
import argonms.game.field.entity.Trade;
import argonms.game.net.external.GameClient;
import argonms.game.net.external.GamePackets;

/**
 *
 * @author GoldenKevin
 */
public final class MiniroomHandler {
	public static void handleAction(LittleEndianReader packet, GameClient gc) {
		GameCharacter p = gc.getPlayer();
		switch (packet.readByte()) {
			case Miniroom.ACT_CREATE:
				createRoom(p, packet);
				break;
			case Miniroom.ACT_INVITE:
				inviteToRoom(p, packet);
				break;
			case Miniroom.ACT_DECLINE:
				declineInvite(p, packet);
				break;
			case Miniroom.ACT_VISIT:
				joinRoom(p, packet);
				break;
			case Miniroom.ACT_CHAT:
				chat(p, packet);
				break;
			case Miniroom.ACT_EXIT:
				leaveRoom(p);
				break;
			case Miniroom.ACT_OPEN:
				break;
			case Miniroom.ACT_SET_ITEMS:
				tradeSetItems(p, packet);
				break;
			case Miniroom.ACT_SET_MESO:
				tradeSetMesos(p, packet);
				break;
			case Miniroom.ACT_CONFIRM:
				tradeConfirm(p);
				break;
			case Miniroom.ACT_READY:
				gameReady(p);
				break;
			case Miniroom.ACT_UN_READY:
				gameUnready(p);
				break;
			case Miniroom.ACT_EXPEL:
				gameExpelVisitor(p);
				break;
			case Miniroom.ACT_START:
				gameStart(p);
				break;
			case Miniroom.ACT_GIVE_UP:
				gameForfeit(p);
				break;
			case Miniroom.ACT_REQUEST_REDO:
				gameAskRedo(p);
				break;
			case Miniroom.ACT_ANSWER_REDO:
				gameDoRedo(p, packet);
				break;
			case Miniroom.ACT_EXIT_AFTER_GAME:
				gameExitAfterFinish(p, true);
				break;
			case Miniroom.ACT_CANCEL_EXIT:
				gameExitAfterFinish(p, false);
				break;
			case Miniroom.ACT_REQUEST_TIE:
				gameAskTie(p);
				break;
			case Miniroom.ACT_ANSWER_TIE:
				gameDoTie(p, packet);
				break;
			case Miniroom.ACT_SKIP:
				gameSkipByTimer(p);
				break;
			case Miniroom.ACT_MOVE_OMOK:
				omokSelect(p, packet);
				break;
			case Miniroom.ACT_SELECT_CARD:
				matchCardSelect(p, packet);
				break;
			case Miniroom.ACT_BAN_PLAYER:
				break;
			case Miniroom.ACT_ADD_ITEM: case Miniroom.ACT_PUT_ITEM:
				break;
			case Miniroom.ACT_BUY: case Miniroom.ACT_MERCHANT_BUY:
				break;
			case Miniroom.ACT_REMOVE_ITEM:
				break;
			case Miniroom.ACT_TAKE_ITEM_BACK:
				break;
			case Miniroom.ACT_CLOSE_MERCHANT:
				killMerchant(p);
				break;
			case Miniroom.ACT_MAINTENANCE_OFF:
				break;
			case Miniroom.ACT_SPAWN_SHOP:
				break;
		}
	}

	private static void createRoom(GameCharacter p, LittleEndianReader packet) {
		String text, pwd;
		Miniroom room = null;
		switch (MiniroomType.valueOf(packet.readByte())) {
			case OMOK: {
				text = packet.readLengthPrefixedString();
				if (packet.readBool()) //private room
					pwd = packet.readLengthPrefixedString();
				else
					pwd = null;
				byte subType = packet.readByte();
				room = new Omok(p, text, pwd, subType);
				p.getClient().getSession().send(room.getFirstPersonJoinMessage(p));
				break;
			}
			case MATCH_CARDS: {
				text = packet.readLengthPrefixedString();
				if (packet.readBool()) //private room
					pwd = packet.readLengthPrefixedString();
				else
					pwd = null;
				byte subType = packet.readByte();
				room = new MatchCards(p, text, pwd, subType);
				p.getClient().getSession().send(room.getFirstPersonJoinMessage(p));
				break;
			}
			case TRADE: {
				room = new Trade(p);
				//p.getClient().getSession().send(room.getFirstPersonJoinMessage(p));
				break;
			}
			case PLAYER_SHOP: {
				text = packet.readLengthPrefixedString();
				if (packet.readBool()) //private room
					pwd = packet.readLengthPrefixedString();
				else
					pwd = null;
				short slot = packet.readShort();
				int itemId = packet.readInt();
				if (p.getInventory(InventoryType.CASH).getItemSlots(itemId).contains(Short.valueOf(slot))) {
					room = new PlayerStore(p, text, itemId);
				} else {
					CheatTracker.get(p.getClient()).suspicious(CheatTracker.Infraction.PACKET_EDITING, "Tried to open player shop without permit");
					return;
				}
				p.getClient().getSession().send(room.getFirstPersonJoinMessage(p));
				break;
			}
			case HIRED_MERCHANT: {
				text = packet.readLengthPrefixedString();
				if (packet.readBool()) //private room
					pwd = packet.readLengthPrefixedString();
				else
					pwd = null;
				short slot = packet.readShort();
				int itemId = packet.readInt();
				if (p.getInventory(InventoryType.CASH).getItemSlots(itemId).contains(Short.valueOf(slot))) {
					room = new HiredMerchant(p, text, itemId);
				} else {
					CheatTracker.get(p.getClient()).suspicious(CheatTracker.Infraction.PACKET_EDITING, "Tried to open hired merchant without permit");
					return;
				}
				//p.getClient().getSession().send(room.getFirstPersonJoinMessage(p));
				break;
			}
		}
		p.setMiniRoom(room);
		p.getMap().spawnEntity(room);
	}

	private static void inviteToRoom(GameCharacter p, LittleEndianReader packet) {
		int pId = packet.readInt();
		GameCharacter invitee = (GameCharacter) p.getMap().getEntityById(EntityType.PLAYER, pId);
		Trade room = (Trade) p.getMiniRoom();
		if (invitee == null) {
			p.setMiniRoom(null);
			p.getClient().getSession().send(writeInviteFail(Miniroom.INVITE_ERROR_NOT_FOUND, null));
			p.getMap().destroyEntity(room);
		} else if (invitee.getMiniRoom() != null) {
			p.setMiniRoom(null);
			p.getClient().getSession().send(writeInviteFail(Miniroom.INVITE_ERROR_BUSY, invitee.getName()));
			p.getMap().destroyEntity(room);
		} else {
			invitee.getClient().getSession().send(writeTradeInvite(p.getName(), room.getId()));
			p.getClient().getSession().send(room.getFirstPersonJoinMessage(p));
		}
	}

	private static void declineInvite(GameCharacter p, LittleEndianReader packet) {
		int entId = packet.readInt();
		byte message = packet.readByte();
		Trade room = (Trade) p.getMap().getEntityById(EntityType.MINI_ROOM, entId);
		if (room != null) {
			GameCharacter owner = room.getPlayerByPosition((byte) 0);
			owner.getClient().getSession().send(writeInviteFail(message, p.getName()));
			owner.setMiniRoom(null);
			room.leaveRoom(owner);
			owner.getClient().getSession().send(room.getFirstPersonLeaveMessage((byte) 0, Miniroom.EXIT_TRADE_CANCELED));
		}
	}

	private static void joinRoom(GameCharacter p, LittleEndianReader packet) {
		int entId = packet.readInt();
		Miniroom room = (Miniroom) p.getMap().getEntityById(EntityType.MINI_ROOM, entId);
		if (room == null)
			p.getClient().getSession().send(writeJoinError(Miniroom.JOIN_ERROR_ALREADY_CLOSED));
		else if (!p.isAlive())
			p.getClient().getSession().send(writeJoinError(Miniroom.JOIN_ERROR_DEAD));
		else if (room.isPlayerBanned(p))
			p.getClient().getSession().send(writeJoinError(Miniroom.JOIN_ERROR_BANNED));
		else if (packet.available() > 0 && packet.readBool() && !packet.readLengthPrefixedString().equals(room.getPassword()))
			p.getClient().getSession().send(writeJoinError(Miniroom.JOIN_ERROR_INCORRECT_PASSWORD));
		else if (!room.joinRoom(p))
			p.getClient().getSession().send(writeJoinError(Miniroom.JOIN_ERROR_FULL));
	}

	private static void chat(GameCharacter p, LittleEndianReader packet) {
		Miniroom room = p.getMiniRoom();
		room.sendToAll(writeChat(room.positionOf(p), p.getName(), packet.readLengthPrefixedString()));
	}

	private static void leaveRoom(GameCharacter p) {
		Miniroom room = p.getMiniRoom();
		p.setMiniRoom(null);
		room.leaveRoom(p);
	}

	private static void tradeSetItems(GameCharacter p, LittleEndianReader packet) {
		InventoryType type = InventoryType.valueOf(packet.readByte());
		short slot = packet.readShort();
		short quantity = packet.readShort();
		byte tradeSlot = packet.readByte();
		Inventory inv = p.getInventory(type);
		InventorySlot item = inv.get(slot);
		if (item == null || item.getQuantity() < quantity) {
			CheatTracker.get(p.getClient()).suspicious(CheatTracker.Infraction.PACKET_EDITING, "Tried to trade nonexistent item");
			return;
		}
		InventorySlot itemToPut;
		if (item.getQuantity() != quantity) {
			item.setQuantity((short) (item.getQuantity() - quantity));
			itemToPut = item.clone();
			itemToPut.setQuantity(quantity);
			p.getClient().getSession().send(GamePackets.writeInventoryUpdateSlotQuantity(type, slot, item));
		} else {
			itemToPut = inv.remove(slot);
			p.getClient().getSession().send(GamePackets.writeInventoryClearSlot(type, slot));
		}
		Trade room = (Trade) p.getMiniRoom();
		room.addItem(p, tradeSlot, itemToPut);
	}

	private static void tradeSetMesos(GameCharacter p, LittleEndianReader packet) {
		int mesosAmt = packet.readInt();
		if (p.getMesos() >= mesosAmt) {
			p.setMesos(p.getMesos() - mesosAmt);
			Trade room = (Trade) p.getMiniRoom();
			room.addMesos(p, mesosAmt);
		} else {
			CheatTracker.get(p.getClient()).suspicious(CheatTracker.Infraction.PACKET_EDITING, "Tried to trade nonexistent mesos");
		}
	}

	private static void tradeConfirm(GameCharacter p) {
		Trade room = (Trade) p.getMiniRoom();
		room.getPlayerByPosition((byte) (((room.positionOf(p) + 1) % 2))).getClient().getSession().send(writeSimpleMessage(Miniroom.ACT_CONFIRM));
		room.confirmTrade(p);
	}

	private static void gameReady(GameCharacter p) {
		Miniroom room = p.getMiniRoom();
		room.sendToAll(writeSimpleMessage(Miniroom.ACT_READY));
	}

	private static void gameUnready(GameCharacter p) {
		Miniroom room = p.getMiniRoom();
		room.sendToAll(writeSimpleMessage(Miniroom.ACT_UN_READY));
	}

	private static void gameExpelVisitor(GameCharacter p) {
		Minigame room = (Minigame) p.getMiniRoom();
		room.banVisitor();
	}

	private static void gameStart(GameCharacter p) {
		Minigame room = (Minigame) p.getMiniRoom();
		room.startGame();
	}

	private static void gameForfeit(GameCharacter p) {
		Minigame room = (Minigame) p.getMiniRoom();
		room.endGame(MinigameResult.LOSS, (byte) ((room.positionOf(p) + 1) % 2));
	}

	private static void gameAskRedo(GameCharacter p) {
		Minigame room = (Minigame) p.getMiniRoom();
		GameCharacter opponent = (room.positionOf(p) == 0) ? room.getPlayerByPosition((byte) 1) : room.getPlayerByPosition((byte) 0);
		opponent.getClient().getSession().send(writeSimpleMessage(Miniroom.ACT_REQUEST_REDO));
	}

	private static void gameDoRedo(GameCharacter p, LittleEndianReader packet) {
		Omok room = (Omok) p.getMiniRoom();
		boolean response = packet.readBool();
		room.redo(response, room.positionOf(p));
	}

	private static void gameExitAfterFinish(GameCharacter p, boolean shouldExit) {
		Minigame room = (Minigame) p.getMiniRoom();
		room.setExitAfterGame(p, shouldExit);
	}

	private static void gameAskTie(GameCharacter p) {
		Minigame room = (Minigame) p.getMiniRoom();
		GameCharacter opponent = (room.positionOf(p) == 0) ? room.getPlayerByPosition((byte) 1) : room.getPlayerByPosition((byte) 0);
		opponent.getClient().getSession().send(writeSimpleMessage(Miniroom.ACT_REQUEST_TIE));
	}

	private static void gameDoTie(GameCharacter p, LittleEndianReader packet) {
		Minigame room = (Minigame) p.getMiniRoom();
		byte opponentPos = (byte) ((room.positionOf(p) + 1) % 2);
		if (packet.readBool()) //accepted
			room.endGame(MinigameResult.TIE, opponentPos);
		else
			room.getPlayerByPosition(opponentPos).getClient().getSession().send(writeSimpleMessage(Miniroom.ACT_ANSWER_TIE));
	}

	private static void gameSkipByTimer(GameCharacter p) {
		Minigame room = (Minigame) p.getMiniRoom();
		room.sendToAll(writeMinigameSkip(room.nextTurn()));
	}

	private static void omokSelect(GameCharacter p, LittleEndianReader packet) {
		Omok room = (Omok) p.getMiniRoom();
		int x = packet.readInt(); // x point
		int y = packet.readInt(); // y point
		byte playerNum = packet.readByte();
		room.setPiece(p, x, y, playerNum);
	}

	private static void matchCardSelect(GameCharacter p, LittleEndianReader packet) {
		MatchCards room = (MatchCards) p.getMiniRoom();
		byte turn = packet.readByte();
		byte slot = packet.readByte();
		room.selectCard(p, turn, slot);
	}

	private static void killMerchant(GameCharacter p) {
		HiredMerchant room = (HiredMerchant) p.getMiniRoom();
		p.setMiniRoom(null);
		room.closeRoom(p.getMap());
		p.getClient().getSession().send(room.getFirstPersonLeaveMessage((byte) 0, Miniroom.EXIT_CLOSE_HIRED_MERCHANT));
	}

	private static byte[] writeJoinError(byte code) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(5);
		lew.writeShort(ClientSendOps.MINIROOM_ACT);
		lew.writeByte(Miniroom.ACT_JOIN);
		lew.writeByte(MiniroomType.NONE.byteValue());
		lew.writeByte(code);
		return lew.getBytes();
	}

	public static byte[] writeTradeInvite(String inviter, int tradeEntId) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(10 + inviter.length());
		lew.writeShort(ClientSendOps.MINIROOM_ACT);
		lew.writeByte(Miniroom.ACT_INVITE);
		lew.writeByte((byte) 3);
		lew.writeLengthPrefixedString(inviter);
		lew.writeInt(tradeEntId);
		return lew.getBytes();
	}

	private static byte[] writeInviteFail(byte code, String name) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(6 + name.length());
		lew.writeShort(ClientSendOps.MINIROOM_ACT);
		lew.writeByte(Miniroom.ACT_DECLINE);
		lew.writeByte(code);
		lew.writeLengthPrefixedString(name);
		return lew.getBytes();
	}

	private static byte[] writeChat(byte pos, String name, String message) {
		String line = name + " : " + message;

		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(7 + line.length());
		lew.writeShort(ClientSendOps.MINIROOM_ACT);
		lew.writeByte(Miniroom.ACT_CHAT);
		lew.writeByte((byte) 8);
		lew.writeByte(pos);
		lew.writeLengthPrefixedString(line);
		return lew.getBytes();
	}

	private static byte[] writeMinigameSkip(byte currentPos) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(4);
		lew.writeShort(ClientSendOps.MINIROOM_ACT);
		lew.writeByte(Miniroom.ACT_SKIP);
		lew.writeByte(currentPos);
		return lew.getBytes();
	}

	private static byte[] writeSimpleMessage(byte code) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(3);
		lew.writeShort(ClientSendOps.MINIROOM_ACT);
		lew.writeByte(code);
		return lew.getBytes();
	}

	private MiniroomHandler() {
		//uninstantiable...
	}
}
