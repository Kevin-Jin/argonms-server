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
 * Lakelis (NPC 9020000)
 * Victoria Road: Kerning City (Map 103000000)
 *
 * Admits players into Kerning City party quest.
 *
 * @author GoldenKevin (content from KiniroMS r227)
 */

let partyMembersInMap = npc.getMapPartyMembersCount(21, 30);
if (partyMembersInMap == -1) {
	npc.say("How about you and your party members collectively beating a quest? Here you'll find obstacles and problems where you won't be able to beat it without great teamwork.  If you want to try it, please tell the #bleader of your party#k to talk to me.");
} else if (!npc.playerIsPartyLeader()) {
	npc.say("If you want to try the quest, please tell the #bleader of your party#k to talk to me.");
} else if (partyMembersInMap < 4) {
	npc.say("Your party is not a party of four. Please make sure all your members are present and qualified to participate in this quest.");
} else {
	//TODO: start the event
	//confiscate smuggled passes and coupons
}