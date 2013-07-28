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
import argonms.common.net.internal.ChannelSynchronizationOps;
import argonms.common.net.internal.RemoteCenterOps;
import argonms.common.util.collections.Pair;
import argonms.common.util.input.LittleEndianReader;
import argonms.common.util.output.LittleEndianByteArrayWriter;
import argonms.common.util.output.LittleEndianWriter;
import argonms.game.character.PlayerContinuation;
import argonms.game.command.CommandTarget;
import argonms.game.command.CrossChannelCommandTarget;
import argonms.game.field.entity.PlayerSkillSummon;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;

/**
 *
 * @author GoldenKevin
 */
public class CrossProcessCrossChannelSynchronization extends ChannelOrShopSynchronization implements CrossChannelSynchronization {
	private final byte localCh;
	private final byte serverId;
	private final byte[] ipAddress;
	private int port;

	public CrossProcessCrossChannelSynchronization(CrossServerSynchronization self, byte localCh, byte remoteCh, byte serverId, byte[] ipAddress, int port) {
		super(self, remoteCh);
		this.localCh = localCh;
		this.serverId = serverId;
		this.ipAddress = ipAddress;
		this.port = port;
	}

	public byte getServerId() {
		return serverId;
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

	@Override
	protected void writeSynchronizationPacketHeader(LittleEndianWriter lew, byte opcode) {
		lew.writeByte(RemoteCenterOps.CROSS_CHANNEL_SYNCHRONIZATION);
		lew.writeByte(targetCh);
		lew.writeByte(localCh);
		lew.writeByte(opcode);
	}

	public void receivedCrossProcessCrossChannelSynchronizationPacket(LittleEndianReader packet) {
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
			case ChannelSynchronizationOps.MULTI_CHAT:
				receivedPrivateChat(packet);
				break;
			case ChannelSynchronizationOps.WHISPER_CHAT:
				receivedWhisper(packet);
				break;
			case ChannelSynchronizationOps.WHISPER_RESPONSE:
				receivedWhisperResult(packet);
				break;
			case ChannelSynchronizationOps.SPOUSE_CHAT:
				receivedSpouseChat(packet);
				break;
			case ChannelSynchronizationOps.BUDDY_INVITE:
				receivedBuddyInvite(packet);
				break;
			case ChannelSynchronizationOps.BUDDY_INVITE_RESPONSE:
				receivedBuddyInviteResult(packet);
				break;
			case ChannelSynchronizationOps.BUDDY_INVITE_RETRACTION:
				receivedBuddyInviteRetracted(packet);
				break;
			case ChannelSynchronizationOps.BUDDY_ONLINE:
				receivedSentBuddyLogInNotifications(packet);
				break;
			case ChannelSynchronizationOps.BUDDY_ONLINE_RESPONSE:
				receivedReturnedBuddyLogInNotifications(packet);
				break;
			case ChannelSynchronizationOps.BUDDY_ACCEPTED:
				receivedBuddyInviteAccepted(packet);
				break;
			case ChannelSynchronizationOps.BUDDY_OFFLINE:
				receivedBuddyLogOffNotifications(packet);
				break;
			case ChannelSynchronizationOps.BUDDY_DELETED:
				receivedBuddyDeleted(packet);
				break;
			case ChannelSynchronizationOps.CHATROOM_INVITE:
				receivedChatroomInvite(packet);
				break;
			case ChannelSynchronizationOps.CHATROOM_INVITE_RESPONSE:
				receivedChatroomInviteResult(packet);
				break;
			case ChannelSynchronizationOps.CHATROOM_DECLINE:
				receivedChatroomDecline(packet);
				break;
			case ChannelSynchronizationOps.CHATROOM_TEXT:
				receivedChatroomText(packet);
				break;
			case ChannelSynchronizationOps.CROSS_CHANNEL_COMMAND_CHARACTER_MANIPULATION:
				receivedCrossChannelCommandCharacterManipulation(packet);
				break;
			case ChannelSynchronizationOps.CROSS_CHANNEL_COMMAND_CHARACTER_ACCESS:
				receivedCrossChannelCommandCharacterAccess(packet);
				break;
			case ChannelSynchronizationOps.CROSS_CHANNEL_COMMAND_CHARACTER_ACCESS_RESPONSE:
				receivedCrossChannelCommandCharacterAccessResult(packet);
				break;
			case ChannelSynchronizationOps.SYNCHRONIZED_NOTICE:
				receivedWorldWideNotice(packet);
				break;
			case ChannelSynchronizationOps.SYNCHRONIZED_SHUTDOWN:
				receivedServerShutdown(packet);
				break;
			case ChannelSynchronizationOps.SYNCHRONIZED_RATE_CHANGE:
				receivedServerRateChange(packet);
				break;
			case ChannelSynchronizationOps.WHO_COMMAND:
				receivedRetrieveConnectedPlayersList(packet);
				break;
			case ChannelSynchronizationOps.WHO_COMMAND_RESPONSE:
				receivedRetrieveConnectedPlayersListResult(packet);
				break;
		}
	}

