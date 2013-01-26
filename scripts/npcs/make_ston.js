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
 * Eurek the Alchemist (NPC 2040050)
 * Dungeon: Sleepywood (Map 105040300),
 *   El Nath: El Nath (Map 211000000),
 *   Ludibrium: Ludibrium (Map 220000000),
 *   Leafre: Leafre (Map 240000000)
 *
 * Makes Magic Rocks and Summoning Rocks.
 *
 * @author GoldenKevin (content from KiniroMS r227)
 */

let COST = 4000;
let makeitems = [4006000, 4006001];
let reqset = [
	[
		[
			[4000046, 20],
			[4000027, 20],
			[4021001, 1]
		],
		[
			[4000025, 20],
			[4000049, 20],
			[4021006, 1]
		],
		[
			[4000129, 15],
			[4000130, 15],
			[4021002, 1]
		],
		[
			[4000074, 15],
			[4000057, 15],
			[4021005, 1]
		],
		[
			[4000054, 7],
			[4000053, 7],
			[4021003, 1]
		]
	],

	[
		[
			[4000046, 20],
			[4000027, 20],
			[4011001, 1]
		],
		[
			[4000014, 20],
			[4000049, 20],
			[4011003, 1]
		],
		[
			[4000132, 15],
			[4000128, 15],
			[4011005, 1]
		],
		[
			[4000074, 15],
			[4000069, 15],
			[4011002, 1]
		],
		[
			[4000080, 7],
			[4000079, 7],
			[4011004, 1]
		]
	]
];

npc.sayNext("Alright, mix up the frog's tongue with the squirrel's tooth and ... oh yeah! Forgot to put in the sparkling white powder!! Man, that could have been really bad ... Whoa!! How long have you been standing there? I maaaay have been a little carried away with my work ... hehe.");
let selection = npc.askMenu("As you can see, I'm just a traveling alchemist. I may be in training, but I can still make a few things that you may need. Do you want to take a look?\r\n\r\n#b"
		+ "#L0#Make Magic Rock#l\r\n"
		+ "#L1#Make The Summoning Rock#l");
let set = selection;
let makeitem = makeitems[set];
let str = "Haha... #b#t" + makeitem + "##k is a mystical rock that only I can make. Many travelers seems to need this for most powerful skills that require more than just MP and HP. There are 5 ways to make #t" + makeitem + "#. Which way do you want to make it?#b";
for (let i = 0; i < reqset[set].length; i++)
	str += "\r\n#L" + i + "#Make it using #t" + reqset[set][i][0][0] + "# and #t" + reqset[set][i][1][0] + "##l";
selection = npc.askMenu(str);

set = reqset[set][selection];
let reqitem = [
	[set[0][0], set[0][1]],
	[set[1][0], set[1][1]],
	[set[2][0], set[2][1]],
]
str = "";
for (let i = 0; i < reqitem.length; i++)
	str += "\r\n#v" + reqitem[i][0] + "# #b" + reqitem[i][1] + " #t" + reqitem[i][0] + "#s#k";
str += "\r\n#i4031138# #b" + COST + " mesos#k";
selection = npc.askYesNo("To make #b5 #t" + makeitem + "##k, I'll need the following items. Most of them can be obtained through hunting, so it won't be terriblt difficult for you to get them. What do you think? Do you want some?\r\n" + str);

if (selection == 0) {
	npc.sayNext("Not enough materials, huh? No worries. Just come see me once you gather up the necessary items. There are numerous ways to obtain them, whether it be hunting or purchasing it from others, so keep going.");
} else if (selection == 1) {
	let access = true;
	for (let i = 0; i < reqitem.length && access; i++)
		if (!player.hasItem(reqitem[i][0], reqitem[i][1]))
			access = false;
	if (!access || !player.canGainItem(makeitem, 1) || !player.hasMesos(COST)) {
		npc.sayNext("Please check and see if you have all the items needed, or if your etc. inventory is full or not");
	} else {
		player.loseItem(reqitem[0][0], reqitem[0][1]);
		player.loseItem(reqitem[1][0], reqitem[1][1]);
		player.loseItem(reqitem[2][0], reqitem[2][1]);
		player.loseMesos(COST);
		player.gainItem(makeitem, 5);
		npc.say("Here, take the 5 pieces of #b#t" + makeitem + "##k. Even I have to admit, this is a masterpiece. Alright, if you need my help down the road, by all means come back and talk to me!");
	}
}