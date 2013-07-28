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

import argonms.common.net.external.CommonPackets;
import argonms.common.util.collections.Pair;
import argonms.game.GameServer;
import argonms.game.character.MapMemoryVariable;
import argonms.game.net.external.GameClient;
import argonms.game.net.external.handler.ChatHandler;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

/**
 *
 * @author GoldenKevin
 */
public abstract class PlayerScriptInteraction {
	protected final Scriptable globalScope;
	private GameClient client;

	public PlayerScriptInteraction(GameClient c, Scriptable globalScope) {
		this.globalScope = globalScope;
		this.client = c;
	}

	protected void dissociateClient() {
		client = null;
	}

	protected GameClient getClient() {
		return client;
	}

	public Object getEvent(String script) {
		ScriptEvent event = GameServer.getChannel(client.getChannel()).getEventManager().getRunningScript(script);
		return Context.javaToJS(event, globalScope);
	}

	public Object makeEvent(String script, boolean onlyOne, Object attachment) {
		return Context.javaToJS(GameServer.getChannel(client.getChannel()).getEventManager().runScript(script, onlyOne, attachment), globalScope);
	}

	public void sayInChat(String message) {
		client.getSession().send(CommonPackets.writeServerMessage(ChatHandler.TextStyle.LIGHT_BLUE_TEXT_CLEAR_BG.byteValue(), message, (byte) -1, true));
	}

	public void sayErrorInChat(String message) {
		client.getSession().send(CommonPackets.writeServerMessage(ChatHandler.TextStyle.RED_TEXT_CLEAR_BG.byteValue(), message, (byte) -1, true));
	}

	public void rememberMap(String variable) {
		client.getPlayer().rememberMap(MapMemoryVariable.valueOf(variable));
	}

	public Object getRememberedMap(String variable) {
		Pair<Integer, Byte> location = client.getPlayer().getRememberedMap(MapMemoryVariable.valueOf(variable));
		Context cx = Context.enter();
		try {
			return cx.newArray(globalScope, new Object[] { location.left, location.right });
		} finally {
			Context.exit();
		}
	}

	public Object resetRememberedMap(String variable) {
		Pair<Integer, Byte> location = client.getPlayer().resetRememberedMap(MapMemoryVariable.valueOf(variable));
		Context cx = Context.enter();
		try {
			return cx.newArray(globalScope, new Object[] { location.left, location.right });
		} finally {
			Context.exit();
		}
	}
}
