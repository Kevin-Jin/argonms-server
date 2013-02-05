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
 * Trainer Bartos: Pet Trainer (NPC 1012006)
 * Victoria Road: Pet-Walking Road (Map 100000202)
 *
 * Starts the pet walking challenge.
 *
 * @author GoldenKevin (content from KiniroMS r227)
 */

function askQuestions() {
	let questions = [
		"Which town is #p1012004#, the person selling #t2120000#, from?\r\n#b"
				+ "#L0# #m104000000##l\r\n"
				+ "#L1# #m100000000##l\r\n"
				+ "#L2# #m102000000##l\r\n"
				+ "#L3# #m101000000##l\r\n"
				+ "#L4# #m103000000##l\r\n"
				+ "#L5# #m105040300##l",
		"Haha... that was just for practice. Ok, then... out of these people, choose the person that has nothing to do with the pets.\r\n#b"
				+ "#L0# #p1032102##l\r\n"
				+ "#L1# #p1012005##l\r\n"
				+ "#L2# #p1012101##l",
		"Too easy, right? Ok, out of these descriptions about the pets, choose the one that does not make sense!\r\n#b"
				+ "#L0#To name a pet, you need the pet-naming item to do so.#l\r\n"
				+ "#L1#When you give the pet a command, and it follows through, the intimacy level sometimes rises.#l\r\n"
				+ "#L2#Starve the pet, and the intimacy level may go down.#l\r\n"
				+ "#L3#Pets can attack the monster with its master.#k#l",
		"Two more to go!!! Well, on what level do pets start using human phrases?\r\n"
				+ "#L0##e1. #n#bLevel 5#k#l\r\n"
				+ "#L1##e2. #n#bLevel 10#k#l\r\n"
				+ "#L2##e3. #n#bLevel 15#k#l\r\n"
				+ "#L3##e4. #n#bLevel 20#k#l",
		"Last question! #p1012004# from #m100000000# sells\r\n"
				+ "#t2120000#. How much fullness level does it raise?\r\n#b"
				+ "#L0# 10#l\r\n"
				+ "#L1# 20#l\r\n"
				+ "#L2# 30#l\r\n"
				+ "#L3# 40#l"
	];
	let correct = [1, 2, 3, 1, 2];

	for (let i = 0; i < questions.length; i++) {
		let answer = npc.askMenu("Question " + (i + 1) + ") " + questions[i]);
		if (answer != correct[i]) {
			if (i == questions.length - 1)
				npc.sayNext("Oh no!! That blows... it was the last question!! Don't give up!");
			else
				npc.sayNext("Wrong!! You don't even know that much... have you really raised a pet? This is horrible!");
			return false;
		}
	}
	return true;
}

let selection = npc.askMenu("Do you have any business with me?\r\n#b"
		+ "#L0#Please tell me about this place.#l\r\n"
		+ "#L1#I'm here through a word from Mar the Fairy...#l");
switch (selection) {
	case 0:
		if (player.hasItem(4031035, 1)) {
			npc.sayNext("Get that letter, jump over obstacles with your pet, and take that letter to my brother Trainer Frod. Give him the letter and something good is going to happen to your pet.");
		} else {
			selection = npc.askYesNo("This is the road where you can go take a walk with your pet. You can just walk around with it, or you can train your pet to go through the obstacles here. If you aren't too close with your pet yet, that may present a problem and he will not follow your command as much... So, what do you think? Wanna train your pet?");
			if (selection == 0) {
				npc.sayNext("Hmmm ... too busy to do it right now? If you feel like doing it, though, come back and find me.");
			} else {
				if (player.gainItem(4031035, 1))
					npc.sayNext("Ok, here's the letter. He wouldn't know I sent you if you just went there straight, so go through the obstacles with your pet, go to the very top, and then talk to Trainer Frod to give him the letter. It won't be hard if you pay attention to your pet while going through obstacles. Good luck!");
				else //TODO: GMS-like line
					npc.say("Please check whether your ETC. inventory is full.");
			}
		}
		break;
	case 1:
		if (player.isQuestActive(2049)) {
			npc.sayNext("Are you here with the #bunmoving pet#k? That's sad to see... Huh? You're here through #b#p1032102##k? I see... #b#t4031034##k, huh... hey hey~ as if I really have that with me... what the, what's this in my pocket?");
			npc.sayNext("Whoa!! Is... is this the #b#t4031034##k? Oh okay... #p1012005# probably borrowed my clothes and went out or something... dang it, I told him not to just take someone else's clothes and wear them... Well this isn't mine anyway... you need this?? Hmm...");
			selection = npc.askYesNo("I don't think I can just give it to you! I need to test your knowledge on pets in general. Sucks for a pet if its owner doesn't even care for it. You need to get all these right, or you won't get the scroll. What do you think? Wanna take the test?");
			if (selection == 0) {
				npc.sayNext("What... you're giving up already? If you've raised your pet well it should be a piece of cake! Find me when you have a change of heart.");
			} else {
				npc.sayNext("Alright! 5 questions, and you need to answer all of them right! Are you up for it? Here it is!!!");
				if (askQuestions()) {
					npc.sayNext("Alright!! Hmmm... you do know quite a bit on pets. Good, since you know a lot, I'll happily give you the scroll. I know it's not mine and all, but... who's the one that wore someone else's clothes and then left something very important in it? Anyway here you go!");
					let needsItem = player.hasItem(4031034, 0);
					if (!needsItem || player.canGainItem(4031034, 1)) {
						if (needsItem)
							player.gainItem(4031034, 1);
						npc.sayNext("Well then, all you need to do now is to take it and go to\r\n"
								+ "#p1032102# with #b#t5180000##k... Hahaha, best of luck to you!");
					} else {
						//TODO: GMS-like line
						npc.say("Please check whether your ETC. inventory is full.");
					}
				}
			}
		} else if (player.isQuestStarted(2049)) {
			npc.sayNext("Hmmm... You already have #b#t4031034##k. Take that scroll to #b#p1032102##k from #m101000000#.");
		} else {
			npc.say("Hey, are you sure you've met #bMar the Fairy#k? Don't lie to me if you've never met her before because it's obvious. That wasn't even a good lie!!");
		}
		break;
}