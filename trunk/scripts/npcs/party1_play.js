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

//TODO: Don't use AWT rectangles.

let stage;
let isLeader;
let chatState;
let questions = [
	"Collect the same number of coupons as the minimum level required to make the first job advancement as warrior.",
	"Collect the same number of coupons as the minimum amount of STR needed to make the first job advancement as a warrior.",
	"Collect the same number of coupons as the minimum amount of INT needed to make the first job advancement as a magician.",
	"Collect the same number of coupons as the minimum amount of DEX needed to make the first job advancement as a bowman.",
	"Collect the same number of coupons as the minimum amount of DEX needed to make the first job advancement as a thief.",
	"Collect the same number of coupons as the minimum level required to advance to 2nd job."
];
let qanswers = [10, 35, 20, 25, 25, 30];
let party;
let preamble;
let stage2rects = [
	java.awt.Rectangle(-770, -132, 28, 178),
	java.awt.Rectangle(-733, -337, 26, 105),
	java.awt.Rectangle(-601, -328, 29, 105),
	java.awt.Rectangle(-495, -125, 24, 165)
];
let stage2combos = [
	[0, 1, 1, 1],
	[1, 0, 1, 1],
	[1, 1, 0, 1],
	[1, 1, 1, 0]
];
let stage3rects = [
	java.awt.Rectangle(608, -180, 140, 50),
	java.awt.Rectangle(791, -117, 140, 45),
	java.awt.Rectangle(958, -180, 140, 50),
	java.awt.Rectangle(876, -238, 140, 45),
	java.awt.Rectangle(702, -238, 140, 45)
];
let stage3combos = [
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
];
let stage4rects = [
	java.awt.Rectangle(910, -236, 35, 5),
	java.awt.Rectangle(877, -184, 35, 5),
	java.awt.Rectangle(946, -184, 35, 5),
	java.awt.Rectangle(845, -132, 35, 5),
	java.awt.Rectangle(910, -132, 35, 5),
	java.awt.Rectangle(981, -132, 35, 5)
];
let stage4combos = [
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
];
let monsterIds = [
	9300002, 9300002, 9300002,								// Evil Eye
	9300000, 9300000, 9300000, 9300000, 9300000, 9300000,	// Jr. Necki
	9300003													// Slime
];
let prizeIdScroll = [
	2040502, 2040505,			// Overall DEX and DEF
	2040802,					// Gloves for DEX 
	2040002, 2040402, 2040602	// Helmet, Topwear and Bottomwear for DEF
];
let prizeIdUse = [
	2000001, 2000002, 2000003, 2000006,	// Orange, White and Blue Potions and Mana Elixir
	2000004, 2022000, 2022003			// Elixir, Pure Water and Unagi
];
let prizeQtyUse = [
	80, 80, 80, 50,
	5, 15, 15
];
let prizeIdEquip = [
	1032004, 1032005, 1032009,	// Level 20-25 Earrings
	1032006, 1032007, 1032010,	// Level 30 Earrings
	1032002,					// Level 35 Earring
	1002026, 1002089, 1002090	// Bamboo Hats
];
let prizeIdEtc = [
	4010000, 4010001, 4010002, 4010003,	// Mineral Ores
	4010004, 4010005, 4010006,			// Mineral Ores
	4020000, 4020001, 4020002, 4020003,	// Jewel Ores
	4020004, 4020005, 4020006,			// Jewel Ores
	4020007, 4020008, 4003000			// Diamond and Black Crystal Ores and Screws
];
let prizeQtyEtc = [
	15, 15, 15, 15,
	8, 8, 8,
	8, 8, 8, 8,
	8, 8, 8,
	3, 3, 30
];

function clear(eim) {
	eim.setVariable(stage + "stageclear", true);
	map.screenEffect("quest/party/clear");
	map.soundEffect("Party1/Clear");
	map.portalEffect("gate");
	//TODO: make sure next00.js is aware of current stage
	//it should warp to portal st00 of (103000800 + clearedStage)
}

function failStage() {
	map.screenEffect("quest/party/wrong_kor");
	map.soundEffect("Party1/Failed");
}

