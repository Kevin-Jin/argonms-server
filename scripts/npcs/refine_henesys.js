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
 * Vicious: Item Maker (NPC 1012002)
 * Victoria Road: Henesys Market (Map 100000100)
 *
 * Refining NPC for bowmen.
 *
 * @author GoldenKevin (content from KiniroMS r227)
 */

function refineEquipMenuText(items, levels) {
	let str = "";
	for (let i = 0; i < items.length; i++)
		str += "\r\n#L" + i + "##z" + items[i] + "##k - Bowman Lv. " + levels[i] + "#l#b";
	return str;
}

let selection = npc.askMenu("Hello. I am Vicious, retired Sniper. However, I used to be the top student of Athena Pierce. Though I no longer hunt, I can make some archer items that will be useful for you...\r\n#b"
		+ "#L0#Create a bow#l\r\n"
		+ "#L1#Create a crossbow#l\r\n"
		+ "#L2#Make a glove#l\r\n"
		+ "#L3#Upgrade a glove#l\r\n"
		+ "#L4#Create materials#l\r\n"
		+ "#L5#Create Arrows#l");
let selStr;
let itemSet, matSet, matQtySet, costSet;
let qty = null;
switch (selection) {
	case 0: {
		itemSet = [1452002, 1452003, 1452001, 1452000, 1452005, 1452006, 1452007];
		let levelSet = [10, 15, 20, 25, 30, 35, 40];
		matSet = [[4003001, 4000000], [4011001, 4003000], [4003001, 4000016], [4011001, 4021006, 4003000], [4011001, 4011006, 4021003, 4021006, 4003000], [4011004, 4021000, 4021004, 4003000], [4021008, 4011001, 4011006, 4003000, 4000014]];
		matQtySet = [[5, 30], [1, 3], [30, 50], [2, 2, 8], [5, 5, 3, 3, 30], [7, 6, 3, 35], [1, 10, 3, 40, 50]];
		costSet = [800, 2000, 3000, 5000, 30000, 40000, 80000];

		selStr = "I may have been a Sniper, but bows and crossbows aren't too much different. Anyway, which would you like to make?#b" + refineEquipMenuText(itemSet, levelSet);
		qty = 1;
		break;
	}
	case 1: {
		itemSet = [1462001, 1462002, 1462003, 1462000, 1462004, 1462005, 1462006, 1462007];
		let levelSet = [12, 18, 22, 28, 32, 38, 42, 50];
		matSet = [[4003001, 4003000], [4011001, 4003001, 4003000], [4011001, 4003001, 4003000], [4011001, 4021006, 4021002, 4003000], [4011001, 4011005, 4021006, 4003001, 4003000], [4021008, 4011001, 4011006, 4021006, 4003000], [4021008, 4011004, 4003001, 4003000], [4021008, 4011006, 4021006, 4003001, 4003000]];
		matQtySet = [[7, 2], [1, 20, 5], [1, 50, 8], [2, 1, 1, 10], [5, 5, 3, 50, 15], [1, 8, 4, 2, 30], [2, 6, 30, 30], [2, 5, 3, 40, 40]];
		costSet = [1000, 2000, 3000, 10000, 30000, 50000, 80000, 200000];

		selStr = "I was a Sniper. Crossbows are my specialty. Which would you like me to make for you?#b" + refineEquipMenuText(itemSet, levelSet);
		qty = 1;
		break;
	}
	case 2: {
		itemSet = [1082012, 1082013, 1082016, 1082048, 1082068, 1082071, 1082084, 1082089];
		let levelSet = [15, 20, 25, 30, 35, 40, 50, 60];
		matSet = [[4000021, 4000009], [4000021, 4000009, 4011001], [4000021, 4000009, 4011006], [4000021, 4011006, 4021001], [4011000, 4011001, 4000021, 4003000], [4011001, 4021000, 4021002, 4000021, 4003000], [4011004, 4011006, 4021002, 4000030, 4003000], [4011006, 4011007, 4021006, 4000030, 4003000]];
		matQtySet = [[15, 20], [20, 20, 2], [40, 50, 2], [50, 2, 1], [1, 3, 60, 15], [3, 1, 3, 80, 25], [3, 1, 2, 40, 35], [2, 1, 8, 50, 50]];
		costSet = [5000, 10000, 15000, 20000, 30000, 40000, 50000, 70000];

		selStr = "Okay, so which glove do you want me to make?#b" + refineEquipMenuText(itemSet, levelSet);
		qty = 1;
		break;
	}
	case 3: {
		itemSet = [1082015, 1082014, 1082017, 1082018, 1082049, 1082050, 1082069, 1082070, 1082072, 1082073, 1082085, 1082083, 1082090, 1082091];
		let levelSet = [20, 20, 25, 25, 30, 30, 35, 35, 40, 40, 50, 50, 60, 60];
		matSet = [[1082013, 4021003], [1082013, 4021000], [1082016, 4021000], [1082016, 4021008], [1082048, 4021003], [1082048, 4021008], [1082068, 4011002], [1082068, 4011006], [1082071, 4011006], [1082071, 4021008], [1082084, 4011000, 4021000], [1082084, 4011006, 4021008], [1082089, 4021000, 4021007], [1082089, 4021007, 4021008]];
		matQtySet = [[1, 2], [1, 1], [1, 3], [1, 1], [1, 3], [1, 1], [1, 4], [1, 2], [1, 4], [1, 2], [1, 1, 5], [1, 2, 2], [1, 5, 1], [1, 2, 2]];
		costSet = [7000, 7000, 10000, 12000, 15000, 20000, 22000, 25000, 30000, 40000, 55000, 60000, 70000, 80000];

		selStr = "Upgrade a glove? That shouldn't be too difficult. Which did you have in mind?#b" + refineEquipMenuText(itemSet, levelSet);
		qty = 1;
		break;
	}
	case 4:
		itemSet = [4003001, 4003001, 4003000];
		matSet = [4000003, 4000018, [4011000, 4011001]];
		matQtySet = [10, 5, [1, 1]];
		costSet = [0, 0, 0];

		selStr = "Materials? I know of a few materials that I can make for you...\r\n#b"
				+ "#L0#Make #t" + itemSet[0] + "# with #t4000003##l\r\n"
				+ "#L1#Make #t" + itemSet[1] + "# with #t4000018##l\r\n"
				+ "#L2#Make #t" + itemSet[2] + "# (packs of 15)#l";
		break;
	case 5:
		itemSet = [2060000, 2061000, 2060001, 2061001, 2060002, 2061002];
		matSet = [[4003001, 4003004], [4003001, 4003004], [4011000, 4003001, 4003004], [4011000, 4003001, 4003004], [4011001, 4003001, 4003005], [4011001, 4003001, 4003005]];
		matQtySet = [[1, 1], [1, 1], [1, 3, 10], [1, 3, 10], [1, 5, 15], [1, 5, 15]];
		costSet = [0, 0, 0, 0, 0, 0];

		selStr = "Arrows? Not a problem at all.\r\n#b"
				+ "#L0##t" + itemSet[0] + "##l\r\n"
				+ "#L1##t" + itemSet[1] + "##l\r\n"
				+ "#L2##t" + itemSet[2] + "##l\r\n"
				+ "#L3##t" + itemSet[3] + "##l\r\n"
				+ "#L4##t" + itemSet[4] + "##l\r\n"
				+ "#L5##t" + itemSet[5] + "##l";
		qty = 1;
		break;
}

