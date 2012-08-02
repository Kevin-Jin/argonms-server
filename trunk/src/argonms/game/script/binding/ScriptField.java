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

import argonms.game.field.GameMap;
import argonms.game.net.external.GamePackets;

/**
 *
 * @author GoldenKevin
 */
public class ScriptField {
	private final GameMap map;

	public ScriptField(GameMap map) {
		this.map = map;
	}

	public int getId() {
		return map.getDataId();
	}

	public int getPlayerCount() {
		return map.getPlayerCount();
	}

	public void portalEffect(String name) {
		map.sendToAll(GamePackets.writeMapEffect((byte) 2, name));
	}

	public void screenEffect(String name) {
		map.sendToAll(GamePackets.writeMapEffect((byte) 3, name));
	}

	public void soundEffect(String name) {
		map.sendToAll(GamePackets.writeMapEffect((byte) 4, name));
	}
}
