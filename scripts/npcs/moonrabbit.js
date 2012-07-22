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
 * Tory (NPC 1012112)
 * Victoria Road: Henesys Park (Map 100000200)
 *
 * Henesys PQ NPC.
 *
 * @author GoldenKevin (content from KiniroMS r227)
 */

switch (npc.getMap()) {
	case 100000200: {
		npc.sayNext("This is the #rPrimrose Hill#k. When there is a full moon the moon bunny comes to make rice cakes. Growlie wants rice cakes so you better go help him or he'll eat you.");
		let selection = npc.askMenu("Would you like to go help Growlie?\r\n#b"
				+ "#L0#Yes, I will go.#l#k");
		if (selection == 0) {
			let partyMembersInMap = npc.getMapPartyMembersCount(10, 255);
			if (partyMembersInMap == -1) {
				npc.say("You are not in a party.");
			} else if (!npc.playerIsPartyLeader()) {
				npc.say("You are not the party leader.");
			} else {
				if (partyMembersInMap >= 3) {
					//TODO: start the event
					npc.say("#e#b#rPrimrose Hill#k is not yet ready. Sorry!#n#k");
				} else {
					npc.say("Your party is not a party of three to six. Make sure all your members are present and qualified to participate in this quest.");
				}
			}
		}
		break;
	}
	case 910010400:
		npc.warpPlayer(100000200);
		npc.sayInChat("You have been warped to Henesys Park.");
		break;
	case 910010100:
		if (npc.askYesNo("Would you like go to #rHenesys Park#k?") == 1)
			npc.warpPlayer(100000200, 0);
		break;
}