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
 * Warrior Job Instructor (NPC 1072004)
 * Hidden Street: Warrior's Rocky Mountain (Map 108000300),
 *   Hidden Street: Warrior's Rocky Mountain (Map 108000301),
 *   Hidden Street: Warrior's Rocky Mountain (Map 108000302)
 *
 * Lets players out of warriors 2nd job advancement quest, whether it be for
 * forfeit or completion.
 *
 * @author GoldenKevin (content from Vana r3171)
 */

if (player.hasItem(4031013, 30)) {
	npc.sayNext("Ohhhhh...you collected all 30 Dark Marbles!! It should have been difficult...just incredible! You've passed the test and for that, I'll reward you #b#t4031012##k. Take that and go back to Perion.");
	player.loseItem(4031013);
	player.loseItem(4031008);
	player.gainItem(4031012, 1);
	player.changeMap(102000000);
} else {
	let selection = npc.askYesNo("What's going on? Doesn't look like you have collected 30 #b#t4031013##k, yet...If you're having problems with it, then you can leave, come back and try it again. So...do you want to give up and get out of here?");

	if (selection == 0) {
		npc.sayNext("That's right! Stop acting weak and start collecting the marbles. Talk to me when you have collected 30 #b#t4031013##k.");
	} else if (selection == 1) {
		npc.sayNext("Really... alright, I'll let you out. Please don't give up, though. You can always try again, so do not give up. Until then, bye...");
		player.changeMap(102020300);
	}
}