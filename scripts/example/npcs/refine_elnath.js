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
 * Vogen: Refining Expert (NPC 2020000)
 * El Nath: El Nath Market (Map 211000100)
 *
 * Wood processor and screw maker.
 * Arrow maker.
 * Ore refiner.
 *
 * @author GoldenKevin (content from KiniroMS r227)
 */

let MESOS = 4031138;

function prompt(str, itemids, itemreqs, quantity) {
	let selection = npc.askMenu(str);

	let item = itemids[selection];
	let reqs = itemreqs[selection];

	if (quantity == null)
		quantity = npc.askNumber("So, you want me to make some #t" + item + "#s? In that case, how many do you want me to make?", 1, 1, 100);

	str = "You want me to make " + (quantity == 1 ? "a" : quantity) + " #t" + item + "#? In that case, I'm going to need specific items from you in order to make it. Make sure you have room in your inventory, though!#b";
	for (let i = 0; i < reqs.length; i += 2) {
		str += "\r\n#i" + reqs[i] + "# ";
		if (reqs[i] == MESOS)
			str += reqs[i + 1] * quantity + " meso";
		else
			str += reqs[i + 1] * quantity + " #t" + reqs[i] + "#";
	}
	let approve = npc.askYesNo(str);

	if (approve == 0)
		selection = -1;
	return [selection, quantity];
}

function refineStr(str) {
	for (let i = 0; i < itemids.length; i++)
		str += ("\r\n#L" + i + "# #t" + itemids[i] + "##l");
	return str
}

let selection = npc.askMenu("Hm? Who might you be? Oh, you've heard about my forging skills? In that case, I'd be glad to process some of your ores... for a fee.#b\r\n"
		+ "#L0# Refine a mineral ore#l\r\n"
		+ "#L1# Refine a jewel ore#l\r\n"
		+ "#L2# Refine a rare jewel#l\r\n"
		+ "#L3# Refine a crystal ore#l\r\n"
		+ "#L4# Create materials#l\r\n"
		+ "#L5# Create Arrows#l");
