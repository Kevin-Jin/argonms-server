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
 * Cherry: Ticketing Usher (NPC 1032008),
 *   Rini: Ticketing Usher (NPC 2012001),
 *   Sunny: Ticketing Usher (NPC 2012013),
 *   Ramini: Crewmember (NPC 2012021),
 *   Geras: Crew (NPC 2012025),
 *   Tian: Ticketing Usher (NPC 2041000),
 *   Tommie: Crewmember (NPC 2082001),
 *   Asesson: Crew (NPC 2102000)
 * Victoria Road: Ellinia Station (Map 101000300),
 *   Orbis: Station<To Ellinia> (Map 200000111),
 *   Orbis: Station<Ludibrium> (Map 200000121),
 *   Orbis: Cabin <To Leafre> (Map 200000131),
 *   Orbis: Station <To Ariant> (Map 200000151),
 *   Ludibrium: Station<Orbis> (Map 220000110),
 *   Leafre: Cabin <To Orbis> (Map 240000110),
 *   Ariant: Ariant Station Platform (Map 260000100)
 *
 * Teleports player from stations to waiting rooms for transportation between
 * continents.
 *
 * @author GoldenKevin (content from KiniroMS r227)
 */

let event;
switch (map.getId()) {
	case 101000300:
		event = npc.getEvent("ship_ossyria");
		if (event == null) {
			npc.say("Sorry, it looks like there's a problem with the boat to Orbis.");
		} else if (event.getVariable("board")) {
			let selection = npc.askYesNo("This will not be a short flight, so if you need to take care of some things, I suggest you do that first before getting on board. Do you still wish to board the ship?");
			if (selection == 0) {
				npc.sayNext("You must have some business to take care of here, right?");
			} else {
				if (!player.hasItem(4031045, 1)) {
					npc.sayNext("Oh no ... I don't think you have the ticket with you. I can't let you in without it. Please buy the ticket at the ticketing booth.");
				} else {
					player.loseItem(4031045, 1);
					player.changeMap(101000301);
				}
			}
		} else if (event.getVariable("0docked")) {
			npc.sayNext("The ship is getting ready for takeoff. I'm sorry, but you'll have to get on the next ride. The ride schedule is available through the usher at the ticketing booth.");
		} else {
			npc.sayNext("We will begin boarding 5 minutes before the takeoff. Please be patient and wait for a few minutes. Be aware that the ship will take off on time, and we stop receiving tickets 1 minute before that, so please make sure to be here on time.");
		}
		break;
}