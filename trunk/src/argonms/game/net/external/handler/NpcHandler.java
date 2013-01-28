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

import argonms.common.net.external.CheatTracker;
import argonms.common.util.input.LittleEndianReader;
import argonms.game.character.GameCharacter;
import argonms.game.field.MapEntity.EntityType;
import argonms.game.field.entity.Npc;
import argonms.game.loading.quest.QuestDataLoader;
import argonms.game.net.external.GameClient;
import argonms.game.net.external.GamePackets;
import argonms.game.script.NpcScriptManager;
import argonms.game.script.binding.ScriptNpc;
import argonms.game.script.binding.ScriptObjectManipulator;

/**
 *
 * @author GoldenKevin
 */
public final class NpcHandler {
	public static void handleStartConversation(LittleEndianReader packet, GameClient gc) {
		int entId = packet.readInt();
		/*Point currentPos = */packet.readPos(); //player's position at time of click
		Npc npc = (Npc) gc.getPlayer().getMap().getEntityById(EntityType.NPC, entId);
		NpcScriptManager.getInstance().runScript(npc, gc);
	}

	public static void handleContinueConversation(LittleEndianReader packet, GameClient gc) {
		ScriptNpc npc = gc.getNpc();
		if (npc != null)
			ScriptObjectManipulator.npcResponseReceived(npc, packet);
	}

	public static void handleQuestAction(LittleEndianReader packet, GameClient gc) {
		byte action = packet.readByte();
		short questId = packet.readShort();
		GameCharacter player = gc.getPlayer();
		switch (action) {
			case 1: { //start quest
				int npcId = packet.readInt();
				/*Point currentPos = */packet.readPos();
				byte error = QuestDataLoader.getInstance().startRequirementError(player, questId);
				if (error == 0) {
					player.startQuest(questId, npcId);
				} else {
					gc.getSession().send(GamePackets.writeQuestActionError(questId, error));
					CheatTracker.get(gc).suspicious(CheatTracker.Infraction.POSSIBLE_PACKET_EDITING, "Tried to start quest without meeting requirements");
				}
				break;
			}
			case 2: { //complete quest
				int npcId = packet.readInt();
				/*Point currentPos = */packet.readPos();
				int selection = packet.available() < 4 ? -1 : packet.readInt();
				byte error = QuestDataLoader.getInstance().completeRequirementError(player, questId);
				if (error == 0) {
					player.completeQuest(questId, npcId, selection);
				} else {
					gc.getSession().send(GamePackets.writeQuestActionError(questId, error));
					CheatTracker.get(gc).suspicious(CheatTracker.Infraction.POSSIBLE_PACKET_EDITING, "Tried to complete quest without meeting requirements");
				}
				break;
			}
			case 3: //forfeit quest
				player.forfeitQuest(questId);
				break;
			case 4: { //scripted quest start
				int npcId = packet.readInt();
				/*Point currentPos = */packet.readPos();
				byte error = QuestDataLoader.getInstance().startRequirementError(player, questId);
				if (error == 0) {
					NpcScriptManager.getInstance().runStartQuestScript(npcId, questId, gc);
				} else {
					gc.getSession().send(GamePackets.writeQuestActionError(questId, error));
					CheatTracker.get(gc).suspicious(CheatTracker.Infraction.POSSIBLE_PACKET_EDITING, "Tried to start quest without meeting requirements");
				}
				break;
			}
			case 5: { //scripted quest completed
				int npcId = packet.readInt();
				/*Point currentPos = */packet.readPos();
				byte error = QuestDataLoader.getInstance().completeRequirementError(player, questId);
				if (error == 0) {
					NpcScriptManager.getInstance().runCompleteQuestScript(npcId, questId, gc);
				} else {
					gc.getSession().send(GamePackets.writeQuestActionError(questId, error));
					CheatTracker.get(gc).suspicious(CheatTracker.Infraction.POSSIBLE_PACKET_EDITING, "Tried to complete quest without meeting requirements");
				}
				break;
			}
		}
	}

	private NpcHandler() {
		//uninstantiable...
	}
}
