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
 * Casey: Master of MiniGame (NPC 1012008),
 *   Chico: Master of MiniGame (NPC 2040014)
 * Victoria Road: Henesys Game Park (Map 100000203),
 *   Ludibrium: Ludibrium Village (Map 220000300)
 *
 * Creates minigame items and gives info on them.
 *
 * @author GoldenKevin (content from KiniroMS r227)
 */

let selection = npc.askMenu("Hey, you look like you need a breather from all that hunting. You should be enjoying the life, just like I am. Well, if you have a couple of items, I can make a trade with you for an item you can play minigames with. Now... what can I do for you?\r\n#b"
		+ "#L0#Create a minigame item#l\r\n"
		+ "#L1#Explain to me what the minigames are about#l");
if (selection == 0) {
	selection = npc.askMenu("You want to make the minigame item? Minigames aren't something you can just go ahead and play right off the bat. You'll need a specific set of items for a specific minigame. Which minigame item do you want to make?\r\n#b"
			+ "#L0#Omok Set#l\r\n"
			+ "#L1#A Set of Match Cards#l");
	if (selection == 0) {
		npc.sayNext("You want to play #bOmok#k, huh? To play it, you'll need the Omok Set. Only the ones with that item can open the room for a game of Omok, and you can play this game almost anywhere except for a few places at the market place.");
		let sets = [4080000, 4080001, 4080002, 4080003, 4080004, 4080005];
		let mat1 = [4030000, 4030000, 4030000, 4030010, 4030011, 4030011];
		let mat2 = [4030001, 4030010, 4030011, 4030001, 4030010, 4030001];
		let menuText = "The set also differs based on what kind of pieces you want to use for the game. Which set would you like to make?#b";
		for (let i = 0; i < sets.length; i++)
			menuText += "\r\n#L" + i + "##t" + sets[i] + "##l";
		selection = npc.askMenu(menuText);
		if (player.hasItem(mat1[selection], 99) && player.hasItem(mat2[selection], 99) && player.hasItem(4030009, 1) && player.canGainItem(sets[selection], 1)) {
			player.loseItem(mat1[selection], 99);
			player.loseItem(mat2[selection], 99);
			player.loseItem(4030009, 1);
			player.gainItem(sets[selection], 1);
		} else {
			npc.sayNext("#bYou want to make #t" + sets[selection] + "##k? Hmm... get me the materials, and I can do just that. Listen carefully, the materials you need will be: #r99 #t" + mat1[selection] + "#, 99 #t" + mat2[selection] + "#, 1 #t4030009##k. The monsters will probably drop those every once in a while...");
		}
	} else if (selection == 1) {
		if (player.hasItem(4030012, 99)) {
			player.loseItem(4030012, 99);
			if (player.canGainItem(4080100, 1))
				player.gainItem(4080100, 1);
			else //TODO: GMS-like line
				npc.say("Please check whether your ETC. inventory is full.");
		} else {
			npc.sayNext("You want #b#t4080100##k? Hmmm... to make #t4080100#, you'll need some #b#t4030012#s#k. #t4030012# can be obtained by taking out the monsters all around the island. Collect 99 #t4030012#s and you can make a set of Match Cards.");
		}
	}
} else if (selection == 1) {
	selection = npc.askMenu("You want to learn more about the minigames? Awesome! Ask me anything. Which minigame do you want to know more about?\r\n#b"
			+ "#L0#Omok#l\r\n"
			+ "#L1#Match Cards#l");
	if (selection == 0) {
		npc.sayNext("Here are the rules to the game of Omok. Listen carefully. Omok is a game where, you and your opponent will take turns laying a piece on the table until someone finds a way to lay 5 consecutive pieces in a line, be it horizontal, diagonal or vertical. That person will be the winner. For starters, only the ones with #bOmok Set#k can open a game room.");
		npc.sayNext("Every game of Omok will cost you #r100 mesos#k. Even if you don't have #bOmok set#k, you can enter the game room and play the game. If you don't have 100 mesos, however, then you won't be allowed in the room, period. The person opening the game room also needs 100 mesos to open the room, or there's no game. If you run out of mesos during the game, then you're automatically kicked out of the room!");
		npc.sayNext("Enter the room, and when you're ready to play, click on #bReady#k. Once the visitor clicks on #bReady#k, the owner of the room can press #bStart#k to start the game. If an unwanted visitor walks in, and you don't want to play with that person, the owner of the room has the right to kick the visitor out of the room. There will be a square box with x written on the right of that person. Click on that for a cold goodbye, ok?");
		npc.sayNext("When the first game starts, #bthe owner of the room goes first#k. Be ware that you'll be given a time limit, and you may lose your turn if you don't make your move on time. Normally, 3 x 3 is not allowed, but if there comes a point that it's absolutely necessary to put your piece there or face a game over, then you can put it there. 3 x 3 is allowed as the last line of defense! Oh, and it won't count if it's #r6 or 7 straight#k. Only 5!");
		npc.sayNext("If you know your back is up against the wall, you can request a #bRedo#k. If the opponent accepts your request, then the opponent's last move, along with yours, will be canceled out. If you ever feel the need to go to the bathroom, or take an extended break, you can request a #btie#k. The game will end in a tie if the opponent accepts the request. This may be a good way to keep your friendship intact with your buddy.");
		npc.sayNext("Once the game is over, and the next game starts, the loser will go first. Oh, and you can't leave in the middle of the game. If you do, you may need to request either a #bforfeit#k, or a #btie#k. Of course, if you request a forfeit, you'll lose the game, so be careful of that. And if you click on \"Leave\" in the middle of the game and call to leave after the game, you'll leave the room right after the game is over, so this will be a much more useful way to leave.");
	} else if (selection == 1) {
		npc.sayNext("Here are the rules to the game of Match Cards. Listen carefully. Match Cards is just like the way it sounds, finding a matching pair among the number of cards laid on the table. When all the matching pairs are found, then the person with more matching pairs will win the game. Just like Omok, you'll need #b#t4080100##k to open the game room.");
		npc.sayNext("Every game of Match Cards will cost you #r100 mesos#k. Even if you don't have #b#t4080100##k, you can enter the game room and play the game. If you don't have 100 mesos, however, then you won't be allowed in the room, period. The person opening the game room also needs 100 mesos to open the room, or there's no game. If you run out of mesos during the game, then you're automatically kicked out of the room!");
		npc.sayNext("Enter the room, and when you're ready to play, click on #bReady#k. Once the visitor clicks on #bReady#k, the owner of the room can press #bStart#k to start the game. If an unwanted visitor walks in, and you don't want to play with that person, the owner of the room has the right to kick the visitor out of the room. There will be a square box with x written on the right of that person. Click on that for a cold goodbye, ok?");
		npc.sayNext("Oh, and unlike Omok, on Match Cards, when you create the game room, you'll need to set your game on the number of cards you'll use for the game. There are three modes available: 3x4, 4x5, and 5x6, which will require 12, 20, and 30 cards. Remember, though, you won't be able to change it up once the game room is open, so if you really wish to change it up, you may have to close the room and open another one.");
		npc.sayNext("When the first game starts, #bthe owner of the room goes first#k. Beware that you'll be given a time limit, and you may lose your turn if you don't make your move on time. When you find a matching pair on your turn, you'll get to keep your turn, as long as you keep finding a pair of matching cards. Use your memorizing skills for a devestating combo of turns.");
		npc.sayNext("If you and your opponent have the same number of matched pairs, then whoever had a longer streak of matched pairs will win. If you ever feel the need to go to the bathroom, or take an extended break, you can request a #btie#k. The game will end in a tie if the opponent accepts the request. This may be a good way to keep your friendship intact with your buddy.");
		npc.say("Once the game is over, and the next game starts, the loser will go first. Oh, and you can't leave in the middle of the game. If you do, you may need to request either a #bforfeit#k, or a #btie#k. Of course, if you request a forfeit, you'll lose the game, so be careful of that. And if you click on \"Leave\" in the middle of the game and call to leave after the game, you'll leave the room right after the game is over, so this will be a much more useful way to leave.");
	}
}