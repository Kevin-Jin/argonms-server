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
 * JM From tha Streetz: Item Creator (NPC 1052002)
 * Victoria Road: Kerning City (Map 103000000)
 *
 * Refining NPC for thieves.
 *
 * @author GoldenKevin (content from KiniroMS r227)
 */

let MESOS = 4031138;

function confirm(giveCount, quantity, item, reqs) {
	let str = "You want me to make " + (quantity == 1 ? "a" : quantity) + " #t" + item + "#? In that case, I'm going to need specific items from you in order to make it. Make sure you have room in your inventory, though!\r\n";
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
	let selection = npc.askYesNo(str);
	if (selection == 1) {
		let okay = true;
		for (let i = 0; i < reqs.length && okay; i += 2) {
			if (reqs[i] != MESOS) {
				if (!player.hasItem(reqs[i], reqs[i + 1] * quantity))
					okay = false;
			} else if (!player.hasMesos(reqs[i + 1] * quantity)) {
				okay = false;
			}
		}
		if (!player.canGainItem(item, giveCount * quantity))
			okay = false;
		if (!okay) {
			npc.say("What are you trying to pull? I can't make anything unless you bring me what I ask for.");
		} else {
			for (let i = 0; i < reqs.length; i += 2)
				if (reqs[i] != MESOS)
					player.loseItem(reqs[i], reqs[i + 1] * quantity);
				else
					player.loseMesos(reqs[i + 1] * quantity);
			player.gainItem(item, giveCount * quantity);
			npc.say("All done. If you need anything else... Well, I'm not going anywhere.");
		}
	}
}

let selection = npc.askMenu("Pst... If you have the right goods, I can turn it into something niice...\r\n#b"
		+ "#L0# Create a glove#l\r\n"
		+ "#L1# Upgrade a glove#l\r\n"
		+ "#L2# Create a claw#l\r\n"
		+ "#L3# Upgrade a claw#l\r\n"
		+ "#L4# Create materials#l");
