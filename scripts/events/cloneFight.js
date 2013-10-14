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
 * Logic for managing an instance map for 3rd job advancement challenges.
 *
 * @author GoldenKevin
 */

let player, map1, map2;

function init(attachment) {
	let destination;
	[player, destination, adjacent] = attachment;

	//create a new instance of the map so we don't have to deal with clearing
	//the map and respawning the clone in the right position
	map1 = event.makeMap(destination);
	map2 = event.makeMap(adjacent);
	player.changeMap(map1);

	player.setEvent(event);
}

function playerDisconnected(player) {
	event.destroyEvent();
}

function playerChangedMap(player, destination) {
	if (map1.getId() != destination.getId() && map2.getId() != destination.getId())
		event.destroyEvent();
}

function deinit() {
	player.setEvent(null);

	event.destroyMap(map2);
	event.destroyMap(map1);
}