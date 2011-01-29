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

package argonms.character.inventory;

/**
 *
 * @author GoldenKevin
 */
public class Pet extends IItem {
	public Pet(int itemid) {
		super(itemid);
	}

	public ItemType getType() {
		return ItemType.PET;
	}

	public short getQuantity() {
		return 1;
	}

	public void setQuantity(short newValue) {
		throw new UnsupportedOperationException("Cannot change quantity of a pet.");
	}

	public Pet clone() {
		Pet copy = new Pet(getItemId());
		copy.setExpiration(getExpiration());
		copy.setUniqueId(getUniqueId());

		return copy;
	}
}
