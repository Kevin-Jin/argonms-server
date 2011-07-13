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

package argonms.shop.net.external;

import argonms.common.net.external.ClientPacketProcessor;
import argonms.common.net.external.ClientRecvOps;
import argonms.common.tools.input.LittleEndianReader;
import argonms.shop.net.external.handler.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author GoldenKevin
 */
public class ClientShopPacketProcessor extends ClientPacketProcessor<ShopClient> {
	private static final Logger LOG = Logger.getLogger(ClientPacketProcessor.class.getName());

	@Override
	public void process(LittleEndianReader reader, ShopClient sc) {
		switch (reader.readShort()) {
			case ClientRecvOps.PLAYER_CONNECTED:
				ShopHandler.handlePlayerConnection(reader, sc);
				break;
			case ClientRecvOps.PONG:
				sc.receivedPong();
				break;
			case ClientRecvOps.CLIENT_ERROR:
				sc.clientError(reader.readLengthPrefixedString());
				break;
			case ClientRecvOps.AES_IV_UPDATE_REQUEST:
				//no-op
				break;
			default:
				LOG.log(Level.FINE, "Received unhandled client packet {0} bytes long:\n{1}", new Object[] { reader.available() + 2, reader });
				break;
		}
	}
}
