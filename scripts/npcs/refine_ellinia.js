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
 * Francois: Item Creator (NPC 1032002)
 * Victoria Road: Ellinia (Map 101000000)
 *
 * Refining NPC for magicians.
 *
 * @author GoldenKevin (content from Vana r3171)
 */

let MESOS = 4031138;

function itemMakePrompt(str, itemids, itemreqs, itemlimits, itemjobs) {
	for (let i = 0; i < itemids.length; i++)
		str += ("\r\n#L" + i + "##b #t" + itemids[i] + "##k(Level limit : " + itemlimits[i] + ", " + itemjobs[i] + ")#l");
	let selection = npc.askMenu(str);

	let item = itemids[selection];
	let reqs = itemreqs[selection];
	let limit = itemlimits[selection];

	str = "To make one #t" + item + "#, you'll need these items below. The level limit for the item will be " + limit + " so please check and see if you really need the item, first of all. Are you sure you want to make one?\r\n\r\n";
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

function itemUpgradePrompt(warning, str, itemids, itemreqs, itemlimits, itemjobs, itemstats) {
	npc.sayNext(warning);
	for (let i = 0; i < itemids.length; i++)
		str += ("\r\n#L" + i + "##b #t" + itemids[i] + "##k(Level limit : " + itemlimits[i] + ", " + itemjobs[i] + ")#l");
	let selection = npc.askMenu(str);

	let item = itemids[selection];
	let reqs = itemreqs[selection];
	let limit = itemlimits[selection];
	let stat = itemstats[selection];

	str = "To upgrade one #t" + item + "#, you'll need these items below. The level limit for the item is " + limit + ", ";
	if (stat != null)
		str += "with the item option of #r" + stat + "#k attached to it, ";
	str += "so please check and see if you really need it. Oh, and one thing. Please make sure NOT to use an upgraded item as a material for the upgrade. Now, are you sure you want to make this item?\r\n\r\n";
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

let selection = npc.askYesNo("Do you want to take a look at some items? Well... any thought of making one? I'm actually a wizard that was banished from the town because I casted an illegal magic. Because of that I've been hiding, doing some business here... well, that's not really the point. Do you want to do some business with me?");
if (selection == 0) {
	npc.sayNext("You don't trust my skills, I suppose... haha... you should know that I used to be a great wizard. You still can't believe my skills, huh... but just remember that I used to be the great magician of old...");
} else if (selection == 1) {
	selection = npc.askMenu("Alright ... it's for both of our own good, right? Choose what you want to do...\r\n"
			+ "#L0##b Make a wand#l\r\n"
			+ "#L1##b Make a staff#l\r\n"
			+ "#L2##b Make a glove#l\r\n"
			+ "#L3##b Upgrade a glove#l\r\n"
			+ "#L4##b Upgrade a hat#l\r\n");
	let itemids;
	let itemreqs;
	switch (selection) {
		case 0: {
			itemids = [1372005, 1372006, 1372002, 1372004, 1372003, 1372001, 1372000, 1372007];
			let wandlimits = [8, 13, 18, 23, 28, 33, 38, 48];
			let wandjobs = ["all", "all", "all", "magician", "magician", "magician", "magician", "magician"];
			itemreqs = [
				[4003001, 5, MESOS, 1000],
				[4003001, 10, 4000001, 50, MESOS, 3000],
				[4011001, 1, 4000009, 30, 4003000, 5, MESOS, 5000],
				[4011002, 2, 4003002, 1, 4003000, 10, MESOS, 12000],
				[4011002, 3, 4021002, 1, 4003000, 10, MESOS, 30000],
				[4021006, 5, 4011002, 3, 4011001, 1, 4003000, 15, MESOS, 60000],
				[4021006, 5, 4021005, 5, 4021007, 1, 4003003, 1, 4003000, 20, MESOS, 12000],
				[4011006, 4, 4021003, 3, 4021007, 2, 4021002, 1, 4003002, 1, 4003000, 30, MESOS, 200000]
			];
			selection = itemMakePrompt("If you gather up the materials for me, I'll make a wand for you with my magical power. How... what kind of a wand do you want to make?",
					itemids, itemreqs, wandlimits, wandjobs);
			break;
		}
		case 1: {
			itemids = [1382000, 1382003, 1382005, 1382004, 1382002, 1382001];
			let stafflimits = [10, 15, 15, 20, 25 ,45];
			let staffjobs = ["magician", "magician", "magician", "magician", "magician", "magician"];
			itemreqs = [
				[4003001, 5, MESOS, 2000],
				[4021005, 1, 4011001, 1, 4003000, 5, MESOS, 2000],
				[4021003, 1, 4011001, 1, 4003000, 5, MESOS, 2000],
				[4003001, 50, 4011001, 1, 4003000, 10, MESOS, 5000],
				[4021006, 2, 4021001, 1, 4011001, 1, 4003000, 15, MESOS, 12000],
				[4011001, 8, 4021006, 5, 4021001, 5, 4021005, 5, 4003000, 30, 4000010, 50, 4003003, 1, MESOS, 180000]
			];
			selection = itemMakePrompt("If you gather up the materials for me, I'll make a staff for you with my magical power. How... what kind of a wand do you want to make?",
					itemids, itemreqs, stafflimits, staffjobs);
			break;
		}
		case 2: {
			itemids = [1082019, 1082020, 1082026, 1082051, 1082054, 1082062, 1082081, 1082086];
			let glovelimits = [15, 20, 25, 30, 35, 40, 50, 60];
			let glovejobs = ["magician", "magician", "magician", "magician", "magician", "magician", "magician", "magician"];
			itemreqs = [
				[4000021, 15, MESOS, 500],
				[4000021, 30, 4011001, 1, MESOS, 300],
				[4000021, 50, 4011006, 2, MESOS, 500],
				[4000021, 60, 4021006, 1, 4021000, 2, MESOS, 800],
				[4000021, 70, 4011006, 1, 4011001, 3, 4021000, 2, MESOS, 500],
				[4000021, 80, 4021000, 3, 4021006, 3, 4003000, 30, MESOS, 800],
				[4021000, 3, 4011006, 2, 4000030, 55, 4003000, 40, MESOS, 1000],
				[4011007, 1, 4011001, 8, 4021007, 1, 4000030, 50, 4003000, 50, MESOS, 1500]
			];
			selection = itemMakePrompt("If you gather up the materials for me, I'll make a glove for you with my magical power. How... what kind of a wand do you want to make?",
					itemids, itemreqs, glovelimits, glovejobs);
			break;
		}
		case 3: {
			itemids = [1082021, 1082022, 1082027, 1082028, 1082052, 1082053, 1082055, 1082056, 1082063, 1082064, 1082082, 1082080, 1082087, 1082088];
			let upglovelimits = [20, 20, 25, 25, 30, 30, 35, 35, 40, 40, 50, 50, 60, 60];
			let upglovejobs = ["magician", "magician", "magician", "magician", "magician", "magician", "magician", "magician", "magician", "magician", "magician", "magician", "magician", "magician"]
			let upglovestats = ["INT + 1", "INT + 2", "INT + 1", "INT + 2", "INT + 1", "INT + 2", "INT + 1", "INT + 2", "INT + 2", "INT + 3", "INT + 2, MP + 15", "INT + 3, MP + 30", "INT + 2, LUK + 1, MP + 15", "INT + 3, LUK + 1, MP + 30"];
			itemreqs = [
				[1082020, 1, 4011001, 1, MESOS, 20000],
				[1082020, 1, 4021001, 2, MESOS, 25000],
				[1082026, 1, 4021000, 3, MESOS, 30000],
				[1082026, 1, 4021008, 1, MESOS, 40000],
				[1082051, 1, 4021005, 3, MESOS, 35000],
				[1082051, 1, 4021008, 1, MESOS, 40000],
				[1082054, 1, 4021005, 3, MESOS, 40000],
				[1082054, 1, 4021008, 1, MESOS, 45000],
				[1082062, 1, 4021002, 4, MESOS, 45000],
				[1082062, 1, 4021008, 2, MESOS, 50000],
				[1082081, 1, 4021002, 5, MESOS, 55000],
				[1082081, 1, 4021008, 3, MESOS, 60000],
				[1082086, 1, 4011004, 3, 4011006, 5, MESOS, 70000],
				[1082086, 1, 4021008, 2, 4011006, 3, MESOS, 80000]
			];
			selection = itemUpgradePrompt("So you want to upgrade a glove? Be careful, though; All the items that will be used for upgrading will be gone, and if you use an item that has been #rupgraded#k with a scroll, the effect will disappear when upgraded, so you may want to think about it before making your decision ...",
					"Now .. which glove do you want to upgrade?", itemids, itemreqs, upglovelimits, upglovejobs, upglovestats);
			break;
		}
		case 4: {
			itemids = [1002065,1002013];
			let hatlimits = [30, 30];
			let hatjobs = ["wizard", "wizard"]
			let hatstats = ["INT + 1", "INT + 2"];
			itemreqs = [
				[1002064, 1, 4011001, 3, MESOS, 40000],
				[1002064, 1, 4011006, 3, MESOS, 50000]
			];
			selection = itemUpgradePrompt("So you want to upgrade a hat ... Be careful, though; All the items that will be used for upgrading will be gone, and if you use an item that has been #rupgraded#k with a scroll, the effect will disappear when upgraded, so you may want to think about it before making your decision ...",
					"Alright, so which hat would you like to upgrade?", itemids, itemreqs, hatlimits, hatjobs, hatstats);
			break;
		}
	}
	if (selection == -1) {
		npc.sayNext("Really? You must be lacking materials. Try harder at gathering them up around town. Fortunately it looks like the monsters around the forest have various materials on their sleeves.");
	} else {
		let okay = true;
		let item = itemids[selection];
		let reqs = itemreqs[selection];
		for (let i = 0; i < reqs.length && okay; i += 2) {
			if (reqs[i] != MESOS) {
				if (!npc.playerHasItem(reqs[i], reqs[i + 1]))
					okay = false;
			} else if (!npc.playerHasMesos(reqs[i + 1])) {
				okay = false;
			}
		}
		if (!npc.playerCanHoldItem(item, 1))
			okay = false;

		if (!okay) {
			npc.sayNext("Please check and see if you have all the items you need, or if your equipment inventory is full or not.");
		} else {
			for (let i = 0; i < reqs.length; i += 2)
				if (reqs[i] != MESOS)
					npc.takeItem(reqs[i], reqs[i + 1]);
				else
					npc.takeMesos(reqs[i + 1]);
			npc.giveItem(item, 1);
			npc.sayNext("Here, take #t" + item + "#. The more I see it, the more it looks perfect. Hahah, it's not a stretch to think that other magicians fear my skills ...");
		}
	}
}