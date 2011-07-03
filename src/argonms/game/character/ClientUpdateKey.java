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

package argonms.game.character;

import java.util.EnumSet;
import java.util.Set;

/**
 *
 * @author GoldenKevin
 */
public enum ClientUpdateKey {
	SKIN		(0x000001),
	FACE		(0x000002),
	HAIR		(0x000004),
	LEVEL		(0x000010),
	JOB			(0x000020),
	STR			(0x000040),
	DEX			(0x000080),
	INT			(0x000100),
	LUK			(0x000200),
	HP			(0x000400),
	MAXHP		(0x000800),
	MP			(0x001000),
	MAXMP		(0x002000),
	AVAILABLEAP	(0x004000),
	AVAILABLESP	(0x008000),
	EXP			(0x010000),
	FAME		(0x020000),
	MESO		(0x040000),
	PET			(0x180008);

	private final int mask;

	private ClientUpdateKey(int mask) {
		this.mask = mask;
	}

	public int intValue() {
		return mask;
	}

	public static Set<ClientUpdateKey> valueOf(int mask) {
		EnumSet<ClientUpdateKey> included = EnumSet.noneOf(ClientUpdateKey.class);
		for (ClientUpdateKey key : values())
			if ((mask & key.intValue()) == key.intValue())
				included.add(key);
		return included;
	}
}
