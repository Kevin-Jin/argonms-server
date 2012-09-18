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
 * Muirhat (NPC 1092007)
 * The Nautilus: Top Floor - Hallway (Map 120000100)
 *
 * Advances A Mysterious Presence On-board The Nautilus - Disciples of the Black
 * Magician (Quest 2175)
 *
 * @author GoldenKevin (content from DelphiMS r418)
 */

if (!player.isQuestStarted(2175)) {
	npc.say("The Black Magician and his followers. Kyrin and the Crew of Nautilus. They'll be chasing one another until one of them doesn't exist, that's for sure.");
} else {
	npc.sayNext("Are you ready? Good, I'll send you to where the disciples of the Black Magician are. Look for the pigs around the area where I'll be sending you. You'll be able to find it by tracking them.");
	npc.sayNext("When they are weakened, they'll change back to their original state. If you find something suspicious, you must fight them until they are weak. I'll be here awaiting your findings.");
	player.changeMap(912000000);
}