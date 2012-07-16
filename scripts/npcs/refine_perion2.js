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
 * Mr. Smith: Item Creator (NPC 1022004)
 * Victoria Road: Perion (Map 102000000)
 *
 * Wood processor and screw maker.
 * Refining NPC for warriors - gloves only.
 *
 * @author GoldenKevin (content from Vana r3171)
 */

let MESOS = 4031138;

function itemMakePrompt(str, itemids, itemreqs, itemlimits, itemjobs) {
	for (let i = 0; i < itemids.length; i++)
		str += ("\r\n#L" + i + "##b #t" + itemids[i] + "##k(level limit : " + itemlimits[i] + ", " + itemjobs[i] + ")#l");
	let selection = npc.askMenu(str);

	let item = itemids[selection];
	let reqs = itemreqs[selection];
	let limit = itemlimits[selection];

	str = "To make one #t" + item + "#, I need the following items. The level limit is " + limit + " and please make sure you don't use an item that's been upgraded as a material for it. What do you think? Do you want one?\r\n";
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

function itemUpgradePrompt(warning, str, itemids, itemreqs, itemlimits, itemjobs) {
	npc.sayNext(warning);
	for (let i = 0; i < itemids.length; i++)
		str += ("\r\n#L" + i + "##b #t" + itemids[i] + "##k(level limit : " + itemlimits[i] + ", " + itemjobs[i] + ")#l");
	let selection = npc.askMenu(str);

	let item = itemids[selection];
	let reqs = itemreqs[selection];
	let limit = itemlimits[selection];

	str = "To make one #t" + item + "#, I need the following items. The level limit is " + limit + " and please make sure you don't use an item that's been upgraded as a material for it. What do you think? Do you want one?";
	str += "Make sure you don't use an item that's been upgraded as a material for it. What do you think? Do you want one?\r\n";
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

let selection = npc.askYesNo("I am Mr. Thunder's apprentice. He's gettin up there with age, and he isn't what he used to be...haha, oh crap, please don't tell him that I said that...Anyway...I can make various items specifically designed for the warriors, so what do you think? Wanna leave it up to me?");
if (selection == 0) {
	npc.sayNext("Sigh ... I'll definitely hear it from my boss if I don't make the norm today ... Oh well, that sucks.");
} else if (selection == 1) {
	selection = npc.askMenu("Alright! The service fee will be reasonable so don't worry about it. What do you want to do?\r\n#b"
			+ "#L0# Make a glove#l\r\n"
			+ "#L1# Upgrade a glove#l\r\n"
			+ "#L2# Create materials#l");
	let itemids;
	let itemreqs;
	let equip;
	switch (selection) {
		case 0:
			itemids = [1082003, 1082000, 1082004, 1082001, 1082007, 1082008, 1082023, 1082009, 1082059];
			let glovelimits = [10, 15, 20, 25, 30, 35, 40, 50, 60];
			let glovejobs = ["warrior", "warrior", "warrior", "warrior", "warrior", "warrior", "warrior", "warrior", "warrior"];
			itemreqs = [
				[4000021, 15, 4011001, 1, MESOS, 1000],
				[4011001, 2, MESOS, 2000],
				[4000021, 40, 4011000, 2, MESOS, 5000],
				[4011001, 2, MESOS, 10000],
				[4011000, 3, 4003000, 15, MESOS, 20000],
				[4000021, 30, 4011001, 4, 4003000, 30, MESOS, 30000],
				[4000021, 50, 4011001, 5, 4003000, 40, MESOS, 40000],
				[4011001, 3, 4021007, 2, 4000030, 30, 4003000, 45, MESOS, 50000],
				[4011007, 1, 4011000, 8, 4011006, 2, 4000030, 50, 4003000, 50, MESOS, 70000]
			];
			selection = itemMakePrompt("I'm the best glove-maker in this town!! Now...what kind of a glove do you want to make?",
					itemids, itemreqs, glovelimits, glovejobs);
			equip = true;
			break;
		case 1:
			itemids = [1082005, 1082006, 1082035, 1082036, 1082024, 1082025, 1082010, 1082011, 1082060, 1082061];
			let upgradelimits = [30, 30, 35, 35, 40, 40, 50, 50, 60, 60];
			let upgradejobs = ["warrior", "warrior", "warrior", "warrior", "warrior", "warrior", "warrior", "warrior", "warrior", "warrior"];
			itemreqs = [
				[1082007, 1, 4011001, 1, MESOS, 20000],
				[1082007, 1, 4011005, 2, MESOS, 25000],
				[1082008, 1, 4021006, 3, MESOS, 30000],
				[1082008, 1, 4021008, 1, MESOS, 40000],
				[1082023, 1, 4011003, 4, MESOS, 45000],
				[1082023, 1, 4021008, 2, MESOS, 50000],
				[1082009, 1, 4011002, 5, MESOS, 55000],
				[1082009, 1, 4011006, 4, MESOS, 60000],
				[1082059, 1, 4011002, 3, 4021005, 5, MESOS, 70000],
				[1082059, 1, 4021007, 2, 4021008, 2, MESOS, 80000]
			];
			selection = itemUpgradePrompt("So you want to upgrade the glove? Ok, then. A word of warning, though: All the items that will be used for upgrading will be gone, but if you use an item that has been #rupgraded#k with a scroll, the effect will disappear when upgraded. Please take that into consideration when making the decision, ok?",
					"So~~ what kind of a glove do you want to upgrade and create?", itemids, itemreqs, upgradelimits, upgradejobs);
			equip = true;
			break;
		case 2: {
			itemids = [4003001, 4003001, 4003000];
			let itemcounts = [1, 1, 15];
			itemreqs = [
				[4000003, 10],
				[4000018, 5],
				[4011000, 1, 4011001, 1]
			];
			selection = npc.askMenu("So you want to make some materials, huh? Okay...what kind of materials do you want to make?\r\n"
					+ "#L0##bMake #t4003001# with #t4000003##k#l\r\n"
					+ "#L1##bMake #t4003001# with #t4000018##k#l\r\n"
					+ "#L2##bMake #t4003000#s#k#l");

			let str;
			switch (selection) {
				case 0:
					str = "With #b10 #t4000003#es#k, I can make 1 #t4003001#(s). Be thankful, because this one's on me. What do you think? How many do you want?";
					break;
				case 1:
					str = "With #b5 #t4000018#s#k, I can make 1 #t4003001#(s). Be thankful, because this one's on me. What do you think? How many do you want?";
					break;
				case 2:
					str = "With #b1 #t4011001#(s) and #t4011000#(s) each#k, I can make 15 #t4003000#s. Be thankful, because this one's on me. What do you think? How many do you want?";
					break;
			}
			let reqs = itemreqs[selection];
			let quantity = npc.askNumber(str, 0, 0, 100);

			// Output for this step is all messed up, thank Nexon
			switch (selection) {
				case 0:
					str = "You want to make #b#t4003001#(s)#k " + quantity + " time(s)? I'll need  #r" + reqs[1] * quantity + " #t4000003#es#k then.";
					break;
				case 1:
					str = "You want to make #b#t4003001#(s)#k " + quantity + " time(s)? I'll need  #r" + reqs[1] * quantity + " #t4000018#s#k then.";
					break;
				case 2:
					str = "You want to make #b#t4003000#s#k " + quantity + " time(s)? I'll need  #r" + reqs[1] * quantity + " #t4011001#(s) and #t4011000#(s) each#k then.";
					break;
			}
			let approve = npc.askYesNo(str);

			if (approve == 0) {
				npc.sayNext("Lacking the materials? It's all good...collect them all and then come find me, alright? I'll be waiting...");
			} else {
				let okay = true;
				let item = itemids[selection];
				let reqs = itemreqs[selection];
				let giveCount = itemcounts[selection];
				if (quantity == 0)
					okay = false;
				for (let i = 0; i < reqs.length && okay; i += 2) {
					if (reqs[i] != MESOS) {
						if (!npc.playerHasItem(reqs[i], reqs[i + 1] * quantity))
							okay = false;
					} else if (!npc.playerHasMesos(reqs[i + 1] * quantity)) {
						okay = false;
					}
				}
				if (!npc.playerCanHoldItem(item, giveCount * quantity))
					okay = false;

				if (!okay) {
					npc.sayNext("Check and see if you have everything you need and if your equipment inventory may be full or not.");
				} else {
					for (let i = 0; i < reqs.length; i += 2)
						if (reqs[i] != MESOS)
							npc.takeItem(reqs[i], reqs[i + 1] * quantity);
						else
							npc.takeMesos(reqs[i + 1] * quantity);
					npc.giveItem(item, giveCount * quantity);
					npc.sayNext("Here! take " + giveCount * quantity  + " #t" + item + "#(s). Don't you think I'm as good as Mr. Thunder? You'll be more than satisfied with what I made here.");
				}
			}

			equip = false;
			break;
		}
	}
	if (equip) {
		if (selection == -1) {
			npc.sayNext("Lacking the materials? It's ok ... collect them all and then come find me, alright? I'll be waiting...");
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
				npc.sayNext("Check and see if you have everything you need and if your equipment inventory may be full or not.");
			} else {
				for (let i = 0; i < reqs.length; i += 2)
					if (reqs[i] != MESOS)
						npc.takeItem(reqs[i], reqs[i + 1]);
					else
						npc.takeMesos(reqs[i + 1]);
				npc.giveItem(item, 1);
				npc.sayNext("Here! take the #t" + item + "#. Don't you think I'm as good as Mr. Thunder? You'll be more than satisfied with what I made here.");
			}
		}
	}
}