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
 * Serryl: Item Maker (NPC 1091003)
 * The Nautilus: Mid Floor - Hallway (Map 120000200)
 *
 * Refining NPC for pirates.
 *
 * @author GoldenKevin (content from KiniroMS r227)
 */

function refineEquipMenuText(items, levels) {
	let str = "";
	for (let i = 0; i < items.length; i++)
		str += "\r\n#L" + i + "##t" + items[i] + "# (Level limit: " + levels[i] + ", Pirate)#l";
	return str;
}

let selection = npc.askMenu("What? You want to make your own weapons and gloves? Seriously... it's tough to do it by yourself if you don't have experience... I'll help you out. I've been a pirate for 20 years, and for 20 years I have made various items for the crew here. It's easy for me.\r\n"
		+ "#L0# Make a Knuckler#l\r\n"
		+ "#L1# Make a Gun#l\r\n"
		+ "#L2# Make a pair of gloves#l");
let selStr;
let itemSet, matSet, matQtySet, costSet, levelSet;
switch (selection) {
	case 0:
		itemSet = [1482001, 1482002, 1482003, 1482004, 1482005, 1482006, 1482007];
		matSet = [[4000021], [4011001, 4011000, 4000021, 4003000], [4011000, 4011001, 4003000], [4011000, 4011001, 4000021, 4003000], [4011000, 4011001, 4000021, 4003000], [4011000, 4011001, 4021000, 4000021, 4003000], [4000039, 4011000, 4011001, 4000030, 4000021, 4003000]];
		matQtySet = [[20], [1, 1, 10, 5], [2, 1, 10], [1, 1, 30, 10], [2, 2, 30, 20], [1, 1, 2, 50, 20], [150, 1, 2, 20, 20, 20]];
		costSet = [1000, 2000, 5000, 15000, 30000, 50000, 100000];
		levelSet = [15, 20, 25, 30, 35, 40, 50];

		selStr = "As long as you bring in the materials required, I'll make you a fine Knuckler. Which Knuckler would you like to make?" + refineEquipMenuText(itemSet, levelSet);
		break;
	case 1:
		itemSet = [1492001, 1492002, 1492003, 1492004, 1492005, 1492006, 1492007];
		matSet = [[4011000, 4003000, 4003001], [4011000, 4003000, 4003001, 4000021], [4011000, 4003000], [4011001, 4000021, 4003000], [4011006, 4011001, 4000021, 4003000], [4011004, 4011001, 4000021, 4003000], [4011006, 4011004, 4011001, 4000030, 4003000]];
		matQtySet = [[1, 5, 1], [1, 10, 5, 10], [2, 10], [2, 10, 10], [10, 2, 5, 10], [1, 2, 10, 20], [1, 2, 4, 30, 30]];
		costSet = [1000, 2000, 5000, 15000, 30000, 50000, 100000];
		levelSet = [15, 20, 25, 30, 35, 40, 50];

		selStr = "As long as you bring in the materials required, I'll make you a fine Gun. Which Gun would you like to make?" + refineEquipMenuText(itemSet, levelSet);
		break;
	case 2:
		itemSet = [1082180, 1082183, 1082186, 1082189, 1082192, 1082195, 1082198, 1082201];
		matSet = [[4000021, 4021003], [4000021], [4011000, 4000021], [4021006, 4000021, 4003000], [4011000, 4000021, 4003000], [4000021, 4011000, 4011001, 4003000], [4011000, 4000021, 4000030, 4003000], [4011007, 4021008, 4021007, 4000030, 4003000]];
		matQtySet = [[15, 1], [35], [2, 20], [2, 50, 10], [3, 60, 15], [80, 3, 3, 25], [3, 20, 40, 30], [1, 1, 1, 50, 50]];
		costSet = [1000, 8000, 15000, 25000, 30000, 40000, 50000, 70000];
		levelSet = [15, 20, 25, 30, 35, 40, 50, 60];

		selStr = "As long as you bring in the materials required, I'll make you a fine glove. Which glove would you like to make?" + refineEquipMenuText(itemSet, levelSet);
		break;
}

selection = npc.askMenu(selStr);

let item = itemSet[selection];
let levelLimit = levelSet[selection];

let prompt = "Making one #t" + item + "# requires the items listed below. The level limit for this item is " + levelLimit + ", so check and make sure you really need this item before getting it. What do you think? Do you really want one?\r\n";

for (let i = 0; i < mats.length; i++)
	prompt += "\r\n#i" + mats[i] + "# " + matQty[i] + " #t" + mats[i] + "#";
if (cost > 0)
	prompt += "\r\n#i4031138# " + cost + " meso";

selection = npc.askYesNo(prompt);
if (selection == 1) {
	let complete = true;
	for (let i = 0; complete && i < mats.length; i++)
		if (!player.hasItem(mats[i], matQty[i]))
			complete = false;
	if (complete)
		complete = player.hasMesos(cost);
	if (complete)
		complete = player.canGainItem(item, 1);

	if (!complete) {
		npc.sayNext("Check and make sure you have all the necessary items to make this. Also, make sure your Equips inventory has room. I can't give you the item if your inventory is full, you know.");
	} else {
		for (let i = 0; i < mats.length; i++)
			player.loseItem(mats[i], matQty[i]);
		if (cost > 0)
			player.loseMesos(cost);

		player.gainItem(item, 1);
		npc.say("All done. If you need anything else... Well, I'm not going anywhere.");
	}
}