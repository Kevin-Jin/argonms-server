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

import argonms.common.net.external.CheatTracker;
import argonms.common.util.input.LittleEndianReader;
import argonms.shop.ShopServer;
import argonms.shop.net.external.ShopClient;

/**
 *
 * @author GoldenKevin
 */
public class CashShopHandler {
	public static void handleReturnToChannel(LittleEndianReader packet, ShopClient sc) {
		if (packet.available() != 0) {
			CheatTracker.get(sc).suspicious(CheatTracker.Infraction.POSSIBLE_PACKET_EDITING, "Changing map in cash shop");
			return;
		}
		
		ShopServer.getInstance().requestChannelChange(sc.getPlayer(), sc.getChannel());
	}
}
