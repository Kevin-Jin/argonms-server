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
 * Arwen the Fairy (NPC 1032100)
 * Victoria Road: Ellinia (Map 101000000)
 *
 * Refines items for Jane's Final Challenge (Quest 2013), Alcaster and the Dark
 * Crystal (Quest 3035), and Luke the Security Man's Wish to Travel (Quest 2035)
 *
 * @author GoldenKevin (content from KiniroMS r227)
 */

/**
 * Thanks to http://stackoverflow.com/a/2901298/444402!
 * @param x must be an integer.
 */
function groupThousands(x) {
	return x.toString().replace(/\B(?=(\d{3})+(?!\d))/g, ",");
}

if (npc.getPlayerLevel() < 40) {
	npc.say("I can make rare, valuable items but unfortunately I can't make it to a stranger like you.");
} else {
	npc.sayNext("Yeah... I am the master alchemist of the fairies. But the fairies are not supposed to be in contact with a human being for a long period of time... A strong person like you will be fine, though. If you get me the materials, I'll make you a special item.");

	
	let items = [4011007, 4021009, 4031042];
	let mesos = [10000, 15000, 30000];
	let materials = [
			[4011000, 4011001, 4011002, 4011003, 4011004, 4011005, 4011006],
			[4021000, 4021001, 4021002, 4021003, 4021004, 4021005, 4021006, 4021007, 4021008],
			[4001006, 4011007, 4021008]
	];

	let str = "What do you want to make?#b";
	for (let i = 0; i < items.length; i++)
		str += "\r\n#L" + i + "##t" + items[i] + "##l";
	let selection = npc.askMenu(str);

	let item = items[selection];
	let cost = mesos[selection];
	let mats = materials[selection];
	if (item != 4031042) {
		str = "So you want to make #t" + item + "#? To do that you need refined one of each of these: ";
		for (let i = 0; i < mats.length - 2; i++)
			str += "#b#t" + mats[i] + "##k, ";
		str += "#b#t" + mats[mats.length - 2] + "##k and #b#t" + mats[mats.length - 1] + "##k. Throw in " + groupThousands(cost) + " mesos and I'll make it for you.";
	} else {
		str = "So you want to make #t" + item + "#? To do that you need ";
		for (let i = 0; i < mats.length - 2; i++)
			str += "#b1 #t" + mats[i] + "##k, ";
		str += "#b1 #t" + mats[mats.length - 2] + "##k and #b1 #t" + mats[mats.length - 1] + "##k. Throw in " + groupThousands(cost) + " mesos and I'll make it for you. Oh yeah, this piece of feather is a very special item, so if you drop it by any chance, it'll disappear, as well as you won't be able to give it away to someone else.";
	}
	selection = npc.askYesNo(str);

	if (selection == 0) {
		npc.sayNext("It's not easy making #t" + item + "#. Please get the materials ready.");
	} else if (selection == 1) {
		let okay = true;
		for (let i = 0; i < mats.length && okay; i++)
			if (!npc.playerHasItem(mats[i], 1))
				okay = false;
		if (!npc.playerHasMesos(cost))
			okay = false;
		if (!npc.playerCanHoldItem(item, 1))
			okay = false;
		if (!okay) {
			if (item != 4031042) {
				str = "Are you sure you have enough mesos? Please check and see if you have the refined ";
				for (let i = 0; i < mats.length; i++)
					str += "#b#t" + mats[i] + "##k, ";
				npc.sayNext(str + "one of each.");
			} else {
				str = "Are you sure you have enough mesos? Please check and see if you have ";
				for (let i = 0; i < mats.length - 1; i++)
					str += "#b1 #t" + mats[i] + "##k, ";
				npc.sayNext(str + "and #b1 #t" + mats[mats.length - 1] + "##k ready for me.");
			}
		} else {
			npc.takeMesos(cost);
			for (let i = 0; i < mats.length; i++)
				npc.takeItem(mats[i], 1);
			npc.giveItem(item, 1);
			npc.sayNext("Ok here, take #t" + item + "#. It's well-made, probably because I'm using good materials. If you need my help down the road, feel free to come back.");
		}
	}
}