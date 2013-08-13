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

import argonms.common.character.Player;
import argonms.common.character.PlayerJob;
import argonms.common.character.inventory.Inventory;
import argonms.common.character.inventory.InventorySlot;
import argonms.common.character.inventory.Ring;
import argonms.common.net.external.CheatTracker;
import argonms.common.util.collections.Pair;
import argonms.common.util.input.LittleEndianReader;
import argonms.shop.ShopServer;
import argonms.shop.character.CashShopStaging;
import argonms.shop.character.ShopCharacter;
import argonms.shop.coupon.Coupon;
import argonms.shop.coupon.CouponFactory;
import argonms.shop.loading.cashshop.CashShopDataLoader;
import argonms.shop.loading.cashshop.Commodity;
import argonms.shop.loading.limitedcommodity.LimitedCommodity;
import argonms.shop.loading.limitedcommodity.LimitedCommodityDataLoader;
import argonms.shop.net.external.CashShopPackets;
import argonms.shop.net.external.ShopClient;
import java.util.ArrayList;
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
		BUY_SIMPLE_ITEM = 3,
		GIFT_ITEM = 4,
		UPDATE_WISH_LIST = 5,
		BUY_INVENTORY_SLOTS = 6,
		BUY_CASH_INVENTORY_SLOTS = 7,
		BUY_CHARACTER_SLOTS = 8,
		TAKE_FROM_STAGING = 12,
		PLACE_INTO_STAGING = 13,
		BUY_COUPLE_RING = 27,
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

	public static void handleCheckCash(LittleEndianReader packet, ShopClient sc) {
		sc.getSession().send(CashShopPackets.writeCashShopCurrencyBalance(sc.getPlayer()));
	}

	private static void buySimpleItem(ShopCharacter p, LittleEndianReader packet) {
		packet.readByte();
		int currencyType = packet.readInt();
		int serialNumber = packet.readInt();
		Commodity c = CashShopDataLoader.getInstance().getCommodity(serialNumber);
		if (c == null || !c.onSale || ShopServer.getInstance().getBlockedSerials().contains(Integer.valueOf(serialNumber))) {
			CheatTracker.get(p.getClient()).suspicious(CheatTracker.Infraction.CERTAIN_PACKET_EDITING, "Tried to buy nonexistent item from cash shop");
			p.getClient().getSession().send(CashShopPackets.writeBuyError(CashShopPackets.ERROR_OUT_OF_STOCK));
			return;
		}

		LimitedCommodity lc = LimitedCommodityDataLoader.getInstance().getLimitedCommodity(c.itemDataId);
		if (lc != null && lc.getSerialNumbers().contains(Integer.valueOf(serialNumber))) {
			synchronized (lc) {
				if (lc.getRemainingStock() == 0) {
					p.getClient().getSession().send(CashShopPackets.writeBuyError(CashShopPackets.ERROR_OUT_OF_STOCK));
					return;
				}

				LimitedCommodityDataLoader.getInstance().commitUsed(c.itemDataId, lc.incrementUsed());
			}
		}

		if (p.getCashShopInventory().isFull()) {
			//or maybe client already prevents us from doing this, in which case POSSIBLE_PACKET_EDITING?
			p.getClient().getSession().send(CashShopPackets.writeBuyError(CashShopPackets.ERROR_INVENTORY_FULL));
			return;
		}

		if (!p.gainCashShopCurrency(currencyType, -c.price)) {
			CheatTracker.get(p.getClient()).suspicious(CheatTracker.Infraction.POSSIBLE_PACKET_EDITING, "Tried to buy item from cash shop with nonexistent cash");
			p.getClient().getSession().send(CashShopPackets.writeBuyError(CashShopPackets.ERROR_INSUFFICIENT_CASH));
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
			p.getClient().getSession().send(CashShopPackets.writeGiftError(CashShopPackets.ERROR_BIRTHDAY));
			return;
		}

		Commodity c = CashShopDataLoader.getInstance().getCommodity(serialNumber);
		if (c == null || !c.onSale || ShopServer.getInstance().getBlockedSerials().contains(Integer.valueOf(serialNumber))) {
			CheatTracker.get(p.getClient()).suspicious(CheatTracker.Infraction.CERTAIN_PACKET_EDITING, "Tried to gift nonexistent item from cash shop");
			p.getClient().getSession().send(CashShopPackets.writeGiftError(CashShopPackets.ERROR_OUT_OF_STOCK));
			return;
		}

		LimitedCommodity lc = LimitedCommodityDataLoader.getInstance().getLimitedCommodity(c.itemDataId);
		if (lc != null && lc.getSerialNumbers().contains(Integer.valueOf(serialNumber))) {
			synchronized (lc) {
				if (lc.getRemainingStock() == 0) {
					p.getClient().getSession().send(CashShopPackets.writeGiftError(CashShopPackets.ERROR_OUT_OF_STOCK));
					return;
				}

				LimitedCommodityDataLoader.getInstance().commitUsed(c.itemDataId, lc.incrementUsed());
			}
		}

		if (p.getCashShopCurrency(ShopCharacter.GAME_CARD_NX) < c.price) {
			CheatTracker.get(p.getClient()).suspicious(CheatTracker.Infraction.POSSIBLE_PACKET_EDITING, "Tried to gift item from cash shop with nonexistent cash");
			p.getClient().getSession().send(CashShopPackets.writeGiftError(CashShopPackets.ERROR_INSUFFICIENT_CASH));
			return;
		}

		int recipientAcct = ShopCharacter.getAccountIdFromName(recipient);
		if (!CashShopStaging.giveGift(p.getClient().getAccountId(), p.getName(), recipientAcct, c, serialNumber, message, null)) {
			p.getClient().getSession().send(CashShopPackets.writeGiftError(CashShopPackets.ERROR_INVENTORY_FULL));
			return;
		}


		p.gainCashShopCurrency(ShopCharacter.GAME_CARD_NX, -c.price);
		p.getClient().getSession().send(CashShopPackets.writeGiftSent(recipient, c.itemDataId, c.price));
	}

	private static void updateWishList(ShopCharacter p, LittleEndianReader packet) {
		List<Integer> newList = new ArrayList<Integer>();
		CashShopDataLoader csdl = CashShopDataLoader.getInstance();
		for (int i = 0; i < 10; i++) {
			int sn = packet.readInt();
			if (sn != 0) {
				if (csdl.getCommodity(sn) == null) {
					CheatTracker.get(p.getClient()).suspicious(CheatTracker.Infraction.CERTAIN_PACKET_EDITING, "Tried to add nonexistent item to wishlist from cash shop");
					p.getClient().getSession().send(CashShopPackets.writeWishListError(CashShopPackets.ERROR_OUT_OF_STOCK));
					return;
				}

				newList.add(Integer.valueOf(sn));
			}
		}
		p.setWishList(newList);
		p.getClient().getSession().send(CashShopPackets.writeChangeWishList(p));
	}

	private static int getMaxInventorySlots(short job, Inventory.InventoryType type) {
		//somehow related to job advancement inventory expansion, but not exactly?
		//e.g. don't third, fourth job advancements get inventory expansion too?
		int max = 48;
		switch (PlayerJob.getJobPath(job)) {
			case PlayerJob.CLASS_WARRIOR:
				switch (type) {
					case EQUIP:
					case SETUP:
						max += 4;
						break;
					case USE:
					case ETC:
						max += 4;
						if (PlayerJob.getAdvancement(job) > 1)
							max += 4;
						break;
				}
				break;
			case PlayerJob.CLASS_MAGICIAN:
				switch (type) {
					case ETC:
						if (PlayerJob.getAdvancement(job) > 1)
							max += 4;
						break;
				}
				break;
			case PlayerJob.CLASS_BOWMAN:
				switch (type) {
					case EQUIP:
					case USE:
						max += 4;
						break;
					case ETC:
						if (PlayerJob.getAdvancement(job) > 1)
							max += 4;
						break;
				}
				break;
			case PlayerJob.CLASS_THIEF:
			case PlayerJob.CLASS_PIRATE:
				switch (type) {
					case EQUIP:
					case ETC:
						max += 4;
						break;
					case USE:
						if (PlayerJob.getAdvancement(job) > 1)
							max += 4;
						break;
				}
				break;
		}
		return max;
	}

	private static void buyInventorySlots(ShopCharacter p, LittleEndianReader packet) {
		packet.readByte();
		int currencyType = packet.readInt();
		boolean hasSerial = packet.readBool();
		Inventory.InventoryType invType;
		int cost;
		if (hasSerial) {
			int sn = packet.readInt();
			LOG.log(Level.INFO, "Unhandled item, SN {0}", sn);
			return;
		} else {
			invType = Inventory.InventoryType.valueOf(packet.readByte());
			cost = 4000;
		}

		Inventory inv = p.getInventory(invType);
		short currentSlots = inv.getMaxSlots();

		if (currentSlots + 4 > getMaxInventorySlots(p.getJob(), invType)) {
			CheatTracker.get(p.getClient()).suspicious(CheatTracker.Infraction.POSSIBLE_PACKET_EDITING, "Tried to buy too many " + invType + " slots in cash shop");
			p.getClient().getSession().send(CashShopPackets.writeBuyInventorySlotsError(CashShopPackets.ERROR_TOO_MANY_CASH_ITEMS));
			return;
		}

		if (!p.gainCashShopCurrency(currencyType, -cost)) {
			CheatTracker.get(p.getClient()).suspicious(CheatTracker.Infraction.POSSIBLE_PACKET_EDITING, "Tried to buy " + invType + " slots from cash shop with nonexistent cash");
			p.getClient().getSession().send(CashShopPackets.writeBuyInventorySlotsError(CashShopPackets.ERROR_INSUFFICIENT_CASH));
			return;
		}

		short newCapacity = p.getInventory(invType).increaseCapacity((short) 4);
		p.getClient().getSession().send(CashShopPackets.writeUpdateInventorySlots(invType, newCapacity));
	}

	private static void buyCashInventorySlots(ShopCharacter p, LittleEndianReader packet) {
		packet.readByte();
		int currencyType = packet.readInt();

		Inventory inv = p.getInventory(Inventory.InventoryType.CASH);
		short currentSlots = inv.getMaxSlots();

		if (currentSlots + 4 > getMaxInventorySlots(p.getJob(), Inventory.InventoryType.CASH)) {
			CheatTracker.get(p.getClient()).suspicious(CheatTracker.Infraction.POSSIBLE_PACKET_EDITING, "Tried to buy too many storage slots in cash shop");
			p.getClient().getSession().send(CashShopPackets.writeBuyStorageSlotsError(CashShopPackets.ERROR_TOO_MANY_CASH_ITEMS));
			return;
		}

		if (!p.gainCashShopCurrency(currencyType, -4000)) {
			CheatTracker.get(p.getClient()).suspicious(CheatTracker.Infraction.POSSIBLE_PACKET_EDITING, "Tried to buy storage slots from cash shop with nonexistent cash");
			p.getClient().getSession().send(CashShopPackets.writeBuyStorageSlotsError(CashShopPackets.ERROR_INSUFFICIENT_CASH));
			return;
		}

		short newCapacity = p.getInventory(Inventory.InventoryType.CASH).increaseCapacity((short) 4);
		p.getClient().getSession().send(CashShopPackets.writeUpdateStorageSlots(newCapacity));
	}

	private static void buyCharacterSlots(ShopCharacter p, LittleEndianReader packet) {
		packet.readByte();
		int currencyType = packet.readInt();

		short currentSlots = p.getMaxCharacters();

		if (currentSlots + 1 > 6) {
			CheatTracker.get(p.getClient()).suspicious(CheatTracker.Infraction.POSSIBLE_PACKET_EDITING, "Tried to buy too many characters slots in cash shop");
			p.getClient().getSession().send(CashShopPackets.writeBuyCharacterSlotsError(CashShopPackets.ERROR_TOO_MANY_CASH_ITEMS));
			return;
		}

		if (!p.gainCashShopCurrency(currencyType, -6900)) {
			CheatTracker.get(p.getClient()).suspicious(CheatTracker.Infraction.POSSIBLE_PACKET_EDITING, "Tried to buy character slots from cash shop with nonexistent cash");
			p.getClient().getSession().send(CashShopPackets.writeBuyCharacterSlotsError(CashShopPackets.ERROR_INSUFFICIENT_CASH));
			return;
		}

		short newCapacity = p.increaseMaxCharacters();
		p.getClient().getSession().send(CashShopPackets.writeUpdateCharacterSlots(newCapacity));
	}

	private static void transferFromStaging(ShopCharacter p, LittleEndianReader packet) {
		long uniqueId = packet.readLong();
		Inventory.InventoryType type = Inventory.InventoryType.valueOf(packet.readByte());
		packet.readByte();
		packet.readByte();
		InventorySlot item = p.getCashShopInventory().getByUniqueId(uniqueId);
		if (item == null) {
			CheatTracker.get(p.getClient()).suspicious(CheatTracker.Infraction.POSSIBLE_PACKET_EDITING, "Tried to transfer nonexistent cash shop item from staging");
			p.getClient().getSession().send(CashShopPackets.writeTakeError(CashShopPackets.ERROR_UNKNOWN));
			return;
		}

		Inventory inv = p.getInventory(type);
		List<Short> freeSlots = inv.getFreeSlots(1);
		if (freeSlots.isEmpty()) {
			p.getClient().getSession().send(CashShopPackets.writeTakeError(CashShopPackets.ERROR_INVENTORY_FULL));
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
			CheatTracker.get(p.getClient()).suspicious(CheatTracker.Infraction.POSSIBLE_PACKET_EDITING, "Tried to transfer nonexistent cash shop item to staging");
			p.getClient().getSession().send(CashShopPackets.writePlaceError(CashShopPackets.ERROR_UNKNOWN));
			return;
		}

		CashShopStaging inv = p.getCashShopInventory();
		CashShopStaging.CashPurchaseProperties props = CashShopStaging.CashPurchaseProperties.loadFromDatabase(uniqueId, item.getDataId(), p.getClient().getAccountId());
		inv.append(item, props);
		p.getClient().getSession().send(CashShopPackets.writeMoveToStaging(props, item));
	}

	private static void buyCoupleRing(final ShopCharacter p, LittleEndianReader packet) {
		int enteredBirthday = packet.readInt();
		int currencyType = packet.readInt();
		final int serialNumber = packet.readInt();
		final String recipient = packet.readLengthPrefixedString();
		String message = packet.readLengthPrefixedString();
		if (p.getBirthday() != 0 && p.getBirthday() != enteredBirthday) {
			p.getClient().getSession().send(CashShopPackets.writeGiftError(CashShopPackets.ERROR_BIRTHDAY));
			return;
		}

		final Commodity c = CashShopDataLoader.getInstance().getCommodity(serialNumber);
		if (c == null || !c.onSale || ShopServer.getInstance().getBlockedSerials().contains(Integer.valueOf(serialNumber))) {
			CheatTracker.get(p.getClient()).suspicious(CheatTracker.Infraction.CERTAIN_PACKET_EDITING, "Tried to buy nonexistent couple ring from cash shop");
			p.getClient().getSession().send(CashShopPackets.writeGiftError(CashShopPackets.ERROR_OUT_OF_STOCK));
			return;
		}

		LimitedCommodity lc = LimitedCommodityDataLoader.getInstance().getLimitedCommodity(c.itemDataId);
		if (lc != null && lc.getSerialNumbers().contains(Integer.valueOf(serialNumber))) {
			synchronized (lc) {
				if (lc.getRemainingStock() == 0) {
					p.getClient().getSession().send(CashShopPackets.writeGiftError(CashShopPackets.ERROR_OUT_OF_STOCK));
					return;
				}

				LimitedCommodityDataLoader.getInstance().commitUsed(c.itemDataId, lc.incrementUsed());
			}
		}

		if (p.getCashShopInventory().isFull()) {
			//or maybe client already prevents us from doing this, in which case POSSIBLE_PACKET_EDITING?
			p.getClient().getSession().send(CashShopPackets.writeGiftError(CashShopPackets.ERROR_INVENTORY_FULL));
			return;
		}

		if (p.getCashShopCurrency(currencyType) < c.price) {
			CheatTracker.get(p.getClient()).suspicious(CheatTracker.Infraction.POSSIBLE_PACKET_EDITING, "Tried to buy couple ring from cash shop with nonexistent cash");
			p.getClient().getSession().send(CashShopPackets.writeGiftError(CashShopPackets.ERROR_INSUFFICIENT_CASH));
			return;
		}

		int recipientAcct = ShopCharacter.getAccountIdFromName(recipient);
		boolean success = CashShopStaging.giveGift(p.getClient().getAccountId(), p.getName(), recipientAcct, c, serialNumber, message, new CashShopStaging.ItemManipulator() {
			@Override
			public void manipulate(InventorySlot partnersRing) {
				Pair<InventorySlot, CashShopStaging.CashPurchaseProperties> ourRing = CashShopStaging.createItem(c, serialNumber, p.getClient().getAccountId(), null);
				Ring ring = (Ring) ourRing.left;
				ring.setPartnerCharId(Player.getIdFromName(recipient));
				ring.setPartnerRingId(partnersRing.getUniqueId());
				p.getCashShopInventory().append(ourRing.left, ourRing.right);
				p.getClient().getSession().send(CashShopPackets.writeInsertToStaging(ourRing.right, ourRing.left));

				ring = (Ring) partnersRing;
				ring.setPartnerCharId(p.getId());
				ring.setPartnerRingId(ourRing.left.getUniqueId());
			}
		});
		if (!success) {
			p.getClient().getSession().send(CashShopPackets.writeGiftError(CashShopPackets.ERROR_INVENTORY_FULL));
			return;
		}

		p.gainCashShopCurrency(currencyType, -c.price);
	}

	private static void buyPackage(ShopCharacter p, LittleEndianReader packet) {
		
	}

	private static void giftPackage(ShopCharacter p, LittleEndianReader packet) {
		
	}

	private static void buyQuestItem(ShopCharacter p, LittleEndianReader packet) {
		
	}

	private static void buyFriendshipRing(final ShopCharacter p, LittleEndianReader packet) {
		int enteredBirthday = packet.readInt();
		int currencyType = packet.readInt();
		final int serialNumber = packet.readInt();
		final String recipient = packet.readLengthPrefixedString();
		String message = packet.readLengthPrefixedString();
		if (p.getBirthday() != 0 && p.getBirthday() != enteredBirthday) {
			p.getClient().getSession().send(CashShopPackets.writeGiftError(CashShopPackets.ERROR_BIRTHDAY));
			return;
		}

		final Commodity c = CashShopDataLoader.getInstance().getCommodity(serialNumber);
		if (c == null || !c.onSale || ShopServer.getInstance().getBlockedSerials().contains(Integer.valueOf(serialNumber))) {
			CheatTracker.get(p.getClient()).suspicious(CheatTracker.Infraction.CERTAIN_PACKET_EDITING, "Tried to buy nonexistent friendship ring from cash shop");
			p.getClient().getSession().send(CashShopPackets.writeGiftError(CashShopPackets.ERROR_OUT_OF_STOCK));
			return;
		}

		LimitedCommodity lc = LimitedCommodityDataLoader.getInstance().getLimitedCommodity(c.itemDataId);
		if (lc != null && lc.getSerialNumbers().contains(Integer.valueOf(serialNumber))) {
			synchronized (lc) {
				if (lc.getRemainingStock() == 0) {
					p.getClient().getSession().send(CashShopPackets.writeGiftError(CashShopPackets.ERROR_OUT_OF_STOCK));
					return;
				}

				LimitedCommodityDataLoader.getInstance().commitUsed(c.itemDataId, lc.incrementUsed());
			}
		}

		if (p.getCashShopInventory().isFull()) {
			//or maybe client already prevents us from doing this, in which case POSSIBLE_PACKET_EDITING?
			p.getClient().getSession().send(CashShopPackets.writeGiftError(CashShopPackets.ERROR_INVENTORY_FULL));
			return;
		}

		if (p.getCashShopCurrency(currencyType) < c.price) {
			CheatTracker.get(p.getClient()).suspicious(CheatTracker.Infraction.POSSIBLE_PACKET_EDITING, "Tried to buy friendship ring from cash shop with nonexistent cash");
			p.getClient().getSession().send(CashShopPackets.writeGiftError(CashShopPackets.ERROR_INSUFFICIENT_CASH));
			return;
		}

		int recipientAcct = ShopCharacter.getAccountIdFromName(recipient);
		boolean success = CashShopStaging.giveGift(p.getClient().getAccountId(), p.getName(), recipientAcct, c, serialNumber, message, new CashShopStaging.ItemManipulator() {
			@Override
			public void manipulate(InventorySlot partnersRing) {
				Pair<InventorySlot, CashShopStaging.CashPurchaseProperties> ourRing = CashShopStaging.createItem(c, serialNumber, p.getClient().getAccountId(), null);
				Ring ring = (Ring) ourRing.left;
				ring.setPartnerCharId(Player.getIdFromName(recipient));
				ring.setPartnerRingId(partnersRing.getUniqueId());
				p.getCashShopInventory().append(ourRing.left, ourRing.right);
				p.getClient().getSession().send(CashShopPackets.writeInsertToStaging(ourRing.right, ourRing.left));

				ring = (Ring) partnersRing;
				ring.setPartnerCharId(p.getId());
				ring.setPartnerRingId(ourRing.left.getUniqueId());
			}
		});
		if (!success) {
			p.getClient().getSession().send(CashShopPackets.writeGiftError(CashShopPackets.ERROR_INVENTORY_FULL));
			return;
		}

		p.gainCashShopCurrency(currencyType, -c.price);
	}

	public static void handleAction(LittleEndianReader packet, ShopClient sc) {
		ShopCharacter p = sc.getPlayer();
		switch (packet.readByte()) {
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
			case BUY_COUPLE_RING:
				buyCoupleRing(p, packet);
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

	public static void handleRedeemCoupon(LittleEndianReader packet, ShopClient sc) {
		packet.readShort();
		String code = packet.readLengthPrefixedString();
		Coupon c = CouponFactory.getInstance().getCoupon(code);
		if (c == null) {
			sc.getSession().send(CashShopPackets.writeCouponError(CashShopPackets.ERROR_COUPON_NUMBER));
			return;
		}
		synchronized (c) {
			if (!c.exists()) {
				sc.getSession().send(CashShopPackets.writeCouponError(CashShopPackets.ERROR_COUPON_NUMBER));
				return;
			}

			if (c.getExpireDate() <= System.currentTimeMillis()) {
				sc.getSession().send(CashShopPackets.writeCouponError(CashShopPackets.ERROR_COUPON_EXPIRED));
				return;
			}

			if (!c.canUse(sc.getAccountId())) {
				sc.getSession().send(CashShopPackets.writeCouponError(CashShopPackets.ERROR_COUPON_USED));
				return;
			}

			c.use(sc.getAccountId());
			CouponFactory.getInstance().commitCoupon(c);

			List<Pair<InventorySlot, CashShopStaging.CashPurchaseProperties>> items = c.createItems(sc.getAccountId());
			if (!items.isEmpty()) {
				CashShopStaging inv = sc.getPlayer().getCashShopInventory();
				for (Pair<InventorySlot, CashShopStaging.CashPurchaseProperties> item : items)
					inv.append(item.left, item.right);
			}
			sc.getPlayer().gainCashShopCurrency(ShopCharacter.MAPLE_POINTS, c.getMaplePointsReward());
			sc.getPlayer().gainMesos(c.getMesosReward());
			sc.getSession().send(CashShopPackets.writeCouponRewards(items, c.getMaplePointsReward(), c.getMesosReward()));
		}
	}
}
