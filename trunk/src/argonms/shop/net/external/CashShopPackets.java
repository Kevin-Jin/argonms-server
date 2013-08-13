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

package argonms.shop.net.external;

import argonms.common.character.inventory.Inventory;
import argonms.common.character.inventory.InventorySlot;
import argonms.common.net.external.ClientSendOps;
import argonms.common.net.external.CommonPackets;
import argonms.common.net.external.PacketSubHeaders;
import argonms.common.util.collections.Pair;
import argonms.common.util.output.LittleEndianByteArrayWriter;
import argonms.common.util.output.LittleEndianWriter;
import argonms.shop.character.CashShopStaging;
import argonms.shop.character.ShopCharacter;
import java.util.Collection;
import java.util.List;

/**
 *
 * @author GoldenKevin
 */
public class CashShopPackets {
	private static byte //sub opcodes for ClientSendOps.CASH_SHOP
		INVENTORY = 0x2F,
		GIFTS = 0x31,
		DISPLAY_WISH_LIST = 0x33,
		UPDATE_WISH_LIST = 0x39,
		WISH_LIST_ERROR = 0x3A,
		INSERT_TO_STAGING = 0x3B,
		BUY_ERROR = 0x3C,
		REDEEM_COUPON = 0x3D,
		COUPON_ERROR = 0x40,
		UPDATE_INVENTORY_SLOTS = 0x44,
		INVENTORY_SLOTS_ERROR = 0x45,
		UPDATE_STORAGE_SLOTS = 0x46,
		STORAGE_SLOTS_ERROR = 0x47,
		UPDATE_CHARACTER_SLOTS = 0x48,
		CHARACTER_SLOTS_ERROR = 0x49,
		MOVE_FROM_STAGING = 0x4A,
		MOVE_FROM_STAGING_ERROR = 0x4B,
		MOVE_TO_STAGING = 0x4C,
		MOVE_TO_STAGING_ERROR = 0x4D,
		EXPIRE_ITEM = 0x4E,
		SEND_GIFT = 0x6B,
		GIFT_ERROR = 0x6C,
		BUY_MESO_ITEM = 0x6D
	;

	public static byte
		ERROR_UNKNOWN = 0x00,
		ERROR_UNKNOWN_THEN_EXIT_TO_CHANNEL_1 = (byte) 0x7F,
		ERROR_REQUEST_TIMED_OUT = (byte) 0x80, //returns to channel
		ERROR_UNKNOWN_WARP_TO_CHANNEL_2 = (byte) 0x81,
		ERROR_INSUFFICIENT_CASH = (byte) 0x82,
		ERROR_AGE_LIMIT = (byte) 0x83,
		ERROR_EXCEEDED_ALLOTTED_LIMIT_OF_PRICE = (byte) 0x84,
		ERROR_TOO_MANY_CASH_ITEMS = (byte) 0x85,
		ERROR_GIFT_ITEM_RECEIVER_GENDER = (byte) 0x86,
		ERROR_COUPON_NUMBER = (byte) 0x87,
		ERROR_COUPON_EXPIRED = (byte) 0x88,
		ERROR_COUPON_USED = (byte) 0x89,
		ERROR_NEXON_CAFE_ONLY_COUPON = (byte) 0x8A,
		ERROR_USED_NEXON_CAFE_ONLY_COUPON = (byte) 0x8B,
		ERROR_EXPIRED_NEXON_CAFE_ONLY_COUPON = (byte) 0x8C,
		ERROR_IS_COUPON_NUMBER = (byte) 0x8D,
		ERROR_GENDER_RESTRICTIONS = (byte) 0x8E,
		ERROR_REGULAR_ITEM_ONLY_COUPON = (byte) 0x8F,
		ERROR_MAPLESTORY_ONLY_COUPON_NO_GIFTS = (byte) 0x90,
		ERROR_INVENTORY_FULL = (byte) 0x91,
		ERROR_PREMIUM_SERVICE = (byte) 0x92,
		ERROR_INVALID_RECIPIENT = (byte) 0x93,
		ERROR_RECIPIENT_NAME = (byte) 0x94,
		ERROR_OUT_OF_STOCK = (byte) 0x96,
		ERROR_INSUFFICIENT_MESOS = (byte) 0x98,
		ERROR_CASH_SHOP_IN_BETA = (byte) 0x99,
		ERROR_BIRTHDAY = (byte) 0x9A,
		ERROR_ONLY_AVAILABLE_FOR_GIFTS = (byte) 0x9D,
		ERROR_ALREADY_APPLIED = (byte) 0x9E,
		ERROR_DAILY_PURCHASE_LIMIT = (byte) 0xA3,
		ERROR_MAXIMUM_USAGE = (byte) 0xA6,
		ERROR_COUPON_SYSTEM_COMING_SOON = (byte) 0xA7,
		ERROR_ITEM_ONLY_USABLE_15_DAYS_AFTER_REGGING = (byte) 0xA8,
		ERROR_INSUFFICIENT_GIFT_TOKENS = (byte) 0xA9,
		ERROR_SEND_TECHNICAL_DIFFICULTIES = (byte) 0xAA,
		ERROR_SEND_LESS_THAN_2_WEEKS_SINCE_FIRST_CHARGE = (byte) 0xAB,
		ERROR_CANNOT_GIVE_WITH_BAN_HISTORY = (byte) 0xAC,
		ERROR_GIFT_LIMITATION = (byte) 0xAD,
		ERROR_TOO_LATE_TO_GIVE = (byte) 0xAE,
		ERROR_GIFT_TECHNICAL_DIFFICULTIES = (byte) 0xAF,
		ERROR_TRANSFER_TO_WORLD_UNDER_20 = (byte) 0xB0,
		ERROR_TRANSFER_TO_SAME_WORLD = (byte) 0xB1,
		ERROR_TRANSFER_TO_YOUNG_WORLD = (byte) 0xB2,
		ERROR_WORLD_CHARACTER_SLOTS_FULL = (byte) 0xB3,
		ERROR_EVENT_NOT_AVAILABLE_OR_EXPIRED = (byte) 0xCE
	;

