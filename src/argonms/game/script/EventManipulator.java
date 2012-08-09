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

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;

/**
 * Interface was inspired by Vana's instance handling.
 * @author GoldenKevin
 */
public class EventManipulator {
	private final Scriptable globalScope;

	public EventManipulator(Scriptable globalScope) {
		this.globalScope = globalScope;
	}

	public void playerDied(int playerId) {
		Object f = globalScope.get("playerDied", globalScope);
		if (f != Scriptable.NOT_FOUND) {
			Context cx = Context.enter();
			try {
				((Function) f).call(cx, globalScope, globalScope, new Object[] { playerId });
			} finally {
				Context.exit();
			}
		}
	}

	public void playerDisconnected(int playerId) {
		Object f = globalScope.get("playerDisconnected", globalScope);
		if (f != Scriptable.NOT_FOUND) {
			Context cx = Context.enter();
			try {
				((Function) f).call(cx, globalScope, globalScope, new Object[] { playerId });
			} finally {
				Context.exit();
			}
		}
	}

	public void playerChangedMap(int playerId, int map) {
		Object f = globalScope.get("playerChangedMap", globalScope);
		if (f != Scriptable.NOT_FOUND) {
			Context cx = Context.enter();
			try {
				((Function) f).call(cx, globalScope, globalScope, new Object[] { playerId, map });
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

	public void partyDisbanded(int partyId) {
		Object f = globalScope.get("partyDisbanded", globalScope);
		if (f != Scriptable.NOT_FOUND) {
			Context cx = Context.enter();
			try {
				((Function) f).call(cx, globalScope, globalScope, new Object[] { partyId });
			} finally {
				Context.exit();
			}
		}
	}

	public void partyMemberRemoved(int partyId, int playerId) {
		Object f = globalScope.get("partyMemberRemoved", globalScope);
		if (f != Scriptable.NOT_FOUND) {
			Context cx = Context.enter();
			try {
				((Function) f).call(cx, globalScope, globalScope, new Object[] { partyId, playerId });
			} finally {
				Context.exit();
			}
		}
	}

	public void mobDied(int dataId, int entityId, int mapId) {
		Object f = globalScope.get("mobDied", globalScope);
		if (f != Scriptable.NOT_FOUND) {
			Context cx = Context.enter();
			try {
				((Function) f).call(cx, globalScope, globalScope, new Object[] { dataId, entityId, mapId });
			} finally {
				Context.exit();
			}
		}
	}

	public void mobSpawned(int dataId, int entityId, int mapId) {
		Object f = globalScope.get("mobSpawned", globalScope);
		if (f != Scriptable.NOT_FOUND) {
			Context cx = Context.enter();
			try {
				((Function) f).call(cx, globalScope, globalScope, new Object[] { dataId, entityId, mapId });
			} finally {
				Context.exit();
			}
		}
	}

	public void friendlyMobHurt(int dataId, int entityId, int mapId, int hp, int maxHp) {
		Object f = globalScope.get("friendlyMobHurt", globalScope);
		if (f != Scriptable.NOT_FOUND) {
			Context cx = Context.enter();
			try {
				((Function) f).call(cx, globalScope, globalScope, new Object[] { dataId, entityId, mapId, hp, maxHp });
			} finally {
				Context.exit();
			}
		}
	}
}
