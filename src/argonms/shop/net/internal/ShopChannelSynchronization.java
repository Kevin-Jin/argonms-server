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

import argonms.common.character.BuffState;
import argonms.common.character.ShopPlayerContinuation;
import argonms.common.net.internal.ChannelSynchronizationOps;
import argonms.common.net.internal.CrossProcessSynchronization;
import argonms.common.net.internal.RemoteCenterOps;
import argonms.common.util.input.LittleEndianReader;
import argonms.common.util.output.LittleEndianByteArrayWriter;
import argonms.common.util.output.LittleEndianWriter;
import argonms.shop.ShopServer;
import argonms.shop.character.ShopCharacter;
import java.util.Map;

/**
 *
 * @author GoldenKevin
 */
public class ShopChannelSynchronization extends CrossProcessSynchronization {
	private final byte targetWorld, targetCh;
	private final byte[] ipAddress;
	private int port;

	public ShopChannelSynchronization(byte world, byte channel, byte[] ipAddress, int port) {
		this.targetWorld = world;
		this.targetCh = channel;
		this.ipAddress = ipAddress;
		this.port = port;
	}

	public byte[] getIpAddress() {
		return ipAddress;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	private void writeShopChannelSynchronizationPacketHeader(LittleEndianWriter lew, byte opcode) {
		lew.writeByte(RemoteCenterOps.SHOP_CHANNEL_SHOP_SYNCHRONIZATION);
		lew.writeByte(targetWorld);
		lew.writeByte(targetCh);
		lew.writeByte(opcode);
	}

	private void writeShopChannelSynchronizationPacket(byte[] packet) {
		ShopServer.getInstance().getCenterInterface().getSession().send(packet);
	}

	public void receivedChannelShopSynchronizationPacket(LittleEndianReader packet) {
		switch (packet.readByte()) {
			case ChannelSynchronizationOps.INBOUND_PLAYER:
				receivedPlayerContext(packet);
				break;
			case ChannelSynchronizationOps.INBOUND_PLAYER_ACCEPTED:
				receivedChannelChangeAcceptance(packet);
				break;
			case ChannelSynchronizationOps.PLAYER_SEARCH:
				receivedPlayerExistsCheck(packet);
				break;
		}
	}

	public void sendPlayerContext(int playerId, ShopPlayerContinuation context) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter();
		writeShopChannelSynchronizationPacketHeader(lew, ChannelSynchronizationOps.INBOUND_PLAYER);
		lew.writeInt(playerId);
		Map<Integer, BuffState.ItemState> activeItems = context.getActiveItems();
		lew.writeByte((byte) activeItems.size());
		for (Map.Entry<Integer, BuffState.ItemState> item : activeItems.entrySet()) {
			lew.writeInt(item.getKey().intValue());
			lew.writeLong(item.getValue().endTime);
		}
		Map<Integer, BuffState.SkillState> activeSkills = context.getActiveSkills();
		lew.writeByte((byte) activeSkills.size());
		for (Map.Entry<Integer, BuffState.SkillState> skill : activeSkills.entrySet()) {
			BuffState.SkillState skillState = skill.getValue();
			lew.writeInt(skill.getKey().intValue());
			lew.writeByte(skillState.level);
			lew.writeLong(skillState.endTime);
		}
		Map<Short, BuffState.MobSkillState> activeDebuffs = context.getActiveDebuffs();
		lew.writeByte((byte) activeDebuffs.size());
		for (Map.Entry<Short, BuffState.MobSkillState> debuff : activeDebuffs.entrySet()) {
			BuffState.MobSkillState debuffState = debuff.getValue();
			lew.writeShort(debuff.getKey().shortValue());
			lew.writeByte(debuffState.level);
			lew.writeLong(debuffState.endTime);
		}
		Map<Integer, BuffState.PlayerSummonState> activeSummons = context.getActiveSummons();
		lew.writeByte((byte) activeSummons.size());
		for (Map.Entry<Integer, BuffState.PlayerSummonState> summon : activeSummons.entrySet()) {
			BuffState.PlayerSummonState summonState = summon.getValue();
			lew.writeInt(summon.getKey().intValue());
			lew.writePos(summonState.pos);
			lew.writeByte(summonState.stance);
		}
		lew.writeShort(context.getEnergyCharge());

		writeShopChannelSynchronizationPacket(lew.getBytes());
	}

