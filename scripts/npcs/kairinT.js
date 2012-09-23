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
 * Kyrin: Pirate Instructor (NPC 1090000)
 * The Nautilus: Navigation Room (Map 120000101),
 *   Hidden Chamber: Kyrin's Training Room (Map 912010200)
 *
 * Pirate job advancement NPC.
 * Admits pirates into the second job advancement challenge.
 *
 * @author GoldenKevin (content from DelphiMS r418)
 */

//TODO: third job chat from http://trac.assembla.com/DelphiMS/browser/bin/Scripts/NPC/kairinT.ds
//need to implement player variables.
function checkSecondJobAdvancementQuest() {
	let brawler, gunslinger;
	if ((brawler = player.isQuestStarted(2191)) || (gunslinger = player.isQuestStarted(2192))) {
		let item, adj, job, skill, destination;
		if (brawler) {
			item = 4031856;
			adj = "strong";
			job = "Brawler";
			skill = "Flash Fist";
			destination = 108000502;
		}
		if (gunslinger) {
			item = 4031857;
			adj = "quick";
			job = "Gunslinger";
			skill = "Double Shot";
			destination = 108000500;
		}

		npc.sayNext("Okay, now I'll take you to the test room. Here are the instructions: defeat the Octopirates and gather #b15 #t" + item + "#s#k. The Octopirates you'll see here are highly trained and are very " + adj + ", so I suggest you really buckle down and get ready for this.");
		npc.sayNext("Oh, and for the sake of training " + job + "s, those Octos will not be affected unless hit with #b" + skill + "#k. And one more thing, when you enter the test room, I'll remove all the #t" + item + "#s you have. Yes, you'll be starting off from scratch.");
		player.loseItem(item);
		//create an event. the playerDisconnected and playerChangedMap handlers
		//make it easier to destroy the instance map when it is no longer needed
		//and we also need a timer, unlike other 2nd job advancement challenges
		npc.makeEvent("kairinT", [player, destination]);
		return true;
	} else if (player.isQuestCompleted(2191)) {
		npc.sayNext("Okay, as promised, you will now become a #bBrawler#k.");
		player.setJob(510);
		player.gainSp(1);
		player.gainUseInventorySlots(4);
		let hpinc = 200 + Math.floor(Math.random() * 50); //Extra HP Given
		let mpinc = 150 + Math.floor(Math.random() * 25); //Extra MP Given
		player.increaseMaxHp(hpinc);
		player.increaseMaxMp(mpinc);
		npc.sayNext("Okay, from here on out, you are a #bBrawler#k. Brawlers rule the world with the power of their bare fists... which means they need to train their body more than others. If you have any trouble training, I'll be more than happy to help.");
		npc.sayNext("I have just given you a skill book that entails Brawler skills, you'll find it very helpful. You have also gained additional slots for Use items, a full row in fact. I also boosted your MaxHP and MaxMP. Check it out for yourself.");
		npc.sayNext("Brawlers need to be a powerful force, but that doesn't mean they have the right to bully the weak. True Brawlers use their immense power in positive ways, which is much harder than just training to gain strength. I hope you follow this creed as you leave your mark in this world as a Brawler. I will see you when you have accomplished everything you can as a Brawler. I'll be waiting for you here.");
		npc.sayNext("I have given you a little bit of #bSP#k, so I suggest you open the #bskill menu#k right now. You'll be able to enhance your newly-acquired 2nd Job skills. Beware that not all skills can be enhanced from the get go. There are some skills that you can only acquire after mastering basic skills.");
		return true;
	} else if (player.isQuestCompleted(2192)) {
		npc.sayNext("I knew you'd pass the test! You had some impressive moves in there. Not bad at all! Now, as promised, you will now become a #bGunslinger#k.");
		player.setJob(520);
		player.gainSp(1);
		player.gainUseInventorySlots(4);
		let hpinc = 200 + Math.floor(Math.random() * 50); //Extra HP Given
		let mpinc = 150 + Math.floor(Math.random() * 25); //Extra MP Given
		player.increaseMaxHp(hpinc);
		player.increaseMaxMp(mpinc);
		npc.sayNext("From here on out, you are a #bGunslinger#k. Gunslingers are notable for their long-range attacks with sniper-like accuracy and, of course, using Guns as their primary weapon. You should continue training to truly master your skills. If you are having trouble, I'll be there to help.");
		npc.sayNext("I have just given you a skill book that entails Gunslinger skills, you'll find it very helpful. You have also gained additional slots for Use items, a full row in fact. I also boosted your MaxHP and MaxMP. Check it out for yourself.");
		npc.sayNext("Gunslingers are deadly at ranged combat, but that doesn't mean they have the right to bully the weak. Gunslingers will need to use their immense power in positive ways, which is actually hard than just training to gain strength. I hope you follow this creed as you leave your mark in this world as a Gunslinger. I will see you when you have accomplished everything you can as a Gunslinger. I'll be waiting for you here.");
		npc.sayNext("I have given you a little bit of #bSP#k, so I suggest you open the #bskill menu#k right now. You'll be able to enhance your newly-acquired 2nd Job skills. Beware that not all skills can be enhanced from the get go. There are some skills that you can only acquire after mastering basic skills.");
		return true;
	}
	return false;
}

