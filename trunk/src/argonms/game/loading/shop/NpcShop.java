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

package argonms.game.loading.shop;

import argonms.common.loading.item.ItemDataLoader;
import argonms.game.net.external.GameClient.NpcMiniroom;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author GoldenKevin
 */
public abstract class NpcShop implements NpcMiniroom {
	public static class ShopSlot {
		public final int itemId;
		public final short quantity;
		public final int price;

		protected ShopSlot(int itemId, short quantity, int price) {
			this.itemId = itemId;
			this.quantity = quantity;
			this.price = price;
		}
	}

	private final List<ShopSlot> items;
	protected Map<Integer, Double> rechargeableOnly;

	protected NpcShop(List<ShopSlot> items) {
		this.items = items;
	}

	public ShopSlot get(short index) {
		return items.get(Integer.valueOf(index));
	}

	public List<ShopSlot> allItems() {
		return items;
	}

	public abstract double rechargeCost(int itemId);

	public abstract int rechargeCost(int itemId, int amount);

	public Map<Integer, Double> nonBuyableRechargeables() {
		return rechargeableOnly;
	}

	protected static class DefaultNpcShopStock extends NpcShop {
		protected DefaultNpcShopStock(List<ShopSlot> items) {
			super(items);
			this.rechargeableOnly = new HashMap<Integer, Double>();
			for (int itemId = 2070000; itemId <= 2070018; itemId++) //stars
				if (itemId != 2070014 && itemId != 2070017)
					rechargeableOnly.put(Integer.valueOf(itemId), Double.valueOf(ItemDataLoader.getInstance().getUnitPrice(itemId)));
			for (int itemId = 2330000; itemId <= 2330006; itemId++) //bullets
				rechargeableOnly.put(Integer.valueOf(itemId), Double.valueOf(ItemDataLoader.getInstance().getUnitPrice(itemId)));
			//more bullets - Blaze and Glaze Capsules
			rechargeableOnly.put(Integer.valueOf(2331000), Double.valueOf(ItemDataLoader.getInstance().getUnitPrice(2331000)));
			rechargeableOnly.put(Integer.valueOf(2332000), Double.valueOf(ItemDataLoader.getInstance().getUnitPrice(2332000)));
			for (ShopSlot item : allItems())
				if (rechargeableOnly.containsKey(Integer.valueOf(item.itemId)))
					rechargeableOnly.remove(Integer.valueOf(item.itemId));
		}

		@Override
		public double rechargeCost(int itemId) {
			return ItemDataLoader.getInstance().getUnitPrice(itemId);
		}

		@Override
		public int rechargeCost(int itemId, int amount) {
			return (int) Math.ceil(ItemDataLoader.getInstance().getUnitPrice(itemId) * amount);
		}
	}

	protected static class McdbNpcShopStock extends NpcShop {
		private final Map<Integer, Double> rechargeables;

		protected McdbNpcShopStock(Map<Integer, Double> rechargeables, List<ShopSlot> items) {
			super(items);
			this.rechargeables = rechargeables;
			this.rechargeableOnly = new HashMap<Integer, Double>(rechargeables);
			for (ShopSlot item : allItems())
				if (rechargeableOnly.containsKey(Integer.valueOf(item.itemId)))
					rechargeableOnly.remove(Integer.valueOf(item.itemId));
		}

		@Override
		public double rechargeCost(int itemId) {
			return rechargeables.get(Integer.valueOf(itemId)).doubleValue();
		}

		@Override
		public int rechargeCost(int itemId, int amount) {
			Double unitCost = rechargeables.get(Integer.valueOf(itemId));
			return (unitCost != null) ? ((int) Math.ceil(unitCost.doubleValue() * amount)) : -1;
		}
	}
}
