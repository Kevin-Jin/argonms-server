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
 * Heena (NPC 2101)
 * Maple Road: Lower level of the Training Camp (Map 2)
 *
 * Teleports players from inside the Mushroom Town Training Camp to the exit.
 *
 * @author GoldenKevin
 */

var result = npc.askYesNo("Are you done with your training? If you wish, I will send you out from this training camp.");
if (result == 1) {
	npc.sayNext("Then, I will send you out from here. Good job.");
	npc.getClient().getPlayer().changeMap(3);
} else {
	npc.say("Haven't you finish the training program yet? If you want to leave this place, please do not hesitate to tell me.");
}