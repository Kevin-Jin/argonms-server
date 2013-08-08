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

package argonms.game.net.internal;

import argonms.common.character.BuffState;
import argonms.common.character.ShopPlayerContinuation;
import argonms.common.net.internal.ChannelSynchronizationOps;
import argonms.common.net.internal.RemoteCenterOps;
import argonms.common.util.input.LittleEndianReader;
import argonms.common.util.output.LittleEndianByteArrayWriter;
import argonms.common.util.output.LittleEndianWriter;
import argonms.game.character.PlayerContinuation;
import java.util.Map;

/**
 *
 * @author GoldenKevin
 */
public class ChannelShopSynchronization extends ChannelOrShopSynchronization {
	private final byte localWorld;
	private final byte localCh;
	private final byte[] ipAddress;
	private int port;

	public ChannelShopSynchronization(CrossServerSynchronization self, byte localWorld, byte localCh, byte[] ipAddress, int port) {
		super(self, ChannelSynchronizationOps.CHANNEL_CASH_SHOP);
		this.localWorld = localWorld;
		this.localCh = localCh;
		this.ipAddress = ipAddress;
		this.port = port;
	}

	public byte[] getIpAddress() {
		return ipAddress;
	}

	public int getPort() {
		return port;
	}

	@Override
	protected void writeSynchronizationPacketHeader(LittleEndianWriter lew, byte opcode) {
		lew.writeByte(RemoteCenterOps.SHOP_CHANNEL_SHOP_SYNCHRONIZATION);
		lew.writeByte(localWorld);
		lew.writeByte(localCh);
		lew.writeByte(opcode);
	}

	public void sendPlayerContext(int playerId, ShopPlayerContinuation context) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter();
		writeSynchronizationPacketHeader(lew, ChannelSynchronizationOps.INBOUND_PLAYER);
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
		lew.writeInt(context.getChatroomId());
		lew.writeBool(context.isEnteringCashShop());

		writeSynchronizationPacket(lew.getBytes());
	}

	private void receivedPlayerContext(LittleEndianReader packet) {
		PlayerContinuation context = new PlayerContinuation();
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
			context.addActiveSummon(packet.readInt(), playerId, packet.readPos(), packet.readByte());
		context.setEnergyCharge(packet.readShort());
		context.setChatroomId(packet.readInt());
		context.setOriginChannel(targetCh);

		handler.receivedChannelChangeRequest(targetCh, playerId, context);
	}

	public void receivedShopChannelSynchronizationPacket(LittleEndianReader packet) {
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
			case ChannelSynchronizationOps.PLAYER_SEARCH_RESPONSE:
				receivedPlayerExistsResult(packet);
				break;
			case ChannelSynchronizationOps.BUDDY_ONLINE:
				receivedSentBuddyLogInNotifications(packet);
				break;
			case ChannelSynchronizationOps.BUDDY_ONLINE_RESPONSE:
				receivedReturnedBuddyLogInNotifications(packet);
				break;
			case ChannelSynchronizationOps.BUDDY_OFFLINE:
				receivedBuddyLogOffNotifications(packet);
				break;
		}
	}
}
