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
 * Dr. Rhomes: Lens Wizard (NPC 9200101)
 * Orbis Park: Orbis Plastic Surgery (Map 200000201)
 *
 * Orbis face changer - eye color only.
 *
 * @author GoldenKevin (content from KiniroMS r227)
 */

function getRandomColor() {
	let array = npc.getAllEyeColors();
	return array[Math.floor(Math.random() * array.length)];
}

let selection = npc.askMenu("Hello, I'm Dr. Rhomes, head of the cosmetic lens department here at the Orbis Plastic Surgery Shop.\r\nMy goal here is to add personality to everyone's eyes through the wonders of cosmetic lenses, and with #b#t5152011##k or #b#t5152014##k, I can do the same for you, too! Now, what would you like to use?\r\n#b"
		+ "#L0#Cosmetic Lenses: #i5152011##t5152011##l\r\n"
		+ "#L1#Cosmetic Lenses: #i5152014##t5152014##l");
if (selection == 0) {
	selection = npc.askYesNo("If you use the regular coupon, you'll be awarded a random pair of cosmetic lenses. Are you going to use a #b#t5152011##k and really make the change to your eyes?");
	if (selection == 1) {
		if (player.hasItem(5152011, 1)) {
			player.loseItem(5152011, 1);
			player.setFace(getRandomColor());
			npc.say("Enjoy your new and improved cosmetic lenses!");
		} else {
			npc.say("I'm sorry, but I don't think you have our cosmetic lens coupon with you right now. Without the coupon, I'm afraid I can't do it for you..");
		}
	}
} else if (selection == 1) {
	let faces = npc.getAllEyeColors();
	selection = npc.askAvatar("With our new computer program, you can see yourself after the treatment in advance. What kind of lens would you like to wear? Please choose the style of your liking.", faces);
	if (player.hasItem(5152014, 1)) {
		player.loseItem(5152014, 1);
		player.setFace(faces[selection]);
		npc.say("Enjoy your new and improved cosmetic lenses!");
	} else {
		npc.say("I'm sorry, but I don't think you have our cosmetic lens coupon with you right now. Without the coupon, I'm afraid I can't do it for you..");
	}
}