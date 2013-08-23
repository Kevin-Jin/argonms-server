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
 * Robo Upgrade! (NPC 9102001 Quest 4659) End
 * New Leaf City Town Street: Nea Leaf City - Town Center (Map 600000000)
 *
 * Handles evolution of Baby Robo.
 *
 * @author GoldenKevin
 */

//TODO: GMS-like lines
if (player.hasItem(4000111, 50)) {
	player.evolveBossPet();
	player.loseItem(5380000, 1);
	player.loseItem(4000111, 50);
	npc.completeQuest();
} else {
	npc.say("Insufficient quantity of #b#t4000111##k. You need #b50#k.");
}