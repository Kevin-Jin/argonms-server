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

package argonms.game.script.binding;

import argonms.common.character.inventory.Inventory;
import argonms.common.character.inventory.InventorySlot;
import argonms.common.character.inventory.InventoryTools;
import argonms.common.net.external.ClientSendOps;
import argonms.common.util.input.LittleEndianReader;
import argonms.common.util.output.LittleEndianByteArrayWriter;
import argonms.common.util.output.LittleEndianWriter;
import argonms.game.loading.beauty.BeautyDataLoader;
import argonms.game.loading.npc.NpcDataLoader;
import argonms.game.loading.npc.NpcStorageKeeper;
import argonms.game.loading.shop.NpcShop;
import argonms.game.loading.shop.NpcShopDataLoader;
import argonms.game.net.external.GameClient;
import argonms.game.net.external.GamePackets;
import argonms.game.net.external.handler.GuildListHandler;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContinuationPending;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.Scriptable;

/**
 *
 * @author GoldenKevin
 */
public class ScriptNpc extends PlayerScriptInteraction {
	/* package-private */ static class ScriptInterruptedException extends RuntimeException {
		private static final long serialVersionUID = -1302552528821402944L;

		private ScriptInterruptedException(int npc, String player) {
			super("NPC " + npc + " conversation with player " + player + " was interrupted");
		}
	}

	private static final Logger LOG = Logger.getLogger(ScriptNpc.class.getName());

	private final int npcId;
	private final MessageSequenceCache sequence;
	private final AtomicBoolean terminated;
	private volatile boolean endingChat;
	private volatile Object continuation;

