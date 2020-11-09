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
 * Maple Leaf Marble (NPC 2012023)
 * Orbis: Top of the Hill (Map 200000300)
 *
 * Related to Maple marble (Quest 6230).
 *
 * @author GoldenKevin (content from Vana r3171)
 */

if (player.isQuestActive(6230) && player.hasItem(4031476, 1)) {
	player.loseItem(4031476, 1);
	if (!player.gainItem(4031456, 1))
		npc.say("Maple Marble can't be earned as there's no blank on Others window. Make a blank and try again.");
	else
		npc.say("Maple leaves were absorbed into sparkling glass marble.");
}