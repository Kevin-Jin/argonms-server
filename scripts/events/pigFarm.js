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
 * Logic for handling the time limit for Camila's Gem jump quest.
 *
 * @author GoldenKevin
 */

let player;

function init(attachment) {
	player = attachment;

	player.changeMap(900000000, "out01");

	event.getMap(900000000).showTimer(10 * 60);
	event.startTimer("kick", 10 * 60 * 1000);

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
			player.changeMap(100030000, "quest00"); //let playerChangedMap handle destroy
			break;
	}
}

function deinit() {
	player.setEvent(null);
}