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
 * Kyrin: Pirate Instructor (NPC 1072008)
 * Hidden Street: Pirate Test Room (Map 108000500),
 *   Hidden Street: Pirate Test Room (Map 108000501),
 *   Hidden Street: Pirate Test Room (Map 108000502),
 *   Hidden Street: Pirate Test Room (Map 108000503)
 *
 * Lets players out of pirates 2nd job advancement quest, whether it be for
 * forfeit or completion.
 *
 * @author GoldenKevin (content from DelphiMS r418)
 */

let brawler = player.isQuestStarted(2191);
let gunslinger = player.isQuestStarted(2192);

let item;
if (brawler)
	item = 4031856;
if (gunslinger)
	item = 4031857;

if (player.hasItem(item, 15)) {
	npc.sayNext("Ohhh... So you managed to gather up 15 #t" + item + "#s! Wasn't it tough? That's amazing... alright then, now let's talk about The Nautilus.");
	npc.sayNext("These crystals can only be used here, so I'll just take them back.");
	player.changeMap(120000101);
} else {
	let selection = npc.askYesNo("Hmmm... What is it? I don't think you have been able to gather up all #b15 #t" + item + "#s#k yet... If it's too hard for you, then you can step out and try again later. Do you want to give up and step outside right now?");

	if (selection == 0) {
		npc.sayNext("Good. You're showing me you don't want to give up this great opportunity. When you collect #b15 #t" + item + "#s#k, then talk to me.");
	} else if (selection == 1) {
		npc.sayNext("Really? Ok, I'll take you outside right now. Please don't give up, though. You'll get the opportunity to try this again. Hopefully by then, you'll be ready to handle this with ease...");
		player.changeMap(120000101);
	}
}