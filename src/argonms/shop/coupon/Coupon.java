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

package argonms.shop.coupon;

import argonms.common.character.inventory.InventorySlot;
import argonms.common.util.collections.Pair;
import argonms.shop.character.CashShopStaging;
import argonms.shop.loading.cashshop.CashShopDataLoader;
import argonms.shop.loading.cashshop.Commodity;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 *
 * @author GoldenKevin
 */
public class Coupon {
	private final String code;
	private boolean initialized;
	private final List<Integer> items;
	private int maplePoints;
	private int mesos;
	private int remainingUses;
	private long expireDate;
	private final Set<Integer> usedBy;
	private boolean itemsDirty, usedByDirty;

	public Coupon(String code) {
		this.code = code;
		this.items = new ArrayList<Integer>();
		this.usedBy = new HashSet<Integer>();
	}

	public boolean exists() {
		return initialized;
	}

	public String getCode() {
		return code;
	}

	public List<Integer> getItems() {
		return Collections.unmodifiableList(items);
	}

	public int getMaplePointsReward() {
		return maplePoints;
	}

	public int getMesosReward() {
		return mesos;
	}

	public int getRemainingUses() {
		return remainingUses;
	}

	public long getExpireDate() {
		return expireDate;
	}

	public Set<Integer> getUsers() {
		return usedBy;
	}

	public boolean shouldUpdateUsers() {
		try {
			return usedByDirty;
		} finally {
			usedByDirty = false;
		}
	}

	public boolean shouldUpdateItems() {
		try {
			return itemsDirty;
		} finally {
			itemsDirty = false;
		}
	}

	public boolean canUse(int accountId) {
		return remainingUses > 0 && !usedBy.contains(Integer.valueOf(accountId));
	}

	public void use(int accountId) {
		remainingUses--;
		addUser(accountId);
	}

	public List<Pair<InventorySlot, CashShopStaging.CashPurchaseProperties>> createItems(int accountId) {
		List<Pair<InventorySlot, CashShopStaging.CashPurchaseProperties>> instances = new ArrayList<Pair<InventorySlot, CashShopStaging.CashPurchaseProperties>>();
		CashShopDataLoader csdl = CashShopDataLoader.getInstance();
		Commodity c;
		for (Integer sn : items)
			if ((c = csdl.getCommodity(sn.intValue())) != null)
				instances.add(CashShopStaging.createItem(c, sn.intValue(), accountId, null));
		return instances;
	}

	public void onInitialized() {
		initialized = true;
		usedByDirty = false;
		itemsDirty = false;
	}

	public void addItem(int sn) {
		items.add(Integer.valueOf(sn));
		itemsDirty = true;
	}

	public void removeItem(int sn) {
		items.remove(Integer.valueOf(sn));
		itemsDirty = true;
	}

	public void setMaplePoints(int reward) {
		this.maplePoints = reward;
	}

	public void setMesos(int reward) {
		this.mesos = reward;
	}

	public void setRemainingUses(int remainingUses) {
		this.remainingUses = remainingUses;
	}

	public void setExpireDate(long timestamp) {
		this.expireDate = timestamp;
	}

	public void addUser(int accountId) {
		usedBy.add(Integer.valueOf(accountId));
		usedByDirty = true;
	}

	public void clearUsers() {
		usedBy.clear();
		usedByDirty = true;
	}
}
