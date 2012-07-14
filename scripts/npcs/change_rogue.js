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
 * Thief Job Instructor (NPC 1072003)
 * Victoria Road: Construction Site North of Kerning City (Map 102040000)
 *
 * Admits magicians into the second job advancement challenge.
 *
 * @author GoldenKevin (content from Vana r3171)
 */

if (npc.playerHasItem(4031011, 1) && npc.playerHasItem(4031013, 0)) {
	npc.sayNext("Hmmm...it is definitely the letter from #b#p1052001##k...so you came all the way here to take the test and make the 2nd job advancement as the rogue. Alright, I'll explain the test to you. Don't sweat it much, though; it's not that complicated.");
	npc.sayNext("I'll send you to a hidden map. You'll see monsters not normally seen in normal fields. They look the same like the regular ones, but with a totally different attitude. They neither boost your experience level nor provide you with item.");
	npc.sayNext("You'll be able to acquire a marble called #b#t4031013##k while knocking down those monsters. It is a special marble made out of their sinister, evil minds. Collect 30 of those, then go talk to a colleague of mine in there. That's how you pass the test.");
	let selection = npc.askYesNo("Once you go inside, you can't leave until you take care of your mission. If you die, your experience level will decrease...so you better really buckle up and get ready...well, do you want to go for it now?");
	if (selection == 0) {
		npc.sayNext("You don't seem too prepared for this. Find me when you ARE ready. There are neither portals or stores inside, so you better get 100% ready for it.");
	} else if (selection == 1) {
		npc.sayNext("Alright! I'll let you in! Defeat the monster inside to earn 30 Dark Marble and then talk to my colleague inside; he'll give you #b#t4031012##k as a proof that you've passed the test. Best of luck to you.");
		npc.warpPlayer(108000401);
	}
} else if (npc.playerHasItem(4031011, 1) && npc.playerHasItem(4031013, 1)) {
	let selection = npc.askYesNo("So you've given up in the middle of this before. Don't worry about it, because you can always retake the test. Now...do you want to go back in and try again?");
	if (selection == 0) {
		npc.sayNext("You don't seem too prepared for this. Find me when you ARE ready. There are neither portals or stores inside, so you better get 100% ready for it.");
	} else if (selection == 1) {
		npc.sayNext("Alright! I'll let you in! Sorry to say this, but I have to take away all your marbles beforehand. Defeat the monsters inside, collect 30 Dark Marbles, then strike up a conversation with a colleague of mine inside. He'll give you the #b#t4031013##k, the proof that you've passed the test. Best of luck to you.");
		npc.takeItem(4031013);
		npc.warpPlayer(108000401);
	}
} else if (npc.getPlayerJob() == 400 && npc.getPlayerLevel() >= 30) {
	npc.sayNext("Do you want to be a stronger thief? Let me take care of that for you, then. You look definitely qualified for it. For now, go see #b#p1052001##k of Kerning City first.");
}