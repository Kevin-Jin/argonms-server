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

package argonms.game.command;

import argonms.common.net.external.ClientSession;
import argonms.game.net.external.GameClient;
import argonms.game.net.external.GamePackets;
import argonms.game.net.external.handler.GameChatHandler.TextStyle;

/**
 *
 * @author GoldenKevin
 */
public class ClientNoticeStream {
	private ClientSession<?> ses;

	public ClientNoticeStream(GameClient rc) {
		ses = rc.getSession();
	}

	public void printOut(String message) {
		ses.send(GamePackets.writeServerMessage(TextStyle.LIGHT_BLUE_TEXT_CLEAR_BG.byteValue(), message, (byte) -1, true));
	}

	public void printErr(String message) {
		ses.send(GamePackets.writeServerMessage(TextStyle.RED_TEXT_CLEAR_BG.byteValue(), message, (byte) -1, true));
	}
}
