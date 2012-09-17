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
 * Sparkling Crystal (NPC 1061010)
 * Hidden Street: The Other Dimension (Map 108010101),
 *   Hidden Street: The Other Dimension (Map 108010201),
 *   Hidden Street: The Other Dimension (Map 108010301),
 *   Hidden Street: The Other Dimension (Map 108010401),
 *   Shadow Zone: The Other Dimension (Map 108010501)
 *
 * Warps player out of the other dimension.
 *
 * @author GoldenKevin (content from Vana r3171)
 */

let selection = npc.askYesNo("You can use the Sparkling Crystal to go back to the real world. Are you sure you want to go back?");
if (selection == 1) {
	let toMap;
	switch (map.getId()) {
		case 108010101:
			toMap = 100000000;
			break;
		case 108010201:
			toMap = 101000000;
			break;
		case 108010301:
			toMap = 102000000;
			break;
		case 108010401:
			toMap = 103000000;
			break;
		case 108010501:
			toMap = 120000000;
			break;
	}
	player.changeMap(toMap);
}