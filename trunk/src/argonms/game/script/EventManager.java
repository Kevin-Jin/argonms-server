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

import argonms.common.GlobalConstants;
import argonms.game.script.binding.ScriptEvent;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;

/**
 *
 * @author GoldenKevin
 */
public class EventManager {
	private static final Logger LOG = Logger.getLogger(EventManager.class.getName());

	private final String eventPath;
	private final byte channel;
	private final Map<String, ScriptEvent> activatedEvents;
	private final Map<String, EventManipulator> handlers;

	public EventManager(String scriptPath, byte channel, String[] activateNow) {
		eventPath = scriptPath + "events" + GlobalConstants.DIR_DELIMIT;
		this.channel = channel;
		activatedEvents = new ConcurrentHashMap<String, ScriptEvent>();
		handlers = new ConcurrentHashMap<String, EventManipulator>();
		for (String script : activateNow)
			runScript(script, null);
	}

	public final ScriptEvent runScript(String scriptName, Object attachment) {
		ScriptEvent event = null;
		EventManipulator delegator = null;
		Context cx = Context.enter();
		try {
			FileReader reader = new FileReader(eventPath + scriptName + ".js");
			Scriptable globalScope = cx.initStandardObjects();
			cx.setOptimizationLevel(1);
			cx.setLanguageVersion(Context.VERSION_1_7);
			cx.getWrapFactory().setJavaPrimitiveWrap(false);
			delegator = new EventManipulator(globalScope);
			event = new ScriptEvent(scriptName, channel, delegator, globalScope);
			delegator.setVariables(event.getVariables());

			handlers.put(scriptName, delegator);
			activatedEvents.put(scriptName, event);

			globalScope.put("event", globalScope, Context.javaToJS(event, globalScope));
			cx.evaluateReader(globalScope, reader, "events/" + scriptName + ".js", 1, null);
			reader.close();

			Object f = globalScope.get("init", globalScope);
			if (f != Scriptable.NOT_FOUND)
				((Function) f).call(cx, globalScope, globalScope, new Object[] { attachment });
		} catch (FileNotFoundException ex) {
			LOG.log(Level.WARNING, "Missing event script {0}", scriptName);
			return null;
		} catch (IOException ex) {
			LOG.log(Level.WARNING, "Error executing event script " + scriptName, ex);
			return null;
		} finally {
			Context.exit();
		}
		return event;
	}

	public ScriptEvent getRunningScript(String scriptName) {
		return activatedEvents.get(scriptName);
	}

	public EventManipulator getScriptInterface(String scriptName) {
		return handlers.get(scriptName);
	}

	public void endScript(String scriptName) {
		activatedEvents.remove(scriptName).stopTimers();
		handlers.remove(scriptName).deinit();
	}
}
