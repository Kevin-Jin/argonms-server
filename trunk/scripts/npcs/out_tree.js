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
 * Scarf Snowman (NPC 2001004)
 * Hidden Street: The Hill of Christmas (Map 209000001),
 *   Hidden Street: The Hill of Christmas (Map 209000002),
 *   Hidden Street: The Hill of Christmas (Map 209000003),
 *   Hidden Street: The Hill of Christmas (Map 209000004),
 *   Hidden Street: The Hill of Christmas (Map 209000005),
 *   Hidden Street: The Hill of Christmas (Map 209000006),
 *   Hidden Street: The Hill of Christmas (Map 209000007),
 *   Hidden Street: The Hill of Christmas (Map 209000008),
 *   Hidden Street: The Hill of Christmas (Map 209000009),
 *   Hidden Street: The Hill of Christmas (Map 209000010),
 *   Hidden Street: The Hill of Christmas (Map 209000011),
 *   Hidden Street: The Hill of Christmas (Map 209000012),
 *   Hidden Street: The Hill of Christmas (Map 209000013),
 *   Hidden Street: The Hill of Christmas (Map 209000014),
 *   Hidden Street: The Hill of Christmas (Map 209000015)
 *
 * Lets the player out of the Christmas tree decorating map in Happyville.
 *
 * @author GoldenKevin (content from Vana r3171)
 */

let selection = npc.askYesNo("Have you decorated your tree nicely? It's an interesting experience, to say the least, decorating a Christmas tree with other people. Don't cha think?  Oh yeah ... are you suuuuure you want to leave this place?");
if (selection == 1)
	player.changeMap(209000000);
else
	npc.sayNext("You need more time decorating trees, huh? If you ever feel like leaving this place, feel free to come talk to me~");