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
 * Cloto (NPC 9020001)
 * Hidden Street: 1st Accompaniment <1st Stage> (Map 103000800),
 *   Hidden Street: 1st Accompaniment <2nd Stage> (Map 103000801),
 *   Hidden Street: 1st Accompaniment <3rd Stage> (Map 103000802),
 *   Hidden Street: 1st Accompaniment <4th stage> (Map 103000803),
 *   Hidden Street: 1st Accompaniment <Last Stage> (Map 103000804)
 *
 * Lets the party continue on through each stage in the Kerning City party
 * quest.
 *
 * @author GoldenKevin (content from KiniroMS r227)
 */

function clear(event, stage, exp) {
	event.setVariable(stage + "stageclear", true);
	map.screenEffect("quest/party/clear");
	map.soundEffect("Party1/Clear");
	map.portalEffect("gate");
	let members = event.getVariable("members");
	for (let i = 0; i < members.length; i++)
		if (members[i].getHp() > 0)
			members[i].gainExp(exp);
}

function failStage() {
	map.screenEffect("quest/party/wrong_kor");
	map.soundEffect("Party1/Failed");
}

function rectangleStages(event, stage) {
	let debug = false; //see which positions are occupied
	let stages = ["2nd", "3rd", "4th"];
	let objs = ["ropes", "platforms", "barrels"];
	let verbs = ["hang", "stand", "stand"];
	let donts = ["hang on the ropes too low", "stand too close to the edges", "stand too close to the edges"];
	let combos = [
		[ //stage 2
			[0, 1, 1, 1],
			[1, 0, 1, 1],
			[1, 1, 0, 1],
			[1, 1, 1, 0]
		], 
		[ //stage 3
			[0, 0, 1, 1, 1],
			[0, 1, 0, 1, 1],
			[0, 1, 1, 0, 1],
			[0, 1, 1, 1, 0],
			[1, 0, 0, 1, 1],
			[1, 0, 1, 0, 1],
			[1, 0, 1, 1, 0],
			[1, 1, 0, 0, 1],
			[1, 1, 0, 1, 0],
			[1, 1, 1, 0, 0]
		],
		[ //stage 4
			[0, 0, 0, 1, 1, 1],
			[0, 0, 1, 0, 1, 1],
			[0, 0, 1, 1, 0, 1],
			[0, 0, 1, 1, 1, 0],
			[0, 1, 0, 0, 1, 1],
			[0, 1, 0, 1, 0, 1],
			[0, 1, 0, 1, 1, 0],
			[0, 1, 1, 0, 0, 1],
			[0, 1, 1, 0, 1, 0],
			[0, 1, 1, 1, 0, 0],
			[1, 0, 0, 0, 1, 1],
			[1, 0, 0, 1, 0, 1],
			[1, 0, 0, 1, 1, 0],
			[1, 0, 1, 0, 0, 1],
			[1, 0, 1, 0, 1, 0],
			[1, 0, 1, 1, 0, 0],
			[1, 1, 0, 0, 0, 1],
			[1, 1, 0, 0, 1, 0],
			[1, 1, 0, 1, 0, 0],
			[1, 1, 1, 0, 0, 0]
		]
	];
	//TODO: Don't use AWT rectangles.
	let rects = [
		[ //stage 2
			java.awt.Rectangle(-770, -132, 28, 178),
			java.awt.Rectangle(-733, -337, 26, 105),
			java.awt.Rectangle(-601, -328, 29, 105),
			java.awt.Rectangle(-495, -125, 24, 165)
		],
		[ //stage 3
			java.awt.Rectangle(608, -180, 140, 50),
			java.awt.Rectangle(791, -117, 140, 45),
			java.awt.Rectangle(958, -180, 140, 50),
			java.awt.Rectangle(876, -238, 140, 45),
			java.awt.Rectangle(702, -238, 140, 45)
		],
		[ //stage 4
			java.awt.Rectangle(910, -236, 35, 5),
			java.awt.Rectangle(877, -184, 35, 5),
			java.awt.Rectangle(946, -184, 35, 5),
			java.awt.Rectangle(845, -132, 35, 5),
			java.awt.Rectangle(910, -132, 35, 5),
			java.awt.Rectangle(981, -132, 35, 5)
		]
	];
	let objsets = [
		[0, 0, 0, 0],
		[0, 0, 0, 0, 0],
		[0, 0, 0, 0, 0, 0]
	];

	let index = stage - 2;

	if (player.getId() == party.getLeader()) {
		let preamble = event.getVariable("leader" + stages[index] + "preamble");
		if (preamble == null || !preamble) {
			npc.sayNext("Hi. Welcome to the " + stages[index] + " stage. Next to me, you'll see a number of " + objs[index] + ". Out of these " + objs[index] + ", #b3 are connected to the portal that sends you to the next stage#k. All you need to do is have #b3 party members find the correct " + objs[index] + " and " + verbs[index] + " on them.#k\r\nBUT, it doesn't count as an answer if you " + donts[index] + "; please be near the middle of the " + objs[index] + " to be counted as a correct answer. Also, only 3 members of your party are allowed on the " + objs[index] + ". Once they are " + verbs[index] + "ing on them, the leader of the party must #bdouble-click me to check and see if the answer's correct or not#k. Now, find the right " + objs[index] + " to " + verbs[index] + " on!");
			event.setVariable("leader" + stages[index] + "preamble", true);
			let sequenceNum = Math.floor(Math.random() * combos[index].length);
			event.setVariable("stage" + stages[index] + "combo", sequenceNum);
		} else {
			// Check for stage completed
			let complete = event.getVariable(stage + "stageclear");
			if (complete != null && complete) {
				npc.sayNext("You all have cleared the quest for this stage. Use the portal to move to the next stage...");
			} else { // Check for people on ropes and their positions
				let totplayers = 0;
				let members = event.getVariable("members");
				for (let i = 0; i < members.length; i++) {
					for (let j = 0; j < objsets[index].length; j++) {
						if (members[i].getMapId() == map.getId() && rects[index][j].contains(members[i].getPosition())) {
							objsets[index][j]++;
							totplayers++;
							break;
						}
					}
				}
				// Compare to correct positions
				// Don't even bother if there aren't three players.
				if (totplayers == 3 || debug) {
					let combo = combos[index][event.getVariable("stage" + stages[index] + "combo")];
					let testcombo = true;
					for (let i = 0; i < objsets[index].length && testcombo; i++)
						if (combo[i] != objsets[index][i])
							testcombo = false;
					if (debug) {
						let str = "Objects contain:"
						for (let i = 0; i < objsets[index].length; i++)
							str += "\r\n" + (i + 1) + ". " + objsets[index][i];
						str += "\r\nCorrect combination: ";
						for (let i = 0; i < combo.length; i++)
							str += "\r\n" + (i + 1) + ". " + combo[i];
						if (testcombo) {
							str += "\r\nResult: #gClear#k";
							npc.say(str);
						} else {
							str += "\r\nResult: #rWrong#k";
							str += "\r\n#bForce clear stage?#k";
							debug = npc.askYesNo(str);
						}
					}
					if (testcombo || debug) {
						clear(event, stage, Math.pow(2, stage) * 50);
					} else {
						failStage();
					}
				} else {
					npc.sayNext("It looks like you haven't found the 3 " + objs[index] + " just yet. Please think of a different combination of " + objs[index] + ". Only 3 are allowed to " + verbs[index] + " on " + objs[index] + ", and if you " + donts[index] + " it may not count as an answer, so please keep that in mind. Keep going!");
				}
			}
		}
	} else {
		let complete = event.getVariable(stage + "stageclear");
		if (complete != null && complete)
			npc.sayNext("You all have cleared the quest for this stage. Use the portal to move to the next stage...");
		else
			npc.sayNext("Please have the party leader talk to me.");
	}
}

