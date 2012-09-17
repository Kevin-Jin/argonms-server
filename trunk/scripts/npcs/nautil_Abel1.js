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

/**
 * Bush (NPC 1094002)
 * Victoria Road: Nautilus Harbor (Map 120000000)
 *
 * Advances Help Me Find My Glasses (Quest 2186).
 *
 * @author GoldenKevin (content from KiniroMS r227)
 */

if (player.isQuestStarted(2186)) {
	let item;
	switch (Math.floor(Math.random() * 2)) {
		case 0:
			if (player.hasItem(4031853, 0))
				item = 4031853;
			else
				item = 4031855;
			break;
		case 1:
			item = 4031854;
			break;
	}
	player.gainItem(item, 1);

	if (item == 4031853)
		npc.sayNext("I found Abel's glasses.");
	else
		npc.say("I found a pair of glasses, but it doesn't seem to be Abel's. Abel's pair is horn-rimmed...");
}