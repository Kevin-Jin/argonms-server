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

package argonms.game.field.entity;

import argonms.common.character.inventory.Inventory;
import argonms.common.character.inventory.Inventory.InventoryType;
import argonms.common.character.inventory.InventorySlot;
import argonms.common.character.inventory.InventoryTools;
import argonms.common.character.inventory.InventoryTools.UpdatedSlots;
import argonms.common.net.external.ClientSendOps;
import argonms.common.net.external.ClientSession;
import argonms.common.net.external.CommonPackets;
import argonms.common.util.output.LittleEndianByteArrayWriter;
import argonms.game.character.GameCharacter;
import argonms.game.field.GameMap;
import argonms.game.net.external.GamePackets;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 *
 * @author GoldenKevin
 */
public class Trade extends Miniroom {
	private final InventorySlot[][] items;
	private final int[] mesos;
	private final boolean[] confirmed;
	private boolean tradeCompleted;

	public Trade(GameCharacter owner) {
		super(owner, 2, null, null, (byte) 0);
		openToMap = false;
		items = new InventorySlot[2][9];
		mesos = new int[2];
		confirmed = new boolean[2];
	}

	@Override
	public boolean joinRoom(GameCharacter p) {
		for (byte i = 1; i < getMaxPlayers(); i++) {
			if (getPlayerByPosition(i) == null) {
				p.setMiniRoom(this);
				sendToAll(getThirdPersonJoinMessage(p, i));
				setPlayerToPosition(i, p);
				p.getClient().getSession().send(getFirstPersonJoinMessage(p));
				return true;
			}
		}
		return false;
	}

	@Override
	public void leaveRoom(GameCharacter p) {
		GameCharacter v;
		closeRoom(p.getMap());
		for (byte i = 0; i < getMaxPlayers(); i++) {
			if ((v = getPlayerByPosition(i)) != null) {
				v.setMiniRoom(null);
				v.getClient().getSession().send(getFirstPersonLeaveMessage(i, EXIT_TRADE_CANCELED));
			}
		}
	}

	private boolean canFitAllItemsAndMesos() {
		Map<Integer, Short> itemQtys;
		for (byte i = 0; i < getMaxPlayers(); i++) {
			GameCharacter p = getPlayerByPosition(i);
			byte traderPos = (byte) ((i + 1) % 2);
			if (((long) p.getMesos() + mesos[traderPos]) > Integer.MAX_VALUE) {
				p.getClient().getSession().send(GamePackets.writeInventoryNoChange());
				p.getClient().getSession().send(GamePackets.writeShowInventoryFull());
				return false;
			}
			itemQtys = new HashMap<Integer, Short>();
			for (InventorySlot item : items[traderPos]) {
				if (item != null) {
					Integer oId = Integer.valueOf(item.getDataId());
					Short qty = itemQtys.get(oId);
					itemQtys.put(oId, Short.valueOf((short) (qty != null ? qty.shortValue() + item.getQuantity() : item.getQuantity())));
				}
			}

			Map<InventoryType, Integer> netEmptySlotRemovals = new EnumMap<InventoryType, Integer>(InventoryType.class);
			netEmptySlotRemovals.put(InventoryType.EQUIP, Integer.valueOf(0));
			netEmptySlotRemovals.put(InventoryType.USE, Integer.valueOf(0));
			netEmptySlotRemovals.put(InventoryType.SETUP, Integer.valueOf(0));
			netEmptySlotRemovals.put(InventoryType.ETC, Integer.valueOf(0));
			netEmptySlotRemovals.put(InventoryType.CASH, Integer.valueOf(0));

			for (Entry<Integer, Short> entry : itemQtys.entrySet()) {
				int itemId = entry.getKey().intValue();
				short quantity = entry.getValue().shortValue();
				InventoryType type = InventoryTools.getCategory(entry.getKey().intValue());

				netEmptySlotRemovals.put(type, Integer.valueOf(netEmptySlotRemovals.get(type).intValue() + InventoryTools.slotsNeeded(p.getInventory(type), itemId, quantity, false)));
			}

			for (Map.Entry<InventoryType, Integer> netEmptySlotChange : netEmptySlotRemovals.entrySet()) {
				if (p.getInventory(netEmptySlotChange.getKey()).freeSlots() < netEmptySlotChange.getValue().intValue()) {
					p.getClient().getSession().send(GamePackets.writeInventoryNoChange());
					p.getClient().getSession().send(GamePackets.writeShowInventoryFull());
					return false;
				}
			}
		}
		return true;
	}

	private static int round(double d) {
		return (int) (d + 0.5);
	}

	private static int subtractTax(int mesos) {
		if (mesos < 50000)
			return mesos;
		else if (mesos < 100000)
			return round(mesos * 0.995);
		else if (mesos < 1000000)
			return round(mesos * 0.99);
		else if (mesos < 5000000)
			return round(mesos * 0.98);
		else if (mesos < 10000000)
			return round(mesos * 0.97);
		else
			return round(mesos * 0.96);
	}

