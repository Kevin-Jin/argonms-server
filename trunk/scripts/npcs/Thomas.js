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
 * Thomas Swift: Amoria Ambassador (NPC 9201022)
 * Victoria Road: Henesys (Map 100000000),
 *   Amoria: Amoria (Map 680000000)
 *
 * Teleports players from Henesys to Amoria, and back.
 *
 * @author GoldenKevin (content from Vana r3171)
 */

if (map.getId() == 100000000) {
	let selection = npc.askYesNo("I can take you to Amoria Village. Are you ready to go?");
	if (selection == 1) {
		npc.sayNext("I hope you had a great time! See you around!");
		player.changeMap(680000000);
	} else if (selection == 0) {
		npc.say("Ok, feel free to hang around until you're ready to go!");
	}
} else if (map.getId() == 680000000) {
	let selection = npc.askYesNo("I can take you back to your original location. Are you ready to go?");
	if (selection == 1) {
		npc.sayNext("I hope you had a great time! See you around!");
		player.changeMap(100000000);
	} else if (selection == 0) {
		npc.sayNext("Ok, feel free to hang around until you're ready to go!");
	}
}