	@Override
	public void sendPlayerContext(int playerId, PlayerContinuation context) {
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

		handler.receivedChannelChangeRequest(targetCh, playerId, context);
	}

	@Override
	public void sendPrivateChat(byte type, int[] recipients, String name, String message) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(10 + recipients.length * 4 + name.length() + message.length());
		writeSynchronizationPacketHeader(lew, ChannelSynchronizationOps.MULTI_CHAT);
		lew.writeByte(type);
		lew.writeByte((byte) recipients.length);
		for (byte i = 0; i < recipients.length; i++)
			lew.writeInt(recipients[i]);
		lew.writeLengthPrefixedString(name);
		lew.writeLengthPrefixedString(message);

		writeSynchronizationPacket(lew.getBytes());
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
		writeSynchronizationPacketHeader(lew, ChannelSynchronizationOps.WHISPER_CHAT);
		lew.writeInt(responseId);
		lew.writeLengthPrefixedString(recipient);
		lew.writeLengthPrefixedString(sender);
		lew.writeLengthPrefixedString(message);

		writeSynchronizationPacket(lew.getBytes());
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
		writeSynchronizationPacketHeader(lew, ChannelSynchronizationOps.WHISPER_RESPONSE);
		lew.writeInt(responseId);
		lew.writeBool(result);

		writeSynchronizationPacket(lew.getBytes());
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
		writeSynchronizationPacketHeader(lew, ChannelSynchronizationOps.SPOUSE_CHAT);
		lew.writeInt(recipient);
		lew.writeLengthPrefixedString(sender);
		lew.writeLengthPrefixedString(message);

		writeSynchronizationPacket(lew.getBytes());
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
		writeSynchronizationPacketHeader(lew, ChannelSynchronizationOps.BUDDY_INVITE);
		lew.writeInt(responseId);
		lew.writeInt(recipientId);
		lew.writeInt(senderId);
		lew.writeLengthPrefixedString(senderName);