	private void giveItemsAndMesos(GameCharacter to, byte from, boolean taxAndShowGain) {
		if (taxAndShowGain)
			to.gainMesos(subtractTax(mesos[from]), false);
		else
			to.setMesos(to.getMesos() + mesos[from]);
		for (InventorySlot item : items[from]) {
			if (item != null) {
				InventoryType type = InventoryTools.getCategory(item.getDataId());
				Inventory inv = to.getInventory(type);
				UpdatedSlots changedSlots = InventoryTools.addToInventory(inv, item, item.getQuantity(), false);
				ClientSession<?> ses = to.getClient().getSession();
				short pos;
				for (Short s : changedSlots.modifiedSlots) {
					pos = s.shortValue();
					ses.send(GamePackets.writeInventoryUpdateSlotQuantity(type, pos, inv.get(pos)));
				}
				for (Short s : changedSlots.addedOrRemovedSlots) {
					pos = s.shortValue();
					ses.send(GamePackets.writeInventoryAddSlot(type, pos, inv.get(pos)));
				}
				to.itemCountChanged(item.getDataId());
				if (taxAndShowGain)
					ses.send(GamePackets.writeShowItemGain(item.getDataId(), item.getQuantity()));
			}
		}
	}

	@Override
	public void closeRoom(GameMap map) {
		GameCharacter v;
		super.closeRoom(map);
		if (!tradeCompleted)
			for (byte i = 0; i < getMaxPlayers(); i++)
				if ((v = getPlayerByPosition(i)) != null)
					giveItemsAndMesos(v, i, false);
	}

	private void performTrade() {
		tradeCompleted = true;
		GameCharacter v;
		GameMap map = null;
		if (!canFitAllItemsAndMesos()) {
			for (byte i = 0; i < getMaxPlayers(); i++) {
				if ((v = getPlayerByPosition(i)) != null) {
					giveItemsAndMesos(v, i, false);
					map = v.getMap();
					v.setMiniRoom(null);
					v.getClient().getSession().send(getFirstPersonLeaveMessage(i, EXIT_TRADE_FAIL));
				}
			}
		} else {
			for (byte i = 0; i < getMaxPlayers(); i++) {
				if ((v = getPlayerByPosition(i)) != null) {
					giveItemsAndMesos(v, (byte) ((i + 1) % 2), true);
					map = v.getMap();
					v.setMiniRoom(null);
					v.getClient().getSession().send(getFirstPersonLeaveMessage(i, EXIT_TRADE_SUCCESS));
				}
			}
		}
		closeRoom(map);
	}

	public void addItem(GameCharacter p, byte slot, InventorySlot item) {
		byte pos = positionOf(p);
		items[pos][slot - 1] = item;
		p.getClient().getSession().send(writeItemAdd((byte) 0, slot, item));
		pos = (byte) ((pos + 1) % 2);
		getPlayerByPosition(pos).getClient().getSession().send(writeItemAdd((byte) 1, slot, item));
	}

	public void addMesos(GameCharacter p, int gain) {
		byte pos = positionOf(p);
		int newMesos = (mesos[pos] += gain);
		p.getClient().getSession().send(writeMesoSet((byte) 0, newMesos));
		pos = (byte) ((pos + 1) % 2);
		getPlayerByPosition(pos).getClient().getSession().send(writeMesoSet((byte) 1, newMesos));
	}

	public void confirmTrade(GameCharacter p) {
		confirmed[positionOf(p)] = true;
		boolean completeTrade = true;
		for (int i = 0; i < getMaxPlayers() && completeTrade; i++)
			if (!confirmed[i])
				completeTrade = false;
		if (completeTrade)
			performTrade();
	}

	@Override
	public MiniroomType getMiniroomType() {
		return MiniroomType.TRADE;
	}

	@Override
	public byte[] getFirstPersonJoinMessage(GameCharacter p) {
		GameCharacter v;

		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter();

		lew.writeShort(ClientSendOps.MINIROOM_ACT);
		lew.writeByte(ACT_JOIN);
		lew.writeByte(getMiniroomType().byteValue());
		lew.writeByte(getMaxPlayers());
		lew.writeByte(positionOf(p));

		for (byte i = 0; i < getMaxPlayers(); i++)
			if ((v = getPlayerByPosition(i)) != null)
				writeMiniroomAvatar(lew, v, i);
		lew.writeByte((byte) 0xFF);

		return lew.getBytes();
	}

	@Override
	public byte[] getThirdPersonJoinMessage(GameCharacter p, byte pos) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter();
		lew.writeShort(ClientSendOps.MINIROOM_ACT);
		lew.writeByte(ACT_VISIT);
		writeMiniroomAvatar(lew, p, pos);
		return lew.getBytes();
	}

	@Override
	public byte[] getShowNewSpawnMessage() {
		throw new UnsupportedOperationException("Trades do not show up on the map");
	}

	@Override
	public byte[] getDestructionMessage() {
		throw new UnsupportedOperationException("Trades do not show up on the map");
	}

	private static byte[] writeItemAdd(byte pos, byte slot, InventorySlot item) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter();
		lew.writeShort(ClientSendOps.MINIROOM_ACT);
		lew.writeByte(ACT_SET_ITEMS);
		lew.writeByte(pos);
		CommonPackets.writeItemInfo(lew, slot, item);
		return lew.getBytes();
	}

	private static byte[] writeMesoSet(byte pos, int add) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(8);
		lew.writeShort(ClientSendOps.MINIROOM_ACT);
		lew.writeByte(ACT_SET_MESO);
		lew.writeByte(pos);
		lew.writeInt(add);
		return lew.getBytes();
	}
}
