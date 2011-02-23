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

import argonms.game.GameClient;
import argonms.net.client.ClientSendOps;
import argonms.tools.output.LittleEndianByteArrayWriter;
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

	public static void runScript(int npcId, GameClient client) {
		Context cx = Context.enter();
		NpcConversationActions convoMan = null;
		try {
			FileReader reader = new FileReader("scripts/npcs/" + npcId + ".js");
			Scriptable globalScope = cx.initStandardObjects();
			cx.setOptimizationLevel(-1); // must use interpreter mode
			convoMan = new NpcConversationActions(npcId, client, cx, globalScope);
			globalScope.put("npc", globalScope, convoMan);
			client.setNpc(convoMan);
			Script script = cx.compileReader(reader, Integer.toString(npcId), 1, null);
			reader.close();
			cx.executeScriptWithContinuations(script, globalScope);
			convoMan.endConversation(false);
		} catch (ContinuationPending pending) {
			convoMan.setContinuation(pending.getContinuation());
		} catch (FileNotFoundException ex) {
			client.getSession().send(unscriptedNpc(npcId));
		} catch (IOException ex) {
			LOG.log(Level.WARNING, "Error executing NPC script " + npcId, ex);
		} finally {
			Context.exit();
		}
	}

	public static void runQuestScript(int questId, GameClient client) {
		/*Context cx = Context.enter();
		NpcConversationActions convoMan = null;
		try {
			FileReader reader = new FileReader("scripts/quest/" + questId + ".js");
			Scriptable globalScope = cx.initStandardObjects();
			cx.setOptimizationLevel(-1); // must use interpreter mode
			convoMan = new NpcConversationActions(npcId, client, cx, globalScope);
			globalScope.put("npc", globalScope, convoMan);
			client.setNpc(convoMan);
			Script script = cx.compileReader(reader, Integer.toString(npcId), 1, null);
			reader.close();
			cx.executeScriptWithContinuations(script, globalScope);
			convoMan.endConversation(false);
		} catch (ContinuationPending pending) {
			convoMan.setContinuation(pending.getContinuation());
		} catch (FileNotFoundException ex) {
			client.getSession().send(unscriptedNpc(npcId));
		} catch (IOException ex) {
			LOG.log(Level.WARNING, "Error executing quest script " + questId, ex);
		} finally {
			Context.exit();
		}*/
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
}
