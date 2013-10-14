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
 * Arec: 3rd Job - Thief Instructor (NPC 2020011)
 * El Nath: Chief's Residence (Map 211000001)
 *
 * Thief third job advancement NPC.
 *
 * @author GoldenKevin (content from Vana r3171, DelphiMS r418)
 */

function advanceJob() {
	let selection = npc.askYesNo("Okay, you will now become an even stronger thief. But before that can happen, check to see if you've used all of your SP. You must use all of the SP you've earned up to Lv. 70 before you can make the 3rd job advancement. Do you wish to make the job advancement now?");
	if (selection == 1) {
		if (player.getSp() > (player.getLevel() - 70) * 3) {
			npc.sayNext("Hmmm... you seem to have too much SP. You can't make the 3rd job advancement with so much SP. Use more in the 1st and 2nd advancement before returning here.");
		} else {
			if (player.getJob() == 410)
				npc.sayNext("You have officially been anointed as a #bHermit#k from this point forward. The skill book introduces a slew of new offensive skills for Hermits, using shadows as a way of duplication and replacement. You'll learn skills like #bShadow Meso#k (replace MP with mesos and attack monsters with the damage based on the amount of mesos thrown) and #bShadow Partner#k (create a shadow that mimics your every move, enabling you to attack twice). Use those skills to take on monsters that may have been difficult to conquer before.");
			else if (player.getJob() == 420)
				npc.sayNext("You have officially been anointed as a #bChief Bandit#k from this point forward. One of the new additions to the skill book is a skill called #bBand of Thieves#k, which lets you summon fellow Bandits to attack multiple monsters at once. Chief Bandits can also utilize mesos in numerous ways, from attacking monsters (#bMeso Explosion#k, which explodes mesos on the ground) to defending yourself (#bMeso Guard#k, which decreases damage done to you).");
			player.setJob(player.getJob() + 1);
			player.gainSp(1);
			player.gainAp(5);
			player.gainUseInventorySlots(4);
			npc.sayNext("I've also given you some SP and AP, which will help you get started. You have now become a powerful thief, indeed. Remember, though, that the real world will be awaiting with even tougher obstacles. Once you feel like you cannot reach a higher place, then come see me. I'll be here waiting.");
		}
	}
}

if (player.getLevel() >= 50) {
	let str = "Can I help you?#b";
	if ((player.getJob() == 410 || player.getJob() == 420) && player.getLevel() >= 70)
		str += "\r\n#L0# I want to make the 3rd job advancement#l";
	str += "\r\n#L1# Please allow me to do the Zakum Dungeon Quest.#l";
	let selection = npc.askMenu(str);
	switch (selection) {
		case 0:
			if (player.getJob() != 410 && player.getJob() != 420 || player.getLevel() < 70) {
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
				selection = npc.askYesNo("Ah, you wish to make your 3rd job advancement? I have the power to make you stronger, but first I must test your persistence. Do you wish to take my test?")
				if (selection == 0) {
					npc.sayNext("I don't think you are ready to face the tough challenges that lie ahead. Come see me only after you have convinced yourself of your toughness.");
				} else if (selection == 1) {
					player.startQuest(7500, npc.getNpcId());
					npc.sayNext("There are two things you must prove you possess: strength and intelligence. First I shall test your strength. Do you remember #b#p1052001##k from Kerning City, who gave you your 1st and 2nd job advancements? Go see him, and he will give you a mission. Complete that mission and bring back #b#t4031057##k from #p1052001#.");
					npc.sayNext("The second test will determine your intelligence, but first pass this one and bring back #b#t4031057##k. I will let #b#p1052001##k know that you are coming. Good luck...");
				}
			} else if (!player.hasItem(4031057, 1) || !player.isQuestStarted(7502)) {
				npc.sayNext("You don't have #b#t4031057##k with you. Please go see #b#p1052001##k of Kerning City, pass the test and bring #b#t4031057##k back with you. Then, and only then, you will be given the second test. Best of luck to you.");
			} else {
				player.loseItem(4031057, 1);
				player.completeQuest(7502, npc.getNpcId());
				player.startQuest(7503, npc.getNpcId());
				npc.sayNext("Ah... I see that you completed the mission #b#p1052001##k gave you. I always believed that you had it in you. But I hope you have not forgotten that a second test awaits you. Before you start the second test, I'll take the necklace you've brought.");
				npc.clearBackButton();
				npc.sayNext("Now, the second test remains. If you pass this test, you'll become a more powerful Thief. When you take the secret portal found in Sharp Cliff I in the El Nath Dungeon, you will find a small Holy Stone, which the monsters don't dare approach. It may appear to be an average Holy Stone, but if you offer a special item, it will test your wisdom.");
				npc.sayNext("Find the Holy Ground and offer a special item. If you are able to answer the questions that are asked of you truthfully and sincerely, you will receive #b#t4031058##k. If you bring the necklace to me, I will advance you into an even stronger Thief. I wish you luck.");
			}
			break;
		case 1:
			if (player.isQuestStarted(7504)) {
				npc.sayNext("How are you along with the Zakum Dungeon Quest? From what I've heard, there's this incredible monster at the innermost part of that place ... anyway, good luck. I'm sure you can do it.");
			} else {
				if (player.getJob() == 0 || player.getJob() >= 400 && player.getJob() < 500) {
					player.startQuest(7504, npc.getNpcId());
					npc.sayNext("You want to be permitted to do the Zakum Dungeon Quest, right? Must be #b#p2030008##k ... ok, alright! I'm sure you'll be fine roaming around the dungeon. Here's hoping you'll be careful there ...");
				} else {
					npc.sayNext("So you want me to give you my permission to go on the Zakum Dungeon Quest? But you don't seem to be a thief under my jurisdiction, so please look for the Trainer that oversees your job.");
				}
			}
			break;
	}
} else {
	npc.say("Hmm... It seems like there is nothing I can help you with. Come to me again when you get stronger. Zakum Dungeon quest is available from #bLevel 50#k and 3rd Job Advancement at #bLevel 70#k");
}