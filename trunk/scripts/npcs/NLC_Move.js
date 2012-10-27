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
 * NLC ticket gate (NPC 9201068)
 * New Leaf City Town Street: NLC Subway Station (Map 600010001)
 *
 * Admits players into New Leaf City subway.
 * Teleports players to waiting room of subway to Kerning City.
 *
 * @author GoldenKevin (content from Vana r3171)
 */

let str = "Here's the ticket reader. ";
let a = player.hasItem(4031713, 1);
let b = player.hasItem(4031712, 1);
if (a || b) {
	str += "You will be brought in immediately. Which ticket would you like to use?\r\n";
	if (a)
		str += "#b#L0#New Leaf city (Normal)#l#k\r\n";
	else if (b)
		str += "#b#L0#New Leaf city (Basic)#l#k\r\n";
	let selection = npc.askMenu(str);

	let maps = [600010002];
	let items = [a ? 4031712 : 4031713];
	let go = true;
	if (selection == 0) {
		let event = npc.getEvent("ship_nlc");
		if (event == null) {
			npc.say("Sorry, it looks like there's a problem with the subway to Kerning City.");
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