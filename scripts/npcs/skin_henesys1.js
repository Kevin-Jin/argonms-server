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
 * Ms. Tan: Dermatologist (NPC 1012105)
 * Victoria Road: Henesys Skin-Care (Map 100000105)
 *
 * Henesys skin changer.
 *
 * @author GoldenKevin (content from Vana r2111)
 */

let skinColorChoices = [0, 1, 2, 3, 4];

npc.sayNext("Well, hello! Welcome to the Henesys Skin-Care! Would you like to have a firm, tight, healthy looking skin like mine?  With #b#t5153000##k, you can let us take care of the rest and have the kind of skin you've always wanted~!");
let selection = npc.askAvatar("With our specialized machine, you can see yourself after the treatment in advance. What kind of skin-treatment would you like to do? Choose the style of your liking...", skinColorChoices);

if (npc.playerHasItem(5153000, 1)) {
	npc.takeItem(5153000, 1);
	npc.setPlayerSkin(skinColorChoices[selection]);
	npc.sayNext("Enjoy your new and improved skin!");
} else {
	npc.sayNext("Um...you don't have the skin-care coupon you need to receive the treatment. Sorry, but I am afraid we can't do it for you.");
}