	public ScriptNpc(int npcId, GameClient client, Scriptable globalScope) {
		super(client, globalScope);
		this.npcId = npcId;
		this.sequence = new MessageSequenceCache();
		this.terminated = new AtomicBoolean(false);
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
		ASK_PET = 0x09,
		ASK_ACCEPT = 0x0C,
		ASK_ACCEPT_NO_ESC = 0x0D,
		ASK_BOX_TEXT = 0x0E
	;

	public void setContinuation(Object continuation) {
		this.continuation = continuation;
	}

	//I guess some scripts would want to put some consecutive sayNexts or a say
	//without back buttons, so this public method is accessible to them
	public void clearBackButton() {
		sequence.clear();
	}

	public void say(String message) {
		if (terminated.get())
			throw new ScriptInterruptedException(npcId, getClient().getPlayer().getName());
		if (!sequence.isEmpty()) //preceded by sayNext, or say after sayNext
			if (!sequence.showNext()) //not preceded by sayNext
				clearBackButton(); //cannot go backwards
			else //preceded by sayNext
				sequence.add(message, false); //save in sequence in case chat goes backwards and then returns forwards
		getClient().getSession().send(writeNpcSay(npcId, message, sequence.hasBack(), false));
		Context cx = Context.enter();
		try {
			throw cx.captureContinuation();
		} finally {
			Context.exit();
		}
	}

	public void sayNext(String message) {
		if (terminated.get())
			throw new ScriptInterruptedException(npcId, getClient().getPlayer().getName());
		if (!sequence.isEmpty()) //preceded by sayNext, or say after sayNext
			if (!sequence.showNext()) //preceded by say
				clearBackButton(); //cannot go backwards
		sequence.add(message, true); //save in sequence in case chat goes backwards
		getClient().getSession().send(writeNpcSay(npcId, message, sequence.hasBack(), true));
		Context cx = Context.enter();
		try {
			throw cx.captureContinuation();
		} finally {
			Context.exit();
		}
	}

	public byte askYesNo(String message) {
		if (terminated.get())
			throw new ScriptInterruptedException(npcId, getClient().getPlayer().getName());
		clearBackButton(); //cannot go backwards
		getClient().getSession().send(writeNpcSimple(npcId, message, ASK_YES_NO));
		Context cx = Context.enter();
		try {
			throw cx.captureContinuation();
		} finally {
			Context.exit();
		}
	}

	public byte askAccept(String message) {
		if (terminated.get())
			throw new ScriptInterruptedException(npcId, getClient().getPlayer().getName());
		clearBackButton(); //cannot go backwards
		getClient().getSession().send(writeNpcSimple(npcId, message, ASK_ACCEPT));
		Context cx = Context.enter();
		try {
			throw cx.captureContinuation();
		} finally {
			Context.exit();
		}
	}

	public byte askAcceptNoESC(String message) {
		if (terminated.get())
			throw new ScriptInterruptedException(npcId, getClient().getPlayer().getName());
		clearBackButton(); //cannot go backwards
		getClient().getSession().send(writeNpcSimple(npcId, message, ASK_ACCEPT_NO_ESC));
		Context cx = Context.enter();
		try {
			throw cx.captureContinuation();
		} finally {
			Context.exit();
		}
	}

	public String askQuiz(byte type, int objectId, int correct, int questions, int time) {
		if (terminated.get())
			throw new ScriptInterruptedException(npcId, getClient().getPlayer().getName());
		clearBackButton(); //cannot go backwards
		getClient().getSession().send(writeNpcQuiz(npcId, type, objectId, correct, questions, time));
		Context cx = Context.enter();
		try {
			throw cx.captureContinuation();
		} finally {
			Context.exit();
		}
	}

	public String askQuizQuestion(String title, String problem,
			String hint, int min, int max, int timeLimit) {
		if (terminated.get())
			throw new ScriptInterruptedException(npcId, getClient().getPlayer().getName());
		clearBackButton(); //cannot go backwards
		getClient().getSession().send(writeNpcQuizQuestion(npcId,
				title, problem, hint, min, max, timeLimit));
		Context cx = Context.enter();
		try {
			throw cx.captureContinuation();
		} finally {
			Context.exit();
		}
	}

	public String askText(String message, String def, short min, short max) {
		if (terminated.get())
			throw new ScriptInterruptedException(npcId, getClient().getPlayer().getName());
		clearBackButton(); //cannot go backwards
		getClient().getSession().send(writeNpcAskText(npcId, message, def, min, max));
		Context cx = Context.enter();
		try {
			throw cx.captureContinuation();
		} finally {
			Context.exit();
		}
	}

	public String askBoxText(String def, short columns, short rows) {
		if (terminated.get())
			throw new ScriptInterruptedException(npcId, getClient().getPlayer().getName());
		clearBackButton(); //cannot go backwards
		getClient().getSession().send(writeNpcBoxText(npcId, def, columns, rows));
		Context cx = Context.enter();
		try {
			throw cx.captureContinuation();
		} finally {
			Context.exit();
		}
	}

	public int askNumber(String message, int def, int min, int max) {
		if (terminated.get())
			throw new ScriptInterruptedException(npcId, getClient().getPlayer().getName());
		clearBackButton(); //cannot go backwards
		getClient().getSession().send(writeNpcAskNumber(npcId, message, def, min, max));
		Context cx = Context.enter();
		try {
			throw cx.captureContinuation();
		} finally {
			Context.exit();
		}
	}

	public int askMenu(String message) {
		if (terminated.get())
			throw new ScriptInterruptedException(npcId, getClient().getPlayer().getName());
		clearBackButton(); //cannot go backwards
		getClient().getSession().send(writeNpcSimple(npcId, message, ASK_MENU));
		Context cx = Context.enter();
		try {
			throw cx.captureContinuation();
		} finally {
			Context.exit();
		}
	}

	public int askAvatar(String message, int... styles) {
		if (terminated.get())
			throw new ScriptInterruptedException(npcId, getClient().getPlayer().getName());
		clearBackButton(); //cannot go backwards
		getClient().getSession().send(writeNpcAskAvatar(npcId, message, styles));
		Context cx = Context.enter();
		try {
			throw cx.captureContinuation();
		} finally {
			Context.exit();
		}
	}

	public long askDoll(String message) {
		if (terminated.get())
			throw new ScriptInterruptedException(npcId, getClient().getPlayer().getName());
		clearBackButton(); //cannot go backwards

		List<Long> expiredPets = new ArrayList<Long>();
		for (InventorySlot item : getClient().getPlayer().getInventory(Inventory.InventoryType.CASH).getAll().values())
			if (InventoryTools.isPet(item.getDataId()) && item.getExpiration() < System.currentTimeMillis())
				expiredPets.add(Long.valueOf(item.getUniqueId()));

		getClient().getSession().send(writeNpcAskPet(npcId, message, expiredPets));
		Context cx = Context.enter();
		try {
			throw cx.captureContinuation();
		} finally {
			Context.exit();
		}
	}

	public boolean sendShop(int shopId) {
		if (terminated.get())
			throw new ScriptInterruptedException(npcId, getClient().getPlayer().getName());
		NpcShop shop = NpcShopDataLoader.getInstance().getShopByNpc(shopId);
		if (shop != null) {
			endConversation();
			getClient().setNpcRoom(shop);
			getClient().getSession().send(GamePackets.writeNpcShop(getClient().getPlayer(), shopId, shop));
			return true;
		}
		return false;
	}

	public boolean sendStorage(int npcId) {
		if (terminated.get())
			throw new ScriptInterruptedException(npcId, getClient().getPlayer().getName());
		NpcStorageKeeper storage = NpcDataLoader.getInstance().getStorageById(npcId);
		if (storage != null) {
			endConversation();
			getClient().setNpcRoom(storage);
			getClient().getSession().send(GamePackets.writeNpcStorage(npcId, getClient().getPlayer().getStorageInventory()));
			return true;
		}
		return false;
	}

	public void askGuildName() {
		if (terminated.get())
			throw new ScriptInterruptedException(npcId, getClient().getPlayer().getName());
		clearBackButton(); //cannot go backwards
		getClient().getSession().send(GamePackets.writeSimpleGuildListMessage(GuildListHandler.ASK_NAME));
		Context cx = Context.enter();
		try {
			throw cx.captureContinuation();
		} finally {
			Context.exit();
		}
	}

	public void askGuildEmblem() {
		if (terminated.get())
			throw new ScriptInterruptedException(npcId, getClient().getPlayer().getName());
		clearBackButton(); //cannot go backwards
		getClient().getSession().send(GamePackets.writeSimpleGuildListMessage(GuildListHandler.ASK_EMBLEM));
		Context cx = Context.enter();
		try {
			throw cx.captureContinuation();
		} finally {
			Context.exit();
		}
	}

	private void fireEndChatEvent() {
		if (terminated.get())
			return;
		if (!endingChat) {
			endingChat = true;
			clearBackButton(); //we probably don't want a back button in the end chat hook...
			Object f = globalScope.get("chatEnded", globalScope);
			if (f != Scriptable.NOT_FOUND) {
				Context cx = Context.enter();
				try {
					cx.callFunctionWithContinuations((Function) f, globalScope, new Object[] { });
					endConversation();
				} catch (ContinuationPending pending) {
					setContinuation(pending.getContinuation());
				} finally {
					Context.exit();
				}
			} else {
				endConversation();
			}
		} else {
			endConversation();
		}
	}

	private void resume(Object obj) {
		if (!terminated.get()) {
			Context cx = Context.enter();
			try {
				cx.resumeContinuation(continuation, globalScope, obj);
				endConversation();
			} catch (ContinuationPending pending) {
				setContinuation(pending.getContinuation());
			} finally {
				Context.exit();
			}
		}
	}

	protected void responseReceived(LittleEndianReader packet) {
		byte type = packet.readByte();
		byte action = packet.readByte();
		switch (type) {
			case SAY:
				switch (action) {
					case -1: //end chat (or esc key)
						fireEndChatEvent();
						break;
					case 0: //back
						//if we clicked back on the message after this, then we
						//must have a next button on this message
						getClient().getSession().send(writeNpcSay(npcId, sequence.goBackwardAndGet(), sequence.hasBack(), true));
						break;
					case 1: //ok/next
						if (sequence.hasNext())
							getClient().getSession().send(writeNpcSay(npcId, sequence.goForwardAndGet(), sequence.hasBack(), sequence.showNext()));
						else
							resume(null);
						break;
				}
				break;
			case ASK_YES_NO:
				switch (action) {
					case -1: //end chat (or esc key)
						fireEndChatEvent();
						break;
					case 0: //no
					case 1: //yes
						resume(Byte.valueOf(action));
						break;
				}
				break;
			case ASK_TEXT:
				switch (action) {
					case 0: //end chat (or esc key)
						fireEndChatEvent();
						break;
					case 1: //ok
						resume(packet.readLengthPrefixedString());
						break;
				}
				break;
			case ASK_BOX_TEXT:
				switch (action) {
					case 0: //end chat (or esc key)
						fireEndChatEvent();
						break;
					case 1:
						resume(packet.readLengthPrefixedString());
						break;
				}
				break;
			case ASK_NUMBER:
				switch (action) {
					case 0: //end chat (or esc key)
						fireEndChatEvent();
						break;
					case 1: //ok
						resume(Integer.valueOf(packet.readInt()));
						break;
				}
				break;
			case ASK_MENU:
				switch (action) {
					case 0: //end chat (or esc key)
						fireEndChatEvent();
						break;
					case 1: //selected a link
						resume(Integer.valueOf(packet.readInt()));
						break;
				}
				break;
			case ASK_AVATAR:
				switch (action) {
					case 0: //leave store (or esc key) or cancel
						fireEndChatEvent();
						break;
					case 1: //ok
						resume(Byte.valueOf(packet.readByte()));
						break;
				}
				break;
			case ASK_PET:
				switch (action) {
					case 0: //leave store (or esc key) or cancel
						fireEndChatEvent();
						break;
					case 1: //ok
						resume(Long.valueOf(packet.readLong()));
						break;
				}
				break;
			case ASK_ACCEPT:
				switch (action) {
					case -1: //end chat (or esc key)
						fireEndChatEvent();
						break;
					case 0: //decline
					case 1: //accept
						resume(Byte.valueOf(action));
						break;
				}
				break;
			case ASK_ACCEPT_NO_ESC:
				switch (action) {
					case 0: //decline
					case 1: //accept
						resume(Byte.valueOf(action));
						break;
				}
				break;
			default:
				LOG.log(Level.INFO, "Did not process NPC type {0}:\n{1}",
						new Object[] { type, packet });
				break;
		}
	}

	protected void guildNameReceived(String name) {
		resume(name);
	}

	protected void guildEmblemReceived(short background, byte backgroundColor, short design, byte designColor) {
		Scriptable array;
		Context cx = Context.enter();
		try {
			array = cx.newArray(globalScope, new Object[] { Short.valueOf(background), Byte.valueOf(backgroundColor), Short.valueOf(design), Byte.valueOf(designColor) });
		} finally {
			Context.exit();
		}
		resume(array);
	}

	public int getNpcId() {
		return npcId;
	}

	public Object getAllSkinColors() {
		return Context.javaToJS(new NativeArray(new Byte[] { 0, 1, 2, 3, 4 }), globalScope);
	}

	public Object getAllEyeStyles() {
		//this method's pretty expensive with all the copying, but it should
		//only be called by KIN in the GM map, so it's not going to be used often
		Set<Short> faces = null;
		if (getClient().getPlayer().getGender() == 0)
			faces = BeautyDataLoader.getInstance().getMaleFaces();
		else if (getClient().getPlayer().getGender() == 1)
			faces = BeautyDataLoader.getInstance().getFemaleFaces();

		short currentEyes = getClient().getPlayer().getEyes();
		short color = (short) (currentEyes % 1000 - (currentEyes % 100));
		//we need a Set first so we don't have duplicate styles when getting rid
		//of the color digit. Needs to be LinkedHashSet so iteration order is
		//insertion order, which is natural order because faces is a SortedSet's
		//head/tail set.
		Set<Short> styles = new LinkedHashSet<Short>();
		for (Short s : faces) {
			short style = (short) (s.shortValue() - s.shortValue() % 1000 + s.shortValue() % 100);
			Short eyes = Short.valueOf((short) (style + color));
			//some eyes don't allow certain colors, and will crash the client if
			//you try to force one - check if our current eye color is allowed
			if (faces.contains(eyes))
				styles.add(eyes);
			else
				styles.add(Short.valueOf(style));
		}

		return Context.javaToJS(new NativeArray(styles.toArray(new Short[styles.size()])), globalScope);
	}

	public boolean isFaceValid(short face) {
		if (getClient().getPlayer().getGender() == 0)
			BeautyDataLoader.getInstance().getMaleFaces().contains(Short.valueOf(face));
		else if (getClient().getPlayer().getGender() == 1)
			BeautyDataLoader.getInstance().getFemaleFaces().contains(Short.valueOf(face));
		return false;
	}

	public Object getAllEyeColors() {
		Set<Short> faces = null;
		if (getClient().getPlayer().getGender() == 0)
			faces = BeautyDataLoader.getInstance().getMaleFaces();
		else if (getClient().getPlayer().getGender() == 1)
			faces = BeautyDataLoader.getInstance().getFemaleFaces();

		short currentEyes = getClient().getPlayer().getEyes();
		short style = (short) (currentEyes - currentEyes % 1000 + currentEyes % 100);
		List<Short> colors = new ArrayList<Short>();
		for (short i = 0; i < 10; i++) {
			Short eyes = Short.valueOf((short) (style + i * 100));
			//some eyes don't allow certain colors, and will crash the client if
			//you try to force one
			if (faces.contains(eyes))
				colors.add(eyes);
		}

		return Context.javaToJS(new NativeArray(colors.toArray(new Short[colors.size()])), globalScope);
	}

	public Object getAllHairStyles() {
		//this method's pretty expensive with all the copying, but it should
		//only be called by KIN in the GM map, so it's not going to be used often
		Set<Short> hairs = null;
		if (getClient().getPlayer().getGender() == 0)
			hairs = BeautyDataLoader.getInstance().getMaleHairs();
		else if (getClient().getPlayer().getGender() == 1)
			hairs = BeautyDataLoader.getInstance().getFemaleHairs();

		short currentHair = getClient().getPlayer().getHair();
		short color = (short) (currentHair % 10);
		//we need a Set first so we don't have duplicate styles when getting rid
		//of the color digit. Needs to be LinkedHashSet so iteration order is
		//insertion order, which is natural order because hairs is a SortedSet's
		//head/tail set.
		Set<Short> styles = new LinkedHashSet<Short>();
		for (Short s : hairs) {
			short style = (short) (s.shortValue() - s.shortValue() % 10);
			Short hair = Short.valueOf((short) (style + color));
			//some hairs don't allow certain colors, and will crash the client
			//if you try to force one - check if our current eye color is allowed
			if (hairs.contains(hair))
				styles.add(hair);
			else
				styles.add(Short.valueOf(style));
		}

		return Context.javaToJS(new NativeArray(styles.toArray(new Short[styles.size()])), globalScope);
	}

	public boolean isHairValid(short hair) {
		if (getClient().getPlayer().getGender() == 0)
			BeautyDataLoader.getInstance().getMaleHairs().contains(Short.valueOf(hair));
		else if (getClient().getPlayer().getGender() == 1)
			BeautyDataLoader.getInstance().getFemaleHairs().contains(Short.valueOf(hair));
		return false;
	}

	public Object getAllHairColors() {
		Set<Short> hairs = null;
		if (getClient().getPlayer().getGender() == 0)
			hairs = BeautyDataLoader.getInstance().getMaleHairs();
		else if (getClient().getPlayer().getGender() == 1)
			hairs = BeautyDataLoader.getInstance().getFemaleHairs();

		short currentHair = getClient().getPlayer().getHair();
		short style = (short) (currentHair - currentHair % 10);
		List<Short> colors = new ArrayList<Short>();
		for (short i = 0; i < 10; i++) {
			Short hair = Short.valueOf((short) (style + i));
			//some hairs don't allow certain colors, and will crash the client
			//if you try to force one
			if (hairs.contains(hair))
				colors.add(hair);
		}

		return Context.javaToJS(new NativeArray(colors.toArray(new Short[colors.size()])), globalScope);
	}

	public void endConversation() {
		if (terminated.compareAndSet(false, true)) {
			getClient().setNpc(null);
			//prevent memory leak of clients in case script is still running -
			//we cannot interrupt it
			dissociateClient();
		}
	}

	private static void writeCommonNpcAction(LittleEndianWriter lew, int npcId, byte type, String msg) {
		lew.writeShort(ClientSendOps.NPC_TALK);
		lew.writeByte((byte) 4); //4 is for NPC conversation actions I guess...
		lew.writeInt(npcId);
		lew.writeByte(type);
		lew.writeLengthPrefixedString(msg);
	}

	private static byte[] writeNpcSimple(int npcId, String msg, byte type) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(10
				+ msg.length());
		writeCommonNpcAction(lew, npcId, type, msg);
		return lew.getBytes();
	}

