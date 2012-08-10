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

function playerDisconnected(playerId) {
	if (party.getLeader() == playerId) {
		for (let i = 0; i < members.length; i++)
			if (members[i].getId() != playerId)
				members[i].changeMap(EXIT_MAP, "st00");
		event.destroyEvent();
	} else {
		//next login, player will be spawned in exit map anyway, so don't warp
		//him and have to deal with packets to a disconnected player
		for (let i = 0; i < members.length; i++) {
			if (members[i].getId() == playerId) {
				members[i].setEvent(null);
				members.splice(i, 1);
				break;
			}
		}
	}
}

function playerChangedMap(playerId, map) {
	if (map == EXIT_MAP) {
		if (party.getLeader() == playerId) {
			//leader has either died and respawned or clicked Nella
			for (let i = 0; i < members.length; i++)
				if (members[i].getId() != playerId)
					members[i].changeMap(EXIT_MAP, "st00");
			event.destroyEvent();
		} else {
			//player left before leader. remove him from members so he doesn't
			//get warped back to exit map when leader exits
			for (let i = 0; i < members.length; i++) {
				if (members[i].getId() == playerId) {
					members[i].setEvent(null);
					members.splice(i, 1);
					break;
				}
			}
		}
	}
}

function partyDisbanded(partyId) {
	for (let i = 0; i < members.length; i++)
		members[i].changeMap(EXIT_MAP, "st00");
	//assert event.destroyEvent() was already called when
	//playerChangedMap(leader, EXIT_MAP) was called by Java
}

function partyMemberRemoved(partyId, playerId) {
	for (let i = 0; i < members.length; i++) {
		if (members[i].getId() == playerId) {
			members[i].changeMap(EXIT_MAP, "st00");
			//assert members[i].setEvent(null) abd members.splice(i, 1) was
			//already called when playerChangedMap(playerId, EXIT_MAP) was
			//called by Java
			break;
		}
	}
}

function timerExpired(key) {
	switch (key) {
		case "kick":
			for (let i = 0; i < members.length; i++)
				members[i].changeMap(EXIT_MAP, "st00");
			//assert event.destroyEvent() was already called when
			//playerChangedMap(leader, EXIT_MAP) was called by Java
			break;
	}
}

function deinit() {
	for (let stage = 1; stage <= 4; stage++)
		event.getMap(103000800 - 1 + stage).revertPortal("next00");

	for (let i = 0; i < members.length; i++)
		members[i].setEvent(null);
}