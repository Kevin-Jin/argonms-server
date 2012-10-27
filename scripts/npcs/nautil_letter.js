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
 * Trash Can (NPC 1092018)
 * The Nautilus: Top Floor - Hallway (Map 120000100)
 *
 * Gives player Crumpled Letter.
 *
 * @author GoldenKevin (content from KiniroMS r227)
 */

npc.sayNext("A half-written letter... maybe it's important! Should I take a look?");
if (player.hasItem(4031839)) {
	npc.say("I've already picked one up. I don't think I'll need to pick up another one.");
} else {
	if (player.gainItem(4031839, 1))
		npc.say("I can barely make this out... but it reads Kyrin.");
	else //TODO: GMS-like line
		npc.say("Please check whether your ETC. inventory is full.");
}