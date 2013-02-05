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

import argonms.common.net.external.ClientSendOps;
import argonms.common.util.output.LittleEndianByteArrayWriter;
import argonms.game.character.MapMemoryVariable;
import argonms.game.net.external.GameClient;
import org.mozilla.javascript.Scriptable;

/**
 *
 * @author GoldenKevin
 */
public class ScriptPortal extends PlayerScriptInteraction {
	private byte portalId;
	private boolean warped;

	public ScriptPortal(byte portalId, GameClient gameClient, Scriptable globalScope) {
		super(gameClient, globalScope);
		this.portalId = portalId;
		this.warped = true;
	}

	@Override
	public void rememberMap(String variable) {
		getClient().getPlayer().rememberMap(MapMemoryVariable.valueOf(variable), portalId);
	}

	public void showHint(String hint, short width, short height) {
		getClient().getSession().send(writeHintBalloon(hint, width, height));
	}

	public void playSoundEffect() {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(3);

		lew.writeShort(ClientSendOps.FIRST_PERSON_VISUAL_EFFECT);
		lew.writeByte((byte) 7);

		getClient().getSession().send(lew.getBytes());
	}

	public void block() {
		getClient().getSession().send(writePortalBlocked((byte) 0x01));
		warped = false;
	}

	public void abortWarp() {
		warped = false;
	}

	public boolean warped() {
		return warped;
	}

	private static byte[] writeHintBalloon(String message, short width, short height) {
		if (width < 1)
			width = (short) Math.max(message.length() * 10, 40);
		if (height < 5)
			height = 5;

		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(9
				+ message.length());
		lew.writeShort(ClientSendOps.PLAYER_HINT);
		lew.writeLengthPrefixedString(message);
		lew.writeShort(width);
		lew.writeShort(height);
		lew.writeBool(true);

		return lew.getBytes();
	}

	private static byte[] writePortalBlocked(byte message) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(3);
		lew.writeShort(ClientSendOps.BLOCK_PORTAL);
		lew.writeByte(message);
		return lew.getBytes();
	}
}
