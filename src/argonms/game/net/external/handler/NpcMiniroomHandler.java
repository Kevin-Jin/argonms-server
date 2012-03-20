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

package argonms.game.net.external.handler;

import argonms.common.UniqueIdGenerator;
import argonms.common.character.inventory.Inventory;
import argonms.common.character.inventory.Inventory.InventoryType;
import argonms.common.character.inventory.InventorySlot;
import argonms.common.character.inventory.InventoryTools;
import argonms.common.character.inventory.InventoryTools.UpdatedSlots;
import argonms.common.loading.item.ItemDataLoader;
import argonms.common.net.external.CheatTracker;
import argonms.common.net.external.ClientSendOps;
import argonms.common.net.external.CommonPackets;
import argonms.common.util.input.LittleEndianReader;
import argonms.common.util.output.LittleEndianByteArrayWriter;
import argonms.game.character.GameCharacter;
import argonms.game.character.inventory.ItemTools;
import argonms.game.character.inventory.StorageInventory;
import argonms.game.loading.npc.NpcStorageKeeper;
import argonms.game.loading.shop.NpcShop;
import argonms.game.net.external.GameClient;
import argonms.game.net.external.GamePackets;
import java.util.EnumSet;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author GoldenKevin
 */
public class NpcMiniroomHandler {
	private static final Logger LOG = Logger.getLogger(NpcMiniroomHandler.class.getName());

	private static final byte
		//shop
		ACT_BUY = 0,
		ACT_SELL = 1,
		ACT_RECHARGE = 2,
		ACT_EXIT_SHOP = 3,
		//storage
		ACT_WITHDRAW_ITEM = 4,
		ACT_DEPOSIT_ITEM = 5,
		ACT_ARRANGE_ITEMS = 6,
		ACT_MESOS_TRANSFER = 7,
		ACT_EXIT_STORAGE = 8
	;

	private static final byte
		TRANSACTION_SUCCESS = 0,
		TRANSACTION_INVENTORY_FULL = 3
	;

