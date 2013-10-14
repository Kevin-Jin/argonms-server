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
 * Spiruna (NPC 2032001)
 * Orbis: Old Man's House (Map 200050001)
 *
 * Refines Dark Crystal Ore at a discount if the player completed The Book of
 * Ancient is Back (Quest 3034).
 *
 * @author GoldenKevin (content from KiniroMS r227)
 */

let COST = 500000;

if (player.isQuestCompleted(3034)) {
	let selection = npc.askYesNo("You've been so much of a help to me... If you have any #t4004004#, I can refine it for you for only #b" + COST + " meso#k each.");
	if (selection == 1) {
		selection = npc.askNumber("Okay, so how many do you want me to make?", 1, 1, 100);
		if (player.hasMesos(selection * COST)) {
			if (player.hasItem(4004004, 10)) {
				player.loseItem(matID, selection * 10);	
				player.loseMesos(selection * COST);
				player.gainItem(4005004, selection);
				npc.say("Use it wisely.");
			} else {
				npc.say("I need that ore to refine the Crystal. No exceptions..");
			}
		} else {
			npc.say("I'm sorry, but I am NOT doing this for free.");
		}
	}
} else {
	npc.say("Go away, I'm trying to meditate.");
}