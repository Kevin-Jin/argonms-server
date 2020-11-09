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
 * Spinel: World Tour Guide (NPC 9000020)
 * Victoria Road: Henesys (Map 100000000),
 *   Victoria Road: Ellinia (Map 101000000),
 *   Victoria Road: Perion (Map 102000000),
 *   Victoria Road: Kerning City (Map 103000000),
 *   Victoria Road: Lith Harbor (Map 104000000),
 *   Orbis: Orbis (Map 200000000),
 *   Ludibrium: Ludibrium (Map 220000000),
 *   Leafre: Leafre (Map 240000000),
 *   Mu Lung: Mu Lung (Map 250000000),
 *   The Burning Road: Ariant (Map 260000000),
 *   Singapore: CBD (Map 540000000),
 *   Singapore: Boat Quay Town (Map 541000000),
 *   Amoria: Amoria (Map 680000000),
 *   Zipangu: Mushroom Shrine (Map 800000000)
 *
 * Teleports player from many towns to Zipangu, and back.
 *
 * @author GoldenKevin (content from Vana r3171)
 */

if (map.getId() == 800000000) {
	let [returnMap, spawnPoint] = npc.getRememberedMap("WORLD_TOUR");
	let selection = npc.askMenu("How's the traveling? Are you enjoying it?\r\n"
			+ "#b#L0#Yes, I'm done with traveling. Can I go back to #m" + returnMap + "#? #l\r\n"
			+ "#b#L1#No, I'd like to continue exploring this place.#l");
	if (selection == 0) {
		npc.sayNext("Alright. I'll now take you back to where you were before the visit to Japan. If you ever feel like traveling again down the road, please let me know!");
		player.changeMap(returnMap, spawnPoint);
		npc.resetRememberedMap("WORLD_TOUR");
	} else if (selection == 1) {
		npc.say("OK. If you ever change your mind, please let me know.");
	}
} else {
	let msg = "If you're tired of the monotonous daily life, how about getting out for a change? There's nothing quite like soaking up a new culture, learning something new by the minute! It's time for you to get out and travel. We, at the Maple Travel Agency recommend you going on a #bWorld Tour#k! Are you worried about the travel expense? ";
	if (player.getJob() == 0)
		msg += "No need to worry! The #bMaple Travel Agency#k offers first class travel accommodation for the low price of #b300 mesos#k";
	else
		msg += "You shouldn't be! We, the #bMaple Travel Agency#k, have carefully come up with a plan to let you travel for ONLY #b3,000 mesos!#k";
	npc.sayNext(msg);
	selection = npc.askMenu("We currently offer this place for your traveling pleasure: #b#m800000000# of Japan#k. I'll be there serving you as the travel guide. Rest assured, the number of destinations will increase over time. Now, would you like to head over to the #m800000000#?\r\n"
			+ "#b#L0# Yes, take me to #m800000000# (Japan)#k#l");
	if (selection == 0) {
		npc.sayNext("Would you like to travel to #b#m800000000# of Japan#k? If you desire to feel the essence of Japan, there's nothing like visiting the Shrine, a Japanese cultural melting pot. #m800000000# is a mythical place that serves the incomparable Mushroom God from ancient times.");
		npc.sayNext("Check out the female shaman serving the Mushroom God, and I strongly recommend trying Takoyaki, Yakisoba, and other delicious food sold in the streets of Japan. Now, let's head over to #b#m800000000##k, a mythical place if there ever was one.");
		let cost = 3000;
		if (player.getJob() == 0)
			cost /= 10;
		if (player.hasMesos(cost)) {
			npc.rememberMap("WORLD_TOUR");
			player.changeMap(800000000);
			player.loseMesos(cost);
		} else {
			npc.say("Please check and see if you have enough mesos to go.");
		}
	}
}