/*
 * ArgonMS MapleStory server emulator written in Java
 * Copyright (C) 2011  GoldenKevin
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
 * Sera (NPC 2100)
 * Entrance - Mushroom Town Training Camp (Map 0)
 *
 * Greets newly created players at the entrance of the
 * Mushroom Town Training Camp.
 *
 * @author GoldenKevin
 */

var answered = false;

var result = npc.askYesNo("Hello! Welcome to the world of MapleStory. Would you like a cookie?");
respond();

function endChat() {
	if (!answered) {
		result = npc.askYesNo("Do you really want to leave before answering me?");
		if (result == 1) {
			npc.sayNext("What kind of person would give up a cookie?! Jeez.");
			npc.say("But, since you insist, I guess I'll stop pestering you");
		} else {
			result = npc.askYesNo("Would you like a cookie?");
			respond();
		}
	}
}

function respond() {
	answered = true;
	if (result == 1) {
		npc.sayNext("Just wait a sec...");
		npc.sayNext("Almost there...");
		npc.say("Oh too bad! I don't have any!");
	} else {
		npc.say("No? Well, that's okay!");
	}
}
