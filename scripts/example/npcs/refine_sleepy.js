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
 * Chrishrama: Item Creator (NPC 1061000)
 * Dungeon: Sleepywood (Map 105040300)
 *
 * Refining NPC in Sleepywood - high level shoes only.
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

function refineEquipMenuText(items, levels, jobs) {
	let str = "";
	for (let i = 0; i < items.length; i++)
		str += "\r\n#L" + i + "# #t" + items[i] + "##k - " + jobs[i] + " Lv. " + levels[i] + "#l#b";
	return str;
}

let selection = npc.askMenu("Hello, I live here, but don't underestimate me. How about I help you by making you a new pair of shoes?\r\n#b"
		+ "#L0# Create a Warrior shoe#l\r\n"
		+ "#L1# Create a Bowman shoe#l\r\n"
		+ "#L2# Create a Magician shoe#l\r\n"
		+ "#L3# Create a Thief shoe#l\r\n");
let selStr;
let itemids;
let itemreqs;
let quantity;
let equip;
switch (selection) {
	case 0: {
		itemids = [1072051, 1072053, 1072052, 1072003, 1072039, 1072040, 1072041, 1072002, 1072112, 1072113, 1072000, 1072126, 1072127, 1072132, 1072133, 1072134, 1072135, 1072147, 1072148, 1072149];
		let shoejobs = ["Warrior", "Warrior", "Warrior", "Warrior", "Warrior", "Warrior", "Warrior", "Warrior", "Warrior", "Warrior", "Warrior", "Warrior", "Warrior", "Warrior", "Warrior", "Warrior", "Warrior", "Warrior", "Warrior", "Warrior"];
		let shoelimits = [25, 25, 25, 30, 30, 30, 30, 35, 35, 35, 40, 40, 40, 50, 50, 50, 50, 60, 60, 60];
		itemreqs = [
			[4011004, 2, 4011001, 1, 4000021, 15, 4003000, 10, MESOS, 10000],
			[4011006, 2, 4011001, 1, 4000021, 15, 4003000, 10, MESOS, 10000],
			[4021008, 1, 4011001, 2, 4000021, 20, 4003000, 10, MESOS, 12000],
			[4021003, 4, 4011001, 2, 4000021, 45, 4003000, 15, MESOS, 20000],
			[4011002, 4, 4011001, 2, 4000021, 45, 4003000, 15, MESOS, 20000],
			[4011004, 4, 4011001, 2, 4000021, 45, 4003000, 15, MESOS, 20000],
			[4021000, 4, 4011001, 2, 4000021, 45, 4003000, 15, MESOS, 20000],
			[4011001, 3, 4021004, 1, 4000021, 30, 4000030, 20, 4003000, 25, MESOS, 22000],
			[4011002, 3, 4021004, 1, 4000021, 30, 4000030, 20, 4003000, 25, MESOS, 22000],
			[4021008, 2, 4021004, 1, 4000021, 30, 4000030, 20, 4003000, 25, MESOS, 25000],
			[4011003, 4, 4000021, 100, 4000030, 40, 4003000, 30, 4000033, 100, MESOS, 38000],
			[4011005, 4, 4021007, 1, 4000030, 40, 4003000, 30, 4000042, 250, MESOS, 38000],
			[4011002, 4, 4021007, 1, 4000030, 40, 4003000, 30, 4000041, 120, MESOS, 38000],
			[4021008, 1, 4011001, 3, 4021003, 6, 4000030, 65, 4003000, 45, MESOS, 50000],
			[4021008, 1, 4011001, 3, 4011002, 6, 4000030, 65, 4003000, 45, MESOS, 50000],
			[4021008, 1, 4011001, 3, 4011005, 6, 4000030, 65, 4003000, 45, MESOS, 50000],
			[4021008, 1, 4011001, 3, 4011006, 6, 4000030, 65, 4003000, 45, MESOS, 50000],
			[4021008, 1, 4011007, 1, 4021005, 8, 4000030, 80, 4003000, 55, MESOS, 60000],
			[4021008, 1, 4011007, 1, 4011005, 8, 4000030, 80, 4003000, 55, MESOS, 60000],
			[4021008, 1, 4011007, 1, 4021000, 8, 4000030, 80, 4003000, 55, MESOS, 60000]
		];
		selection = itemMakePrompt("Warrior shoes? Sure thing, which kind?",
				itemids, itemreqs, shoelimits, shoejobs);
		break;
	}
	case 1: {
		itemids = [1072027, 1072034, 1072069, 1072079, 1072080, 1072081, 1072082, 1072083, 1072101, 1072102, 1072103, 1072118, 1072119, 1072120, 1072121, 1072122, 1072123, 1072124, 1072125, 1072144, 1072145, 1072146];
		let shoejobs = ["Bowman", "Bowman", "Bowman", "Bowman", "Bowman", "Bowman", "Bowman", "Bowman", "Bowman", "Bowman", "Bowman", "Bowman", "Bowman", "Bowman", "Bowman", "Bowman", "Bowman", "Bowman", "Bowman", "Bowman", "Bowman", "Bowman"];
		let shoelimits = [25, 25, 25, 30, 30, 30, 30, 30, 35, 35, 35, 40, 40, 40, 40, 50, 50, 50, 50, 60, 60, 60];
		itemreqs = [
			[4000021, 35, 4011000, 3, 4003000, 10, MESOS, 9000],
			[4000021, 35, 4021003, 1, 4003000, 10, MESOS, 9000],
			[4000021, 35, 4021000, 1, 4003000, 10, MESOS, 9000],
			[4000021, 50, 4021000, 2, 4003000, 15, MESOS, 19000],
			[4000021, 50, 4021005, 2, 4003000, 15, MESOS, 19000],
			[4000021, 50, 4021003, 2, 4003000, 15, MESOS, 19000],
			[4000021, 50, 4021004, 2, 4003000, 15, MESOS, 19000],
			[4000021, 50, 4021006, 2, 4003000, 15, MESOS, 19000],
			[4021002, 3, 4021006, 1, 4000030, 15, 4000021, 30, 4003000, 20, MESOS, 20000],
			[4021003, 3, 4021006, 1, 4000030, 15, 4000021, 30, 4003000, 20, MESOS, 20000],
			[4021000, 3, 4021006, 1, 4000030, 15, 4000021, 30, 4003000, 20, MESOS, 20000],
			[4021000, 4, 4003000, 30, 4000030, 45, 4000024, 20, MESOS, 32000],
			[4021006, 4, 4003000, 30, 4000030, 45, 4000027, 20, MESOS, 32000],
			[4011003, 5, 4003000, 30, 4000030, 45, 4000044, 40, MESOS, 40000],
			[4021002, 5, 4003000, 30, 4000030, 45, 4000009, 40, MESOS, 40000],
			[4011001, 3, 4021006, 3, 4021008, 1, 4000030, 60, 4003000, 35, 4000033, 80, MESOS, 50000],
			[4011001, 3, 4021006, 3, 4021008, 1, 4000030, 60, 4003000, 35, 4000032, 150, MESOS, 50000],
			[4011001, 3, 4021006, 3, 4021008, 1, 4000030, 60, 4003000, 35, 4000041, 100, MESOS, 50000],
			[4011001, 3, 4021006, 3, 4021008, 1, 4000030, 60, 4003000, 35, 4000042, 250, MESOS, 50000],
			[4011006, 5, 4021000, 8, 4021007, 1, 4000030, 75, 4003000, 50, MESOS, 60000],
			[4011006, 5, 4021005, 8, 4021007, 1, 4000030, 75, 4003000, 50, 60000],
			[4011006, 5, 4021003, 8, 4021007, 1, 4000030, 75, 4003000, 50, 60000]
		];
		selection = itemMakePrompt("Bowman shoes? Sure thing, which kind?",
				itemids, itemreqs, shoelimits, shoejobs);
		break;
	}
	case 2: {
		itemids = [1072019, 1072020, 1072021, 1072072, 1072073, 1072074, 1072075, 1072076, 1072077, 1072078, 1072089, 1072090, 1072091, 1072114, 1072115, 1072116, 1072117, 1072140, 1072141, 1072142, 1072143, 1072136, 1072137, 1072138, 1072139];
		let shoejobs = ["Magician", "Magician", "Magician", "Magician", "Magician", "Magician", "Magician", "Magician", "Magician", "Magician", "Magician", "Magician", "Magician", "Magician", "Magician", "Magician", "Magician", "Magician", "Magician", "Magician", "Magician", "Magician", "Magician", "Magician", "Magician"];
		let shoelimits = [20, 20, 20, 25, 25, 25, 30, 30, 30, 30, 35, 35, 35, 40, 40, 40, 40, 50, 50, 50, 50, 60, 60, 60, 60];
		itemreqs = [
			[4021005, 1, 4000021, 30, 4003000, 5, MESOS, 3000],
			[4021001, 1, 4000021, 30, 4003000, 5, MESOS, 3000],
			[4021000, 1, 4000021, 30, 4003000, 5, MESOS, 3000],
			[4011004, 1, 4000021, 35, 4003000, 10, MESOS, 8000],
			[4021006, 1, 4000021, 35, 4003000, 10, MESOS, 8000],
			[4021004, 1, 4000021, 35, 4003000, 10, MESOS, 8000],
			[4021000, 2, 4000021, 50, 4003000, 15, MESOS, 18000],
			[4021002, 2, 4000021, 50, 4003000, 15, MESOS, 18000],
			[4011004, 2, 4000021, 50, 4003000, 15, MESOS, 18000],
			[4021008, 1, 4000021, 50, 4003000, 15, MESOS, 18000],
			[4021001, 3, 4021006, 1, 4000021, 30, 4000030, 15, 4003000, 20, MESOS, 20000],
			[4021000, 3, 4021006, 1, 4000021, 30, 4000030, 15, 4003000, 20, MESOS, 20000],
			[4021008, 2, 4021006, 1, 4000021, 40, 4000030, 25, 4003000, 20, MESOS, 22000],
			[4021000, 4, 4000030, 40, 4000043, 35, 4003000, 25, MESOS, 30000],
			[4021005, 4, 4000030, 40, 4000037, 70, 4003000, 25, MESOS, 30000],
			[4011006, 2, 4021007, 1, 4000030, 40, 4000027, 20, 4003000, 25, MESOS, 35000],
			[4021008, 2, 4021007, 1, 4000030, 40, 4000014, 30, 4003000, 30, MESOS, 40000],
			[4021009, 1, 4011006, 3, 4021000, 3, 4000030, 60, 4003000, 40, MESOS, 50000],
			[4021009, 1, 4011006, 3, 4021005, 3, 4000030, 60, 4003000, 40, MESOS, 50000],
			[4021009, 1, 4011006, 3, 4021001, 3, 4000030, 60, 4003000, 40, MESOS, 50000],
			[4021009, 1, 4011006, 3, 4021003, 3, 4000030, 60, 4003000, 40, MESOS, 50000],
			[4021009, 1, 4011006, 4, 4011005, 5, 4000030, 70, 4003000, 50, MESOS, 60000],
			[4021009, 1, 4011006, 4, 4021003, 5, 4000030, 70, 4003000, 50, MESOS, 60000],
			[4021009, 1, 4011006, 4, 4011003, 5, 4000030, 70, 4003000, 50, MESOS, 60000],
			[4021009, 1, 4011006, 4, 4021002, 5, 4000030, 70, 4003000, 50, MESOS, 60000]
		];
		selection = itemMakePrompt("Magician shoes? Sure thing, which kind?",
				itemids, itemreqs, shoelimits, shoejobs);
		break;
	}
	case 3: {
		itemids = [1072084, 1072085, 1072086, 1072087, 1072032, 1072033, 1072035, 1072036, 1072104, 1072105, 1072106, 1072107, 1072108, 1072109, 1072110, 1072128, 1072130, 1072129, 1072131, 1072150, 1072151, 1072152];
		let shoejobs = ["Thief", "Thief", "Thief", "Thief", "Thief", "Thief", "Thief", "Thief", "Thief", "Thief", "Thief", "Thief", "Thief", "Thief", "Thief", "Thief", "Thief", "Thief", "Thief", "Thief", "Thief", "Thief"];
		let shoelimits = [25, 25, 25, 25, 30, 30, 30, 30, 35, 35, 35, 40, 40, 40, 40, 50, 50, 50, 50, 60, 60, 60];
		itemreqs = [
			[4021005, 1, 4000021, 35, 4003000, 10, MESOS, 9000],
			[4021000, 1, 4000021, 35, 4003000, 10, MESOS, 9000],
			[4021003, 1, 4000021, 35, 4003000, 10, MESOS, 9000],
			[4021004, 1, 4000021, 35, 4003000, 10, MESOS, 9000],
			[4011000, 3, 4000021, 50, 4003000, 15, MESOS, 19000],
			[4011001, 3, 4000021, 50, 4003000, 15, MESOS, 19000],
			[4011004, 2, 4000021, 50, 4003000, 15, MESOS, 19000],
			[4011006, 2, 4000021, 50, 4003000, 15, MESOS, 21000],
			[4021000, 3, 4021004, 1, 4000021, 30, 4000030, 15, 4003000, 20, MESOS, 20000],
			[4021003, 3, 4021004, 1, 4000021, 30, 4000030, 15, 4003000, 20, MESOS, 20000],
			[4021002, 3, 4021004, 1, 4000021, 30, 4000030, 15, 4003000, 20, MESOS, 20000],
			[4021000, 5, 4000030, 45, 4000033, 50, 4003000, 30, MESOS, 40000],
			[4021003, 4, 4000030, 45, 4000032, 30, 4003000, 30, MESOS, 32000],
			[4021006, 4, 4000030, 45, 4000040, 3, 4003000, 30, MESOS, 35000],
			[4021005, 4, 4000030, 45, 4000037, 70, 4003000, 30, MESOS, 35000],
			[4011007, 2, 4021005, 3, 4000030, 50, 4000037, 200, 4003000, 35, MESOS, 50000],
			[4011007, 2, 4021000, 3, 4000030, 50, 4000043, 15, 4003000, 35, MESOS, 50000],
			[4011007, 2, 4021003, 3, 4000030, 50, 4000045, 80, 4003000, 35, MESOS, 50000],
			[4011007, 2, 4021001, 3, 4000030, 50, 4000036, 80, 4003000, 35, MESOS, 50000],
			[4021008, 1, 4011007, 1, 4021005, 8, 4000030, 75, 4003000, 50, MESOS, 60000],
			[4021008, 1, 4011007, 1, 4011005, 5, 4000030, 75, 4003000, 50, MESOS, 60000],
			[4021008, 1, 4011007, 1, 4021000, 1, 4000030, 75, 4003000, 50, MESOS, 60000]
		];
		selection = itemMakePrompt("Thief shoes? Sure thing, which kind?",
				itemids, itemreqs, shoelimits, shoejobs);
		break;
	}
}

if (selection != -1) {
	let okay = true;
	let item = itemids[selection];
	let reqs = itemreqs[selection];
	for (let i = 0; i < reqs.length && okay; i += 2) {
		if (reqs[i] != MESOS) {
			if (!player.hasItem(reqs[i], reqs[i + 1]))
				okay = false;
		} else if (!player.hasMesos(reqs[i + 1])) {
			okay = false;
		}
	}
	if (!player.canGainItem(item, 1))
		okay = false;

	if (!okay) {
		npc.sayNext("Sorry, but I have to have those items to get this exactly right. Perhaps next time.");
	} else {
		for (let i = 0; i < reqs.length; i += 2)
			if (reqs[i] != MESOS)
				player.loseItem(reqs[i], reqs[i + 1]);
			else
				player.loseMesos(reqs[i + 1]);
		player.gainItem(item, 1);
		npc.sayNext("There, the shoes are ready. Be careful not to trip!");
	}
}