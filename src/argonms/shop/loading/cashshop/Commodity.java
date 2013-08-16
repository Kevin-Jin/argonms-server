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

package argonms.shop.loading.cashshop;

/**
 *
 * @author GoldenKevin
 */
public class Commodity {
	public int itemDataId;
	public short quantity;
	public int price;
	public byte period;
	public byte gender;
	public boolean onSale;

	public Commodity(int itemDataId, short quantity, int price, byte period, byte gender, boolean onSale) {
		this.itemDataId = itemDataId;
		this.quantity = quantity;
		this.price = price;
		this.period = period;
		this.gender = gender;
		this.onSale = onSale;
	}
}