let itemids;
let itemreqs;
let equip;
switch (selection) {
	case 0: {
		itemids = [1082002, 1082029, 1082030, 1082031, 1082032, 1082037, 1082042, 1082046, 1082075, 1082065, 1082092];
		let glovejobs = ["Common", "Thief", "Thief", "Thief", "Thief", "Thief", "Thief", "Thief", "Thief", "Thief", "Thief"];
		let glovelimits = [10, 15, 15, 15, 20, 25, 30, 35, 40, 50, 60];
		itemreqs = [
			[4000021, 15, MESOS, 1000],
			[4000021, 30, 4000018, 20, MESOS, 7000],
			[4000021, 30, 4000015, 20, MESOS, 7000],
			[4000021, 30, 4000020, 20, MESOS, 7000],
			[4011000, 2, 4000021, 40, MESOS, 10000],
			[4011000, 2, 4011001, 1, 4000021, 10, MESOS, 15000],
			[4011001, 2, 4000021, 50, 4003000, 10, MESOS, 25000],
			[4011001, 3, 4011000, 1, 4000021, 60, 4003000, 15, MESOS, 30000],
			[4021000, 3, 4000014, 200, 4000021, 80, 4003000, 30, MESOS, 40000],
			[4021005, 3, 4021008, 1, 4000030, 40, 4003000, 30, MESOS, 50000],
			[4011007, 1, 4011000, 8, 4021007, 1, 4000030, 50, 4003000, 50, MESOS, 70000]
		];
		let str = "So, what kind of glove would you like me to make?";
		for (let i = 0; i < itemids.length; i++)
			str += "\r\n#L" + i + "##b #t" + itemids[i] + "##k - " + glovejobs[i] + " Lv. " + glovelimits[i] + "#l";
		selection = npc.askMenu(str);
		equip = true;
		break;
	}
	case 1: {
		itemids = [1082033, 1082034, 1082038, 1082039, 1082043, 1082044, 1082047, 1082045, 1082076, 1082074, 1082067, 1082066, 1082093, 1082094];
		let glovejobs = ["Thief", "Thief", "Thief", "Thief", "Thief", "Thief", "Thief", "Thief", "Thief", "Thief", "Thief", "Thief", "Thief", "Thief"];
		let glovelimits = [20, 20, 25, 25, 30, 30, 35, 35, 40, 40, 50, 50, 60, 60];
		itemreqs = [
			[1082032, 1, 4011002, 1, MESOS, 5000],
			[1082032, 1, 4021004, 1, MESOS, 7000],
			[1082037, 1, 4011002, 2, MESOS, 10000],
			[1082037, 1, 4021004, 2, MESOS, 12000],
			[1082042, 1, 4011004, 2, MESOS, 15000],
			[1082042, 1, 4011006, 1, MESOS, 20000],
			[1082046, 1, 4011005, 3, MESOS, 22000],
			[1082046, 1, 4011006, 2, MESOS, 25000],
			[1082075, 1, 4011006, 4, MESOS, 40000],
			[1082075, 1, 4021008, 2, MESOS, 50000],
			[1082065, 1, 4021000, 5, MESOS, 55000],
			[1082065, 1, 4011006, 2, 4021008, 1, MESOS, 60000],
			[1082092, 1, 4011001, 7, 4000014, 200, MESOS, 70000],
			[1082092, 1, 4011006, 7, 4000027, 150, MESOS, 80000]
		];
		let str = "An upgraded glove? Sure thing, but note that upgrades won't carry over to the new item...";
		for (let i = 0; i < itemids.length; i++)
			str += "\r\n#L" + i + "##b #t" + itemids[i] + "##k - " + glovejobs[i] + " Lv. " + glovelimits[i] + "#l";
		selection = npc.askMenu(str);
		equip = true;
		break;
	}
	case 2: {
		itemids = [1472001, 1472004, 1472007, 1472008, 1472011, 1472014, 1472018];
		let clawjobs = ["Thief", "Thief", "Thief", "Thief", "Thief", "Thief", "Thief"];
		let clawlimits = [15, 20, 25, 30, 35, 40, 50];
		itemreqs = [
			[4011001, 1, 4000021, 20, 4003000, 5, MESOS, 2000],
			[4011000, 2, 4011001, 1, 4000021, 30, 4003000, 10, MESOS, 3000],
			[1472000, 1, 4011001, 3, 4000021, 20, 4003001, 30, MESOS, 5000],
			[4011000, 3, 4011001, 2, 4000021, 50, 4003000, 20, MESOS, 15000],
			[4011000, 4, 4011001, 2, 4000021, 80, 4003000, 25, MESOS, 30000],
			[4011000, 3, 4011001, 2, 4000021, 100, 4003000, 30, MESOS, 240000],
			[4011000, 4, 4011001, 2, 4000030, 40, 4003000, 35, MESOS, 50000]
		];
		let str = "So, what kind of claw would you like me to make?";
		for (let i = 0; i < itemids.length; i++)
			str += "\r\n#L" + i + "##b #t" + itemids[i] + "##k - " + clawjobs[i] + " Lv. " + clawlimits[i] + "#l";
		selection = npc.askMenu(str);
		equip = true;
		break;
	}
	case 3: {
		itemids = [1472002, 1472003, 1472005, 1472006, 1472009, 1472010, 1472012, 1472013, 1472015, 1472016, 1472017, 1472019, 1472020];
		let clawjobs = ["Thief", "Thief", "Thief", "Thief", "Thief", "Thief", "Thief", "Thief", "Thief", "Thief", "Thief", "Thief", "Thief"];
		let clawlimits = [15, 15, 20, 20, 30, 30, 35, 35, 40, 40, 40, 50, 50];
		itemreqs = [
			[1472001, 1, 4011002, 1, MESOS, 1000],
			[1472001, 1, 4011006, 1, MESOS, 2000],
			[1472004, 1, 4011001, 2, MESOS, 3000],
			[1472004, 1, 4011003, 2, MESOS, 5000],
			[1472008, 1, 4011002, 3, MESOS, 10000],
			[1472008, 1, 4011003, 3, MESOS, 15000],
			[1472011, 1, 4011004, 4, MESOS, 20000],
			[1472011, 1, 4021008, 1, MESOS, 25000],
			[1472014, 1, 4021000, 5, MESOS, 30000],
			[1472014, 1, 4011003, 5, MESOS, 30000],
			[1472014, 1, 4021008, 2, MESOS, 35000],
			[1472018, 1, 4021000, 6, MESOS, 40000],
			[1472018, 1, 4021005, 6, MESOS, 40000]
		];
		let str = "An upgraded claw? Sure thing, but note that upgrades won't carry over to the new item...";
		for (let i = 0; i < itemids.length; i++)
			str += "\r\n#L" + i + "##b #t" + itemids[i] + "##k - " + clawjobs[i] + " Lv. " + clawlimits[i] + "#l";
		selection = npc.askMenu(str);
		equip = true;
		break;
	}
	case 4: {
		itemids = [4003001, 4003001, 4003000];
		let itemcounts = [1, 1, 15];
		itemreqs = [
			[4000003, 10],
			[4000018, 5],
			[4011000, 1, 4011001, 1]
		];
		selection = npc.askMenu("Materials? I know of a few materials that I can make for you...\r\n#b"
				+ "#L0# Make #t" + itemids[0] + "# with #t" + itemreqs[0][0] + "#\r\n"
				+ "#L1# Make #t" + itemids[1] + "# with #t" + itemreqs[1][0] + "#\r\n"
				+ "#L2# Make #t" + itemids[2] + "#s (packs of " + itemcounts[2] + ")");
		let quantity = npc.askNumber("So, you want me to make some #t" + itemids[selection] + "#s? In that case, how many do you want me to make?", 1, 1, 100);
		equip = false;
		let item = itemids[selection];
		let reqs = itemreqs[selection];
		let giveCount = itemcounts[selection];
		confirm(giveCount, quantity, item, reqs);
		break;
	}
}
if (equip) {
	let item = itemids[selection];
	let reqs = itemreqs[selection];
	confirm(1, 1, item, reqs);
}