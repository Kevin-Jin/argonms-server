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
 * Shuang (NPC 9040000)
 * Victoria Road: Excavation Site <Camp> (Map 101030104)
 *
 * Starts the guild quest.
 *
 * @author GoldenKevin (content from KiniroMS r227)
 */

/*let gqItems = [1032033, 4001024, 4001025, 4001026, 4001027, 4001028, 4001031, 4001032, 4001033, 4001034, 4001035, 4001037];

let selection = npc.askMenu("The path to Sharenian starts here. What would you like to do? \r\n#b"
		+ "#L0#Start a Guild Quest#l\r\n"
		+ "#L1#Join your guild's Guild Quest#l");
switch (selection) {
	case 0:
		if (npc.getPlayerGuildId() == 0 || npc.getPlayerGuildRank() >= 3) { //no guild or not guild master/jr. master
			npc.sayNext("Only a Master or Jr. Master of the guild can start an instance.");
		} else {
			 //no true requirements, make an instance and start it up
			//cm.startPQ("ZakumPQ");
			let em = npc.getEventManager("GuildQuest");
			if (em == null) {
				npc.say("This trial is currently under construction.");
			} else {
				if (getEimForGuild(em, npc.getPlayerGuildId()) != null) {
					npc.say("Your guild already has an active instance. Please try again later.")
				} else {
					//start GQ
					let guildId = npc.getPlayerGuildId();
					let eim = em.newInstance(guildId);
					em.startInstance(eim, player.getName());

					//force the two scripts on portals in the map
					let map = eim.getMapInstance(990000000);

					map.getPortal(5).setScriptName("guildwaitingenter");
					map.getPortal(4).setScriptName("guildwaitingexit");

					eim.registerPlayer(cm.getPlayer());
					cm.guildMessage("The guild has been entered into the Guild Quest. Please report to Shuang at the Excavation Camp on channel " + cm.getC().getChannel() + ".");

					//remove all GQ items from player entering
					for (let i = 0; i < gqItems.length; i++)
						player.loseItem(gqItems[i]);
				}
			}
		}
		break;
	case 1:
		if (npc.getPlayerGuildId() == 0) { //no guild or not guild master/jr. master
			npc.sayNext("You must be in a guild to join an instance.");
		} else {
			let em = npc.getEventManager("GuildQuest");
			if (em == null) {
				npc.say("This trial is currently under construction.");
			} else {
				let eim = getEimForGuild(em, npc.getPlayerGuildId());
				if (eim == null) {
					npc.say("Your guild is currently not registered for an instance.");
				} else {
					if ("true".equals(eim.getProperty("canEnter"))) {
						eim.registerPlayer(npc.getPlayer());

						//remove all GQ items from player entering
						for (let i = 0; i < gqItems.length; i++)
							player.loseItem(gqItems[i]);
					} else {
						npc.say("I'm sorry, but the guild has gone on without you. Try again later.");
					}
				}
			}
		}
		break;
}*/

//TODO: write event script