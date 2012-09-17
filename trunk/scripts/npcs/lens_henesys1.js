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
 * Dr. Lenu: Lens Specialist (NPC 9200100)
 * Victoria Road: Henesys Plastic Surgery (Map 100000103)
 *
 * Henesys face changer - eye color only.
 *
 * @author GoldenKevin (content from Vana r2111)
 */

function getRandomColor() {
	let array = npc.getAllEyeColors();
	return array[Math.floor(Math.random() * array.length)];
}

let selection = npc.askMenu("Hi, there~! I'm Dr. Lenu, in charge of the cosmetic lenses here at the Henesys Plastic Surgery Shop! With #b#t5152010##k or #b#t5152013##k, you can let us take care of the rest and have the kind of beautiful look you've always craved~! Remember, the first thing everyone notices about you is the eyes, and we can help you find the cosmetic lens that most fits you! Now, what would you like to use?\r\n#b"
		+ "#L0# Cosmetic Lenses at Henesys (Reg. coupon)#l\r\n"
		+ "#L1# Cosmetic Lenses at Henesys (VIP coupon)#l");
if (selection == 0) {
	selection = npc.askYesNo("If you use the regular coupon, you'll be awarded a random pair of cosmetic lenses. Are you going to use #b#t5152010##k and really make the change to your eyes?");
	if (selection == 1) {
		if (player.hasItem(5152010, 1)) {
			player.loseItem(5152010, 1);
			player.setFace(getRandomColor());
			npc.sayNext("Enjoy your new and improved cosmetic lenses!");
		} else {
			npc.sayNext("I'm sorry, but I don't think you have our cosmetic lens coupon with you right now. Without the coupon, I'm afraid I can't do it for you");
		}
	} else if (selection == 0) {
		npc.sayNext("I see. That's understandable, since you're unsure whether you'll get the cosmetic lenses of your liking. We're in no hurry, whatsoever, so take your time! Please let me know when you decide to make the change~!");
	}
} else if (selection == 1) {
	let faces = npc.getAllEyeColors();
	selection = npc.askAvatar("With our specialized machine, you can see yourself after the treatment in advance. What kind of lens would you like to wear? Choose the style of your liking...", faces);
	if (player.hasItem(5152013, 1)) {
		player.loseItem(5152013, 1);
		player.setFace(faces[selection]);
		npc.sayNext("Enjoy your new and improved cosmetic lenses!");
	} else {
		npc.sayNext("I'm sorry, but I don't think you have our cosmetic lens coupon with you right now. Without the coupon, I'm afraid I can't do it for you");
	}
}