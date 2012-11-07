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

package argonms.center.net.remoteadmin;

/**
 *
 * @author GoldenKevin
 */
public class TelnetCommandProcessor {
	public void process(String message, TelnetClient client) {
		if (message.equals("exit") || message.equals("quit")) {
			client.getSession().close("User typed '" + message + "'");
			return;
		} else if (message.equals("help")) {
			client.getSession().send("EXIT\t\tCloses the current telnet session.\r\n"
					+ "HELP\t\tDisplays this message.\r\n"
					+ "\r\n");
		} else if (!message.trim().isEmpty()) {
			client.getSession().send('\'' + message.trim().split(" ")[0] + "\' is not recognized as a command. Type 'HELP' for a list of accepted commands.\r\n\r\n");
		}
	}
}
