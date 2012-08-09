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

package argonms.game.script.binding;

import argonms.game.GameServer;
import argonms.game.character.MapMemoryVariable;
import argonms.game.net.external.GameClient;
import argonms.game.net.external.GamePackets;
import argonms.game.net.external.handler.GameChatHandler;

/**
 *
 * @author GoldenKevin
 */
public abstract class PlayerScriptInteraction {
	private GameClient client;

	public PlayerScriptInteraction(GameClient c) {
		this.client = c;
	}

	protected void dissociateClient() {
		client = null;
	}

	protected GameClient getClient() {
		return client;
	}

	//TODO: Context.toObject
	public ScriptEvent getEvent(String script) {
		return GameServer.getChannel(client.getChannel()).getEventManager().getRunningScript(script);
	}

	//TODO: Context.toObject
	public ScriptEvent makeEvent(String script, Object attachment) {
		return GameServer.getChannel(client.getChannel()).getEventManager().runScript(script, attachment);
	}

	public void sayInChat(String message) {
		client.getSession().send(GamePackets.writeServerMessage(GameChatHandler.TextStyle.LIGHT_BLUE_TEXT_CLEAR_BG.byteValue(), message, (byte) -1, true));
	}

	public void rememberMap(String variable) {
		client.getPlayer().rememberMap(MapMemoryVariable.valueOf(variable));
	}

	public int getRememberedMap(String variable) {
		return client.getPlayer().getRememberedMap(MapMemoryVariable.valueOf(variable));
	}

	public int resetRememberedMap(String variable) {
		return client.getPlayer().resetRememberedMap(MapMemoryVariable.valueOf(variable));
	}
}
