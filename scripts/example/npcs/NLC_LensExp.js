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
 * Bomack: Lens Broker (NPC 9201061)
 * New Leaf City Town Street: NLC Mall (Map 600000001)
 *
 * New Leaf City face changer - random face, eye color only.
 *
 * @author GoldenKevin (content from Vana r3171)
 */

function getRandomColor() {
	let array = npc.getAllEyeColors();
	return array[Math.floor(Math.random() * array.length)];
}

let selection = npc.askYesNo("What's up! I'm Bomack. If you use the regular coupon, you'll be hooked up with a random pair of cosmetic lenses. You wanna use #b#t5152035##k and go forward with the procedure?");
if (selection == 1) {
	if (player.hasItem(5152035, 1)) {
		player.loseItem(5152035, 1);
		player.setFace(getRandomColor());
		npc.sayNext("Enjoy your new and improved cosmetic lenses!");
	} else {
		npc.sayNext("Ah, it looks like you don't have the right coupon for this place. Sorry, but that means it's a no-go with the procedure. ");
	}
} else if (selection == 0) {
	npc.sayNext("For real? Nah, that's fine. Some people get the cold feet, I can sympathize. When you decide to make the change, you just let me know.");
}