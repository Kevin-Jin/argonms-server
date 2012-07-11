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

package argonms.game.net.external.handler;

import argonms.common.GlobalConstants;
import argonms.common.net.external.CheatTracker;
import argonms.common.net.external.ClientSendOps;
import argonms.common.util.input.LittleEndianReader;
import argonms.common.util.output.LittleEndianByteArrayWriter;
import argonms.game.character.GameCharacter;
import argonms.game.field.MapEntity.EntityType;
import argonms.game.field.entity.Npc;
import argonms.game.loading.quest.QuestDataLoader;
import argonms.game.net.external.GameClient;
import argonms.game.script.NpcConversationActions;
import argonms.game.script.NpcScriptManager;

/**
 *
 * @author GoldenKevin
 */
public class GameNpcHandler {
	public static void handleStartConversation(LittleEndianReader packet, GameClient gc) {
		int entId = packet.readInt();
		/*Point currentPos = */packet.readPos(); //player's position at time of click
		Npc npc = (Npc) gc.getPlayer().getMap().getEntityById(EntityType.NPC, entId);
		NpcScriptManager.getInstance().runScript(npc, gc);
	}

	public static void handleContinueConversation(LittleEndianReader packet, GameClient gc) {
		NpcConversationActions npc = gc.getNpc();
		if (npc != null)
			npc.responseReceived(packet);
	}

	public static void handleQuestAction(LittleEndianReader packet, GameClient gc) {
		byte action = packet.readByte();
		short questId = packet.readShort();
		GameCharacter player = gc.getPlayer();
		switch (action) {
			case 1 : { //start quest
				int npcId = packet.readInt();
				/*Point currentPos = */packet.readPos();
				if (QuestDataLoader.getInstance().canStartQuest(player, questId)) {
					player.startQuest(questId, npcId);
				} else {
					CheatTracker.get(gc).suspicious(CheatTracker.Infraction.PACKET_EDITING, "Tried to start quest without meeting requirements");
				}
				break;
			} case 2 : { //complete quest
				int npcId = packet.readInt();
				/*Point currentPos = */packet.readPos();
				if (packet.available() >= 4) {
					int selection = packet.readInt();
					if (QuestDataLoader.getInstance().canCompleteQuest(player, questId)) {
						player.completeQuest(questId, npcId, selection);
					} else {
						CheatTracker.get(gc).suspicious(CheatTracker.Infraction.PACKET_EDITING, "Tried to complete quest without meeting requirements");
					}
				} else {
					if (QuestDataLoader.getInstance().canCompleteQuest(player, questId)) {
						player.completeQuest(questId, npcId, -1);
					} else {
						CheatTracker.get(gc).suspicious(CheatTracker.Infraction.PACKET_EDITING, "Tried to complete quest without meeting requirements");
					}
				}
				break;
			} case 3 : { //forfeit quest
				player.forfeitQuest(questId);
				break;
			} case 4 : { //scripted quest start
				int npcId = packet.readInt();
				/*Point currentPos = */packet.readPos();
				if (QuestDataLoader.getInstance().canStartQuest(player, questId)) {
					NpcScriptManager.getInstance().runStartQuestScript(npcId, questId, gc);
				} else {
					CheatTracker.get(gc).suspicious(CheatTracker.Infraction.PACKET_EDITING, "Tried to start quest without meeting requirements");
				}
				break;
			} case 5 : { //scripted quest completed
				int npcId = packet.readInt();
				/*Point currentPos = */packet.readPos();
				if (QuestDataLoader.getInstance().canCompleteQuest(player, questId)) {
					NpcScriptManager.getInstance().runCompleteQuestScript(npcId, questId, gc);
				} else {
					CheatTracker.get(gc).suspicious(CheatTracker.Infraction.PACKET_EDITING, "Tried to complete quest without meeting requirements");
				}
				break;
			}
		}
	}
}
