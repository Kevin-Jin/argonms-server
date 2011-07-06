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

package argonms.game.handler;

import argonms.common.character.inventory.Inventory;
import argonms.common.character.inventory.Inventory.InventoryType;
import argonms.common.character.inventory.InventorySlot;
import argonms.common.character.inventory.InventoryTools;
import argonms.common.character.inventory.InventoryTools.UpdatedSlots;
import argonms.common.loading.item.ItemDataLoader;
import argonms.common.net.external.ClientSendOps;
import argonms.common.tools.input.LittleEndianReader;
import argonms.common.tools.output.LittleEndianByteArrayWriter;
import argonms.game.GameClient;
import argonms.game.GameCommonPackets;
import argonms.game.character.GameCharacter;
import argonms.game.character.ItemTools;
import argonms.game.field.NpcMiniroom.AccountItemStorage;
import argonms.game.loading.shop.NpcShop;

/**
 *
 * @author GoldenKevin
 */
public class NpcMiniroomHandler {
	private static final byte
		//shop
		ACT_BUY = 0,
		ACT_SELL = 1,
		ACT_RECHARGE = 2,
		ACT_EXIT_SHOP = 3,
		//storage
		ACT_TAKE_ITEM = 4,
		ACT_STORE_ITEM = 5,
		ACT_MESOS_TRANSFER = 6,
		ACT_EXIT_STORAGE = 7
	;

	private static final byte
		TRANSACTION_SUCCESS = 0,
		TRANSACTION_INVENTORY_FULL = 3
	;

