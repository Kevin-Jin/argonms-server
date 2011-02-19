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

package argonms.map.object;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import argonms.character.Player;
import argonms.character.inventory.InventorySlot;
import argonms.loading.mob.MobStats;
import argonms.map.MapObject;
import argonms.net.client.CommonPackets;

/**
 *
 * @author GoldenKevin
 */
public class Mob extends MapObject {
	private MobStats stats;
	private List<MobDeathHook> hooks;
	private Player highestDamageKiller;

	public Mob(MobStats stats) {
		this.stats = stats;
		this.hooks = new ArrayList<MobDeathHook>();
	}

	public int getMobId() {
		return stats.getMobId();
	}

	public List<ItemDrop> getDrops() {
		List<InventorySlot> items = stats.getItemsToDrop();
		List<ItemDrop> combined = new ArrayList<ItemDrop>(items.size() + 1);
		for (InventorySlot item : items)
			combined.add(new ItemDrop(item));
		combined.add(new ItemDrop(stats.getMesosToDrop()));
		Collections.shuffle(combined);
		return combined;
	}

	public void addDeathHook(MobDeathHook hook) {
		hooks.add(hook);
	}

	public void died() {
		for (MobDeathHook hook : hooks)
			hook.monsterKilled(highestDamageKiller);
	}

	public MapObjectType getObjectType() {
		return MapObjectType.MONSTER;
	}

	public boolean isVisible() {
		return true;
	}

	public byte[] getCreationMessage() {
		return CommonPackets.writeShowMonster(this, true, (byte) 0);
	}

	public byte[] getShowObjectMessage() {
		return CommonPackets.writeShowMonster(this, false, (byte) 0);
	}

	public byte[] getOutOfViewMessage() {
		return CommonPackets.writeRemoveMonster(this, (byte) 0);
	}

	public byte[] getDestructionMessage() {
		return CommonPackets.writeRemoveMonster(this, (byte) 1);
	}

	public boolean isNonRangedType() {
		return false;
	}

	public interface MobDeathHook {
		public void monsterKilled(Player highestDamageChar);
	}
}
