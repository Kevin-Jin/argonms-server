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
 * next00
 * Hidden Street: 1st Accompaniment <1st Stage> (Map 103000800),
 *   Hidden Street: 1st Accompaniment <2nd Stage> (Map 103000801),
 *   Hidden Street: 1st Accompaniment <3rd Stage> (Map 103000802),
 *   Hidden Street: 1st Accompaniment <4th stage> (Map 103000803),
 *
 * Kerning PQ portals.
 *
 * @author GoldenKevin
 */

let stage = map.getId() - 103000800 + 1;
let clear = portal.getEvent("party1").getVariable(stage + "stageclear");
if (clear != null && clear)
	player.changeMap(stage + 103000800, "st00");
else
	portal.block();