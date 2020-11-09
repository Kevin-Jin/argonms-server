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
 * out00
 * Hidden Street: Free Market Entrance (Map 910000000)
 *
 * Free Market portal.
 * Warps players from Free Market to entry map/portal.
 *
 * @author GoldenKevin
 */

let [map, spawnPoint] = portal.resetRememberedMap("FREE_MARKET");
if (map == 999999999) { //warped to FM without having previous position saved
	map = 102000000; //Perion
	spawnPoint = 28; //market00 on Perion
}
portal.playSoundEffect();
player.changeMap(map, spawnPoint);