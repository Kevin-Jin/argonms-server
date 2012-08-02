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

import argonms.common.GlobalConstants;
import argonms.game.field.entity.PlayerNpc;
import argonms.game.net.external.GameClient;
import argonms.game.net.external.GamePackets;
import org.mozilla.javascript.Scriptable;

/**
 *
 * @author GoldenKevin
 */
public class ScriptPlayerNpc extends ScriptNpc {
	private PlayerNpc npc;

	public ScriptPlayerNpc(PlayerNpc npc, GameClient client, Scriptable globalScope) {
		super(npc.getDataId(), client, globalScope);
		this.npc = npc;
	}

	public String getNpcName() {
		return npc.getPlayerName();
	}

	public short getNpcLevel() {
		return GlobalConstants.MAX_LEVEL;
	}

	public void refreshAppearance() {
		npc.refreshAppearance(getClient().getPlayer());
		getClient().getPlayer().getMap().sendToAll(GamePackets.writePlayerNpcLook(npc));
	}
}
