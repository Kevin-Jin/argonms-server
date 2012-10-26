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
 * market01
 * Hidden Street: Free Market Entrance (Map 910000000)
 *
 * Free Market portal.
 * Warps players from Free Market to entry map/portal.
 *
 * @author GoldenKevin
 */

function getPortal(map) {
	//TODO: less hackish way of doing this - perhaps save portal as a db variable
	//if we implement a method of entering FM from a map that doesn't have an FM
	//portal, this fails. we probably would have to save the nearest portal to
	//the db in such a case.
	switch (map) {
		case 230000000: //Aquarium FM
			return "market01";
		default:
			return "market00";
	}
}

let map = portal.resetRememberedMap("FREE_MARKET");
if (map == 999999999)
	map = 102000000;
portal.playSoundEffect();
player.changeMap(map, getPortal(map));