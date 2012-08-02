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
 * Jake: Subway Worker (NPC 1052006)
 * Victoria Road: Subway Ticketing Booth (Map 103000100)
 *
 * Sells tickets to Kerning City subway construction sites.
 *
 * @author GoldenKevin (content from Vana r3171)
 */

let str = "You must purchase the ticket to enter. Once you have made the purchase, you can enter through The Ticket Gate on the right. What would you like to buy?";
if (player.getLevel() < 20) {
	npc.sayNext(str);
} else {
	str += "\r\n#L0##bConstruction Site B1#k#l";
	if (player.getLevel() >= 30)
		str += "\r\n#L1##bConstruction Site B2#k#l";
	if (player.getLevel() >= 40)
		str += "\r\n#L2##bConstruction Site B3#k#l";
	let selection = npc.askMenu(str);

	let site = ["construction site B1", "construction site B2", "construction site B3"];
	let cost = [500, 1200, 2000];
	let approve = npc.askYesNo("Will you purchase the ticket to #b" + site[selection] + "#k? It'll cost you " + cost[selection] + " mesos. Before making the purchase, please make sure you have an empty slot on your etc. inventory.");
	if (approve == 0) {
		npc.say("You can enter the premise once you have bought the ticket. I heard there are strange devices in there everywhere but in the end, rare precious items await you. So let me know if you ever decide to change your mind.");
	} else if (approve == 1) {
		let items = [4031036, 4031037, 4031038];
		if (!player.hasMesos(cost[selection]) || !player.canGainItem(items[selection], 1)) {
			npc.say("Are you lacking mesos? Check and see if you have an empty slot on your etc. inventory or not.");
		} else {
			player.loseMesos(cost[selection]);
			player.gainItem(items[selection], 1);
			npc.say("You can insert the ticket in the The Ticket Gate. I heard Area " + (selection + 1) + " has some precious items available but with so many traps all over the place most come back out early. Please be safe.");
		}
	}
}