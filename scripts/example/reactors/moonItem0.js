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
 * nut (Reactor 9102002),
 *   nut (Reactor 9102003) [linked from moonItem1],
 *   nut (Reactor 9102004) [linked from moonItem2],
 *   nut (Reactor 9102005) [linked from moonItem3],
 *   nut (Reactor 9102006) [linked from moonItem4],
 *   nut (Reactor 9102007) [linked from moonItem5]
 * Hidden Street: Primrose Hill (Map 910010000)
 *
 * Henesys PQ plants.
 * Drops random colors of primrose seeds to advance first stage of Henesys party
 * quest.
 *
 * @author GoldenKevin
 */

let items = [4001095, 4001096, 4001097, 4001098, 4001099, 4001100];
if (Math.floor(Math.random() * 2) == 1) //50% chance a seed is dropped
	//8.33% chance a particular color seed is drpoped
	reactor.dropItems(0, 0, 0, items[Math.floor(Math.random() * items.length)], 1000000);