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
 * Pedro: 3rd Job - Pirate Instructor (NPC 2020013)
 * El Nath: Chief's Residence (Map 211000001)
 *
 * Pirate third job advancement NPC.
 *
 * @author GoldenKevin (content from Vana r3171, DelphiMS r418)
 */

function advanceJob() {
	let selection = npc.askYesNo("Great! You may now become the kind of Pirate you've always dreamed of! With newfound power and stellar new skills, your power has endless possibilities! Before we proceed, however, please check and see if you have used up your Skill Points. You must use up all the SPs you've earned up to Level 70 in order for you to make the 3rd job advancement. Since you've already chosen which of the two Pirate paths you wanted to take in the 2nd job advancement, this will not require much thought. Do you wish to make the job advancement right now?");
	if (selection == 1) {
		if (player.getSp() > (player.getLevel() - 70) * 3) {
			npc.sayNext("Hmmm... you seem to have too much SP. You can't make the 3rd job advancement with so much SP. Use more in the 1st and 2nd advancement before returning here.");
		} else {
			if (player.getJob() == 510)
				npc.sayNext("Great! You are now a #bMarauder#k. As a Marauder, you will learn some of the most sophisticated skills related to melee-based attacks. #bEnergy Charge#k is a skill that allows you to store your power and the damage you receive into a special form of energy. Once this ball of energy is charged, you may use #bEnergy Blast#k to apply maximum damage against your enermies, and also use #bEnergy Drain#k to steal your enemy's HP to recover your own. #bTransformation#k will allow you to transform into a superhuman being with devastating melee attacks, and while transformed, you can use #bShockwave#k to cause a mini-earthquake and inflict massive damage to your enemies.");
			else if (player.getJob() == 520)
				npc.sayNext("Great! You are now an #bOutlaw#k. As an Outlaw, you will become a true pistolero, a master of every known Gun attack, as well as a few other skills to help you vanquish evil. #bBurst Fire#k is a more powerful version of Double Shot, shooting more bullets and causing more damage at the same time. You also no have the ability to summon a loyal #bOctopus#k and the swooping #bGaviota#k as your trusty allies, while attacking your enemies using #bBullseye#k. You can also use element-based attacks by using #bFlamethrower#k and #bIce Splitter#k.");
			player.setJob(player.getJob() + 1);
			player.gainSp(1);
			player.gainAp(5);
			player.gainUseInventorySlots(4);
			npc.sayNext("I've also given you some SP and AP, which will help you get started. You have now become a powerful pirate, indeed. Remember, though, that the real world will be awaiting with even tougher obstacles. Once you feel like you cannot reach a higher place, then come see me. I'll be here waiting.");
		}
	}
}

