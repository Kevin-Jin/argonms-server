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

package argonms.shop.net.external.handler;

import argonms.common.net.external.RemoteClient;
import argonms.common.tools.input.LittleEndianReader;
import argonms.shop.ShopServer;
import argonms.shop.character.ShopCharacter;
import argonms.shop.net.external.ShopClient;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author GoldenKevin
 */
public class ShopHandler {
	private static final Logger LOG = Logger.getLogger(ShopHandler.class.getName());

	public static void handlePlayerConnection(LittleEndianReader packet, ShopClient sc) {
		int cid = packet.readInt();
		/*Pair<Byte, Boolean> info = ShopServer.getInstance().getChannelInfo(cid);
		client.setChannel(info.getLeft().byteValue());
		boolean mts = info.getRight().booleanValue();*/
		ShopCharacter player = null;
		player = ShopCharacter.loadPlayer(sc, cid);
		if (player == null)
			return;
		sc.setPlayer(player);
		boolean allowLogin;
		byte state = sc.getOnlineState();
		allowLogin = (state == RemoteClient.STATUS_MIGRATION);
		if (!allowLogin) {
			LOG.log(Level.WARNING, "Player {0} tried to double login on shop",
					player.getName());
			sc.getSession().close();
			return;
		}
		sc.updateState(RemoteClient.STATUS_INSHOP);

		ShopServer.getInstance().addPlayer(player);
		/*try {
			ShopServer.getInstance().getCSInterface().loggedOn(c.getPlayer().getName(), cid, c.getWorld(), c.getPlayer().getBuddylist().getBuddyIds());
		} catch (RemoteException e) {
			LOG.error("Could not update buddies.", e);
		}
		c.getSession().write(MaplePacketCreator.updateGender(c.getPlayer()));
		if (mts)
			c.getSession().write(MaplePacketCreator.writeEnterMts(c.getPlayer()));
		else
			c.getSession().write(MaplePacketCreator.writeEnterCs(c.getPlayer()));
		c.getSession().write(MaplePacketCreator.enableCSorMTS());
		if (mts) {
			c.getSession().write(MaplePacketCreator.MTSWantedListingOver(0, 0));
			c.getSession().write(MaplePacketCreator.showMTSCash(c.getPlayer()));

			Pair<Integer, List<MTSItemInfo>> pagesAndItems = MTSFunctions.getPagesAndItems();
			c.getSession().write(MaplePacketCreator.sendMTS(pagesAndItems.getRight(), 1, 0, 0, pagesAndItems.getLeft().intValue()));
			c.getSession().write(MaplePacketCreator.TransferInventory(MTSFunctions.getTransfer(c.getPlayer().getId())));
			c.getSession().write(MaplePacketCreator.NotYetSoldInv(MTSFunctions.getNotYetSold(c.getPlayer().getId())));
		} else {
			c.getSession().write(MaplePacketCreator.showNXMapleTokens(c.getPlayer()));
			c.getSession().write(MaplePacketCreator.getCSInventory(c.getPlayer()));
			c.getSession().write(MaplePacketCreator.getCSGifts(c.getPlayer()));
			c.getSession().write(MaplePacketCreator.enableCSUse3());
			c.getSession().write(MaplePacketCreator.sendWishList(c.getPlayer().getId(), false));
		}*/
	}
}
