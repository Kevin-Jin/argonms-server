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

import argonms.game.net.external.GameClient;
import org.mozilla.javascript.Scriptable;

/**
 *
 * @author GoldenKevin
 */
public class ScriptQuest extends ScriptNpc {
	private final short questId;

	public ScriptQuest(int npcId, short questId, GameClient client, Scriptable globalScope) {
		super(npcId, client, globalScope);
		this.questId = questId;
	}

	public void startQuest() {
		getClient().getPlayer().startQuest(questId, getNpcId());
	}

	public void completeQuest() {
		getClient().getPlayer().completeQuest(questId, getNpcId(), -1);
	}
}
