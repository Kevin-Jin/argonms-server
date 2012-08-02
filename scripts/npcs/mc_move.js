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
 * Spiegelmann: Monster Carnival (NPC 2042002)
 * Victoria Road: Kerning City (Map 103000000),
 *   Orbis: Orbis (Map 200000000),
 *   Ludibrium: Ludibrium (Map 220000000),
 *   Singapore: CBD (Map 540000000),
 *   Monster Carnival: Exit (Map 980000010),
 *   Monster Carnival: Carnival Field 1 <Victorious> (Map 980000103),
 *   Monster Carnival: Carnival Field 1 <The Defeated> (Map 980000104),
 *   Monster Carnival: Carnival Field 2 <Victorious> (Map 980000203),
 *   Monster Carnival: Carnival Field 2 <The Defeated> (Map 980000204),
 *   Monster Carnival: Carnival Field 3 <Victorious> (Map 980000303),
 *   Monster Carnival: Carnival Field 3 <The Defeated> (Map 980000304),
 *   Monster Carnival: Carnival Field 4 <Victorious> (Map 980000403),
 *   Monster Carnival: Carnival Field 4 <The Defeated> (Map 980000404),
 *   Monster Carnival: Carnival Field 5 <Victorious> (Map 980000503),
 *   Monster Carnival: Carnival Field 5 <The Defeated> (Map 980000504),
 *   Monster Carnival: Carnival Field 6 <Victorious> (Map 980000603),
 *   Monster Carnival: Carnival Field 6 <The Defeated> (Map 980000604)
 *
 * Admits players into Monster Carnival and exchanges Maple Coins for items.
 *
 * @author GoldenKevin (content from Vana r3171)
 */

let MAPLE_COIN = 4001129;

function giveReward(rewardpair) {
	if (!player.hasItem(MAPLE_COIN, rewardpair[1]) || !player.canGainItem(rewardpair[0], 1)) {
		npc.sayNext("Check and see if you are either lacking #b#t4001129##k or if your Equipment inventory is full.");
	} else {
		player.loseItem(MAPLE_COIN, rewardpair[1]);
		player.gainItem(rewardpair[0], 1);
	}
}

function selectItem(items, start, end) {
	let prompt = start;
	let i;
	for (i = 0; i < items.length; i++)
		prompt += "\r\n#L" + i + "# #z" + items[i][0] + "#(" + items[i][1] + " coins)#l";
	if (end != null)
		prompt += "\r\n#L" + i + "# " + end + "#l";
	let selection = npc.askMenu(prompt);
	if (selection != i) {
		giveReward(items[selection]);
		return false;
	}
	return true;
}

let selection = npc.askMenu("What would you like to do? If you have never participated in the Monster Carnival, you'll need to know a thing or two about it before joining.\r\n#b"
		+ "#L0# Go to the Monster Carnival Field.#l\r\n"
		+ "#L1# Learn about the Monster Carnival.#l\r\n"
		+ "#L2# Trade #t4001129#.#l");

