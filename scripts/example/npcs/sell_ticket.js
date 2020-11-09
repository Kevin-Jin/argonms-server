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
 * Joel: Ticketing Usher (NPC 1032007),
 *   Agatha: Ticketing Usher (NPC 2012000),
 *   Mel: Selling Ticket to Orbis (NPC 2040000),
 *   Mue: Ticket Usher (NPC 2082000),
 *   Syras: Ticket box desk (NPC 2102002)
 * Victoria Road: Ellinia Station (Map 101000300),
 *   Orbis: Orbis Ticketing Booth (Map 200000100),
 *   Ludibrium: Ludibrium Ticketing Place (Map 220000100),
 *   Leafre: Leafre Ticketing Booth (Map 240000100),
 *   Ariant: Ariant Station Platform (Map 260000100)
 *
 * Sells tickets for transportation between continents.
 *
 * @author GoldenKevin (content from Vana r3171)
 */

switch (map.getId()) {
	case 101000300:
		if (player.getLevel() < 10) {
			npc.sayNext("Your level seems to be too low for this. We do not allow anyone below Level 10 to get on this ride, for the sake of safety.");
		} else {
			let item;
			let price;
			if (player.getLevel() < 30) {
				item = 4031044; //Ticket to Orbis (Basic)
				price = 1000;
			} else {
				item = 4031045; //Ticket to Orbis (Regular)
				price = 5000;
			}
			let selection = npc.askYesNo("Hello, I'm in charge of selling tickets for the ship ride to Orbis Station of Ossyria. The ride to Orbis takes off every 15 minutes, beginning on the hour, and it'll cost you #b" + price + " mesos#k. Are you sure you want to purchase #b#t" + item + "##k?");
			if (selection == 1) {
				if (player.hasMesos(price) && player.canGainItem(item, 1)) {
					player.loseMesos(price);
					player.gainItem(item, 1);
				} else {
					npc.say("Are you sure you have #b" + price + " mesos#k? If so, then I urge you to check your etc. inventory, and see if it's full or not.");
				}
			} else if (selection == 0) {
				npc.sayNext("You must have some business to take care of here, right?");
			}
		}
		break;
}