	private static byte[] writeNpcSay(int npcId, String msg, boolean back, boolean next) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(12
				+ msg.length());
		writeCommonNpcAction(lew, npcId, SAY, msg);
		lew.writeBool(back);
		lew.writeBool(next);
		return lew.getBytes();
	}

	private static byte[] writeNpcAskText(int npcId, String msg, String def, short min, short max) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(16
				+ msg.length() + def.length());
		writeCommonNpcAction(lew, npcId, ASK_TEXT, msg);
		lew.writeLengthPrefixedString(def);
		lew.writeShort(min);
		lew.writeShort(max); //some short that seems to have no purpose?
		return lew.getBytes();
	}

	private static byte[] writeNpcBoxText(int npcId, String def, short cols, short rows) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(16
				+ def.length());
		writeCommonNpcAction(lew, npcId, ASK_BOX_TEXT, "");
		lew.writeLengthPrefixedString(def);
		lew.writeShort(cols);
		lew.writeShort(rows);
		return lew.getBytes();
	}

	private static byte[] writeNpcAskNumber(int npcId, String msg, int def, int min, int max) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(26
				+ msg.length());
		writeCommonNpcAction(lew, npcId, ASK_NUMBER, msg);
		lew.writeInt(def);
		lew.writeInt(min);
		lew.writeInt(max);
		lew.writeInt(0); //some int that seems to have no purpose?
		return lew.getBytes();
	}

	private static byte[] writeNpcQuiz(int npcId, int type, int objectId, int correct, int questions, int time) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(29);
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
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(27
				+ msg.length() + problem.length() + hint.length());
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
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(11
				+ 4 * styles.length);
		writeCommonNpcAction(lew, npcId, ASK_AVATAR, msg);
		lew.writeByte((byte) styles.length);
		for (byte i = 0; i < styles.length; i++)
			lew.writeInt(styles[i]);
		return lew.getBytes();
	}

	private static byte[] writeNpcAskPet(int npcId, String msg, List<Long> uniqueIds) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(11);
		writeCommonNpcAction(lew, npcId, ASK_PET, msg);
		lew.writeByte((byte) uniqueIds.size());
		for (Long uniqueId : uniqueIds) {
			lew.writeLong(uniqueId.longValue());
			lew.writeByte((byte) 0);
		}
		return lew.getBytes();
	}

	private static class MessageSequenceCache {
		private Node first;
		private Node cursor;

		public void add(String data, boolean showNext) {
			Node insert = new Node(data, showNext);
			if (first == null) {
				first = insert;
			} else {
				//this method should only be called at the end of the cached
				//sequence (i.e. called after a script resume rather than a
				//sequence cache fetch), so cursor better be the tail node
				assert cursor.forward == null;
				cursor.forward = insert;
				insert.backward = cursor;
			}
			cursor = insert;
		}

		public void clear() {
			first = cursor = null;
		}

		public boolean isEmpty() {
			return (cursor == null);
		}

		public String goBackwardAndGet() {
			cursor = cursor.backward;
			return cursor.data;
		}

		public String goForwardAndGet() {
			cursor = cursor.forward;
			return cursor.data;
		}

		public boolean showNext() {
			return cursor.showNext;
		}

		public boolean hasBack() {
			//assert cursor.backward.showNext if this returns true
			return cursor != null && cursor.backward != null;
		}

		public boolean hasNext() {
			//assert cursor.showNext if this returns true
			return cursor != null && cursor.forward != null;
		}

		private static class Node {
			private Node backward;
			private String data;
			private boolean showNext;
			private Node forward;

			public Node(String data, boolean showNext) {
				this.data = data;
				this.showNext = showNext;
			}
		}
	}
}