	public static void handleNpcShopAction(LittleEndianReader packet, GameClient gc) {
		NpcShop shop = (NpcShop) gc.getNpcRoom();
		if (shop == null) {
			CheatTracker.get(gc).suspicious(CheatTracker.Infraction.PACKET_EDITING, "Tried to perform NPC shop transaction to nonexistant shop");
			return;
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
					CheatTracker.get(gc).suspicious(CheatTracker.Infraction.PACKET_EDITING, "Tried to buy nonexistant item from NPC shop");
					return;
				}
				GameCharacter p = gc.getPlayer();
				InventoryType invType = InventoryTools.getCategory(itemId);
				Inventory inv = p.getInventory(invType);
				if (p.getMesos() < totalPrice) {
					CheatTracker.get(gc).suspicious(CheatTracker.Infraction.PACKET_EDITING, "Tried to buy item from NPC shop with nonexistant mesos");
					return;
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
						CheatTracker.get(gc).suspicious(CheatTracker.Infraction.PACKET_EDITING, "Tried to buy more items from NPC shop than one slot allows");
						return;
					}
				} else {
					//the player's only paying for the normal amount of stars,
					//so he/she should have to recharge it himself/herself if
					//he/she has claw/gun mastery and is buying stars/bullets,
					//so that's why it's not getPersonalSlotMax
					synchronized(inv.getAll()) {
						if (inv.getFreeSlots(totalQuantity).size() >= totalQuantity) {
							changedSlots = InventoryTools.addToInventory(inv, InventoryTools.makeItemWithId(itemId), slotMax, false);
							for (int i = 1; i < totalQuantity; i++)
								changedSlots.union(InventoryTools.addToInventory(inv, InventoryTools.makeItemWithId(itemId), slotMax, false));
						}
					}
				}
				if (changedSlots != null) {
					p.gainMesos(-totalPrice, false);
					for (Short s : changedSlots.modifiedSlots) {
						short pos = s.shortValue();
						gc.getSession().send(GamePackets.writeInventoryUpdateSlotQuantity(invType, pos, p.getInventory(invType).get(pos)));
					}
					for (Short s : changedSlots.addedOrRemovedSlots) {
						short pos = s.shortValue();
						gc.getSession().send(GamePackets.writeInventoryAddSlot(invType, pos, p.getInventory(invType).get(pos)));
					}
					gc.getSession().send(writeConfirmShopTransaction(TRANSACTION_SUCCESS));
					p.itemCountChanged(itemId);
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
					CheatTracker.get(gc).suspicious(CheatTracker.Infraction.PACKET_EDITING, "Tried to sell nonexistant items to NPC shop");
					return;
				}
				int price = ItemDataLoader.getInstance().getWholePrice(itemId) * quantity;
				if (InventoryTools.isRechargeable(itemId)) {
					inventory.remove(slot);
					gc.getSession().send(GamePackets.writeInventoryClearSlot(invType, slot));
					int rechargeCost = shop.rechargeCost(itemId, item.getQuantity());
					if (rechargeCost < 0)
						rechargeCost = (int) Math.ceil(ItemDataLoader.getInstance().getUnitPrice(itemId) * item.getQuantity());
					if (rechargeCost > 0)
						price += rechargeCost;
				} else if (item.getQuantity() == quantity) {
					inventory.remove(slot);
					gc.getSession().send(GamePackets.writeInventoryClearSlot(invType, slot));
				} else {
					item.setQuantity((short) (item.getQuantity() - quantity));
					gc.getSession().send(GamePackets.writeInventoryUpdateSlotQuantity(invType, slot, item));
				}
				p.itemCountChanged(itemId);
				p.gainMesos(price, false);
				gc.getSession().send(writeConfirmShopTransaction(TRANSACTION_SUCCESS));
				break;
			} case ACT_RECHARGE : {
				short slot = packet.readShort();
				GameCharacter p = gc.getPlayer();
				Inventory inventory = p.getInventory(InventoryType.USE);
				InventorySlot item = inventory.get(slot);
				if (item == null || !InventoryTools.isRechargeable(item.getDataId())) {
					CheatTracker.get(gc).suspicious(CheatTracker.Infraction.PACKET_EDITING, "Tried to recharge nonexistant ranged weapon ammunition");
					return;
				}
				short slotMax = ItemTools.getPersonalSlotMax(p, item.getDataId());
				int rechargeCost = shop.rechargeCost(item.getDataId(), slotMax - item.getQuantity());
				if (rechargeCost < 0 || p.getMesos() < rechargeCost) {
					CheatTracker.get(gc).suspicious(CheatTracker.Infraction.PACKET_EDITING, "Tried to recharge ranged weapon ammunition with nonexistant mesos");
					return;
				}
				item.setQuantity(slotMax);
				p.gainMesos(-rechargeCost, false);
				gc.getSession().send(GamePackets.writeInventoryUpdateSlotQuantity(InventoryType.USE, slot, item));
				gc.getSession().send(writeConfirmShopTransaction(TRANSACTION_SUCCESS));
				break;
			} case ACT_EXIT_SHOP: {
				gc.setNpcRoom(null);
				break;
			}
		}
	}

	public static void handleNpcStorageAction(LittleEndianReader packet, GameClient gc) {
		NpcStorageKeeper keeper = (NpcStorageKeeper) gc.getNpcRoom();
		if (keeper == null) {
			CheatTracker.get(gc).suspicious(CheatTracker.Infraction.PACKET_EDITING, "Tried to perform storage keeper transaction to nonexistant storage keeper");
			return;
		}
		StorageInventory storageInv = gc.getPlayer().getStorageInventory();
		switch (packet.readByte()) {
			case ACT_WITHDRAW_ITEM: {
				GameCharacter p = gc.getPlayer();
				InventoryType invType = InventoryType.valueOf(packet.readByte());
				Inventory destInv = p.getInventory(invType);
				byte slot = packet.readByte();
				if (p.getMesos() < keeper.getWithdrawCost()) {
					gc.getSession().send(writeStorageInsufficientFunds());
					return;
				}
				InventorySlot item = storageInv.get(invType, slot);
				if (item == null) {
					CheatTracker.get(gc).suspicious(CheatTracker.Infraction.PACKET_EDITING, "Tried to take out nonexistant item from storage keeper");
					return;
				}
				if (!InventoryTools.canFitEntirely(destInv, item.getDataId(), item.getQuantity(), false)) {
					gc.getSession().send(writeStorageWithdrawInventoryFull());
					return;
				}
				p.gainMesos(-keeper.getWithdrawCost(), false);
				storageInv.remove(invType, slot);
				UpdatedSlots changedSlots = InventoryTools.addToInventory(destInv, item, item.getQuantity(), false);
				for (Short s : changedSlots.modifiedSlots) {
					short pos = s.shortValue();
					gc.getSession().send(GamePackets.writeInventoryUpdateSlotQuantity(invType, pos, p.getInventory(invType).get(pos)));
				}
				for (Short s : changedSlots.addedOrRemovedSlots) {
					short pos = s.shortValue();
					gc.getSession().send(GamePackets.writeInventoryAddSlot(invType, pos, p.getInventory(invType).get(pos)));
				}
				gc.getSession().send(writeStorageWithdrawal(storageInv, invType));
				p.itemCountChanged(item.getDataId());
				break;
			} case ACT_DEPOSIT_ITEM: {
				GameCharacter p = gc.getPlayer();
				short slot = packet.readShort();
				int itemId = packet.readInt();
				short quantity = packet.readShort();
				InventoryType invType = InventoryTools.getCategory(itemId);
				Inventory inv = p.getInventory(invType);
				InventorySlot item = inv.get(slot);
				if (item == null || item.getDataId() != itemId || quantity < 1 || quantity > item.getQuantity()) {
					CheatTracker.get(gc).suspicious(CheatTracker.Infraction.PACKET_EDITING, "Tried to store nonexistant item to storage keeper");
					return;
				}
				if (p.getMesos() < keeper.getDepositCost()) {
					gc.getSession().send(writeStorageInsufficientFunds());
					return;
				}
				if (storageInv.freeSlots() <= 0) {
					gc.getSession().send(writeStorageFull());
					return;
				}
				p.gainMesos(-keeper.getDepositCost(), false);
				if (quantity == item.getQuantity() || InventoryTools.isRechargeable(itemId)) {
					storageInv.put(inv.remove(slot));
					gc.getSession().send(GamePackets.writeInventoryClearSlot(invType, slot));
				} else {
					InventorySlot split = item.clone();
					item.setQuantity((short) (item.getQuantity() - quantity));
					split.setQuantity(quantity);
					if (item.getUniqueId() != 0) {
						try {
							split.setUniqueId(UniqueIdGenerator.incrementAndGet());
						} catch (Exception e) {
							LOG.log(Level.WARNING, "Failed to set new uid for cash item.", e);
						}
					}
					storageInv.put(split);
					gc.getSession().send(GamePackets.writeInventoryUpdateSlotQuantity(invType, slot, item));
				}
				p.itemCountChanged(item.getDataId());
				gc.getSession().send(writeStorageDeposit(storageInv, invType));
					break;
			} case ACT_ARRANGE_ITEMS: {
				//TODO: IMPLEMENT
				break;
			} case ACT_MESOS_TRANSFER: {
				GameCharacter p = gc.getPlayer();
				int delta = packet.readInt();
				if (delta > 0 && delta > storageInv.getMesos() || delta < 0 && -delta > p.getMesos()) {
					CheatTracker.get(gc).suspicious(CheatTracker.Infraction.PACKET_EDITING, "Tried to store nonexistant mesos to storage keeper");
					return;
				}
				if ((long) p.getMesos() + delta > Integer.MAX_VALUE) {
					gc.getSession().send(writeStorageWithdrawInventoryFull());
				} else if ((long) storageInv.getMesos() + -delta > Integer.MAX_VALUE) {
					gc.getSession().send(writeStorageFull());
				} else {
					p.gainMesos(delta, false);
					storageInv.changeMesos(delta);
					gc.getSession().send(writeStorageMesoUpdate(storageInv));
				}
				break;
			} case ACT_EXIT_STORAGE: {
				storageInv.collapse();
				gc.setNpcRoom(null);
				break;
			}
		}
	}

	private static byte[] writeConfirmShopTransaction(byte code) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(3);

		lew.writeShort(ClientSendOps.CONFIRM_SHOP_TRANSACTION);
		lew.writeByte(code);

		return lew.getBytes();
	}

	private static byte[] writeStorageWithdrawal(StorageInventory storage, InventoryType destInv) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter();

		lew.writeShort(ClientSendOps.NPC_STORAGE);
		lew.writeByte((byte) 0x09);
		lew.writeByte((byte) storage.getMaxSlots());
		lew.writeInt(storage.getBitfield(false, EnumSet.of(destInv), false));
		lew.writeInt(0);
		List<InventorySlot> items = storage.getRealItems(destInv);
		lew.writeByte((byte) items.size());
		for (InventorySlot item : items)
			CommonPackets.writeItemInfo(lew, item, true, false);

		return lew.getBytes();
	}

	private static byte[] writeStorageWithdrawInventoryFull() {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(3);

		lew.writeShort(ClientSendOps.NPC_STORAGE);
		lew.writeByte((byte) 0x0A);

		return lew.getBytes();
	}

	private static byte[] writeStorageDeposit(StorageInventory storage, InventoryType srcInv) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter();

		lew.writeShort(ClientSendOps.NPC_STORAGE);
		lew.writeByte((byte) 0x0C);
		lew.writeByte((byte) storage.getMaxSlots());
		lew.writeInt(storage.getBitfield(false, EnumSet.of(srcInv), false));
		lew.writeInt(0);
		List<InventorySlot> items = storage.getRealItems(srcInv);
		lew.writeByte((byte) items.size());
		for (InventorySlot item : items)
			CommonPackets.writeItemInfo(lew, item, true, false);

		return lew.getBytes();
	}

	private static byte[] writeStorageInsufficientFunds() {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(3);

		lew.writeShort(ClientSendOps.NPC_STORAGE);
		lew.writeByte((byte) 0x0F);

		return lew.getBytes();
	}

	private static byte[] writeStorageFull() {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(3);

		lew.writeShort(ClientSendOps.NPC_STORAGE);
		lew.writeByte((byte) 0x10);

		return lew.getBytes();
	}

	private static byte[] writeStorageMesoUpdate(StorageInventory storage) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(16);

		lew.writeShort(ClientSendOps.NPC_STORAGE);
		lew.writeByte((byte) 0x12);
		lew.writeByte((byte) storage.getMaxSlots());
		lew.writeInt(storage.getBitfield(false, EnumSet.noneOf(InventoryType.class), true));
		lew.writeInt(0);
		lew.writeInt(storage.getMesos());

		return lew.getBytes();
	}
}