if (player.getJob() != 500 || player.getLevel() < 30 || !checkSecondJobAdvancementQuest()) {
	let selection = npc.askMenu("Have you got something to say?\r\n#b"
			+ "#L0# I would like to learn more about pirates..#l");
	switch (selection) {
		case 0:
			if (player.getJob() == 0) {
				npc.sayNext("Do you wish to become a Pirate? You'll need to meet our set of standard if you are to become one of us. I need you to be #bat least at Level 10#k. Let's see...");
				if (player.getLevel() >= 10) {
					selection = npc.askYesNo("You seem more than qualified! Great, are you ready to become one of us?");
					if (selection == 1) {
						npc.sayNext("Welcome to the band of Pirates! You may have to spend some time as a wanderer at first, but better days will certainly dawn upon you, sooner than you think! In the mean time, let me share some of my abilities with you.");

						player.setJob(500);
						player.gainItem(1482014, 1); //Scallywag Knuckler
						player.gainItem(1492014, 1); //Pirate's Pistol
						player.gainItem(2330006, 600); //Bullet for Novice Pirates
						player.gainItem(2330006, 600); //Bullet for Novice Pirates
						player.gainItem(2330006, 600); //Bullet for Novice Pirates
						let hpinc = 150; //Extra HP Given
						let mpinc = 50; //Extra MP Given
						player.increaseMaxHp(hpinc);
						player.increaseMaxMp(mpinc);
						player.gainEquipInventorySlots(4);
						player.gainEtcInventorySlots(4);
						npc.sayNext("I have just increased the number of slots for your equipment and etc. inventory. You have also gotten a bit stronger. Can you feel it? Now that you can officially call yourself a Pirate, join us in our quest for adventure and freedom!");
						npc.sayNext("I have just given you a little bit of #bSP#k. Look at the #bSkill menu#k to find some skills, and use your SP to learn the skills. Beware that not all skills can be enhanced from the get go. There are some skills that you can only acquire after mastering basic skills.");
						npc.sayNext("One more thing. Now that you have graduated from the ranks of a Beginner into a Pirate, you'll have to make sure not to die prematurely. If you do lose all your health, you'll lose valuable EXP that you have earned. Wouldn't it stink to lose hard-earned EXP by dying?");
						npc.sayNext("This is all I can teach you. I have also given you some useful weapons to work with, so it's up to you now to train with them. The world is yours for the taking, so use your resources wisely, and when you feel like you've reached the top, let me know. I'll have something better for you in store...");
						npc.sayNext("Oh, and... your stats should accurately reflect your new occupation as a Pirate. Click on #bAuto Assign#k on your stat window to make yourself into an even more formidable pirate.");
					} else if (selection == 0) {
						npc.sayNext("I see... Well, selecting a new job is a very important decision to make. If you are ready, then let me know!");
					}
				} else {
					npc.sayNext("Hmm...I dont think you have trained enough, yet. See me when you get stronger.");
				}
			} else {
				npc.sayNext("Don't you want to feel the freedom eminating from the sea? Dont you want the power, the fame, and everything else that comes with it? Then you should join us and enjoy it yourself.");
			}
			break;
	}
}