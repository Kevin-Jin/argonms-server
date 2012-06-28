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
 * Jane (NPC 1002100)
 * Victoria Road: Lith Harbor (Map 104000000)
 *
 * Sells potions after completing Jane the Alchemist quests - i.e. Jane and the
 * Wild Boar (Quest 2010), Jane's First Challenge (Quest 2011), Jane's Second
 * Challenge (Quest 2012), and Jane's Final Challenge (Quest 2013).
 *
 * @author GoldenKevin (content from KiniroMS r227)
 */

if (npc.isQuestFinished(2013)) {
	let items = [2000002, 2022003, 2022000, 2001000];
	let costs = [310, 1060, 1600, 3120];
	let recoverAmount = [300, 1000, 800, 1000];
	let recoverType = ["HP", "HP", "MP", "HP and MP"];

	npc.sayNext("It's you... thanks to you I was able to get a lot done. Nowadays I've been making a bunch of items. If you need anything let me know.");
	let selStr = "Which item would you like to buy?#b";
	for (let i = 0; i < items.length; i++)
		selStr += "\r\n#L" + i + "##z" + items[i] + "# (Price : " + costs[i] + " mesos)#l";
	selection = npc.askMenu(selStr);

	let item = items[selection];
	let cost = costs[selection];
	let count = npc.askNumber("You want #b#t" + item + "##k? #t" + item + "# allows you to recover " + recoverAmount[selection] + " " + recoverType[selection] + ". How many would you like to buy?", 1, 1, 100);
	selection = npc.askYesNo("Will you purchase #r" + count + "#k #b#t" + item + "#(s)#k? #t" + item + "# costs " + cost + " mesos for one, so the total comes out to be #r" + cost * count + "#k mesos.");
	cost *= count;
	if (selection == 1) {
		if (npc.playerHasMesos(cost) && npc.giveItem(item, count)) {
			npc.takeMesos(cost);
			npc.sayNext("Thank you for coming. Stuff here can always be made so if you need something, please come again.");
		} else {
			npc.sayNext("Are you lacking mesos by any chance? Please check and see if you have an empty slot available at your etc. inventory, and if you have at least #r" + cost + "#k mesos with you.");
		}
	} else if (selection == 0) {
		npc.sayNext("I still have quite a few of the materials you got me before. The items are all there so take your time choosing.");
	}
} else if (npc.isQuestFinished(2010)) {
	npc.sayNext("You don't seem strong enough to be able to purchase my potion...");
} else {
	npc.say("My dream is to travel everywhere, much like you. My father, however, does not allow me to do it, because he thinks it's very dangerous. He may say yes, though, if I show him some sort of a proof that I'm not the weak girl that he thinks I am...");
}