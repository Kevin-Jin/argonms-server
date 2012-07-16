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
 * Mong from Kong: Internet Cafe Worker (NPC 1052012)
 * Victoria Road: Kerning City (Map 103000000)
 *
 * Maple Internet Cafe bouncer.
 *
 * @author GoldenKevin (content from Vana r3171)
 */

let selection = npc.askYesNo("Aren't you connected through the Internet Cafe? If so, then go in here ... you'll probably head to a familiar place. What do you think? Do you want to go in?");
if (selection == 1)
	npc.sayNext("Hey, hey ... I don't think you're logging on from the internet cafe. You can't enter this place if you are logging on from home ...");
else
	npc.sayNext("You must be busy, huh? But if you're loggin on from the internet cafe, then you should try going in. You may end up in a strange place once inside.");