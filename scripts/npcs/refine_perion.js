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
 * Mr. Thunder: Item Creator (NPC 1022003)
 * Victoria Road: Perion (Map 102000000)
 *
 * Ore refiner.
 * Refining NPC for warriors - upgrades only.
 *
 * @author GoldenKevin (content from Vana r3171)
 */

let MESOS = 4031138;

function refinePrompt(str, itemids, itemreqs) {
	for (let i = 0; i < itemids.length; i++)
		str += ("\r\n#L" + i + "##b #t" + itemids[i] + "##k#l");
	let selection = npc.askMenu(str);

	let item = itemids[selection];
	let reqs = itemreqs[selection];

	str = "To make a(n) #t" + item + "#, I need the following materials. How many would you like to make?\r\n\r\n";
	for (let i = 0; i < reqs.length; i += 2) {
		str += "#i" + reqs[i] + "# ";
		if (reqs[i] == MESOS)
			str += reqs[i + 1] + " mesos";
		else if (reqs[i + 1] != 1)
			str += reqs[i + 1] + " #t" + reqs[i] + "#";
		else
			str += "#t" + reqs[i] + "#";
		str += "\r\n";
	}
	let quantity = npc.askNumber(str, 0, 0, 100);

	// either Nexon or Vana done goofed with the item id part, not me
	str = "To make #b" + quantity + " #t" + reqs[0] + "#(s)#k, I need the following materials. Are you sure you want to make these?\r\n\r\n";
	for (let i = 0; i < reqs.length; i += 2) {
		str += "#i" + reqs[i] + "# ";
		if (reqs[i] == MESOS)
			str += reqs[i + 1] * quantity + " mesos";
		else if (reqs[i + 1] * quantity != 1)
			str += reqs[i + 1] * quantity + " #t" + reqs[i] + "#";
		else
			str += "#t" + reqs[i] + "#";
		str += "\r\n";
	}
	let approve = npc.askYesNo(str);

	if (approve == 0)
		selection = -1;
	return [selection, quantity];
}

