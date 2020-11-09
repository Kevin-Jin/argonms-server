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
 * NLC Taxi (NPC 9201056)
 * New Leaf City Town Street: New Leaf City - Town Center (Map 600000000),
 *   Phantom Forest: Haunted House (Map 682000000)
 *
 * Taxi NPC for Masteria. Teleports players from New Leaf City - Town Center to
 * the Haunted House in Phantom Forest.
 *
 * @author GoldenKevin (content from Vana r3171)
 */

switch (map.getId()) {
	case 600000000: {
		npc.sayNext("Hey, there. Want to take a trip deeper into the Masterian wilderness? A lot of this continent is still quite unknown and untamed... so there's still not much in the way of roads. Good thing we've got this baby... We can go offroading, and in style too!");
		npc.sayNext("Right now, I can drive you to the #bPhantom Forest#k. The old #bPrendergast Mansion#k is located there. Some people say the place is haunted!");
		let selection = npc.askYesNo("What do you say...? Want to head over there?");

		if (selection == 1) {
			npc.sayNext("Alright! Buckle your seat belt, and let's head to the Mansion!\r\nIt's going to get bumpy!");
			player.changeMap(682000000);
		} else if (selection == 0) {
			npc.say("Really? I don't blame you... Sounds like a pretty scary place to me too! If you change your mind, I'll be right here.");
		}
		break;
	}
	case 682000000: {
		let selection = npc.askYesNo("Hey, there. Hope you had fun here! Ready to head back to #bNew Leaf City#k?");

		if (selection == 1) {
			npc.sayNext("Back to civilization it is! Hop in and get comfortable back there... We'll have you back to the city in a jiffy!");
			player.changeMap(600000000);
		} else if (selection == 0) {
			npc.say("Oh, you want to stay and look around some more? That's understandable. If you wish to go back to #bNew Leaf City#k, you know who to talk to!");
		}
		break;
	}
}