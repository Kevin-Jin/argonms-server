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

import argonms.common.net.internal.ChannelSynchronizationOps;
import argonms.common.net.internal.CrossProcessSynchronization;
import argonms.common.util.collections.Pair;
import argonms.common.util.input.LittleEndianReader;
import argonms.common.util.output.LittleEndianByteArrayWriter;
import argonms.common.util.output.LittleEndianWriter;
import argonms.game.GameServer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;

/**
 *
 * @author GoldenKevin
 */
public abstract class ChannelOrShopSynchronization extends CrossProcessSynchronization {
	protected final CrossServerSynchronization handler;
	protected final byte targetCh;

	public ChannelOrShopSynchronization(CrossServerSynchronization self, byte remoteCh) {
		handler = self;
		targetCh = remoteCh;
	}

	protected abstract void writeSynchronizationPacketHeader(LittleEndianWriter lew, byte opcode);

	protected void writeSynchronizationPacket(byte[] packet) {
		GameServer.getInstance().getCenterInterface().getSession().send(packet);
	}

	public void sendChannelChangeAcceptance(int playerId) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(8);
		writeSynchronizationPacketHeader(lew, ChannelSynchronizationOps.INBOUND_PLAYER_ACCEPTED);
		lew.writeInt(playerId);

		writeSynchronizationPacket(lew.getBytes());
	}

	protected void receivedChannelChangeAcceptance(LittleEndianReader packet) {
		int playerId = packet.readInt();

		handler.receivedChannelChangeAcceptance(targetCh, playerId);
	}

	public void callPlayerExistsCheck(BlockingQueue<Pair<Byte, Object>> resultConsumer, String name) {
		int responseId = nextResponseId.incrementAndGet();
		blockingCalls.put(Integer.valueOf(responseId), resultConsumer);

		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(10 + name.length());
		writeSynchronizationPacketHeader(lew, ChannelSynchronizationOps.PLAYER_SEARCH);
		lew.writeInt(responseId);
		lew.writeLengthPrefixedString(name);

		writeSynchronizationPacket(lew.getBytes());
	}

	protected void receivedPlayerExistsCheck(LittleEndianReader packet) {
		int responseId = packet.readInt();
		String name = packet.readLengthPrefixedString();

		returnPlayerExistsResult(responseId, handler.makePlayerExistsResult(name));
	}

	private void returnPlayerExistsResult(int responseId, byte result) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(9);
		writeSynchronizationPacketHeader(lew, ChannelSynchronizationOps.PLAYER_SEARCH_RESPONSE);
		lew.writeInt(responseId);
		lew.writeByte(result);

		writeSynchronizationPacket(lew.getBytes());
	}

	protected void receivedPlayerExistsResult(LittleEndianReader packet) {
		int responseId = packet.readInt();
		byte result = packet.readByte();

		BlockingQueue<Pair<Byte, Object>> consumer = blockingCalls.remove(Integer.valueOf(responseId));
		if (consumer == null)
			//timed out and garbage collected
			return;

		consumer.offer(new Pair<Byte, Object>(Byte.valueOf(targetCh), Byte.valueOf(result)));
	}

	public int exchangeBuddyLogInNotifications(int sender, int[] recipients) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(9 + recipients.length * 4);
		writeSynchronizationPacketHeader(lew, ChannelSynchronizationOps.BUDDY_ONLINE);
		lew.writeInt(sender);
		lew.writeByte((byte) recipients.length);
		for (int i = 0; i < recipients.length; i++)
			lew.writeInt(recipients[i]);

		writeSynchronizationPacket(lew.getBytes());
		return 0;
	}

	protected void receivedSentBuddyLogInNotifications(LittleEndianReader packet) {
		int sender = packet.readInt();
		byte receiversCount = packet.readByte();
		int[] receivers = new int[receiversCount];
		for (int i = 0; i < receiversCount; i++)
			receivers[i] = packet.readInt();

		handler.receivedSentBuddyLogInNotifications(sender, receivers, targetCh);
	}

	protected void receivedReturnedBuddyLogInNotifications(LittleEndianReader packet) {
		int recipient = packet.readInt();
		byte count = packet.readByte();
		List<Integer> senders = new ArrayList<Integer>(count);
		for (int i = 0; i < count; i++)
			senders.add(Integer.valueOf(packet.readInt()));
		boolean bubble = packet.readBool();

		handler.receivedReturnedBuddyLogInNotifications(recipient, senders, bubble, targetCh);
	}

	protected void receivedBuddyLogOffNotifications(LittleEndianReader packet) {
		int sender = packet.readInt();
		byte count = packet.readByte();
		int[] recipients = new int[count];
		for (int i = 0; i < count; i++)
			recipients[i] = packet.readInt();

		handler.receivedBuddyLogOffNotifications(sender, recipients);
	}

	public void sendWorldWideNotice(byte style, String message) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(7 + message.length());
		writeSynchronizationPacketHeader(lew, ChannelSynchronizationOps.SYNCHRONIZED_NOTICE);
		lew.writeByte(style);
		lew.writeLengthPrefixedString(message);

		writeSynchronizationPacket(lew.getBytes());
	}
}
