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
 * The Ticket Gate (NPC 1052007)
 * Victoria Road: Subway Ticketing Booth (Map 103000100)
 *
 * Admits players into Kerning City subway.
 * Starts jump quests Shumi's Lost Coin (Quest 2055), Shumi's Lost Bundle of
 * Money (Quest 2056), and Shumi's Lost Bundle of Money (Quest 2057)
 * Teleports players to waiting room of subway to New Leaf City.
 *
 * @author GoldenKevin (content from Vana r3171)
 */

let str = "Here's the ticket reader. ";
let a = player.hasItem(4031036, 1);
let b = player.hasItem(4031037, 1);
let c = player.hasItem(4031038, 1);
let d = player.hasItem(4031711, 1);
let e = player.hasItem(4031710, 1);
if (a || b || c || d || e) {
	str += "You will be brought in immediately. Which ticket would you like to use?\r\n";
	if (a)
		str += "#b#L0#Construction site B1#l#k\r\n";
	if (b)
		str += "#b#L1#Construction site B2#l#k\r\n";
	if (c)
		str += "#b#L2#Construction site B3#l#k\r\n";
	if (d)
		str += "#b#L3#New Leaf city (Normal)#l#k\r\n";
	else if (e)
		str += "#b#L3#New Leaf city (Basic)#l#k\r\n";
	let selection = npc.askMenu(str);

	let maps = [103000900, 103000903, 103000906, 600010004];
	let items = [4031036, 4031037, 4031038, d ? 4031711 : 4031710];
	let go = true;
	if (selection == 3) {
		let event = npc.getEvent("ship_nlc");
		if (event == null) {
			npc.say("Sorry, it looks like there's a problem with the subway to New Leaf City.");
			go = false;
		} else if (event.getVariable("board")) {
			if (npc.askYesNo("It looks like there's plenty of room for this ride. Please have your ticket ready so I can let you in. The ride will be long, but you'll get to your destination just fine. What do you think? Do you want to get on this ride?") == 0) {
				npc.sayNext("You must have some business to take care of here, right?");
				go = false;
			}
		} else if (event.getVariable("docked")) {
			npc.sayNext("This subway is getting ready for takeoff. I'm sorry, but you'll have to get on the next ride. The ride schedule is available through the usher at the ticketing booth.");
			go = false;
		} else {
			npc.sayNext("We will begin boarding 5 minutes before the takeoff. Please be patient and wait for a few minutes. Be aware that the subway will take off right on time, and we stop receiving tickets 1 minute before that, so please make sure to be here on time.");
		}
	}
	if (go) {
		player.loseItem(items[selection], 1);
		player.changeMap(maps[selection]);
	}
} else {
	npc.say(str + "You are not allowed in without the ticket.");
}