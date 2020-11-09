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
 * Brittany: Hair Salon Assistant (NPC 1012104)
 * Victoria Road: Henesys Hair Salon (Map 100000104)
 *
 * Henesys hair changer - random hair.
 *
 * @author GoldenKevin (content from Vana r3171)
 */

function getRandomStyle(gender, currentHair) {
	let color = currentHair % 10;
	let styles;
	if (gender == 0)
		styles = [30310, 30330, 30060, 30150, 30410, 30210, 30140, 30120, 30200, 30560, 30510, 30610, 30470];
	else if (gender == 1)
		styles = [31150, 31310, 31300, 31160, 31100, 31410, 31030, 31080, 31070, 31610, 31350, 31510, 31740];
	let style = styles[Math.floor(Math.random() * styles.length)];
	if (npc.isHairValid(style + color)) //prefer current hair color
		style += color;
	return style;
}

function getRandomColor() {
	let array = npc.getAllHairColors();
	return array[Math.floor(Math.random() * array.length)];
}

let selection = npc.askMenu("I'm Brittany the assistant. If you have #b#t5150010##k or #b#t5151000##k by any chance, then how about letting me change your hairdo?\r\n#b"
		+ "#L0# Haircut(EXP coupon)#l\r\n"
		+ "#L1# Dye your hair(REG coupon)#l");
let item;
let hair;
if (selection == 0) {
	item = 5150010;
	selection = npc.askYesNo("If you use the EXP coupon your hair will change RANDOMLY with a chance to obtain a new experimental style that even you didn't think was possible. Are you going to use #b#t5150010##k and really change your hairstyle?");
	hair = getRandomStyle(player.getGender(), player.getHair());
} else if (selection == 1) {
	item = 5151000;
	selection = npc.askYesNo("If you use a regular coupon your hair will change RANDOMLY. Do you still want to use #b#t5151000##k and change it up?");
	hair = getRandomColor();
}
if (selection == 1) {
	if (player.hasItem(item, 1)) {
		player.loseItem(item, 1);
		player.setHair(hair);
		npc.say("Enjoy!");
	} else {
		npc.sayNext("Hmmm...it looks like you don't have our designated coupon...I'm afraid I can't give you a haircut without it. I'm sorry.");
	}
} else if (selection == 0) {
	npc.sayNext("I understand...think about it, and if you still feel like changing come talk to me.");
}