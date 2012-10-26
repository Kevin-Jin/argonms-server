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
 * Dark Lord: Thief Instructor (NPC 1052001)
 * Victoria Road: Thieves' Hideout (Map 103000003)
 *
 * Thief job advancement NPC.
 *
 * @author GoldenKevin (content from Vana r3171)
 */

//TODO: third job chat from http://trac.assembla.com/DelphiMS/browser/bin/Scripts/NPC/rogue.ds
//need to implement player variables.
if (player.isQuestStarted(6141)) {
	let selection = npc.askYesNo("Would you like to go to the training ground?");
	if (selection == 0) {
		npc.say("Let me know when you want to enter.");
	} else if (selection == 1) {
		//TODO: implement instance maps - only warp if empty
		//player.changeMap(910300000);
		npc.say("Someone is already inside. Try again later.");
	}
} else if (player.getJob() == 0) {
	npc.sayNext("Want to be a thief? There are some standards to meet, because we can't just accept EVERYONE in ... #bYour level should be at least 10#k. Let's see...");
	if (player.getLevel() >= 10) {
		let selection = npc.askYesNo("Oh...! You look like someone that can definitely be a part of us...all you need is a little sinister mind, and...yeah...so, what do you think? Wanna be the Rouge?");
		if (selection == 1) {
			npc.sayNext("Alright, from here on out, you are part of us! You'll be living the life of a wanderer at first, but just be patient and soon, you'll be living the high life. Alright, it ain't much, but I'll give you some of my abilities...HAAAHHH!!");

			if (player.getLevel() >= 30) { //For the rare "too high level" instance.
				npc.sayNext("I think you've made the job advancement way too late. Usually, for beginners under Level 29 that were late in making job advancements, we compensate them with lost Skill Points, that weren't rewarded, but...I think you're a little too late for that. I am so sorry, but there's nothing I can do.");
				player.gainSp(1);
			} else {
				player.gainSp((player.getLevel() - 10) * 3 + 1); //Make up any SP for over-leveling like in GMS
			}
			player.setJob(400);
			player.gainItem(1472061, 1); //Beginners Garnier
			player.gainItem(1332063, 1); //Beginners Dagger
			player.gainItem(2070015, 3); //Special subis
			let hpinc = 100 + Math.floor(Math.random() * 50); //Extra HP Given
			let mpinc = 25 + Math.floor(Math.random() * 25); //Extra MP Given
			player.increaseMaxHp(hpinc);
			player.increaseMaxMp(mpinc);
			player.gainEquipInventorySlots(4);
			player.gainEtcInventorySlots(4);
			npc.clearBackButton();
			npc.sayNext("I've just created more slots for your equipment and etc. storage. Not only that, but you've also gotten stronger as well. As you become part of us, and learn to enjoy life in different angles, you may one day be on top of this of darkness. I'll be watching your every move, so don't let me down.");
			npc.clearBackButton();
			npc.sayNext("I just gave you a little bit of #bSP#k. When you open up the #bSkill menu#k on the lower left corner of the screen, there are skills you can learn by using SP's. One warning, though: You can't raise it all together all at once. There are also skills you can acquire only after having learned a couple of skills first.");
			npc.sayNext("One more warning. Once you have chosen your job, try to stay alive as much as you can. Once you reach that level, when you die, you will lose your experience level. You wouldn't want to lose your hard-earned experience points, do you?");
			npc.sayNext("OK! This is all I can teach you. Go to places, train and better yourself. Find me when you feel like you've done all you can, and need something interesting. Then, and only then, I'll hook you up with more interesting experiences...");
			npc.sayNext("Oh, and... if you have any questions about being the Thief, feel free to ask. I don't know EVERYTHING, but I'll help you out with all that I know of. Til then...");
		} else {
			npc.sayNext("Oh, and... if you have any questions about being the Thief, feel free to ask. I don't know EVERYTHING, but I'll help you out with all that I know of. Til then...");
		}
	} else {
		npc.sayNext("I see...well, it's a very important step to take, choosing your job. But don't you want to live the fun life? let me know when you have made up your mind, ok?");
	}
} else if (player.getJob() == 400 && player.getLevel() >= 30) {
	if (player.hasItem(4031011, 0) && player.hasItem(4031012, 0)) {
		let selection = npc.askYesNo("Hmmm...you seem to have gotten a whole lot stronger. You got rid of the old, weak self and and look much more like a thief now. Well, what do you think? Don't you want to get even more powerful than that? Pass a simple test and I'll do just that for you. Do you want to do it?");
		if (selection == 0) {
			npc.sayNext("Really? It will help you out a great deal on your journey if you get stronger fast...if you choose to change your mind in the future, please feel free to come back. Know that I'll make you much more powerful than you are right now.");
		} else if (selection == 1) {
			npc.sayNext("Good thinking. But, I need to make sure you are as strong as you look. It's not a hard test, one that should be easy for you to pass. First, take this letter...make sure you don't lose it.");
			player.gainItem(4031011, 1);
			npc.sayNext("Please take this letter to #b#p1072003##k around #b#m102040000##k near Kerning City. He's doing the job of an instructor in place of me. Give him the letter and he'll give you the test for me. If you want more details, hear it straight from him. I'll be wishing you good luck.");
		}
	} else if (player.hasItem(4031010, 1) && player.hasItem(4031012, 0)) {
		npc.sayNext("Still haven't met the person yet? Find #b#p1072003##k who's around #b#m102040000##k near Kerning City. Give the letter to him and he may let you know what to do.");
	} else if (player.hasItem(4031010, 0) && player.hasItem(4031012, 1)) {
		npc.sayNext("Hmmm...so you got back here safely. I knew that test would be too easy for you. I admit, you are a great great thief. Now...I'll make you even more powerful than you already are. But, before all that...you need to choose one of two ways. It'll be a difficult decision for you to make, but...if you have any questions, please ask.");

		selection = npc.askMenu("Alright, when you have made your decision, click on [I'll choose my occupation!] at the very bottom...\r\n"
				+ "#L0##bPlease explain the characteristics of the Assassin.#k#l\r\n"
				+ "#L1##bPlease explain the characteristics of the Bandit.#k#l\r\n"
				+ "#L2##bI'll choose my occupation!#k#l\r\n");
		switch (selection) {
			case 0:
				npc.sayNext("Let me explain the role of the Assassin. Assassin is the Thief that uses throwing stars or daggers. Skills like #bClaw Mastery#k and #bCritical Throw#k will help you use your throwing stars better. Boost Claw Mastery up more and your maximum number of throwing stars increases, so it'll be best to learn it. Please remember that.");
				npc.sayNext("I'll explain to you one of the skills of the Assassin, #bHaste#k. It temporarily boost up you and your party members' abilities and moving speed, perfect when facing enemies that are really fast. It's also useful when walking to a place far far away. Wouldn't it be much nicer to get to your destination on time as opposed to taking a whole day just to get there?");
				npc.sayNext("And this is the over skill available for the Assassin, #bDrain#k. It allows you to take back a portion of the damage you dished out on an enemy and absorb it as HP! The more the damage, the more you'll regain health...how awesome is that? Remember the most you can absorb at once is half of your maximum HP. The higher the enemy's HP, the more you can take away.");
				break;
			case 1:
				npc.sayNext("This is what being the Bandit is all about. Bandits are thieves who specialize in using daggers. Skills like #bDagger Mastery#k and #bDagger Booster#k will help you use your dagger better. Daggers have quick attacking speed to begin with, and if you add that with a booster, then...oh my! Fast enough to scare the crap out of the monsters!");
				npc.sayNext("I'll explain to you what #bSteal#k does for Bandits. It gives you a certain probability to let you steal an item from an enemy. You may only steal once from one enemy, but you can keep trying until you succeed from it. The stolen item will be dropped onto the ground; make sure you pick it up first because it's anyone's to grab once it's dropped.");
				npc.sayNext("I'll explain to you what #bSavage Blow#k does for Bandits. It uses up HP and MP to attack the enemy 6 TIMES with the dagger. The higher the skill level, the more the attacks may occur. You'll cut up the enemy to pieces with the dagger...ooooh, isn't it sweet? What do you think? Want to become a Bandit and feel the adrenaline rush that comes with it?");
				break;
			case 2:
				selection = npc.askMenu("Hmmm, have you made up your mind? Then choose the 2nd job advancement of your liking.\r\n"
						+ "#L0##bAssassin#k#l\r\n"
						+ "#L1##bBandit#k#l\r\n");
				switch (selection) {
					case 0:
						selection = npc.askYesNo("So you want to make the 2nd job advancement as the #bAssassin#k? Once you have made the decision, you can't go back and change your mind. You ARE sure about this, right?");
						if (selection == 0) {
							npc.sayNext("Really? Have to give more thought to it, huh? Take your time, take your time. This is not something you should take lightly ... come talk to me once you have made your decision.");				
						} else if (selection == 1) {
							if (player.getSp() > ((player.getLevel() - 30) * 3)) {
								npc.sayNext("Hmmm...you have too much SP...you can't make the 2nd job advancement with that many SP in store. Use more SP on the skills on the 1st level and then come back.");
							} else {
								player.setJob(410);
								player.gainSp(1);
								player.loseItem(4031012, 1); //Take The Proof of a Hero
								player.gainUseInventorySlots(4);
								let hpinc = 300 + Math.floor(Math.random() * 50); //Extra HP Given
								let mpinc = 150 + Math.floor(Math.random() * 50); //Extra MP Given
								player.increaseMaxHp(hpinc);
								player.increaseMaxMp(mpinc);
								npc.sayNext("Alright, from here on out you are the #bAssassin#k. Assassins revel in shadows and darkness, waiting until the right time comes for them to stick a dagger through the enemy's heart, suddenly and swiftly...please keep training. I'll make you even more powerful than you are right now!");
								npc.sayNext("I have just given you a book that gives you the the list of skills you can acquire as an assassin. I have also added a whole row to your use inventory, along with boosting up your max HP and MP...go see for it yourself.");
								npc.sayNext("I have also given you a little bit of #bSP#k. Open the #bSkill Menu#k located at the bottomleft corner. You'll be able to boost up the newly-acquired 2nd level skills. A word of warning though: You can't boost them up all at once. Some of the skills are only available after you have learned other skills. Make sure to remember that.");
								npc.sayNext("Assassins have to be strong. But remember that you can't abuse that power and use it on a weakling. Please use your enormous power the right way, because...for you to use that the right way, that is much harder than just getting stronger. Find me after you have advanced much further.");
							}
						}
						break;
					case 1:
						selection = npc.askYesNo("So you want to make the 2nd job advancement as the #bBandit#k? Once you have made the decision, you can't go back and change your mind. You ARE sure about this, right?");
						if (selection == 0) {
							npc.sayNext("Really? Have to give more thought to it, huh? Take your time, take your time. This is not something you should take lightly ... come talk to me once you have made your decision.");				
						} else if (selection == 1) {
							if (player.getSp() > ((player.getLevel() - 30) * 3)) {
								npc.sayNext("Hmmm...you have too much SP...you can't make the 2nd job advancement with that many SP in store. Use more SP on the skills on the 1st level and then come back.");
							} else {
								player.setJob(420);
								player.gainSp(1);
								player.loseItem(4031012, 1); //Take The Proof of a Hero
								player.gainUseInventorySlots(4);
								let hpinc = 300 + Math.floor(Math.random() * 50); //Extra HP Given
								let mpinc = 150 + Math.floor(Math.random() * 50); //Extra MP Given
								player.increaseMaxHp(hpinc);
								player.increaseMaxMp(mpinc);
								npc.sayNext("Alright from here on out, you're the #bBandit#k. Bandits have quick hands and quicker feet to dominate the enemies. Please keep training. I'll make you even more powerful than you are right now.");
								npc.sayNext("I have just given you a book that gives you the the list of skills you can acquire as an assassin. I have also added a whole row to your use inventory, along with boosting up your max HP and MP...go see for it yourself.");
								npc.sayNext("I have also given you a little bit of #bSP#k. Open the #bSkill Menu#k located at the bottomleft corner. You'll be able to boost up the newly-acquired 2nd level skills. A word of warning though: You can't boost them up all at once. Some of the skills are only available after you have learned other skills. Make sure to remember that.");
								npc.sayNext("Assassins have to be strong. But remember that you can't abuse that power and use it on a weakling. Please use your enormous power the right way, because...for you to use that the right way, that is much harder than just getting stronger. Find me after you have advanced much further.");
							}
						}
						break;
				}
				break;
		}
	}
} else if (Math.floor(player.getJob() / 100) == 3) {
	let selection = npc.askMenu("Do you have anything you want to know about thieves?\r\n"
			+ "#L0##bWhat are the basic characters of a Thief?#k#l\r\n"
			+ "#L1##bWhat are the weapons that the Thieves use?#k#l\r\n"
			+ "#L2##bWhat are the armors that the Thieves use?#k#l\r\n"
			+ "#L3##bWhat are the skills available for Thieves?#k#l");
	switch (selection) {
		case 0:
			npc.sayNext("Let me explain to you what being a thief means. Thieves have just the right amount of stamina and strength to survive. We don't recommend training for strength just like those warriors. What we do need are LUK and DEX...");
			npc.sayNext("If you raise the level of luck and dexterity, that will increase the damage you'll dish out to the enemies. Thieves also differentiate themselves from the rest by using such throwing weapons as throwing stars and throwing knives. They can also avoid many attacks with high dexterity.");
			break;
		case 1:
			npc.sayNext("Thieves use these weapons. They have just the right amount of intelligence and power...what we do have are quick movements and even quick brain...");
			npc.sayNext("Because of that, we usually use daggers or throwing weapons. Daggers are basically small swords but they are very quick, enabling you to squeeze in many attacks. If you fight in a close combat, use the dagger to quickly eliminate the enemy before it reacts to your attack.");
			npc.sayNext("For throwing weapons there are throwing-stars and throwing-knives available. You can't just use them by themselves, though. Go to the weapon store at Kerning City, and they'll sell an claw call Garnier. Equip yourself with it, then you'll be able to throw the throwing stars that's in the use inventory.");
			npc.sayNext("Not only is it important to choose the right Claw to use, but it's just as important to choose the right kind of throwing stars to use. Do you want to know where to get those stars? Go check out the armor store around this town...there's probably someone that handles those specifically...");
			break;
		case 2:
			npc.sayNext("Let me explain to you the armors that the thieves can wear. Thieves value quick so you need clothes that fit you perfectly. But then again, they don't need chained armors like the bowman, because it won't help you one bit.");
			npc.sayNext("Instead of flashy clothes or tough, hard gold-plated armor, try putting on simple, comfortable clothes that fit you perfectly and still do its job in the process. It'll help you a lot in hunting the monsters.");
			break;
		case 3:
			npc.sayNext("For thieves there are skills that supports high accuracy and dexterity we have in general. A good mix of skills are available, for both the throwing stars and daggers. Choose your weapon carefully, for skills are determined by it.");
			npc.sayNext("If you are using throwing-stars, skills like #bKeen Eyes#k or #bLucky Seven#k are perfect. Lucky Seven allows you to throw multiple throwing-stars at once, so it'll help you greatly in hunting down enemies.");
			npc.sayNext("If you are using daggers choose #bDisorder#k or #bDouble Stab#k as skills. Double Stab, in fact, will be a great skills to use in that it enables you to attack an enemy in a blinding flurry of stabs, a definate stunner indeed.");
			break;
	}
} else {
	npc.sayNext("Exploring is good, and getting stronger is good and all... but don't you want to enjoy living the life as you know it? How about becoming a Rouge like us and really LIVE the life? Sounds fun, isn't it?");
}