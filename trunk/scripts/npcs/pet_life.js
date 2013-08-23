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
 * Mar the Fairy: Pet Master (NPC 1032102)
 * Hidden Street: Marr's Forest (Map 101000200)
 *
 * Revives pets with the Water of Life in "Mar" the Fairy and the Water of Life
 * (Quest 2049)
 *
 * @author GoldenKevin
 */

//TODO: GMS-like lines
let selection = npc.askMenu("I'm Mar the Fairy. I can revive your pet or transfer its existing EXP to another.\r\n#b"
		+ "#L0#I want to turn my doll back into a pet.#l\r\n"
		+ "#L1#I want to transfer the existing EXP of my pet to a new pet.#l");
switch (selection) {
	case 0:
		if (!player.isQuestStarted(2049) || player.isQuestCompleted(2049)) {
			npc.sayNext("Nice to meet you! I'm #p1032102# and I study various types of spells here in #m101000000#. I'm especially fascinated by the magic of life. The mystery that has no end, the mystery known as life... I'm trying to discover how to create life.");
			selection = npc.askYesNo("It looks like you already found #p1012005#. #p1012005# is a person who studied the magic of life with me. I heard that he used an incomplete magic of life on a doll to create a living animal... the doll you have is the same as the one #p1012005# created, a #bpet#k?");
			if (selection == 0) {
				npc.say("But it looks like it was done by #p1012005# for sure. Ah... well nevermind. I visited #p1012005# years ago and I'm certain that he was unsuccessful in the magic of life for dolls. Well, then...");
			} else {
				npc.sayNext("I understand. The doll changed into a live animal... but the same item that #p1012005# used to give life to the pet, #b#t5180000##k dried up and so it turned back into a doll... obviously immobile, since it's a doll now... hummm... this thing called real life, is it not something that you can create with magic...?");
				npc.sayNext("Do you want to bring the doll to what it once was, with life? Do you want to return to the time in which your pet obeyed, only to you, and its company, right? Sure, it's totally possible. I was once a fairy that studied the magic of life with #p1012005#... maybe I can make it move again...");
				selection = npc.askYesNo("If I obtain a #b#t5180000##k and a #b#t4031034##k, maybe I can bring your doll to life again. What do you think? Do you want to bring the items? Collect the items and I will try to revive your doll.");
				if (selection == 0) {
					npc.say("You want to leave the doll as it is? It's a doll and all, but... it will be hard to erase your memories with it too. If you change your mind, find me, okay?");
				} else {
					player.startQuest(2049, npc.getNpcId());
					npc.say("Very good. I'll say it again, but what I need exactly is a #b#t5180000##k and a #b#t4031034##k. Obtain them and I can bring the doll back to life. Oh, and #b#t4031034##k is the more difficult to obtain... can you find #b#p1012006##k of #bHenesys#k? Maybe this person can give one or two tips...");
				}
			}
		} else {
			if (player.hasItem(4031034, 1) && player.hasItem(5180000, 1)) {
				selection = npc.askYesNo("You brought #b#t5180000##k and #b#t4031034##k... with them I can bring your doll back to life with my magic power. What do you think? Do you want to use the items and awaken your pet...?");
				if (selection == 0) {
					npc.say("I understand... you are not 100% ready for this, right? You are not thinking of leaving this doll as it is, right? Please come back if you change your opinion.");
				} else {
					selection = npc.askDoll("So, which pet do you want to recover? Choose the pet that you want alive the most...");
					player.revivePet(selection);
					player.loseItem(4031034, 1);
					player.loseItem(5180000, 1);
					player.completeQuest(2049, npc.getNpcId());
					npc.say("Your doll now has turned back into a pet. However, my magic is not perfect. Therefore, I cannot promise eternal life... please take good care of this pet before the #t5180000# dries up. Well then... goodbye...");
				}
			} else {
				npc.say("Still don't have #b#t5180000##k and #b#t4031034##k, right? Go see #b#p1012006##k in #m100000000#, that person should know something about the scroll. Please, collect these items quickly.");
			}
		}
		break;
	case 1:
		//TODO: GMS-like lines
		break;
}