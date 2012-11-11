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
import argonms.common.util.collections.Pair;
import argonms.common.util.input.LittleEndianReader;
import argonms.common.util.output.LittleEndianByteArrayWriter;
import argonms.common.util.output.LittleEndianWriter;
import argonms.game.GameServer;
import argonms.game.character.BuffState;
import argonms.game.character.PlayerContinuation;
import argonms.game.field.entity.PlayerSkillSummon;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
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
		BUDDY_DELETED = 15,
		CHATROOM_INVITE = 16,
		CHATROOM_INVITE_RESPONSE = 17,
		CHATROOM_DECLINE = 18,
		CHATROOM_TEXT = 19
	;

	private static class WeakValueMap<K, V> {
		private final Map<K, WeakValue<K, V>> backingMap;
		private final ReferenceQueue<V> queue;

		public WeakValueMap(Map<K, WeakValue<K, V>> backingMap) {
			this.backingMap = backingMap;
			queue = new ReferenceQueue<V>();
		}

		@SuppressWarnings("unchecked")
		private void processQueue() {
			WeakValue<K, V> wv;
			while ((wv = (WeakValue<K, V>) queue.poll()) != null)
				backingMap.remove(wv.key);
		}

		private V getReferenceObject(WeakReference<V> ref) {
			return ref == null ? null : ref.get();
		}

		public V get(K key) {
			return getReferenceObject(backingMap.get(key));
		}

		public V put(K key, V value) {
			processQueue();
			WeakValue<K, V> oldValue = backingMap.put(key, WeakValue.<K, V>create(key, value, queue));
			return getReferenceObject(oldValue);
		}

		public V remove(K key) {
			return getReferenceObject(backingMap.remove(key));
		}

		private static class WeakValue<K, V> extends WeakReference<V> {
			private K key;

			private WeakValue(K key, V value, ReferenceQueue<V> queue) {
				super(value, queue);
				this.key = key;
			}

			private static <K, V> WeakValue<K, V> create(K key, V value, ReferenceQueue<V> queue) {
				return (value == null ? null : new WeakValue<K, V>(key, value, queue));
			}

			@Override
			public boolean equals(Object obj) {
				if (this == obj)
					return true;
				if (!(obj instanceof WeakValue))
					return false;
				Object ref1 = this.get();
				Object ref2 = ((WeakValue) obj).get();
				if (ref1 == ref2)
					return true;
				if ((ref1 == null) || (ref2 == null))
					return false;
				return ref1.equals(ref2);
			}

			@Override
			public int hashCode() {
				Object ref = this.get();
				return (ref == null) ? 0 : ref.hashCode();
			}
		}
	}

	private final CrossServerCommunication handler;
	private final byte localCh;
	private final byte targetCh;
	private final byte[] ipAddress;
	private int port;
	private final WeakValueMap<Integer, BlockingQueue<Pair<Byte, Object>>> blockingCalls;
	private final AtomicInteger nextResponseId;

	public CrossProcessCrossChannelCommunication(CrossServerCommunication self, byte localCh, byte remoteCh, byte[] ipAddress, int port) {
		this.handler = self;
		this.localCh = localCh;
		this.targetCh = remoteCh;
		this.ipAddress = ipAddress;
		this.port = port;
		//prevents memory leaks in case responses time out and never reach us
		this.blockingCalls = new WeakValueMap<Integer, BlockingQueue<Pair<Byte, Object>>>
				(new ConcurrentHashMap<Integer, WeakValueMap.WeakValue<Integer, BlockingQueue<Pair<Byte, Object>>>>());
		this.nextResponseId = new AtomicInteger(0);
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
			case PLAYER_SEARCH:
				receivedPlayerExistsCheck(packet);
				break;
			case PLAYER_SEARCH_RESPONSE:
				receivedPlayerExistsResult(packet);
				break;
			case MULTI_CHAT:
				receivedPrivateChat(packet);
				break;
			case WHISPER_CHAT:
				receivedWhisper(packet);
				break;
			case WHISPER_RESPONSE:
				receivedWhisperResult(packet);
				break;
			case SPOUSE_CHAT:
				receivedSpouseChat(packet);
				break;
			case BUDDY_INVITE:
				receivedBuddyInvite(packet);
				break;
			case BUDDY_INVITE_RESPONSE:
				receivedBuddyInviteResult(packet);
				break;
			case BUDDY_ONLINE:
				receivedSentBuddyLogInNotifications(packet);
				break;
			case BUDDY_ONLINE_RESPONSE:
				receivedReturnedBuddyLogInNotifications(packet);
				break;
			case BUDDY_ACCEPTED:
				receivedBuddyAccepted(packet);
				break;
			case BUDDY_OFFLINE:
				receivedBuddyLogOffNotifications(packet);
				break;
			case BUDDY_DELETED:
				receivedBuddyDeleted(packet);
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

	@Override
	public void callPlayerExistsCheck(BlockingQueue<Pair<Byte, Object>> resultConsumer, String name) {
		int responseId = nextResponseId.incrementAndGet();
		blockingCalls.put(Integer.valueOf(responseId), resultConsumer);

		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(10 + name.length());
		writeCrossProcessCrossChannelCommunicationPacketHeader(lew, PLAYER_SEARCH);
		lew.writeInt(responseId);
		lew.writeLengthPrefixedString(name);

		writeCrossProcessCrossChannelCommunicationPacket(lew.getBytes());
	}

	private void receivedPlayerExistsCheck(LittleEndianReader packet) {
		int responseId = packet.readInt();
		String name = packet.readLengthPrefixedString();

		returnPlayerExistsResult(responseId, handler.makePlayerExistsResult(name));
	}

	private void returnPlayerExistsResult(int responseId, boolean result) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(9);
		writeCrossProcessCrossChannelCommunicationPacketHeader(lew, PLAYER_SEARCH_RESPONSE);
		lew.writeInt(responseId);
		lew.writeBool(result);

		writeCrossProcessCrossChannelCommunicationPacket(lew.getBytes());
	}

	private void receivedPlayerExistsResult(LittleEndianReader packet) {
		int responseId = packet.readInt();
		boolean result = packet.readBool();

		BlockingQueue<Pair<Byte, Object>> consumer = blockingCalls.remove(Integer.valueOf(responseId));
		if (consumer == null)
			//timed out and garbage collected
			return;

		consumer.offer(new Pair<Byte, Object>(Byte.valueOf(targetCh), Boolean.valueOf(result)));
	}

	@Override
	public void sendPrivateChat(byte type, int[] recipients, String name, String message) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(10 + recipients.length * 4 + name.length() + message.length());
		writeCrossProcessCrossChannelCommunicationPacketHeader(lew, MULTI_CHAT);
		lew.writeByte(type);
		lew.writeByte((byte) recipients.length);
		for (byte i = 0; i < recipients.length; i++)
			lew.writeInt(recipients[i]);
		lew.writeLengthPrefixedString(name);
		lew.writeLengthPrefixedString(message);

		writeCrossProcessCrossChannelCommunicationPacket(lew.getBytes());
	}

	private void receivedPrivateChat(LittleEndianReader packet) {
		byte type = packet.readByte();
		byte amount = packet.readByte();
		int[] recipients = new int[amount];
		for (byte i = 0; i < amount; i++)
			recipients[i] = packet.readInt();
		String name = packet.readLengthPrefixedString();
		String message = packet.readLengthPrefixedString();

		handler.receivedPrivateChat(type, recipients, name, message);
	}

	@Override
	public void callSendWhisper(BlockingQueue<Pair<Byte, Object>> resultConsumer, String recipient, String sender, String message) {
		int responseId = nextResponseId.incrementAndGet();
		blockingCalls.put(Integer.valueOf(responseId), resultConsumer);

		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(14 + recipient.length() + sender.length() + message.length());
		writeCrossProcessCrossChannelCommunicationPacketHeader(lew, WHISPER_CHAT);
		lew.writeInt(responseId);
		lew.writeLengthPrefixedString(recipient);
		lew.writeLengthPrefixedString(sender);
		lew.writeLengthPrefixedString(message);

		writeCrossProcessCrossChannelCommunicationPacket(lew.getBytes());
	}

	private void receivedWhisper(LittleEndianReader packet) {
		int responseId = packet.readInt();
		String recipient = packet.readLengthPrefixedString();
		String sender = packet.readLengthPrefixedString();
		String message = packet.readLengthPrefixedString();

		returnWhisperResult(responseId, handler.makeWhisperResult(recipient, sender, message, targetCh));
	}

	private void returnWhisperResult(int responseId, boolean result) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(9);
		writeCrossProcessCrossChannelCommunicationPacketHeader(lew, WHISPER_RESPONSE);
		lew.writeInt(responseId);
		lew.writeBool(result);

		writeCrossProcessCrossChannelCommunicationPacket(lew.getBytes());
	}

	private void receivedWhisperResult(LittleEndianReader packet) {
		int responseId = packet.readInt();
		boolean result = packet.readBool();

		BlockingQueue<Pair<Byte, Object>> consumer = blockingCalls.remove(Integer.valueOf(responseId));
		if (consumer == null)
			//timed out and garbage collected
			return;

		consumer.offer(new Pair<Byte, Object>(Byte.valueOf(targetCh), Boolean.valueOf(result)));
	}

	@Override
	public boolean sendSpouseChat(int recipient, String sender, String message) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(12 + sender.length() + message.length());
		writeCrossProcessCrossChannelCommunicationPacketHeader(lew, SPOUSE_CHAT);
		lew.writeInt(recipient);
		lew.writeLengthPrefixedString(sender);
		lew.writeLengthPrefixedString(message);

		writeCrossProcessCrossChannelCommunicationPacket(lew.getBytes());
		return false;
	}

	private void receivedSpouseChat(LittleEndianReader packet) {
		int recipient = packet.readInt();
		String sender = packet.readLengthPrefixedString();
		String message = packet.readLengthPrefixedString();

		handler.receivedSpouseChat(recipient, sender, message);
	}

	@Override
	public void callSendBuddyInvite(BlockingQueue<Pair<Byte, Object>> resultConsumer, int recipientId, int senderId, String senderName) {
		int responseId = nextResponseId.incrementAndGet();
		blockingCalls.put(Integer.valueOf(responseId), resultConsumer);

		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(18 + senderName.length());
		writeCrossProcessCrossChannelCommunicationPacketHeader(lew, BUDDY_INVITE);
		lew.writeInt(responseId);
		lew.writeInt(recipientId);
		lew.writeInt(senderId);
		lew.writeLengthPrefixedString(senderName);

		writeCrossProcessCrossChannelCommunicationPacket(lew.getBytes());
	}

	private void receivedBuddyInvite(LittleEndianReader packet) {
		int responseId = packet.readInt();
		int recipient = packet.readInt();
		int sender = packet.readInt();
		String senderName = packet.readLengthPrefixedString();

		returnBuddyInviteResult(responseId, handler.receivedBuddyInvite(recipient, sender, senderName));
	}

	private void returnBuddyInviteResult(int responseId, byte result) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(9);
		writeCrossProcessCrossChannelCommunicationPacketHeader(lew, BUDDY_INVITE_RESPONSE);
		lew.writeInt(responseId);
		lew.writeByte(result);

		writeCrossProcessCrossChannelCommunicationPacket(lew.getBytes());
	}

	private void receivedBuddyInviteResult(LittleEndianReader packet) {
		int responseId = packet.readInt();
		byte result = packet.readByte();

		BlockingQueue<Pair<Byte, Object>> consumer = blockingCalls.remove(Integer.valueOf(responseId));
		if (consumer == null)
			//timed out and garbage collected
			return;

		consumer.offer(new Pair<Byte, Object>(Byte.valueOf(targetCh), Byte.valueOf(result)));
	}

	@Override
	public int exchangeBuddyLogInNotifications(int sender, int[] recipients) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(9 + recipients.length * 4);
		writeCrossProcessCrossChannelCommunicationPacketHeader(lew, BUDDY_ONLINE);
		lew.writeInt(sender);
		lew.writeByte((byte) recipients.length);
		for (int i = 0; i < recipients.length; i++)
			lew.writeInt(recipients[i]);

		writeCrossProcessCrossChannelCommunicationPacket(lew.getBytes());
		return 0;
	}

	private void receivedSentBuddyLogInNotifications(LittleEndianReader packet) {
		int sender = packet.readInt();
		byte receiversCount = packet.readByte();
		int[] receivers = new int[receiversCount];
		for (int i = 0; i < receiversCount; i++)
			receivers[i] = packet.readInt();

		handler.receivedSentBuddyLogInNotifications(sender, receivers, targetCh);
	}

	@Override
	public void sendReturnBuddyLogInNotifications(int recipient, List<Integer> senders, boolean bubble) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(10 + senders.size() * 4);
		writeCrossProcessCrossChannelCommunicationPacketHeader(lew, BUDDY_ONLINE_RESPONSE);
		lew.writeInt(recipient);
		lew.writeByte((byte) senders.size());
		for (Integer sender : senders)
			lew.writeInt(sender.intValue());
		lew.writeBool(bubble);

		writeCrossProcessCrossChannelCommunicationPacket(lew.getBytes());
	}

	private void receivedReturnedBuddyLogInNotifications(LittleEndianReader packet) {
		int recipient = packet.readInt();
		byte count = packet.readByte();
		List<Integer> senders = new ArrayList<Integer>();
		for (int i = 0; i < count; i++)
			senders.add(Integer.valueOf(packet.readInt()));
		boolean bubble = packet.readBool();

		handler.receivedReturnedBuddyLogInNotifications(recipient, senders, bubble, targetCh);
	}

	@Override
	public boolean sendBuddyAccepted(int sender, int recipient) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(12);
		writeCrossProcessCrossChannelCommunicationPacketHeader(lew, BUDDY_ACCEPTED);
		lew.writeInt(sender);
		lew.writeInt(recipient);

		writeCrossProcessCrossChannelCommunicationPacket(lew.getBytes());
		return false;
	}

	private void receivedBuddyAccepted(LittleEndianReader packet) {
		int sender = packet.readInt();
		int receiver = packet.readInt();

		handler.receivedBuddyAccepted(sender, receiver, targetCh);
	}

	@Override
	public void sendBuddyLogOffNotifications(int sender, int[] recipients) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(9 + recipients.length * 4);
		writeCrossProcessCrossChannelCommunicationPacketHeader(lew, BUDDY_OFFLINE);
		lew.writeInt(sender);
		lew.writeByte((byte) recipients.length);
		for (Integer receiver : recipients)
			lew.writeInt(receiver.intValue());

		writeCrossProcessCrossChannelCommunicationPacket(lew.getBytes());
	}

	private void receivedBuddyLogOffNotifications(LittleEndianReader packet) {
		int sender = packet.readInt();
		byte count = packet.readByte();
		int[] recipients = new int[count];
		for (int i = 0; i < count; i++)
			recipients[i] = packet.readInt();

		handler.receivedBuddyLogOffNotifications(sender, recipients);
	}

	@Override
	public void sendBuddyDeleted(int sender, int recipient) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(13);
		writeCrossProcessCrossChannelCommunicationPacketHeader(lew, BUDDY_DELETED);
		lew.writeInt(sender);
		lew.writeInt(recipient);

		writeCrossProcessCrossChannelCommunicationPacket(lew.getBytes());
	}

	private void receivedBuddyDeleted(LittleEndianReader packet) {
		int sender = packet.readInt();
		int recipient = packet.readInt();

		handler.receivedBuddyDeleted(recipient, sender);
	}
}
