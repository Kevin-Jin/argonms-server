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
 * Cherry: Ticketing Usher (NPC 1032008)
 * Victoria Road: Ellinia Station (Map 101000300)
 *
 * Teleports player from Ellinia Station to the waiting room on
 * the boat to Orbis.
 *
 * @author GoldenKevin (content from KiniroMS r227)
 */

//TODO: implement events
/*bm = cm.getEventManager("Boats");
if (bm == null) {*/
	cm.sendYesNo("It looks like there's a problem with the boat to Orbis. I could instead warp you straight there as long as you have the original ticket you bought for the trip. Would you like to go straight to Orbis? Strong monsters and a whole new world of adventures await you there.");
	if (selection == 0) {
		npc.sayNext("You must have some business to take care of here, right?");
	} else {
		if (!player.hasItem(4031045, 1)) {
			npc.sayNext("Oh no ... I don't think you have the ticket with you. I can't let you in without it. Please buy the ticket at the ticketing booth.");
		} else {
			player.loseItem(4031045, 1);
			player.changeMap(200000100);
		}
	}
/*} else if (bm.getProperty("entry").equals("true")) {
	let selection = npc.askYesNo("This will not be a short flight, so if you need to take care of some things, I suggest you do that first before getting on board. Do you still wish to board the ship?");
	if (selection == 0) {
		npc.sayNext("You must have some business to take care of here, right?");
	} else {
		if (!player.hasItem(4031045, 1)) {
			npc.sayNext("Oh no ... I don't think you have the ticket with you. I can't let you in without it. Please buy the ticket at the ticketing booth.");
		} else {
			player.loseItem(4031045, 1);
			player.changeMap(101000301);
		}
	}
} else if (bm.getProperty("entry").equals("false") && bm.getProperty("docked").equals("true")) {
	cm.sendNext("The ship is getting ready for takeoff. I'm sorry, but you'll have to get on the next ride. The ride schedule is available through the usher at the ticketing booth.");
} else {
	cm.sendNext("We will begin boarding 5 minutes before the takeoff. Please be patient and wait for a few minutes. Be aware that the ship will take off on time, and we stop receiving tickets 1 minute before that, so please make sure to be here on time.");
}*/