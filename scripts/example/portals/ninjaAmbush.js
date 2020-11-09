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
 * out00 [custom]
 * Hidden Street: Dark Lord's Practice Field <1st Stage> (Map 910300000)
 *
 * The One Hidden in the Shadow -  The Night Warrior (Quest 6141) portal.
 * Overridden from script-less portal because the WZ data is broken. Victoria
 * Road: Kerning City (Map 103000000) does not have a portal named hide00, so
 * the portal would not warp the player out of the map. The ninjaAmbush event
 * would be canceled even if this portal was scriptless because the
 * playerChangedMap trigger would be fired, so no special event code handling is
 * done here.
 *
 * @author GoldenKevin
 */

player.changeMap(103000000);