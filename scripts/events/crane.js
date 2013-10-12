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
 * Logic for transferring players from cranes between Mu Lung and Orbis to the
 * opposite station using timers.
 *
 * @author GoldenKevin
 */

let muLungStation, orbisStation,
		toMuLung, toOrbis;

function init(attachment) {
	muLungStation = event.getMap(250000100);
	orbisStation = event.getMap(251000000);
	toMuLung = event.getMap(200090300);
	toOrbis = event.getMap(200090310);

	event.setVariable(toMuLung.getId() + "docked", true);
	event.setVariable(toOrbis.getId() + "docked", true);
}

function playerChangedMap(player, destination) {
	if (destination.getId() == toMuLung.getId() || destination.getId() == toOrbis.getId()) {
		event.setVariable(destination.getId() + "docked", false);

		destination.showTimer(60);
		event.startTimer(destination.getId(), 60 * 1000);

		player.setEvent(null);
	}
}

function timerExpired(key) {
	if (key == toMuLung.getId()) {
		toMuLung.transferPlayers(muLungStation.getId());
		event.setVariable(toMuLung.getId() + "docked", true);
	} else if (key == toOrbis.getId()) {
		toOrbis.transferPlayers(orbisStation.getId());
		event.setVariable(toOrbis.getId() + "docked", true);
	}
}