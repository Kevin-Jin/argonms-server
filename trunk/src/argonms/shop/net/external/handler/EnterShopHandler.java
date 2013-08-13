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

import argonms.common.character.ShopPlayerContinuation;
import argonms.common.net.external.ClientSendOps;
import argonms.common.net.external.CommonPackets;
import argonms.common.net.external.RemoteClient;
import argonms.common.util.HexTool;
import argonms.common.util.TimeTool;
import argonms.common.util.input.LittleEndianReader;
import argonms.common.util.output.LittleEndianByteArrayWriter;
import argonms.shop.ShopServer;
import argonms.shop.character.CashShopStaging;
import argonms.shop.character.ShopCharacter;
import argonms.shop.loading.commodityoverride.CommodityMod;
import argonms.shop.loading.commodityoverride.CommodityOverrideDataLoader;
import argonms.shop.loading.limitedcommodity.LimitedCommodity;
import argonms.shop.loading.limitedcommodity.LimitedCommodityDataLoader;
import argonms.shop.net.external.CashShopPackets;
import argonms.shop.net.external.ShopClient;
import java.util.Calendar;
import java.util.Collection;
import java.util.Map;

/**
 *
 * @author GoldenKevin
 */
public final class EnterShopHandler {
	public static void handlePlayerConnection(LittleEndianReader packet, ShopClient sc) {
		int cid = packet.readInt();
		ShopCharacter player = ShopCharacter.loadPlayer(sc, cid);
		if (player == null)
			return;
		sc.setPlayer(player);
		boolean allowLogin;
		byte state = sc.getOnlineState();
		allowLogin = (state == RemoteClient.STATUS_MIGRATION);
		if (!allowLogin) {
			sc.getSession().close(player.getName() + " tried to double login on shop");
			return;
		}
		sc.updateState(RemoteClient.STATUS_INSHOP);

		ShopServer sserv = ShopServer.getInstance();
		sserv.addPlayer(player);
		ShopPlayerContinuation context = sserv.getServerEntryContext(player);
		if (context == null) {
			sc.getSession().close(player.getName() + " tried to illegally enter cash shop");
			return;
		}

		sc.setChannel(context.getOriginChannel());
		sserv.getCrossServerInterface().sendBuddyLogInNotifications(player);
		if (player.getPartyId() != 0)
			sserv.getCrossServerInterface().sendPartyMemberLogInNotifications(player);
		if (player.getGuildId() != 0)
			sserv.getCrossServerInterface().sendGuildMemberLogInNotifications(player);
		if (context.getChatroomId() != 0)
			sserv.getCrossServerInterface().sendLeaveChatroom(context.getChatroomId(), player);
		player.setReturnContext(context);

		sc.getSession().send(writeGender(player.getGender()));
		if (context.isEnteringCashShop())
			sc.getSession().send(writeEnterCs(player,
					ShopServer.getInstance().getBlockedSerials(),
					CommodityOverrideDataLoader.getInstance().getAllModifications(),
					CashShopStaging.getBestItems(),
					LimitedCommodityDataLoader.getInstance().getAllLimitedCommodities()));
		else
			sc.getSession().send(writeEnterMts(player));
		//sc.getSession().send(ShopPackets.writeEnableCsOrMts());
		if (context.isEnteringCashShop()) {
			sc.getSession().send(CashShopPackets.writeCashShopCurrencyBalance(player));
			sc.getSession().send(CashShopPackets.writeCashItemStagingInventory(player));
			sc.getSession().send(CashShopPackets.writeGiftedCashItems(player));
			sc.getSession().send(CashShopPackets.writePopulateWishList(player));
		} else {
			//sc.getSession().send(MaplePacketCreator.MTSWantedListingOver(0, 0));
			//sc.getSession().send(MaplePacketCreator.showMTSCash(player));

			//Pair<Integer, List<MTSItemInfo>> pagesAndItems = MTSFunctions.getPagesAndItems();
			//sc.getSession().send(MaplePacketCreator.sendMTS(pagesAndItems.getRight(), 1, 0, 0, pagesAndItems.getLeft().intValue()));
			//sc.getSession().send(MaplePacketCreator.TransferInventory(MTSFunctions.getTransfer(player.getId())));
			//sc.getSession().send(MaplePacketCreator.NotYetSoldInv(MTSFunctions.getNotYetSold(player.getId())));
		}

		String serverMessage = ShopServer.getInstance().getNewsTickerMessage();
		if (!serverMessage.isEmpty())
			sc.getSession().send(CashShopPackets.writeNewsTickerMessage(serverMessage));
	}