	private void receivedPlayerContext(LittleEndianReader packet) {
		ShopPlayerContinuation context = new ShopPlayerContinuation();
		int playerId = packet.readInt();
		byte count = packet.readByte();
		for (int i = 0; i < count; i++)
			context.addItemBuff(packet.readInt(), packet.readLong());
		count = packet.readByte();
		for (int i = 0; i < count; i++)
			context.addSkillBuff(packet.readInt(), packet.readByte(), packet.readLong());
		count = packet.readByte();
		for (int i = 0; i < count; i++)
			context.addMonsterDebuff(packet.readShort(), packet.readByte(), packet.readLong());
		count = packet.readByte();
		for (int i = 0; i < count; i++)
			context.addActiveSummon(packet.readInt(), packet.readPos(), packet.readByte());
		context.setEnergyCharge(packet.readShort());
		context.setChatroomId(packet.readInt());
		context.setEnteringCashShop(packet.readBool());
		context.setReturnChannel(targetCh);

		ShopServer.getInstance().storePlayerBuffs(playerId, context);
		sendChannelChangeAcceptance(playerId);
	}

	public void sendChannelChangeAcceptance(int playerId) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(8);
		writeShopChannelSynchronizationPacketHeader(lew, ChannelSynchronizationOps.INBOUND_PLAYER_ACCEPTED);
		lew.writeInt(playerId);

		writeShopChannelSynchronizationPacket(lew.getBytes());
	}

	private void receivedChannelChangeAcceptance(LittleEndianReader packet) {
		int playerId = packet.readInt();

		ShopServer.getInstance().performChannelChange(playerId);
	}

	private void receivedPlayerExistsCheck(LittleEndianReader packet) {
		int responseId = packet.readInt();
		String name = packet.readLengthPrefixedString();

		byte result;
		ShopCharacter p = ShopServer.getInstance().getPlayerByName(name);
		if (p == null)
			result = ChannelSynchronizationOps.SCAN_PLAYER_CHANNEL_NO_MATCH;
		else
			result = ChannelSynchronizationOps.SCAN_PLAYER_CHANNEL_FOUND;

		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(9);
		writeShopChannelSynchronizationPacketHeader(lew, ChannelSynchronizationOps.PLAYER_SEARCH_RESPONSE);
		lew.writeInt(responseId);
		lew.writeByte(result);

		writeShopChannelSynchronizationPacket(lew.getBytes());
	}

	public int sendBuddyLogInNotifications(int sender, int[] recipients) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(9 + recipients.length * 4);
		writeShopChannelSynchronizationPacketHeader(lew, ChannelSynchronizationOps.BUDDY_ONLINE);
		lew.writeInt(sender);
		lew.writeByte((byte) recipients.length);
		for (int i = 0; i < recipients.length; i++)
			lew.writeInt(recipients[i]);

		writeShopChannelSynchronizationPacket(lew.getBytes());
		return 0;
	}

	public void sendBuddyLogOffNotifications(int sender, int[] recipients) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(9 + recipients.length * 4);
		writeShopChannelSynchronizationPacketHeader(lew, ChannelSynchronizationOps.BUDDY_OFFLINE);
		lew.writeInt(sender);
		lew.writeByte((byte) recipients.length);
		for (Integer receiver : recipients)
			lew.writeInt(receiver.intValue());

		writeShopChannelSynchronizationPacket(lew.getBytes());
	}
}
