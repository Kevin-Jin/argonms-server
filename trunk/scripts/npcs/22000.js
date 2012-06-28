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
 * Shanks (NPC 22000)
 * Maple Road: Southperry (Map 60000)
 *
 * Teleports player from Southperry to Lith Harbor.
 *
 * @author GoldenKevin (content from KiniroMS r227)
 */

function levelFail() {
	npc.say("Let's see... I don't think you are strong enough. You'll have to be at least #bLevel 7#k to go to Victoria Island.");
}

function warp() {
	npc.warpPlayer(104000000);
}

let response = npc.askYesNo("Take this ship and you'll head off to a bigger continent. For #e150 mesos#n, I'll take you to #bVictoria Island#k."
		+ "The thing is, once you leave this place, you can't ever come back. What do you think? Do you want to go to Victoria Island?");
if (response == 1) {
	if (npc.playerHasItem(4031801, 1)) {
		npc.sayNext("Okay, now give me 150 mesos... Hey, what's that? Is that the recommendation letter from Lucas, the chief of Amherst? "
				+ "Hey, you should have told me you had this. I, Shanks, recognize greatness when I see one, and since you have been recommended by Lucas, "
				+ "I see that you have a great, great potential as an adventurer. No way would I charge you for this trip!");
		if (npc.getPlayerLevel() >= 7) {
			npc.sayNext("Since you have the recommendation letter, I won't charge you for this. "
					+ "Alright, buckle up, because we're going to head to Victoria Island right now, and it might get a bit turbulent!!");
			npc.takeItem(4031801, 1);
			warp();
		} else {
			levelFail();
		}
	} else {
		npc.sayNext("Bored of this place? Here... Give me 150 mesos first...");
		if (npc.getPlayerLevel() >= 7) {
			if (npc.playerHasMesos(150)) {
				npc.sayNext("Awesome! #e150 mesos#n accepted! Alright, off to Victoria Island!");
				npc.takeMesos(150);
				warp();
			} else {
				npc.say("What? You're telling me you wanted to go without any money? You're one weirdo...");
			}
		} else {
			levelFail();
		}
	}
} else if (response == 0) {
	npc.say("Hmm... I guess you still have things to do here?");
}