function rectangleStages() {
	let debug = false; // makes these stages clear without being correct
	let stages = ["2nd", "3rd", "4th"];
	let objs = ["ropes", "platforms", "barrels"];
	let verbs = ["hang", "stand", "stand"];
	let donts = ["hang on the ropes too low", "stand too close to the edges", "stand too close to the edges"];
	let combos = [stage2combos, stage3combos, stage4combos];
	let rects = [stage2rects, stage3rects, stage4rects];
	let objsets = [[0, 0, 0, 0], [0, 0, 0, 0, 0], [0, 0, 0, 0, 0, 0]];
	let index = stage - 2;

	let eim = npc.getEvent("kpq");
	if (player.getId() == party.getLeader()) {
		// Check for preamble
		preamble = eim.getVariable("leader" + stages[index] + "preamble");
		if (preamble == null || !preamble) {
			npc.sayNext("Hi. Welcome to the " + stages[index] + " stage. Next to me, you'll see a number of " + objs[index] + ". Out of these " + objs[index] + ", #b3 are connected to the portal that sends you to the next stage#k. All you need to do is have #b3 party members find the correct " + objs[index] + " and " + verbs[index] + " on them.#k\r\nBUT, it doesn't count as an answer if you " + donts[index] + "; please be near the middle of the " + objs[index] + " to be counted as a correct answer. Also, only 3 members of your party are allowed on the " + objs[index] + ". Once they are " + verbs[index] + "ing on them, the leader of the party must #bdouble-click me to check and see if the answer's correct or not#k. Now, find the right " + objs[index] + " to " + verbs[index] + " on!");
			eim.setVariable("leader" + stages[index] + "preamble", true);
			let sequenceNum = Math.floor(Math.random() * combos[index].length);
			eim.setVariable("stage" + stages[index] + "combo", sequenceNum);
		} else {
			// Otherwise, check for stage completed
			let complete = eim.getVariable(stage + "stageclear");
			if (complete != null && complete) {
				npc.sayNext("You all have cleared the quest for this stage. Use the portal to move to the next stage...");
			} else { // Check for people on ropes and their positions
				let totplayers = 0;
				let members = eim.getVariable("members");
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
				// First, are there 3 players on the correct positions?
				if (totplayers == 3 || debug) {
					if (debug) {
						let outstring = "Objects contain:"
						for (let i = 0; i < objsets[index].length; i++)
							outstring += "\r\n" + (i + 1) + ". " + objsets[index][i];
						npc.sayNext(outstring);
					}
					let combo = combos[index][eim.getVariable("stage" + stages[index] + "combo")];
					let testcombo = true;
					for (let i = 0; i < objsets[index].length && testcombo; i++)
						if (combo[i] != objsets[index][i])
							testcombo = false;
					if (testcombo || debug) {
						// Do clear
						clear(eim);
						party.gainExp(Math.pow(2, stage) * 50);
					} else { // Wrong
						// Do wrong
						failStage();
					}
				} else {
					npc.sayNext("It looks like you haven't found the 3 " + objs[index] + " just yet. Please think of a different combination of " + objs[index] + ". Only 3 are allowed to " + verbs[index] + " on " + objs[index] + ", and if you " + donts[index] + " it may not count as an answer, so please keep that in mind. Keep going!");
				}
			}
		}
	} else {
		let complete = eim.getVariable(stage + "stageclear");
		if (complete != null && complete)
			npc.sayNext("You all have cleared the quest for this stage. Use the portal to move to the next stage...");
		else
			npc.sayNext("Please have the party leader talk to me.");
	}
}

function getPrize() {
	let itemSetSel = Math.random();
	let itemSet;
	let itemSetQty;
	let hasQty = false;
	if (itemSetSel < 0.3)
		itemSet = prizeIdScroll;
	else if (itemSetSel < 0.6)
		itemSet = prizeIdEquip;
	else if (itemSetSel < 0.9) {
		itemSet = prizeIdUse;
		itemSetQty = prizeQtyUse;
		hasQty = true;
	} else { 
		itemSet = prizeIdEtc;
		itemSetQty = prizeQtyEtc;
		hasQty = true;
	}
	let sel = Math.floor(Math.random()*itemSet.length);
	let qty = 1;
	if (hasQty)
		qty = itemSetQty[sel];
	player.gainItem(itemSet[sel], qty);
	player.changeMap(103000805, "sp");
}

stage = map.getId() - 103000800 + 1;
preamble = null;

