/*
 * ArgonMS MapleStory server emulator written in Java
 * Copyright (C) 2011  GoldenKevin
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
import argonms.common.loading.quest.QuestDataLoader;
import argonms.common.net.external.ClientSendOps;
import argonms.common.tools.output.LittleEndianByteArrayWriter;
import argonms.game.GameClient;
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

	public void runScript(int npcId, GameClient client) {
		Context cx = Context.enter();
		NpcConversationActions convoMan = null;
		try {
			FileReader reader = new FileReader(npcPath + npcId + ".js");
			Scriptable globalScope = cx.initStandardObjects();
			cx.setOptimizationLevel(-1); // must use interpreter mode
			convoMan = new NpcConversationActions(npcId, client, cx, globalScope);
			globalScope.put("npc", globalScope, convoMan);
			client.setNpc(convoMan);
			Script script = cx.compileReader(reader, "n" + npcId, 1, null);
			reader.close();
			cx.executeScriptWithContinuations(script, globalScope);
			convoMan.endConversation();
		} catch (ContinuationPending pending) {
			convoMan.setContinuation(pending.getContinuation());
		} catch (FileNotFoundException ex) {
			client.getSession().send(unscriptedNpc(npcId));
		} catch (IOException ex) {
			LOG.log(Level.WARNING, "Error executing NPC script " + npcId, ex);
			if (convoMan != null)
				convoMan.endConversation();
		} finally {
			Context.exit();
		}
	}

	//one script two methods routines for quests
	/*private void runQuestScript(int npcId, short questId, GameClient client, String fn) {
		Context cx = Context.enter();
		QuestConversationActions convoMan = null;
		try {
			FileReader reader = new FileReader(questPath + questId + ".js");
			Scriptable globalScope = cx.initStandardObjects();
			cx.setOptimizationLevel(-1); // must use interpreter mode
			convoMan = new QuestConversationActions(npcId, questId, client, cx, globalScope);
			globalScope.put("npc", globalScope, convoMan);
			client.setNpc(convoMan);
			cx.evaluateReader(globalScope, reader, "q" + questId + fn, 1, null);
			reader.close();
			Object f = globalScope.get(fn, globalScope);
			if (f != Scriptable.NOT_FOUND) {
				try {
					cx.callFunctionWithContinuations((Function) f, globalScope, new Object[] { });
					convoMan.endConversation();
				} catch (ContinuationPending pending) {
					convoMan.setContinuation(pending.getContinuation());
				}
			} else {
				client.getSession().send(unscriptedQuest(npcId, questId, fn));
				convoMan.endConversation();
			}
		} catch (FileNotFoundException ex) {
			client.getSession().send(unscriptedQuest(npcId, questId, fn));
		} catch (IOException ex) {
			LOG.log(Level.WARNING, "Error executing quest script " + questId, ex);
			if (convoMan != null)
				convoMan.endConversation();
		} finally {
			Context.exit();
		}
	}

	public void runStartQuestScript(int npcId, short questId, GameClient client) {
		runQuestScript(npcId, questId, client, "start");
	}

	public void runCompleteQuestScript(int npcId, short questId, GameClient client) {
		runQuestScript(npcId, questId, client, "end");
	}

	private static byte[] unscriptedQuest(int npc, short quest, String type) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter();

		lew.writeShort(ClientSendOps.NPC_TALK);
		lew.writeByte((byte) 4); //4 is for NPC conversation actions I guess...
		lew.writeInt(npc);
		lew.writeByte((byte) 0); //SAY (ok box)
		lew.writeLengthPrefixedString("I have not been scripted yet. Please tell your server administrator about quest #" + quest + " (" + type + ")!");
		lew.writeBool(false); //prev button
		lew.writeBool(false); //next button

		return lew.getBytes();
	}*/

	private void runQuestScript(int npcId, short questId, GameClient client, String scriptName) {
		Context cx = Context.enter();
		QuestConversationActions convoMan = null;
		try {
			FileReader reader = new FileReader(questPath + scriptName + ".js");
			Scriptable globalScope = cx.initStandardObjects();
			cx.setOptimizationLevel(-1); // must use interpreter mode
			convoMan = new QuestConversationActions(npcId, questId, client, cx, globalScope);
			globalScope.put("npc", globalScope, convoMan);
			client.setNpc(convoMan);
			Script script = cx.compileReader(reader, scriptName, 1, null);
			reader.close();
			cx.executeScriptWithContinuations(script, globalScope);
			convoMan.endConversation();
		} catch (ContinuationPending pending) {
			convoMan.setContinuation(pending.getContinuation());
		} catch (FileNotFoundException ex) {
			client.getSession().send(unscriptedQuest(npcId, scriptName));
		} catch (IOException ex) {
			LOG.log(Level.WARNING, "Error executing quest script " + scriptName, ex);
			if (convoMan != null)
				convoMan.endConversation();
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
