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

package argonms.login.net.external;

import argonms.common.net.external.ClientPacketProcessor;
import argonms.common.net.external.ClientRecvOps;
import argonms.common.util.input.LittleEndianReader;
import argonms.login.net.external.handler.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author GoldenKevin
 */
public class ClientLoginPacketProcessor extends ClientPacketProcessor<LoginClient> {
	private static final Logger LOG = Logger.getLogger(ClientPacketProcessor.class.getName());

	@Override
	public void process(LittleEndianReader reader, LoginClient lc) {
		switch (reader.readShort()) {
			case ClientRecvOps.LOGIN_PASSWORD:
				AuthHandler.handleLogin(reader, lc);
				break;
			case ClientRecvOps.SERVERLIST_REREQUEST:
				WorldlistHandler.handleWorldListRequest(reader, lc);
				break;
			case ClientRecvOps.CHARLIST_REQ:
				WorldlistHandler.handleCharlist(reader, lc);
				break;
			case ClientRecvOps.REQ_SERVERLOAD:
				WorldlistHandler.sendServerStatus(reader, lc);
				break;
			case ClientRecvOps.SET_GENDER:
				AuthHandler.handleGender(reader, lc);
				break;
			case ClientRecvOps.PIN_OPERATION:
				AuthHandler.handlePin(reader, lc);
				break;
			case ClientRecvOps.REGISTER_PIN:
				AuthHandler.handlePinRegister(reader, lc);
				break;
			case ClientRecvOps.SERVERLIST_REQUEST:
				WorldlistHandler.handleWorldListRequest(reader, lc);
				break;
			case ClientRecvOps.EXIT_CHARLIST:
				//I guess if we're loading a character right now, we cancel it?
				break;
			case ClientRecvOps.VIEW_ALL_CHARS:
				WorldlistHandler.handleViewAllChars(reader, lc);
				break;
			case ClientRecvOps.PICK_ALL_CHAR:
				WorldlistHandler.handlePickFromAllChars(reader, lc);
				break;
			case ClientRecvOps.ENTER_EXIT_VIEW_ALL:
				//I guess if we're loading a character right now, we cancel it?
				break;
			case ClientRecvOps.CHAR_SELECT:
				WorldlistHandler.handlePickFromWorldCharlist(reader, lc);
				break;
			case ClientRecvOps.CHECK_CHAR_NAME:
				WorldlistHandler.handleNameCheck(reader, lc);
				break;
			case ClientRecvOps.CREATE_CHAR:
				WorldlistHandler.handleCreateCharacter(reader, lc);
				break;
			case ClientRecvOps.DELETE_CHAR:
				WorldlistHandler.handleDeleteChar(reader, lc);
				break;
			case ClientRecvOps.PONG:
				lc.getSession().receivedPong();
				break;
			case ClientRecvOps.CLIENT_ERROR:
				lc.clientError(reader.readLengthPrefixedString());
			case ClientRecvOps.AES_IV_UPDATE_REQUEST:
				//no-op
				break;
			case ClientRecvOps.RELOG:
				WorldlistHandler.backToLogin(reader, lc);
				break;
			case ClientRecvOps.PLAYER_UPDATE:
				//no-op
				break;
			default:
				LOG.log(Level.FINE, "Received unhandled client packet {0} bytes long:\n{1}", new Object[] { reader.available() + 2, reader });
				break;
		}
	}
}
