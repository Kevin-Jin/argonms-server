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
 * Logic for transferring players from New Leaf City or Kerning City waiting
 * rooms to respective subways, and finally to the opposite station using
 * timers.
 *
 * @author GoldenKevin
 */

let nlcStation, kcStation,
		toNlcWait, toKcWait,
		toNlc, toKc;

function init(attachment) {
	let remainingTimeUntilDeparture = untilNextTenMinuteInterval();
	if (remainingTimeUntilDeparture > 5 * 60 * 1000) {
		//subway is undocked until last five minutes to departure
		undock(remainingTimeUntilDeparture - 5 * 60 * 1000, false);
	} else if (remainingTimeUntilDeparture > 60 * 1000) {
		//subway is docked and boarding until last minute to departure
		dock(remainingTimeUntilDeparture - 60 * 1000, false);
	} else {
		//doors closed until takeoff
		closeDoors(remainingTimeUntilDeparture, false);
	}
	nlcStation = event.getMap(600010001);
	kcStation = event.getMap(103000100);
	toNlcWait = event.getMap(600010004);
	toKcWait = event.getMap(600010002);
	toNlc = event.getMap(600010005);
	toKc = event.getMap(600010003);
}

function untilNextTenMinuteInterval() {
	let now = new Date();
	return ((10 - now.getMinutes() % 10) * 60 - now.getSeconds()) * 1000 - now.getMilliseconds();
}

function undock(remainingTimeUntilArrival, transition) {
	event.setVariable("board", false);
	event.setVariable("docked", false);
	event.startTimer("dock", remainingTimeUntilArrival);

	if (transition) {
		//warp players from waiting room to ship
		toNlcWait.transferPlayers(toNlc.getId());
		toKcWait.transferPlayers(toKc.getId());
	}
}

function dock(remainingTimeUntilDoorsClosed, transition) {
	event.setVariable("board", true);
	event.setVariable("docked", true);
	event.startTimer("closedoors", remainingTimeUntilDoorsClosed);

	if (transition) {
		//warp players from ship to stations
		toNlc.transferPlayers(nlcStation.getId());
		toKc.transferPlayers(kcStation.getId());
	}
}

function closeDoors(remainingTimeUntilDeparture, transition) {
	event.setVariable("board", false);
	event.setVariable("docked", true);
	event.startTimer("takeoff", remainingTimeUntilDeparture);
}

function timerExpired(key) {
	switch (key) {
		case "dock":
			//doors close in 4 minutes
			dock(4 * 60 * 1000, true);
			break;
		case "closedoors":
			//undock in a minute
			closeDoors(60 * 1000, true);
			break;
		case "takeoff":
			//next subway will arrive in 5 minutes
			undock(5 * 60 * 1000, true);
			break;
	}
}