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

import argonms.common.util.Scheduler;
import argonms.game.GameServer;
import argonms.game.script.EventManipulator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

/**
 *
 * @author GoldenKevin
 */
public class ScriptEvent {
	private final Scriptable globalScope;
	private final String name;
	private final byte channel;
	private final EventManipulator hooks;
	private final Map<String, Object> variables;
	private final Map<String, ScheduledFuture<?>> timers;

	public ScriptEvent(String scriptName, byte channel, EventManipulator hooks, Scriptable globalScope) {
		this.globalScope = globalScope;
		this.name = scriptName;
		this.channel = channel;
		this.hooks = hooks;
		variables = new ConcurrentHashMap<String, Object>();
		timers = new ConcurrentHashMap<String, ScheduledFuture<?>>();
	}

	public String getName() {
		return name;
	}

	public Map<String, Object> getVariables() {
		return variables;
	}

	public void setVariable(String key, Object value) {
		variables.put(key, value);
	}

	public Object getVariable(String key) {
		return Context.javaToJS(variables.get(key), globalScope);
	}

	public Object getMap(int mapId) {
		return Context.javaToJS(new ScriptField(GameServer.getChannel(channel).getMapFactory().getMap(mapId)), globalScope);
	}

	public Object makeMap(int id) {
		return Context.javaToJS(new ScriptField(GameServer.getChannel(channel).getMapFactory().makeInstanceMap(id)), globalScope);
	}

	public void destroyMap(ScriptField map) {
		GameServer.getChannel(channel).getMapFactory().destroyInstanceMap(map.getMap());
	}

	public void startTimer(final String key, int millisDelay) {
		timers.put(key, Scheduler.getInstance().runAfterDelay(new Runnable() {
			@Override
			public void run() {
				timers.remove(key);
				hooks.timerExpired(key);
			}
		}, millisDelay));
	}

	public void stopTimers() {
		for (ScheduledFuture<?> future : timers.values())
			future.cancel(false);
	}

	public void destroyEvent() {
		GameServer.getChannel(channel).getEventManager().endScript(name);
	}
}
