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
 * Peter (NPC 9101001)
 * Maple Road: Entrance - Mushroom Town Training Camp (Map 3)
 *
 * Teleports players from the Training Camp exit to the road to
 * Mushroom Town.
 *
 * @author GoldenKevin (content from KiniroMS r227)
 */

npc.sayNext("You have finished all your trainings. Good job. You seem to be ready to start with the journey right away! Good, I will let you move on to the next place.");
npc.sayNext("But remember, once you get out of here, you will enter a village full with monsters. Well them, good bye!");
player.changeMap(40000);
player.gainExp(3);