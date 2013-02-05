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
 * Treasure Chest (NPC 1052010)
 * Line 3 Construction Site: B3 <Subway Depot> (Map 103000909)
 *
 * Completes Shumi's Lost Bundle of Money (Quest 2057) and gives rare jewel or
 * mineral ores as a reward if the quest is completed and the player
 * successfully reached the end.
 *
 * @author GoldenKevin (content from KiniroMS r227)
 */

let itemId, quantity;
if (player.isQuestActive(2057)) {
	itemId = 4031041;
	quantity = 1;
} else {
	let rewards = [4020007, 4020008, 4010006];
	itemId = rewards[Math.floor(Math.random() * rewards.length)];
	quantity = 2;
}

if (player.gainItem(itemId, quantity))
	player.changeMap(103000100);
else //TODO: GMS-like line
	npc.say("Please check whether your ETC. inventory is full.");