		writeSynchronizationPacket(lew.getBytes());
	}

	private void receivedBuddyInvite(LittleEndianReader packet) {
		int responseId = packet.readInt();
		int recipient = packet.readInt();
		int sender = packet.readInt();
		String senderName = packet.readLengthPrefixedString();

		returnBuddyInviteResult(responseId, handler.makeBuddyInviteResult(recipient, targetCh, sender, senderName));
	}

	private void returnBuddyInviteResult(int responseId, byte result) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(9);
		writeSynchronizationPacketHeader(lew, ChannelSynchronizationOps.BUDDY_INVITE_RESPONSE);
		lew.writeInt(responseId);
		lew.writeByte(result);

		writeSynchronizationPacket(lew.getBytes());
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
	public boolean sendBuddyInviteRetracted(int sender, int recipient) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(12);
		writeSynchronizationPacketHeader(lew, ChannelSynchronizationOps.BUDDY_INVITE_RETRACTION);
		lew.writeInt(sender);
		lew.writeInt(recipient);

		writeSynchronizationPacket(lew.getBytes());
		return false;
	}

	private void receivedBuddyInviteRetracted(LittleEndianReader packet) {
		int sender = packet.readInt();
		int recipient = packet.readInt();

		handler.receivedBuddyInviteRetracted(recipient, sender);
	}

	@Override
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

	@Override
	public void sendReturnBuddyLogInNotifications(int recipient, List<Integer> senders, boolean bubble) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(10 + senders.size() * 4);
		writeSynchronizationPacketHeader(lew, ChannelSynchronizationOps.BUDDY_ONLINE_RESPONSE);
		lew.writeInt(recipient);
		lew.writeByte((byte) senders.size());
		for (Integer sender : senders)
			lew.writeInt(sender.intValue());
		lew.writeBool(bubble);

		writeSynchronizationPacket(lew.getBytes());
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
	public boolean sendBuddyInviteAccepted(int sender, int recipient) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(12);
		writeSynchronizationPacketHeader(lew, ChannelSynchronizationOps.BUDDY_ACCEPTED);
		lew.writeInt(sender);
		lew.writeInt(recipient);

		writeSynchronizationPacket(lew.getBytes());
		return false;
	}

	private void receivedBuddyInviteAccepted(LittleEndianReader packet) {
		int sender = packet.readInt();
		int receiver = packet.readInt();

		handler.receivedBuddyInviteAccepted(sender, receiver, targetCh);
	}

	@Override
	public void sendBuddyLogOffNotifications(int sender, int[] recipients) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(9 + recipients.length * 4);
		writeSynchronizationPacketHeader(lew, ChannelSynchronizationOps.BUDDY_OFFLINE);
		lew.writeInt(sender);
		lew.writeByte((byte) recipients.length);
		for (Integer receiver : recipients)
			lew.writeInt(receiver.intValue());

		writeSynchronizationPacket(lew.getBytes());
	}

	@Override
	public void sendBuddyDeleted(int sender, int recipient) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(13);
		writeSynchronizationPacketHeader(lew, ChannelSynchronizationOps.BUDDY_DELETED);
		lew.writeInt(sender);
		lew.writeInt(recipient);

		writeSynchronizationPacket(lew.getBytes());
	}

	private void receivedBuddyDeleted(LittleEndianReader packet) {
		int sender = packet.readInt();
		int recipient = packet.readInt();

		handler.receivedBuddyDeleted(recipient, sender);
	}

	@Override
	public void callSendChatroomInvite(BlockingQueue<Pair<Byte, Object>> resultConsumer, String invitee, int roomId, String inviter) {
		int responseId = nextResponseId.incrementAndGet();
		blockingCalls.put(Integer.valueOf(responseId), resultConsumer);

		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(16 + invitee.length() + inviter.length());
		writeSynchronizationPacketHeader(lew, ChannelSynchronizationOps.CHATROOM_INVITE);
		lew.writeLengthPrefixedString(invitee);
		lew.writeInt(roomId);
		lew.writeLengthPrefixedString(inviter);
		lew.writeInt(responseId);

		writeSynchronizationPacket(lew.getBytes());
	}

	private void receivedChatroomInvite(LittleEndianReader packet) {
		String invitee = packet.readLengthPrefixedString();
		int roomId = packet.readInt();
		String inviter = packet.readLengthPrefixedString();
		int responseId = packet.readInt();

		returnChatroomInviteResult(responseId, handler.makeChatroomInviteResult(invitee, roomId, inviter));
	}

	private void returnChatroomInviteResult(int responseId, boolean result) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(9);
		writeSynchronizationPacketHeader(lew, ChannelSynchronizationOps.CHATROOM_INVITE_RESPONSE);
		lew.writeInt(responseId);
		lew.writeBool(result);

		writeSynchronizationPacket(lew.getBytes());
	}

	private void receivedChatroomInviteResult(LittleEndianReader packet) {
		int responseId = packet.readInt();
		boolean result = packet.readBool();

		BlockingQueue<Pair<Byte, Object>> consumer = blockingCalls.remove(Integer.valueOf(responseId));
		if (consumer == null)
			//timed out and garbage collected
			return;

		consumer.offer(new Pair<Byte, Object>(Byte.valueOf(targetCh), Boolean.valueOf(result)));
	}

	@Override
	public boolean sendChatroomDecline(String invitee, String inviter) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(8 + inviter.length() + invitee.length());
		writeSynchronizationPacketHeader(lew, ChannelSynchronizationOps.CHATROOM_DECLINE);
		lew.writeLengthPrefixedString(inviter);
		lew.writeLengthPrefixedString(invitee);

		writeSynchronizationPacket(lew.getBytes());
		return false;
	}

	private void receivedChatroomDecline(LittleEndianReader packet) {
		String inviter = packet.readLengthPrefixedString();
		String invitee = packet.readLengthPrefixedString();

		handler.receivedChatroomDecline(invitee, inviter);
	}

	@Override
	public void sendChatroomText(String text, int roomId, int sender) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(14 + text.length());
		writeSynchronizationPacketHeader(lew, ChannelSynchronizationOps.CHATROOM_TEXT);
		lew.writeLengthPrefixedString(text);
		lew.writeInt(roomId);
		lew.writeInt(sender);

		writeSynchronizationPacket(lew.getBytes());
	}

	private void receivedChatroomText(LittleEndianReader packet) {
		String text = packet.readLengthPrefixedString();
		int roomId = packet.readInt();
		int sender = packet.readInt();

		handler.receivedChatroomText(text, roomId, sender);
	}

	@Override
	public void sendCrossChannelCommandCharacterManipulation(String recipient, List<CommandTarget.CharacterManipulation> updates) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter();
		writeSynchronizationPacketHeader(lew, ChannelSynchronizationOps.CROSS_CHANNEL_COMMAND_CHARACTER_MANIPULATION);
		lew.writeLengthPrefixedString(recipient);
		CrossChannelCommandTarget.serialize(updates, lew);

		writeSynchronizationPacket(lew.getBytes());
	}

	private void receivedCrossChannelCommandCharacterManipulation(LittleEndianReader packet) {
		String recipient = packet.readLengthPrefixedString();
		List<CommandTarget.CharacterManipulation> updates = CrossChannelCommandTarget.deserialize(packet);

		handler.receivedCrossChannelCommandCharacterManipulation(recipient, updates);
	}

	@Override
	public void callCrossChannelCommandCharacterAccess(BlockingQueue<Pair<Byte, Object>> resultConsumer, String target, CommandTarget.CharacterProperty key) {
		int responseId = nextResponseId.incrementAndGet();
		blockingCalls.put(Integer.valueOf(responseId), resultConsumer);

		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(11 + target.length());
		writeSynchronizationPacketHeader(lew, ChannelSynchronizationOps.CROSS_CHANNEL_COMMAND_CHARACTER_ACCESS);
		lew.writeInt(responseId);
		lew.writeLengthPrefixedString(target);
		lew.writeByte(key.byteValue());

		writeSynchronizationPacket(lew.getBytes());
	}

	private void receivedCrossChannelCommandCharacterAccess(LittleEndianReader packet) {
		int responseId = packet.readInt();
		String target = packet.readLengthPrefixedString();
		CommandTarget.CharacterProperty key = CommandTarget.CharacterProperty.valueOf(packet.readByte());

		returnCrossChannelCommandCharacterAccessResult(responseId, handler.makeCrossChannelCommandCharacterAccessResult(target, key), key);
	}

	private void returnCrossChannelCommandCharacterAccessResult(int responseId, Object result, CommandTarget.CharacterProperty key) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(9 + key.getSizeOfValue());
		writeSynchronizationPacketHeader(lew, ChannelSynchronizationOps.CROSS_CHANNEL_COMMAND_CHARACTER_ACCESS_RESPONSE);
		lew.writeInt(responseId);
		lew.writeByte(key.byteValue());
		CrossChannelCommandTarget.serialize(key, result, lew);

		writeSynchronizationPacket(lew.getBytes());
	}

	private void receivedCrossChannelCommandCharacterAccessResult(LittleEndianReader packet) {
		int responseId = packet.readInt();
		CommandTarget.CharacterProperty key = CommandTarget.CharacterProperty.valueOf(packet.readByte());
		Object result = CrossChannelCommandTarget.deserialize(key, packet);

		BlockingQueue<Pair<Byte, Object>> consumer = blockingCalls.remove(Integer.valueOf(responseId));
		if (consumer == null)
			//timed out and garbage collected
			return;

		consumer.offer(new Pair<Byte, Object>(Byte.valueOf(targetCh), result));
	}

	private void receivedWorldWideNotice(LittleEndianReader packet) {
		byte style = packet.readByte();
		String message = packet.readLengthPrefixedString();

		handler.receivedWorldWideNotice(style, message);
	}

	@Override
	public void sendServerShutdown(boolean halt, boolean restart, boolean cancel, int seconds, String message) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(13 + message.length());
		writeSynchronizationPacketHeader(lew, ChannelSynchronizationOps.SYNCHRONIZED_SHUTDOWN);
		lew.writeBool(halt);
		lew.writeBool(restart);
		lew.writeBool(cancel);
		lew.writeInt(seconds);
		lew.writeLengthPrefixedString(message);

		writeSynchronizationPacket(lew.getBytes());
	}

	private void receivedServerShutdown(LittleEndianReader packet) {
		boolean halt = packet.readBool();
		boolean restart = packet.readBool();
		boolean cancel = packet.readBool();
		int seconds = packet.readInt();
		String message = packet.readLengthPrefixedString();

		handler.receivedServerShutdown(halt, restart, cancel, seconds, message);
	}

	@Override
	public void sendServerRateChange(byte type, short newRate) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(7);
		writeSynchronizationPacketHeader(lew, ChannelSynchronizationOps.SYNCHRONIZED_RATE_CHANGE);
		lew.writeByte(type);
		lew.writeShort(newRate);

		writeSynchronizationPacket(lew.getBytes());
	}

	private void receivedServerRateChange(LittleEndianReader packet) {
		byte type = packet.readByte();
		short newRate = packet.readShort();

		handler.receivedServerRateChange(type, newRate);
	}

	@Override
	public void callRetrieveConnectedPlayersList(BlockingQueue<Pair<Byte, Object>> resultConsumer, byte privilegeLevelLimit) {
		int responseId = nextResponseId.incrementAndGet();
		blockingCalls.put(Integer.valueOf(responseId), resultConsumer);

		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(9);
		writeSynchronizationPacketHeader(lew, ChannelSynchronizationOps.WHO_COMMAND);
		lew.writeInt(responseId);
		lew.writeByte(privilegeLevelLimit);

		writeSynchronizationPacket(lew.getBytes());
	}

	private void receivedRetrieveConnectedPlayersList(LittleEndianReader packet) {
		int responseId = packet.readInt();
		byte privilegeLevelLimit = packet.readByte();

		returnRetrieveConnectedPlayersListResult(responseId, handler.makeRetrieveConnectedPlayersListResult(privilegeLevelLimit));
	}

	private void returnRetrieveConnectedPlayersListResult(int responseId, String result) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(10 + result.length());
		writeSynchronizationPacketHeader(lew, ChannelSynchronizationOps.WHO_COMMAND_RESPONSE);
		lew.writeInt(responseId);
		lew.writeLengthPrefixedString(result);

		writeSynchronizationPacket(lew.getBytes());
	}

	private void receivedRetrieveConnectedPlayersListResult(LittleEndianReader packet) {
		int responseId = packet.readInt();
		String result = packet.readLengthPrefixedString();

		BlockingQueue<Pair<Byte, Object>> consumer = blockingCalls.remove(Integer.valueOf(responseId));
		if (consumer == null)
			//timed out and garbage collected
			return;

		consumer.offer(new Pair<Byte, Object>(Byte.valueOf(targetCh), result));
	}
}