	/*public static byte[] writeEnableCsOrMts() {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(7);

		lew.writeShort((short) 0x12);
		lew.writeInt(0);
		lew.writeByte((byte) 0);

		return lew.getBytes();
	}*/

	public static byte[] writeNewsTickerMessage(String message) {
		return CommonPackets.writeServerMessage((byte) 4, message, (byte) -1, true);
	}

	public static byte[] writeCashShopCurrencyBalance(ShopCharacter p) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(14);
		lew.writeShort(ClientSendOps.CS_BALANCE);
		lew.writeInt(p.getCashShopCurrency(1)); // Paypal/PayByCash NX
		lew.writeInt(p.getCashShopCurrency(2)); // Maple Points
		lew.writeInt(p.getCashShopCurrency(4)); // Game Card NX
		return lew.getBytes();
	}

	private static void writeStagingSlot(LittleEndianWriter lew, CashShopStaging.CashPurchaseProperties props, InventorySlot item) {
		lew.writeLong(item.getUniqueId());
		lew.writeInt(props.getPurchaserAccountId());
		lew.writeInt(0);
		lew.writeInt(item.getDataId());
		lew.writeInt(props.getSerialNumber());
		lew.writeShort(item.getQuantity());
		lew.writePaddedAsciiString(props.getGifterCharacterName(), 13);
		CommonPackets.writeItemExpire(lew, item.getExpiration(), true);
		lew.writeInt(props.getSerialNumber());
		lew.writeInt(0);
	}

	public static byte[] writeCashItemStagingInventory(ShopCharacter p) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter();
		lew.writeShort(ClientSendOps.CASH_SHOP);
		lew.writeByte(INVENTORY);
		CashShopStaging inv = p.getCashShopInventory();
		inv.lockRead();
		try {
			Collection<InventorySlot> items = inv.getAllValues();
			lew.writeShort((short) items.size());
			for (InventorySlot item : items)
				writeStagingSlot(lew, inv.getPurchaseProperties(item.getUniqueId()), item);
		} finally {
			inv.unlockRead();
		}
		lew.writeShort(p.getInventory(Inventory.InventoryType.CASH).getMaxSlots());
		lew.writeShort(p.getMaxCharacters());
		return lew.getBytes();
	}

	public static byte[] writeGiftedCashItems(ShopCharacter p) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter();
		lew.writeShort(ClientSendOps.CASH_SHOP);
		lew.writeByte(GIFTS);
		CashShopStaging inv = p.getCashShopInventory();
		inv.lockRead();
		try {
			Collection<CashShopStaging.CashItemGiftNotification> gifts = inv.getGiftedItems();
			lew.writeShort((short) gifts.size());
			for (CashShopStaging.CashItemGiftNotification gift : gifts) {
				lew.writeLong(gift.getUniqueId());
				lew.writeInt(gift.getItemId());
				lew.writePaddedAsciiString(gift.getSender(), 13);
				lew.writePaddedAsciiString(gift.getMessage(), 73);
			}
		} finally {
			inv.unlockRead();
		}
		return lew.getBytes();
	}

	private static void writeCashItemWishList(LittleEndianWriter lew, ShopCharacter p) {
		int remaining = 40;
		for (Integer sn : p.getWishListSerialNumbers()) {
			lew.writeInt(sn.intValue());
			remaining -= 4;
		}
		lew.writeBytes(new byte[remaining]);
	}

	public static byte[] writePopulateWishList(ShopCharacter p) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(43);
		lew.writeShort(ClientSendOps.CASH_SHOP);
		lew.writeByte(DISPLAY_WISH_LIST);
		writeCashItemWishList(lew, p);
		return lew.getBytes();
	}

	public static byte[] writeChangeWishList(ShopCharacter p) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(43);
		lew.writeShort(ClientSendOps.CASH_SHOP);
		lew.writeByte(UPDATE_WISH_LIST);
		writeCashItemWishList(lew, p);
		return lew.getBytes();
	}

	private static byte[] writeSimpleError(byte header, byte message) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(message != ERROR_OUT_OF_STOCK ? 4 : 8);
		lew.writeShort(ClientSendOps.CASH_SHOP);
		lew.writeByte(header);
		lew.writeByte(message);
		if (message == ERROR_OUT_OF_STOCK)
			lew.writeInt(0);
		return lew.getBytes();
	}

	public static byte[] writeWishListError(byte message) {
		return writeSimpleError(WISH_LIST_ERROR, message);
	}

	public static byte[] writeInsertToStaging(CashShopStaging.CashPurchaseProperties props, InventorySlot item) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(58);
		lew.writeShort(ClientSendOps.CASH_SHOP);
		lew.writeByte(INSERT_TO_STAGING);
		writeStagingSlot(lew, props, item);
		return lew.getBytes();
	}

	public static byte[] writeBuyError(byte message) {
		return writeSimpleError(BUY_ERROR, message);
	}

	public static byte[] writeCouponRewards(List<Pair<InventorySlot, CashShopStaging.CashPurchaseProperties>> itemRewards, int maplePointsReward, int mesosReward) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(16 + itemRewards.size() * 55);
		lew.writeShort(ClientSendOps.CASH_SHOP);
		lew.writeByte(REDEEM_COUPON);
		lew.writeByte((byte) itemRewards.size());
		for (Pair<InventorySlot, CashShopStaging.CashPurchaseProperties> item : itemRewards)
			writeStagingSlot(lew, item.right, item.left);
		lew.writeInt(maplePointsReward);
		lew.writeInt(0);
		lew.writeInt(mesosReward);
		return lew.getBytes();
	}

	public static byte[] writeCouponError(byte message) {
		return writeSimpleError(COUPON_ERROR, message);
	}

	public static byte[] writeUpdateInventorySlots(Inventory.InventoryType invType, short newCapacity) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(6);
		lew.writeShort(ClientSendOps.CASH_SHOP);
		lew.writeByte(UPDATE_INVENTORY_SLOTS);
		lew.writeByte(invType.byteValue());
		lew.writeShort(newCapacity);
		return lew.getBytes();
	}

	public static byte[] writeBuyInventorySlotsError(byte message) {
		return writeSimpleError(INVENTORY_SLOTS_ERROR, message);
	}

	public static byte[] writeUpdateStorageSlots(short newCapacity) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(5);
		lew.writeShort(ClientSendOps.CASH_SHOP);
		lew.writeByte(UPDATE_STORAGE_SLOTS);
		lew.writeShort(newCapacity);
		return lew.getBytes();
	}

	public static byte[] writeBuyStorageSlotsError(byte message) {
		return writeSimpleError(STORAGE_SLOTS_ERROR, message);
	}

	public static byte[] writeUpdateCharacterSlots(short newCapacity) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(5);
		lew.writeShort(ClientSendOps.CASH_SHOP);
		lew.writeByte(UPDATE_CHARACTER_SLOTS);
		lew.writeShort(newCapacity);
		return lew.getBytes();
	}

	public static byte[] writeBuyCharacterSlotsError(byte message) {
		return writeSimpleError(CHARACTER_SLOTS_ERROR, message);
	}

	public static byte[] writeMoveFromStaging(InventorySlot item, short destPos) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter();
		lew.writeShort(ClientSendOps.CASH_SHOP);
		lew.writeByte(MOVE_FROM_STAGING);
		lew.writeShort(destPos);
		CommonPackets.writeItemInfo(lew, item, true, true);
		return lew.getBytes();
	}

	public static byte[] writeTakeError(byte message) {
		return writeSimpleError(MOVE_FROM_STAGING_ERROR, message);
	}

	public static byte[] writeMoveToStaging(CashShopStaging.CashPurchaseProperties props, InventorySlot item) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(58);
		lew.writeShort(ClientSendOps.CASH_SHOP);
		lew.writeByte(MOVE_TO_STAGING);
		writeStagingSlot(lew, props, item);
		return lew.getBytes();
	}

	public static byte[] writePlaceError(byte message) {
		return writeSimpleError(MOVE_TO_STAGING_ERROR, message);
	}

	public static byte[] writeGiftSent(String recipient, int itemId, int price) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(17 + recipient.length());
		lew.writeShort(ClientSendOps.CASH_SHOP);
		lew.writeByte(SEND_GIFT);
		lew.writeLengthPrefixedString(recipient);
		lew.writeInt(itemId);
		lew.writeShort((short) 0);
		lew.writeShort((short) 0);
		lew.writeInt(price);
		return lew.getBytes();
	}

	public static byte[] writeGiftError(byte message) {
		return writeSimpleError(GIFT_ERROR, message);
	}

	public static byte[] writeBuyMesoItem(short quantity, short pos, int itemId) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(15);
		lew.writeShort(ClientSendOps.CASH_SHOP);
		lew.writeByte(BUY_MESO_ITEM);
		lew.writeInt(1); //amount of items to change
		lew.writeShort(quantity);
		lew.writeShort(pos);
		lew.writeInt(itemId);
		return lew.getBytes();
	}
}
