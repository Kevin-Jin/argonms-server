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
 * Lenario: Manager of Guild Union (NPC 2010009)
 * Orbis: Guild Head Quarters<Hall of Fame> (Map 200000301)
 *
 * Creates, expands, and destroys alliances.
 *
 * @author GoldenKevin (content from Vana r3171)
 */

let selection = npc.askMenu("Hello there! I'm #bLenario#k.\r\n#b"
		+ "#L0#Can you please tell me what Guild Union is all about?#l\r\n"
		+ "#L1#How do I make a Guild Union?#l\r\n"
		+ "#L2#I want to make a Guild Union.#l\r\n"
		+ "#L3#I want to add more guilds for the Guild Union.#l\r\n"
		+ "#L4#I want to break up the Guild Union.#l");

switch (selection) {
	case 0:
		npc.sayNext("Guild Union is just as it says, a union of a number of guilds to form a super group. I am in charge of managing these Guild Unions.");
		break;
	case 1:
		npc.sayNext("To make a Guild Union, 2 Guild Masters need to be in a party. The leader of this party will be assigned as the Guild Union Master.");
		npc.sayNext("If the 2 Guild Masters are present, then I need 5,000,000 mesos. It's the fee you'll need to pay in order to register for a Guild Union.");
		npc.sayNext("Oh, and one more thing! It's obvious, but you can't create a new Guild Union if you are already a member of another one!");
		break;
	case 2:
		if (player.getAllianceId() != 0) {
			npc.sayNext("You cannot form a Guild Union if you are already affiliated with a different Union.");
		} else if (party == null) {
			npc.sayNext("You may not create an alliance until you get into a party of 2 people.");
		} else if (player.getId() != party.getLeader()) {
			npc.sayNext("Please let the partyleader talk to me if you want to create an union.");
		} else if (player.getGuildRank() != 1) {
			npc.sayNext("Only the Guild Master can form a Guild Union.");
		} else if (party.getMembersCount() != 2) {
			npc.sayNext("Please make sure there are only 2 players in your party.");
		} else if (party.getMembersCount(200000301, 0, 200) != 2) {
			npc.sayNext("Get your other party member on the same map please.");
		} else {
			selection = npc.askYesNo("Oh, are you interested in forming a Guild Union?");

			if (selection == 1) {
				let allianceName = npc.askText("Now please enter the name of your new Guild Union. (max. 12 letters)", "", 4, 12);
				selection = npc.askAccept("Will " + allianceName + " be the name of your Guild Union?");
				if (selection == 1) {
					player.createAlliance(allianceName);
					npc.say("You have successfully formed a Guild Union.");
				}
			}
		}
		break;
	case 3:
		if (player.getAllianceRank() != 1) {
			npc.sayNext("Only the Guild Union Master can expand the number of guilds in the Union.");
		} else {
			npc.sayNext("You may currently have up to " + player.getAllianceCapacity() + " guilds in Union. In order to add 1, it'll cost you an additional 1,000,000 mesos.");
			selection = npc.askYesNo("Would you like to spend 1 million mesos to add another guild to your Union?");

			if (selection == 1) {
				if (!player.hasMesos(1000000)) {
					npc.sayNext("You do not have enough mesos to process this.");
				} else {
					player.increaseAllianceCapacity();
					npc.say("You may now accept up to " + player.getAllianceCapacity() + " guilds in your Union.");
				}
			} else if (selection == 0) {
				npc.sayNext("If you want to expand your Guild Union, then let me know.");
			}
		}
		break;
	case 4:
		if (player.getAllianceRank() != 1) {
			npc.sayNext("Only the Guild Union Master may disband the Guild Union.");
		} else {
			selection = npc.askAccept("Are you sure you want to disband your Guild Union?");

			if (selection == 1)
				npc.say("Your Guild Union has been disbanded.");
			else if (selection == 0)
				npc.sayNext("If you want to disband your Guild Union, then please let me know");
		}
		break;
}