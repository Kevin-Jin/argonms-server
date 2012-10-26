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
 * Athena Pierce: Bowman Instructor (NPC 1012100)
 * Victoria Road: Bowman Instructional School (Map 100000201)
 *
 * Bowman job advancement NPC.
 *
 * @author GoldenKevin (content from Vana r3171)
 */

//TODO: third job chat from http://trac.assembla.com/DelphiMS/browser/bin/Scripts/NPC/bowman.ds
//need to implement player variables.
if (player.getJob() == 0) {
	npc.sayNext("So you want to become the Bowman??? Well...you need to meet some requirements to do so...at least #bLevel10, and 25 of DEX#k. Let's see...hmm...");
	if (player.getLevel() >= 10 && player.getDex() >= 25) {
		let selection = npc.askYesNo("You look qualified for this. With a great pair of eyes being able to spot the real monsters and have the coldhearted skills to shoot the arrow through them...we needed someone like that. Do you want to become a Bowman?");
		if (selection == 1) {
			npc.sayNext("Alright! You are the Bowman from here on out, because I said so...haha here's a little bit of my power to you...Haahhhh!");

			if (player.getLevel() >= 30) { //For the rare "too high level" instance.
				npc.sayNext("I think you've made the job advancement way too late. Usually, for beginners under Level 29 that were late in making job advancements, we compensate them with lost Skill Points, that weren't rewarded, but...I think you're a little too late for that. I am so sorry, but there's nothing I can do.");
				player.gainSp(1);
			} else {
				player.gainSp((player.getLevel() - 10) * 3 + 1); //Make up any SP for over-leveling like in GMS
			}
			player.setJob(300);
			player.gainItem(1452051, 1); //Beginner Bow
			player.gainItem(2060000, 2000); //Arrow for Bow
			player.gainItem(2060000, 2000); //Arrow for Bow
			player.gainItem(2060000, 2000); //Arrow for Bow
			let hpinc = 100 + Math.floor(Math.random() * 50); //Extra HP Given
			let mpinc = 25 + Math.floor(Math.random() * 25); //Extra MP Given
			player.increaseMaxHp(hpinc);
			player.increaseMaxMp(mpinc);
			player.gainEquipInventorySlots(4);
			player.gainUseInventorySlots(4);
			npc.clearBackButton();
			npc.sayNext("I have added slots for your equipment and etc. inventory. You have also gotten much stronger. Train harder, and you may one day reach the very top of the bowman. I'll be watching you from afar. Please work hard.");
			npc.clearBackButton();
			npc.sayNext("I just gave you a little bit of #bSP#k. When you open up the #bSkill menu#k on the lower left corner of the screen, there are skills you can learn by using SP's. One warning, though: You can't raise it all together all at once. There are also skills you can acquire only after having learned a couple of skills first.");
			npc.sayNext("One more warning. Once you have chosen your job, try to stay alive as much as you can. Once you reach that level, when you die, you will lose your experience level. You wouldn't want to lose your hard-earned experience points, do you?");
			npc.sayNext("OK! This is all I can teach you. Go to places, train and better yourself. Find me when you feel like you've done all you can, and need something interesting. I'll be waiting for you.");
			npc.sayNext("Oh, and... if you have any other questions about being the Bowman, feel free to ask. I don't every single thing about  being the bowman, but I'll answer as many questions as I can. Til then...");
		} else {
			npc.sayNext("Really? Have to give more though to it, huh? Take your time, take your time. This is not something you should take lightly...come talk to me once you have made your decision.");
		}
	} else {
		npc.sayNext("You need to train more. Don't think being the Bowman is a walk in the park...");
	}
} else if (player.getJob() == 300 && player.getLevel() >= 30) {
	if (player.hasItem(4031010, 0) && player.hasItem(4031012, 0)) {
		let selection = npc.askYesNo("Hmmm...you have grown a lot since I last saw you. I don't see the weakling I saw before, and instead, look much more like a bowman now. Well, what do you think? Don't you want to get even more powerful than that? Pass a simple test and I'll do just that for you. Do you want to do it?");
		if (selection == 0) {
			npc.sayNext("Really? Have to give it more thought, huh? Take your time, take your time. This is not something you should take lightly...come talk to me once you have made your decision.");
		} else if (selection == 1) {
			npc.sayNext("Good decision. You look strong, but I need to see if you really are strong enough to pass the test. It's not a difficult test, so you'll do just fine. Here, take my letter first...make sure you don't lose it!");
			player.gainItem(4031010, 1);
			npc.sayNext("Please get this letter to #b#p1072002##k who's around #b#m106010000##k near Henesys. She's taking care of the the job of an instructor in place of me. Give her the letter and she'll test you in place of me. Best of luck to you.");
		}
	} else if (player.hasItem(4031010, 1) && player.hasItem(4031012, 0)) {
		npc.sayNext("Still haven't met the person yet? Find #b#p1072002##k who's around #b#m106010000##k near Henesys. Give the letter to her and she may let you know what to do.");
	} else if (player.hasItem(4031010, 0) && player.hasItem(4031012, 1)) {
		npc.sayNext("Haha...I knew you'd breeze through that test. I'll admit, you are a great bowman. I'll make you much stronger than you are right now. Before that, however...you'll need to choose one of two ppaths given to you. It'll be a difficult decision for you to make, but...if there's any question to ask, please do so.");

		selection = npc.askMenu("Alright, when you have made your decision, click on [I'll choose my occupation!] at the very bottom.\r\n"
				+ "#L0##bPlease explain to me what being the Hunter is all about.#k#l\r\n"
				+ "#L1##bPlease explain to me what being the Crossbowman is all about.#k#l\r\n"
				+ "#L2##bI'll choose my occupation!#k#l\r\n");
		switch (selection) {
			case 0:
				npc.sayNext("Ok. This is what being the Hunter is all about. Hunters have skills such as Bow Mastery and Bow Booster that enables you to use bows well. There's also a skill called Soul Arrow : Bow for the Hunters that waste quite a few arrows. It allows you to fire away arrows for a long period of time without actually wasting the arrows, so if you may have spent some mesos before on arrows, this may be just for you...");
				npc.sayNext("I'll explain to you more about one of the skills of the Hunter, #bPower Knock-Back#k. No one beats Hunter in terms of long-range attacks, but it's a whole different story when there's a lot of enemies or if you need to attack them up close. Therefore, it makes this skill very important to acquire. It allows you not only to strike the enemy up close, but also send multiple monsters far back. It's a very important skill to have to acquire some much-needed space.");
				npc.sayNext("I'll explain to you the offensive skill of the Hunter, #bArrow Bomb : Bow#k. It's a skill that allows you to fire away arrows with bombs. If struck just right, the bomb will go off on the enemy, damaging those around it and temporarily knocking them out. Combine that skill with the Critical Shot, the first level skill, and the damage will be incredible. You should try becoming the Hunter for your job advancement.");
				break;
			case 1:
				npc.sayNext("Ok. This is what being the Crossbowman is all about. For the Crossbowman, skills like Crossbow Mastery and Crossbow Booster are available along with Soul Arrow : Bow for those who wastes the bows by shooting a lot and missing a lot. This skill enables the player to shoot the arrows for a long period of time without wasting the bows, so if you have been spending a lot of mesos on bows, you may want to check it out...");
				npc.sayNext("Ok. One of the skills that the Crossbowman can have is #bPower Knock-Back#k. No one can approach the long-distance attacks of the Crossbowman, but it's a different story altogether when talking about close combats or facing lots of enemies at once. For that, this is a very important skill to acquire. It allows you to strike down the enemy with full force, sending a number of enemies far back in the process. A very important skill that provides you with some much-needed space.");
				npc.sayNext("Ok, I'll explain to you one of the attacking skills for the Crossbowman, #bIron Arrow : Crossbow#k. This skill enables you to attack multiple enemies, as the arrow that hits a monster will go through it and hit another monster behind one. The damage decreases an arrow goes through an enemy, but it can still attack a number of enemies at once, a very Threaten skill to have. And...if it's combined with Critical Shot...that will be just incredible.");
				break;
			case 2:
				selection = npc.askMenu("Hmmm, have you made up your mind? Then choose the 2nd job advancement of your liking.\r\n"
						+ "#L0##bHunter#k#l\r\n"
						+ "#L1##bCrossbowman#k#l\r\n");
				switch (selection) {
					case 0:
						selection = npc.askYesNo("So you want to make the second job advancement as the #bHunter#k? You know you won't be able to choose a different job for the second job advancement once you make your decision here, right?");
						if (selection == 0) {
							npc.sayNext("Really? Have to give more thought to it, huh? Take your time, take your time. This is not something you should take lightly ... come talk to me once you have made your decision.");                         
						} else if (selection == 1) {
							if (player.getSp() > ((player.getLevel() - 30) * 3)) {
								npc.sayNext("Hmmm...you have too much SP...you can't make the 2nd job advancement with that many SP in store. Use more SP on the skills on the 1st level and then come back.");
							} else {
								player.setJob(310);
								player.gainSp(1);
								player.loseItem(4031012, 1); //Take The Proof of a Hero
								player.gainEtcInventorySlots(4);
								let hpinc = 300 + Math.floor(Math.random() * 50); //Extra HP Given
								let mpinc = 150 + Math.floor(Math.random() * 50); //Extra MP Given
								player.increaseMaxHp(hpinc);
								player.increaseMaxMp(mpinc);
								npc.sayNext("Alright, you're the #bHunter#k from here on out. Hunters are the intelligent bunch with incredible vision, able to pierce the arrow through the heart of the monsters with ease...please train yourself each and everyday. We'll help you become even stronger than you already are.");
								npc.sayNext("I have just given you a book that gives you the the list of skills you can acquire as a hunter. Also your etc. inventory has expanded by adding another row to it. Your max HP and MP have also increased, too. Go check and see for it yourself.");
								npc.sayNext("I have also given you a little bit of #bSP#k. Open the #bSkill Menu#k located at the bottomleft corner. You'll be able to boost up the newly-acquired 2nd level skills. A word of warning though: You can't boost them up all at once. Some of the skills are only available after you have learned other skills. Make sure to remember that.");
								npc.sayNext("Hunters need to be strong. But remember that you can't abuse that power and use it on a weakling. Please use your enormous power the right way, because...for you to use that the right way, that is much harder than just getting stronger. Find me after you have advanced much further. I'll be waiting for you.");
							}
						}
						break;
					case 1:
						selection = npc.askYesNo("So you want to make the second job advancement as the #bCrossbowman#k? You know you won't be able to choose a different job for the second job advancement once you make your decision here, right?");
						if (selection == 0) {
							npc.sayNext("Really? Have to give more thought to it, huh? Take your time, take your time. This is not something you should take lightly ... come talk to me once you have made your decision.");
						} else if (selection == 1) {
							if (player.getSP() > ((player.getLevel() - 30) * 3)) {
								npc.sayNext("Hmmm...you have too much SP...you can't make the 2nd job advancement with that many SP in store. Use more SP on the skills on the 1st level and then come back.");
							} else {
								player.setJob(320);
								player.gainSp(1);
								player.loseItem(4031012, 1); //Take The Proof of a Hero
								player.gainEtcInventorySlots(4);
								let hpinc = 300 + Math.floor(Math.random() * 50); //Extra HP Given
								let mpinc = 150 + Math.floor(Math.random() * 50); //Extra MP Given
								player.increaseMaxHp(hpinc);
								player.increaseMaxMp(mpinc);
								npc.sayNext("Alright, you're the #bCrossbowman#k from here on out. Crossbowman are the intelligent bunch with incredible vision, able to pierce the arrow through the heart of the monsters with ease...please train yourself each and everyday. We'll help you become even stronger than you already are.");
								npc.sayNext("I have just given you a book that gives you the the list of skills you can acquire as a hunter. Also your etc. inventory has expanded by adding another row to it. Your max HP and MP have also increased, too. Go check and see for it yourself.");
								npc.sayNext("I have also given you a little bit of #bSP#k. Open the #bSkill Menu#k located at the bottomleft corner. You'll be able to boost up the newly-acquired 2nd level skills. A word of warning though: You can't boost them up all at once. Some of the skills are only available after you have learned other skills. Make sure to remember that.");
								npc.sayNext("Crossbowmen need to be strong. But remember that you can't abuse that power and use it on a weakling. Please use your enormous power the right way, because...for you to use that the right way, that is much harder than just getting stronger. Find me after you have advanced much further. I'll be waiting for you.");
							}
						}
						break;
				}
				break;
		}
	}
} else if (Math.floor(player.getJob() / 100) == 3) {
	let selection = npc.askMenu("Do you have any questions regarding the life of the Bowman?\r\n"
			+ "#L0##bWhat are the basic characters of a Bowman?#k#l\r\n"
			+ "#L1##bWhat are the weapons that the Bowman use?#k#l\r\n"
			+ "#L2##bWhat are the armors that the bowman can wear?#k#l\r\n"
			+ "#L3##bWhat are the skills of the Bowman?#k#l");
	switch (selection) {
		case 0:
			npc.sayNext("This is what being a bowman is all about. The bowman possesses just enough stamina and strength. Their most important ability to use is DEX. They don't have much of a stamina, so please avoid close combat if possible.");
			npc.sayNext("The main advantage is that you can attack from afar, enabling you to avoid many close attacks by the monsters. Not only that, but with high dexterity, you can avoid quite attacks up close. The higher the DEX, the more damage you can dish out.");
			break;
		case 1:
			npc.sayNext("I'll explain the weapons that bowman use. Instead of using weapons to strike or slash the opponents, they use long-distance weapons such as bows and rockbows to kill the monsters. They both have their share of advantages and disadvantages.");
			npc.sayNext("Bows aren't as powerful as the rockbows, but they are much quicker to attack with. Rockbows, on the other hand, are more powerful with less quickness. It'll be hard for you to make a decision on this...");
			npc.sayNext("Good arrows and rockbows are available through monsters, so it's a must that you hunt often. It won't be easy to obtain, however. Train yourself harder each and everyday, and you'll see some success coming your way ...");
			break;
		case 2:
			npc.sayNext("I'll explain the armors the bowman use. They need to move around quickly so it won't be any good to put on huge, elaborate armor. Clothes with long cumbersome laces are definitely off limits.");
			npc.sayNext("But if you wear huge stiff armor that the warriors don, you'll be surrounded by the enemies in no time. Equip yourself with simple, comfortable armor that fits you just fine and still does the job. It'll help you a great deal when hunting down the monsters.");
			break;
		case 3:
			npc.sayNext("For bowman, skills that are available are the ones that puts their high accuracy and dexterity to good use. It's a must for the Bowman to acquire skills that allows them to attack the enemies accurately.");
			npc.sayNext("There are two kinds of offensive skills for the bowman: #bArrow Blow#k and #bDouble Shot#k. Arrow Blow is a nice, basic skills that allows you to highly damage the enemy with minimal use of MP.");
			npc.sayNext("On the other hand, Double Shot allows you to attack the enemy twice using some MP. You'll only be able to get it after boosting Arrow Blow to at least higher than 1, so remember that. Whatever the choice, make it your own.");
			break;
	}
} else {
	npc.sayNext("Don't you want to feel the excitement of hunting down the monsters from out of nowhere? Only the Bowman can do that...");
}