let itemids;
let itemreqs;
let quantity;
let giveQty = function(item, quantity) { return quantity; };
switch (selection) {
	case 0:
		itemids = [4011000, 4011001, 4011002, 4011003, 4011004, 4011005, 4011006];
		itemreqs = [
			[4010000, 10, MESOS, 300],
			[4010001, 10, MESOS, 300],
			[4010002, 10, MESOS, 300],
			[4010003, 10, MESOS, 500],
			[4010004, 10, MESOS, 500],
			[4010005, 10, MESOS, 500],
			[4010006, 10, MESOS, 800]
		];
		[selection, quantity] = prompt(refineStr("So, what kind of mineral ore would you like to refine?#b"), itemids, itemreqs, null);
		break;
	case 1:
		itemids = [4021000, 4021001, 4021002, 4021003, 4021004, 4021005, 4021006, 4021007, 4021008];
		itemreqs = [
			[4020000, 10, MESOS, 500],
			[4020001, 10, MESOS, 500],
			[4020002, 10, MESOS, 500],
			[4020003, 10, MESOS, 500],
			[4020004, 10, MESOS, 500],
			[4020005, 10, MESOS, 500],
			[4020006, 10, MESOS, 500],
			[4020007, 10, MESOS, 1000],
			[4020008, 10, MESOS, 3000]
		];
		[selection, quantity] = prompt(refineStr("So, what kind of jewel ore would you like to refine?#b"), itemids, itemreqs, null);
		break;
	case 2:
		itemids = [4011007, 4021009];
		itemreqs = [
			[4011000, 1, 4011001, 1, 4011002, 1, 4011003, 1, 4011004, 1, 4011005, 1, 4011006, 1, MESOS, 10000],
			[4021000, 1, 4021001, 1, 4021002, 1, 4021003, 1, 4021004, 1, 4021005, 1, 4021006, 1, 4021007, 1, 4021008, 1, MESOS, 15000]
		];
		[selection, quantity] = prompt(refineStr("A rare jewel? Which one were you thinking of?#b"), itemids, itemreqs, null);
		break;
	case 3:
		itemids = [4005000, 4005001, 4005002, 4005003, 4005004];
		itemreqs = [
			[4004000, 10, MESOS, 5000],
			[4004001, 10, MESOS, 5000],
			[4004002, 10, MESOS, 5000],
			[4004003, 10, MESOS, 5000],
			[4004004, 10, MESOS, 1000000]
		];
		[selection, quantity] = prompt(refineStr("Crystal ore? It's hard to find those around here...#b"), itemids, itemreqs, null);
		break;
	case 4:
		itemids = [4003001, 4003001, 4003000];
		itemreqs = [
			[4000003, 10],
			[4000018, 5],
			[4011000, 1, 4011001, 1]
		];
		[selection, quantity] = prompt("Materials? I know of a few materials that I can make for you...#b\r\n"
				+ "#L0#Make #t" + itemids[0] + "# with #t4000003##l\r\n"
				+ "#L1#Make #t" + itemids[1] + "# with #t4000018##l\r\n"
				+ "#L2#Make #t" + itemids[2] + "# (packs of 15)#l", itemids, itemreqs, null);
		giveQty = function(item, quantity) {
			if (item == 4003000)
				return quantity * 15;
			return quantity;
		};
		break;
	case 5:
		itemids = [2060000, 2061000, 2060001, 2061001, 2060002, 2061002];
		itemreqs = [
			[4003001, 1, 4003004, 1],
			[4003001, 1, 4003004, 1],
			[4011000, 1, 4003001, 3, 4003004, 10],
			[4011000, 1, 4003001, 3, 4003004, 10],
			[4011001, 1, 4003001, 5, 4003005, 15],
			[4011001, 1, 4003001, 5, 4003005, 15]
		];
		[selection, quantity] = prompt("Arrows? Not a problem at all.#b\r\n"
				+ "#L0##t" + itemids[0] + "##l\r\n"
				+ "#L1##t" + itemids[1] + "##l\r\n"
				+ "#L2##t" + itemids[2] + "##l\r\n"
				+ "#L3##t" + itemids[3] + "##l\r\n"
				+ "#L4##t" + itemids[4] + "##l\r\n"
				+ "#L5##t" + itemids[5] + "##l", itemids, itemreqs, 1);
		giveQty = function(item, quantity) {
			if (item >= 2060000 && item <= 2060002)
				return 1000 - (item - 2060000) * 100;
			else if (item >= 2061000 && item <= 2061002)
				return 1000 - (item - 2061000) * 100;
			return quantity;
		};
		break;
}
if (selection != -1) {
	let costOkay = true;
	let itemOkay = true;
	let item = itemids[selection];
	let reqs = itemreqs[selection];
	if (quantity == 0)
		okay = false;
	for (let i = 0; i < reqs.length && okay; i += 2) {
		if (reqs[i] != MESOS) {
			if (!player.hasItem(reqs[i], reqs[i + 1] * quantity))
				itemOkay = false;
		} else if (!player.hasMesos(reqs[i + 1] * quantity)) {
			costOkay = false;
		}
	}

	let gain = giveQty(item, quantity);
	if (!player.canGainItem(item, gain))
		itemOkay = false;

	if (!costOkay) {
		npc.say("I'm afraid you cannot afford my services.");
	} else if (!okay) {
		npc.say("I can't refine anything for you without the proper items.");
	} else {
		for (let i = 0; i < reqs.length; i += 2)
			if (reqs[i] != MESOS)
				player.loseItem(reqs[i], reqs[i + 1] * quantity);
			else
				player.loseMesos(reqs[i + 1] * quantity);
		player.gainItem(item, gain);
		npc.say("All done. If you need anything else, just ask.");
	}
}