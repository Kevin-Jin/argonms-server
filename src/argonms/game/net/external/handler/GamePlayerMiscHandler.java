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

package argonms.game.net.external.handler;

import argonms.common.character.inventory.Inventory.InventoryType;
import argonms.common.net.external.CheatTracker;
import argonms.common.net.external.ClientSendOps;
import argonms.common.util.input.LittleEndianReader;
import argonms.common.util.output.LittleEndianByteArrayWriter;
import argonms.game.character.GameCharacter;
import argonms.game.net.external.GameClient;

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

	public static void handleReplenishHpMp(LittleEndianReader packet, GameClient gc) {
		long now = System.currentTimeMillis();
		GameCharacter p = gc.getPlayer();
		packet.skip(4);
		short hp = packet.readShort();
		short mp = packet.readShort();
		//TODO: use MP recovery and HP recovery skill levels to determine if the
		//proper amount of HP/MP is being recovered
		if (p.getHp() == 0 || hp > 400 || mp > 1000 || (hp > 0 && mp > 0)) {
			CheatTracker.get(gc).suspicious(CheatTracker.Infraction.PACKET_EDITING, "Tried to replenish too much HP/MP at once");
			return;
		}
		if (hp > 0) {
			CheatTracker ct = CheatTracker.get(gc);
			long last = ct.getLoggedTime("hpr");
			ct.logTime("hpr", now);
			if (now - last < 9500) { //9.5 seconds, give some leniency since time recording isn't always accurate w/ latency
				ct.suspicious(CheatTracker.Infraction.PACKET_EDITING, "Tried to replenish HP too rapidly (" + ((now - last) / 1000.0) + " seconds)");
				return;
			}
			p.gainHp(hp);
		}
		if (mp > 0) {
			CheatTracker ct = CheatTracker.get(gc);
			long last = ct.getLoggedTime("mpr");
			ct.logTime("mpr", now);
			if (now - last < 9500) { //9.5 seconds, give some leniency since time recording isn't always accurate w/ latency
				ct.suspicious(CheatTracker.Infraction.PACKET_EDITING, "Tried to replenish MP too rapidly (" + ((now - last) / 1000.0) + " seconds)");
				return;
			}
			p.gainMp(mp);
		}
	}

	public static void handleEmote(LittleEndianReader packet, GameClient gc) {
		GameCharacter p = gc.getPlayer();
		int emote = packet.readInt();
		if (emote > 7) { //cash emotes
			int itemid = 5159992 + emote;
			if (p.getInventory(InventoryType.CASH).hasItem(itemid, 1)) {
				CheatTracker.get(gc).suspicious(CheatTracker.Infraction.PACKET_EDITING, "Tried to use cash shop emote without owning the item");
				return;
			}
		}
		p.getMap().sendToAll(writeExpressionChange(p, emote), p);
	}

	public static void handleBindingChange(LittleEndianReader packet, GameClient gc) {
		GameCharacter p = gc.getPlayer();
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

	private static byte[] writeExpressionChange(GameCharacter p, int expression) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(10);
		lew.writeShort(ClientSendOps.FACIAL_EXPRESSION);
		lew.writeInt(p.getId());
		lew.writeInt(expression);
		return lew.getBytes();
	}
}
