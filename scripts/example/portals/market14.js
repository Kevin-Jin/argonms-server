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
 * market00
 * Korean Folk Town: Korean Folk Town (Map 222000000)
 *
 * Korean Folk Town Free Market portal.
 * Warps players from Korean Folk Town to Free Market.
 *
 * @author GoldenKevin
 */

portal.rememberMap("FREE_MARKET");
portal.playSoundEffect();
player.changeMap(910000000, "out00");