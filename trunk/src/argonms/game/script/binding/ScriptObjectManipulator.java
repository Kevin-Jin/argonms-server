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

package argonms.game.script.binding;

import argonms.common.util.input.LittleEndianReader;

/**
 * Proxy class that allows classes outside this package to call protected
 * methods in this package. These methods must be protected so that JavaScript
 * code cannot call them, and this class allows those methods to be called by
 * Java code.
 * @author GoldenKevin
 */
public class ScriptObjectManipulator {
	public static void npcResponseReceived(ScriptNpc npc, LittleEndianReader packet) {
		npc.responseReceived(packet);
	}

	public static void guildNameReceived(ScriptNpc npc, String name) {
		npc.guildNameReceived(name);
	}

	public static void guildEmblemReceived(ScriptNpc npc, short background, byte backgroundColor, short design, byte designColor) {
		npc.guildEmblemReceived(background, backgroundColor, design, designColor);
	}
}
