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

import argonms.common.character.inventory.Equip;
import argonms.common.character.inventory.InventorySlot;
import argonms.common.character.inventory.InventorySlot.ItemType;
import argonms.common.character.inventory.InventoryTools;
import argonms.common.tools.Rng;
import argonms.game.GameClient;
import argonms.game.GameServer;
import argonms.game.character.GameCharacter;
import argonms.game.field.entity.ItemDrop;
import argonms.game.field.entity.Reactor;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

/**
 *
 * @author GoldenKevin
 */
public class ReactorInteraction extends PlayerScriptInteraction {
	private Reactor reactor;

	public ReactorInteraction(int reactorId, Reactor reactor, GameClient client, Context cx, Scriptable globalScope) {
		super(client);
		this.reactor = reactor;
	}

	//TODO: some way to drop items only if the player has a specific quest active
	public void dropItems(int mesosMin, int mesosMax, int mesoChance, int... itemsAndChances) {
		Random generator = Rng.getGenerator();
		List<ItemDrop> drops;
		int multiplier = GameServer.getVariables().getMesoRate();
		//TODO: should we multiply mesoChance by drop rate?
		if (mesoChance == 0 || generator.nextInt(1000000) >= mesoChance) {
			drops = new ArrayList<ItemDrop>(itemsAndChances.length / 2);
		} else {
			drops = new ArrayList<ItemDrop>(1 + itemsAndChances.length / 2);
			int mesos = (generator.nextInt(mesosMin - mesosMax + 1) + mesosMax);
			drops.add(new ItemDrop(mesos * multiplier));
		}
		multiplier = GameServer.getVariables().getDropRate();
		for (int i = 0; i + 1 < itemsAndChances.length; i+= 2) {
			if (generator.nextInt(1000000) < (itemsAndChances[i + 1] * multiplier)) {
				InventorySlot item = InventoryTools.makeItemWithId(itemsAndChances[i]);
				if (item.getType() == ItemType.EQUIP)
					InventoryTools.randomizeStats((Equip) item);
				drops.add(new ItemDrop(item));
			}
		}
		GameCharacter p = getClient().getPlayer();
		p.getMap().drop(drops, reactor, ItemDrop.PICKUP_ALLOW_OWNER, p.getId());
	}
}
