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
 * Joel: Ticketing Usher (NPC 1032007)
 * Victoria Road: Ellinia Station (Map 101000300)
 *
 * Sells tickets to Orbis
 *
 * @author GoldenKevin (content from Vana r3171)
 */

if (npc.getPlayerLevel() < 10) {
	npc.sayNext("Your level seems to be too low for this. We do not allow anyone below Level 10 to get on this ride, for the sake of safety.");
} else {
	let item;
	let price;
	if (npc.getPlayerLevel() < 30) {
		item = 4031044; //Ticket to Orbis (Basic)
		price = 1000;
	} else {
		item = 4031045; //Ticket to Orbis (Regular)
		price = 5000;
	}
	let selection = npc.askYesNo("Hello, I'm in charge of selling tickets for the ship ride to Orbis Station of Ossyria. The ride to Orbis takes off every 15 minutes, beginning on the hour, and it'll cost you #b" + price + " mesos#k. Are you sure you want to purchase #b#t" + item + "##k?");
	if (selection == 1) {
		if (npc.playerHasMesos(price) && npc.playerCanHoldItem(item, 1)) {
			npc.takeMesos(price);
			npc.giveItem(item, 1);
		} else {
			npc.say("Are you sure you have #b" + price + " mesos#k? If so, then I urge you to check your etc. inventory, and see if it's full or not.");
		}
	} else if (selection == 0) {
		npc.sayNext("You must have some business to take care of here, right?");
	}
}