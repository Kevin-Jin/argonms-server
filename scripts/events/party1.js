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
 * Logic for starting and exiting Kerning City party quest (AKA party1) using
 * timers and party member triggers.
 *
 * @author GoldenKevin
 */

let EXIT_MAP = 103000890;

let party;
let members;

function init(attachment) {
	party = attachment;
	party.loseItem(4001008);
	party.loseItem(4001007);
	party.changeMap(103000800, "st00");
	members = party.getLocalMembers();

	event.getMap(103000800).showTimer(30 * 60);
	event.startTimer("kick", 30 * 60 * 1000);

	event.setVariable("members", members);

	for (let i = 0; i < members.length; i++)
		members[i].setEvent(event);

	for (let stage = 1; stage <= 4; stage++)
		event.getMap(103000800 - 1 + stage).overridePortal("next00", "party1");
}

function removePlayer(playerId, changeMap) {
	if (party.getLeader() == playerId) {
		//boot the entire party (changeMap parameter only applies to member
		//whose player ID matches playerId parameter, so those who are not the
		//leader are always booted)
		for (let i = 0; i < members.length; i++) {
			//dissociate event before changing map so playerChangedMap is not
			//called and this method is not called again by the player
			members[i].setEvent(null);
			if (changeMap || members[i].getId() != playerId)
				members[i].changeMap(EXIT_MAP, "st00");
		}
		event.destroyEvent();
	} else {
		for (let i = 0; i < members.length; i++) {
			if (members[i].getId() == playerId) {
				//dissociate event before changing map so playerChangedMap is
				//not called and this method is not called again by the player
				members[i].setEvent(null);
				if (changeMap)
					members[i].changeMap(EXIT_MAP, "st00");
				//collapse the members array so we don't accidentally warp
				//this member again if the leader leaves later.
				members.splice(i, 1);
				break;
			}
		}
	}
}

function playerDisconnected(playerId) {
	//changeMap is false since all PQ maps force return the player to the exit
	//map on his next login anyway, and we don't want to deal with sending
	//change map packets to a disconnected client
	removePlayer(playerId, false);
}

function playerChangedMap(playerId, destination) {
	//TODO: is it true that even when a non-leader clicks Nella, the entire
	//party is booted? and that GMS forces party out when only two members
	//remain alive and online?
	if (destination == EXIT_MAP)
		//player died and respawned or clicked Nella to leave PQ
		//changeMap is false so player doesn't get re-warped to exit map
		removePlayer(playerId, false);
}

function partyMemberDischarged(partyId, playerId) {
	removePlayer(playerId, true);
}

function timerExpired(key) {
	switch (key) {
		case "kick":
			removePlayer(party.getLeader(), true);
			break;
	}
}

function deinit() {
	for (let stage = 1; stage <= 4; stage++)
		event.getMap(103000800 - 1 + stage).revertPortal("next00");

	for (let i = 0; i < members.length; i++)
		members[i].setEvent(null);
}