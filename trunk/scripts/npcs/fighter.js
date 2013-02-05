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
 * Dances with Balrog: Warrior Instructor (NPC 1022000)
 * Victoria Road: Warriors' Sanctuary (Map 102000003)
 *
 * Warrior job advancement NPC.
 *
 * @author GoldenKevin (content from Vana r3171)
 */

//TODO: third job chat from http://trac.assembla.com/DelphiMS/browser/bin/Scripts/NPC/fighter.ds
//need to implement player variables.
if (player.getJob() == 0) {
	npc.sayNext("Do you wish to be a Warrior? You need to meet some criteria in order to do so. #bYou need to be at least in Level 10#k. Let's see...");
	if (player.getLevel() >= 10) {
		let selection = npc.askYesNo("You definitely have the look of a Warrior. You may not be there just yet, but I can see the Warrior in you. What do you think? Do you want to become a Warrior?");
		if (selection == 1) {
			npc.sayNext("From here on out, you are going to be the Warrior! Please continue working hard...I'll enhance your abilities a bit with the hope of you training yourself to be even stronger than you're now. Haaaaaap!!");

			if (player.getLevel() >= 30) { //For the rare "too high level" instance.
				npc.sayNext("I think you've made the job advancement way too late. Usually, for beginners under Level 29 that were late in making job advancements, we compensate them with lost Skill Points, that weren't rewarded, but...I think you're a little too late for that. I am so sorry, but there's nothing I can do.");
				player.gainSp(1);
			} else {
				player.gainSp((player.getLevel() - 10) * 3 + 1); //Make up any SP for over-leveling like in GMS
			}
			player.setJob(100);
			//don't bother checking whether the inventories are full.
			//1. the user's inventory is not expected to be full by now, and
			//2. the items given are insignificant (just beginner's items)
			player.gainItem(1302077, 1); //Beginner Sword
			let hpinc = 200 + Math.floor(Math.random() * 50); //Extra HP Given
			player.increaseMaxHp(hpinc);
			player.gainEquipInventorySlots(4);
			player.gainUseInventorySlots(4);
			player.gainSetupInventorySlots(4);
			player.gainEtcInventorySlots(4);
			npc.clearBackButton();
			npc.sayNext("You've gotten much stronger now. Plus every single one of your inventories have added slots. A whole row, to be exact. Go see for it yourself. I just gave you a little bit of #bSP#k. When you open up the #bSkill menu#k on the lower left corner of the screen, there are skills you can learn by using SP's. One warning, though: You can't raise it all together all at once. There are also skills you can accquire only after having learned a couple of skills first.");
			npc.sayNext("One more warning. Once you have chosen your job, try to stay alive as much as you can. Once you reach that level, when you die, you will lose your experience level. You wouldn't want to lose your hard-earned experience points, do you? This is all i can teach you...from here on out, it's all about pushing yourself harder and become better. See me after you feel that you have gotten much more powerful than you are right now.");
			npc.sayNext("Oh, and... if you have questions about being a Warrior, feel free to ask. I don't know EVERYTHING, but I'll help you out with all that I know of. Til then...");
		} else if (selection == 0) {
			npc.sayNext("Really? Do you need more time to give more thought to it? By all means... this is not something you should take lightly. Come talk to me once your have made your decision.");
		}
	} else {
		npc.sayNext("You need more training to be a Warrior. In order to be one, you need to train yourself to be more powerful than you are right now. Please come back much stronger.");
	}
} else if (player.getJob() == 100 && player.getLevel() >= 30) {
	if (player.hasItem(4031008, 0) && player.hasItem(4031012, 0)) {
		let selection = npc.askYesNo("Whoa! You have definitely grown up! You don't look small and weak anymore...rather, now I can feel your presence as the Warrior! Impressive...so, what do you think? Do you want to get even stronger than you are right now? Pass a simple test and I'll do just that! Wanna do it?");
		if (selection == 0) {
			npc.sayNext("Really? It will help you out a great deal on your journey if you get stronger fast...if you choose to change your mind in the future, please feel free to come back. Know that I'll make you much more powerful than you are right now.");
		} else if (selection == 1) {
			npc.sayNext("Good thinking. You look strong, don't get me wrong, but there's still a need to test your strength and see if you are for real. The test isn't too difficult, so you'll do just fine...Here, take this letter first. Make sure you don't lose it.");
			if (player.gainItem(4031008, 1))
				npc.sayNext("Please get this letter to #b#p1072000##k who may be around #b#m102020300##k that's near Perion. He's the one being the instructor now in place of me, as I am busy here. Get him the letter and he'll give the test in place of me. For more details, hear it straight from him. Best of luck to you.");
			else //TODO: GMS-like line
				npc.say("Please check whether your ETC. inventory is full.");
		}
	} else if (player.hasItem(4031008, 1) && player.hasItem(4031012, 0)) {
		npc.sayNext("Still haven't met the person yet? Find #b#p1072000##k who's around #b#m102020300##k near Perion. Give the letter to him and he may let you know what to do.");
	} else if (player.hasItem(4031008, 0) && player.hasItem(4031012, 1)) {
		npc.sayNext("OHH...you came back safe! I knew you'd breeze through...I'll admit you are a strong, formidable warrior...alright, I'll make you an even strong Warrior than you already are right now...Before THAT! you need to choose one of the three paths that you'll be given...it isn't going to be easy, so if you have any questions, feel free to ask.");

		let selection = npc.askMenu("Alright, when you have made your decision, click on [I'll choose my occupation!] at the very bottom.\r\n"
				+ "#L0##bPlease explain the role of the Fighter.#k#l\r\n"
				+ "#L1##bPlease explain the role of the Page.#k#l\r\n"
				+ "#L2##bPlease explain the role of the Spearman.#k#l\r\n"
				+ "#L3##bI'll choose my occupation!#k#l\r\n");
		switch (selection) {
			case 0:
				npc.sayNext("Let me explain the role of the fighter. It's the most common kind of Warriors. The weapons they use are #bsword#k and #baxe#k , because there will be advanced skills available to acquired later on. I strongly recommend you avoid using both weapons, but rather stick to the one of your liking...");
				npc.sayNext("Other than that, there are also skills such as #bRage#k and #bPower Guard#k available for fighters. #bRage#k is the kind of an ability that allows you and your party to temporarily enhance your weapon power. With that you can take out the enemies with a sudden surge of power, so it'll come very handy for you. The downside to this is that your guarding ability (defense) goes down a bit.");
				npc.sayNext("#bPower Guard#k is an ability that allows you to return a portion of the damage that you take from a weapon hit by an enemy. The harder the hit, the harder the damage they'll get in return. It'll help those that prefer close combat. What do you think? Isn't being the Fighter pretty cool?");
				break;
			case 1:
				npc.sayNext("Let me explain the role of the Page. Page is a knight-in-training, taking its first steps to becoming an actual knight. They usually use #bswords#k and/or #bblunt weapons#k. It's not wise to use both weapons so it'll be best for you to stick to one or the other.");
				npc.sayNext("Other than that, there are also skills such as #bThreaten#k and #Power Guard#k to learn. #bThreaten#k makes every opponent around you lose some attacking and defending abilities for a time being. It's very useful against powerful monsters with good attacking abilities. It also works well in party play.");
				npc.sayNext("#bPower Guard#k is an ability that allows you to return a portion of the damage that you take from a weapon hit by an enemy. The harder the hit, the harder the damage they'll get in return. It'll help those that prefer close combat. What do you think? Isn't being the Page pretty cool?");
				break;
			case 2:
				npc.sayNext("Let me explain the role of the Spearman. It's a job that specializes in using long weapons such as #bspears#k and #bpolearms#k. There are lots of useful skills to acquire with both of the weapons, but I strongly recommend you stick to one and focus on it.");
				npc.sayNext("Other than that, there are also skills such as #bIron Will#k and #bHyper Body#k to learn. #bIron Will#k allows you and the members of your party to increase attack and magic defense for a period of time. It's the skill that's very useful for Spearmen with weapons that require both hands and can't guard themselves as well.");
				npc.sayNext("#bHyper Body#k is a skill that allows you and your party to temporarily improve the max HP and MP. You can improve almost 160% so it'll help you and your party especially when going up against really tough opponents. What do you think? Don't you think being the Spearman can be pretty cool?");
				break;
			case 3:
				selection = npc.askMenu("Hmmm, have you made up your mind? Then choose the 2nd job advancement of your liking.\r\n"
						+ "#L0##bFighter#k#l\r\n"
						+ "#L1##bPage#k#l\r\n"
						+ "#L2##bSpearman#k#l\r\n");
				switch (selection) {
					case 0:
						selection = npc.askYesNo("So you want to make the 2nd job advancement as the #bFighter#k? Once you make that decision you can't go back and choose another job...do you still wanna do it?");;
						if (selection == 0) {
							npc.sayNext("Really? So you need to think about it a little more? Take your time...this is not something that you should take lightly...let me know when you have made your decision, okay?");
						} else if (selection == 1) {
							if (player.getSp() > ((player.getLevel() - 30) * 3)) {
								npc.sayNext("Hmmm...you have too much SP...you can't make the 2nd job advancement with that many SP in store. Use more SP on the skills on the 1st level and then come back.");
							} else {
								player.setJob(110);
								player.gainSp(1);
								player.loseItem(4031012, 1); //Take The Proof of a Hero
								player.gainUseInventorySlots(4);
								player.gainEtcInventorySlots(4);
								let hpinc = 300 + Math.floor(Math.random() * 50); //Extra HP Given
								player.increaseMaxHp(hpinc);
								npc.sayNext("Alright! You have now become the #bFighter#k! A fighter strives to become the strongest of the strong, and never stops fighting. Don't ever lose that will to fight, and push forward 24/7. It'll help you become even stronger than you already are.");
								npc.sayNext("I have just given you a book that gives you the list of skills you can acquire as the Fighter. In that book, you'll find a bunch of skills the Fighter can learn. Your use and etc. inventories have also been expanded with an additional row of slots also available. Your max MP has also been increased...go check and see for it yourself.");
								npc.sayNext("I have also given you a little bit of #bSP#k. Open the #bSkill Menu#k located at the bottomleft corner. You'll be able to boost up the newly-acquired 2nd level skills. A word of warning though: You can't boost them up all at once. Some of the skills are only available after you have learned other skills. Make sure to remember that.");
								npc.sayNext("Fighters have to be strong. But remember that you can't abuse that power and use it on a weakling. Please use your enormous power the right way, because...for you to use that the right way, that is much harder than just getting stronger. Find me after you have advanced much further.");
							}
						}
						break;
					case 1:
						selection = npc.askYesNo("So you want to make the 2nd job advancement as the #bPage#k? Once you make that decision you can't go back and choose another job...do you still wanna do it?");
						if (selection == 0) {
							npc.sayNext("Really? So you need to think about it a little more? Take your time...this is not something that you should take lightly...let me know when you have made your decision, okay?");				
						} else if (selection == 1) {
							if (player.getSp() > ((player.getLevel() - 30) * 3)) {
								npc.sayNext("Hmmm...you have too much SP...you can't make the 2nd job advancement with that many SP in store. Use more SP on the skills on the 1st level and then come back.");
							} else {
								player.setJob(120);
								player.gainSp(1);
								player.loseItem(4031012, 1); //Take The Proof of a Hero
								player.gainUseInventorySlots(4);
								player.gainEtcInventorySlots(4);
								let mpinc = 100 + Math.floor(Math.random() * 50); //Extra MP Given
								player.increaseMaxMp(mpinc);
								npc.sayNext("Alright! You have now become the #bPage#k! The Pages have high intelligence and bravery for a Warrior...here's hoping that you'll take the right path with the right mindset...I'll help you become much stronger than you are right now.");
								npc.sayNext("I have just given you a book that gives you the list of skills you can acquire as the Page. In that book, you'll find a bunch of skills the Fighter can learn. Your use and etc. inventories have also been expanded with an additional row of slots also available. Your max MP has also been increased...go check and see for it yourself.");
								npc.sayNext("I have also given you a little bit of #bSP#k. Open the #bSkill Menu#k located at the bottomleft corner. You'll be able to boost up the newly-acquired 2nd level skills. A word of warning you have learned other skills. Make sure to remember that.");
								npc.sayNext("Pages have to be strong. But remember that you can't abuse that power and use it on a weakling. Please use your enormous power the right way, because...for you to use that the right way, that is much harder than just getting stronger. Find me after you have advanced much further.");
							}
						}
						break;
					case 2:
						selection = npc.askYesNo("So you want to make the 2nd job advancement as the #bSpearman#k? Once you make that decision you can't go back and choose another job...do you still wanna do it?");
						if (selection == 0) {
							npc.sayNext("Really? So you need to think about it a little more? Take your time...this is not something that you should take lightly...let me know when you have made your decision, okay?");				
						} else if (selection == 1) {
							if (player.getSp() > ((player.getLevel() - 30) * 3)) {
								npc.sayNext("Hmmm...you have too much SP...you can't make the 2nd job advancement with that many SP in store. Use more SP on the skills on the 1st level and then come back.");
							} else {
								player.setJob(130);
								player.gainSp(1);
								player.loseItem(4031012, 1); //Take The Proof of a Hero
								player.gainUseInventorySlots(4);
								player.gainEtcInventorySlots(4);
								let mpinc = 100 + Math.floor(Math.random() * 50); //Extra MP Given
								player.increaseMaxMp(mpinc);
								npc.sayNext("Alright! You have now become the #bSpearman#k! The spearman use the power of darkness to take out the enemies, always in shadows...please believe in yourself and your awesome power as you go on in your journey...I'll help you become much stronger than you are right now.");
								npc.sayNext("I have just given you a book that gives you the list of skills you can acquire as the Spearman. In that book, you'll find a bunch of skills the Fighter can learn. Your use and etc. inventories have also been expanded with an additional row of slots also available. Your max MP has also been increased...go check and see for it yourself.");
								npc.sayNext("I have also given you a little bit of #bSP#k. Open the #bSkill Menu#k located at the bottomleft corner. You'll be able to boost up the newly-acquired 2nd level skills. A word of warning though: You can't boost them up all at once. Some of the skills are only available after you have learned other skills. Make sure to remember that.");
								npc.sayNext("Spearmen have to be strong. But remember that you can't abuse that power and use it on a weakling. Please use your enormous power the right way, because...for you to use that the right way, that is much harder than just getting stronger. Find me after you have advanced much further.");
							}
						}
						break;
				}
				break;
		}
	}
} else if (Math.floor(player.getJob() / 100) == 1) {
	let selection = npc.askMenu("Oh, you have a question?\r\n"
			+ "#L0##bWhat are the general characteristics of being a Warrior?#k#l\r\n"
			+ "#L1##bWhat are the weapons that the Warriors use?#k#l\r\n"
			+ "#L2##bWhat are the armors that the Warriors can wear?#k#l\r\n"
			+ "#L3##bWhat are the skills available for the Warriors?#k#l");
	switch (selection) {
		case 0:
			npc.sayNext("Let me explain the role of a Warrior. Warriors possess awesome physical strength and power. They can also defende monsters' attacks, so they are the best when fighting up close with the monsters. With a high level of stamina, you won't be dying easily either.");
			npc.sayNext("To accurately attack the monster, however, you need a healthy dose of DEX, so don't just concentrate on boosting up the STR. If you want to improve rapidly, I recommend that you face stronger monsters.");
			break;
		case 1:
			npc.sayNext("Let me explain the weapons Warriors use. They can use weapons that allow them to slash, stab or strike. You won't be able to use weapons like bows and projectile weapons. Same with the small canes. ");
			npc.sayNext("The most common weapons are sword, blunt weapon, polearm, speak, axe, and etc...Every weapon has its advantages and disadvantages, so please take a close look at them before choosin gone. For now, try using the ones with high attack rating.");
			break;
		case 2:
			npc.sayNext("Let me explain the armors Warriors wear. Warriors are strong with high stamine, so they are able to wear tough, strong armor. It's not the greatest looking ones...but it serves its purpose well, the best of the armors.");
			npc.sayNext("Especially the shields, they are perfect for the Warriors. Remember, though, that you won't be able to use the shield if you are using the weapon that requires both hands. I know it's going to be a hard decision for you...");
			break;
		case 3:
			npc.sayNext("For the Warriors, the skills available are geared towards their awesome physical strength and power. The skill that helps you in close combats will help you the most. There's also a skill that allows you to recover your HP. Make sure to master that.");
			npc.sayNext("The two attacking skills available are #bPower Strike#k and #bSlash Blast#k. Power Strike is the one that applies a lot of damage to a single enemy. You can boost this skills up from the beginning.");
			npc.sayNext("On the other hand, Slash Blast does not apply much damage, but instead attacks multiple enemies around the area at once. You can only use this once you have 1 Power Strike boosted up. Its up to you.");
			break;
	}
} else {
	npc.sayNext("Awesome body! Awesome power! Warriors are they way to go!!!! What do you think? Want to make the job advancement as a Warrior??");
}
