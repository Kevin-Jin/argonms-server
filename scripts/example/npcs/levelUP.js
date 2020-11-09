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
 * KIN (NPC 9900000)
 * Victoria Road: Ellinia (Map 180000000)
 *
 * NPC in the GM map that lets GMs to change to any appearance style.
 *
 * @author GoldenKevin (content from Vana r3171)
 */

function presentStyles(styles) {
	let selection = npc.askAvatar("", styles);
	let style = styles[selection]
	switch (Math.floor(style / 10000)) {
		case 0:
			player.setSkin(style);
			break;
		case 2:
			player.setFace(style);
			break;
		case 3:
			player.setHair(style);
			break;
	}
}

function randomElement(array) {
	return array[Math.floor(Math.random() * array.length)];
}

function randomStyle() {
	player.setFace(randomElement(npc.getAllEyeStyles()));
	player.setFace(randomElement(npc.getAllEyeColors()));
	player.setHair(randomElement(npc.getAllHairStyles()));
	player.setHair(randomElement(npc.getAllHairColors()));
}

let selection = npc.askMenu("Hello, what can I do for you today?\r\n"
		+ "#L0##bSkin#k#l\r\n"
		+ "#L1##bHair#k#l\r\n"
		+ "#L2##bHair Color#k#l\r\n"
		+ "#L3##bEyes#k#l\r\n"
		+ "#L4##bEyes Color#k#l\r\n"
		+ "#L5##bRandom New Look#k#l\r\n");
switch (selection) {
	case 0:
		presentStyles(npc.getAllSkinColors())
		break;
	case 1:
		presentStyles(npc.getAllHairStyles());
		break;
	case 2:
		presentStyles(npc.getAllHairColors());
		break;
	case 3:
		presentStyles(npc.getAllEyeStyles());
		break;
	case 4:
		presentStyles(npc.getAllEyeColors());
		break;
	case 5:
		randomStyle();
		break;
}