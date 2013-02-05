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

package argonms.game.script.binding;

import argonms.common.character.inventory.InventorySlot;
import argonms.common.character.inventory.InventoryTools;
import argonms.game.field.entity.ItemDrop;
import argonms.game.field.entity.Mob;
import java.util.Collections;

/**
 *
 * @author GoldenKevin
 */
public class ScriptMob {
	private Mob m;

	public ScriptMob(Mob m) {
		this.m = m;
	}

	public int getDataId() {
		return m.getDataId();
	}

	public int getEntityId() {
		return m.getId();
	}

	public int getMapId() {
		return m.getMap().getDataId();
	}

	public int getHp() {
		return m.getHp();
	}

	public int getMaxHp() {
		return m.getMaxHp();
	}

	public void dropItem(int itemId) {
		InventorySlot item = InventoryTools.makeItemWithId(itemId);
		m.getMap().drop(Collections.singletonList(new ItemDrop(item)), m, ItemDrop.PICKUP_ALLOW_ALL, 0);
	}

	public int getDropAfter(boolean afterHit) {
		int dropTime = m.getDropItemPeriod() * 1000;
		if (afterHit)
			dropTime += m.getAnimationTime("hit1");
		return dropTime;
	}
}
