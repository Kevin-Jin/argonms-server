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
 * Shawn (NPC 9040002)
 * Victoria Road: Excavation Site <Camp> (Map 101030104)
 *
 * Gives background info on the guild quest.
 *
 * @author GoldenKevin (content from Vana r3171)
 */

let loop = true;
let str = "We, the Union of Guilds, have been trying to decipher 'Emerald Tablet,' a treasured old relic, for a long time. As a result, we have found out that Sharenian, the mysterious country from the past, lay asleep here. We also found out that clues of #t4001024#, a legendary, mythical jewelry, may be here at the remains of Sharenian. This is why the Union of Guilds have opened Guild Quest to ultimately find #t4001024#.\r\n";
while (loop) {
	let selection = npc.askMenu(str + "#b#L0# What's Sharenian?#l \r\n"
			+ "#b#L1# #t4001024#? What's that?#l\r\n"
			+ "#b#L2# Guild Quest?#l\r\n"
			+ "#b#L3# No, I'm fine now.#l");
	str = "Do you have any other questions?\r\n";

	switch (selection) {
		case 0:
			npc.sayNext("Sharenian was a literate civilization from the past that had control over every area of the Victoria Island. The Temple of Golem, the Shrine in the deep part of the Dungeon, and other old architectural constructions where no one knows who built it are indeed made during the Sharenian times.");
			npc.sayNext("The last king of Sharenian was a gentleman named Sharen III, and apparently he was a very wise and compassionate king. But one day, the whole kingdom collapsed, and there was no explanation made for it.");
			break;
		case 1:
			npc.sayNext("#t4001024# is a legendary jewel that brings eternal youth to the one that possesses it. Ironically, it seems like everyone that had #t4001024# ended up downtrodden, which should explain the downfall of Sharenian.");
			break;
		case 2:
			npc.sayNext("I've sent groups of the explorers to Sharenian before, but none of them ever came back, which prompted us to start the Guild Quest. We've been waiting for guilds that are strong enough to take on tough challenges, guilds like yours.");
			npc.sayNext("The ultimate goal of this Guild Quest is to explore Sharenian and find #t4001024#. This is not a task where power solves everything. Teamwork is more important here.");
			break;
		case 3:
			npc.say("Really? If you have anything else to ask, please feel free to talk to me.");
			loop = false;
			break;
	}
}