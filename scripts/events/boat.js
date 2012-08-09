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
 * Logic for transferring players from Ellinia or Orbis waiting rooms to
 * respective boats, and finally to the opposite station using timers.
 *
 * @author GoldenKevin
 */

function untilNextQuarterHour() {
	let now = new Date();
	return ((15 - now.getMinutes() % 15) * 60 - now.getSeconds()) * 1000 - now.getMilliseconds();
}