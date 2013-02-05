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
 * Shane (NPC 1032003)
 * Victoria Road: Ellinia (Map 101000000)
 *
 * Starts jump quests Sabitrama and the Diet Medicine (Quest 2050) and
 * Sabitrama's Anti-Aging Medicine (Quest 2051).
 *
 * @author GoldenKevin (content from KiniroMS r227)
 */

let level = player.getLevel();
if (level < 25) {
	npc.say("You must be a higher level to enter the Forest of Patience.");
} else {
	let selection = npc.askYesNo("Hi, i'm Shane. I can let you into the Forest of Patience for a small fee. Would you like to enter for #b5000#k mesos?");
	if (selection == 0) {
		npc.say("Alright, see you next time.");
	} else {
		if (!player.hasMesos(5000)) {
			npc.say("Sorry but it doesn't look like you have enough mesos!");
		} else {
			let quest1 = player.isQuestStarted(2050);
			let quest2 = player.isQuestStarted(2051);
			if (quest1 || !quest2 && level < 50)
				player.changeMap(101000100);
			else if (quest2 || !quest1 && level >= 50)
				player.changeMap(101000102);
			player.loseMesos(5000);
		}
	}
}