selection = npc.askMenu(selStr);

let item = itemSet[selection];
if (qty == null)
	qty = npc.askNumber("So, you want me to make some #t" + item + "#s? In that case, how many do you want me to make?", 1, 1, 100);
let mats = matSet[selection];
let matQty = matQtySet[selection];
let cost = costSet[selection] * qty;

let prompt = "You want me to make " + (qty == 1 ? "a" : qty) + " #t" + item + "#?"
		+ " In that case, I'm going to need specific items from you in order to make it. Make sure you have room in your inventory, though!#b";

if (mats instanceof Array)
	for (let i = 0; i < mats.length; i++)
		prompt += "\r\n#i" + mats[i] + "# " + matQty[i] * qty + " #t" + mats[i] + "#";
else
	prompt += "\r\n#i" + mats + "# " + matQty * qty + " #t" + mats + "#";
if (cost > 0)
	prompt += "\r\n#i4031138# " + cost + " meso";

selection = npc.askYesNo(prompt);
if (selection == 1) {
	if (player.hasMesos(cost)) {
		let complete = true;
		if (mats instanceof Array) {
			for (let i = 0; complete && i < mats.length; i++)
				if (!player.hasItem(mats[i], matQty[i] * qty))
					complete = false;
		} else {
			if (!player.hasItem(mats, matQty * qty))
				complete = false;
		}

		if (complete) {
			let giveQty;
			if (item >= 2060000 && item <= 2060002)
				giveQty = 1000 - (item - 2060000) * 100;
			else if (item >= 2061000 && item <= 2061002)
				giveQty = 1000 - (item - 2061000) * 100;
			else if (item == 4003000)
				giveQty = qty * 15;
			else
				giveQty = 1;

			if (player.canGainItem(item, giveQty)) {
				if (mats instanceof Array)
					for (let i = 0; i < mats.length; i++)
						player.loseItem(mats[i], matQty[i] * qty);
				else
					player.loseItem(mats, matQty * qty);
				if (cost > 0)
					player.loseMesos(cost);

				player.gainItem(item, giveQty);
				npc.say("A perfect item, as usual. Come and see me if you need anything else.");
			} else {
				npc.say("Please make sure you have room in your inventory, and talk to me again.");
			}
		} else {
			npc.say("Surely you, of all people, would understand the value of having quality items? I can't do that without the items I require.");
		}
	} else {
		npc.say("Sorry, but this is how I make my living. No meso, no item.");
	}
}