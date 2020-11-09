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
 * Tylus (NPC 2022004)
 * Hidden Street: Protecting Tylus : Complete (Map 921100301)
 *
 * Related to Protecting Tylus (Quest 6192).
 *
 * @author GoldenKevin
 */

let changeMap = true;
if (player.isQuestStarted(6192)) {
	npc.sayNext("Thank you for guarding me. I could do my mission thanks to you. Talk to me when you're out.");
	if (player.hasItem(4031495, 0)) {
		if (!player.gainItem(4031495, 1)) {
			npc.say("You're not given items as there's no blank in Others box. Make a blank and talk to me again.");
			changeMap = false;
		}
	}
}

if (changeMap)
	player.changeMap(211000001);