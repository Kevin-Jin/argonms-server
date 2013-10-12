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
 * Platform Usher: Station Info (NPC 2012006)
 * Orbis: Orbis Ticketing Booth (Map 200000100)
 *
 * Teleports player from Orbis Tickering Booth to the proper platforms for the
 * rides to each destination.
 *
 * @author GoldenKevin (content from Vana r3171)
 */

let selection = npc.askMenu("Orbis Station has lots of platforms available to choose from. You need to choose the one that'll take you to the destination of your choice. Which platform will you take?\r\n"
		+ "#b#L0#The platform to the ship that heads to Ellinia#l\r\n"
		+ "#b#L1#The platform to the ship that heads to Ludibrium#l#k\r\n"
		+ "#b#L2#The platform to Hak that heads to Leafre#l\r\n"
		+ "#b#L3#The platform to Hak that heads to Mu Lung#l\r\n"
		+ "#b#L4#The platform to Geenie that heads to Ariant#l#k");

let str = "Even if you took the wrong passage you can get back here using the portal, so no worries.";
let map;
switch (selection) {
	case 0:
		map = 200000110;
		str += " Will you move to the #bplatform to the ship that heads to Ellinia#k?";
		break;
	case 1:
		map = 200000120;
		str += " Will you move to the #bplatform to the ship that heads to Ludibrium#k?";
		break;
	case 2:
		map = 200000130;
		str += "\r\nWill you move to the #bplatform to the ship that heads to Leafre#k?";
		break;
	case 3:
		map = 200000140;
		str += " Will you move to the #bplatform to Hak that heads to Mu Lung#k?";
		break;
	case 4:
		map = 200000150;
		str += " Will you move to the #bplatform to Geenie that heads to Ariant#k";
		break;
}

selection = npc.askYesNo(str);
if (selection == 1) {
	player.changeMap(map, "west00");
} else if (selection == 0) {
	str = "Please make sure you know where you are going and then go to the platform through me.";
	if (map == 200000110 || map == 200000120 || map == 200000130)
		str += " The ride is on schedule so you better not miss it!";
	npc.sayNext(str);
}