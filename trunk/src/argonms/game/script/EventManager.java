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

package argonms.game.script;

import argonms.common.GlobalConstants;
import argonms.common.util.collections.Pair;
import argonms.game.script.binding.ScriptEvent;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
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
	private final ConcurrentMap<String, Pair<ScriptEvent, EventManipulator>> activatedEvents;

	public EventManager(String scriptPath, byte channel, String[] activateNow) {
		eventPath = scriptPath + "events" + GlobalConstants.DIR_DELIMIT;
		this.channel = channel;
		activatedEvents = new ConcurrentHashMap<String, Pair<ScriptEvent, EventManipulator>>();
		for (String script : activateNow)
			runScript(script, true, null);
	}

	/**
	 * 
	 * @param scriptName
	 * @param associateWithName allows the use of {@link #getRunningScript(String)},
	 * {@link #getScriptInterface(String)}, and {@link #endScript(String)}.
	 * If the same script is already executing (i.e. scriptName already exists as a key in activatedEvents),
	 * a new instance will not be executed and this method will return <code>null</code>.
	 * @param attachment
	 * @return 
	 */
	public final ScriptEvent runScript(String scriptName, boolean associateWithName, Object attachment) {
		ScriptEvent event = null;
		EventManipulator delegator;
		Context cx = Context.enter();
		try {
			FileReader reader = new FileReader(eventPath + scriptName + ".js");
			Scriptable globalScope = cx.initStandardObjects();
			cx.setOptimizationLevel(1);
			cx.setLanguageVersion(Context.VERSION_1_7);
			cx.getWrapFactory().setJavaPrimitiveWrap(false);
			delegator = new EventManipulator(globalScope);
			event = new ScriptEvent(associateWithName ? scriptName : null, channel, delegator, globalScope);
			delegator.setVariables(event.getVariables());

			if (associateWithName && activatedEvents.putIfAbsent(scriptName, new Pair<ScriptEvent, EventManipulator>(event, delegator)) != null)
				return null;

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

	/**
	 * In order to have an effect, {@link #runScript(String, boolean, Object)} must have been called with
	 * the same <code>scriptName</code> and <code>true</code> passed to <code>associateWithName</code>.
	 * @param scriptName 
	 */
	public ScriptEvent getRunningScript(String scriptName) {
		Pair<ScriptEvent, EventManipulator> event = activatedEvents.get(scriptName);
		if (event == null)
			return null;

		return event.left;
	}

	/**
	 * In order to have an effect, {@link #runScript(String, boolean, Object)} must have been called with
	 * the same <code>scriptName</code> and <code>true</code> passed to <code>associateWithName</code>.
	 * @param scriptName 
	 */
	public EventManipulator getScriptInterface(String scriptName) {
		Pair<ScriptEvent, EventManipulator> event = activatedEvents.get(scriptName);
		if (event == null)
			return null;

		return event.right;
	}

	public void endScript(ScriptEvent event, EventManipulator delegator) {
		event.stopTimers();
		delegator.deinit();
	}

	/**
	 * In order to have an effect, {@link #runScript(String, boolean, Object)} must have been called with
	 * the same <code>scriptName</code> and <code>true</code> passed to <code>associateWithName</code>.
	 * @param scriptName 
	 */
	public void endScript(String scriptName) {
		Pair<ScriptEvent, EventManipulator> event = activatedEvents.remove(scriptName);
		if (event == null)
			return;

		endScript(event.left, event.right);
	}
}
