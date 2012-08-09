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
import argonms.game.field.entity.Reactor;
import argonms.game.loading.reactor.ReactorDataLoader;
import argonms.game.net.external.GameClient;
import argonms.game.script.binding.ScriptField;
import argonms.game.script.binding.ScriptParty;
import argonms.game.script.binding.ScriptPlayer;
import argonms.game.script.binding.ScriptReactor;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

/**
 *
 * @author GoldenKevin
 */
public class ReactorScriptManager {
	private static final Logger LOG = Logger.getLogger(ReactorScriptManager.class.getName());

	private static ReactorScriptManager singleton;

	private final String reactorScriptPath;

	private ReactorScriptManager(String scriptsPath) {
		reactorScriptPath = scriptsPath + "reactors" + GlobalConstants.DIR_DELIMIT;
	}

	public boolean runScript(int reactorId, Reactor reactor, GameClient client) {
		String scriptName = ReactorDataLoader.getInstance().getReactorStats(reactorId).getScript();
		Context cx = Context.enter();
		try {
			FileReader reader = new FileReader(reactorScriptPath + scriptName + ".js");
			Scriptable globalScope = cx.initStandardObjects();
			cx.setLanguageVersion(Context.VERSION_1_7);
			ScriptReactor actions = new ScriptReactor(reactorId, reactor, client, globalScope);
			globalScope.put("reactor", globalScope, Context.toObject(actions, globalScope));
			globalScope.put("player", globalScope, Context.toObject(new ScriptPlayer(client.getPlayer()), globalScope));
			globalScope.put("map", globalScope, Context.toObject(new ScriptField(client.getPlayer().getMap()), globalScope));
			globalScope.put("party", globalScope, client.getPlayer().getParty() == null ? null : Context.toObject(new ScriptParty(client.getChannel(), client.getPlayer().getParty()), globalScope));
			cx.evaluateReader(globalScope, reader, "reactors/" + scriptName + ".js", 1, null);
			reader.close();
			return true;
		} catch (FileNotFoundException ex) {
			//not like most of our reactor scripts are implemented anyway...
			LOG.log(Level.FINE, "Missing reactor script {0}", scriptName);
			return false;
		} catch (IOException ex) {
			LOG.log(Level.WARNING, "Error executing reactor script " + scriptName, ex);
			return false;
		} finally {
			Context.exit();
		}
	}

	public static void setInstance(String scriptPath) {
		if (singleton == null)
			singleton = new ReactorScriptManager(scriptPath);
	}

	public static ReactorScriptManager getInstance() {
		return singleton;
	}
}
