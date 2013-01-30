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

package argonms.game.net.external.handler;

import argonms.common.character.inventory.Inventory.InventoryType;
import argonms.common.character.inventory.InventorySlot;
import argonms.common.character.inventory.Pet;
import argonms.common.character.inventory.TamingMob;
import argonms.common.net.external.CheatTracker;
import argonms.common.net.external.ClientSendOps;
import argonms.common.util.input.LittleEndianReader;
import argonms.common.util.output.LittleEndianByteArrayWriter;
import argonms.game.character.GameCharacter;
import argonms.game.character.GuildList;
import argonms.game.field.MapEntity.EntityType;
import argonms.game.net.external.GameClient;
import java.util.List;

/**
 *
 * @author GoldenKevin
 */
public final class PersonalInfoHandler {
	private static final byte
		FAME_OPERATION_RESPONSE_SUCCESS = 0,
		FAME_OPERATION_RESPONSE_NOT_IN_MAP = 1,
		FAME_OPERATION_RESPONSE_UNDER_LEVEL = 2,
		FAME_OPEARTION_RESPONSE_NOT_TODAY = 3,
		FAME_OPERATION_RESPONSE_NOT_THIS_MONTH = 4,
		FAME_OPERATION_FAME_CHANGED = 5
	;

	public static void handleFameUp(LittleEndianReader packet, GameClient gc) {
		GameCharacter self = gc.getPlayer();
		GameCharacter receiver = (GameCharacter) self.getMap().getEntityById(EntityType.PLAYER, packet.readInt());
		boolean add = packet.readBool();

		if (receiver == self) {
			CheatTracker.get(gc).suspicious(CheatTracker.Infraction.CERTAIN_PACKET_EDITING, "Tried to give fame to self");
			return;
		}
		if (receiver == null) {
			gc.getSession().send(writeFameError(FAME_OPERATION_RESPONSE_NOT_IN_MAP));
			return;
		}
		if (self.getLevel() < 15) {
			gc.getSession().send(writeFameError(FAME_OPERATION_RESPONSE_UNDER_LEVEL));
			return;
		}
		if (self.getLastFameGivenTime() >= System.currentTimeMillis() - 1000 * 60 * 60 * 24) {
			gc.getSession().send(writeFameError(FAME_OPEARTION_RESPONSE_NOT_TODAY));
			return;
		}
		if (!self.canGiveFameToPlayer(receiver.getId())) {
			gc.getSession().send(writeFameError(FAME_OPERATION_RESPONSE_NOT_THIS_MONTH));
			return;
		}
		if (add)
			receiver.setFame((short) (receiver.getFame() + 1));
		else
			receiver.setFame((short) (receiver.getFame() - 1));
		self.gaveFame(receiver.getId());
		gc.getSession().send(writeFameSuccess(add, receiver.getName(), receiver.getFame()));
		receiver.getClient().getSession().send(writeFameChanged(add, self.getName()));
	}

	public static void handleOpenInfo(LittleEndianReader packet, GameClient gc) {
		/*int tickCount = */packet.readInt();
		GameCharacter p = (GameCharacter) gc.getPlayer().getMap().getEntityById(EntityType.PLAYER, packet.readInt());
		if (p != null)
			gc.getSession().send(writePersonalInfo(p, packet.readBool()));
	}

	private static byte[] writeFameSuccess(boolean add, String to, short newFame) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(10
				+ to.length());

		lew.writeShort(ClientSendOps.FAME_OPERATION);
		lew.writeByte(FAME_OPERATION_RESPONSE_SUCCESS);
		lew.writeLengthPrefixedString(to);
		lew.writeBool(add);
		lew.writeShort(newFame);
		lew.writeShort((short) 0);

		return lew.getBytes();
	}

	private static byte[] writeFameChanged(boolean add, String from) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(6
				+ from.length());

		lew.writeShort(ClientSendOps.FAME_OPERATION);
		lew.writeByte(FAME_OPERATION_FAME_CHANGED);
		lew.writeLengthPrefixedString(from);
		lew.writeBool(add);

		return lew.getBytes();
	}

	private static byte[] writeFameError(byte message) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(3);

		lew.writeShort(ClientSendOps.FAME_OPERATION);
		lew.writeByte(message);

		return lew.getBytes();
	}

	private static byte[] writePersonalInfo(GameCharacter p, boolean self) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter();

		lew.writeShort(ClientSendOps.PERSONAL_INFO_RESPONSE);
		lew.writeInt(p.getId());
		lew.writeByte((byte) p.getLevel());
		lew.writeShort(p.getJob());
		lew.writeShort(p.getFame());
		lew.writeBool(p.getSpouseId() != 0);

		String allianceName = "";
		GuildList guild = p.getGuild();
		if (guild != null) {
			lew.writeLengthPrefixedString(guild.getName());
			/*if (guild.getAllianceId() != 0)
				allianceName = GuildAlliances.get(guild.getAllianceId()).getName();*/
		} else {
			lew.writeLengthPrefixedString("-");
		}
		lew.writeLengthPrefixedString(allianceName);
		lew.writeBool(self);
		for (Pet pet : p.getPets()) {
			if (pet != null) {
				lew.writeBool(true);
				lew.writeInt(pet.getDataId());
				lew.writeLengthPrefixedString(pet.getName());
				lew.writeByte(pet.getLevel());
				lew.writeShort(pet.getCloseness());
				lew.writeByte(pet.getFullness());
				lew.writeShort((short) 0);
				InventorySlot petEquip = p.getInventory(InventoryType.EQUIPPED).get((short) -114);
				lew.writeInt(petEquip == null ? 0 : petEquip.getDataId());
			}
		}
		lew.writeBool(false);
		if (p.getInventory(InventoryType.EQUIPPED).get((short) -18) != null) {
			TamingMob mount = p.getEquippedMount();
			lew.writeBool(true);
			lew.writeInt(mount.getLevel());
			lew.writeInt(mount.getExp());
			lew.writeInt(mount.getTiredness());
		} else {
			lew.writeBool(false);
		}
		List<Integer> wishList = p.getWishListSerialNumbers();
		lew.writeByte((byte) wishList.size());
		for (Integer sn : wishList)
			lew.writeInt(sn.intValue());

		lew.writeInt(1); //monster book level
		lew.writeInt(0); //monster book normals
		lew.writeInt(0); //monster book specials
		lew.writeInt(0); //monster book size
		lew.writeInt(0); //monster book cover

		return lew.getBytes();
	}

	private PersonalInfoHandler() {
		//uninstantiable...
	}
}