switch (selection) {
	case 0:
		if (player.getLevel() > 50 || player.getLevel() < 30) {
			npc.say("I'm sorry, but only the users within Level 30~50 may participate in Monster Carnival.");
		} else {
			npc.rememberMap("MONSTER_CARNIVAL");
			player.changeMap(980000000, "st00");
		}
		break;
	case 1: {
		let loop = true;
		while (loop) {
			selection = npc.askMenu("What do you want to do?\r\n#b"
					+ "#L0# What's a Monster Carnival?#l\r\n"
					+ "#L1# General overview of the Monster Carnival#l\r\n"
					+ "#L2# Detailed instructions about the Monster Carnival#l\r\n"
					+ "#L3# Nothing, really. I've changed my mind.#l");

			switch (selection) {
				case 0:
					npc.sayNext("Haha! I'm Spiegelmann, the leader of this traveling carnival. I started the 1st ever #bMonster Carnival#k here, waiting for travelers like you to participate in this extravaganza!");
					npc.sayNext("What's a #bMonster Carnival#k? Hahaha! let's just say that it's an experience you will never forget! It's a #bbattle against other travelers like yourself!#k");
					npc.sayNext("I know that it is way too dangerous for you to fight one another using real weapons; nor would I suggest such a barbaric act. No my friend, what I offer is competition. The thrill of battle and the excitement of competing against people just as strong and motivated as yourself. I offer the premise that your party and the opposing party both #bsummon monsters, and defeat the monsters summoned by the opposing party. That's the essence of the Monster Carnival. Also, you can use Maple Coins earned during the Monster Carnival to obtain new items and weapons! #k");
					npc.sayNext("Of course, it's not as simple as that. There are different ways to prevent the other party from hunting monsters, and it's up to you to figure out how. What do you think? Interested in a little friendly (or not-so-friendly) competition?");
					break;
				case 1:
					npc.sayNext("#bMonster Carnival#k consists of 2 parties entering the battleground, and hunting the monsters summoned by the other party. It's a #bcombat quest that determines the victor by the amount of Carnival Points (CP) earned#k.");
					npc.sayNext("Once you enter the Carnival Field, the task is to #bearn CP by hunting monsters from the opposing party, and use those CP's to distract the opposing party from hunting monsters.#k.");
					npc.sayNext("There are three ways to distract the other party: #bSummon a Monster, Skill, and Protector#k. I'll give you a more in-depth look if you want to know more about 'Detailed Instructions'.");
					npc.sayNext("Please remember this, though. It's never a good idea to save up CP just for the sake of it. #bThe CP's you've used will also help determine the winner and the loser of the carnival#k.");
					break;
				case 2:
					npc.sayNext("Once you enter the Carnival Field, you'll see a Monster Carnival window appear. All you have to do is #bselect the ones you want to use, and press OK#k. Pretty easy, right?");
					npc.sayNext("Once you get used to the commands, try using #bthe Hotkeys TAB and F1 ~  F12#k. #bTAB toggles between Summoning Monsters/Skill/Protector,#k and, #bF1~ F12 allows you to directly enter one of the windows#k.");
					npc.sayNext("#bSummon a Monster#k calls a monster that attacks the opposing party, under your control. Use CP to bring out a Summoned Monster, and it'll appear in the same area, attacking the opposing party.");
					npc.sayNext("#bSkill#k is an option of using skills such as Darkness, Weakness, and others to prevent the opposing party from defeating the monsters. It requires a lot of CP, but it's well worth it. The only problem is that it doesn't last that long. Use this tactic wisely!");
					npc.sayNext("#bProtector#k is basically a summoned item that greatly boosts the abilities of the monster summoned by your party. Protector works as long as it's not demolished by the opposing party, so I suggest you summon a lot of monsters first, and then bring out the Protector.");
					npc.sayNext("Lastly, while you're in the Monster Carnival, #byou cannot use the recovery items/potions that you carry around with you.#k However, the monsters will drop those items every once in a while, and #bas soon as you pick it up, the item will activate immediately#k. That's why it's just as important to know WHEN to pick up those items.");
					break;
				case 3:
					loop = false;
					break;
			}
		}
		break;
	}
	case 2:
		selection = npc.askMenu("Remember, if you have Maple Coins, you can trade them in for items. Please make sure you have enough Maple Coins for the item you want. Select the item you'd like to trade for! \r\n#b"
				+ "#L0# #t1122007#(50 coins)#l\r\n"
				+ "#L1# #t2041211#(40 coins)#l\r\n"
				+ "#L2# Weapon for Warriors#l\r\n"
				+ "#L3# Weapon for Magicians#l\r\n"
				+ "#L4# Weapon for Bowmen#l\r\n"
				+ "#L5# Weapon for Thieves#l\r\n"
				+ "#L6# Weapon for Pirates#l");

		switch (selection) {
			case 0:
				giveReward([1122007, 50]);
				break;
			case 1:
				giveReward([2041211, 40]);
				break;
			case 2: {
				let page = 0;
				let loop = true;
				let warriorItems = [
					[ // Page 1
						[1302004, 7],
						[1402006, 7],
						[1302009, 10],
						[1402007, 10],
						[1302010, 20],
						[1402003, 20],
						[1312006, 7],
						[1412004, 7],
						[1312007, 10],
						[1412005, 10],
						[1312008, 20],
						[1412003, 20]
					],
					[ // Page 2
						[1322015, 7],
						[1422008, 7],
						[1322016, 10],
						[1422007, 10],
						[1322017, 20],
						[1422005, 20],
						[1432003, 7],
						[1442003, 7],
						[1432005, 10],
						[1442009, 10],
						[1442005, 20],
						[1432004, 20]
					]
				];
				while (loop) {
					let start, end;
					if (page == 0)
						start = "Please make sure you have enough Maple Coins for the weapon you desire. Select the weapon you'd like to trade Maple Coins for. The selection I have is pretty good, if I do say so myself!#b";
					else
						start = "#b"; //actually, since selectItem puts a new line after this, the first line is skipped, but close enough.
					if (page != warriorItems.length - 1)
						end = "Go to the Next Page(" + (page + 1) + "/" + warriorItems.length + ")";
					else
						end = "Back to First Page(" + (page + 1) + "/" + warriorItems.length + ")";
					if (!selectItem(warriorItems[page], start, end))
						loop = false;
					else
						page = (page + 1) % warriorItems.length;
				}
				break;
			}
			case 3: {
				let magicianItems = [
					[1372001, 7],
					[1382018, 7],
					[1372012, 10],
					[1382019, 10],
					[1382001, 20],
					[1372007, 20]
				];
				selectItem(magicianItems, "Select the weapon you'd like to trade for. The weapons I have here are extremely appealing. See for yourself!#b", null);
				break;
			}
			case 4: {
				let bowmanItems = [
					[1452006, 7],
					[1452007, 10],
					[1452008, 20],
					[1462005, 7],
					[1462006, 10],
					[1462007, 20]
				];
				selectItem(bowmanItems, "Select the weapon you'd like to trade for. The weapons I have here are highly appealing. See for yourself!#b", null);
				break;
			}
			case 5: {
				let thiefItems = [
					[1472013, 7],
					[1472017, 10],
					[1472021, 20],
					[1332014, 7],
					[1332031, 10],
					[1332011, 10],
					[1332016, 20],
					[1332003, 20]
				];
				selectItem(thiefItems, "Select the weapon you'd like to trade for. The weapons I have here are of the highest quality. Select what appeals to you!#b", null);
				break;
			}
			case 6: {
				let pirateItems = [
					[1482005, 7],
					[1482006, 10],
					[1482007, 20],
					[1492005, 7],
					[1492006, 10],
					[1492007, 20]
				];
				selectItem(pirateItems, "Select the weapon you'd like to trade for. The weapons I have here are of the highest quality. Select what appeals to you!#b", null);
				break;
			}
		}
		break;
}