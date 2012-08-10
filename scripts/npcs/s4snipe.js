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
 * Insignificant Being (NPC 1061012)
 * Dungeon: Another Entrance (Map 105090200)
 *
 * Finishes Extreme Temptation - Junior Balrog in a Different World (Quest 6108)
 * by warping to Another World: Practice Field (Map 910500000).
 *
 * @author GoldenKevin (content from Vana r3171)
 */

if (player.isQuestActive(6108)) {
	if (party == null) {
		npc.say("You don't have a party. You can challenge with party.");
	} else if (player.getId() == party.getLeader()) {
		if (party.getMembersCount() != 2) {
			npc.say("You can make a quest when you have a party with two. Please make your party with two members.");
		} else {
			let eligible = 0;
			let members = party.getLocalMembers();
			for (let i = 0; i < members.length; i++) {
				member = members[i];
				if (member.getLevel() < 120) {
					eligible = -1;
					break;
				}
				if (Math.floor(member.getJob() / 100) == 3 && Math.floor((member.getJob() - 300) / 10) == 2 && member.getMapId() == map.getId()) // Only want 4th job bowmen
					eligible++;
			}
			if (eligible == -1) {
				npc.say("There is a character among your party whose level is not eligible. You should be level 120 above. Please adjust level.");
			} else if (eligible != 2) {
				npc.say("You can't enter. Your party member's job is not Bow Master or Marksman or Your party doesn't consist of two members.");
			} else {
				//TODO: event
				if (npc.getEvent("snipe4th") == null) {
					npc.makeEvent("snipe4th", party);
					//TODO: in init: (20 minute timer?), warp party to map
					//910500000
				} else {
					npc.say("Other parties are challenging on quest clear now. Try again later.");
				}
			}
		}
	} else {
		npc.say("After two Bowmans who made 4th advancement make a party, party leader can take to me.");
	}
} else {
	npc.say("I don't know what you're talking about.");
}