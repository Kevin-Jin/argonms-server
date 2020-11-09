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
 * El Nath Magic Spot (NPC 2012015)
 * Orbis: Orbis Tower <1st Floor> (Map 200082100)
 *
 * Warps to the Orbis Magic Spot.
 *
 * @author GoldenKevin (content from Vana r3171)
 */

if (player.hasItem(4001019, 1)) {
	let selected = npc.askYesNo("You can use #b#t4001019##k to activate #b#p2012015##k. Will you teleport to where #b#p2012014##k is?");
	if (selected == 1) {
		player.loseItem(4001019, 1);
		player.changeMap(200080200);
	}
} else {
	npc.say("There's a #b#p2012015##k that'll enable you to teleport to where #b#p2012014##k is, but you can't activate it without the scroll.");
}