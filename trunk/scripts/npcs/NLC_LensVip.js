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
 * J.J.: Lens Magician (NPC 9201062)
 * New Leaf City Town Street: NLC Mall (Map 600000001)
 *
 * <insert description here>
 *
 * @author GoldenKevin (content from Vana r3171)
 */

let faces = npc.getAllEyeColors();
selection = npc.askAvatar("Hi there! I'm J.J., the one in charge of the cosmetic lenses here at NLC Shop! With our specialized machine, you can see the results of your potential treatment in advance. What kind of lens would you like to wear? Please choose the style of your liking.", faces);
if (player.hasItem(5152036, 1)) {
	player.loseItem(5152036, 1);
	player.setFace(faces[selection]);
	npc.sayNext("Enjoy your new and improved cosmetic lenses!");
} else {
	npc.sayNext("I'm sorry, but I don't think you have our cosmetic lens coupon with you. We can't proceed without the coupon.");
}