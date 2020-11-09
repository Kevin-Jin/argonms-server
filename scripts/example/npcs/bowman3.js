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
 * Rene: 3rd Job - Bowman Instructor (NPC 2020010)
 * El Nath: Chief's Residence (Map 211000001)
 *
 * Bowman third job advancement NPC.
 *
 * @author GoldenKevin (content from Vana r3171, DelphiMS r418)
 */

function advanceJob() {
	let selection = npc.askYesNo("Okay! Now, I'll transform you into a much more powerful bowman. Before that, however, please make sure your SP has been thoroughly used. You'll need to use at least all the SP gained until level 70 to make the 3rd job advancement. Oh, and since you have already chosen your path of the occupation by the 2nd job advancement, you won't have to choose again for the 3rd job advancement. Do you want to do it right now?");
	if (selection == 1) {
		if (player.getSp() > (player.getLevel() - 70) * 3) {
			npc.sayNext("Hmmm... you seem to have too much SP. You can't make the 3rd job advancement with so much SP. Use more in the 1st and 2nd advancement before returning here.");
		} else {
			if (player.getJob() == 310)
				npc.sayNext("You have officially become a #bRanger#k. One of the skills that you'll truly embrace is a skill called #bMortal Blow#k that allows Rangers to fire arrows from close-range. #bInferno#k allows Rangers to temporarily perform fire-based attacks on monsters, while skills like #bPuppet#k (summons a scarecrow which attracts the monsters' attention) and #bSilver Hawk#k (summons a Silver Hawk that attacks monsters) solidify the Bowman's status as a long-range attack extraordinaire.");
			else if (player.getJob() == 320)
				npc.sayNext("You have officially become a #bSniper#k. One of the skills that you'll truly embrace is a skill called #bMortal Blow#k that allows Snipers to fire arrows from close-range. #bBlizzard#k allows the Snipers to temporarily perform ice-based attacks on monsters, while skills like #bPuppet#k (summons a scarecrow which attracts the monsters' attention) and #bGolden Eagle#k (summons a Golden Eagle that attacks monsters) solidify the Bowman's status as a long-range attack extraordinaire.");
			player.setJob(player.getJob() + 1);
			player.gainSp(1);
			player.gainAp(5);
			player.gainEtcInventorySlots(4);
			npc.sayNext("I've also given you some SP and AP, which will help you get started. You have now become a powerful bowman, indeed. Remember, though, that the real world will be awaiting with even tougher obstacles. Once you feel like you cannot reach a higher place, then come see me. I'll be here waiting.");
		}
	}
}

if (player.getLevel() >= 50) {
	let str = "Can I help you?#b";
	if ((player.getJob() == 310 || player.getJob() == 320) && player.getLevel() >= 70)
		str += "\r\n#L0# I want to make the 3rd job advancement#l";
	str += "\r\n#L1# Please allow me to do the Zakum Dungeon Quest.#l";
	let selection = npc.askMenu(str);
	switch (selection) {
		case 0:
			if (player.getJob() != 310 && player.getJob() != 320 || player.getLevel() < 70) {
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
					npc.sayNext("Great job completing the mental part of the test. You have wisely answered all the questions correctly. I must say, I am quite impressed with the level of wisdom you have displayed there. Please hand me the necklace first, before we take on the next step.");

					advanceJob();
				}
			} else if (!player.isQuestStarted(7500) && !player.isQuestCompleted(7500)) {
				selection = npc.askYesNo("Welcome. I'm #bRene#k, the chief of all bowmen, in charge of bringing out the best in each and every bowman that needs my guidance. You seem like the kind of bowman that wants to make the leap forward, the one ready to take on the challenges of the 3rd job advancement. But I've seen countless bowmen eager to make the jump just like you, only to see them fail. What about you? Are you ready to be tested and make the 3rd job advancement?");
				if (selection == 0) {
					npc.sayNext("I don't think you are ready to face the tough challenges that lie ahead. Come see me only after you have convinced yourself of your toughness.");
				} else if (selection == 1) {
					player.startQuest(7500, npc.getNpcId());
					npc.sayNext("Good. You will be tested on two important aspects of the bowman: strength and wisdom. I'll now explain to you the physical half of the test. Remember #b#p1012100##k from Henesys? Go see her, and she'll give you the details on the first half of the test. Please complete the mission, and get #b#t4031057##k from #p1012100#.");
					npc.sayNext("The mental half of the test can only start after you pass the physical part of the test. #bThe Necklace of Strength#k will be the proof that you have indeed passed the test. I'll let #b#p1012100##k know in advance that you're making your way there, so get ready. It won't be easy, but I have the utmost faith in you. Good luck.")
				}
			} else if (!player.hasItem(4031057, 1) || !player.isQuestStarted(7502)) {
				npc.sayNext("You don't have #b#t4031057##k with you. Please go see #b#p1012100##k of Henesys, pass the test and bring #b#t4031057##k back with you. Then, and only then, you will be given the second test. Best of luck to you.");
			} else {
				player.loseItem(4031057, 1);
				player.completeQuest(7502, npc.getNpcId());
				player.startQuest(7503, npc.getNpcId());
				npc.sayNext("Great job completing the physical part of the test. I knew you could do it. Now that you have passed the first half of the test, here's the second half. Please give me the necklace first.");
				npc.clearBackButton();
				npc.sayNext("Here's the 2nd half of the test. This test will determine whether you are smart enough to take the next step towards greatness. There is a dark, snow-covered area called the Holy Ground at the snowfield in Ossyria, where even the monsters can't reach. On the center of the area lies a huge stone called the Holy Stone. You'll need to offer a special item as the sacrifice, then the Holy Stone will test your wisdom right there on the spot.");
				npc.sayNext("You'll need to answer each and every question given to you with honesty and conviction. If you correctly answer all the questions, then the Holy Stone will formally accept you and hand you #b#t4031058##k. Bring back the necklace, and I will help you make the next leap forward. Good luck.");
			}
			break;
		case 1:
			if (player.isQuestStarted(7504)) {
				npc.sayNext("How are you along with the Zakum Dungeon Quest? From what I've heard, there's this incredible monster at the innermost part of that place ... anyway, good luck. I'm sure you can do it.");
			} else {
				if (player.getJob() == 0 || player.getJob() >= 300 && player.getJob() < 400) {
					player.startQuest(7504, npc.getNpcId());
					npc.sayNext("You want to be permitted to do the Zakum Dungeon Quest, right? Must be #b#p2030008##k ... ok, alright! I'm sure you'll be fine roaming around the dungeon. Here's hoping you'll be careful there ...");
				} else {
					npc.sayNext("So you want me to give you my permission to go on the Zakum Dungeon Quest? But you don't seem to be a bowman under my jurisdiction, so please look for the Trainer that oversees your job.");
				}
			}
			break;
	}
} else {
	npc.say("Hmm... It seems like there is nothing I can help you with. Come to me again when you get stronger. Zakum Dungeon quest is available from #bLevel 50#k and 3rd Job Advancement at #bLevel 70#k");
}