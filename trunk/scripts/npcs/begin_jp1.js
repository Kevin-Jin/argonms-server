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
 * Sera (NPC 2100)
 * Maple Road: Entrance - Mushroom Town Training Camp (Map 0),
 *   Maple Road: Upper level of the Training Camp (Map 1)
 *
 * Greets newly created players at the entrance of the Mushroom Town Training
 * Camp, and gives a player some help in Upper Level of the Training Camp.
 *
 * @author GoldenKevin (content from Vana r3171)
 */

let map = map.getId();
if (map == 0 || map == 3) {
	let enterCamp = npc.askYesNo("Welcome to the world of MapleStory. The purpose of this training camp is to help beginners. Would you like to enter this training camp? Some people start their journey without taking the training program. But I strongly recommend you take the training program first.");
	if (enterCamp == 1) {
		npc.sayNext("Ok then, I will let you enter the training camp. Please follow your instructor's lead.");
		player.changeMap(1);
	} else {
		let confirm = npc.askYesNo("Do you really wanted to start your journey right away?");
		if (confirm) {
			npc.sayNext("It seems like you want to start your journey without taking the training program. Then, I will let you move on the training ground. Be careful~");
			player.changeMap(40000);
		} else {
			npc.sayNext("Please talk to me again when you finally made your decision.");
		}
	}
} else if (map == 1) {
	npc.sayNext("This is the image room where your first training program begins. In this room, you will have an advance look into the job of your choice.");
	npc.say("Once you train hard enough, you will be entitled to occupy a job. You can become a Bowman in Henesys, a Magician in Ellinia, a Warrior in Perion, and a Thief in Kerning City..");
}