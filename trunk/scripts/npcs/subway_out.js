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
 * Exit (NPC 1052011)
 * Line 3 Construction Site: B1 <Area 1> (Map 103000900),
 *   Line 3 Construction Site: B1 <Area 2> (Map 103000901),
 *   Line 3 Construction Site: B2 <Area 1> (Map 103000903),
 *   Line 3 Construction Site: B2 <Area 2> (Map 103000904),
 *   Line 3 Construction Site: B3 <Area 1> (Map 103000906),
 *   Line 3 Construction Site: B3 <Area 2> (Map 103000907),
 *   Line 3 Construction Site: B3 <Area 3> (Map 103000908)
 *
 * Exit sign in Kerning City subway constructions sites, to forfeit Shumi's jump
 * quests.
 *
 * @author GoldenKevin (content from Vana r3171)
 */

if (npc.askYesNo("This device is connected to outside. Are you going to give up and leave this place? You'll have to start from scratch the next time you come in...") == 1)
	npc.warpPlayer(103000100);