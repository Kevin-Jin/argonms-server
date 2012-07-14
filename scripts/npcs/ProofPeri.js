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
 * Nana(P): Love Fairy (NPC 9201027)
 * Victoria Road: Perion (Map 102000000)
 *
 * Barters firewood for a proof of love.
 *
 * @author GoldenKevin (content from KiniroMS r227)
 */

//TODO: implement marriage quest (?)
//if (npc.getPlayerMarriageQuestLevel() != 1 && npc.getPlayerMarriageQuestLevel() != 52) {
	npc.say("Hi, I'm Nana the love fairy... Hows it going?");
/*} else {
	if (!npc.playerHasItem(4003005, 20)) {
		npc.sayNext("Hey, you look like you need Proofs of Love? I can get them for you.");
		npc.sayNext("All you have to do is bring me 40 #b#t4000018##k.");
	} else {
		npc.sayNext("Wow, you were quick! Here's the #t4031371#...");
		npc.takeItem(4000018, 40)
		npc.giveItem(4031371, 1);
	}
}*/