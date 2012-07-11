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
 * a pile of flowers (NPC 1043000)
 * Hidden Street: The Forest of Patience <Step 2> (Map 101000101)
 *
 * Completes Sabitrama and the Diet Medicine (Quest 2050) and gives jewel ores
 * as rewards if the quest is completed and the player successfully reached the
 * top.
 *
 * @author GoldenKevin (content from KiniroMS r227)
 */

if (npc.isQuestActive(2050)) {
	npc.giveItem(4031020, 1);
} else {
	let rewards = [4020005, 4020006, 4020004, 4020001, 4020003, 4020000, 4020002];
	npc.giveItem(rewards[Math.floor(Math.random() * rewards.length)], 2);
}
npc.warpPlayer(101000000);