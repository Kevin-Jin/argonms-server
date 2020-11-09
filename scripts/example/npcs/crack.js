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

/**
 * Door of Dimension (NPC 1061009)
 * Hidden Street: The Forest of Evil II (Map 100040106),
 *   Dungeon: Sleepy Dungeon V (Map 105040305),
 *   Dungeon: Ant Tunnel Park (Map 105070001),
 *   Dungeon: The Cave of Evil Eye II (Map 105070200),
 *   Hidden Street: Monkey Swamp II (Map 107000402)
 *
 * Third job advancement challenge NPC.
 *
 * @author GoldenKevin
 */

let map1 = null;
if (player.isQuestStarted(7501)) {
	switch (player.getJob()) {
		case 110:
		case 120:
		case 130:
			map1 = 108010300;
			break;
		case 210:
		case 220:
		case 230:
			map1 = 108010200;
			break;
		case 310:
		case 320:
			map1 = 108010100;
			break;
		case 410:
		case 420:
			map1 = 108010400;
			break;
		case 510:
		case 520:
			map1 = 108010500;
			break;
	}
}

if (map1 == null) {
	npc.sayNext("It seems that there is a door that will take you to another dimension, but you can't go in for some reason.");
} else {
	if (npc.makeEvent("cloneFight", true, [player, map1, map1 + 1]) == null)
		npc.sayNext("Somebody is already fighting the clone. Try again later.");
}