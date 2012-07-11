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
 * 
 * Victoria Road: Bowman Instructional School (Map 100000201),
 *   Victoria Road: Magic Library (Map 101000003),
 *   Victoria Road: Warriors' Sanctuary (Map 102000003),
 *   Victoria Road: Thieves' Hideout (Map 103000003)
 *
 * The level 200 players who show up in NPC form in all the first job
 * advancement maps.
 *
 * @author GoldenKevin (content from Vana r3171)
 */

if (npc.getPlayerName() != npc.getNpcName()) {
	npc.say("Hello, I am #b" + npc.getNpcName() + "#k, and I am LEVEL " + npc.getNpcLevel() + ".");
} else {
	/*if (last transform was less than 24 hours ago) {
		//TODO: limit appearance change to once per day
		npc.say("You may transform your other self only once a day. Please try again tomorrow.");
	} else {*/
		let selection = npc.askYesNo("Will you transform your other self's appearance to your current state? You may transform your other self once a day.");
		if (selection == 0) {
			npc.say("It's okay to take your time. Let me know when you are ready.");
		} else if (selection == 1) {
			npc.refreshAppearance();
			npc.say("Your other self has been transformed to resemble your current state. The transformation is available only once a day.");
		}
	//}
}