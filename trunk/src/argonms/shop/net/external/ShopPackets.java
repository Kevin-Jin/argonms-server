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

import argonms.common.net.external.ClientSendOps;
import argonms.common.net.external.CommonPackets;
import argonms.common.util.output.LittleEndianByteArrayWriter;
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
		UPDATE_WISH_LIST = 0x39
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

	public static byte[] writeCashItemWishList(ShopCharacter p, boolean update) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(43);
		lew.writeShort(ClientSendOps.CASH_SHOP);
		if (update)
			lew.writeByte(UPDATE_WISH_LIST);
		else
			lew.writeByte(DISPLAY_WISH_LIST);
		int remaining = 40;
		for (Integer sn : p.getWishListSerialNumbers()) {
			lew.writeInt(sn.intValue());
			remaining -= 4;
		}
		lew.writeBytes(new byte[remaining]);
		return lew.getBytes();
	}
}
