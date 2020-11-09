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
 * NimaKIN (NPC 9900001)
 * Victoria Road: Ellinia (Map 180000000)
 *
 * NPC in the GM map that maxes inventory capacities and skill levels.
 *
 * @author GoldenKevin (content from Vana r3171)
 */

let showBye = false;

function chatEnded() {
	if (showBye)
		npc.say("Okay, come back to me any time if you change your mind.")
}

if (player.isGm()) {
	showBye = true;
	let selection = npc.askMenu("Hi #h #! What do you wish?\r\n#b"
			+ "#L0#I would like my inventory slots maxed#l\r\n"
			+ "#L1#I would like my skills maxed#l");
	switch (selection) {
		case 0:
			player.gainEquipInventorySlots(255);
			player.gainUseInventorySlots(255);
			player.gainSetupInventorySlots(255);
			player.gainEtcInventorySlots(255);
			player.gainCashInventorySlots(255);
			npc.say("There we go! Have fun!");
			break;
		case 1:
			player.maxSkills();
			npc.say("There we go! Have fun!");
			break;
	}
} else {
	npc.say("You need to be a GM in order for me to max your skills, #h #!");
}