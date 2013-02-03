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
 * Heracle (NPC 2010007)
 * Orbis: Guild Head Quarters<Hall of Fame> (Map 200000301)
 *
 * Creates, expands, and destroys guilds.
 *
 * @author GoldenKevin (content from Vana r3171)
 */

if (player.getGuildId() == 0) {
	npc.sayNext("Hey ... are you interested in GUILDS by any chance?");

	let selection = npc.askMenu("#b"
			+ "#L0#What's a guild?#l\r\n"
			+ "#L1#What do I do to form a guild?#l\r\n"
			+ "#L2#I want to start a guild#l");

	switch (selection) {
		case 0:
			npc.sayNext("A guild is ... you can think of it as a small crew, a crew full of people with similar interests and goals, except it will be officially registered in our Guild Headquarters and be accepted as a valid GUILD.");
			break;
		case 1:
			npc.sayNext("To make your own guild, you'll need to be at least level 10. You'll also need to have at least 1,500,000 mesos with you. That's the fee you need to pay in order to register your guild.");
			npc.sayNext("To make a guild you'll need a total of 6 people. Those 6 should be in a same party, and the party leader should come talk to me. Please be aware that the party leader also becomes the Guild Master. Once the Guild Master is assigned, the position remains the same until the Guild breaks up.");
			npc.sayNext("Once the 6 people are gathered up, you'll need 1,500,000 mesos. This is the fee you pay to register your guild");
			npc.sayNext("Alright, to register your guild, bring 6 people here~ You can't make one without all 6...Oh, and of course, the 6 cannot be a part of some other guild!!");
			break;
		case 2:
			selection = npc.askYesNo("Alright, now, do you want to make a guild?");

			if (selection == 1) {
				if (party == null) {
					npc.sayNext("I don't care how tough you think you are... In order to form a guild, you need to be in a party of 6. Create a party of 6, and then bring all your party members back here if you are indeed serious about forming a guild.");
				} else if (player.getId() != party.getLeader()) {
					npc.sayNext("Please let the party leader talk to me if you want to create a guild.");
				} else if (player.getLevel() < 10) {
					npc.sayNext("Hmm ... I don't think you have the qualifications to be a master of the guild. Please train more to become the Master of the guild.");
				} else if (party.getMembersCount(200000301, 0, 200) < 6) {
					npc.sayNext("It seems like either you don't have enough members in your party, or some of your members are not here. I need all 6 party members here to register you as a guild. If your party can't even coordinate this simple task, you should think twice about forming a guild.");
				} else {
					let members = party.getLocalMembers();
					let eligible = true;
					for (let i = 0; i < members.length && eligible; i++)
						if (members[i].getGuildId() != 0)
							eligible = false;
					if (!eligible) {
						npc.sayNext("There seems to be a traitor among us. Someone in your party is already part of another guild. To form a guild, all of your party members must be out of their guild. Come back when you have solved the problem with the traitor.");
					} else if (!player.hasMesos(1500000)) {
						//TODO: GMS-like line
						npc.say("Please check again. You must pay the service fee to create a guild and register it.");
					} else {
						npc.sayNext("Enter the name of your guild and your guild will be created.\r\n"
								+ "The guild will also be officially registered under our Guild Headquarters, so best of luck to you and your guild!");
						let retryName = player.createGuild(npc.askGuildName());
						while (retryName != null)
							retryName = player.createGuild(retryName);
						player.loseMesos(1500000);
					}
				}
			}
			break;
	}
} else {
	let selection = npc.askMenu("Now, how can I help you?\r\n#b"
			+ "#L0#I want to expand my guild#l\r\n"
			+ "#L1#I want to break up my guild#l");

	if (player.getGuildRank() != 1) {
		npc.say("Hey, you're not the Guild Master!! This decision can only be made by the Guild Master.");
	} else {
		switch (selection) {
			case 0: {
				let capacity = player.getGuildCapacity();
				if (capacity == 100) {
					npc.sayNext("Your guild seems to have grown quite a bit. I cannot expand your guild any longer...");
				} else {
					npc.sayNext("Are you here to expand your guild? Your guild must have grown quite a bit~ To expand your guild, the guild has to be re-registered in our Guiild Headquarters, and that'll require some service fee .....");
					let fee = 500000;
					if (capacity >= 25)
						fee *= 7;
					if (capacity >= 20)
						fee *= 5;
					if (capacity >= 15)
						fee *= 3;
					selection = npc.askYesNo("The service fee will only cost you #r" + fee + " mesos#k. Would you like to expand your guild?");

					if (selection == 1) {
						if (player.hasMesos(fee))
							player.increaseGuildCapacity(5);
						else
							npc.say("Please check again. You'll need to pay the service fee in order to expand your guild and re-register it....");
					}
				}
				break;
			}
			case 1:
				if (player.getAllianceRank() == 1)
					npc.say("You need to pass the alliance leadership first before you disband the guild.");
				else if (npc.askYesNo("Are you sure you want to break up your guild? Really ... just remember, once you break up your guild, that guild will be gone forever. Oh, and one thing. If you want to break up your guild, you'll have to pay a 200,000 meso service fee. Are you sure you still want to do it?") == 1)
					player.disbandGuild();
				break;
		}
	}
}