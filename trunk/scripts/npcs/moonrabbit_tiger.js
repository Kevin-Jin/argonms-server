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
 * Growlie (NPC 1012114)
 * Hidden Street: Primrose Hill (Map 910010000)
 *
 * Clears Henesys PQ stage.
 *
 * @author GoldenKevin
 */

function warpPartyAfterClear() {
	let members = event.getVariable("members");
	for (let i = 0; i < members.length; i++)
		if (members[i].getHp() > 0)
			player.changeMap(910010100, "st00");
}

let event = npc.getEvent("moonrabbit");
if (event.getVariable("cleared")) {
	//in case leader clicked End Chat instead of Next after Growlie cleared the stage.
	npc.sayNext("Please come see me next time for more #b#t4001101##k. Have a safe trip home!");

	warpPartyAfterClear();
} else {
	let str = "Growl! I am Growlie, always ready to protect this place.\r\nWhat brought you here?\r\n#b"
			+ "#L0#Please tell me what this place is all about.#l\r\n";
	if (player.getId() == party.getLeader())
		str += "#L1#I have brought #t4001101#.#l\r\n";
	str += "#L2#I would like to leave this place.#l";
	let selection = npc.askMenu(str);

	switch (selection) {
		case 0:
			npc.sayNext("This place can be best described as the prime spot where you can taste the delicious rice cakes made by Moon Bunny every full moon.");
			npc.sayNext("Gather up the primrose seeds from the primrose leaves all over this area, and plant the seeds at the footing near the crescent moon to see the primrose bloom. There are 6 types of primroses, and all of them require different footings. It is imperative that the footing fits the seed of the flower.");
			npc.sayNext("When the flowers of primrose blooms, the full moon will rise, and that's when the Moon Bunnies will appear and start pounding the mill. Your task is to fight off the monsters to make sure that Moon Bunny can concentrate on making the best rice cake possible.");
			npc.sayNext("I would like for you and your party members to cooperate and get me 10 rice cakes. I strongly advise you to get me the rice cakes within the allotted time.");
			break;
		case 1:
			if (player.hasItem(4001101, 10)) {
				npc.sayNext("Oh... isn't this rice cake made by Moon Bunny? Please hand me the rice cake.");

				player.loseItem(4001101, 10);
				event.setVariable("cleared", true);
				map.screenEffect("quest/party/clear");
				map.soundEffect("Party1/Clear");
				let members = event.getVariable("members");
				for (let i = 0; i < members.length; i++)
					if (members[i].getHp() > 0)
						members[i].gainExp(1600);
				npc.sayNext("Mmmm ... this is delicious. Please come see me next time for more #b#t4001101##k. Have a safe trip home!");

				warpPartyAfterClear();
			} else {
				npc.say("I advise you to check and make sure that you have indeed gathered up #b10 #t4001101#s#k.");
			}
			break;
		case 2:
			selection = npc.askYesNo("Are you sure you want to leave?");
			if (selection == 1) {
				//TODO: GMS-like line
				npc.sayNext("Okay then. See you around.");
				player.changeMap(910010300, "st00");
			} else if (selection == 0) {
				//TODO: GMS-like line
				npc.sayNext("Good. Keep trying.");
			}
			break;
	}
}