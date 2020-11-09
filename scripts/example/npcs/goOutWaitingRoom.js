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
 * Purin: Crewmember (NPC 1032009),
 *   Erin: Crewmember (NPC 2012002),
 *   Pelace: Crewmember (NPC 2012022),
 *   Egnet: Crew (NPC 2012024),
 *   Rosey: Crewmember (NPC 2041001),
 *   Harry: Crewmember (NPC 2082002),
 *   Slyn: Crew (NPC 2102001)
 * Victoria Road: Before Takeoff <To Orbis> (Map 101000301),
 *   Orbis: Before Takeoff <To Ellinia> (Map 200000112),
 *   Orbis: Cabin <To Leafre> (Map 200000132),
 *   Orbis: Station <To Ariant> (Map 200000152),
 *   Orbis: Before the Departure <Ludibrium> (Map 200000122),
 *   Ludibrium: Before the Departure <Orbis> (Map 220000111),
 *   Leafre: Before Takeoff <To Orbis> (Map 240000111),
 *   Ariant: Before Takeoff <To Orbis> (Map 260000110)
 *
 * Teleports player out of waiting rooms for transportation between continents
 * back to stations.
 *
 * @author GoldenKevin (content from Vana r3171)
 */

let selection = npc.askYesNo("Do you want to leave the waiting room? You can, but the ticket is NOT refundable. Are you sure you still want to leave this room?");
if (selection == 1) {
	let toMap;
	switch (map.getId()) {
		case 101000301:
			toMap = 101000300;
			break;
		case 200000112:
			toMap = 200000100;
			break;
		case 200000122:
			toMap = 200000100;
			break;
		case 200000132:
			toMap = 200000100;
			break;
		case 200000152:
			toMap = 200000100;
			break;
		case 220000111:
			toMap = 220000100;
			break;
		case 240000111:
			toMap = 240000100;
			break;
		case 260000110:
			toMap = 260000100;
			break;
	}
	player.changeMap(toMap);
} else if (selection == 0) {
	npc.sayNext("You'll get to your destination in a few. Go ahead and talk to other people, and before you know it, you'll be there already.");
}