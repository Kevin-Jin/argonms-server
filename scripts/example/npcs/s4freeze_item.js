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
 * Ancient Icy Stone (NPC 2030014)
 * Hidden Street: Ice Valley (Map 921100100)
 *
 * Gives item that teaches Ice Demon skill.
 *
 * @author GoldenKevin (content from Vana r3171)
 */

if (player.hasItem(4031450, 1)) {
	if (player.gainItem(2280011, 1)) {
		player.loseItem(4031450, 1);
		npc.sayNext("As you collect the ice, your hammer breaks.");
	} else {
		npc.sayNext("You have no inventory space.");
	}
} else {
	npc.sayNext("You have nothing to pick the ice with.");
}