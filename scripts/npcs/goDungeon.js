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
 * Jeff: Dungeon Guide (NPC 2030000)
 * El Nath: Ice Valley II (Map 211040200)
 *
 * Allows player to obtain Ice Demon skill or enter Dead Mine.
 *
 * @author GoldenKevin (content from Vana r3171)
 */

if (player.isQuestStarted(6141) && player.hasItem(4031450, 1)) {
	let selection = npc.askYesNo("I see you have a #b#t4031450##k. This is great quality from Vogen. Would you like to enter #m921100100#?");
	if (selection == 1) {
		if (npc.makeEvent("iceDemon", true, player) == null)
			npc.sayNext("Someone is currently using the room. Try again later.");
	} else if (selection == 0) {
		npc.sayNext("Come back when you're ready.");
	}
} else {
	npc.sayNext("Hey, you look like you want to go farther and deeper past this place. Over there, though, you'll find yourself surrounded by aggressive, dangerous monsters, so even if you feel that you're ready to go, please be careful. Long ago, a few brave men from our town went in wanting to eliminate anyone threatening the town, but never came back out...");

	let str = "If you are thinking of going in, I suggest you change your mind. But if you really want to go in...I'm only letting in the ones that are strong enough to stay alive in there. I do not wish to see anyone else die. Let's see ... Hmmm ...";
	if (player.getLevel() < 50) {
		npc.say(str + " you haven't reached Level 50 yet. I can't let you in, then, so forget it.");
	} else {
		let selection = npc.askYesNo(str + "! You look pretty strong. All right, do you want to go in?");
		if (selection == 1) {
			player.changeMap(211040300, "sp");
		} else if (selection == 0) {
			npc.sayNext("Even if your level's high it's hard to actually go in there, but if you ever change your mind please find me. After all, my job is to protect this place.");
		}
	}
}