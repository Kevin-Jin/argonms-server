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

import argonms.game.GameClient;
import argonms.net.client.ClientSendOps;
import argonms.tools.input.LittleEndianReader;
import argonms.tools.output.LittleEndianByteArrayWriter;
import argonms.tools.output.LittleEndianWriter;

import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;
import java.util.logging.Logger;

//I really need to write my own language and script parser.
//Rhino/JS doesn't work out well when using say/sayPrev/sayPrevNext/sayNext...
//By the way, NullPointerExceptions when pressing 'End Chat' is totally normal,
//until I think of a better way to implement this.
/**
 *
 * @author GoldenKevin
 */
public class NpcConversationActions {
	private static final Logger LOG = Logger.getLogger(NpcConversationActions.class.getName());

	private int npcId;
	private Object response;
	private CountDownLatch latch;
	private GameClient client;

	public NpcConversationActions(int npcId, GameClient client) {
		this.npcId = npcId;
		this.client = client;
	}

	private static final byte
		SAY = 0x00,
		ASK_YES_NO = 0x01,
		ASK_TEXT = 0x02,
		ASK_NUMBER = 0x03,
		ASK_MENU = 0x04,
		ASK_QUESTION = 0x05,
		ASK_QUIZ = 0x06,
		ASK_AVATAR = 0x07,
		ASK_ACCEPT = 0x0C,
		ASK_ACCEPT_NO_ESC = 0x0D
	;

	private void waitForResponse() {
		response = null;
		latch = new CountDownLatch(1);
		try {
			latch.await();
		} catch (InterruptedException e) {
			LOG.log(Level.INFO, "NPC conversation interrupted", e);
		}
	}

	//in Wvs, say checks if there are more things to say after the current
	//line. if there are, then there's a next, otherwise there's an ok. 
	//if there is a line before, there's also a previous button.
	public void say(String message) {
		client.getSession().send(writeNpcSay(npcId, message, false, false));
		waitForResponse();
	}

	public void sayPrev(String message) {
		client.getSession().send(writeNpcSay(npcId, message, true, false));
		waitForResponse();
	}

	public void sayNext(String message) {
		client.getSession().send(writeNpcSay(npcId, message, false, true));
		waitForResponse();
	}

	public void sayPrevNext(String message) {
		client.getSession().send(writeNpcSay(npcId, message, true, true));
		waitForResponse();
	}
	public byte askYesNo(String message) {
		client.getSession().send(writeNpcSimple(npcId, message, ASK_YES_NO));
		waitForResponse();
		return ((Byte) response).byteValue();
	}

	public byte askAccept(String message) {
		client.getSession().send(writeNpcSimple(npcId, message, ASK_ACCEPT));
		waitForResponse();
		return ((Byte) response).byteValue();
	}

	public byte askAcceptNoESC(String message) {
		client.getSession().send(writeNpcSimple(npcId, message, ASK_ACCEPT_NO_ESC));
		waitForResponse();
		return ((Byte) response).byteValue();
	}

	public String askQuiz(byte type, int objectId, int correct, int questions, int time) {
		client.getSession().send(writeNpcQuiz(npcId, type, objectId, correct, questions, time));
		waitForResponse();
		return (String) response;
	}

	public String askQuizQuestion(String title, String problem,
			String hint, int min, int max, int timeLimit) {
		client.getSession().send(writeNpcQuizQuestion(npcId,
				title, problem, hint, min, max, timeLimit));
		waitForResponse();
		return (String) response;
	}

	public String askText(String message, String def, short min, short max) {
		client.getSession().send(writeNpcAskText(npcId, message, def, min, max));
		waitForResponse();
		return (String) response;
	}

	public String askBoxText(String message, String def, int col, int line) {
		waitForResponse();
		return (String) response;
	}

	public int askNumber(String message, int def, int min, int max) {
		client.getSession().send(writeNpcAskNumber(npcId, message, def, min, max));
		waitForResponse();
		return ((Integer) response).intValue();
	}

	public int askMenu(String message) {
		client.getSession().send(writeNpcSimple(npcId, message, ASK_MENU));
		waitForResponse();
		return ((Integer) response).intValue();
	}

	public int askAvatar(String message, int... styles) {
		client.getSession().send(writeNpcAskAvatar(npcId, message, styles));
		waitForResponse();
		return ((Integer) response).intValue();
	}

