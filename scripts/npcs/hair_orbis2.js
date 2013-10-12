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
 * Rinz the Assistant: Assistant Hair Stylist (NPC 2012007)
 * Orbis Park: Orbis Hair Salon (Map 200000202)
 *
 * Orbis hair changer - random hair.
 *
 * @author GoldenKevin (content from KiniroMS r227)
 */

function getRandomStyle(gender, currentHair) {
	let color = currentHair % 10;
	let styles;
	if (gender == 0)
		styles = [30030, 30020, 30000, 30270, 30230, 30260, 30280, 30240, 30290, 30340, 30370, 30630, 30530, 30760];
	else if (gender == 1)
		styles = [31040, 31000, 31250, 31220, 31260, 31240, 31110, 31270, 31030, 31230, 31530, 31710, 31320, 31650, 31630];
	let style = styles[Math.floor(Math.random() * styles.length)];
	if (npc.isHairValid(style + color)) //prefer current hair color
		style += color;
	return style;
}

function getRandomColor() {
	let array = npc.getAllHairColors();
	return array[Math.floor(Math.random() * array.length)];
}

let selection = npc.askMenu("I'm Rinz, the assistant. Do you have #b#t5150013##k or #b#t5151004##k with you? If so, what do you think about letting me take care of your hairdo? What do you want to do with your hair?\r\n#b"
		+ "#L0#Haircut: #i5150013##t5150013##l\r\n"
		+ "#L1#Dye your hair: #i5151004##t5151004##l");
let item;
let styleChange;
let hair;
if (selection == 0) {
	item = 5150013;
	selection = npc.askYesNo("If you use the EXP coupon your hair will change RANDOMLY with a chance to obtain a new experimental style that I came up with. Are you going to use #b#t5150010##k and really change your hairstyle?");
	hair = getRandomStyle(player.getGender(), player.getHair());
	styleChange = true;
} else if (selection == 1) {
	item = 5151004;
	selection = npc.askYesNo("If you use a regular coupon your hair will change RANDOMLY. Do you still want to use #b#t5151004##k and change it up?");
	hair = getRandomColor();
	styleChange = false;
}
if (selection == 1) {
	if (player.hasItem(item, 1)) {
		player.loseItem(item, 1);
		player.setHair(hair);
		npc.say("Enjoy your new and improved hair" + (styleChange ? "style" : "color") + "!");
	} else {
		npc.say("Hmmm...it looks like you don't have our designated coupon...I'm afraid I can't " + (styleChange ? "give you a haircut" : "dye your hair") + " without it. I'm sorry...");
	}
}