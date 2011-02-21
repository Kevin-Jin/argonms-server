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

package argonms.net.client.handler;

import java.util.List;
import argonms.character.Player;
import argonms.character.inventory.InventoryTools;
import argonms.game.GameClient;
import argonms.game.npcscript.NpcConversationActions;
import argonms.game.npcscript.NpcScriptManager;
import argonms.loading.item.ItemDataLoader;
import argonms.loading.shop.NpcShopDataLoader;
import argonms.map.MapObject;
import argonms.map.object.Npc;
import argonms.map.object.NpcShop;
import argonms.map.object.NpcShop.ShopItem;
import argonms.map.object.PlayerNpc;
import argonms.net.client.ClientSendOps;
import argonms.net.client.RemoteClient;
import argonms.tools.BitTools;
import argonms.tools.input.LittleEndianReader;
import argonms.tools.output.LittleEndianByteArrayWriter;

/**
 *
 * @author GoldenKevin
 */
public class GameNpcHandler {
	public static void handleStartConversation(LittleEndianReader packet, RemoteClient rc) {
		GameClient client = (GameClient) rc;
		int oid = packet.readInt();
		packet.readInt();
		MapObject obj = client.getPlayer().getMap().getObjectById(oid);

		Npc npc = null;
		switch (obj.getObjectType()) {
			case NPC: {
				npc = (Npc) obj;
				if (NpcShopDataLoader.getInstance().canLoad(npc.getNpcId())) {
					client.getSession().send(npc.getShopPacket());
					return;
				}
				break;
			} case PLAYER_NPC: {
				npc = (PlayerNpc) obj;
				switch (client.getPlayer().getMapId()) {
					case 100000201: //Bowman Instructional School
					case 101000003: //Magic Library
					case 102000003: //Warriors' Sanctuary
					case 103000003: //Thieves' Hideout
						client.getSession().send(writeMaxLevelPlayerNpc(npc.getNpcId()));
						return;
				}
				break;
			}
		}
		if (npc != null)
			NpcScriptManager.runScript(npc.getNpcId(), client);
	}

	public static void handleContinueConversation(LittleEndianReader packet, RemoteClient rc) {
		NpcConversationActions npc = ((GameClient) rc).getNpc();
		if (npc != null)
			npc.responseReceived(packet);
	}

	private static byte[] writeMaxLevelPlayerNpc(int npc) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter();

		lew.writeShort(ClientSendOps.NPC_TALK);
		lew.writeByte((byte) 4); //4 is for NPC conversation actions I guess...
		lew.writeInt(npc);
		lew.writeByte((byte) 0); //SAY (ok box)
		lew.writeLengthPrefixedString("I am #r" + npc + ", and I have reached level #b200#k.");
		lew.writeBool(false); //prev button
		lew.writeBool(false); //next button

		return lew.getBytes();
	}

	public static byte[] writeNpcShop(Player customer, NpcShop shop) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter();

		lew.writeShort(ClientSendOps.NPC_SHOP);
		lew.writeInt(shop.getId());
		List<ShopItem> items = shop.getStock();
		lew.writeShort((short) items.size());
		for (ShopItem item : items) {
			lew.writeInt(item.getItemId());
			lew.writeInt(item.getPrice());
			if (!InventoryTools.isThrowingStar(item.getItemId()) && !InventoryTools.isBullet(item.getItemId())) {
				lew.writeShort((short) 1);
				lew.writeShort(item.getBuyable());
			} else {
				lew.writeShort((short) 0);
				lew.writeInt(0);
				lew.writeShort((short) BitTools.doubleToShortBits(ItemDataLoader.getInstance().getUnitPrice(item.getItemId())));
				lew.writeShort(InventoryTools.getPersonalSlotMax(customer, item.getItemId()));
			}
		}

		return lew.getBytes();
	}
}
