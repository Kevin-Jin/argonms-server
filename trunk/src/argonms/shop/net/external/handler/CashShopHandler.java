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

package argonms.shop.net.external.handler;

import argonms.common.character.inventory.Inventory;
import argonms.common.character.inventory.InventorySlot;
import argonms.common.net.external.CheatTracker;
import argonms.common.util.collections.Pair;
import argonms.common.util.input.LittleEndianReader;
import argonms.shop.ShopServer;
import argonms.shop.character.CashShopStaging;
import argonms.shop.character.ShopCharacter;
import argonms.shop.loading.cashshop.CashShopDataLoader;
import argonms.shop.loading.cashshop.Commodity;
import argonms.shop.net.external.CashShopPackets;
import argonms.shop.net.external.ShopClient;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author GoldenKevin
 */
public class CashShopHandler {
	private static final Logger LOG = Logger.getLogger(CashShopHandler.class.getName());

	private static final byte //handleAction opcodes
		REDEEM_COUPON = 0,
		BUY_SIMPLE_ITEM = 3,
		GIFT_ITEM = 4,
		UPDATE_WISH_LIST = 5,
		BUY_INVENTORY_SLOTS = 6,
		BUY_CASH_INVENTORY_SLOTS = 7,
		BUY_CHARACTER_SLOTS = 8,
		TAKE_FROM_STAGING = 12,
		PLACE_INTO_STAGING = 13,
		BUY_LOVE_RING = 27,
		BUY_PACKAGE = 28,
		GIFT_PACKAGE = 29,
		BUY_ITEM_WITH_MESOS = 30,
		BUY_FRIENDSHIP_RING = 33
	;

	public static void handleReturnToChannel(LittleEndianReader packet, ShopClient sc) {
		if (packet.available() != 0) {
			CheatTracker.get(sc).suspicious(CheatTracker.Infraction.POSSIBLE_PACKET_EDITING, "Tried to change map in cash shop");
			return;
		}
		
		ShopServer.getInstance().requestChannelChange(sc.getPlayer(), sc.getChannel());
	}

	private static void redeemCoupon(ShopCharacter p, LittleEndianReader packet) {
		
	}

	private static void buySimpleItem(ShopCharacter p, LittleEndianReader packet) {
		packet.readByte();
		int currencyType = packet.readInt();
		int serialNumber = packet.readInt();
		Commodity c = CashShopDataLoader.getInstance().getCommodity(serialNumber);
		if (c == null || !c.onSale/* || ShopServer.getInstance().isBlocked(serialNumber)*/) {
			CheatTracker.get(p.getClient()).suspicious(CheatTracker.Infraction.CERTAIN_PACKET_EDITING, "Tried to buy nonexistent item from cash shop");
			return;
		}

		if (!p.gainCashShopCurrency(currencyType, -c.price)) {
			CheatTracker.get(p.getClient()).suspicious(CheatTracker.Infraction.CERTAIN_PACKET_EDITING, "Tried to buy item from cash shop with nonexistent cash");
			return;
		}

		if (p.getCashShopInventory().isFull()) {
			//or maybe client already prevents us from doing this, in which case POSSIBLE_PACKET_EDITING?
			p.getClient().getSession().send(CashShopPackets.writeCashShopOperationFailure(CashShopPackets.ERROR_INVENTORY_FULL));
			return;
		}
		Pair<InventorySlot, CashShopStaging.CashPurchaseProperties> item = CashShopStaging.createItem(c, serialNumber, p.getClient().getAccountId(), null);
		p.getCashShopInventory().append(item.left, item.right);
		p.getClient().getSession().send(CashShopPackets.writeInsertToStaging(item.right, item.left));
	}

	private static void giftItem(ShopCharacter p, LittleEndianReader packet) {
		int enteredBirthday = packet.readInt();
		int serialNumber = packet.readInt();
		String recipient = packet.readLengthPrefixedString();
		String message = packet.readLengthPrefixedString();
		if (p.getBirthday() != 0 && p.getBirthday() != enteredBirthday) {
			p.getClient().getSession().send(CashShopPackets.writeCashShopOperationFailure(CashShopPackets.ERROR_BIRTHDAY));
			return;
		}

		Commodity c = CashShopDataLoader.getInstance().getCommodity(serialNumber);
		if (c == null || !c.onSale/* || ShopServer.getInstance().isBlocked(serialNumber)*/) {
			CheatTracker.get(p.getClient()).suspicious(CheatTracker.Infraction.CERTAIN_PACKET_EDITING, "Tried to gift nonexistent item from cash shop");
			return;
		}

		if (!p.gainCashShopCurrency(ShopCharacter.GAME_CARD_NX, -c.price)) {
			CheatTracker.get(p.getClient()).suspicious(CheatTracker.Infraction.CERTAIN_PACKET_EDITING, "Tried to gift item from cash shop with nonexistent cash");
			return;
		}

		int recipientAcct = ShopCharacter.getAccountIdFromName(recipient);
		if (!CashShopStaging.giveGift(p.getClient().getAccountId(), p.getName(), recipientAcct, c, serialNumber, message)) {
			p.getClient().getSession().send(CashShopPackets.writeCashShopOperationFailure(CashShopPackets.ERROR_INVENTORY_FULL));
			return;
		}

		p.getClient().getSession().send(CashShopPackets.writeGiftSent(recipient, c.itemDataId, c.price));
	}

