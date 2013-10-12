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
 * Hak: Warp Assistant (NPC 2090005)
 * Orbis: Cabin <To Mu Lung> (Map 200000141),
 *   Mu Lung: Mu Lung Temple (Map 250000100),
 *   Herb Town: Herb Town (Map 251000000)
 *
 * Teleports player from Mu Lung and Orbis stations to respective Crane rides,
 * and immediately warps players between Mu Lung and Herb Town.
 *
 * @author GoldenKevin (content from Vana r3171)
 */

function selectWarp(maps, src, dest) {
	let str = "Hello there? I'm the crane that flies from #b" + src + "#k to #b" + dest + "#k and back. I fly around all the time, so I figured, why not make some money by taking travelers like you along for a small fee? It's good business for me. Anyway, what do you think?";
	let info;
	if (Array.isArray(maps[0])) {
		str += " #b";
		for (let i = 0; i < maps.length; i++)
			str += "\r\n#L" + i + "# " + maps[i][0] + "(" + maps[i][1] + " mesos)#l";
		let selection = npc.askMenu(str);
		let info = maps[selection];

		str = "Will you move to #b#m" + info[2] + "##k now? If you have #b" + info[1] + "mesos#k, I'll take you there right now.";
	} else {
		str += " Do you want to fly to #b" + maps[0] + "#k right now? I only charge #b" + maps[1] + " mesos#k.";
		info = maps;
	}

	if (info.length == 4) {
		//trip through ride
		let event = npc.getEvent("crane");
		if (event != null && !event.getVariable(info[2] + "docked")) {
			npc.sayNext("Someone else is on the way to " + info[0] + " right now. Talk to me in a little bit.");
		} else if (!player.loseMesos(info[1])) {
			npc.sayNext("Are you sure you have enough mesos?");
		} else if (event != null) {
			player.setEvent(event);
			player.changeMap(info[2]);
		} else {
			//direct warp to destination if the event is broken
			player.changeMap(info[3]);
		}
	} else if (info.length == 3) {
		//direct warp to destination
		let selection = npc.askYesNo(str);
		if (selection == 1)
			if (!player.loseMesos(info[1]))
				player.changeMap(info[2];
			else
				npc.sayNext("Are you sure you have enough mesos?");
		else
			npc.sayNext("OK. If you ever change your mind, please let me know.");
	}
}
 
switch (map.getId()) {
	case 200000141:
		selectWarp([["Mu Lung", 6000, 200090300, 250000100]], "Orbis", "Mu Lung");
		break;
	case 250000100:
		selectWarp([["Orbis", 6000, 200090310, 200000141], ["Herb Town", 1500, 251000000], "Mu Lung", "Orbis");
		break;
	case 251000000:
		selectWarp(["Mu Lung", 1500, 250000100], "Orbis", "Mu Lung");
		break;
}