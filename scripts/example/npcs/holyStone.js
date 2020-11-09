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
 * Holy Stone (NPC 2030006)
 * Hidden Street: Holy Ground at the Snowfield (Map 211040401)
 *
 * <insert description here>
 *
 * @author GoldenKevin (content from DelphiMS r418)
 */

//TODO: make wrong answers
let questions = [
	["In MS, what EXP needed to level up from Lv1 to Lv2?", ["15"], [[0]]],
	["In 1st job adv. which of the following is WRONG requirement?", ["Thief - 20 LUK or more"], [0]],
	["When you hit by monster, which of following not fully explained?", ["Weaken - slow down moving speed."], [0]],
	["For the 1st job adv. Which job fully states the job adv. requirement?", ["Warrior - 30 STR", "Magician - 25 INT", "Bowman - 25 DEX", "Thief - 20 DEX", "Thief - 20 LUK"], [2]],
	["Which of following monsters got CORRECT item corresponding to the monster?", ["#o3210100# - Fire Boar's Nose", "#o4230100# - Cold Eye Eyeball", "#o1210100# - Pig's Ear", "#o2300100# - #t4000042#", "#o2230101# - Zombie Mushroom Cap#"], [3]],
	["Which of following monsters got WRONG item corresponding to the monster?", ["Nependeath - Nependeath's leaf"], [0]],
	["In GM Event, how many FRUIT CAKE you can get as reward?", ["5"], [0]],
	["Which of following potions got CORRECT info.?", ["Pizza - Recover 400 HP"], [0]],
	["Which of following potions got WRONG info.?", ["Sunrise Dew - Recover 3000 MP"], [0]],
	["Green Mushroom, Tree Stump, Bubbling, Axe Stump, Octopus, which is highest level of all?", ["Axe Stump"], [0]],
	["Which monster will be seen during the ship trip to Orbis/Ellinia?", ["Crimson Barlog"], [0]],
	["Maple Island doesn't have which following monsters?", ["#o100101#", "#o1210102#", "#o130101#", "#o1210101#", "#o120100#"], [3]],
	["Which monster is not at Victoria Island and Sleepywood?", ["#o6230100#", "#o7130101#", "#o5130100#", "#o6400005#", "#o3000000#"], [4]],
	["El Nath doesn't have which following monsters?", ["Dark Ligator"], [0]],
	["Which of following monsters can fly?", ["Malady"], [0]],
	["Which of these monsters will you NOT be facing in Ossyria?", ["Croco"], [0]],
	["Which monster has not appeared in Maple Island?", ["Evil Eye"], [0]],
	["Which material doesn't need for awaken Hero's Gladius?", ["Fairy Wing"], [0]],
	["Which of following quests can be repeated?", ["Arwen and the Glass Shoe."], [0]],
	["Which of following are not 2nd job adv. ?", ["Mage"], [0]],
	["Which of following highest level quest?", ["Alcaster and the Dark Crystal"], [0]],
	["Which town is not at Victoria Island?", ["Kerning City", "Amherst", "Perion", "Nautilus Harbor", "Ellinia", "Southperry"], [1, 5]],
	["Which is the first NPC you meet in Maple Island?", ["Heena"], [0]],
	["Which NPC cannot be seen in El Nath?", ["Sophia"], [0]],
	["Which NPC cannot be seen in El Nath snowfield?", ["Elma the Housekeeper"], [0]],
	["Which NPC cannot be seen in Perion?", ["#p1021100#", "#p1032002#", "#p1022002#", "#p1022003#", "#p1022100#"], [1]],
	["Which NPC cannot be seen in Henesys?", ["#p1012101#", "#p1002001#", "#p1010100#", "#p1012100#", "#p1012102#"], [1]],
	["Which NPC cannot be seen in Ellinia?", ["Roel"], [0]],
	["Which NPC cannot be seen in Kerning City?", ["Luke"], [0]],
	["Which NPC is not related to pets?", ["Vicious"], [0]],
	["In Kerning City, who is the father of Alex, the run way kid?", ["#p1012005#", "#p1012002#", "#p12000#", "#p20000#", "#p1012003#"], [4]],
	["Which NPC is not belong to Alpha Platoon's Network of Communication?", ["Peter"], [0]],
	["What do you recieve in return from giving 30 dark Marples to the 2nd job advancement NPC?", ["#t4031012#", "Necklace of a Hero", "Pendant of a Hero", "Medal of a Hero", "Sign of a Hero"], [0]],
	["Which item you give Maya at Henesys in order to cure her sickness?", ["Incredible Medicine", "Bad Medicine", "Cure-All", "Chinese Medicine", "#t4031006#"], [4]],
	["Which of following NPC is not related to item synthesis/refine?", ["Shane"], [0]],
	["Which NPC cannot be seen in Maple Island?", ["Teo"], [0]],
	["Who do you see in the monitor in the navigation room with Kyrin?", ["Dr. Kim"], [0]],
	["You know Athena Pierce in Henesys? What color are her eyes?", ["Green"], [0]],
	["How many feathers are there on Dances with Barlog's Hat?", ["13"], [0]],
	["What's the color of the marble Grendel the Really Old from Ellinia carries with him?", ["Blue"], [0]]
];

function shuffle(a) {
	for (let i = a.length - 1; i > 0; i--) {
		j = (Math.random() * (i + 1)) | 0; //Math.floor(Math.random() * (i + 1))
		t = a[j];
		a[j] = a[i];
		a[i] = t;
    }
}

function generateQuestion(msg, question, answers) {
	let str = msg + question + "#b";
	for (let i = 0; i < answers.length; i++)
		str += "\r\n#L" + i + "# " + answers[i] + "#l";
	return str;
}

function isCorrect(validAnswers, selection) {
	for (let i = 0; i < validAnswers.length; i++)
		if (validAnswers[i] == selection)
			return true;

	return false;
}

if (player.isQuestStarted(7503) && !player.hasItem(4031058, 1)) {
	let selection = npc.askYesNo("... ... ...\r\nIf you want to test out your wisdom, then you'll have to offer #bDark Crystal#k as the sacrifice ...\r\nAre you ready to offer Dark Crystal and answer a set of questions from me?");
	if (selection == 1) {
		if (!player.hasItem(4005004, 1)) {
			npc.sayNext("If you want me to test your wisdom, you will have to provide a #b#t4005004##k as a sacrifice.");
		} else {
			player.loseItem(4005004, 1);
			npc.sayNext("Alright ... I'll be testing your widsom here. Anwer all the questions correctly, and you will pass the test. But if you are wrong just once, then you'll have to start over again... Okay, here we go.");

			shuffle(questions);
			let prompts = ["Here's the first question. ", "Question number two. ", "Question number three. ", "Question number four. ", "Question number five. "];
			let pass = true;
			for (let i = 0; i < 5 && pass; i++) {
				selection = npc.askMenu(generateQuestion(prompts[i], questions[i][0], questions[i][1]));
				if (!isCorrect(questions[i][2], selection)) {
					npc.sayNext("Wrong... Start again...");
					pass = false;
				}
			}
			if (pass) {
				npc.sayNext("I detect nothing but the truth in your answers.\r\nYour wisdom has been proven.\r\nTake this necklace before you leave.");
				player.gainItem(4031058, 1);
			}
		}
	}
}