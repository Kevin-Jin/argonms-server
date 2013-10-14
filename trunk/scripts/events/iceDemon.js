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
 * Logic for managing an instance map that allows Ice/Lightning Arch Mages to
 * obtain the 4th job skill Ice Demon and logic for handling the time limit.
 *
 * @author GoldenKevin
 */

let player;

function init(attachment) {
	player = attachment;

	let map = event.getMap(921100100);
	player.changeMap(map);

	map.showTimer(5 * 60);
	event.startTimer("kick", 5 * 60 * 1000);

	player.setEvent(event);
}

function playerDisconnected(player) {
	event.destroyEvent();
}

function playerChangedMap(player, destination) {
	event.destroyEvent();
}

function timerExpired(key) {
	switch (key) {
		case "kick":
			player.changeMap(211040200); //let playerChangedMap handle destroy
			break;
	}
}

function deinit() {
	player.setEvent(null);
}