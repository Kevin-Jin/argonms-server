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
 * Denma the Owner: Plastic Surgeon (NPC 1052004)
 * Victoria Road: Henesys Plastic Surgery (Map 100000103)
 *
 * Henesys face changer - VIP coupons, eye style only.
 *
 * @author GoldenKevin (content from Vana r2111)
 */

function getStyleChoices(gender, currentFace) {
	let color = currentFace % 1000 - (currentFace % 100);
	let styles;
	if (gender == 0)
		styles = [20000, 20001, 20002, 20003, 20004, 20005, 20006, 20007, 20008, 20012, 20014];
	else if (gender == 1)
		styles = [21000, 21001, 21002, 21003, 21004, 21005, 21006, 21007, 21008, 21012, 21014];
	for (let i = 0; i < styles.length; i++)
		styles[i] += color; //eye style choice colors are current eye color
	return styles;
}

let faces = getStyleChoices(npc.getPlayerGender(), npc.getPlayerFace());
let selection = npc.askAvatar("Let's see... I can totally transform your face into something new. Don't you want to try it? For #b#t5152001##k, you can get the face of your liking. Take your time in choosing the face of your preference...", faces);
if (npc.playerHasItem(5152001, 1)) {
	npc.takeItem(5152001, 1);
	npc.setPlayerFace(faces[selection]);
	npc.sayNext("Enjoy your new and improved face!");
} else {
	npc.sayNext("Hmm ... it looks like you don't have the coupon specifically for this place...sorry to say this, but without the coupon, there's no plastic surgery for you.");
}