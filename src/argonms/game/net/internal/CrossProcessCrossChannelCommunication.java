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

package argonms.game.net.internal;

import argonms.common.net.internal.RemoteCenterOps;
import argonms.common.util.input.LittleEndianReader;
import argonms.common.util.output.LittleEndianByteArrayWriter;
import argonms.common.util.output.LittleEndianWriter;
import argonms.game.GameServer;
import argonms.game.character.BuffState;
import argonms.game.character.PlayerContinuation;
import argonms.game.field.entity.PlayerSkillSummon;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 *
 * @author GoldenKevin
 */
public class CrossProcessCrossChannelCommunication implements CrossChannelCommunication {
	private static final byte
		INBOUND_PLAYER = 1,
		INBOUND_PLAYER_ACCEPTED = 2,
		PLAYER_SEARCH = 3,
		PLAYER_SEARCH_RESPONSE = 4,
		MULTI_CHAT = 5,
		WHISPER_CHAT = 6,
		WHISPER_RESPONSE = 7,
		SPOUSE_CHAT = 8,
		BUDDY_INVITE = 9,
		BUDDY_INVITE_RESPONSE = 10,
		BUDDY_ONLINE = 11,
		BUDDY_ACCEPTED = 12,
		BUDDY_ONLINE_RESPONSE = 13,
		BUDDY_OFFLINE = 14,
		BUDDY_DELETE = 15,
		CHATROOM_INVITE = 16,
		CHATROOM_INVITE_RESPONSE = 17,
		CHATROOM_DECLINE = 18,
		CHATROOM_TEXT = 19
	;

	private static class ResponseFuture<T> implements Future<List<T>> {
		private final CountDownLatch waitHandle;
		private final List<T> responses;
		private final AtomicInteger responsesReceived;
		private final AtomicBoolean canceled;

		public ResponseFuture(int expectedResponses) {
			waitHandle = new CountDownLatch(expectedResponses);
			responses = new ArrayList<T>(expectedResponses);
			responsesReceived = new AtomicInteger(0);
			canceled = new AtomicBoolean(false);
		}

		@Override
		public boolean cancel(boolean mayInterruptIfRunning) {
			if (canceled.compareAndSet(false, true)) {
				for (long i = waitHandle.getCount() - 1; i >= 0; i--)
					waitHandle.countDown();
				return true;
			}
			return false;
		}

		@Override
		public boolean isCancelled() {
			return canceled.get();
		}

		@Override
		public boolean isDone() {
			return isCancelled() || waitHandle.getCount() == 0;
		}

		@Override
		public List<T> get() throws InterruptedException, ExecutionException {
			waitHandle.await();
			if (canceled.get())
				throw new CancellationException();
			return responses;
		}

		@Override
		public List<T> get(long timeout, TimeUnit unit) throws InterruptedException, TimeoutException {
			if (!waitHandle.await(timeout, unit))
				throw new TimeoutException("Response for inter channel intercourse timed out after " + timeout + " " + unit.toString().toLowerCase() + ". Needed " + waitHandle.getCount() + " more responses.");
			if (canceled.get())
				throw new CancellationException();
			return responses;
		}

		public void set(T response) {
			responses.set(responsesReceived.getAndIncrement(), response);
			waitHandle.countDown();
		}
	}

	private final CrossServerCommunication handler;
	private final byte localCh;
	private final byte targetCh;
	private final byte[] ipAddress;
	private int port;

	public CrossProcessCrossChannelCommunication(CrossServerCommunication self, byte localCh, byte remoteCh, byte[] ipAddress, int port) {
		this.handler = self;
		this.localCh = localCh;
		this.targetCh = remoteCh;
		this.ipAddress = ipAddress;
		this.port = port;
	}

	@Override
	public byte[] getIpAddress() throws UnknownHostException {
		return ipAddress;
	}

	@Override
	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	private void writeCrossProcessCrossChannelCommunicationPacketHeader(LittleEndianWriter lew, byte opcode) {
		lew.writeByte(RemoteCenterOps.INTER_CHANNEL);
		lew.writeByte(targetCh);
		lew.writeByte(localCh);
		lew.writeByte(opcode);
	}

	private void writeCrossProcessCrossChannelCommunicationPacket(byte[] packet) {
		GameServer.getInstance().getCenterInterface().getSession().send(packet);
	}

	public void receivedCrossProcessCrossChannelCommunicationPacket(LittleEndianReader packet) {
		switch (packet.readByte()) {
			case INBOUND_PLAYER:
				receivedPlayerContext(packet);
				break;
			case INBOUND_PLAYER_ACCEPTED:
				receivedChannelChangeAcceptance(packet);
				break;
		}
	}

	@Override
	public void sendPlayerContext(int playerId, PlayerContinuation context) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter();
		writeCrossProcessCrossChannelCommunicationPacketHeader(lew, INBOUND_PLAYER);
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
		Map<Integer, PlayerSkillSummon> activeSummons = context.getActiveSummons();
		lew.writeByte((byte) activeSummons.size());
		for (Map.Entry<Integer, PlayerSkillSummon> summon : activeSummons.entrySet()) {
			PlayerSkillSummon summonState = summon.getValue();
			lew.writeInt(summon.getKey().intValue());
			lew.writePos(summonState.getPosition());
			lew.writeByte(summonState.getStance());
		}
		lew.writeShort(context.getEnergyCharge());
		lew.writeInt(context.getChatroomId());

		writeCrossProcessCrossChannelCommunicationPacket(lew.getBytes());
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

		handler.receivedChannelChangeRequest(targetCh, playerId, context);
	}

	@Override
	public void sendChannelChangeAcceptance(int playerId) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(8);
		writeCrossProcessCrossChannelCommunicationPacketHeader(lew, INBOUND_PLAYER_ACCEPTED);
		lew.writeInt(playerId);

		writeCrossProcessCrossChannelCommunicationPacket(lew.getBytes());
	}

	private void receivedChannelChangeAcceptance(LittleEndianReader packet) {
		int playerId = packet.readInt();

		handler.receivedChannelChangeAcceptance(targetCh, playerId);
	}
}
