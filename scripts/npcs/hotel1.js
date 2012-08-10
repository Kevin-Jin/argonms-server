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
 * Hotel Receptionist (NPC 1061100)
 * Dungeon: Sleepywood Hotel (Map 105040400)
 *
 * Admits players into Sleepywood saunas.
 *
 * @author GoldenKevin (content from Vana r3171)
 */

npc.sayNext("Welcome. We're the Sleepywood Hotel. Our hotel works hard to serve you the best at all times. If you are tired and worn out from hunting, how about a relaxing stay at our hotel?");
let selection = npc.askMenu("We offer two kinds of rooms for service. Please choose the one of your liking.\r\n#b"
		+ "#L0#Regular sauna(499 mesos per use)#l\r\n"
		+ "#L1#VIP sauna(999 mesos per use)#l");

let price, map;
switch (selection) {
	case 0:
		price = 499;
		map = 105040401;
		selection = npc.askYesNo("You've chosen the regular sauna. Your HP and MP will recover fast and you can even purchase some items there. Are you sure you want to go in?");
		break;
	case 1:
		price = 999;
		map = 105040402;
		selection = npc.askYesNo("You've chosen the VIP sauna. Your HP and MP will recover even faster than that of the regular sauna and you can even find a special item in there. Are you sure you want to go in?");
		break;
}

if (selection == 1) {
	if (player.hasMesos(price)) {
		player.loseMesos(price);
		player.changeMap(map);
	} else {
		npc.say("I'm sorry. It looks like you don't have enough mesos. It will cost you " + price + " mesos to stay at our hotel.");
	}
} else if (selection == 0) {
	npc.say("We offer other kinds of services, too, so please think carefully and then make your decision.");
}