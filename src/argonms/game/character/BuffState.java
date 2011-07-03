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

/**
 *
 * @author GoldenKevin
 */
public abstract class BuffState {
	public final long endTime;

	protected BuffState(long endTime) {
		this.endTime = endTime;
	}

	public static class SkillState extends BuffState {
		public final byte level;

		protected SkillState(byte level, long endTime) {
			super(endTime);
			this.level = level;
		}
	}

	public static class ItemState extends BuffState {
		protected ItemState(long endTime) {
			super(endTime);
		}
	}

	public static class MobSkillState extends BuffState {
		public final byte level;

		protected MobSkillState(byte level, long endTime) {
			super(endTime);
			this.level = level;
		}
	}
}
