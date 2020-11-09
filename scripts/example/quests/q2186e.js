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
 * Help Me Find My Glasses (NPC 1094001 Quest 2186) End
 * Victoria Road: Nautilus Harbor (Map 120000000)
 *
 * Teleports player to Nihal Desert.
 *
 * @author GoldenKevin
 */

//TODO: GMS-like conversation
player.gainExp(1700);
player.gainItem(2030019, 10);
npc.completeQuest();