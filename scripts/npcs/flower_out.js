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
 * Crumbling Statue (NPC 1061007)
 * Hidden Street: The Deep Forest of Patience <Step 1> (Map 105040310),
 *   Hidden Street: The Deep Forest of Patience <Step 2> (Map 105040311),
 *   Hidden Street: The Deep Forest of Patience <Step 3> (Map 105040312),
 *   Hidden Street: The Deep Forest of Patience <Step 4> (Map 105040313),
 *   Hidden Street: The Deep Forest of Patience <Step 5> (Map 105040314),
 *   Hidden Street: The Deep Forest of Patience <Step 6> (Map 105040315),
 *   Hidden Street: The Deep Forest of Patience <Step 7> (Map 105040316)
 *
 * Forfeits John's jump quests.
 *
 * @author GoldenKevin (content from KiniroMS r227)
 */

npc.sayNext("It's a Crumbling Statue.");
if (npc.askYesNo("Would you like to leave this place?") == 1)
	player.changeMap(105040300);