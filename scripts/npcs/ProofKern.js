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
 * Nana(K): Love Fairy (NPC 9201023)
 * Victoria Road: Kerning City (Map 103000000),
 *   Singapore: CBD (Map 540000000),
 *   Singapore: Boat Quay Town (Map 541000000)
 *
 * Barters horned mushroom caps for a proof of love.
 *
 * @author GoldenKevin (content from KiniroMS r227)
 */

//TODO: implement marriage quest (?)
//if (npc.getPlayerMarriageQuestLevel() != 1 && npc.getPlayerMarriageQuestLevel() != 52) {
	npc.say("Hi, I'm Nana the love fairy... Hows it going?");
/*} else {
	if (!player.hasItem(4000015, 40)) {
		npc.sayNext("Hey, you look like you need Proofs of Love? I can get them for you.");
		npc.sayNext("All you have to do is bring me 40 #b#t4000015#s#k.");
	} else {
		npc.sayNext("Wow, you were quick! Here's the #t4031367#...");
		player.loseItem(4000015, 40)
		player.gainItem(4031367, 1);
	}
}*/