	private static byte[] writeGender(byte gender) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(3);
		lew.writeShort(ClientSendOps.GENDER);
		lew.writeByte(gender);
		return lew.getBytes();
	}

	private static final byte[] ADDITIONAL_CS_BYTES = HexTool.getByteArrayFromHexString(
		"00 08 00 00 00 37 00 31 00 38 00 31 00 00 00 00 00 18 00 0E 00 0F 00 0C 06 38 02 14 00 08 80 B6 03 67 00 69 00 6E 00 49 00 70 00 00 00 00 00 00 00 06 00 04 00 13 00 0E 06 A8 01 14 00 D8 9F CD 03 33 00 2E 00 33 00 31 00 2E 00 32 00 33 00 35 00 2E 00 32 00 32 00 34 00 00 00 00 00 00 00 00 00 04 00 0A 00 15 01 0C 06 0E 00 00 00 62 00 65 00 67 00 69 00"
	);

	private static byte[] writeEnterCs(ShopCharacter p,
			Collection<Integer> blockedSerials,
			Map<Integer, Map<CommodityMod, Object>> moddedCommodities,
			int[] bestItems,
			Map<Integer, LimitedCommodity> limitedCommodities) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter();
		lew.writeShort(ClientSendOps.CS_OPEN);
		CommonPackets.writeCharData(lew, p);
		lew.writeBool(true);
		lew.writeLengthPrefixedString(p.getClient().getAccountName());

		lew.writeInt(blockedSerials.size());
		for (Integer sn : blockedSerials)
			lew.writeInt(sn.intValue());
		lew.writeShort((short) moddedCommodities.size());
		for (Map.Entry<Integer, Map<CommodityMod, Object>> entry : moddedCommodities.entrySet()) {
			Map<CommodityMod, Object> properties = entry.getValue();
			short flags = 0;
			for (CommodityMod key : properties.keySet())
				flags |= key.shortValue();
			lew.writeInt(entry.getKey().intValue()); //serial number
			lew.writeShort(flags);
			for (Map.Entry<CommodityMod, Object> property : properties.entrySet()) {
				switch (property.getKey()) {
					case ITEM_ID:
						lew.writeInt(((Number) property.getValue()).intValue());
						break;
					case COUNT:
						lew.writeShort(((Number) property.getValue()).shortValue());
						break;
					case PRIORITY:
						lew.writeByte(((Number) property.getValue()).byteValue());
						break;
					case SALE_PRICE:
						lew.writeInt(((Number) property.getValue()).intValue());
						break;
					case ON_SALE:
						lew.writeBool(((Boolean) property.getValue()).booleanValue());
						break;
					case CLASS:
						lew.writeByte(((Number) property.getValue()).byteValue());
						break;
				}
			}
		}

		lew.writeBytes(ADDITIONAL_CS_BYTES); //no clue
		for (int i = 1; i <= 8; i++) {
			for (int j = 0; j <= 1; j++) {
				for (int k = 0; k < 5; k++) {
					lew.writeInt(bestItems[k]);
					lew.writeInt(i);
					lew.writeInt(j);
				}
			}
		}
		lew.writeShort((short) 110); //no clue
		lew.writeShort((short) 73); //no clue
		lew.writeShort((short) 0); //no clue

		lew.writeShort((short) limitedCommodities.size());
		for (Map.Entry<Integer, LimitedCommodity> entry : limitedCommodities.entrySet()) {
			lew.writeInt(entry.getKey().intValue()); //item ID
			LimitedCommodity lc = entry.getValue();
			int remaining = 40;
			for (Number sn : lc.getSerialNumbers()) {
				lew.writeInt(sn.intValue());
				remaining -= 4;
			}
			lew.writeBytes(new byte[remaining]);
			synchronized (lc) {
				lew.writeInt(lc.getRemainingStock() == 0 ? 1 : 0);
				lew.writeInt(lc.getInitialStock());
				lew.writeInt(lc.getRemainingStock());
			}
			lew.writeInt(0x0F);
			lew.writeInt(lc.getBeginDate()); //intdate
			lew.writeInt(lc.getEndDate()); //intdate
			lew.writeInt(lc.getBeginHour()); //24HR format
			lew.writeInt(lc.getEndHour()); //24HR format
			Calendar beginDay = TimeTool.intDateToCalendar(lc.getBeginDate());
			Calendar endDay = TimeTool.intDateToCalendar(lc.getEndDate());
			if (endDay.getTimeInMillis() - beginDay.getTimeInMillis() < 6L * 24 * 60 * 60 * 1000) {
				int[] days = new int[7];
				int beginDayIndex = beginDay.get(Calendar.DAY_OF_WEEK);
				int endDayIndex = endDay.get(Calendar.DAY_OF_WEEK);
				for (int i = beginDayIndex - 1; i != endDayIndex; i = (i + 1) % 7)
					days[i] = 1;
				for (int i = 0; i < 7; i++) //0 = Sunday, 6 = Saturday
					lew.writeInt(days[i]);
			} else {
				for (int i = 0; i < 7; i++)
					lew.writeInt(1);
			}
		}

		lew.writeByte((byte) 0); //no clue
		lew.writeInt(39);
		return lew.getBytes();
	}

	private static final byte[] ADDITIONAL_MTS_BYTES = HexTool.getByteArrayFromHexString(
		"0A 00 00 00 64 00 00 00 18 00 00 00 A8 00 00 00 B0 ED 4E 3C FD 68 C9 01"
	);

	private static byte[] writeEnterMts(ShopCharacter p) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter();
		lew.writeShort(ClientSendOps.MTS_OPEN);
		CommonPackets.writeCharData(lew, p);
		lew.writeLengthPrefixedString(p.getClient().getAccountName());
		lew.writeInt(5000);
		lew.writeBytes(ADDITIONAL_MTS_BYTES);
		return lew.getBytes();
	}

	private EnterShopHandler() {
		//uninstantiable...
	}
}
