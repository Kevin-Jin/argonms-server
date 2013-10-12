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
 * Staff Sergeant Charlie (NPC 2010000)
 * Orbis: Orbis (Map 200000000)
 *
 * Refining NPC in Orbis that trades random items for materials.
 *
 * @author GoldenKevin (content from Vana r3171)
 */

let takereqs = [
	[4000073, 100], //Solid Horn
	[4000059, 100], //Star Pixie's Starpiece
	[4000076, 100], //Fly-Eye Wings
	[4000058, 100], //Nependeath Seed
	[4000078, 100], //Jr. Cerebes Tooth
	[4000060, 100], //Lunar Pixie's Moonpiece
	[4000062, 100], //Dark Nependeath Seed
	[4000048, 100], //Jr. Yeti Skin
	[4000081, 100], //Firebomb Flame
	[4000061, 100], //Luster Pixie's Sunpiece
	[4000070, 100], //Cellion Tail
	[4000071, 100], //Lioner Tail
	[4000072, 100], //Grupin Tail
	[4000051, 100], //Hector Tail
	[4000055, 100], //Dark Jr. Yeti Skin
	[4000069, 100], //Zombie's Lost Tooth
	[4000052, 100], //White Pang Tail
	[4000050, 100], //Pepe Beak
	[4000057, 100], //Dark Pepe Beak
	[4000049, 100], //Yeti Horn
	[4000056, 100], //Dark Yeti Horn
	[4000079, 100], //Cerebes Tooth
	[4000053, 100], //Werewolf Toenail
	[4000054, 100], //Lycanthrope Toenail
	[4000080, 100]  //Bain's Spiky Collar
];
rewards = [
	[[2000001, 20],	[2010004, 10], [2000003, 15], [4003001, 15], [2020001, 15], [2030000, 15]],
	[[2000001, 30],	[2010001, 40], [2000003, 20], [4003001, 20], [2040002, 01]],
	[[2000001, 30],	[2010001, 40], [2000003, 20], [4003001, 20], [2040002, 01]],
	[[2000002, 15],	[2010004, 15], [2000003, 25], [4003001, 30], [2040302, 01]],
	[[2000002, 15],	[2010004, 15], [2000003, 25], [2050004, 15], [4003001, 30], [2040302, 01]],
	[[2000002, 25],	[2000006, 10], [2022000, 05], [4000030, 15], [2040902, 01]],
	[[2000002, 30],	[2000006, 15], [2020000, 20], [4003000, 05], [2040402, 01]],
	[[2000002, 30],	[2000006, 15], [2020000, 20], [4003000, 05], [2040402, 01]],
	[[2000006, 25],	[2020006, 25], [4010004, 08], [4010005, 08], [4010006, 03], [4020007, 02], [4020008, 02], [2040705, 01]],
	[[2000002, 30],	[2000006, 15], [2020000, 20], [4003000, 05], [2041016, 01]],
	[[2000002, 30],	[2000006, 15], [2020000, 20], [4003000, 05], [2041005, 01]],
	[[2000002, 30],	[2000006, 15], [2020000, 20], [4003000, 05], [2041005, 01]],
	[[2000002, 30],	[2000006, 15], [2020000, 20], [4003000, 05], [2041005, 01]],
	[[2002004, 15], [2002005, 15], [2002003, 10], [4001005, 01], [2040502, 01]],
	[[2002005, 30], [2002006, 15], [2022001, 30], [4003003, 01], [2040505, 01]],
	[[2002005, 30], [2002006, 15], [2000006, 20], [2050004, 20], [4003003, 01], [2041002, 01]],
	[[2000006, 20], [4010004, 07], [4010005, 07], [4010003, 07], [4003002, 01], [2040602, 01]],
	[[2000006, 20], [4010000, 07], [4010001, 07], [4010002, 07], [4010006, 02], [4003000, 05], [2040702, 01]],
	[[2000006, 20], [4010004, 07], [4010005, 07], [4010006, 03], [4020007, 02], [4020008, 02], [2040705, 01]],
	[[2000006, 25], [2020000, 20], [4020000, 07], [4020001, 07], [4020002, 03], [4020007, 02], [2040708, 01]],
	[[2000006, 25], [4020005, 07], [4020003, 07], [4020004, 07], [4020008, 02], [2040802, 01]],
	[[2000006, 25], [2050004, 30], [2022001, 35], [4020000, 07], [4020001, 07], [4020002, 07], [4020007, 02], [2041023, 01]],
	[[2000006, 30], [4020006, 07], [4020007, 02], [4020008, 02], [2070010, 01], [2040805, 01]],
	[[2000006, 30], [4020006, 07], [4020007, 02], [4020008, 02], [2041020, 01]],
	[[2000006, 35], [4020006, 09], [4020007, 04], [4020008, 04], [2041008, 01]],
];