function getPrize() {
	let scrolls = [
		2040502, 1, 2040505, 1,				// Overall DEX and DEF
		2040802, 1,							// Gloves for DEX 
		2040002, 1, 2040402, 1, 2040602, 1	// Helmet, Topwear and Bottomwear for DEF
	];
	let potions = [
		2000001, 80, 2000002, 80, 2000003, 80,	// Orange, White, Blue Potions
		2000006, 50, 2000004, 5,				// Mana Elixir, Elixir
		2022000, 15, 2022003, 15				// Pure Water, Unagi
	];
	let equips = [
		1032004, 1, 1032005, 1, 1032009, 1,	// Level 20-25 Earrings
		1032006, 1, 1032007, 1, 1032010, 1,	// Level 30 Earrings
		1032002, 1,							// Level 35 Earring
		1002026, 1, 1002089, 1, 1002090, 1	// Bamboo Hats
	];
	let etc = [
		4010000, 15, 4010001, 15, 4010002, 15, 4010003, 15,	// Mineral Ores
		4010004, 8, 4010005, 8, 4010006, 8,					// Mineral Ores
		4020000, 8, 4020001, 8, 4020002, 8, 4020003, 8,		// Jewel Ores
		4020004, 8, 4020005, 8, 4020006, 8,					// Jewel Ores
		4020007, 3, 4020008, 3, 4003000, 30					// Diamond and Black Crystal Ores and Screws
	];

	let rewards;
	let itemSetSel = Math.random();
	if (itemSetSel < 0.3) //30% chance
		rewards = scrolls;
	else if (itemSetSel < 0.6) //30% chance
		rewards = equips;
	else if (itemSetSel < 0.9) //30% chance
		rewards = potions;
	else //10% chance
		rewards = etc;
	
	let index = Math.floor(Math.random() * (rewards.length / 2)) * 2;
	player.gainItem(rewards[index], rewards[index + 1]);
	player.changeMap(103000805, "sp");
}

