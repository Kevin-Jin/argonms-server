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
 * Purin: Crewmember (NPC 1032009)
 * Victoria Road: Before Takeoff <To Orbis> (Map 101000301)
 *
 * Teleports player out of waiting room on the boat to Orbis
 *
 * @author GoldenKevin (content from Vana r3171)
 */

let selection = npc.askYesNo("Do you want to leave the waiting room? You can, but the ticket is NOT refundable. Are you sure you still want to leave this room?");
if (selection == 0) {
	player.changeMap(101000300);
} else if (selection == 1) {
	npc.sayNext("You'll get to your destination in a few. Go ahead and talk to other people, and before you know it, you'll be there already.");
}