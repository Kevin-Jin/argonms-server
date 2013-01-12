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

package argonms.game.command;

import argonms.game.character.GameCharacter;
import argonms.game.field.GameMap;

/**
 *
 * @author GoldenKevin
 */
public class GameCharacterCommandCaller implements CommandCaller {
	private final GameCharacter caller;

	public GameCharacterCommandCaller(GameCharacter caller) {
		this.caller = caller;
	}

	@Override
	public String getName() {
		return caller.getName();
	}

	@Override
	public byte getWorld() {
		return caller.getClient().getWorld();
	}

	@Override
	public byte getChannel() {
		return caller.getClient().getChannel();
	}

	@Override
	public GameMap getMap() {
		return caller.getMap();
	}

	@Override
	public boolean isInGame() {
		return true;
	}

	@Override
	public byte getPrivilegeLevel() {
		return caller.getPrivilegeLevel();
	}

	@Override
	public boolean isDisconnected() {
		return caller.isClosed();
	}

	public GameCharacter getBackingCharacter() {
		return caller;
	}
}
