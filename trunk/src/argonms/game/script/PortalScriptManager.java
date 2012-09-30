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
import argonms.game.character.GameCharacter;
import argonms.game.script.binding.ScriptField;
import argonms.game.script.binding.ScriptParty;
import argonms.game.script.binding.ScriptPlayer;
import argonms.game.script.binding.ScriptPortal;
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
public class PortalScriptManager {
	private static final Logger LOG = Logger.getLogger(PortalScriptManager.class.getName());

	private static PortalScriptManager singleton;

	private final String portalPath;

	private PortalScriptManager(String scriptPath) {
		portalPath = scriptPath + "portals" + GlobalConstants.DIR_DELIMIT;
	}

	public boolean runScript(String scriptName, GameCharacter p) {
		Context cx = Context.enter();
		try {
			FileReader reader = new FileReader(portalPath + scriptName + ".js");
			Scriptable globalScope = cx.initStandardObjects();
			cx.setOptimizationLevel(1);
			cx.setLanguageVersion(Context.VERSION_1_7);
			cx.getWrapFactory().setJavaPrimitiveWrap(false);
			ScriptPortal portalManager = new ScriptPortal(p.getClient(), globalScope);
			globalScope.put("portal", globalScope, Context.javaToJS(portalManager, globalScope));
			globalScope.put("player", globalScope, Context.javaToJS(new ScriptPlayer(p), globalScope));
			globalScope.put("map", globalScope, Context.javaToJS(new ScriptField(p.getMap(), globalScope), globalScope));
			globalScope.put("party", globalScope, Context.javaToJS(p.getParty() == null ? null : new ScriptParty(p.getClient().getChannel(), p.getParty(), globalScope), globalScope));
			cx.evaluateReader(globalScope, reader, "portals/" + scriptName + ".js", 1, null);
			reader.close();
			return portalManager.warped();
		} catch (FileNotFoundException ex) {
			//not like most of our portal scripts are implemented anyway...
			LOG.log(Level.FINE, "Missing portal script {0}", scriptName);
			return false;
		} catch (IOException ex) {
			LOG.log(Level.WARNING, "Error executing portal script " + scriptName, ex);
			return false;
		} finally {
			Context.exit();
		}
	}

	public static void setInstance(String scriptPath) {
		singleton = new PortalScriptManager(scriptPath);
	}

	public static PortalScriptManager getInstance() {
		return singleton;
	}
}
