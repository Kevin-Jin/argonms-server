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

package argonms.game.field.entity;

import argonms.game.character.inventory.InventorySlot;
import argonms.game.character.GameCharacter;
import argonms.game.field.GameMap;
import argonms.common.net.external.ClientSendOps;
import argonms.common.net.external.CommonPackets;
import argonms.common.tools.Scheduler;
import argonms.common.tools.output.LittleEndianByteArrayWriter;
import argonms.common.tools.output.LittleEndianWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledFuture;

/**
 *
 * @author GoldenKevin
 */
public abstract class FreeMarketShop extends Miniroom {
	protected static class ShopItem {
		public short bundles;
		public int price;
		public InventorySlot item;
	}

	protected final List<ShopItem> items;
	private final List<String> bannedPlayers;

	public FreeMarketShop(GameCharacter owner, String desc, byte type) {
		super(owner, 4, desc, null, type);
		items = new ArrayList<ShopItem>();
		bannedPlayers = new ArrayList<String>();
	}

	public void banVisitor(String name) {
		bannedPlayers.add(name.toLowerCase());
		GameCharacter v;
		for (byte i = 1; i < getMaxPlayers(); i++) {
			if ((v = getPlayerByPosition(i)) != null && v.getName().equalsIgnoreCase(name)) {
				banVisitor(i);
				break;
			}
		}
	}

	public boolean isPlayerBanned(GameCharacter p) {
		return bannedPlayers.contains(p.getName().toLowerCase());
	}

	protected abstract byte[] getSlotUpdateMessage();

	public byte[] getThirdPersonJoinMessage(GameCharacter p, byte pos) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter();
		lew.writeShort(ClientSendOps.MINIROOM_ACT);
		lew.writeByte(ACT_VISIT);
		writeMiniroomAvatar(lew, p, pos);
		return lew.getBytes();
	}

	public static class PlayerStore extends FreeMarketShop {
		private static final int TYPE_OFFSET = 5140000; //first itemid

		public PlayerStore(GameCharacter owner, String desc, int itemId) {
			super(owner, desc, (byte) (itemId - TYPE_OFFSET));
			openToMap = true;
		}

		public MiniroomType getMiniroomType() {
			return MiniroomType.PLAYER_SHOP;
		}

		protected byte[] getSlotUpdateMessage() {
			LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter();

			lew.writeShort(ClientSendOps.MINIROOM_ACT);
			lew.writeByte(ACT_SHOP_ITEM_UPDATE);
			writeShopItems(lew, items);

			return lew.getBytes();
		}

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

			lew.writeLengthPrefixedString(getMessage());
			lew.writeByte((byte) 0x10);
			writeShopItems(lew, items);

			return lew.getBytes();
		}
	}

	public static class HiredMerchant extends FreeMarketShop {
		private static final int TYPE_OFFSET = 5030000; //first itemid
		private final String ownerName;
		private final int ownerId;
		private final ScheduledFuture<?> expireSchedule;

		public HiredMerchant(GameCharacter owner, String desc, int itemId) {
			super(null, desc, (byte) (itemId - TYPE_OFFSET));
			openToMap = false;
			ownerName = owner.getName();
			ownerId = owner.getId();
			final GameMap map = owner.getMap();
			expireSchedule = Scheduler.getInstance().runAfterDelay(new Runnable() {
				public void run() {
					closeRoom(map);
				}
			}, 1000 * 60 * 60 * 24);
		}

		public void closeRoom(GameMap map) {
			super.closeRoom(map);
			expireSchedule.cancel(false);
		}

		public MiniroomType getMiniroomType() {
			return MiniroomType.HIRED_MERCHANT;
		}

		protected byte[] getSlotUpdateMessage() {
			LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter();

			lew.writeShort(ClientSendOps.MINIROOM_ACT);
			lew.writeByte(ACT_SHOP_ITEM_UPDATE);
			lew.writeInt(0);
			lew.writeByte((byte) items.size());
			for (ShopItem item : items) {
				lew.writeShort(item.bundles);
				lew.writeShort(item.item.getQuantity());
				lew.writeInt(item.price);
				CommonPackets.writeItemInfo(lew, (byte) 0, item.item, true, true, false);
			}

			return lew.getBytes();
		}

		public byte[] getFirstPersonJoinMessage(GameCharacter p) {
			return getFirstPersonJoinMessage(p, false);
		}

		public byte[] getFirstPersonJoinMessage(GameCharacter p, boolean justCreated) {
			GameCharacter v;

			LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter();

			lew.writeShort(ClientSendOps.MINIROOM_ACT);
			lew.writeByte(ACT_JOIN);
			lew.writeByte(getMiniroomType().byteValue());
			lew.writeByte(getMaxPlayers());
			lew.writeByte(positionOf(p));
			lew.writeByte((byte) 0);
			lew.writeInt(getStyle() + TYPE_OFFSET);
			lew.writeLengthPrefixedString("Hired Merchant"); //err, what?

			for (byte i = 0; i < getMaxPlayers(); i++)
				if ((v = getPlayerByPosition(i)) != null)
					writeMiniroomAvatar(lew, v, i);
			lew.writeByte((byte) 0xFF);

			lew.writeShort((short) 0);
			lew.writeLengthPrefixedString(ownerName);
			if (p.getName().equals(ownerName)) {
				lew.writeInt(Integer.MAX_VALUE); //timing
				lew.writeBool(justCreated);
				lew.writeInt(0);
				lew.writeByte((byte) 0);
			}

			lew.writeLengthPrefixedString(getMessage());
			lew.writeByte((byte) 0x10);
			lew.writeInt(0);
			if (!items.isEmpty()) {
				writeShopItems(lew, items);
			} else {
				lew.writeByte((byte) 0);
				lew.writeByte((byte) 0);
			}

			return lew.getBytes();
		}

		public byte[] getUpdateBalloonMessage() {
			LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter();

			lew.writeShort(ClientSendOps.HIRED_MERCHANT_BALLOON);
			lew.writeInt(ownerId);
			CommonPackets.writeMiniroomBalloon(lew, this);

			return lew.getBytes();
		}

		public byte[] getShowNewSpawnMessage() {
			LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter();
			lew.writeShort(ClientSendOps.SHOW_HIRED_MERCHANT);
			lew.writeInt(ownerId);
			lew.writeInt(getStyle() + TYPE_OFFSET);
			lew.writePos(getPosition());
			lew.writeShort(getFoothold());
			lew.writeLengthPrefixedString(ownerName);
			lew.writeByte((byte) 5);
			lew.writeInt(getId());
			lew.writeLengthPrefixedString(getMessage());
			lew.writeByte(getStyle());
			lew.writeByte((byte) 1);
			lew.writeByte((byte) 4);
			return lew.getBytes();
		}

		public byte[] getDestructionMessage() {
			LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(6);
			lew.writeShort(ClientSendOps.REMOVE_HIRED_MERCHANT);
			lew.writeInt(getId());
			return lew.getBytes();
		}
	}

	protected static void writeShopItems(LittleEndianWriter lew, List<ShopItem> items) {
		lew.writeByte((byte) items.size());
		for (ShopItem item : items) {
			lew.writeShort(item.bundles);
			lew.writeShort(item.item.getQuantity());
			lew.writeInt(item.price);
			CommonPackets.writeItemInfo(lew, (byte) 0, item.item, true, true, false);
		}
	}
}
