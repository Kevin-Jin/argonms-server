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
 * Phil (NPC 1002000)
 * Victoria Road: Lith Harbor (Map 104000000)
 *
 * Taxi NPC for Lith Harbor who also gives town advice for beginners.
 *
 * @author GoldenKevin (content from KiniroMS r227)
 */

let taxiMaps = [102000000, 101000000, 100000000, 103000000, 120000000];
let taxiFares = [1200, 1200, 800, 1000, 1000];

npc.sayNext("Do you wanna head over to some other town? With a little money involved, I can make it happen. It's a tad expensive, but I run a special 90% discount for beginners.");
let selection = npc.askMenu("It's understandable that you may be confused about this place if this is your first time around. If you got any questions about this place, fire away.\r\n#b"
		+ "#L0#What kind of towns are here in Victoria Island?#l\r\n"
		+ "#L1#Please take me somewhere else.#l#k");
if (selection == 0) {
	selection = npc.askMenu("There are 6 big towns here in Victoria Island. Which of those do you want to know more of?\r\n"
			 + "#b#L0##m104000000##l\r\n"
			 + "#L1##m102000000##l\r\n"
			 + "#L2##m101000000##l\r\n"
			 + "#L3##m100000000##l\r\n"
			 + "#L4##m103000000##l\r\n"
			 + "#L5##m105040300##l");
	switch (selection) {
		case 0:
			npc.sayNext("The town you are at is Lith Harbor! Alright I'll explain to you more about #b#m104000000##k. It's the place you landed on Victoria Island by riding The Victoria. That's #m104000000#. A lot of beginners who just got here from Maple Island start their journey here.");
			npc.sayNext("It's a quiet town with the wide body of water on the back of it, thanks to the fact that the harbor is located at the west end of the island. Most of the people here are, or used to be fishermen, so they may look intimidating, but if you strike up a conversation with them, they'll be friendly to you.");
			npc.sayNext("Around town lies a beautiful prairie. Most of the monsters there are small and gentle, perfect for beginners. If you haven't chosen your job yet, this is a good place to boost up your level.");
			break;
		case 1:
			npc.sayNext("Alright I'll explain to you more about #b#m102000000##k. It's a warrior-town located at the northern-most part of Victoria Island, surrounded by rocky mountains. With an unfriendly atmosphere, only the strong survives there.");
			npc.sayNext("Around the highland you'll find a really skinny tree, a wild hog running around the place, and monkeys that live all over the island. There's also a deep valley, and when you go deep into it, you'll find a humongous dragon with the power to match his size. Better go in there very carefully, or don't go at all.");
			npc.sayNext("If you want to be a #bWarrior#k then find #r#p1022000##k, the chief of #m102000000#. If you're level 10 or higher, along with a good STR level, he may make you a warrior after all. If not, better keep training yourself until you reach that level.");
			break;
		case 2:
			npc.sayNext("Alright I'll explain to you more about #b#m101000000##k. It's a magician-town located at the fart east of Victoria Island, and covered in tall, mystic trees. You'll find some fairies there, too; They don't like humans in general so it'll be best for you to be on their good side and stay quiet.");
			npc.sayNext("Near the forest you'll find green slimes, walking mushrooms, monkeys and zombie monkeys all residing there. Walk deeper into the forest and you'll find witches with the flying broomstick navigating the skies. A word of warning: unless you are really strong, I recommend you don't go near them.");
			npc.sayNext("If you want to be the #bMagician#k, search for #r#p1032001##k, the head wizard of #m101000000#. He may make you a wizard if you're at or above level 8 with a decent amount of INT. If that's not the case, you may have to hunt more and train yourself to get there.");
			break;
		case 3:
			npc.sayNext("Alright I'll explain to you more about #b#m100000000##k. It's a bowman-town located at the southernmost part of the island, made on a flatland in the midst of a deep forest and prairies. The weather's just right, and everything is plentiful around that town, perfect for living. Go check it out.");
			npc.sayNext("Around the prairie you'll find weak monsters such as snails, mushrooms, and pigs. According to what I hear, though, in the deepest part of the Pig Park, which is connected to the town somewhere, you'll find a humongous, powerful mushroom called Mushmom every now and then.");
			npc.sayNext("If you want to be the #bBowman#k, you need to go see #r#p1012100##k at #m100000000#. With a level at or above 10 and a decent amount of DEX, she may make you be one after all. If not, go train yourself, make yourself stronger, then try again.");
			break;
		case 4:
			npc.sayNext("Alright I'll explain to you more about #b#m103000000##k. It's a thief-town located at the northwest part of Victoria Island, and there are buildings up there that have just this strange feeling around them. It's mostly covered in black clouds, but if you can go up to a really high place, you'll be able to see a very beautiful sunset there.");
			npc.sayNext("From #m103000000#, you can go into several dungeons. You can go to a swamp where alligators and snakes are abound, or hit the subway full of ghosts and bats. At the deepest part of the underground, you'll find Lace, who is just as big and dangerous as a dragon.");
			npc.sayNext("If you want to be the #bThief#k, seek #r#p1052001##k, the heart of darkness of #m103000000#. He may well make you a thief if you're at or above level 10 with a good amount of DEX. If not, go hunt and train yourself to reach there.");
			break;
		case 5:
			npc.sayNext("Alright I'll explain to you more about #b#m105040300##k. It's a forest town located at the southeast side of Victoria Island. It's pretty much in between #m100000000# and the ant-tunnel dungeon. There's a hotel there, so you can rest up after a long day at the dungeon ... it's a quiet town in general.");
			npc.sayNext("In front of the hotel there's an old buddhist monk by the name of #r#p1061000##k. Nobody knows a thing about that monk. Apparently he collects materials from the travellers and create something, but I am not too sure about the details. If you have any business going around that area, please check that out for me.");
			npc.sayNext("From #m105040300#, head east and you'll find the ant tunnel connected to the deepest part of the Victoria Island. Lots of nasty, powerful monsters abound so if you walk in thinking it's a walk in the park, you'll be coming out as a corpse. You need to fully prepare yourself for a rough ride before going in.");
			npc.sayNext("And this is what I hear ... apparently, at #m105040300# there's a secret entrance leading you to an unknown place. Apparently, once you move in deep, you'll find a stack of black rocks that actually move around. I want to see that for myself in the near future ...");
			break;
	}
} else if (selection == 1) {
	let prompt;
	if (player.getJob() == 0) {
		prompt = "There's a special 90% discount for all beginners. Alright, where would you want to go?#b";
		for (let i = 0; i < taxiMaps.length; i++)
			prompt += "\r\n#L" + i + "##m" + taxiMaps[i] + "#(" + taxiFares[i] / 10 + " mesos)#l";
	} else {
		prompt = "Oh you aren't a beginner, huh? Then I'm afraid I may have to charge you full price. Where would you like to go?#b";
		for (let i = 0; i < taxiMaps.length; i++)
			prompt += "\r\n#L" + i + "##m" + taxiMaps[i] + "#(" + taxiFares[i] + " mesos)#l";
	}
	mapIndex = npc.askMenu(prompt);
	let fare = taxiFares[mapIndex];
	if (player.getJob() == 0)
		fare /= 10;
	selection = npc.askYesNo("I guess you don't need to be here. Do you really want to move to #b#m" + taxiMaps[mapIndex] + "##k? Well it'll cost you #b" + fare + " meso#k. What do you think?");
	if (selection == 1) {
		if (player.hasMesos(fare)) {
			player.loseMesos(fare);
			player.changeMap(taxiMaps[mapIndex]);
		} else {
			npc.say("You don't have enough mesos. With your abilities, you should have more than that!");
		}
	} else if (selection == 0) {
		npc.say("There's a lot to see in this town too. Let me know if you want to go somewhere else.");
	}
}