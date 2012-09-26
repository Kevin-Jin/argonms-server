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
 * Victoria Road: Henesys Park (Map 100000200),
 *   Hidden Street: Shortcut (Map 910010100),
 *   Hidden Street: Shortcut (Map 910010400)
 *
 * Admits players into Henesys PQ and warps players that have successfully
 * completed the PQ, either with or without the bonus stage, back to Henesys
 * Park.
 *
 * @author GoldenKevin
 */

switch (map.getId()) {
	case 100000200:
		if (party == null || player.getId() != party.getLeader()) {
			npc.sayNext("Hi there! I'm Tory. This place is covered with mysterious aura of the full moon, and no one person can enter here by him/herself.");
			npc.say("If you'd like to enter here, the leader of your party will have to talk to me. Talk to your party leader about this.");
		} else {
			npc.sayNext("Hi there! I'm Tory. Inside here is a beautiful hill where the primrose blooms. There's a tiger that lives in the hill, Growlie, and he seems to be looking for something to eat.");
			let selection = npc.askMenu("Would you like to head over to the hill of primrose and join forces with your party members to help Growlie out?\r\n#b"
					+ "#L0# Yes, I will go.#l");
			switch (selection) {
				case 0:
					let totalMembers = party.numberOfMembersInChannel();
					if (totalMembers >= 3
							&& party.getMembersCount(map.getId(), 1, 200) == totalMembers
							&& party.getMembersCount(map.getId(), 10, 255) == totalMembers) {
						if (npc.getEvent("moonrabbit") != null)
							//TODO: GMS-like line
							npc.sayNext("I'm sorry, but there's another party inside finishing the quest. Please, speak to me here soon.");
						else
							npc.makeEvent("moonrabbit", party);
					} else {
						npc.sayNext("I'm sorry, but the party you're a member of does NOT consist of at least 3 members. Please adjust your party to make sure that your party consists of at least 3 members that are all at Level 10 or higher. Let me know when you're done.");
					}
					break;
			}
		}
		break;
	case 910010100: {
		let selection = npc.askMenu("I appreciate you giving some rice cakes for the hungry Growlie. It looks like you have nothing else to do now. Would you like to leave this place?\r\n#b"
				+ "#L0# Yes, please get me out of here.#l");
		switch (selection) {
			case 0:
				player.loseItem(4001095);
				player.loseItem(4001096);
				player.loseItem(4001097);
				player.loseItem(4001098);
				player.loseItem(4001099);
				player.loseItem(4001100);
				player.loseItem(4001101);
				player.changeMap(100000200);
				break;
		}
		break;
	}
	case 910010400: {
		let selection = npc.askMenu("Are you guys done putting a good whooping on those pigs? It looks like you'll have nothing else to do here now. Would you like to leave this place?\r\n#b"
				+ "#L0# Yes, I'd like to leave here.#l");
		switch (selection) {
			case 0:
				player.loseItem(4001095);
				player.loseItem(4001096);
				player.loseItem(4001097);
				player.loseItem(4001098);
				player.loseItem(4001099);
				player.loseItem(4001100);
				player.loseItem(4001101);
				player.changeMap(100000200);
				break;
		}
		break;
	}
}