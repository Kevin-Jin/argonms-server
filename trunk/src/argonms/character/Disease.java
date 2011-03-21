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

package argonms.character;

/**
 *
 * @author GoldenKevin
 */
public enum Disease {
	NULL		(0x0000000000000000L),
	SLOW		(0x0000000000000001L),
	SEDUCE		(0x0000000000000080L),
	STUN		(0x0002000000000000L),
	POISON		(0x0004000000000000L),
	SEAL		(0x0008000000000000L),
	DARKNESS	(0x0010000000000000L),
	WEAKEN		(0x4000000000000000L),
	CURSE		(0x8000000000000000L)
	;

	private final long mask;

	private Disease(long mask) {
		this.mask = mask;
	}

	public long getMask() {
		return mask;
	}
}
