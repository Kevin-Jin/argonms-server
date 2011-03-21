/*
 * ArgonMS MapleStory server emulator written in Java
 * Copyright (C) 2011  GoldenKevin
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

import argonms.character.Player;
import argonms.game.GameClient;
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

	//TODO: maybe we should just return a boolean value instead of calling
	//PortalActions.abortWarp? We would have to call a function though instead
	//of entering on the first line in the global scope...
	public static void runScript(String scriptName, Player p) {
		Context cx = Context.enter();
		try {
			FileReader reader = new FileReader("scripts/portals/" + scriptName + ".js");
			Scriptable globalScope = cx.initStandardObjects();
			globalScope.put("portal", globalScope, new PortalActions((GameClient) p.getClient()));
			cx.evaluateReader(globalScope, reader, scriptName, 1, null);
			reader.close();
		} catch (FileNotFoundException ex) {
			//not like most of our portal scripts are implemented anyway...
			LOG.log(Level.FINE, "Missing portal script {0}", scriptName);
		} catch (IOException ex) {
			LOG.log(Level.WARNING, "Error executing portal script " + scriptName, ex);
		} finally {
			Context.exit();
		}
	}
}
