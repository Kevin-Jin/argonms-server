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

package argonms.game.npcscript;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import argonms.game.GameClient;
import argonms.net.client.ClientSendOps;
import argonms.tools.output.LittleEndianByteArrayWriter;

/**
 *
 * @author GoldenKevin
 */
public class NpcScriptManager implements Runnable {
	private static final Logger LOG = Logger.getLogger(NpcScriptManager.class.getName());

	private GameClient client;
	private int npcId;

	public NpcScriptManager(GameClient client, int npc) {
		this.client = client;
		this.npcId = npc;
	}

	public void run() {
		ScriptEngineManager mgr = new ScriptEngineManager();
		ScriptEngine jsEngine = mgr.getEngineByName("JavaScript");
		NpcConversationActions convoActs = new NpcConversationActions(npcId, client);
		jsEngine.put("npc", convoActs);
		try {
			FileReader fr = new FileReader("scripts/" + npcId + ".js");
			client.setNpc(convoActs);
			jsEngine.eval(fr);
			fr.close();
		} catch (FileNotFoundException ex) {
			client.getSession().send(unscriptedNpc(npcId));
			return;
		} catch (ScriptException ex) {
			LOG.log(Level.INFO, "Error running NPC script " + npcId, ex);
		} catch (/*IO*/Exception ex) {
			LOG.log(Level.INFO, "Error while running NPC script " + npcId, ex);
		}
		convoActs.endConversation();
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
