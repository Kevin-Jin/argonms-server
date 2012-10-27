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
 * ? (NPC 9102100)
 * Victoria Road: Pet-Walking Road (Map 100000202)
 *
 * Advances the Pet Instructor Test (Quest 4646) by giving the hidden item.
 *
 * @author GoldenKevin (content from KiniroMS r227)
 */

if (!player.isQuestStarted(4646)) {
	npc.say("#b(I couldn't find anything.)");
} else if (player.isQuestActive(4646)) {
	let selection = npc.askYesNo("#b(I can see something covered in grass. Should I pull it out?)");
	if (selection == 0) {
		npc.sayNext("#b(I didn't touch this hidden item covered in grass)");
	} else {
		if (player.gainItem(4031921, 1))
			npc.sayNext("I found the item that Pet Trainer Bartos hid... this note.");
		else //TODO: GMS-like line
			npc.say("Please check whether your ETC. inventory is full.");
	}
} else {
	npc.sayNext("#b(What's this... eww... a pet's poop was in there!)");
}