let stage = map.getId() - 103000800 + 1;
let event = npc.getEvent("party1");

switch (stage) {
	case 1:
		let questions = [
			"Collect the same number of coupons as the minimum level required to make the first job advancement as warrior.",
			"Collect the same number of coupons as the minimum amount of STR needed to make the first job advancement as a warrior.",
			"Collect the same number of coupons as the minimum amount of INT needed to make the first job advancement as a magician.",
			"Collect the same number of coupons as the minimum amount of DEX needed to make the first job advancement as a bowman.",
			"Collect the same number of coupons as the minimum amount of DEX needed to make the first job advancement as a thief.",
			"Collect the same number of coupons as the minimum level required to advance to 2nd job."
		];
		let answers = [10, 35, 20, 25, 25, 30];
		if (player.getId() == party.getLeader()) {
			let preamble = event.getVariable("leader1stpreamble");
			if (preamble == null || !preamble) {
				event.setVariable("leader1stpreamble", true);
				npc.sayNext("Hello. Welcome to the first stage. Look around and you'll see Ligators wandering around. When you defeat them, they will cough up a #bcoupon#k. Every member of the party other than the leader should talk to me, geta  question, and gather up the same number of #bcoupons#k as the answer to the question I'll give to them.\r\nIf you gather up the right amount of #bcoupons#k, I'll give the #bpass#k to that player. Once all the party members other than the leader gather up the #bpasses#k and give them to the leader, the leader will hand over the #bpasses#k to me, clearing the stage in the process. The faster you take care of the stages, the more stages you'll be able to challenge. So I suggest you take care of things quickly and swiftly. Well then, best of luck to you.");
			} else {
				let complete = event.getVariable(stage + "stageclear");
				if (complete != null && complete) {
					npc.sayNext("You all have cleared the quest for this stage. Use the portal to move to the next stage...");
				} else {
					 // Check how many passes they have compared to number of party members
					let numPasses = event.getVariable("members").length - 1;
					if (!player.hasItem(4001008, numPasses)) {
						npc.sayNext("I'm sorry, but you are short on the number of passes. You need to give me the right number of passes; it should be the number of members of your party minus the leader, #b" + numPasses + " passes#k to clear the stage. Tell your party members to solve the questions, gather up the passes, and give them to you.");
					} else {
						clear(event, stage, 100);
						player.loseItem(4001008, numPasses);
						npc.sayNext("You gathered up #b" + numPasses + " passes#k! Congratulations on clearing the stage! I'll make the portal that sends you to the next stage. There's a time limit on getting there, so please hurry. Best of luck to you all!");
					}
				}
			}
		} else {
			let pVar = "member1stpreamble" + player.getId();
			let qIndexVar = "member1st" + player.getId();
			let preamble = event.getVariable(pVar);
			if (preamble == null || !preamble) {
				let qIndex = event.getVariable(qIndexVar);
				if (qIndex == null) {
					// Select a random question to ask the player.
					qIndex = Math.floor(Math.random() * questions.length);
					event.setVariable(qIndexVar, qIndex);
				}
				npc.sayNext("Here, you need to collect #bcoupons#k by defeating the same number of Ligators as the answer to the questions asked individually.");
				npc.sayNext("Here's the question. " + questions[qIndex]);
				event.setVariable(pVar, true);
			} else {
				let complete = event.getVariable(stage + "stageclear");
				if (complete != null && complete) {
					npc.sayNext("You all have cleared the quest for this stage. Use the portal to move to the next stage...");
				} else {
					let dVar = "member1stdone" + player.getId();
					complete = event.getVariable(dVar);
					// don't let one player get more than one pass for his question
					if (complete == null || !complete) {
						// Reply to player correct/incorrect response to the question they have been asked
						let numcoupons = answers[event.getVariable(qIndexVar)];
						if (!player.hasItem(4001007, numcoupons + 1) && player.hasItem(4001007, numcoupons)) {
							player.loseItem(4001007, numcoupons);
							player.gainItem(4001008, 1);
							npc.sayNext("That's the right answer! For that you have just received a #bpass#k. Please hand it to the leader of the party.");
							event.setVariable(dVar, true);
						} else {
							npc.sayNext("I'm sorry, but that is not the right answer! Please have the correct number of coupons in your inventory. Here's the question again : #b" + questions[event.getVariable(qIndexVar)] + "#k");
						}
					} else {
						npc.sayNext("Please hand your pass to your leader and have your leader contact me once all passes are collected in order to advance to the next stage.");
					}
				}
			}
		}
		break;
	case 2:
	case 3:
	case 4:
		rectangleStages(event, stage);
		break;
	case 5:
		let complete = event.getVariable(stage + "stageclear");
		if (complete == null || !complete) {
			if (player.getId() == party.getLeader()) {
				if (player.hasItem(4001008, 10)) {
					player.loseItem(4001008, 10);
					clear(event, stage, 1500);
					npc.sayNext("Here's the portal that leads you to the last, bonus stage. It's a stage that allows you to defeat regular monsters a little easier. You'll be given a set amount of time to hunt as much as possible, but you can always leave the stage in the middle of it through the NPC. Again, congratulations on clearing all the stages. Take care...");
				} else {
					npc.sayNext("Hello. Welcome to the 5th and final stage. Walk around the map and you'll be able to find some Boss monsters. Defeat all of them, gather up #bthe passes#k, and please get them to me. Once you earn your pass, the leader of your party will collect them, and then get them to me once the #bpasses#k are gathered up. The monsters may be familiar to you, but they may be much stronger than you think, so please be careful. Good luck!\r\nAs a result of complaints, it is now mandatory to kill all the Slimes! Do it!");
				}
			} else {
				npc.sayNext("Welcome to the 5th and final stage.  Walk around the map and you will be able to find some Boss monsters.  Defeat them all, gather up the #bpasses#k, and give them to your leader.  Once you are done, return to me to collect your reward.");
			}
		} else {
			npc.sayNext("Incredible! You cleared all the stages to get to this point. Here's a small prize for your job well done. Before you accept it, however, please make sure your use and etc. inventories have empty slots available.\r\n#bYou will not receive a prize if you have no free slots!#k");
			getPrize();
		}
		break;
}