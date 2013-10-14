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
 * Neve: Glove Maker (NPC 2010003)
 * Orbis: Orbis Park (Map 200000200)
 *
 * Refining NPC in Orbis - high level gloves only.
 *
 * @author GoldenKevin (content from KiniroMS r227)
 */

let MESOS = 4031138;

function itemMakePrompt(str, itemids, itemreqs, itemlimits, itemjobs) {
	for (let i = 0; i < itemids.length; i++)
		str += "\r\n#L" + i + "# #b#t" + itemids[i] + "##k - " + itemjobs[i] + " Lv. " + itemlimits[i] + "#l";
	let selection = npc.askMenu(str);

	let item = itemids[selection];
	let reqs = itemreqs[selection];
	let limit = itemlimits[selection];

	str = "You want me to make a #t" + item + "#? In that case, I'm going to need specific items from you in order to make it. Make sure you have room in your inventory, though!\r\n#b";
	for (let i = 0; i < reqs.length; i += 2) {
		str += "#i" + reqs[i] + "# ";
		if (reqs[i] == MESOS)
			str += reqs[i + 1] + " meso";
		else if (reqs[i + 1] != 1)
			str += reqs[i + 1] + " #t" + reqs[i] + "#";
		else
			str += "#t" + reqs[i] + "#";
		str += "\r\n";
	}
	let approve = npc.askYesNo(str);

	if (approve == 0)
		return -1;
	return selection;
}

let selection = npc.askMenu("Hello there. I'm Orbis' number one glove maker. Would you like me to make you something?\r\n#b"
		+ "#L0#Create or upgrade a Warrior glove#l\r\n"
		+ "#L1#Create or upgrade a Bowman glove#l\r\n"
		+ "#L2#Create or upgrade a Magician glove#l\r\n"
		+ "#L3#Create or upgrade a Thief glove#l\r\n");
