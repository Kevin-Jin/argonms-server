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
 * q2073
 * Victoria Road: The Forest East of Henesys (Map 100010000)
 *
 * Warps players into Utah's pig farm for Camila's Gem (quest 2073).
 *
 * @author GoldenKevin (content from Vana r3171)
 */

if (player.isQuestActive(2073)) {
	if (portal.getEvent("party1") == null) {
		portal.playSoundEffect();
		portal.makeEvent("pigFarm", player);
	} else {
		portal.sayErrorInChat("It seems like someone already has visited Yoota's Farm.");
	}
} else {
	portal.sayErrorInChat("There's a door that'll lead me somewhere, but I can't seem to get in there.");
}