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

package argonms.shop.loading.commodityoverride;

/**
 *
 * @author GoldenKevin
 */
public enum CommodityMod {
	//3A B4 C4 04 FF FF 6F D1 10 00 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 FF 00 FF 00 00 00 00 00 00 00 00
	//3B B4 C4 04 FF FF 71 D1 10 00 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 FF 00 FF 00 00 00 00 00 00 00 00
	//3C B4 C4 04 FF FF 68 4D 0F 00 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 FF 00 FF 00 00 00 00 00 00 00 00
	//3D B4 C4 04 FF FF 5A DE 13 00 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 FF 00 FF 00 00 00 00 00 00 00 00
	//3E B4 C4 04 FF FF 86 86 3D 00 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 FF 00 FF 00 00 00 00 00 00 00 00
	//3F B4 C4 04 FF FF 27 DC 1E 00 05 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 FF 00 FF 00 00 00 00 00 00 00 00
	//40 B4 C4 04 FF FF 28 DC 1E 00 05 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 FF 00 FF 00 00 00 00 00 00 00 00
	//41 B4 C4 04 FF FF 29 DC 1E 00 05 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 FF 00 FF 00 00 00 00 00 00 00 00

	ITEM_ID		(0x0001),
	COUNT		(0x0002),
	PRIORITY	(0x0008),
	SALE_PRICE	(0x0004),
	ON_SALE		(0x0200),
	CLASS		(0x0400); //Plain = -1, New = 0, Sale = 1, Hot = 2, Event = 3

	private final short value;

	private CommodityMod(int value) {
		this.value = (short) value;
	}

	public short shortValue() {
		return value;
	}
}
