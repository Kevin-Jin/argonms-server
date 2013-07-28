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

package argonms.shop.net.internal;

import argonms.common.character.CenterServerSynchronizationOps;
import argonms.common.net.internal.ChannelSynchronizationOps;
import argonms.common.net.internal.RemoteCenterOps;
import argonms.common.util.input.LittleEndianReader;
import argonms.common.util.output.LittleEndianByteArrayWriter;
import argonms.common.util.output.LittleEndianWriter;
import argonms.shop.ShopServer;
import argonms.shop.character.ShopCharacter;

/**
 *
 * @author GoldenKevin
 */
public class ShopCenterServerSynchronization {
	private final byte world;

	public ShopCenterServerSynchronization(byte world) {
		this.world = world;
	}

	private void writeCenterServerSynchronizationPacketHeader(LittleEndianWriter lew, byte opcode) {
		lew.writeByte(RemoteCenterOps.CENTER_SERVER_SYNCHRONIZATION);
		lew.writeByte(world);
		lew.writeByte(opcode);
	}

	private void writeCenterServerSynchronizationPacket(byte[] packet) {
		ShopServer.getInstance().getCenterInterface().getSession().send(packet);
	}

	public void receivedCenterServerSynchronizationPacket(LittleEndianReader packet) {
		switch (packet.readByte()) {
			
		}
	}

	public void sendPartyMemberOnline(ShopCharacter p) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(12);
		writeCenterServerSynchronizationPacketHeader(lew, CenterServerSynchronizationOps.PARTY_MEMBER_CONNECTED);
		lew.writeInt(p.getPartyId());
		lew.writeInt(p.getId());
		lew.writeByte(ChannelSynchronizationOps.CHANNEL_CASH_SHOP);

		writeCenterServerSynchronizationPacket(lew.getBytes());
	}

	public void sendPartyMemberOffline(ShopCharacter exiter, boolean loggingOff) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(13);
		writeCenterServerSynchronizationPacketHeader(lew, CenterServerSynchronizationOps.PARTY_MEMBER_DISCONNECTED);
		lew.writeInt(exiter.getPartyId());
		lew.writeInt(exiter.getId());
		lew.writeByte(ChannelSynchronizationOps.CHANNEL_CASH_SHOP);
		lew.writeBool(loggingOff);

		writeCenterServerSynchronizationPacket(lew.getBytes());
	}

	public void sendGuildMemberOnline(ShopCharacter p) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(13);
		writeCenterServerSynchronizationPacketHeader(lew, CenterServerSynchronizationOps.GUILD_MEMBER_CONNECTED);
		lew.writeInt(p.getGuildId());
		lew.writeInt(p.getId());
		lew.writeByte(ChannelSynchronizationOps.CHANNEL_CASH_SHOP);
		lew.writeBool(false);

		writeCenterServerSynchronizationPacket(lew.getBytes());
	}

	public void sendGuildMemberOffline(ShopCharacter exiter, boolean loggingOff) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(13);
		writeCenterServerSynchronizationPacketHeader(lew, CenterServerSynchronizationOps.GUILD_MEMBER_DISCONNECTED);
		lew.writeInt(exiter.getGuildId());
		lew.writeInt(exiter.getId());
		lew.writeByte(ChannelSynchronizationOps.CHANNEL_CASH_SHOP);
		lew.writeBool(loggingOff);

		writeCenterServerSynchronizationPacket(lew.getBytes());
	}

	public void sendLeaveChatroom(int roomId, int leaver) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(11);
		writeCenterServerSynchronizationPacketHeader(lew, CenterServerSynchronizationOps.CHATROOM_REMOVE_PLAYER);
		lew.writeInt(roomId);
		lew.writeInt(leaver);

		writeCenterServerSynchronizationPacket(lew.getBytes());
	}
}