function itemUpgradePrompt(warning, str, itemids, itemreqs, itemlimits, itemjobs, itemstats) {
	npc.sayNext(warning);
	for (let i = 0; i < itemids.length; i++)
		str += ("\r\n#L" + i + "##b #t" + itemids[i] + "##k(level limit: " + itemlimits[i] + ", " + itemjobs[i] + ")#l");
	let selection = npc.askMenu(str);

	let item = itemids[selection];
	let reqs = itemreqs[selection];
	let limit = itemlimits[selection];
	let stat = itemstats[selection];

	str = "To make one #t" + item + "#, I need the following materials.";
	if (stat != null)
		str += "This item has an option of " + stat + ". ";
	str += "Make sure you don't use an item that's been upgraded as a material for it. What do you think? Do you want one?\r\n\r\n";
	for (let i = 0; i < reqs.length; i += 2) {
		str += "#i" + reqs[i] + "# ";
		if (reqs[i] == MESOS)
			str += reqs[i + 1] + " mesos";
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

let selection = npc.askYesNo("Wait, do you have the ore of either a jewel or mineral? With a little service fee, I can turn them into materials needed to create weapons or shields. Not only that, I can also upgrade an item with it to make an even better item. What do you think? Do you want to do it?");
if (selection == 0) {
	npc.sayNext("Really? Sorry to hear that. If you don't need it, then oh well ... if you happen to collect a lot of ores in the future, please find me. I'll make something only I can make.");
} else if (selection == 1) {
	selection = npc.askMenu("Alright, with the ore and a little service fee, I'll refine it so you can you use it. Check and see if your etc, storage has any room. Now ... what would you like me to do?\r\n"
			+ "#L0##b Refine the raw ore of a mineral#l\r\n"
			+ "#L1##b Refine a jewel ore#l\r\n"
			+ "#L2##b Upgrade a helmet#l\r\n"
			+ "#L3##b Upgrade a shield#l\r\n");
	let itemids;
	let itemreqs;
	let quantity;
	let equip;
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
			[selection, quantity] = refinePrompt("Which mineral do you want to make?", itemids, itemreqs);
			equip = false;
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
			[selection, quantity] = refinePrompt("Which jewel do you want to refine?", itemids, itemreqs);
			equip = false;
			break;
		case 2:
			itemids = [1002042, 1002041, 1002002, 1002044, 1002003, 1002040, 1002007, 1002052, 1002011, 1002058, 1002009, 1002056, 1002087, 1002088, 1002049, 1002050, 1002047, 1002048, 1002099, 1002098, 1002085, 1002028, 1002022, 1002101];
			let helmetlimits = [15, 15, 10, 10, 12, 12, 15, 15, 20, 20, 20, 20, 22, 22, 25, 25, 35, 35, 40, 40, 50, 50, 55, 55];
			let helmetjobs = ["all", "all", "warrior", "warrior", "warrior", "warrior", "warrior", "warrior", "warrior", "warrior", "warrior", "warrior", "warrior", "warrior", "warrior", "warrior", "warrior", "warrior", "warrior", "warrior", "warrior", "warrior", "warrior", "warrior"]
			let helmetstats = [null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, "HP + 10", "DEX + 1", "STR + 1", "STR + 2", "STR + 1", "STR + 2", "DEX + 1, MP + 30", "STR + 1, MP + 30"];
			itemreqs = [
				[1002001, 1, 4011002, 1, MESOS, 500],
				[1002001, 1, 4021006, 1, MESOS, 300],
				[1002043, 1, 4011001, 1, MESOS, 500],
				[1002043, 1, 4011002, 1, MESOS, 800],
				[1002039, 1, 4011001, 1, MESOS, 500],
				[1002039, 1, 4011002, 1, MESOS, 800],
				[1002051, 1, 4011001, 2, MESOS, 1000],
				[1002051, 1, 4011002, 2, MESOS, 1500],
				[1002059, 1, 4011001, 3, MESOS, 1500],
				[1002059, 1, 4011002, 3, MESOS, 2000],
				[1002055, 1, 4011001, 3, MESOS, 1500],
				[1002055, 1, 4011002, 3, MESOS, 2000],
				[1002027, 1, 4011002, 4, MESOS, 2000],
				[1002027, 1, 4011006, 4, MESOS, 4000],
				[1002005, 1, 4011006, 5, MESOS, 4000],
				[1002005, 1, 4011005, 5, MESOS, 5000],
				[1002004, 1, 4021000, 3, MESOS, 8000],
				[1002004, 1, 4021005, 3, MESOS, 10000],
				[1002021, 1, 4011002, 5, MESOS, 12000],
				[1002021, 1, 4011006, 6, MESOS, 15000],
				[1002086, 1, 4011002, 5, MESOS, 20000],
				[1002086, 1, 4011004, 4, MESOS, 25000],
				[1002100, 1, 4011007, 1, 4011001, 7, MESOS, 30000],
				[1002100, 1, 4011007, 1, 4011002, 7, MESOS, 30000]
			];
			selection = itemUpgradePrompt("So you want to upgrade the helmet? Ok, then. A word of warning, though: All the items that will be used for upgrading will be gone, and if you use an item that has been #rupgraded#k with a scroll before, the effect will not go in when upgraded. Please take that info consideration when making the decision, ok?",
					"So~~ what kind of a helmet do you want to upgrade and create?", itemids, itemreqs, helmetlimits, helmetjobs, helmetstats);
			quantity = 1;
			equip = true;
			break;
		case 3:
			itemids = [1092013, 1092014, 1092010, 1092011];
			let shieldlimits = [40, 40, 60, 60];
			let shieldjobs = ["warrior", "warrior", "warrior", "warrior"];
			let shieldstats = ["STR + 2", "DEX + 2", "DEX + 2", "STR + 2"];
			itemreqs = [
				[1092012, 1, 4011002, 10, MESOS, 100000],
				[1092012, 1, 4011003, 10, MESOS, 100000],
				[1092009, 1, 4011007, 1, 4011004, 15, MESOS, 120000],
				[1092009, 1, 4011007, 1, 4011003, 15, MESOS, 120000]
			];
			selection = itemUpgradePrompt("So you want to upgrade the shield? Ok, then. A word of warning, though: All the items that will be used for upgrading will be gone, and if you use an item that has been #rupgraded#k with a scroll before, the effect will not go in when upgraded. Please take that info consideration when making the decision, ok?",
					"So~~ what kind of a shield do you want to upgrade and create?", itemids, itemreqs, shieldlimits, shieldjobs, shieldstats);
			quantity = 1;
			equip = true;
			break;
	}
	if (selection == -1) {
		if (equip)
			npc.sayNext("Really? Sorry to hear that. Come back when you need me.");
		else
			npc.sayNext("We have all kinds of items so don't panic, and choose the one you want to buy...");
	} else {
		let okay = true;
		let item = itemids[selection];
		let reqs = itemreqs[selection];
		if (quantity == 0)
			okay = false;
		for (let i = 0; i < reqs.length && okay; i += 2) {
			if (reqs[i] != MESOS) {
				if (!player.hasItem(reqs[i], reqs[i + 1] * quantity))
					okay = false;
			} else if (!player.hasMesos(reqs[i + 1] * quantity)) {
				okay = false;
			}
		}
		if (!player.canGainItem(item, quantity))
			okay = false;

		if (!okay) {
			npc.sayNext("Please double-check you have all the materials you need and if you etc. inventory may be full or not.");
		} else {
			for (let i = 0; i < reqs.length; i += 2)
				if (reqs[i] != MESOS)
					player.loseItem(reqs[i], reqs[i + 1] * quantity);
				else
					player.loseMesos(reqs[i + 1] * quantity);
			player.gainItem(item, quantity);
			if (equip)
				npc.sayNext("Hey! Here, take #t" + item + "#. I'm good ... a finely refined item like this, have you seen it anywhere else?? Please come again~");
			else
				npc.sayNext("Hey! Here, take " + quantity + " #t" + item + "#(s). This came out even finer than I though ... a finely refined item like this, I don't think you'll see it anywhere else!! Please come again~");
		}
	}
}