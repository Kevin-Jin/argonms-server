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
 * moonflower1 (Reactor 9108000) [custom],
 *   moonflower2 (Reactor 9108001) [custom],
 *   moonflower3 (Reactor 9108002) [custom],
 *   moonflower4 (Reactor 9108003) [custom],
 *   moonflower5 (Reactor 9108004) [custom],
 *   moonflower6 (Reactor 9108005) [custom]
 * Hidden Street: Primrose Hill (Map 910010000)
 *
 * Henesys PQ flowers.
 * Activated and blooms when the correct color primrose seed is dropped in its
 * area.
 * Overridden from script-less reactors to trigger the Moon Bunny spawn.
 *
 * @author GoldenKevin
 */

let event = reactor.getEvent("moonrabbit");
let newCount = event.getVariable("flowers") + 1;
event.setVariable("flowers", newCount);
if (newCount == 6) {
	let map = event.getMap(910010000);
	map.setNoSpawn(false);
	map.spawnMob(9300061, -180, -196);
}