if (player.getLevel() >= 50) {
	let str = "Is there something that you want from me?#b";
	if ((player.getJob() == 510 || player.getJob() == 520) && player.getLevel() >= 70)
		str += "\r\n#L0# I want to make the 3rd job advancement#l";
	str += "\r\n#L1# I want your permission to attempt the Zakum Dungeon Quest!#l";
	let selection = npc.askMenu(str);
	switch (selection) {
		case 0:
			if (player.getJob() != 510 && player.getJob() != 520 || player.getLevel() < 70) {
				npc.logSuspicious("Tried to select NPC option that was not given");
				break;
			}

			if (player.isQuestCompleted(7503)) {
				advanceJob();
			} else if (player.isQuestStarted(7503)) {
				if (!player.hasItem(4031058, 1)) {
					npc.sayNext("You don't have #b#t4031058##k with you. Please go see #bHoly Stone#k in the Holy Ground, pass the test and bring #b#t4031058##k back with you. Then, and only then, I will help you make the next leap forward. Best of luck to you.");
				} else {
					player.loseItem(4031058, 1);
					player.completeQuest(7503, npc.getNpcId());
					npc.sayNext("Ah... You found the Holy Ground, I see. I... I didn't think you would pass the second test. I will take the necklace now before your advance to your 3rd job.");

					advanceJob();
				}
			} else if (!player.isQuestStarted(7500) && !player.isQuestCompleted(7500)) {
				selection = npc.askYesNo("Hmm... so you want to be a stronger pirate by making the 3rd job advancement? First I can say congrats--few have this level of dedication. I can certainly make you stronger with my powers, but I'll need to test your strength to see if your training has been adequate. Many come professing their strength, few are actually able to prove it. Are you ready for me to test your strength?");
				if (selection == 0) {
					npc.sayNext("I don't think you are ready to face the tough challenges that lie ahead. Come see me only after you have convinced yourself of your toughness.");
				} else if (selection == 1) {
					player.startQuest(7500, npc.getNpcId());
					npc.sayNext("Great! Now, you will have to prove your strength and intelligence. Let me first explain the strength test. Do you remember #b#p1090000##k from The Nautilus who helped you make the 1st and 2nd advancements? Go see her and she will give you a task to fulfill. Complete that task and you will receive #b#t4031057##k from #p1090000#.");
					npc.sayNext("The second part will be to test your intelligence, however, you must pass the strength test first. After that, you will be qualified for the second test. Now... I will let #b#p1090000##k know that you are on your way, so follow this road and go see her right away. It won't be easy, but I trust that you will be able to handle it.")
				}
			} else if (!player.hasItem(4031057, 1) || !player.isQuestStarted(7502)) {
				npc.sayNext("You don't have #b#t4031057##k with you. Please go see #b#p1090000##k of The Nautilus, pass the test and bring #b#t4031057##k back with you. Then, and only then, you will be given the second test. Best of luck to you.");
			} else {
				player.loseItem(4031057, 1);
				player.completeQuest(7502, npc.getNpcId());
				player.startQuest(7503, npc.getNpcId());
				npc.sayNext("So you were able to complete the task given to you by #b#p1090000##k. Nice! I knew you would be able to handle it. But it's not over yet. You do remember that there's a second part of the test, right? Before we move on, why don't I take that necklace from you now.");
				npc.clearBackButton();
				npc.sayNext("Now, the second test remains. If you pass this test, you'll become a more powerful Pirate. When you take the secret portal found in Sharp Cliff I in the El Nath Dungeon, you will find a small Holy Stone, which the monsters don't dare approach. It may appear to be an average Holy Stone, but if you offer a special item, it will test your wisdom.");
				npc.sayNext("Find the Holy Ground and offer a special item. If you are able to answer the questions that are asked of you truthfully and sincerely, you will receive #b#t4031058##k. If you bring the necklace to me, I will advance you into an even stronger pirate. Best of luck to you and dress warm! Pirate gear isn't exactly built for the cold.");
			}
			break;
		case 1:
			if (player.isQuestStarted(7504)) {
				npc.sayNext("So how is the Zakum Dungeon Quest going? I hear there's an extremely scary monster in the deepest part of that dungeon... Well anyways, press on. I know you can handle it!");
			} else {
				if (player.getJob() == 0 || player.getJob() >= 500 && player.getJob() < 600) {
					player.startQuest(7504, npc.getNpcId());
					npc.sayNext("You want to be permitted to do the Zakum Dungeon Quest, right? Must be #b#p2030008##k ... ok, alright! I'm sure you'll be fine roaming around the dungeon. Don't get yourself killed ...");
				} else {
					npc.sayNext("So you want me to give you my permission to go on the Zakum Dungeon Quest? But you don't seem to be a pirate under my jurisdiction, so please look for the Trainer that oversees your job.");
				}
			}
			break;
	}
} else {
	npc.sayNext("I don't think there's much help I could give you right now. Why don't you come back to see me after you've grown stronger? You can go attempt the Zakum Dungeon Quest once you reach #bLevel 50#k and make your 3rd job advancement when you reach #bLevel 70#k.");
}