	public static void handleNpcShopAction(LittleEndianReader packet, GameClient gc) {
		NpcShop shop = (NpcShop) gc.getNpcRoom();
		if (shop == null) {
			//TODO: hacking
		}
		switch (packet.readByte()) {
			case ACT_BUY : {
				short position = packet.readShort();
				int itemId = packet.readInt();
				short quantity = packet.readShort();
				int price = packet.readInt();

				NpcShop.ShopSlot item = shop.get(position);
				int totalPrice = price * quantity;
				int totalQuantity = item.quantity * quantity;
				if (item == null || item.itemId != itemId || item.price != price) {
					//TODO: hacking
				}
				GameCharacter p = gc.getPlayer();
				InventoryType invType = InventoryTools.getCategory(itemId);
				Inventory inv = p.getInventory(invType);
				if (p.getMesos() < totalPrice) {
					//TODO: hacking
				}
				UpdatedSlots changedSlots = null;
				short slotMax = ItemDataLoader.getInstance().getSlotMax(itemId);
				if (!InventoryTools.isRechargeable(itemId)) {
					//client normally only allows total quantity to be less than
					//slot's max - unless the packet bypassed the client checks!
					if (totalQuantity < slotMax) {
						if (InventoryTools.canFitEntirely(inv, itemId, quantity, true))
							changedSlots = InventoryTools.addToInventory(p.getInventory(invType), itemId, totalQuantity);
					} else {
						//TODO: hacking
					}
				} else {
					//the player's only paying for the normal amount of stars,
					//so he/she should have to recharge it himself/herself if
					//he/she has claw/gun mastery and is buying stars/bullets,
					//so that's why it's not getPersonalSlotMax
					if (inv.getFreeSlots(totalQuantity).size() >= totalQuantity) {
						changedSlots = InventoryTools.addToInventory(inv, InventoryTools.makeItemWithId(itemId), slotMax, false);
						for (int i = 1; i < totalQuantity; i++)
							changedSlots.union(InventoryTools.addToInventory(inv, InventoryTools.makeItemWithId(itemId), slotMax, false));
					}
				}
				if (changedSlots != null) {
					p.gainMesos(-totalPrice, false);
					for (Short s : changedSlots.modifiedSlots) {
						short pos = s.shortValue();
						gc.getSession().send(GameCommonPackets.writeInventorySlotUpdate(invType, pos, p.getInventory(invType).get(pos)));
					}
					for (Short s : changedSlots.addedOrRemovedSlots) {
						short pos = s.shortValue();
						gc.getSession().send(GameCommonPackets.writeInventoryAddSlot(invType, pos, p.getInventory(invType).get(pos)));
					}
					gc.getSession().send(writeConfirmShopTransaction(TRANSACTION_SUCCESS));
				} else {
					gc.getSession().send(writeConfirmShopTransaction(TRANSACTION_INVENTORY_FULL));
				}
				break;
			} case ACT_SELL : {
				short slot = packet.readShort();
				int itemId = packet.readInt();
				short quantity = packet.readShort();
				GameCharacter p = gc.getPlayer();
				InventoryType invType = InventoryTools.getCategory(itemId);
				Inventory inventory = p.getInventory(invType);
				InventorySlot item = inventory.get(slot);
				if (item == null || item.getDataId() != itemId || !InventoryTools.isRechargeable(itemId) && item.getQuantity() < quantity) {
					//TODO: hacking
				}
				int price = ItemDataLoader.getInstance().getWholePrice(itemId) * quantity;
				if (InventoryTools.isRechargeable(itemId)) {
					inventory.remove(slot);
					gc.getSession().send(GameCommonPackets.writeInventoryClearSlot(invType, slot));
					int rechargeCost = shop.rechargeCost(itemId, item.getQuantity());
					if (rechargeCost < 0)
						rechargeCost = (int) Math.ceil(ItemDataLoader.getInstance().getUnitPrice(itemId) * item.getQuantity());
					if (rechargeCost > 0)
						price += rechargeCost;
				} else if (item.getQuantity() == quantity) {
					inventory.remove(slot);
					gc.getSession().send(GameCommonPackets.writeInventoryClearSlot(invType, slot));
				} else {
					item.setQuantity((short) (item.getQuantity() - quantity));
					gc.getSession().send(GameCommonPackets.writeInventorySlotUpdate(invType, slot, item));
				}
				p.gainMesos(price, false);
				gc.getSession().send(writeConfirmShopTransaction(TRANSACTION_SUCCESS));
				break;
			} case ACT_RECHARGE : {
				short slot = packet.readShort();
				GameCharacter p = gc.getPlayer();
				Inventory inventory = p.getInventory(InventoryType.USE);
				InventorySlot item = inventory.get(slot);
				if (item == null || !InventoryTools.isRechargeable(item.getDataId())) {
					//TODO: hacking
				}
				short slotMax = ItemTools.getPersonalSlotMax(p, item.getDataId());
				int rechargeCost = shop.rechargeCost(item.getDataId(), slotMax - item.getQuantity());
				if (rechargeCost < 0 || p.getMesos() < rechargeCost) {
					//TODO: hacking
				}
				item.setQuantity(slotMax);
				p.gainMesos(-rechargeCost, false);
				gc.getSession().send(GameCommonPackets.writeInventorySlotUpdate(InventoryType.USE, slot, item));
				gc.getSession().send(writeConfirmShopTransaction(TRANSACTION_SUCCESS));
				break;
			} case ACT_EXIT_SHOP: {
				gc.setNpcRoom(null);
				break;
			}
		}
	}

	public static void handleNpcStorageAction(LittleEndianReader packet, GameClient gc) {
		AccountItemStorage storage = (AccountItemStorage) gc.getNpcRoom();
		if (storage == null) {
			//TODO: hacking
		}
		//TODO: implement
		switch (packet.readByte()) {
			case ACT_TAKE_ITEM:
				break;
			case ACT_STORE_ITEM:
				break;
			case ACT_MESOS_TRANSFER:
				break;
			case ACT_EXIT_STORAGE:
				gc.setNpcRoom(null);
				break;
		}
	}

	private static byte[] writeConfirmShopTransaction(byte code) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter();

		lew.writeShort(ClientSendOps.CONFIRM_SHOP_TRANSACTION);
		lew.writeByte(code);

		return lew.getBytes();
	}
}