let selStr;
let itemids;
let itemreqs;
let quantity;
let equip;
switch (selection) {
	case 0: {
		itemids = [1082103, 1082104, 1082105, 1082114, 1082115, 1082116, 1082117];
		let glovejobs = ["Warrior", "Warrior", "Warrior", "Warrior", "Warrior", "Warrior", "Warrior"];
		let glovelimits = [70, 70, 70, 80, 80, 80, 80];
		itemreqs = [
			[4005000, 2, 4011000, 8, 4011006, 3, 4000030, 70, 4003000, 55, MESOS, 90000],
			[1082103, 1, 4011002, 6, 4021006, 4, MESOS, 90000],
			[1082103, 1, 4021006, 8, 4021008, 3, MESOS, 100000],
			[4005000, 2, 4005002, 1, 4021005, 8, 4000030, 90, 4003000, 60, MESOS, 100000],
			[1082114, 1, 4005000, 1, 4005002, 1, 4021003, 7, MESOS, 110000],
			[1082114, 1, 4005002, 3, 4021000, 8, MESOS, 110000],
			[1082114, 1, 4005000, 2, 4005002, 1, 4021008, 4, MESOS, 120000]
		];
		selection = itemMakePrompt("Warrior glove? Okay, then which one?",
				itemids, itemreqs, glovelimits, glovejobs);
		break;
	}
	case 1: {
		itemids = [1082106, 1082107, 1082108, 1082109, 1082110, 1082111, 1082112];
		let glovejobs = ["Bowman", "Bowman", "Bowman", "Bowman", "Bowman", "Bowman", "Bowman"];
		let glovelimits = [70, 70, 70, 80, 80, 80, 80];
		itemreqs = [
			[4005002, 2, 4021005, 8, 4011004, 3, 4000030, 70, 4003000, 55, MESOS, 9000],
			[1082106, 1, 4021006, 5, 4011006, 3, MESOS, 9000],
			[1082106, 1, 4021007, 2, 4021008, 3, MESOS, 100000],
			[4005002, 2, 4005000, 1, 4021000, 8, 4000030, 90, 4003000, 60, MESOS, 100000],
			[1082109, 1, 4005002, 1, 4005000, 1, 4021005, 7, MESOS, 110000],
			[1082109, 1, 4005002, 1, 4005000, 1, 4021003, 7, MESOS, 110000],
			[1082109, 1, 4005002, 2, 4005000, 1, 4021008, 4, MESOS, 120000]
		];
		selection = itemMakePrompt("Bowman glove? Okay, then which one?",
				itemids, itemreqs, glovelimits, glovejobs);
		break;
	}
	case 2: {
		itemids = [1082098, 1082099, 1082100, 1082121, 1082122, 1082123];
		let glovejobs = ["Magician", "Magician", "Magician", "Magician", "Magician", "Magician"];
		let glovelimits = [70, 70, 70, 80, 80, 80];
		itemreqs = [
			[4005001, 2, 4011000, 6, 4011004, 6, 4000030, 70, 4003000, 55, MESOS, 90000],
			[1082098, 1, 4021002, 6, 4021007, 2, MESOS, 90000],
			[1082098, 1, 4021008, 3, 4011006, 3, MESOS, 100000],
			[4005001, 2, 4005003, 1, 4021003, 8, 4000030, 90, 4003000, 60, MESOS, 100000],
			[1082121, 1, 4005001, 1, 4005003, 1, 4021005, 7, MESOS, 110000],
			[1082121, 1, 4005001, 2, 4005003, 1, 4021008, 4, MESOS, 120000]
		];
		selection = itemMakePrompt("Magician glove? Okay, then which one?",
				itemids, itemreqs, glovelimits, glovejobs);
		break;
	}
	case 3: {
		itemids = [1082095, 1082096, 1082097, 1082118, 1082119, 1082120];
		let glovejobs = ["Thief", "Thief", "Thief", "Thief", "Thief", "Thief"];
		let glovelimits = [70, 70, 70, 80, 80, 80];
		itemreqs = [
			[4005003, 2, 4011000, 6, 4011003, 6, 4000030, 70, 4003000, 55, MESOS, 90000],
			[1082095, 1, 4011004, 6, 4021007, 2, MESOS, 90000],
			[1082095, 1, 4021007, 3, 4011006, 3, MESOS, 100000],
			[4005003, 2, 4005002, 1, 4011002, 8, 4000030, 90, 4003000, 60, MESOS, 100000],
			[1082118, 1, 4005003, 1, 4005002, 1, 4021001, 7, MESOS, 110000],
			[1082118, 1, 4005003, 2, 4005002, 1, 4021000, 8, MESOS, 120000]
		];
		selection = itemMakePrompt("Thief glove? Okay, then which one?",
				itemids, itemreqs, glovelimits, glovejobs);
		break;
	}
}

if (selection != -1) {
	let costOkay = true;
	let itemOkay = true;
	let item = itemids[selection];
	let reqs = itemreqs[selection];
	for (let i = 0; i < reqs.length && okay; i += 2) {
		if (reqs[i] != MESOS) {
			if (!player.hasItem(reqs[i], reqs[i + 1]))
				itemOkay = false;
		} else if (!player.hasMesos(reqs[i + 1])) {
			costOkay = false;
		}
	}
	if (!player.canGainItem(item, 1))
		itemOkay = false;

	if (!costOkay) {
		npc.say("I'm afraid you cannot afford my services.");
	} else if (!itemOkay) {
		npc.say("I'm afraid that substitute items are unacceptable, if you want your gloves made properly.");
	} else {
		for (let i = 0; i < reqs.length; i += 2)
			if (reqs[i] != MESOS)
				player.loseItem(reqs[i], reqs[i + 1]);
			else
				player.loseMesos(reqs[i + 1]);
		player.gainItem(item, 1);
		npc.say("Done. If you need anything else, just ask again.");
	}
}