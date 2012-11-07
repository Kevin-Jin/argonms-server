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
 * Grendel the Really Old: Magician Instructor (NPC 1032001)
 * Victoria Road: Magic Library (Map 101000003)
 *
 * Magician job advancement NPC.
 *
 * @author GoldenKevin (content from Vana r3171)
 */

//TODO: third job chat from http://trac.assembla.com/DelphiMS/browser/bin/Scripts/NPC/magician.ds
//need to implement player variables.
if (player.getJob() == 0) {
	npc.sayNext("Do you want to be a Magician? You need to meet some requirements in order to do so. You need to be at least at #bLevel 8#k. Let's see if you have what it takes to become a Magician...");
	if (player.getLevel() >= 8) {
		let selection = npc.askYesNo("You definitely have the look of a Magician. You may not be there yet, but I can see the Magician in you...what do you think? Do you want to become the Magician?");
		if (selection == 1) {
			npc.sayNext("Alright, you're a Magician from here on out, since I, Grendel the Really old, the head Magician, allow you so. It isn't much, but I'll give you a little bit of what I have...");

			if (player.getLevel() >= 30) { //For the rare "too high level" instance.
				npc.sayNext("I think you've made the job advancement way too late. Usually, for beginners under Level 29 that were late in making job advancements, we compensate them with lost Skill Points, that weren't rewarded, but...I think you're a little too late for that. I am so sorry, but there's nothing I can do.");
				player.gainSp(1);
			} else {
				player.gainSp((player.getLevel() - 8) * 3 + 1); //Make up any SP for over-leveling like in GMS
			}
			player.setJob(200);
			//don't bother checking whether the inventories are full.
			//1. the user's inventory is not expected to be full by now, and
			//2. the items given are insignificant (just beginner's items)
			player.gainItem(1372043, 1); //Beginner Wand
			let mpinc = 100 + Math.floor(Math.random() * 50); //Extra MP Given
			player.increaseMaxMp(mpinc);
			player.gainEquipInventorySlots(4);
			player.gainUseInventorySlots(4);
			npc.clearBackButton();
			npc.sayNext("You have just equipped yourself with much more magicial power. Please keep training and make yourself much better...I'll be watching you from here and there...");
			npc.clearBackButton();
			npc.sayNext("I just gave you a little bit of #bSP#k. When you open up the #bSkill menu#k on the lower left corner of the screen, there are skills you can learn by using SP's. One warning, though: You can't raise it all together all at once. There are also skills you can acquire only after having ");
			npc.sayNext("One more warning. Once you have chosen your job, try to stay alive as much as you can. Once you reach that level, when you die, you will lose your experience level. You wouldn't want to lose your hard-earned experience points, do you?");
			npc.sayNext("OK! This is all I can teach you. Go to places, train and better yourself. Find me when you feel like you've done all you can, and need something interesting. I'll be waiting for you here...");
			npc.sayNext("Oh, and... if you have any questions about being the Magician, feel free to ask. I don't know EVERYTHING, per se, but I'll help you out with all that I know of. Til then...");
		} else if (selection == 0) {
			npc.sayNext("Really? Have to give more thought to it, huh? Take your time, take your time. This is not something you should take lightly...come talk to me once your have made your decision...");
		}
	} else {
		npc.sayNext("You need more training to be a Magician. In order to be one, you need to train yourself to be more powerful than you are right now. Please come back much stronger.");
	}
} else if (player.getJob() == 200 && player.getLevel() >= 30) {
	if (player.hasItem(4031009, 0) && player.hasItem(4031012, 0)) {
		let selection = npc.askYesNo("Hmmm...you have grown quite a bit since last time. You look much different from before, where you looked weak and small...instead now I can definitely feel you presence as the Magician...so...what do you think? Do you want to get even stronger than you are right now? Pass a simple test and I can do that for you...do you want to do it?");
		if (selection == 0) {
			npc.sayNext("Really? It will help you out a great deal on your journey if you get stronger fast...if you choose to change your mind in the future, please feel free to come back. Know that I'll make you much more powerful than you are right now.");
		} else if (selection == 1) {
			npc.sayNext("Good...you look strong, alright, but I need to see if it is for real. The test isn't terribly difficult and you should be able to pass it. Here, take my letter first. Make sure you don't lose it.");
			if (player.gainItem(4031009, 1))
				npc.sayNext("Please get this letter to #b#p1072001##k around #b#m101020000##k near Ellinia. He's doing the role of an instructor in place of me. He'll give you all the details about it. Best of luck to you...");
			else //TODO: GMS-like line
				npc.say("Please check whether your ETC. inventory is full.");
		}
	} else if (player.hasItem(4031009, 1) && player.hasItem(4031012, 0)) {
		npc.sayNext("Still haven't met the person yet? Find #b#p1072001##k who's around #b#m101020000#k near Ellinia. Give the letter to him and he may let you know what to do.");
	} else if (player.hasItem(4031009, 0) && player.hasItem(4031012, 1)) {
		npc.sayNext("You got back here safely. Well done. I knew you'd pass the tests very easily...alright, I'll make you much stronger now. Before that, though...you need to choose one of the three paths that will be given to you. It will be a tough decision for you to make, but...if you have any questions about it, feel free to ask.");

		let selection = npc.askMenu("Alright, when you have made your decision, click on [I'll choose my occupation!] at the very bottom...\r\n"
				+ "#L0##bPlease explain the characteristics of the Wizard of Fire and Poison.#l#k\r\n"
				+ "#L1##bPlease explain the characteristics of the Wizard of ICe and Lightning.#l#k\r\n"
				+ "#L2##bPlease explain the characteristics of the Cleric.#l#k\r\n"
				+ "#L3##bI'll choose my occupation!#l#k\r\n");
		switch (selection) {
			case 0:
				npc.sayNext("Allow me to explain the Wizard of Fire and Poison. They specialize in fire and poision magic. Skills like #bMeditation#k, that allows you and your whole party's magic ability to increase for a time being, and #bMP Eater#k, which allows you a certain probability of absorbing some of your enemy's MP, are essential to all the attacking Magicians.");
				npc.sayNext("I'll explain to you a magic attack called #bFire Arrow#k. It fires away flamearrows to the enemies, making it the most powerful skill available for the skills in the 2nd level. It'll work best on enemies that are weak against fire in general, for the damage will be much bigger. On the other hand, if you use them on enemies that are strong against fire, the damage will only be half of what it usually is, so keep that in mind.");
				npc.sayNext("I'll explain to you a magic attack called #bPoison Breath#k. It fires away venomous bubbles on the enemies, poisoning them in the process. Once poisoned, the enemy's HP will decrease little by little over time. If the magic doesn't work too well or the monster has high HP, it may be a good idea to fire enough to kill them with the overdose of poison.");
				break;
			case 1:
				npc.sayNext("Allow me to explain the Wizard of Ice and Lightning. They specialize in ice and lightning magic. Skills like #bMeditation#k, that allows you and your whole party's magic ability to increase for a time being, and #bMP Eater#k, which allows you a certain probability of absorbing some of your enemy's MP, are essential to all the attacking Magicians.");
				npc.sayNext("I'll explain to you a magic attack called #bCold Beam#k. It fires away pieces of ice at the enemies, and although not quite as powerful as Fire Arrow, whoever's struck by it will be frozen for a short period of time. The damage increases much more if the enemy happens to be weak against ice. The opposite holds true, too, in that if the enemy is used to ice, the damage won't quite be as much, so keep that in mind.");
				npc.sayNext("I'll explain to you a magic attack called #bThunder Bolt#k. It's the only 2nd-level skill for Magicians that can be considered the Total Spell, affecting a lot of monsters at once. It may not dish out a lot of damage, but the advantage is that it damages all the monsters around you. You can only attack upto six monsters at once, though. Still, it's a pretty incredible attack.");
				break;
			case 2:
				npc.sayNext("Allow me to explain the Cleric. Clerics use religious magic on monsters through prayers and incantation. Skills like #bBless#k, which temporarily improves the weapon def., magic def., accuracy, avoidability, and #bInvincible#k, which decreases the weapon damage for a certain amount, help magicians overcome their shortcomings ...");
				npc.sayNext("Cleric is the only Wizard that can perform recovering magic. Clerics are the only one that can do recovery magic. It's called #bHeal#k, and the more MP, INT's, and the skill level for this skill you have, the more HP you may recover. It also affects your party close by so it's a very useful skill, enabling you to continue to hunt without the help of the potion.");
				npc.sayNext("Clerics also have a magic attack called #bHoly Arrow#k. It's a spell that allows the Cleric to fire away phantom arrows at the monsters. The damage isn't too great, but it can apply tremendous damage to the undead's and other evil-based monsters. Those monsters are very weak against holy attack. What do you think, isn't it interesting, right?");
				break;
			case 3:
				selection = npc.askMenu("Now, have you made up your mind? Please select your occupation for the 2nd job advancement.\r\n"
						+ "#L0##bThe Wizard of Fire and Poison#l#k\r\n"
						+ "#L1##bThe Wizard of Ice and Lightning#l#k\r\n"
						+ "#L2##bCleric#l#k\r\n");
				switch (selection) {
					case 0:
						selection = npc.askYesNo("So you want to make the 2nd job advancement as the #bWizard of Fire and Poison#k? Once you have made your decision, you can't go back and change your job anymore. Are you sure about the decision?");
						if (selection == 0) {
							npc.sayNext("Really? Have to give more thought to it, huh? Take your time, take your time. This is not something you should take lightly ... come talk to me once you have made your decision.");				
						} else if (selection == 1) {
							if (player.getSp() > ((player.getLevel() - 30) * 3)) {
								npc.sayNext("Hmmm...you have too much SP...you can't make the 2nd job advancement with that many SP in store. Use more SP on the skills on the 1st level and then come back.");
							} else {
								player.setJob(210);
								player.gainSp(1);
								player.loseItem(4031012, 1); //Take The Proof of a Hero
								player.gainEtcInventorySlots(4);
								let mpinc = 450 + Math.floor(Math.random() * 50); //Extra MP Given
								player.increaseMaxMp(mpinc);
								npc.sayNext("From here on out, you have become the #bWizard of Fire and Poison#k... Wizards use high intelligence and the power of nature all around us to take down the enemies...please continue your studies, for one day I may make you much more powerful with my own power...");
								npc.sayNext("I have just given you a book that gives you the list of skills you can acquire as the Wizard of Fire and Poison...I've also extended your etc. inventory by added a whole row to it, along with your maximum MP...go see it for yourself.");
								npc.sayNext("I have also given you a little bit of #bSP#k. Open the #bSkill Menu#k located at the bottomleft corner. You'll be able to boost up the newly-acquired 2nd level skills. A word of warning though: You can't boost them up all at once. Some of the skills are only available after you have learned other skills. Make sure to remember that.");
								npc.sayNext("The Wizards have to be strong. But remember that you can't abuse that power and use it on a weakling. Please use your enormous power the right way, because...for you to use that the right way, that is much harder than just getting stronger. Find me after you have advanced much further ...");
							}
						}
						break;
					case 1:
						selection = npc.askYesNo("So you want to make the 2nd job advancement as the #bWizard of Ice and Lightning#k? Once you have made your decision, you can't go back and change your job anymore. Are you sure about the decision?");
						if (selection == 0) {
							npc.sayNext("Really? Have to give more thought to it, huh? Take your time, take your time. This is not something you should take lightly ... come talk to me once you have made your decision.");				
						} else {
							if (player.getSp() > ((player.getLevel() - 30) * 3)) {
								npc.sayNext("Hmmm...you have too much SP...you can't make the 2nd job advancement with that many SP in store. Use more SP on the skills on the 1st level and then come back.");
							} else {
								player.setJob(220);
								player.gainSp(1);
								player.loseItem(4031012, 1); //Take The Proof of a Hero
								player.gainEtcInventorySlots(4);
								let mpinc = 450 + Math.floor(Math.random() * 50); //Extra MP Given
								player.increaseMaxMp(mpinc);
								npc.sayNext("From here on out, you have become the #bWizard of Ice and Lightning#k... Wizards use high intelligence and the power of nature all around us to take down the enemies...please continue your studies, for one day I may make you much more powerful with my own power...");
								npc.sayNext("I have just given you a book that gives you the list of skills you can acquire as the Wizard of Ice and Lightning...I've also extended your etc. inventory by added a whole row to it. Your maximum MP has gone up, too. Go see for it yourself.");
								npc.sayNext("I have also given you a little bit of #bSP#k. Open the #bSkill Menu#k located at the bottomleft corner. You'll be able to boost up the newly-acquired 2nd level skills. A word of warning though: You can't boost them up all at once. Some of the skills are only available after you have learned other skills. Make sure to remember that.");
								npc.sayNext("The Wizards have to be strong. But remember that you can't abuse that power and use it on a weakling. Please use your enormous power the right way, because...for you to use that the right way, that is much harder than just getting stronger. Find me after you have advanced much further. I'll be waiting ...");
							}
						}
						break;
					case 2:
						selection = npc.askYesNo("So you want to make the 2nd job advancement as the #bCleric#k? Once you have made your decision, you can't go back and change your job anymore. Are you sure about the decision?");
						if (selection == 0) {
							npc.sayNext("Really? Have to give more thought to it, huh? Take your time, take your time. This is not something you should take lightly ... come talk to me once you have made your decision.");				
						} else {
							if (player.getSp() > ((player.getLevel() - 30) * 3)) {
								npc.sayNext("Hmmm...you have too much SP...you can't make the 2nd job advancement with that many SP in store. Use more SP on the skills on the 1st level and then come back.");
						} else {
								player.setJob(230);
								player.gainSp(1);
								player.loseItem(4031012, 1); //Take The Proof of a Hero
								player.gainEtcInventorySlots(4);
								let mpinc = 450 + Math.floor(Math.random() * 50); //Extra MP Given
								player.increaseMaxMp(mpinc);
								npc.sayNext("Alright, you're a #bCleric#k from here on out. Clerics blow life into every living organism here with their undying faith in God. Never stop working on your faith...then one day, I'll help you become much more powerful...");
								npc.sayNext("I have just given you a book that gives you the list of skills you can acquire as the Cleric...I've also extended your etc. inventory by added a whole row to it, along with your maximum MP...go see it for yourself.");
								npc.sayNext("I have also given you a little bit of #bSP#k. Open the #bSkill Menu#k located at the bottomleft corner. You'll be able to boost up the newly-acquired 2nd level skills. A word of warning though: You can't boost them up all at once. Some of the skills are only available after you have learned other skills. Make sure to remember that.");
								npc.sayNext("The Cleric needs more faith than anything else. Keep your strong faith in God and treat everyone with respect and dignity they deserve. Keep working hard and you may one day earn more religious magic power...alright...please find me after you have made more strides. I'll be waiting for you...");
							}
						}
						break;
				}
				break;
		}
	}
} else if (Math.floor(player.getJob() / 100) == 2) {
	let selection = npc.askMenu("Any questions about being a Magician?\r\n"
		+ "#L0##bWhat are the basic characteristics of being a Magician?#k#l\r\n"
		+ "#L1##bWhat are the weapons that the Magicians use?#k#l\r\n"
		+ "#L2##bWhat are the armors the Magicians can wear?#k#l\r\n"
		+ "#L3##bWhat are the skills available for Magicians?#k#l");
	switch (selection) {
		case 0:
			npc.sayNext("I'll tell you more about being a Magician. Magicians put high levels of magic and intelligence to good use. They can use the power of nature all around us to kill the enemies, but they are very weak in close combats. The stamina isn't high, either, so be careful and avoid death at all cost.");
			npc.sayNext("Since you can attack the monsters from afar, that'll help you quite a bit. Try boosting up the level of INT if you want to attack the enemies accurately with your magic. The higher your intelligence, the better you'll be able to handle your magic...");
			break;
		case 1:
			npc.sayNext("I'll tell you more about the weapons that Magicians use. Actually, it doesn't mean much for Magicians to attack the opponents with weapons. Magicians lack power and dexterity, so you will have a hard time even defeating a snail.");
			npc.sayNext("If we're talking about the magicial powers, then THAT's a whole different story. The weapons that Magicians use are blunt weapons, staff, and wands. Blunt weapons are good for, well, blunt attacks, but...I would not recommend that on Magicians, period...");
			npc.sayNext("Rather, staffs and wands are the main weaponry of choice. These weapons have special magicial powers in them, so it enhances the Magicians' effectiveness. It'll be wise for you to carry a weapon with a lot of magicial powers in it...");
			break;
		case 2:
			npc.sayNext("I'll tell you more about the armors that Magicians can wear. Honestly, the Magicians don't have much armor to wear since they are weak in physiical strength and low in stamina. Its defensive abilities isn't great either, so I don't know if it helps a lot or not...");
			npc.sayNext("Some armors, however, have the ability to eliminate the magicial power, so it can guard you from magic attacks. It won't help much, but still better than not warning them at all...so buy them if you have time...");
			break;
		case 3:
			npc.sayNext("The skills available for Magicians use the high levels of intelligence and magic that Magicians have. Also available are Magic Guard and Magic Armor, which help Magicians with weak stamina prevent from dying.");
			npc.sayNext("The offensive skills are #bEnergy Bolt#k and #bMagic Claw#k. First, Energy Bolt is a skill that applies a lot of damage to the opponent with minimal use of MP.");
			npc.sayNext("Magic Claw, on the other hand, uses up a lot of MP to attack one opponent TWICE. But, you can only use Energy Bolt once it's more than 1, so keep that in mind. Whatever you choose to do, it's all upto you...");
			break;
	}
} else {
	npc.sayNext("Would you like to have the power of nature in itself in your hands? It may be a long, hard road to be on, but you'll surely be rewarded in the end, reaching the very top of wizardry...");
}