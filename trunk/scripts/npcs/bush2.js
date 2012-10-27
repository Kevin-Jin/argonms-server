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
 * a pile of herbs (NPC 1043001)
 * Hidden Street: The Forest of Patience <Step 5> (Map 101000104)
 *
 * Completes Sabitrama's Anti-Aging Medicine (Quest 2051) and gives rare jewel
 * or mineral ores or Red-Hearted Earrings as a reward if the quest is completed
 * and the player successfully reached the end.
 *
 * @author GoldenKevin (content from KiniroMS r227)
 */

let itemId, quantity;
if (player.isQuestActive(2051)) {
	itemId = 4031032;
	quantity = 1;
} else {
	let rewards = [4020007, 2, 4020008, 2, 4010006, 2, 1032013, 1];
	let index = Math.floor(Math.random() * (rewards.length / 2)) * 2;
	itemId = rewards[index];
	quantity = rewards[index + 1];
}

if (player.gainItem(itemId, quantity))
	player.changeMap(101000000);
else //TODO: GMS-like line
	npc.say("Please check whether your ETC. inventory is full.");