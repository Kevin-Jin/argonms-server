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

import argonms.common.character.inventory.Inventory;
import argonms.common.character.inventory.InventorySlot;
import argonms.common.character.inventory.InventoryTools;
import argonms.common.net.external.ClientSession;
import argonms.game.GameServer;
import argonms.game.character.GameCharacter;
import argonms.game.character.PartyList;
import argonms.game.net.external.GamePackets;
import java.awt.Point;

/**
 *
 * @author GoldenKevin
 */
public class ScriptParty {
	private final byte channel;
	private final PartyList party;

	public ScriptParty(byte channel, PartyList party) {
		this.channel = channel;
		this.party = party;
	}

	public int getLeader() {
		return party.getLeader();
	}

	public byte getMembersCount(int mapId, short minLevel, short maxLevel) {
		byte count = 0;
		party.lockRead();
		try {
			for (GameCharacter c : party.getLocalMembersInMap(mapId))
				if (c.getLevel() >= minLevel && c.getLevel() <= maxLevel)
					count++;
		} finally {
			party.unlockRead();
		}
		return count;
	}

	public byte numberOfMembersInChannel() {
		party.lockRead();
		try {
			return (byte) party.getMembersInLocalChannel().size();
		} finally {
			party.unlockRead();
		}
	}

	public void gainExp(int gain) {
		party.lockRead();
		try {
			for (PartyList.LocalMember member : party.getMembersInLocalChannel())
				member.getPlayer().gainExp((int) Math.min((long) gain * GameServer.getVariables().getExpRate(), Integer.MAX_VALUE), false, true);
		} finally {
			party.unlockRead();
		}
	}

	public void loseItem(int itemId) {
		party.lockRead();
		try {
			for (PartyList.LocalMember member : party.getMembersInLocalChannel()) {
				GameCharacter player = member.getPlayer();
				Inventory.InventoryType type = InventoryTools.getCategory(itemId);
				short quantity = InventoryTools.getAmountOfItem(player.getInventory(type), itemId);
				if (type == Inventory.InventoryType.EQUIP)
					quantity += InventoryTools.getAmountOfItem(player.getInventory(Inventory.InventoryType.EQUIPPED), itemId);

				if (quantity > 0) {
					Inventory inv = player.getInventory(type);
					ClientSession<?> ses = player.getClient().getSession();
					InventoryTools.UpdatedSlots changedSlots = InventoryTools.removeFromInventory(player, itemId, quantity);
					short pos;
					InventorySlot slot;
					for (Short s : changedSlots.modifiedSlots) {
						pos = s.shortValue();
						slot = inv.get(pos);
						ses.send(GamePackets.writeInventoryUpdateSlotQuantity(type, pos, slot));
					}
					for (Short s : changedSlots.addedOrRemovedSlots) {
						pos = s.shortValue();
						ses.send(GamePackets.writeInventoryClearSlot(type, pos));
					}
					ses.send(GamePackets.writeShowItemGainFromQuest(itemId, -quantity));
					player.itemCountChanged(itemId);
				}
			}
		} finally {
			party.unlockRead();
		}
	}

	public int[] getMembersIdsInMap(int mapId) {
		int[] members = new int[6];
		int size = 0;
		party.lockRead();
		try {
			for (GameCharacter c : party.getLocalMembersInMap(mapId))
				members[size++] = c.getId();
		} finally {
			party.unlockRead();
		}
		int[] trimmed = new int[size];
		System.arraycopy(members, 0, trimmed, 0, size);
		return trimmed;
	}

	//TODO: Context.toObject
	public Point positionOf(int playerId) {
		party.lockRead();
		try {
			return ((PartyList.LocalMember) party.getMember(playerId)).getPlayer().getPosition();
		} finally {
			party.unlockRead();
		}
	}

	public void changeMap(int mapId) {
		party.lockRead();
		try {
			for (PartyList.LocalMember member : party.getMembersInLocalChannel())
				member.getPlayer().changeMap(mapId);
		} finally {
			party.unlockRead();
		}
	}

	public void changeMap(int mapId, byte portal) {
		party.lockRead();
		try {
			for (PartyList.LocalMember member : party.getMembersInLocalChannel())
				member.getPlayer().changeMap(mapId, portal);
		} finally {
			party.unlockRead();
		}
	}

	public void changeMap(int mapId, String portal) {
		changeMap(mapId, GameServer.getChannel(channel).getMapFactory().getMap(mapId).getPortalIdByName(portal));
	}
}
