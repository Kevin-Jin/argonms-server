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
 * Bell: NLC Subway staff (NPC 9201057)
 * Victoria Road: Subway Ticketing Booth (Map 103000100),
 *   New Leaf City Town Street: NLC Subway Station (Map 600010001),
 *   New Leaf City Town Street: Waiting Room(From NLC to KC) (Map 600010002),
 *   Kerning City Town Street: Waiting Room(From KC to NLC) (Map 600010004)
 *
 * NPC for subway between New Leaf City and Kerning City.
 *
 * @author GoldenKevin (content from Vana r3171)
 */

let basic = (npc.getPlayerLevel() <= 10);
let cost = 5000;
if (basic)
	cost /= 5;
let item = 0;

let selection;
switch (npc.getMap()) {
	case 103000100:
		selection = npc.askYesNo("Hi, I can take you to New Leaf City (NLC) for only #b" + cost + " mesos#k. Would you like to go?");
		if (selection == 1)
			if (basic)
				item = 4031711;
			else
				item = 4031712;
		else if (selection == 0)
			npc.say("Ok, come talk to me again when you want to go to NLC.");
		break;
	case 600010001:
		selection = npc.askYesNo("Hi, I can take you back to Kerning City for only #b" + cost + " mesos#k. Would you like to go?");
		if (selection == 1)
			if (basic)
				item = 4031713;
			else
				item = 4031714;
		else if (selection == 0)
			npc.say("Ok, come talk to me again when you want to go back to Kerning City.");
		break;
	case 600010004:
		selection = npc.askYesNo("Do you want to go back to Kerning City subway station now?");
		if (selection == 1)
			npc.warpPlayer(103000100);
		else if (selection == 0)
			npc.sayNext("Okay, Please wait~!");
		break;
	case 600010002:
		selection = npc.askYesNo("Do you want to go back to New Leaf City subway station now?");
		if (selection == 1)
			npc.warpPlayer(600010001);
		else if (selection == 0)
			npc.sayNext("Okay, Please wait~!");
		break;
}

if (item != 0) {
	if (npc.playerHasMesos(cost)) {
		npc.takeMesos(cost);
		npc.giveItem(item, 1);
	} else {
		npc.say("I'm sorry, but you don't have enough money. It costs #b" + cost + " Mesos#k.");
	}
}