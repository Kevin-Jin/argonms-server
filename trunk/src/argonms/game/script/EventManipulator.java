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

package argonms.game.script;

import argonms.game.character.GameCharacter;
import argonms.game.field.entity.Mob;
import argonms.game.script.binding.ScriptField;
import argonms.game.script.binding.ScriptMob;
import argonms.game.script.binding.ScriptParty;
import argonms.game.script.binding.ScriptPlayer;
import java.util.Map;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;

/**
 * Interface was inspired by Vana's instance handling.
 * @author GoldenKevin
 */
public class EventManipulator {
	private final Scriptable globalScope;
	private Map<String, Object> variables;

	public EventManipulator(Scriptable globalScope) {
		this.globalScope = globalScope;
	}

	/*package-private*/ void setVariables(Map<String, Object> variables) {
		this.variables = variables;
	}

	public Object getVariable(String key) {
		return variables.get(key);
	}

	public void playerDied(GameCharacter p) {
		Object f = globalScope.get("playerDied", globalScope);
		if (f != Scriptable.NOT_FOUND) {
			Context cx = Context.enter();
			try {
				((Function) f).call(cx, globalScope, globalScope, new Object[] { Context.javaToJS(new ScriptPlayer(p), globalScope) });
			} finally {
				Context.exit();
			}
		}
	}

	public void playerDisconnected(GameCharacter p) {
		Object f = globalScope.get("playerDisconnected", globalScope);
		if (f != Scriptable.NOT_FOUND) {
			Context cx = Context.enter();
			try {
				((Function) f).call(cx, globalScope, globalScope, new Object[] { Context.javaToJS(new ScriptPlayer(p), globalScope) });
			} finally {
				Context.exit();
			}
		}
	}

	public void playerChangedMap(GameCharacter p) {
		Object f = globalScope.get("playerChangedMap", globalScope);
		if (f != Scriptable.NOT_FOUND) {
			Context cx = Context.enter();
			try {
				((Function) f).call(cx, globalScope, globalScope, new Object[] { Context.javaToJS(new ScriptPlayer(p), globalScope), Context.javaToJS(new ScriptField(p.getMap()), globalScope) });
			} finally {
				Context.exit();
			}
		}
	}

	public void timerExpired(String timerId) {
		Object f = globalScope.get("timerExpired", globalScope);
		if (f != Scriptable.NOT_FOUND) {
			Context cx = Context.enter();
			try {
				((Function) f).call(cx, globalScope, globalScope, new Object[] { timerId });
			} finally {
				Context.exit();
			}
		}
	}

	public void partyMemberDischarged(GameCharacter p) {
		Object f = globalScope.get("partyMemberDischarged", globalScope);
		if (f != Scriptable.NOT_FOUND) {
			Context cx = Context.enter();
			try {
				((Function) f).call(cx, globalScope, globalScope, new Object[] { Context.javaToJS(new ScriptParty(p.getClient().getChannel(), p.getParty(), globalScope), globalScope), Context.javaToJS(new ScriptPlayer(p), globalScope) });
			} finally {
				Context.exit();
			}
		}
	}

	public void mobDied(Mob m, int mapId) {
		Object f = globalScope.get("mobDied", globalScope);
		if (f != Scriptable.NOT_FOUND) {
			Context cx = Context.enter();
			try {
				((Function) f).call(cx, globalScope, globalScope, new Object[] { Context.javaToJS(new ScriptMob(m, mapId), globalScope) });
			} finally {
				Context.exit();
			}
		}
	}

	public void mobSpawned(Mob m, int mapId) {
		Object f = globalScope.get("mobSpawned", globalScope);
		if (f != Scriptable.NOT_FOUND) {
			Context cx = Context.enter();
			try {
				((Function) f).call(cx, globalScope, globalScope, new Object[] { Context.javaToJS(new ScriptMob(m, mapId), globalScope) });
			} finally {
				Context.exit();
			}
		}
	}

	public void friendlyMobHurt(Mob m, int mapId) {
		Object f = globalScope.get("friendlyMobHurt", globalScope);
		if (f != Scriptable.NOT_FOUND) {
			Context cx = Context.enter();
			try {
				((Function) f).call(cx, globalScope, globalScope, new Object[] { Context.javaToJS(new ScriptMob(m, mapId), globalScope) });
			} finally {
				Context.exit();
			}
		}
	}

	public void deinit() {
		Object f = globalScope.get("deinit", globalScope);
		if (f != Scriptable.NOT_FOUND) {
			Context cx = Context.enter();
			try {
				((Function) f).call(cx, globalScope, globalScope, new Object[] { });
			} finally {
				Context.exit();
			}
		}
	}
}
