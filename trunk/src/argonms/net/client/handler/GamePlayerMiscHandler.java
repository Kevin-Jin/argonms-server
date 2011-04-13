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

import argonms.character.Player;
import argonms.character.inventory.Inventory.InventoryType;
import argonms.game.GameClient;
import argonms.net.client.ClientSendOps;
import argonms.net.client.RemoteClient;
import argonms.tools.input.LittleEndianReader;
import argonms.tools.output.LittleEndianByteArrayWriter;

/**
 *
 * @author GoldenKevin
 */
public class GamePlayerMiscHandler {
	private static final int
		BINDING_CHANGE_KEY_MAPPING = 0,
		BINDING_CHANGE_AUTO_HP_POT = 1,
		BINDING_CHANGE_AUTO_MP_POT = 2
	;

	public static void handleReplenishHpMp(LittleEndianReader packet, RemoteClient rc) {
		Player p = ((GameClient) rc).getPlayer();
		packet.skip(4);
		short hp = packet.readShort();
		short mp = packet.readShort();
		if (p.getHp() == 0 || hp > 400 || mp > 1000 || (hp > 0 && mp > 0)) {
			//TODO: hacking
			return;
		}
		if (hp > 0)
			p.gainHp(hp);
		if (mp > 0)
			p.gainMp(mp);
	}

	public static void handleEmote(LittleEndianReader packet, RemoteClient rc) {
		Player p = ((GameClient) rc).getPlayer();
		int emote = packet.readInt();
		if (emote > 7) { //cash emotes
			int itemid = 5159992 + emote;
			if (p.getInventory(InventoryType.CASH).hasItem(itemid, (short) 1)) {
				//TODO: hacking
				return;
			}
		}
		p.getMap().sendToAll(writeExpressionChange(p, emote), p);
	}

	public static void handleBindingChange(LittleEndianReader packet, RemoteClient rc) {
		Player p = ((GameClient) rc).getPlayer();
		int actionType = packet.readInt();
		switch (actionType) {
			case BINDING_CHANGE_KEY_MAPPING: {
				for (int i = packet.readInt(); i > 0; --i) {
					byte key = (byte) packet.readInt();
					byte type = packet.readByte();
					int action = packet.readInt();
					p.bindKey(key, type, action);
				}
				break;
			//TODO: how the heck do you send these bindings to the client?
			} case BINDING_CHANGE_AUTO_HP_POT: {
				int itemid = packet.readInt();
				if (itemid == 0) {
					//unequip
				} else {
					//equip
				}
				break;
			} case BINDING_CHANGE_AUTO_MP_POT: {
				int itemid = packet.readInt();
				if (itemid == 0) {
					//unequip
				} else {
					//equip
				}
				break;
			}
		}
	}

	private static byte[] writeExpressionChange(Player p, int expression) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(10);
		lew.writeShort(ClientSendOps.FACIAL_EXPRESSION);
		lew.writeInt(p.getId());
		lew.writeInt(expression);
		return lew.getBytes();
	}
}