	public void responseReceived(LittleEndianReader packet) {
		byte type = packet.readByte();
		byte action = packet.readByte();
		switch (type) {
			case SAY:
				switch (action) {
					case -1: //end chat (or esc key)
						break;
					case 0: //prev
						break;
					case 1: //ok/next
						break;
				}
				break;
			case ASK_YES_NO:
				switch (action) {
					case -1: //end chat (or esc key)
						break;
					case 0: //no
					case 1: //yes
						this.response = Byte.valueOf(action);
						break;
				}
				break;
			case ASK_TEXT:
				switch (action) {
					case 0: //end chat (or esc key)
						break;
					case 1: //ok
						this.response = packet.readLengthPrefixedString();
						break;
				}
				break;
			case ASK_NUMBER:
				switch (action) {
					case 0: //end chat (or esc key)
						break;
					case 1: //ok
						this.response = Integer.valueOf(packet.readInt());
						break;
				}
				break;
			case ASK_MENU:
				switch (action) {
					case 0: //end chat (or esc key)
						break;
					case 1: //selected a link
						this.response = Integer.valueOf(packet.readInt());
						break;
				}
				break;
			case ASK_AVATAR:
				System.out.println(packet);
				break;
			case ASK_ACCEPT:
				switch (action) {
					case -1: //end chat (or esc key)
						break;
					case 0: //decline
					case 1: //accept
						this.response = Byte.valueOf(action);
						break;
				}
				break;
			case ASK_ACCEPT_NO_ESC:
				switch (action) {
					case 0: //decline
					case 1: //accept
						this.response = Byte.valueOf(action);
						break;
				}
				break;
			default:
				LOG.log(Level.INFO, "Did not process NPC type {0}:\n{1}",
						new Object[] { type, packet });
				break;
		}
		latch.countDown();
	}

	public void endConversation() {
		if (latch != null) { //interrupt any waitForResponses...
			while (latch.getCount() > 0)
				latch.countDown();
			latch = null;
		}
		client.setNpc(null);
	}

	private static void writeCommonNpcAction(LittleEndianWriter lew, int npcId, byte type, String msg) {
		lew.writeShort(ClientSendOps.NPC_TALK);
		lew.writeByte((byte) 4); //4 is for NPC conversation actions I guess...
		lew.writeInt(npcId);
		lew.writeByte(type);
		lew.writeLengthPrefixedString(msg);
	}

	private static byte[] writeNpcSimple(int npcId, String msg, byte type) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter();
		writeCommonNpcAction(lew, npcId, type, msg);
		return lew.getBytes();
	}

	private static byte[] writeNpcSay(int npcId, String msg, boolean prev, boolean next) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter();
		writeCommonNpcAction(lew, npcId, SAY, msg);
		lew.writeBool(prev);
		lew.writeBool(next);
		return lew.getBytes();
	}

	private static byte[] writeNpcAskText(int npcId, String msg, String def, short min, short max) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter();
		writeCommonNpcAction(lew, npcId, ASK_TEXT, msg);
		lew.writeLengthPrefixedString(def);
		lew.writeShort(min);
		lew.writeShort(max); //some short that seems to have no purpose?
		return lew.getBytes();
	}

	private static byte[] writeNpcAskNumber(int npcId, String msg, int def, int min, int max) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter();
		writeCommonNpcAction(lew, npcId, ASK_NUMBER, msg);
		lew.writeInt(def);
		lew.writeInt(min);
		lew.writeInt(max);
		lew.writeInt(0); //some int that seems to have no purpose?
		return lew.getBytes();
	}

	private static byte[] writeNpcQuiz(int npcId, int type, int objectId, int correct, int questions, int time) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter();
		lew.writeShort(ClientSendOps.NPC_TALK);
		lew.writeByte((byte) 4); //4 is for NPC conversation actions I guess...
		lew.writeInt(npcId);
		lew.writeByte(ASK_QUIZ);
		lew.writeBool(false);
		lew.writeInt(type); // 0 = NPC, 1 = Mob, 2 = Item
		lew.writeInt(objectId);
		lew.writeInt(correct);
		lew.writeInt(questions);
		lew.writeInt(time);
		return lew.getBytes();
	}

	private static byte[] writeNpcQuizQuestion(int npcId, String msg, String problem, String hint,
			int min, int max, int timeLimit) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter();
		lew.writeShort(ClientSendOps.NPC_TALK);
		lew.writeByte((byte) 4); //4 is for NPC conversation actions I guess...
		lew.writeInt(npcId);
		lew.writeByte(ASK_QUESTION);
		lew.writeBool(false);
		lew.writeLengthPrefixedString(msg);
		lew.writeLengthPrefixedString(problem);
		lew.writeLengthPrefixedString(hint);
		lew.writeInt(min);
		lew.writeInt(max);
		lew.writeInt(timeLimit);
		return lew.getBytes();
	}

	private static byte[] writeNpcAskAvatar(int npcId, String msg, int... styles) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter();
		writeCommonNpcAction(lew, npcId, ASK_AVATAR, msg);
		lew.writeByte((byte) styles.length);
		for (byte i = 0; i < styles.length; i++)
			lew.writeInt(styles[i]);
		return lew.getBytes();
	}
}