//Default rewards:

//Meat: 2010001
//Orange Potion: 2000001
//Fried Chicken: 2020001
//White Potion: 2000002
//Hot Dog: 2020005
//Hot Dog Supreme: 2020006

//Blue Potion: 2000003
//Lemon: 2010004
//Salad: 2020000
//Mana Elixir: 2000006
//Pure Water: 2022000

//All Cure: 2050004
//Warrior Potion: 2002004
//Sniper Potion: 2002005
//Wizard Potion: 2002003
//Icicles: 2070010
//Red Bean Porridge: 2022001

//Nearest Town Scroll: 2030000
//10% helmet for def: 2040002
//10% shield for def: 2040902
//10% earring for int: 2040302
//10% topwear for def: 2040402
//10% bottomwear for def: 2040602
//10% overall for def: 2040505
//10% overall for dex: 2040502
//10% shoes for jump: 2040705
//10% shoes for speed: 2040708
//10% shoes for dex: 2040702
//10% gloves for dex: 2040802
//10% gloves for att: 2040805
//10% cape for dex: 2041020
//10% cape for luk: 2041023
//10% cape for mdef: 2041002
//10% cape for wdef: 2041005
//10% cape for HP: 2041008
//60% cape for int: 2041016

//Dragon Skin: 4000030
//Screw: 4003000
//Processed Wood: 4003001
//Ancient Scroll: 4001005
//Piece of Ice: 4003002
//Fairy Wing: 4003003

//Silver Ore: 4010004
//Orihalcon Ore: 4010005
//Mithril Ore: 4010002
//Steel Ore: 4010001
//Bronze Ore: 4010000
//Adamantium Ore: 4010003
//Gold Ore: 4010006
//Garnet Ore: 4020000
//Amethyst Ore: 4020001
//Aquamarine Ore: 4020002
//Emerald Ore: 4020003
//Opal Ore: 4020004
//Sapphire Ore: 4020005
//Topaz Ore: 4020006
//Diamond Ore: 4020007
//Black Crystal Ore: 4020008

npc.sayNext("Hey, got a little bit of time? Well, my job is to collect items here and sell them elsewhere, but these days the monsters have become much more hostile so it's been difficult getting good items ... What do you think? Do you want to do some business with me?");
let selection = npc.askYesNo("The deal is simple. You get me something I need, I get you something you need. The problem is, I deal with a whole bunch of people, so the items I have to offer may change every time you see me. What do you think? Still want to do it?");
if (selection == 0) {
	npc.sayNext("Hmmm...it shouldn't be a bad deal for you. Come see me at the right time and you may get a much better item to be offered. Anyway, let me know when you have a change of heart.");
} else if (selection == 1) {
	let str = "Ok! First you need to choose the item that you'll trade with. The better the item, the more likely the chance that I'll give you something much nicer in return.#b";
	for (let i = 0; i < takereqs.length; i++)
		str += "\r\n#L" + i + "# " + takereqs[i][1]" #t" + takereqs[i][0] + "#s#l";
	selection = npc.askMenu(str);

	let reqs = takereqs[selection];
	let rews = rewards[selection];
	selection = npc.askYesNo("Let's see, you want to trade your #b" + reqs[1] + " #t" + reqs[0] + "#s#k with my stuff, right? Before trading make sure you have an empty slot available on your use or etc. inventory. Now, do you really want to trade with me?");
	if (selection == 0) {
		npc.sayNext("Hmmm ... it shouldn't be a bad deal for you at all. If you come at the right time I can hook you up with good items. Anyway if you feel like trading, feel free to come.");
	} else if (selection == 1) {
		let rew = rews[Math.floor(Math.random() * rews.length)];
		if (!player.canGainItem(rew[0], rew[1]) || !player.hasItem(reqs[0], reqs[1])) {
			npc.sayNext("Hmmm... are you sure you have #b" + reqs[1] + " #t" + reqs[0] + "#s#k? If so, then please check and see if your item inventory is full or not.");
		} else {
			player.gainExp(500);
			player.loseItem(reqs[0], reqs[1]);
			player.gainItem(rew[0], rew[1]);
			npc.sayNext("For your #b" + reqs[1] + " #t" + reqs[0] + "#s#k, here's my #b" + rew[1] + " #t" + rew[0] + "#(s)#k. What do you think? Do you like the items I gave you in return? I plan on being here for a while, so if you gather up more items, I'm always open for a trade ...");
		}
	}
}