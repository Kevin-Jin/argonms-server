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
import argonms.game.character.PartyList;
import java.awt.Rectangle;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

/**
 *
 * @author GoldenKevin
 */
public class ScriptPartyMember extends ScriptPlayer {
	private final Scriptable globalScope;

	public ScriptPartyMember(PartyList.LocalMember member, Scriptable globalScope) {
		super(member.getPlayer());
		this.globalScope = globalScope;
	}

	public byte getChannel() {
		return getPlayer().getClient().getChannel();
	}

	public int getMapId() {
		return getPlayer().getMapId();
	}

	public void setEvent(ScriptEvent event) {
		getPlayer().setEvent(event == null ? null : GameServer.getChannel(getChannel()).getEventManager().getScriptInterface(event.getName()));
	}

	public boolean inRectangle(int x, int y, int width, int height) {
		return new Rectangle(x, y, width, height).contains(getPlayer().getPosition());
	}
}
