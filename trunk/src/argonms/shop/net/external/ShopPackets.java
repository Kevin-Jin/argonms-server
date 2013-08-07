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

import argonms.common.character.inventory.InventorySlot;
import argonms.common.net.external.ClientSendOps;
import argonms.common.net.external.CommonPackets;
import argonms.common.util.output.LittleEndianByteArrayWriter;
import argonms.common.util.output.LittleEndianWriter;
import argonms.shop.character.CashShopStaging;
import argonms.shop.character.ShopCharacter;

/**
 *
 * @author GoldenKevin
 */
public class ShopPackets {
	public static byte //sub opcodes for ClientSendOps.CASH_SHOP
		INVENTORY = 0x2F,
		GIFTS = 0x31,
		DISPLAY_WISH_LIST = 0x33,
		UPDATE_WISH_LIST = 0x39,
		INSERT_TO_STAGING = 0x3B,
		ERROR = 0x3C,
		MOVE_FROM_STAGING = 0x4A,
		MOVE_TO_STAGING = 0x4C,
		EXPIRE_ITEM = 0x4E,
		SEND_GIFT = 0x6B
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

	/*public static byte[] writeEnableCsUse4() {
		return argonms.common.util.HexTool.getByteArrayFromHexString("9F 00 00 00 00 00 00 00 00 00 00 00 00 00");
	}*/

	public static byte[] writeCashShopCurrencyBalance(ShopCharacter p) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(14);
		lew.writeShort(ClientSendOps.CS_BALANCE);
		lew.writeInt(p.getCashShopCurrency(1)); // Paypal/PayByCash NX
		lew.writeInt(p.getCashShopCurrency(2)); // Maple Points
		lew.writeInt(p.getCashShopCurrency(4)); // Game Card NX
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

	public static void writeStagingSlot(LittleEndianWriter lew, CashShopStaging.CashPurchaseProperties props, InventorySlot item) {
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

	public static byte[] writeInsertToStaging(CashShopStaging.CashPurchaseProperties props, InventorySlot item) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(58);
		lew.writeShort(ClientSendOps.CASH_SHOP);
		lew.writeByte(INSERT_TO_STAGING);
		writeStagingSlot(lew, props, item);
		return lew.getBytes();
	}

	public static byte[] writeCashShopOperationFailure(byte message) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(4);
		lew.writeShort(ClientSendOps.CASH_SHOP);
		lew.writeByte(ERROR);
		lew.writeByte(message);
		return lew.getBytes();
	}

	public static byte[] writeMoveFromStaging(InventorySlot item, short destPos) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter();
		lew.writeShort(ClientSendOps.CASH_SHOP);
		lew.writeByte(MOVE_FROM_STAGING);
		lew.writeShort(destPos);
		CommonPackets.writeItemInfo(lew, item, true, true);
		return lew.getBytes();
	}

	public static byte[] writeMoveToStaging(CashShopStaging.CashPurchaseProperties props, InventorySlot item) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(58);
		lew.writeShort(ClientSendOps.CASH_SHOP);
		lew.writeByte(MOVE_TO_STAGING);
		writeStagingSlot(lew, props, item);
		return lew.getBytes();
	}
}