switch (stage) {
	case 1:
		if (player.getId() == party.getLeader()) {
			let eim = npc.getEvent("kpq");
			preamble = eim.getVariable("leader1stpreamble");
			if (preamble == null || !preamble) {
				eim.setVariable("leader1stpreamble", true);
				npc.sayNext("Hello. Welcome to the first stage. Look around and you'll see Ligators wandering around. When you defeat them, they will cough up a #bcoupon#k. Every member of the party other than the leader should talk to me, geta  question, and gather up the same number of #bcoupons#k as the answer to the question I'll give to them.\r\nIf you gather up the right amount of #bcoupons#k, I'll give the #bpass#k to that player. Once all the party members other than the leader gather up the #bpasses#k and give them to the leader, the leader will hand over the #bpasses#k to me, clearing the stage in the process. The faster you take care of the stages, the more stages you'll be able to challenge. So I suggest you take care of things quickly and swiftly. Well then, best of luck to you.");
			} else { // Check how many they have compared to number of party members
				// Check for stage completed
				let complete = eim.getVariable(stage + "stageclear");
				if (complete != null && complete) {
					npc.sayNext("You all have cleared the quest for this stage. Use the portal to move to the next stage...");
				} else {
					let numPasses = eim.getVariable("members").length - 1;
					if (!player.hasItem(4001008, numPasses)) {
						npc.sayNext("I'm sorry, but you are short on the number of passes. You need to give me the right number of passes; it should be the number of members of your party minus the leader, #b" + numPasses + " passes#k to clear the stage. Tell your party members to solve the questions, gather up the passes, and give them to you.");
					} else {
						clear(eim);
						party.gainExp(100);
						player.loseItem(4001008, numPasses);
						// TODO: Make the shiny thing flash
						npc.sayNext("You gathered up #b" + numPasses + " passes#k! Congratulations on clearing the stage! I'll make the portal that sends you to the next stage. There's a time limit on getting there, so please hurry. Best of luck to you all!");
					}
				}
			}
		} else {
			let eim = npc.getEvent("kpq");
			let pstring = "member1stpreamble" + player.getId();
			preamble = eim.getVariable(pstring);
			if (preamble == null || !preamble) {
				let qstring = "member1st" + player.getId();
				let question = eim.getVariable(qstring);
				if (question == null) {
					// Select a random question to ask the player.
					question = Math.floor(Math.random() * questions.length);
					eim.setVariable(qstring, question);
				}
				npc.sayNext("Here, you need to collect #bcoupons#k by defeating the same number of Ligators as the answer to the questions asked individually.");
				qstring = "member1st" + player.getId();
				npc.sayNext("Here's the question. " + questions[question]);
				eim.setVariable(pstring, true);
			} else { // Otherwise, check for stage completed
				let complete = eim.getVariable(stage + "stageclear");
				if (complete != null && complete) {
					npc.sayNext("You all have cleared the quest for this stage. Use the portal to move to the next stage...");
				} else {
					// Reply to player correct/incorrect response to the question they have been asked
					let qstring = "member1st" + player.getId();
					let numcoupons = qanswers[eim.getVariable(qstring)];
					let qcorr = player.hasItem(4001007, numcoupons + 1);
					let enough = false;
					if (!qcorr) { // Not too many
						qcorr = player.hasItem(4001007, numcoupons);
						if (qcorr) { // Just right
							player.loseItem(4001007, numcoupons);
							player.gainItem(4001008, 1);
							npc.sayNext("That's the right answer! For that you have just received a #bpass#k. Please hand it to the leader of the party.");
							enough = true;
						}
					}
					if (!enough) {
						qstring = "member1st" + player.getId();
						let question = eim.getVariable(qstring);
						npc.sayNext("I'm sorry, but that is not the right answer! Please have the correct number of coupons in your inventory. Here's the question again : #b" + questions[question] + "#k");
					}
				}
			}
		}
		break;
	case 2:
	case 3:
	case 4:
		rectangleStages();
		break;
	case 5:
		let eim = npc.getEvent("kpq");
		let complete = eim.getVariable(stage + "stageclear");
		if (complete == null && complete) {
			if (player.getId() == party.getLeader()) {
				let passes = player.hasItem(4001008, 10);
				if (passes) {
					// Clear stage
					player.loseItem(4001008, 10);
					clear(eim);
					party.gainExp(1500);
					npc.sayNext("Here's the portal that leads you to the last, bonus stage. It's a stage that allows you to defeat regular monsters a little easier. You'll be given a set amount of time to hunt as much as possible, but you can always leave the stage in the middle of it through the NPC. Again, congratulations on clearing all the stages. Take care...");
				} else { // Not done yet
					npc.sayNext("Hello. Welcome to the 5th and final stage. Walk around the map and you'll be able to find some Boss monsters. Defeat all of them, gather up #bthe passes#k, and please get them to me. Once you earn your pass, the leader of your party will collect them, and then get them to me once the #bpasses#k are gathered up. The monsters may be familiar to you, but they may be much stronger than you think, so please be careful. Good luck!\r\nAs a result of complaints, it is now mandatory to kill all the Slimes! Do it!");
				}
			} else {
				npc.sayNext("Welcome to the 5th and final stage.  Walk around the map and you will be able to find some Boss monsters.  Defeat them all, gather up the #bpasses#k, and give them to your leader.  Once you are done, return to me to collect your reward.");
			}
		} else { // Give rewards and warp to bonus
			npc.sayNext("Incredible! You cleared all the stages to get to this point. Here's a small prize for your job well done. Before you accept it, however, please make sure your use and etc. inventories have empty slots available.\r\n#bYou will not receive a prize if you have no free slots!#k");
			getPrize(eim);
		}
		break;
}