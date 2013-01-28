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

package argonms.game.net.external.handler;

import argonms.common.util.input.LittleEndianReader;
import argonms.game.character.GameCharacter;
import argonms.game.net.external.GameClient;
import argonms.game.script.binding.ScriptNpc;
import argonms.game.script.binding.ScriptObjectManipulator;

/**
 *
 * @author GoldenKevin
 */
public class GuildListHandler {
	public static final byte //guild receive op codes
		CREATE = 0x02,
		INVITE = 0x05,
		JOIN = 0x06,
		LEAVE = 0x07,
		EXPEL = 0x08,
		CHANGE_RANK_STRING = 0x0D,
		CHANGE_PLAYER_RANK = 0x0E,
		CHANGE_EMBLEM = 0x0F,
		CHANGE_NOTICE = 0x10
	;

	public static final byte //guild send op codes
		ASK_NAME = 0x01,
		ASK_EMBLEM = 0x11
	;

	public static void handleListModification(LittleEndianReader packet, GameClient gc) {
		GameCharacter p = gc.getPlayer();
		switch (packet.readByte()) {
			case CREATE: {
				String name = packet.readLengthPrefixedString();
				ScriptNpc npc = gc.getNpc();
				if (npc != null)
					ScriptObjectManipulator.guildNameReceived(npc, name);
				break;
			}
			case INVITE:
				break;
			case JOIN:
				break;
			case LEAVE:
				break;
			case EXPEL:
				break;
			case CHANGE_RANK_STRING:
				break;
			case CHANGE_PLAYER_RANK:
				break;
			case CHANGE_EMBLEM: {
				short background = packet.readShort();
				byte backgroundColor = packet.readByte();
				short design = packet.readShort();
				byte designColor = packet.readByte();
				ScriptNpc npc = gc.getNpc();
				if (npc != null)
					ScriptObjectManipulator.guildEmblemReceived(npc, background, backgroundColor, design, designColor);
				break;
			}
			case CHANGE_NOTICE:
				break;
		}
	}

	public static void handleDenyRequest(LittleEndianReader packet, GameClient gc) {
		
	}
}
