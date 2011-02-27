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

package argonms.net.client.handler;

import argonms.character.Player;
import argonms.character.inventory.Inventory;
import argonms.character.inventory.Inventory.InventoryType;
import argonms.character.inventory.InventorySlot;
import argonms.character.inventory.InventorySlot.ItemType;
import argonms.character.inventory.InventoryTools;
import argonms.game.GameClient;
import argonms.loading.item.ItemDataLoader;
import argonms.map.MapEntity;
import argonms.map.MapEntity.MapEntityType;
import argonms.map.entity.ItemDrop;
import argonms.net.client.CommonPackets;
import argonms.net.client.RemoteClient;
import argonms.tools.input.LittleEndianReader;

/**
 *
 * @author GoldenKevin
 */
public class InventoryHandler {
	public static void handleItemMove(LittleEndianReader packet, RemoteClient rc) {
		packet.readInt();
		InventoryType type = InventoryType.get(packet.readByte());
		short src = packet.readShort();
		short dst = packet.readShort();
		short qty = packet.readShort();
		Player p = ((GameClient) rc).getPlayer();
		if (src < 0 && dst > 0) { //unequip
			InventoryTools.unequip(p.getInventory(InventoryType.EQUIPPED), p.getInventory(InventoryType.EQUIP), src, dst);
			rc.getSession().send(CommonPackets.writeInventoryMoveItem(InventoryType.EQUIP, src, dst, (byte) 1));
			p.updateEquips();
		} else if (dst < 0) { //equip
			short[] result = InventoryTools.equip(p.getInventory(InventoryType.EQUIP), p.getInventory(InventoryType.EQUIPPED), src, dst);
			if (result != null) {
				rc.getSession().send(CommonPackets.writeInventoryMoveItem(InventoryType.EQUIP, src, dst, (byte) 2));
				if (result.length == 2)
					rc.getSession().send(CommonPackets.writeInventoryMoveItem(InventoryType.EQUIP, result[0], result[1], (byte) 1));
				p.updateEquips();
			} else {
				rc.getSession().send(CommonPackets.writeInventoryFull());
			}
		} else if (dst == 0) { //drop
			Inventory inv = p.getInventory(src >= 0 ? type : InventoryType.EQUIPPED);
			InventorySlot item = inv.get(src);
			if (item.getType() == ItemType.PET) {
				//TODO: unequip pet
			}
			InventorySlot toDrop;
			short newQty = (short) (item.getQuantity() - qty);
			if (newQty == 0) {
				inv.remove(src);
				toDrop = item;
				rc.getSession().send(CommonPackets.writeInventoryClearSlot(type, src));
			} else {
				toDrop = item.clone();
				toDrop.setQuantity(newQty);
				rc.getSession().send(CommonPackets.writeInventoryDropItem(type, src, newQty));
			}
			ItemDrop d = new ItemDrop(toDrop);
			d.init(p.getId(), p.getPosition(), p.getPosition(), p.getId());
			if (ItemDataLoader.getInstance().canDrop(toDrop.getItemId()))
				p.getMap().drop(d);
			else //TODO: does this just show to the current player or to the entire map?
				rc.getSession().send(d.getDisappearMessage());
		} else { //move item
			Inventory inv = p.getInventory(type);
			InventorySlot move = inv.get(src);
			InventorySlot replace = inv.get(dst);
			short slotMax = ItemDataLoader.getInstance().getSlotMax(move.getItemId());
			if (notStackable(move, replace, slotMax)) { //swap
				exchange(p, type, src, dst);
			} else { //merge!
				short srcQty = move.getQuantity();
				short dstQty = replace.getQuantity();
				int total = srcQty + dstQty;
				if (total > slotMax) { //exchange quantities
					short rest = (short) (total - slotMax);
					move.setQuantity(rest);
					replace.setQuantity(slotMax);
					rc.getSession().send(CommonPackets.writeInventoryMoveItemShiftQuantities(type, src, rest, dst, slotMax));
				} else { //combine
					replace.setQuantity((short) total);
					inv.remove(src);
					rc.getSession().send(CommonPackets.writeInventoryMoveItemCombineQuantities(type, src, dst, (short) total));
				}
			}
		}
	}

	public static void handleMesoDrop(LittleEndianReader packet, RemoteClient rc) {
		packet.readInt();
		int amount = packet.readInt();
		Player p = ((GameClient) rc).getPlayer();
		if (amount <= p.getMesos()) {
			p.gainMesos(-amount);
			ItemDrop d = new ItemDrop(amount);
			d.init(p.getId(), p.getPosition(), p.getPosition(), p.getId());
			p.getMap().drop(d);
		}
	}

	public static void handleMapItemPickUp(LittleEndianReader packet, RemoteClient rc) {
		/*byte mode = */packet.readByte();
		packet.readInt(); //?
		/*short x = */packet.readShort();
		/*short y = */packet.readShort();
		int eid = packet.readInt();
		Player p = ((GameClient) rc).getPlayer();
		MapEntity ent = p.getMap().getEntityById(eid);
		if (ent == null) {
			rc.getSession().send(CommonPackets.writeInventoryFull());
			rc.getSession().send(CommonPackets.writeShowInventoryFull());
			return;
		}
		if (ent.getEntityType() == MapEntityType.ITEM) {
			ItemDrop d = (ItemDrop) ent;
			if (!d.isAlive()) {
				rc.getSession().send(CommonPackets.writeInventoryFull());
				rc.getSession().send(CommonPackets.writeShowInventoryFull());
				return;
			}
			p.getMap().pickUpDrop(d, p);
		}
	}

	private static boolean notStackable(InventorySlot src, InventorySlot dst, short slotMax) {
		return dst == null || src.getItemId() != dst.getItemId() || slotMax == 1
				|| InventoryTools.isThrowingStar(src.getItemId())
				|| InventoryTools.isBullet(src.getItemId());
	}

	private static void exchange(Player p, InventoryType type, short src, short dst) {
		Inventory inv = p.getInventory(type);
		InventorySlot item1 = inv.remove(src);
		InventorySlot item2 = inv.remove(dst);
		inv.put(dst, item1);
		if (item2 != null)
			inv.put(src, item2);
		p.getClient().getSession().send(CommonPackets.writeInventoryMoveItem(type, src, dst, (byte) -1));
	}
}
