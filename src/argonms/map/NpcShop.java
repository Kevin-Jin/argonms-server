/*
 * ArgonMS MapleStory server emulator written in Java
 * Copyright (C) 2011  GoldenKevin
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

package argonms.map;

import java.util.Collections;
import java.util.List;

/**
 *
 * @author GoldenKevin
 */
public class NpcShop {
	private List<ShopItem> stock;

	public int getId() {
		return 0;
	}

	public List<ShopItem> getStock() {
		return Collections.unmodifiableList(stock);
	}

	public class ShopItem {
		private int itemid;
		private int price;
		private short remaining;

		public int getItemId() {
			return itemid;
		}

		public int getPrice() {
			return price;
		}

		public short getBuyable() {
			return remaining;
		}
	}
}