	private static void updateWishList(ShopCharacter p, LittleEndianReader packet) {
		
	}

	private static void buyInventorySlots(ShopCharacter p, LittleEndianReader packet) {
		
	}

	private static void buyCashInventorySlots(ShopCharacter p, LittleEndianReader packet) {
		
	}

	private static void buyCharacterSlots(ShopCharacter p, LittleEndianReader packet) {
		
	}

	private static void transferFromStaging(ShopCharacter p, LittleEndianReader packet) {
		long uniqueId = packet.readLong();
		Inventory.InventoryType type = Inventory.InventoryType.valueOf(packet.readByte());
		packet.readByte();
		packet.readByte();
		InventorySlot item = p.getCashShopInventory().getByUniqueId(uniqueId);
		if (item == null) {
			CheatTracker.get(p.getClient()).suspicious(CheatTracker.Infraction.CERTAIN_PACKET_EDITING, "Tried to transfer nonexistent cash shop item from staging");
			p.getClient().getSession().send(CashShopPackets.writeCashShopOperationFailure(CashShopPackets.ERROR_UNKNOWN));
			return;
		}

		Inventory inv = p.getInventory(type);
		List<Short> freeSlots = inv.getFreeSlots(1);
		if (freeSlots.isEmpty()) {
			p.getClient().getSession().send(CashShopPackets.writeCashShopOperationFailure(CashShopPackets.ERROR_INVENTORY_FULL));
			return;
		}

		short slot = freeSlots.get(0).shortValue();
		p.getCashShopInventory().removeByUniqueId(uniqueId);
		inv.put(slot, item);
		p.getClient().getSession().send(CashShopPackets.writeMoveFromStaging(item, slot));
	}

	private static void transferToStaging(ShopCharacter p, LittleEndianReader packet) {
		long uniqueId = packet.readLong();
		byte type = packet.readByte();

		InventorySlot item = null;
		for (Iterator<Map.Entry<Short, InventorySlot>> iter = p.getInventory(Inventory.InventoryType.valueOf(type)).getAll().entrySet().iterator(); item == null && iter.hasNext(); ) {
			Map.Entry<Short, InventorySlot> itemCandidate = iter.next();
			if (itemCandidate.getValue().getUniqueId() == uniqueId) {
				item = itemCandidate.getValue();
				iter.remove();
			}
		}
		if (item == null) {
			CheatTracker.get(p.getClient()).suspicious(CheatTracker.Infraction.CERTAIN_PACKET_EDITING, "Tried to transfer nonexistent cash shop item to staging");
			p.getClient().getSession().send(CashShopPackets.writeCashShopOperationFailure(CashShopPackets.ERROR_UNKNOWN));
			return;
		}

		CashShopStaging inv = p.getCashShopInventory();
		CashShopStaging.CashPurchaseProperties props = CashShopStaging.CashPurchaseProperties.loadFromDatabase(uniqueId, item.getDataId(), p.getClient().getAccountId());
		inv.append(item, props);
		p.getClient().getSession().send(CashShopPackets.writeMoveToStaging(props, item));
	}

	private static void buyLoveRing(ShopCharacter p, LittleEndianReader packet) {
		
	}

	private static void buyPackage(ShopCharacter p, LittleEndianReader packet) {
		
	}

	private static void giftPackage(ShopCharacter p, LittleEndianReader packet) {
		
	}

	private static void buyQuestItem(ShopCharacter p, LittleEndianReader packet) {
		
	}

	private static void buyFriendshipRing(ShopCharacter p, LittleEndianReader packet) {
		
	}

	public static void handleAction(LittleEndianReader packet, ShopClient sc) {
		ShopCharacter p = sc.getPlayer();
		switch (packet.readByte()) {
			case REDEEM_COUPON:
				redeemCoupon(p, packet);
				break;
			case BUY_SIMPLE_ITEM:
				buySimpleItem(p, packet);
				break;
			case GIFT_ITEM:
				giftItem(p, packet);
				break;
			case UPDATE_WISH_LIST:
				updateWishList(p, packet);
				break;
			case BUY_INVENTORY_SLOTS:
				buyInventorySlots(p, packet);
				break;
			case BUY_CASH_INVENTORY_SLOTS:
				buyCashInventorySlots(p, packet);
				break;
			case BUY_CHARACTER_SLOTS:
				buyCharacterSlots(p, packet);
				break;
			case TAKE_FROM_STAGING:
				transferFromStaging(p, packet);
				break;
			case PLACE_INTO_STAGING:
				transferToStaging(p, packet);
				break;
			case BUY_LOVE_RING:
				buyLoveRing(p, packet);
				break;
			case BUY_PACKAGE:
				buyPackage(p, packet);
				break;
			case GIFT_PACKAGE:
				giftPackage(p, packet);
				break;
			case BUY_ITEM_WITH_MESOS:
				buyQuestItem(p, packet);
				break;
			case BUY_FRIENDSHIP_RING:
				buyFriendshipRing(p, packet);
				break;
			default:
				LOG.log(Level.INFO, "Received unhandled cash shop action packet:\n{0}", packet);
				break;
		}
	}
}
