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
 * Chris: Ore Refiner (NPC 1052003)
 * Victoria Road: Kerning City Repair Shop (Map 103000006)
 *
 * Ore refiner.
 * Refining NPC for thieves - Bronze Gigantic claw upgrades only.
 *
 * @author GoldenKevin (content from KiniroMS r227)
 */

let MESOS = 4031138;

let selection = npc.askMenu("Yes, I do own this forge. If you're willing to pay, I can offer you some of my services.\r\n#b"
		+ "#L0# Refine a mineral ore#l\r\n"
		+ "#L1# Refine a jewel ore#l\r\n"
		+ "#L2# I have Iron Hog's Metal Hoof...#l\r\n"
		+ "#L3# Upgrade a claw#l");
let itemids;
let itemreqs;
let quantity;
let str;
switch (selection) {
	case 0: {
		itemids = [4011000, 4011001, 4011002, 4011003, 4011004, 4011005, 4011006];
		let mineralnames = ["Bronze", "Steel", "Mithril", "Adamantium", "Silver", "Orihalcon", "Gold"];
		itemreqs = [
			[4010000, 10, MESOS, 300],
			[4010001, 10, MESOS, 300],
			[4010002, 10, MESOS, 300],
			[4010003, 10, MESOS, 500],
			[4010004, 10, MESOS, 500],
			[4010005, 10, MESOS, 500],
			[4010006, 10, MESOS, 800]
		];
		str = "So, what kind of mineral ore would you like to refine?#b";
		for (let i = 0; i < itemids.length; i++)
			str += ("\r\n#L" + i + "# " + mineralnames[i] + "#l");
		selection = npc.askMenu(str);

		quantity = npc.askNumber("So, you want me to make some #t" + itemids[selection] + "#s? In that case, how many do you want me to make?", 1, 1, 100);
		break;
	}
	case 1: {
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
		str = "So, what kind of jewel ore would you like to refine?#b";
		for (let i = 0; i < itemids.length; i++)
			str += ("\r\n#L" + i + "# #t" + itemids[i] + "##l");
		selection = npc.askMenu(str);

		quantity = npc.askNumber("So, you want me to make some #t" + itemids[selection] + "#s? In that case, how many do you want me to make?", 1, 1, 100);
		break;
	}
	case 2:
		itemids = [4011001];
		itemreqs = [[4000039, 100, MESOS, 1000]];
		selection = npc.askYesNo("You know about that? Not many people realize the potential in the #t4000039#... I can make this into something special, if you want me to.") - 1;

		if (selection != -1)
			quantity = npc.askNumber("So, you want me to make some #t4011001#s? In that case, how many do you want me to make?", 1, 1, 100);
		break;
	case 3: {
		itemids = [1472023, 1472024, 1472025];
		let clawjobs = ["Thief", "Thief", "Thief"];
		let clawlimits = [60, 60, 60];
		itemreqs = [
			[1472022, 1, 4011007, 1, 4021000, 8, 2012000, 10, MESOS, 80000],
			[1472022, 1, 4011007, 1, 4021005, 8, 2012002, 10, MESOS, 80000],
			[1472022, 1, 4011007, 1, 4021008, 3, 4000046, 5, MESOS, 100000]
		];
		str = "Ah, you wish to upgrade a claw? Then tell me, which one?";
		for (let i = 0; i < itemids.length; i++)
			str += "\r\n#L" + i + "##b #t" + itemids[i] + "##k - " + clawjobs[i] + " Lv. " + clawlimits[i] + "#l";
		selection = npc.askMenu(str);

		quantity = 1;
		break;
	}
}

if (selection != -1) {
	let item = itemids[selection];
	let reqs = itemreqs[selection];
	str = "You want me to make " + (quantity == 1 ? "a" : quantity) + " #t" + item + "#? In that case, I'm going to need specific items from you in order to make it. Make sure you have room in your inventory, though!\r\n";
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
	selection = npc.askYesNo(str);
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
		if (!player.canGainItem(item, quantity))
			okay = false;
		if (!okay) {
			npc.say("I cannot accept substitutes. If you don't have what I need, then I won't be able to help you.");
		} else {
			for (let i = 0; i < reqs.length; i += 2)
				if (reqs[i] != MESOS)
					player.loseItem(reqs[i], reqs[i + 1] * quantity);
				else
					player.loseMesos(reqs[i + 1] * quantity);
			player.gainItem(item, quantity);
			npc.say("Phew... I almost didn't think that would work for a second... Well, I hope you enjoy it, anyway.");
		}
	}
}