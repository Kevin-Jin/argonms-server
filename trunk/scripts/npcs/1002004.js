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
 * VIP Cab (NPC 1002004)
 * Victoria Road: Lith Harbor (Map 104000000)
 *
 * Teleports player from Lith Harbor to Ant Tunnel Park.
 *
 * @author GoldenKevin (content from KiniroMS r227)
 */

npc.sayNext("Hi there! This cab is for VIP customers only. Instead of just taking you to different towns like the regular cabs, we offer a much better service worthy of VIP class. It's a bit pricey, but... for only 10,000 mesos, we'll take you safely to the #bant tunnel#k.");
let prompt, price;
if (npc.getPlayerJob() == 0) {
	prompt = "We have a special 90% discount for beginners. The ant tunnel is located deep inside in the dungeon that's placed at the center of the Victoria Island, where #p1061001# is. Would you like to go there for #b1,000 mesos#k?";
	price = 1000;
} else {
	prompt = "The regular fee applies for all non-beginners. The ant tunnel is located deep inside in the dungeon that's placed at the center of the Victoria Island, where #p1061001# is. Would you like to go there for #b10,000 mesos#k?.";
	price = 10000;
}
let selection = npc.askYesNo(prompt);
if (selection == 1) {
	if (npc.playerHasMesos(price)) {
		npc.takeMesos(price);
		npc.warpPlayer(105070001);
	} else {
		npc.sayNext("It looks like you don't have enough mesos. Sorry but you won't be able to use this without it.");
	}
} else {
	npc.sayNext("This town also has a lot to offer. Find us if and when you feel the need to go to the #m105070001#.");
}