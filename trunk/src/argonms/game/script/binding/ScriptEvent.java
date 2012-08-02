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
import argonms.game.script.EventManipulator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 * @author GoldenKevin
 */
public class ScriptEvent {
	private final EventManipulator hooks;
	private final Map<String, String> variables;

	public ScriptEvent(EventManipulator hooks) {
		this.hooks = hooks;
		variables = new ConcurrentHashMap<String, String>();
	}

	public void setVariable(String key, String value) {
		variables.put(key, value);
	}

	public String getVariable(String key) {
		return variables.get(key);
	}

	public void startTimer(final String key, int millisDelay) {
		Scheduler.getInstance().runAfterDelay(new Runnable() {
			@Override
			public void run() {
				hooks.timerExpired(key);
			}
		}, millisDelay);
	}
}
