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

package argonms.game.script;

import argonms.common.GlobalConstants;
import argonms.common.net.external.ClientSendOps;
import argonms.common.util.output.LittleEndianByteArrayWriter;
import argonms.game.loading.npc.NpcDataLoader;
import argonms.game.loading.npc.NpcStorageKeeper;
import argonms.game.loading.quest.QuestDataLoader;
import argonms.game.loading.shop.NpcShop;
import argonms.game.loading.shop.NpcShopDataLoader;
import argonms.game.net.external.GameClient;
import argonms.game.net.external.GamePackets;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContinuationPending;
import org.mozilla.javascript.Script;
import org.mozilla.javascript.Scriptable;

/**
 *
 * @author GoldenKevin
 */
public class NpcScriptManager {
	private static final Logger LOG = Logger.getLogger(NpcScriptManager.class.getName());

	private static NpcScriptManager singleton;

	private final String npcPath;
	private final String questPath;

	private NpcScriptManager(String scriptsPath) {
		npcPath = scriptsPath + "npcs" + GlobalConstants.DIR_DELIMIT;
		questPath = scriptsPath + "quests" + GlobalConstants.DIR_DELIMIT;
	}

	private boolean openDefault(int npcId, GameClient client) {
		NpcShop shop = NpcShopDataLoader.getInstance().getShopByNpc(npcId);
		if (shop != null) {
			client.setNpcRoom(shop);
			client.getSession().send(GamePackets.writeNpcShop(client.getPlayer(), npcId, shop));
			return true;
		}
		NpcStorageKeeper storage = NpcDataLoader.getInstance().getStorageById(npcId);
		if (storage != null) {
			client.setNpcRoom(storage);
			client.getSession().send(GamePackets.writeNpcStorage(npcId, client.getPlayer().getStorageInventory()));
			return true;
		}
		return false;
	}

	//scripts for shop NPC overrides the shop.
	public void runScript(int npcId, GameClient client) {
		Context cx = Context.enter();
		NpcConversationActions convoMan = null;
		try {
			FileReader reader = new FileReader(npcPath + npcId + ".js");
			Scriptable globalScope = cx.initStandardObjects();
			cx.setOptimizationLevel(-1); // must use interpreter mode
			Script script = cx.compileReader(reader, "n" + npcId, 1, null);
			reader.close();
			convoMan = new NpcConversationActions(npcId, client, globalScope);
			globalScope.put("npc", globalScope, convoMan);
			client.setNpc(convoMan);
			cx.executeScriptWithContinuations(script, globalScope);
			convoMan.endConversation();
		} catch (ContinuationPending pending) {
			convoMan.setContinuation(pending.getContinuation());
		} catch (FileNotFoundException ex) {
			if (!openDefault(npcId, client))
				client.getSession().send(unscriptedNpc(npcId));
		} catch (IOException ex) {
			LOG.log(Level.WARNING, "Error executing NPC script " + npcId, ex);
		} finally {
			Context.exit();
		}
	}

	private void runQuestScript(int npcId, short questId, GameClient client, String scriptName) {
		Context cx = Context.enter();
		QuestConversationActions convoMan = null;
		try {
			FileReader reader = new FileReader(questPath + scriptName + ".js");
			Scriptable globalScope = cx.initStandardObjects();
			cx.setOptimizationLevel(-1); // must use interpreter mode
			Script script = cx.compileReader(reader, scriptName, 1, null);
			reader.close();
			convoMan = new QuestConversationActions(npcId, questId, client, globalScope);
			globalScope.put("npc", globalScope, convoMan);
			client.setNpc(convoMan);
			cx.executeScriptWithContinuations(script, globalScope);
			convoMan.endConversation();
		} catch (ContinuationPending pending) {
			convoMan.setContinuation(pending.getContinuation());
		} catch (FileNotFoundException ex) {
			client.getSession().send(unscriptedQuest(npcId, scriptName));
		} catch (IOException ex) {
			LOG.log(Level.WARNING, "Error executing quest script " + scriptName, ex);
		} finally {
			Context.exit();
		}
	}

	public void runStartQuestScript(int npcId, short questId, GameClient client) {
		String scriptName = QuestDataLoader.getInstance().getStartScriptName(questId);
		if (scriptName == null)
			scriptName = "q" + questId + "s";
		runQuestScript(npcId, questId, client, scriptName);
	}

	public void runCompleteQuestScript(int npcId, short questId, GameClient client) {
		String scriptName = QuestDataLoader.getInstance().getEndScriptName(questId);
		if (scriptName == null)
			scriptName = "q" + questId + "e";
		runQuestScript(npcId, questId, client, scriptName);
	}

	private static byte[] unscriptedNpc(int npc) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter();

		lew.writeShort(ClientSendOps.NPC_TALK);
		lew.writeByte((byte) 4); //4 is for NPC conversation actions I guess...
		lew.writeInt(npc);
		lew.writeByte((byte) 0); //SAY (ok box)
		lew.writeLengthPrefixedString("I have not been scripted yet. Please tell your server administrator about NPC #" + npc + "!");
		lew.writeBool(false); //prev button
		lew.writeBool(false); //next button

		return lew.getBytes();
	}

	private static byte[] unscriptedQuest(int npc, String scriptName) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter();

		lew.writeShort(ClientSendOps.NPC_TALK);
		lew.writeByte((byte) 4); //4 is for NPC conversation actions I guess...
		lew.writeInt(npc);
		lew.writeByte((byte) 0); //SAY (ok box)
		lew.writeLengthPrefixedString("I have not been scripted yet. Please tell your server administrator about quest " + scriptName + "!");
		lew.writeBool(false); //prev button
		lew.writeBool(false); //next button

		return lew.getBytes();
	}

	public static void setInstance(String scriptPath) {
		if (singleton == null)
			singleton = new NpcScriptManager(scriptPath);
	}

	public static NpcScriptManager getInstance() {
		return singleton;
	}
}
